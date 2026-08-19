CREATE TABLE IF NOT EXISTS admin_empresa_configuracion_local (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id UUID NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    tipo VARCHAR(30) NOT NULL,
    codigo VARCHAR(80) NOT NULL,
    nombre VARCHAR(140) NOT NULL,
    descripcion TEXT NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    editable BOOLEAN NOT NULL DEFAULT false,
    orden INTEGER NOT NULL DEFAULT 100,
    detalle_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    fecha_hora_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_hora_modificacion TIMESTAMPTZ,
    usuario_creacion_id UUID REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id UUID REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_admin_empresa_configuracion_tipo CHECK (tipo IN ('PARAMETRO', 'JOB')),
    CONSTRAINT uk_admin_empresa_configuracion UNIQUE (empresa_id, tipo, codigo)
);

ALTER TABLE admin_empresa_configuracion_local ENABLE ROW LEVEL SECURITY;
ALTER TABLE admin_empresa_configuracion_local FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_admin_empresa_configuracion_local ON admin_empresa_configuracion_local;
CREATE POLICY tenant_isolation_admin_empresa_configuracion_local ON admin_empresa_configuracion_local
    USING (empresa_id = nullif(current_setting('app.current_empresa_id', true), '')::uuid)
    WITH CHECK (empresa_id = nullif(current_setting('app.current_empresa_id', true), '')::uuid);

DROP TRIGGER IF EXISTS trg_audit_admin_empresa_configuracion_local ON admin_empresa_configuracion_local;
CREATE TRIGGER trg_audit_admin_empresa_configuracion_local
    BEFORE INSERT OR UPDATE ON admin_empresa_configuracion_local
    FOR EACH ROW EXECUTE FUNCTION fn_set_audit_fields();

CREATE INDEX IF NOT EXISTS ix_admin_empresa_configuracion_tipo
    ON admin_empresa_configuracion_local (empresa_id, tipo, estado, orden);

COMMENT ON TABLE admin_empresa_configuracion_local IS 'Configuracion visible y administrable para la consola Admin Empresa. Reemplaza listas hardcodeadas en backend.';
COMMENT ON COLUMN admin_empresa_configuracion_local.tipo IS 'PARAMETRO para configuraciones locales visibles; JOB para tareas programadas o sincronizaciones operativas.';
COMMENT ON COLUMN admin_empresa_configuracion_local.codigo IS 'Codigo estable usado por frontend/backend para identificar la configuracion.';
COMMENT ON COLUMN admin_empresa_configuracion_local.detalle_json IS 'Metadata no sensible: frecuencia, ultima ejecucion, endpoint, owner funcional o recomendaciones.';
