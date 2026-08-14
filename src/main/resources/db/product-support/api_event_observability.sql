CREATE TABLE IF NOT EXISTS api_evento (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id UUID REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_id UUID REFERENCES usuarios(id) ON DELETE SET NULL ON UPDATE CASCADE,
    origen VARCHAR(30) NOT NULL,
    direccion VARCHAR(20) NOT NULL,
    servicio VARCHAR(100) NOT NULL,
    endpoint VARCHAR(250),
    metodo_http VARCHAR(12),
    status_http INTEGER,
    codigo_error VARCHAR(100),
    mensaje TEXT,
    resultado VARCHAR(20) NOT NULL,
    categoria_error VARCHAR(80),
    duracion_ms BIGINT,
    correlation_id VARCHAR(120),
    request_id VARCHAR(120),
    ip_origen VARCHAR(100),
    user_agent TEXT,
    referencia_entidad VARCHAR(100),
    referencia_id VARCHAR(120),
    detalle_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    fecha_evento TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE api_evento ENABLE ROW LEVEL SECURITY;
ALTER TABLE api_evento FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_api_evento ON api_evento;
CREATE POLICY tenant_isolation_api_evento ON api_evento
    USING (
        empresa_id IS NULL
        OR empresa_id = current_setting('app.current_empresa_id', true)::uuid
    )
    WITH CHECK (
        empresa_id IS NULL
        OR empresa_id = current_setting('app.current_empresa_id', true)::uuid
    );

CREATE INDEX IF NOT EXISTS ix_api_evento_empresa_fecha
    ON api_evento (empresa_id, fecha_evento DESC);
CREATE INDEX IF NOT EXISTS ix_api_evento_empresa_resultado
    ON api_evento (empresa_id, resultado, fecha_evento DESC);
CREATE INDEX IF NOT EXISTS ix_api_evento_empresa_origen
    ON api_evento (empresa_id, origen, fecha_evento DESC);
CREATE INDEX IF NOT EXISTS ix_api_evento_endpoint
    ON api_evento (empresa_id, servicio, endpoint, fecha_evento DESC);
CREATE INDEX IF NOT EXISTS ix_api_evento_correlation
    ON api_evento (correlation_id);

COMMENT ON TABLE api_evento IS 'Telemetria tecnica de APIs internas, externas y Control Plane. Alimenta dashboards de latencia, trafico, errores y disponibilidad sin mezclar auditoria funcional.';
COMMENT ON COLUMN api_evento.origen IS 'INTERNA, EXTERNA o CONTROL_PLANE segun el origen del evento tecnico.';
COMMENT ON COLUMN api_evento.direccion IS 'ENTRANTE para requests recibidos por el backend; SALIENTE para llamadas a proveedores o Control Plane.';
COMMENT ON COLUMN api_evento.resultado IS 'EXITOSO o ERROR. No reemplaza auditoria_sistema; solo registra observabilidad tecnica.';
COMMENT ON COLUMN api_evento.detalle_json IS 'Detalle tecnico no sensible. No debe almacenar documentos, cuentas, tokens, API keys ni payloads con PII.';
