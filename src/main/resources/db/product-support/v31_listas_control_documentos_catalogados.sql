-- V31: completa listas internas con pais/tipo de documento para screening AML.
-- Permite distinguir una coincidencia de documento por jurisdiccion, por ejemplo
-- PY + CI_PY frente a PY + RUC_PY, y preparar reglas mas precisas.

ALTER TABLE elemento_lista_control_cliente
    ADD COLUMN IF NOT EXISTS pais_id bigint REFERENCES pais(id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE elemento_lista_control_cliente
    ADD COLUMN IF NOT EXISTS tipo_documento_id bigint REFERENCES tipo_documento(id) ON DELETE RESTRICT ON UPDATE CASCADE;

CREATE INDEX IF NOT EXISTS ix_elemento_lista_control_documento_catalogado
    ON elemento_lista_control_cliente (empresa_id, tipo_identificador, pais_id, tipo_documento_id, valor_normalizado, estado)
    WHERE tipo_identificador = 'DOCUMENTO';

COMMENT ON COLUMN elemento_lista_control_cliente.pais_id IS
    'Pais o jurisdiccion del identificador cargado en la lista interna. Relevante para documentos de identidad y tributarios.';

COMMENT ON COLUMN elemento_lista_control_cliente.tipo_documento_id IS
    'Tipo documental catalogado asociado al valor de lista. Permite diferenciar CI, RUC, DNI, CPF u otros documentos por pais.';
