-- V32: Parametrización de canales de alto riesgo
-- Agrega el flag alto_riesgo a canal_transaccion para reemplazar los literales
-- hardcodeados de riesgo-canal.drl por una configuración (editable por el cliente).

ALTER TABLE canal_transaccion ADD COLUMN IF NOT EXISTS alto_riesgo BOOLEAN NOT NULL DEFAULT FALSE;

-- Canales considerados de alto riesgo por defecto (operaciones FX, comercio exterior y remesas).
UPDATE canal_transaccion SET alto_riesgo = TRUE WHERE codigo IN ('CAMBIO', 'COMEX', 'REMESA', 'CRYPTO', 'CHEQUE');
