-- ============================================================================
-- V17 - Unifica la observabilidad de APIs en api_evento.
--
-- api_evento se convierte en la unica fuente de telemetria y auditoria tecnica
-- de APIs internas, externas y Control Plane. La tabla consultas_externas
-- duplicaba la telemetria EXTERNA (mismo evento escrito en ambas tablas por
-- ExternalAuditService), generando UNIONs y doble conteo en los dashboards.
--
-- Estrategia:
--   1. Agrega a api_evento las columnas funcionales que solo existian en
--      consultas_externas (documento_hash, intentos, resultado_funcional, estado).
--   2. Backfill: enriquece los api_evento EXTERNA existentes (misma
--      correlation_id) y materializa como api_evento las consultas externas
--      que no tenian contraparte de telemetria.
--   3. Elimina la tabla consultas_externas, sus politicas RLS e indices.
-- ============================================================================

ALTER TABLE api_evento
    ADD COLUMN IF NOT EXISTS documento_hash varchar(64),
    ADD COLUMN IF NOT EXISTS intentos integer,
    ADD COLUMN IF NOT EXISTS resultado_funcional varchar(40),
    ADD COLUMN IF NOT EXISTS estado varchar(40);

COMMENT ON COLUMN api_evento.documento_hash IS 'Hash SHA-256 del documento consultado en un proveedor externo. Reemplaza consultas_externas.documento_hash.';
COMMENT ON COLUMN api_evento.intentos IS 'Numero de intentos para completar la consulta externa. Reemplaza consultas_externas.intentos.';
COMMENT ON COLUMN api_evento.resultado_funcional IS 'Resultado funcional del proveedor (COINCIDENCIA / SIN_COINCIDENCIA). Reemplaza consultas_externas.resultado_funcional.';
COMMENT ON COLUMN api_evento.estado IS 'Estado funcional de la consulta externa (COMPLETADA, ERROR, etc). Reemplaza consultas_externas.estado.';

-- Enriquecer api_evento EXTERNA que ya tenian contraparte de telemetria.
UPDATE api_evento a
SET documento_hash       = c.documento_hash,
    intentos            = c.intentos,
    resultado_funcional = c.resultado_funcional,
    estado              = c.estado
FROM consultas_externas c
WHERE a.correlation_id = c.correlation_id
  AND a.origen = 'EXTERNA';

-- Materializar como api_evento las consultas externas sin contraparte.
INSERT INTO api_evento (
    empresa_id, usuario_id, origen, direccion, servicio, endpoint, metodo_http,
    status_http, codigo_error, mensaje, resultado, categoria_error, duracion_ms,
    correlation_id, request_id, ip_origen, user_agent, referencia_entidad,
    referencia_id, detalle_json, fecha_evento, documento_hash, intentos,
    resultado_funcional, estado
)
SELECT
    c.empresa_id,
    c.usuario_creacion_id,
    'EXTERNA',
    'SALIENTE',
    coalesce(c.proveedor, 'PROVEEDOR_EXTERNO'),
    c.tipo_consulta,
    'GET',
    c.status_http,
    NULL,
    c.mensaje_error,
    CASE WHEN c.status_http >= 400 OR c.categoria_error IS NOT NULL
         THEN 'ERROR' ELSE 'EXITOSO' END,
    c.categoria_error,
    c.duracion_ms,
    c.correlation_id,
    NULL,
    NULL,
    NULL,
    'consultas_externas',
    c.correlation_id,
    jsonb_build_object('migradoV17', true),
    c.fecha_consulta,
    c.documento_hash,
    c.intentos,
    c.resultado_funcional,
    c.estado
FROM consultas_externas c
WHERE NOT EXISTS (
    SELECT 1 FROM api_evento a WHERE a.correlation_id = c.correlation_id
);

-- Eliminar politicas, indices y la tabla legacy.
DROP POLICY IF EXISTS tenant_isolation_consultas_externas ON consultas_externas;
DROP INDEX IF EXISTS idx_consulta_externa_correlation;
DROP INDEX IF EXISTS idx_consulta_externa_documento_hash;
DROP TABLE IF EXISTS consultas_externas;