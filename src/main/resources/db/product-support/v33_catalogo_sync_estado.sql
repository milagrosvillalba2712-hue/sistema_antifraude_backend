-- V33: Estado de sincronizacion de catalogos desde el Control Plane
-- Permite al job CatalogoSyncJob persistir por codigo: la version y sha256
-- ya sincronizados, la tabla destino, el resultado y los conteos de
-- upsert/desactivacion, para reanudar sincronizaciones incrementales
-- (solo se re-descarga un catalogo cuando cambia su sha256).

CREATE TABLE IF NOT EXISTS catalogo_sync_estado (
    id BIGSERIAL PRIMARY KEY,
    empresa_id UUID NOT NULL,
    catalogo_codigo VARCHAR(80) NOT NULL,
    version VARCHAR(40),
    sha256 VARCHAR(128),
    tabla_destino VARCHAR(60),
    estado VARCHAR(30) NOT NULL,
    items_recibidos INTEGER NOT NULL DEFAULT 0,
    items_upserted INTEGER NOT NULL DEFAULT 0,
    items_desactivados INTEGER NOT NULL DEFAULT 0,
    mensaje TEXT,
    fecha_sync TIMESTAMPTZ,
    fecha_hora_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_hora_modificacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_catalogo_sync_estado_empresa_codigo UNIQUE (empresa_id, catalogo_codigo)
);

CREATE INDEX IF NOT EXISTS idx_catalogo_sync_estado_empresa
    ON catalogo_sync_estado (empresa_id);
