-- V36: Evidencia con archivo real (bytea) + unicidad de snapshot de cliente por alerta.
-- 1) Deduplica cliente_snapshot_alerta (los seeds R__ podian insertar duplicados al no haber
--    constraint unico, rompiendo findByAlertaId con NonUniqueResultException).
-- 2) Aplica UNIQUE(alerta_id), coherente con el mapeo JPA (unique=true) del historial.
-- 3) Agrega columnas de archivo binario a evidencia_alerta para carga real de evidencia.

WITH ranked AS (
    SELECT id,
           row_number() OVER (
               PARTITION BY alerta_id
               ORDER BY (CASE WHEN snapshot_json -> 'perfil' IS NOT NULL THEN 0 ELSE 1 END),
                        fecha_consulta DESC,
                        id DESC
           ) AS rn
    FROM cliente_snapshot_alerta
)
DELETE FROM cliente_snapshot_alerta c
USING ranked r
WHERE c.id = r.id AND r.rn > 1;

ALTER TABLE cliente_snapshot_alerta
    DROP CONSTRAINT IF EXISTS uq_cliente_snapshot_alerta_alerta;

ALTER TABLE cliente_snapshot_alerta
    ADD CONSTRAINT uq_cliente_snapshot_alerta_alerta UNIQUE (alerta_id);

ALTER TABLE evidencia_alerta ADD COLUMN IF NOT EXISTS contenido bytea;
ALTER TABLE evidencia_alerta ADD COLUMN IF NOT EXISTS contenido_nombre varchar(255);

COMMENT ON COLUMN evidencia_alerta.contenido IS 'Contenido binario del archivo de evidencia cargado (max. 10 MB).';
COMMENT ON COLUMN evidencia_alerta.contenido_nombre IS 'Nombre original del archivo cargado como evidencia.';
