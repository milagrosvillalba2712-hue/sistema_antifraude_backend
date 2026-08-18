-- V21: Tablas para Términos y Condiciones / Política de Privacidad
-- Cumple Ley 7593/2025 (Protección de Datos Personales) Art. 6 y Art. 4.f

-- ============================================================
-- 1. documento_legal — documentos versionados (global, sin RLS)
-- ============================================================
CREATE TABLE IF NOT EXISTS documento_legal (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo                  VARCHAR(20) NOT NULL,
    version               INTEGER NOT NULL,
    titulo                VARCHAR(255) NOT NULL,
    contenido             TEXT NOT NULL,
    url_documento         VARCHAR(500),
    activo                BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion        TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_publicacion     TIMESTAMPTZ,
    fecha_hora_creacion   TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_hora_modificacion TIMESTAMPTZ,
    usuario_creacion_id   UUID REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id UUID REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_documento_legal_tipo CHECK (tipo IN ('TERMINOS', 'POLITICA_PRIVACIDAD')),
    CONSTRAINT uk_documento_legal_version UNIQUE (tipo, version)
);

-- Sin RLS: documento global visto por todas las empresas

DROP TRIGGER IF EXISTS trg_audit_documento_legal ON documento_legal;
CREATE TRIGGER trg_audit_documento_legal
    BEFORE INSERT OR UPDATE ON documento_legal
    FOR EACH ROW EXECUTE FUNCTION fn_set_audit_fields();

CREATE INDEX IF NOT EXISTS ix_documento_legal_tipo_version
    ON documento_legal (tipo, version, activo);

COMMENT ON TABLE documento_legal IS 'Documentos legales versionados: Términos y Condiciones y Política de Privacidad. Documento global sin aislamiento por empresa.';
COMMENT ON COLUMN documento_legal.tipo IS 'TERMINOS para Términos y Condiciones; POLITICA_PRIVACIDAD para Política de Privacidad.';
COMMENT ON COLUMN documento_legal.version IS 'Número de versión incremental por tipo. Único por tipo.';
COMMENT ON COLUMN documento_legal.contenido IS 'Contenido completo del documento legal en texto plano o HTML.';
COMMENT ON COLUMN documento_legal.url_documento IS 'URL alternativa al contenido embebido (opcional).';

-- ============================================================
-- 2. usuario_documento_legal — aceptación por usuario (con RLS)
-- ============================================================
CREATE TABLE IF NOT EXISTS usuario_documento_legal (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id            UUID NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_id            UUID NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    documento_legal_id    BIGINT NOT NULL REFERENCES documento_legal(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    acepto                BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_aceptacion      TIMESTAMPTZ,
    ip_address            VARCHAR(45),
    user_agent            TEXT,
    fecha_hora_creacion   TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_hora_modificacion TIMESTAMPTZ,
    usuario_creacion_id   UUID REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id UUID REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT uk_usuario_documento_legal UNIQUE (usuario_id, documento_legal_id)
);

ALTER TABLE usuario_documento_legal ENABLE ROW LEVEL SECURITY;
ALTER TABLE usuario_documento_legal FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_usuario_documento_legal ON usuario_documento_legal;
CREATE POLICY tenant_isolation_usuario_documento_legal ON usuario_documento_legal
    USING (empresa_id = nullif(current_setting('app.current_empresa_id', true), '')::uuid)
    WITH CHECK (empresa_id = nullif(current_setting('app.current_empresa_id', true), '')::uuid);

DROP TRIGGER IF EXISTS trg_audit_usuario_documento_legal ON usuario_documento_legal;
CREATE TRIGGER trg_audit_usuario_documento_legal
    BEFORE INSERT OR UPDATE ON usuario_documento_legal
    FOR EACH ROW EXECUTE FUNCTION fn_set_audit_fields();

CREATE INDEX IF NOT EXISTS ix_usuario_documento_legal_empresa
    ON usuario_documento_legal (empresa_id, usuario_id, acepto);

CREATE INDEX IF NOT EXISTS ix_usuario_documento_legal_documento
    ON usuario_documento_legal (documento_legal_id, usuario_id);

COMMENT ON TABLE usuario_documento_legal IS 'Registro de aceptación de documentos legales por usuario. Aislado por empresa (RLS).';
COMMENT ON COLUMN usuario_documento_legal.acepto IS 'TRUE si el usuario aceptó el documento; FALSE si aún no lo ha aceptado.';
COMMENT ON COLUMN usuario_documento_legal.fecha_aceptacion IS 'Timestamp de cuando el usuario aceptó el documento.';
