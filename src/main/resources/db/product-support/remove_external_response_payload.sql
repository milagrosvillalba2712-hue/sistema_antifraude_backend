-- V6 sanitizo el identificador documental y agrego metadatos operativos.
-- V7 elimina el payload externo legacy; no se modifica V6 porque ya fue publicada.
ALTER TABLE consultas_externas
    DROP COLUMN IF EXISTS respuesta_json;
