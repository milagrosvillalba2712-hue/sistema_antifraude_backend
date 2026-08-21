-- ============================================================================
-- V18 - Hardening RLS para schedulers y procesos sin tenant.
--
-- Problema:
--   Varias policies hacian cast directo:
--     current_setting('app.current_empresa_id', true)::uuid
--   En conexiones reutilizadas por schedulers, PostgreSQL puede devolver cadena
--   vacia cuando el setting no fue establecido en la transaccion actual. Ese
--   valor provoca SQLState 22P02: invalid input syntax for type uuid: "".
--
-- Solucion:
--   Reemplazar las policies tenant de tablas con empresa_id y RLS activo por
--   un patron fail-closed:
--     empresa_id = nullif(current_setting('app.current_empresa_id', true), '')::uuid
--
--   Si empresa_id es nullable (por ejemplo api_evento para eventos globales),
--   se conserva acceso a filas globales con empresa_id IS NULL.
-- ============================================================================

DO $$
DECLARE
    rec record;
    pol record;
    policy_name text;
    tenant_expr text;
BEGIN
    FOR rec IN
        SELECT
            c.table_schema,
            c.table_name,
            c.is_nullable = 'YES' AS empresa_nullable
        FROM information_schema.columns c
        JOIN pg_class pc ON pc.relname = c.table_name
        JOIN pg_namespace pn ON pn.oid = pc.relnamespace AND pn.nspname = c.table_schema
        WHERE c.table_schema = 'public'
          AND c.column_name = 'empresa_id'
          AND pc.relkind = 'r'
          AND pc.relrowsecurity = true
    LOOP
        FOR pol IN
            SELECT policyname
            FROM pg_policies
            WHERE schemaname = rec.table_schema
              AND tablename = rec.table_name
        LOOP
            EXECUTE format('DROP POLICY IF EXISTS %I ON %I.%I',
                           pol.policyname, rec.table_schema, rec.table_name);
        END LOOP;

        policy_name := 'tenant_isolation';
        tenant_expr := 'empresa_id = nullif(current_setting(''app.current_empresa_id'', true), '''')::uuid';
        IF rec.empresa_nullable THEN
            tenant_expr := '(empresa_id IS NULL OR ' || tenant_expr || ')';
        END IF;

        EXECUTE format(
            'CREATE POLICY %I ON %I.%I USING (%s) WITH CHECK (%s)',
            policy_name,
            rec.table_schema,
            rec.table_name,
            tenant_expr,
            tenant_expr
        );
    END LOOP;
END $$;

COMMENT ON SCHEMA public IS 'RLS hardening: tenant policies compare empresa_id against nullif(app.current_empresa_id, '''')::uuid to fail closed without UUID cast errors in schedulers.';
