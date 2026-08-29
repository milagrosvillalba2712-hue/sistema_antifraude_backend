ALTER TABLE transacciones ADD COLUMN IF NOT EXISTS remitente_nombre_completo varchar(220);
ALTER TABLE transacciones ADD COLUMN IF NOT EXISTS beneficiario_nombre_completo varchar(220);

ALTER TABLE transacciones DISABLE ROW LEVEL SECURITY;

UPDATE transacciones
SET remitente_nombre_completo = COALESCE(NULLIF(TRIM(nombre_remitente), ''), 'SIN NOMBRE'),
    beneficiario_nombre_completo = COALESCE(NULLIF(TRIM(nombre_beneficiario), ''), 'SIN NOMBRE')
WHERE remitente_nombre_completo IS NULL
   OR beneficiario_nombre_completo IS NULL;

ALTER TABLE transacciones ENABLE ROW LEVEL SECURITY;
ALTER TABLE transacciones FORCE ROW LEVEL SECURITY;

ALTER TABLE transacciones ALTER COLUMN remitente_nombre_completo SET NOT NULL;
ALTER TABLE transacciones ALTER COLUMN beneficiario_nombre_completo SET NOT NULL;

CREATE INDEX IF NOT EXISTS ix_tx_remitente_nombre_screening
    ON transacciones (empresa_id, remitente_nombre_completo, fecha_transaccion DESC);
CREATE INDEX IF NOT EXISTS ix_tx_beneficiario_nombre_screening
    ON transacciones (empresa_id, beneficiario_nombre_completo, fecha_transaccion DESC);

COMMENT ON COLUMN transacciones.remitente_nombre_completo IS 'Identidad completa del remitente: nombre y apellido (persona fisica) o razon social (persona juridica). Fuente del screening por nombre contra listas PEP/OFAC/control.';
COMMENT ON COLUMN transacciones.beneficiario_nombre_completo IS 'Identidad completa del beneficiario: nombre y apellido (persona fisica) o razon social (persona juridica). Fuente del screening por nombre contra listas PEP/OFAC/control.';
