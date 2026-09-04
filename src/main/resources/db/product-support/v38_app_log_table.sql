CREATE TABLE IF NOT EXISTS app_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id UUID REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    nivel VARCHAR(10) NOT NULL,
    logger VARCHAR(250),
    mensaje TEXT NOT NULL,
    ip_origen VARCHAR(100),
    metodo VARCHAR(20),
    endpoint VARCHAR(250),
    status_http INTEGER,
    trace_id VARCHAR(120),
    detalle_json JSONB,
    fecha TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE app_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_log FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_app_log ON app_log;
CREATE POLICY tenant_isolation_app_log ON app_log
    USING (
        empresa_id IS NULL
        OR empresa_id = nullif(current_setting('app.current_empresa_id', true), '')::uuid
    )
    WITH CHECK (
        empresa_id IS NULL
        OR empresa_id = nullif(current_setting('app.current_empresa_id', true), '')::uuid
    );

CREATE INDEX IF NOT EXISTS ix_app_log_empresa_fecha
    ON app_log (empresa_id, fecha DESC);
CREATE INDEX IF NOT EXISTS ix_app_log_empresa_nivel
    ON app_log (empresa_id, nivel, fecha DESC);
CREATE INDEX IF NOT EXISTS ix_app_log_empresa_logger
    ON app_log (empresa_id, logger, fecha DESC);

GRANT SELECT, INSERT, UPDATE, DELETE ON app_log TO regula_app;
GRANT SELECT ON app_log TO regula_readonly;

COMMENT ON TABLE app_log IS 'Registro de logs de aplicacion (niveles INFO, DEBUG, WARN, ERROR) persistidos por un appender de logging para su visualizacion en la consola de administracion. No reemplaza telemetria de APIs ni auditoria funcional.';
COMMENT ON COLUMN app_log.nivel IS 'Nivel del evento de logging: TRACE, DEBUG, INFO, WARN o ERROR.';
COMMENT ON COLUMN app_log.empresa_id IS 'Empresa activa al momento del evento. NULL cuando el log ocurre fuera de un contexto de request (p.ej. jobs de scheduler).';
COMMENT ON COLUMN app_log.detalle_json IS 'Detalle tecnico no sensible. No debe almacenar documentos, cuentas, tokens, API keys ni payloads con PII.';