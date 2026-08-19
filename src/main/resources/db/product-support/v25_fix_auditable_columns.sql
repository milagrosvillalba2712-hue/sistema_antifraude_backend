-- ============================================================================
-- V25: Correccion de columnas AuditableEntity faltantes en solicitud_roles.
-- La tabla solicitud_roles (V24) no incluyo las columnas que AuditableEntity
-- exige via ddl-auto: validate.
-- ============================================================================

ALTER TABLE solicitud_roles
    ADD COLUMN IF NOT EXISTS usuario_creacion_id uuid REFERENCES usuarios(id),
    ADD COLUMN IF NOT EXISTS usuario_modificacion_id uuid REFERENCES usuarios(id);

-- Migrar datos de la columna antigua 'creado_por' si existe
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'solicitud_roles' AND column_name = 'creado_por') THEN
        UPDATE solicitud_roles SET usuario_creacion_id = creado_por WHERE usuario_creacion_id IS NULL;
        ALTER TABLE solicitud_roles DROP COLUMN IF EXISTS creado_por;
    END IF;
END $$;
