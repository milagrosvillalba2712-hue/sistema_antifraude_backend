-- V34: Parametros de scoring del motor Drools, configurables por el cliente.
-- Des-hardcodea los puntajes de las reglas de dominio, la ventana de horario
-- no habitual y los umbrales de nivel / alerta automatica. Se siembran con los
-- valores que estaban hardcodeados para no alterar el comportamiento actual.

CREATE TABLE IF NOT EXISTS configuracion_drools (
    clave VARCHAR(80) PRIMARY KEY,
    valor VARCHAR(255) NOT NULL,
    descripcion VARCHAR(255),
    editable BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO configuracion_drools (clave, valor, descripcion, editable) VALUES
('PEP_SCORE', '40', 'Puntos por PEP detectado', TRUE),
('OBSERVADO_SCORE', '60', 'Puntos por cliente observado detectado', TRUE),
('HORARIO_RIESGO_EMPIEZA', '23', 'Hora (>=) desde la que el horario se considera no habitual', TRUE),
('HORARIO_RIESGO_TERMINA', '5', 'Hora (<) hasta la que el horario se considera no habitual', TRUE),
('HORARIO_SCORE', '15', 'Puntos por horario no habitual', TRUE),
('PAIS_INTERNACIONAL_SCORE', '20', 'Puntos por transaccion internacional', TRUE),
('PAIS_ALTO_RIESGO_SCORE', '15', 'Puntos por pais de origen de alto riesgo', TRUE),
('PAIS_DESTINO_ALTO_RIESGO_SCORE', '15', 'Puntos por pais destino de alto riesgo', TRUE),
('PAIS_DESTINO_DISTINTO_SCORE', '10', 'Puntos por pais destino distinto al origen', TRUE),
('UMBRAL_CRITICO', '70', 'Score minimo para nivel CRITICO y alerta automatica', TRUE),
('UMBRAL_ALTO', '50', 'Score minimo para nivel ALTO', TRUE),
('UMBRAL_MEDIO', '30', 'Score minimo para nivel MEDIO', TRUE)
ON CONFLICT (clave) DO NOTHING;
