-- V27: endurecimiento RLS para tablas tenant-scoped de licenciamiento local.
-- Las politicas usan NULLIF para evitar errores 22P02 cuando el contexto RLS
-- no esta inicializado y PostgreSQL retorna cadena vacia.

ALTER TABLE instalacion_local ENABLE ROW LEVEL SECURITY;
ALTER TABLE instalacion_local FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON instalacion_local;
CREATE POLICY tenant_isolation ON instalacion_local
    FOR ALL
    USING (empresa_id = NULLIF(current_setting('app.current_empresa_id', true), '')::uuid)
    WITH CHECK (empresa_id = NULLIF(current_setting('app.current_empresa_id', true), '')::uuid);

ALTER TABLE roles_adquiridos ENABLE ROW LEVEL SECURITY;
ALTER TABLE roles_adquiridos FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON roles_adquiridos;
CREATE POLICY tenant_isolation ON roles_adquiridos
    FOR ALL
    USING (empresa_id = NULLIF(current_setting('app.current_empresa_id', true), '')::uuid)
    WITH CHECK (empresa_id = NULLIF(current_setting('app.current_empresa_id', true), '')::uuid);

ALTER TABLE solicitud_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE solicitud_roles FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON solicitud_roles;
CREATE POLICY tenant_isolation ON solicitud_roles
    FOR ALL
    USING (empresa_id = NULLIF(current_setting('app.current_empresa_id', true), '')::uuid)
    WITH CHECK (empresa_id = NULLIF(current_setting('app.current_empresa_id', true), '')::uuid);

ALTER TABLE licencia_local ENABLE ROW LEVEL SECURITY;
ALTER TABLE licencia_local FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON licencia_local;
CREATE POLICY tenant_isolation ON licencia_local
    FOR ALL
    USING (instalacion_id IN (
        SELECT id FROM instalacion_local
        WHERE empresa_id = NULLIF(current_setting('app.current_empresa_id', true), '')::uuid
    ))
    WITH CHECK (instalacion_id IN (
        SELECT id FROM instalacion_local
        WHERE empresa_id = NULLIF(current_setting('app.current_empresa_id', true), '')::uuid
    ));

ALTER TABLE evento_licencia_local ENABLE ROW LEVEL SECURITY;
ALTER TABLE evento_licencia_local FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON evento_licencia_local;
CREATE POLICY tenant_isolation ON evento_licencia_local
    FOR ALL
    USING (instalacion_id IN (
        SELECT id FROM instalacion_local
        WHERE empresa_id = NULLIF(current_setting('app.current_empresa_id', true), '')::uuid
    ))
    WITH CHECK (instalacion_id IN (
        SELECT id FROM instalacion_local
        WHERE empresa_id = NULLIF(current_setting('app.current_empresa_id', true), '')::uuid
    ));

