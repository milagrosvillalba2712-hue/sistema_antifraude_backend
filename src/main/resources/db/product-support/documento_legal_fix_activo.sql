-- V23: Agrega columna 'activo' faltante en usuario_documento_legal
-- La entidad Java extiende TenantAwareEntity → BaseEntity que incluye 'activo',
-- pero la tabla V21 fue creada sin esa columna.

ALTER TABLE usuario_documento_legal
    ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;

COMMENT ON COLUMN usuario_documento_legal.activo IS 'Soft-delete flag heredado de BaseEntity.';
