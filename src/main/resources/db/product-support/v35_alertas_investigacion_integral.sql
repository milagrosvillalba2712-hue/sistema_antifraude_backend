-- V35: Datos operativos para investigacion integral de alertas.
-- Agrega entidad financiera origen/destino en transacciones y persistencia completa
-- de hallazgos para separar reglas disparadas de evidencias regulatorias/de control.

ALTER TABLE transacciones ADD COLUMN IF NOT EXISTS entidad_origen_tipo varchar(40);
ALTER TABLE transacciones ADD COLUMN IF NOT EXISTS entidad_origen_codigo varchar(80);
ALTER TABLE transacciones ADD COLUMN IF NOT EXISTS entidad_origen_nombre varchar(180);
ALTER TABLE transacciones ADD COLUMN IF NOT EXISTS entidad_destino_tipo varchar(40);
ALTER TABLE transacciones ADD COLUMN IF NOT EXISTS entidad_destino_codigo varchar(80);
ALTER TABLE transacciones ADD COLUMN IF NOT EXISTS entidad_destino_nombre varchar(180);
ALTER TABLE transacciones ADD COLUMN IF NOT EXISTS referencia_externa varchar(120);

COMMENT ON COLUMN transacciones.entidad_origen_tipo IS 'Tipo de entidad financiera origen de la operacion: BANCO, FINANCIERA, COOPERATIVA, EMPE, REMESADORA, PROCESADORA, SUCURSAL, ATM u OTRO.';
COMMENT ON COLUMN transacciones.entidad_origen_codigo IS 'Codigo operacional o externo de la entidad origen, usado para trazabilidad y conciliacion.';
COMMENT ON COLUMN transacciones.entidad_origen_nombre IS 'Nombre visible de la entidad financiera origen informada por el canal transaccional.';
COMMENT ON COLUMN transacciones.entidad_destino_tipo IS 'Tipo de entidad financiera destino de la operacion: BANCO, FINANCIERA, COOPERATIVA, EMPE, REMESADORA, PROCESADORA, SUCURSAL, ATM u OTRO.';
COMMENT ON COLUMN transacciones.entidad_destino_codigo IS 'Codigo operacional o externo de la entidad destino, usado para trazabilidad y conciliacion.';
COMMENT ON COLUMN transacciones.entidad_destino_nombre IS 'Nombre visible de la entidad financiera destino informada por el canal transaccional.';
COMMENT ON COLUMN transacciones.referencia_externa IS 'Referencia de camara, proveedor, comercio, remesadora o integracion externa relacionada con la transaccion.';

CREATE INDEX IF NOT EXISTS ix_transacciones_entidad_origen
    ON transacciones (empresa_id, entidad_origen_tipo, entidad_origen_codigo, fecha_transaccion DESC);

CREATE INDEX IF NOT EXISTS ix_transacciones_entidad_destino
    ON transacciones (empresa_id, entidad_destino_tipo, entidad_destino_codigo, fecha_transaccion DESC);

ALTER TABLE hallazgo_alerta ADD COLUMN IF NOT EXISTS titulo varchar(180);
ALTER TABLE hallazgo_alerta ADD COLUMN IF NOT EXISTS fuente varchar(80);

UPDATE hallazgo_alerta
SET titulo = COALESCE(NULLIF(titulo, ''), initcap(replace(tipo_hallazgo, '_', ' ')))
WHERE titulo IS NULL OR titulo = '';

UPDATE hallazgo_alerta
SET fuente = COALESCE(NULLIF(fuente, ''), 'MOTOR_REGLAS')
WHERE fuente IS NULL OR fuente = '';

COMMENT ON COLUMN hallazgo_alerta.titulo IS 'Titulo funcional del hallazgo mostrado al usuario durante la investigacion.';
COMMENT ON COLUMN hallazgo_alerta.fuente IS 'Origen funcional del hallazgo: MOTOR_REGLAS, LISTA_CONTROL, PAIS_RIESGO, SCREENING, TRANSACCION u otra fuente controlada.';

CREATE INDEX IF NOT EXISTS ix_hallazgo_alerta_tipo_fuente
    ON hallazgo_alerta (empresa_id, tipo_hallazgo, fuente, fecha_hora_creacion DESC);
