-- Fase 4 (plan MEJORA_ESTRUCTURAL · punto D/#5): índices de FK.
--
-- Postgres no crea índices automáticamente en columnas FK. Estas tablas
-- referencian caso/alerta/sujeto_riesgo y no tenían índice de soporte,
-- por lo que los JOIN y los borrados en cascada hacían Seq Scan.
--
-- Sin CREATE INDEX CONCURRENTLY: esta migración corre dentro de la transacción
-- que Flyway abre por defecto. Se acepta el lock de instalador/actualización
-- (sin tráfico concurrente en el momento del upgrade); IF NOT EXISTS la hace
-- idempotente y segura de re-aplicar.

-- Grupo caso_id.
CREATE INDEX IF NOT EXISTS ix_actuacion_caso ON actuacion (empresa_id, caso_id);
CREATE INDEX IF NOT EXISTS ix_aprobacion_supervisor_caso ON aprobacion_supervisor (empresa_id, caso_id);
CREATE INDEX IF NOT EXISTS ix_comentario_caso_caso ON comentario_caso (empresa_id, caso_id);
CREATE INDEX IF NOT EXISTS ix_decision_caso_caso ON decision_caso (empresa_id, caso_id);
CREATE INDEX IF NOT EXISTS ix_evidencia_caso ON evidencia (empresa_id, caso_id);
CREATE INDEX IF NOT EXISTS ix_historial_estado_caso ON historial_estado_caso (empresa_id, caso_id);

-- Grupo alerta_id.
CREATE INDEX IF NOT EXISTS ix_aprobacion_supervisor_alerta ON aprobacion_supervisor (empresa_id, alerta_id);
CREATE INDEX IF NOT EXISTS ix_caso_alerta_alerta ON caso_alerta (empresa_id, alerta_id);
CREATE INDEX IF NOT EXISTS ix_cliente_snapshot_alerta_alerta ON cliente_snapshot_alerta (empresa_id, alerta_id);
CREATE INDEX IF NOT EXISTS ix_coincidencia_lista_alerta ON coincidencia_lista_alerta (empresa_id, alerta_id);
CREATE INDEX IF NOT EXISTS ix_consulta_kyc_alerta ON consulta_kyc_alerta (empresa_id, alerta_id);
CREATE INDEX IF NOT EXISTS ix_resolucion_alerta ON resolucion_alerta (empresa_id, alerta_id);
CREATE INDEX IF NOT EXISTS ix_aprobacion_supervisor_resolucion ON aprobacion_supervisor (empresa_id, resolucion_alerta_id);

-- Grupo sujeto_riesgo_id (tablas globales de listas, sin tenant).
CREATE INDEX IF NOT EXISTS ix_coincidencia_lista_sujeto ON coincidencia_lista_alerta (sujeto_riesgo_id);
CREATE INDEX IF NOT EXISTS ix_sujeto_riesgo_alias_sujeto ON sujeto_riesgo_alias (sujeto_riesgo_id);
CREATE INDEX IF NOT EXISTS ix_sujeto_riesgo_documento_sujeto ON sujeto_riesgo_documento (sujeto_riesgo_id);
CREATE INDEX IF NOT EXISTS ix_sujeto_riesgo_relacion_origen ON sujeto_riesgo_relacion (sujeto_origen_id);
CREATE INDEX IF NOT EXISTS ix_sujeto_riesgo_relacion_destino ON sujeto_riesgo_relacion (sujeto_destino_id);
