ALTER TABLE consultas_externas
    DROP COLUMN IF EXISTS identificador_documento,
    ADD COLUMN IF NOT EXISTS proveedor varchar(40),
    ADD COLUMN IF NOT EXISTS documento_hash varchar(64),
    ADD COLUMN IF NOT EXISTS correlation_id varchar(120),
    ADD COLUMN IF NOT EXISTS status_http integer,
    ADD COLUMN IF NOT EXISTS duracion_ms bigint,
    ADD COLUMN IF NOT EXISTS intentos integer NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS resultado_funcional varchar(40),
    ADD COLUMN IF NOT EXISTS categoria_error varchar(60);

CREATE INDEX IF NOT EXISTS idx_consulta_externa_correlation
    ON consultas_externas(correlation_id);
CREATE INDEX IF NOT EXISTS idx_consulta_externa_documento_hash
    ON consultas_externas(documento_hash);
