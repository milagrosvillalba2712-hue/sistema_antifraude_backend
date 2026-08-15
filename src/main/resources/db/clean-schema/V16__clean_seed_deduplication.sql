-- ============================================================================
-- Regula AML - Seed deduplication V16
-- Limpieza idempotente de duplicados exactos generados por reejecucion de seeds.
-- ============================================================================

WITH ranked AS (
    SELECT id, row_number() OVER (
        PARTITION BY empresa_id, caso_id, resolucion_alerta_id, decision, descripcion
        ORDER BY id
    ) AS rn
    FROM decision_caso
)
DELETE FROM decision_caso d
USING ranked r
WHERE d.id = r.id
  AND r.rn > 1;

WITH ranked AS (
    SELECT id, row_number() OVER (
        PARTITION BY empresa_id, alerta_id, resolucion_alerta_id, supervisor_id, decision, observacion, motivo_rechazo, faltantes
        ORDER BY id
    ) AS rn
    FROM aprobacion_supervisor
)
DELETE FROM aprobacion_supervisor a
USING ranked r
WHERE a.id = r.id
  AND r.rn > 1;

WITH duplicate_resolutions AS (
    SELECT id
    FROM (
        SELECT id, row_number() OVER (
            PARTITION BY empresa_id, alerta_id, analista_id, resultado, conclusion, justificacion
            ORDER BY id
        ) AS rn
        FROM resolucion_alerta
    ) x
    WHERE rn > 1
)
DELETE FROM aprobacion_supervisor a
USING duplicate_resolutions d
WHERE a.resolucion_alerta_id = d.id;

WITH duplicate_resolutions AS (
    SELECT id
    FROM (
        SELECT id, row_number() OVER (
            PARTITION BY empresa_id, alerta_id, analista_id, resultado, conclusion, justificacion
            ORDER BY id
        ) AS rn
        FROM resolucion_alerta
    ) x
    WHERE rn > 1
)
DELETE FROM decision_caso dc
USING duplicate_resolutions d
WHERE dc.resolucion_alerta_id = d.id;

WITH ranked AS (
    SELECT id, row_number() OVER (
        PARTITION BY empresa_id, alerta_id, analista_id, resultado, conclusion, justificacion
        ORDER BY id
    ) AS rn
    FROM resolucion_alerta
)
DELETE FROM resolucion_alerta r0
USING ranked r
WHERE r0.id = r.id
  AND r.rn > 1;

WITH ranked AS (
    SELECT id, row_number() OVER (
        PARTITION BY empresa_id, alerta_id, transaccion_id, fecha_transaccion, tipo_hallazgo, descripcion
        ORDER BY id
    ) AS rn
    FROM hallazgo_alerta
)
DELETE FROM hallazgo_alerta h
USING ranked r
WHERE h.id = r.id
  AND r.rn > 1;

WITH ranked AS (
    SELECT id, row_number() OVER (
        PARTITION BY empresa_id, alerta_id, sujeto_riesgo_id, lista_regulatoria_id, tipo_coincidencia
        ORDER BY id
    ) AS rn
    FROM coincidencia_lista_alerta
)
DELETE FROM coincidencia_lista_alerta c
USING ranked r
WHERE c.id = r.id
  AND r.rn > 1;

WITH ranked AS (
    SELECT id, row_number() OVER (
        PARTITION BY empresa_id, transaccion_id, fecha_transaccion, fuente
        ORDER BY id
    ) AS rn
    FROM transaccion_detalle_snapshot
)
DELETE FROM transaccion_detalle_snapshot t
USING ranked r
WHERE t.id = r.id
  AND r.rn > 1;

WITH ranked AS (
    SELECT id, row_number() OVER (
        PARTITION BY empresa_id, alerta_id, persona_id, fuente
        ORDER BY id
    ) AS rn
    FROM cliente_snapshot_alerta
)
DELETE FROM cliente_snapshot_alerta c
USING ranked r
WHERE c.id = r.id
  AND r.rn > 1;

WITH ranked AS (
    SELECT id, row_number() OVER (
        PARTITION BY empresa_id, alerta_id, proveedor, estado, mensaje
        ORDER BY id
    ) AS rn
    FROM consulta_kyc_alerta
)
DELETE FROM consulta_kyc_alerta c
USING ranked r
WHERE c.id = r.id
  AND r.rn > 1;

WITH ranked AS (
    SELECT id, row_number() OVER (
        PARTITION BY empresa_id, alerta_id, usuario_nuevo_id, tipo, motivo, observacion
        ORDER BY id
    ) AS rn
    FROM historial_asignacion
)
DELETE FROM historial_asignacion h
USING ranked r
WHERE h.id = r.id
  AND r.rn > 1;

WITH ranked AS (
    SELECT id, row_number() OVER (
        PARTITION BY empresa_id, caso_id, usuario_id, tipo_actuacion, descripcion
        ORDER BY id
    ) AS rn
    FROM actuacion
)
DELETE FROM actuacion a
USING ranked r
WHERE a.id = r.id
  AND r.rn > 1;

WITH ranked AS (
    SELECT id, row_number() OVER (
        PARTITION BY empresa_id, caso_id, usuario_id, comentario, visibilidad
        ORDER BY id
    ) AS rn
    FROM comentario_caso
)
DELETE FROM comentario_caso c
USING ranked r
WHERE c.id = r.id
  AND r.rn > 1;

WITH ranked AS (
    SELECT id, row_number() OVER (
        PARTITION BY empresa_id, caso_id, nombre, descripcion, referencia_archivo
        ORDER BY id
    ) AS rn
    FROM evidencia
)
DELETE FROM evidencia e
USING ranked r
WHERE e.id = r.id
  AND r.rn > 1;

WITH ranked AS (
    SELECT id, row_number() OVER (
        PARTITION BY empresa_id, caso_id, estado_anterior, estado_nuevo, motivo, usuario_id
        ORDER BY id
    ) AS rn
    FROM historial_estado_caso
)
DELETE FROM historial_estado_caso h
USING ranked r
WHERE h.id = r.id
  AND r.rn > 1;

WITH ranked AS (
    SELECT id, row_number() OVER (
        PARTITION BY empresa_id, usuario_id, accion, descripcion, entidad_afectada, entidad_id
        ORDER BY id
    ) AS rn
    FROM auditoria_sistema
    WHERE accion LIKE 'SEED_%'
)
DELETE FROM auditoria_sistema a
USING ranked r
WHERE a.id = r.id
  AND r.rn > 1;
