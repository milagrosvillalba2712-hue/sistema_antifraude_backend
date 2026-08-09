-- ============================================================================
-- Regula AML - Clean schema hardening V15
-- Correcciones post-auditoria 2026-07-23.
-- ============================================================================

SET client_min_messages TO warning;

-- ----------------------------------------------------------------------------
-- 1. RLS real y forzado en tablas tenant, incluyendo auditoria_sistema.
-- ----------------------------------------------------------------------------

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'regula_batch') THEN
        CREATE ROLE regula_batch BYPASSRLS NOLOGIN;
    END IF;
END $$;

COMMENT ON ROLE regula_batch IS
    'Rol tecnico con BYPASSRLS para procesos batch/ETL/reportes inter-tenant. No usar desde la aplicacion web transaccional.';

DO $$
DECLARE
    r record;
BEGIN
    FOR r IN
        SELECT unnest(ARRAY[
            'persona','transacciones','alertas_antifraude','ejecucion_reglas','evaluaciones_riesgo',
            'suscripcion','contrato','pago','uso_suscripcion','usuario_empresa','perfil_usuario',
            'disponibilidad_usuario','horario_laboral_usuario','documento','perfil_cliente',
            'cliente_pep','cliente_observado','escenario','accion','reglas_riesgo',
            'control_importe','control_frecuencia','horario_riesgo','calendario_riesgo',
            'hallazgo_alerta','coincidencia_lista_alerta','transaccion_detalle_snapshot',
            'cliente_snapshot_alerta','consulta_kyc_alerta','historial_asignacion',
            'estadistica_carga_analista','caso','caso_alerta','actuacion','comentario_caso',
            'evidencia','evidencia_alerta','historial_estado_caso','resolucion_alerta',
            'aprobacion_supervisor','decision_caso','reportes_ros','consultas_externas',
            'auditoria_sistema'
        ]) AS table_name
    LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', r.table_name);
        EXECUTE format('ALTER TABLE public.%I FORCE ROW LEVEL SECURITY', r.table_name);
    END LOOP;
END $$;

DROP POLICY IF EXISTS tenant_isolation_auditoria_sistema ON auditoria_sistema;
CREATE POLICY tenant_isolation_auditoria_sistema ON auditoria_sistema
USING (empresa_id = current_setting('app.current_empresa_id', true)::uuid)
WITH CHECK (empresa_id = current_setting('app.current_empresa_id', true)::uuid);

-- ----------------------------------------------------------------------------
-- 2. Screening fuzzy para nombres de personas y sujetos de riesgo.
-- ----------------------------------------------------------------------------

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_sujeto_riesgo_nombre_trgm
ON sujeto_riesgo USING gin (nombre_normalizado gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_sujeto_riesgo_alias_trgm
ON sujeto_riesgo_alias USING gin (alias_normalizado gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_persona_nombre_trgm
ON persona USING gin (nombre_razon_social gin_trgm_ops);

-- ----------------------------------------------------------------------------
-- 3. Separacion explicita entre sujetos demo y sujetos productivos.
-- ----------------------------------------------------------------------------

ALTER TABLE sujeto_riesgo
ADD COLUMN IF NOT EXISTS es_demo boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN sujeto_riesgo.es_demo IS
    'Marca registros ficticios/QA. El screening productivo debe excluir registros demo.';

UPDATE sujeto_riesgo
   SET es_demo = true
 WHERE codigo LIKE 'SR-DEMO-%'
    OR detalle_json->>'demo' = 'true';

CREATE INDEX IF NOT EXISTS idx_sujeto_riesgo_es_demo
ON sujeto_riesgo (es_demo)
WHERE es_demo = true;

CREATE OR REPLACE VIEW v_sujeto_riesgo_productivo AS
SELECT *
FROM sujeto_riesgo
WHERE es_demo = false
  AND estado = 'ACTIVO';

COMMENT ON VIEW v_sujeto_riesgo_productivo IS
    'Vista para screening productivo: excluye sujetos demo/QA e inactivos.';

-- ----------------------------------------------------------------------------
-- 4. Horizonte de particiones y control de indices GIN.
-- ----------------------------------------------------------------------------

DO $$
DECLARE
    v_month date := DATE '2028-01-01';
BEGIN
    WHILE v_month < DATE '2028-07-01' LOOP
        PERFORM fn_create_transacciones_month_partition(v_month);
        v_month := (v_month + interval '1 month')::date;
    END LOOP;
END $$;

CREATE OR REPLACE VIEW vw_partition_horizon_control AS
WITH parts AS (
    SELECT to_date(right(c.relname, 7), 'YYYY_MM') AS partition_month
    FROM pg_inherits i
    JOIN pg_class c ON c.oid = i.inhrelid
    JOIN pg_class p ON p.oid = i.inhparent
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public'
      AND p.relname = 'transacciones'
      AND c.relname ~ '^transacciones_[0-9]{4}_[0-9]{2}$'
)
SELECT
    count(*) FILTER (WHERE partition_month >= date_trunc('month', now())::date) AS particiones_futuras_creadas,
    max(partition_month) AS ultima_particion_creada,
    (max(partition_month) < (date_trunc('month', now()) + interval '3 months')::date) AS alerta_horizonte_bajo
FROM parts;

COMMENT ON VIEW vw_partition_horizon_control IS
    'Alerta si quedan menos de 3 meses de particiones futuras creadas para transacciones.';

CREATE OR REPLACE VIEW vw_transacciones_indices_gin_control AS
SELECT
    indexrelid::regclass::text AS indice,
    indisvalid AS valido,
    indisready AS listo
FROM pg_index
WHERE indexrelid::regclass::text IN (
    'idx_transacciones_datos_gin',
    'idx_transacciones_riesgo_py_gin',
    'idx_transacciones_screening_gin',
    'idx_transacciones_reglas_gin'
)
ORDER BY indice;

COMMENT ON VIEW vw_transacciones_indices_gin_control IS
    'Control de validez de indices GIN principales sobre transacciones particionada.';

-- ----------------------------------------------------------------------------
-- 5. Documentacion de decisiones de diseno.
-- ----------------------------------------------------------------------------

COMMENT ON COLUMN auditoria_sistema.entidad_id IS
    'Identificador polimorfico de la entidad auditada, almacenado como texto porque las entidades referenciadas usan UUID o BIGINT.';

COMMENT ON TABLE sujeto_riesgo IS
    'Catalogo compartido de sujetos de riesgo. Intencionalmente sin empresa_id porque representa referencias regulatorias/globales.';

COMMENT ON TABLE lista_regulatoria IS
    'Catalogo compartido de listas regulatorias. Intencionalmente sin empresa_id.';

COMMENT ON TABLE pais_riesgo IS
    'Catalogo compartido de paises de riesgo. Intencionalmente sin empresa_id.';

COMMENT ON TABLE fuente_datos_riesgo IS
    'Catalogo compartido de fuentes de datos de riesgo. Intencionalmente sin empresa_id.';
