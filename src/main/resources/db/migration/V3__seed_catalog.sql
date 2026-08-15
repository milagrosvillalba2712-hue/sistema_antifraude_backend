-- Seed: datos de catalogo y configuracion para testing
-- Se ejecuta via SqlSeedRunner despues de que Hibernate crea el schema
-- Todos los INSERTs tienen WHERE NOT EXISTS para ser idempotentes

-- ============================================================
-- Fix: columnas de horario cambiaron de numeric a time
-- ============================================================
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'perfil_cliente'
    AND column_name = 'horario_habitual_desde'
    AND data_type = 'numeric'
  ) THEN
    ALTER TABLE perfil_cliente
      ALTER COLUMN horario_habitual_desde TYPE time USING horario_habitual_desde::text::time,
      ALTER COLUMN horario_habitual_hasta TYPE time USING horario_habitual_hasta::text::time;
  END IF;
END $$;

-- ============================================================
-- 1. nivel_riesgo
-- ============================================================
INSERT INTO nivel_riesgo (codigo, nombre, orden, activo)
SELECT 'BAJO', 'Bajo', 1, true
WHERE NOT EXISTS (SELECT 1 FROM nivel_riesgo WHERE codigo = 'BAJO');

INSERT INTO nivel_riesgo (codigo, nombre, orden, activo)
SELECT 'MEDIO', 'Medio', 2, true
WHERE NOT EXISTS (SELECT 1 FROM nivel_riesgo WHERE codigo = 'MEDIO');

INSERT INTO nivel_riesgo (codigo, nombre, orden, activo)
SELECT 'ALTO', 'Alto', 3, true
WHERE NOT EXISTS (SELECT 1 FROM nivel_riesgo WHERE codigo = 'ALTO');

INSERT INTO nivel_riesgo (codigo, nombre, orden, activo)
SELECT 'CRITICO', 'Critico', 4, true
WHERE NOT EXISTS (SELECT 1 FROM nivel_riesgo WHERE codigo = 'CRITICO');

-- ============================================================
-- 2. pais
-- ============================================================
INSERT INTO pais (codigo_iso, nombre, continente, activo)
SELECT 'AR', 'Argentina', 'Sudamerica', true
WHERE NOT EXISTS (SELECT 1 FROM pais WHERE codigo_iso = 'AR');

INSERT INTO pais (codigo_iso, nombre, continente, activo)
SELECT 'BR', 'Brasil', 'Sudamerica', true
WHERE NOT EXISTS (SELECT 1 FROM pais WHERE codigo_iso = 'BR');

INSERT INTO pais (codigo_iso, nombre, continente, activo)
SELECT 'CL', 'Chile', 'Sudamerica', true
WHERE NOT EXISTS (SELECT 1 FROM pais WHERE codigo_iso = 'CL');

INSERT INTO pais (codigo_iso, nombre, continente, activo)
SELECT 'UY', 'Uruguay', 'Sudamerica', true
WHERE NOT EXISTS (SELECT 1 FROM pais WHERE codigo_iso = 'UY');

INSERT INTO pais (codigo_iso, nombre, continente, activo)
SELECT 'PY', 'Paraguay', 'Sudamerica', true
WHERE NOT EXISTS (SELECT 1 FROM pais WHERE codigo_iso = 'PY');

INSERT INTO pais (codigo_iso, nombre, continente, activo)
SELECT 'US', 'Estados Unidos', 'Norteamerica', true
WHERE NOT EXISTS (SELECT 1 FROM pais WHERE codigo_iso = 'US');

INSERT INTO pais (codigo_iso, nombre, continente, activo)
SELECT 'ES', 'España', 'Europa', true
WHERE NOT EXISTS (SELECT 1 FROM pais WHERE codigo_iso = 'ES');

INSERT INTO pais (codigo_iso, nombre, continente, activo)
SELECT 'MX', 'Mexico', 'Norteamerica', true
WHERE NOT EXISTS (SELECT 1 FROM pais WHERE codigo_iso = 'MX');

INSERT INTO pais (codigo_iso, nombre, continente, activo)
SELECT 'CO', 'Colombia', 'Sudamerica', true
WHERE NOT EXISTS (SELECT 1 FROM pais WHERE codigo_iso = 'CO');

INSERT INTO pais (codigo_iso, nombre, continente, activo)
SELECT 'PE', 'Peru', 'Sudamerica', true
WHERE NOT EXISTS (SELECT 1 FROM pais WHERE codigo_iso = 'PE');

-- ============================================================
-- 3. moneda
-- ============================================================
INSERT INTO moneda (codigo_iso, nombre, activo)
SELECT 'ARS', 'Peso Argentino', true
WHERE NOT EXISTS (SELECT 1 FROM moneda WHERE codigo_iso = 'ARS');

INSERT INTO moneda (codigo_iso, nombre, activo)
SELECT 'USD', 'Dolar Estadounidense', true
WHERE NOT EXISTS (SELECT 1 FROM moneda WHERE codigo_iso = 'USD');

INSERT INTO moneda (codigo_iso, nombre, activo)
SELECT 'EUR', 'Euro', true
WHERE NOT EXISTS (SELECT 1 FROM moneda WHERE codigo_iso = 'EUR');

INSERT INTO moneda (codigo_iso, nombre, activo)
SELECT 'BRL', 'Real Brasileño', true
WHERE NOT EXISTS (SELECT 1 FROM moneda WHERE codigo_iso = 'BRL');

INSERT INTO moneda (codigo_iso, nombre, activo)
SELECT 'UYU', 'Peso Uruguayo', true
WHERE NOT EXISTS (SELECT 1 FROM moneda WHERE codigo_iso = 'UYU');

INSERT INTO moneda (codigo_iso, nombre, activo)
SELECT 'CLP', 'Peso Chileno', true
WHERE NOT EXISTS (SELECT 1 FROM moneda WHERE codigo_iso = 'CLP');

INSERT INTO moneda (codigo_iso, nombre, activo)
SELECT 'PYG', 'Guarani Paraguayo', true
WHERE NOT EXISTS (SELECT 1 FROM moneda WHERE codigo_iso = 'PYG');

-- ============================================================
-- 4. canal
-- ============================================================
INSERT INTO canal (codigo, nombre, activo)
SELECT 'WEB', 'Banca Web', true
WHERE NOT EXISTS (SELECT 1 FROM canal WHERE codigo = 'WEB');

INSERT INTO canal (codigo, nombre, activo)
SELECT 'SUCURSAL', 'Sucursal Fisica', true
WHERE NOT EXISTS (SELECT 1 FROM canal WHERE codigo = 'SUCURSAL');

INSERT INTO canal (codigo, nombre, activo)
SELECT 'CAJERO', 'Cajero Automatico', true
WHERE NOT EXISTS (SELECT 1 FROM canal WHERE codigo = 'CAJERO');

INSERT INTO canal (codigo, nombre, activo)
SELECT 'TRANSFERENCIA', 'Transferencia Electronica', true
WHERE NOT EXISTS (SELECT 1 FROM canal WHERE codigo = 'TRANSFERENCIA');

INSERT INTO canal (codigo, nombre, activo)
SELECT 'DEBITO_AUTOMATICO', 'Debito Automatico', true
WHERE NOT EXISTS (SELECT 1 FROM canal WHERE codigo = 'DEBITO_AUTOMATICO');

INSERT INTO canal (codigo, nombre, activo)
SELECT 'MOVIL', 'App Movil', true
WHERE NOT EXISTS (SELECT 1 FROM canal WHERE codigo = 'MOVIL');

-- ============================================================
-- 5. producto
-- ============================================================
INSERT INTO producto (codigo, nombre, activo)
SELECT 'CTA_CTE', 'Cuenta Corriente', true
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE codigo = 'CTA_CTE');

INSERT INTO producto (codigo, nombre, activo)
SELECT 'CAJA_AHORRO', 'Caja de Ahorro', true
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE codigo = 'CAJA_AHORRO');

INSERT INTO producto (codigo, nombre, activo)
SELECT 'PLAZO_FIJO', 'Plazo Fijo', true
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE codigo = 'PLAZO_FIJO');

INSERT INTO producto (codigo, nombre, activo)
SELECT 'TARJETA_CREDITO', 'Tarjeta de Credito', true
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE codigo = 'TARJETA_CREDITO');

INSERT INTO producto (codigo, nombre, activo)
SELECT 'TARJETA_DEBITO', 'Tarjeta de Debito', true
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE codigo = 'TARJETA_DEBITO');

INSERT INTO producto (codigo, nombre, activo)
SELECT 'PRESTAMO', 'Prestamo Personal', true
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE codigo = 'PRESTAMO');

-- ============================================================
-- 6. tipo_documento
-- ============================================================
INSERT INTO tipo_documento (codigo, nombre, pais_relacion_id, activo)
SELECT 'DNI', 'Documento Nacional de Identidad', p.id, true FROM pais p WHERE p.codigo_iso = 'AR'
WHERE NOT EXISTS (SELECT 1 FROM tipo_documento WHERE codigo = 'DNI');

INSERT INTO tipo_documento (codigo, nombre, pais_relacion_id, activo)
SELECT 'PASAPORTE', 'Pasaporte', NULL, true
WHERE NOT EXISTS (SELECT 1 FROM tipo_documento WHERE codigo = 'PASAPORTE');

INSERT INTO tipo_documento (codigo, nombre, pais_relacion_id, activo)
SELECT 'CUIT', 'Clave Unica de Identificacion Tributaria', p.id, true FROM pais p WHERE p.codigo_iso = 'AR'
WHERE NOT EXISTS (SELECT 1 FROM tipo_documento WHERE codigo = 'CUIT');

INSERT INTO tipo_documento (codigo, nombre, pais_relacion_id, activo)
SELECT 'CUIL', 'Clave Unica de Identificacion Laboral', p.id, true FROM pais p WHERE p.codigo_iso = 'AR'
WHERE NOT EXISTS (SELECT 1 FROM tipo_documento WHERE codigo = 'CUIL');

INSERT INTO tipo_documento (codigo, nombre, pais_relacion_id, activo)
SELECT 'CDI', 'Cedula de Identidad', p.id, true FROM pais p WHERE p.codigo_iso = 'PY'
WHERE NOT EXISTS (SELECT 1 FROM tipo_documento WHERE codigo = 'CDI');

-- ============================================================
-- 7. escenario
-- ============================================================
INSERT INTO escenario (codigo, nombre, descripcion, activo)
SELECT 'FRAUDE_CLASICO', 'Fraude Clasico', 'Operaciones fraudulentas tipicas: suplantacion de identidad, uso no autorizado', true
WHERE NOT EXISTS (SELECT 1 FROM escenario WHERE codigo = 'FRAUDE_CLASICO');

INSERT INTO escenario (codigo, nombre, descripcion, activo)
SELECT 'LAVADO_DINERO', 'Lavado de Dinero', 'Operaciones de lavado de activos', true
WHERE NOT EXISTS (SELECT 1 FROM escenario WHERE codigo = 'LAVADO_DINERO');

INSERT INTO escenario (codigo, nombre, descripcion, activo)
SELECT 'FINANCIAMIENTO_TERRORISMO', 'Financiamiento del Terrorismo', 'Operaciones de financiamiento a actividades ilicitas', true
WHERE NOT EXISTS (SELECT 1 FROM escenario WHERE codigo = 'FINANCIAMIENTO_TERRORISMO');

INSERT INTO escenario (codigo, nombre, descripcion, activo)
SELECT 'OPERACIONES_SOSPECHOSAS', 'Operaciones Sospechosas', 'Operaciones que superan umbrales regulatorios', true
WHERE NOT EXISTS (SELECT 1 FROM escenario WHERE codigo = 'OPERACIONES_SOSPECHOSAS');

-- ============================================================
-- 8. accion
-- ============================================================
INSERT INTO accion (codigo, descripcion, activo)
SELECT 'CREAR', 'Crear un recurso', true
WHERE NOT EXISTS (SELECT 1 FROM accion WHERE codigo = 'CREAR');

INSERT INTO accion (codigo, descripcion, activo)
SELECT 'ACTUALIZAR', 'Actualizar un recurso', true
WHERE NOT EXISTS (SELECT 1 FROM accion WHERE codigo = 'ACTUALIZAR');

INSERT INTO accion (codigo, descripcion, activo)
SELECT 'ELIMINAR', 'Eliminar un recurso', true
WHERE NOT EXISTS (SELECT 1 FROM accion WHERE codigo = 'ELIMINAR');

INSERT INTO accion (codigo, descripcion, activo)
SELECT 'CONSULTAR', 'Consultar un recurso', true
WHERE NOT EXISTS (SELECT 1 FROM accion WHERE codigo = 'CONSULTAR');

INSERT INTO accion (codigo, descripcion, activo)
SELECT 'ASIGNAR', 'Asignar un recurso a un usuario', true
WHERE NOT EXISTS (SELECT 1 FROM accion WHERE codigo = 'ASIGNAR');

INSERT INTO accion (codigo, descripcion, activo)
SELECT 'CERRAR', 'Cerrar un recurso', true
WHERE NOT EXISTS (SELECT 1 FROM accion WHERE codigo = 'CERRAR');

-- ============================================================
-- 9. servicio_externo
-- ============================================================
INSERT INTO servicio_externo (codigo, nombre, url_base, timeout_ms, reintentos, activo)
SELECT 'API_RENAPER', 'API Renaper', 'https://api.renaper.gob.ar/v1', 5000, 3, true
WHERE NOT EXISTS (SELECT 1 FROM servicio_externo WHERE codigo = 'API_RENAPER');

INSERT INTO servicio_externo (codigo, nombre, url_base, timeout_ms, reintentos, activo)
SELECT 'API_OFAC', 'API OFAC SDN', 'https://api.ofac.treasury.gov/v1', 5000, 3, true
WHERE NOT EXISTS (SELECT 1 FROM servicio_externo WHERE codigo = 'API_OFAC');

INSERT INTO servicio_externo (codigo, nombre, url_base, timeout_ms, reintentos, activo)
SELECT 'API_BCRA', 'API BCRA', 'https://api.bcra.gob.ar/v1', 3000, 2, true
WHERE NOT EXISTS (SELECT 1 FROM servicio_externo WHERE codigo = 'API_BCRA');

-- ============================================================
-- 10. lista_regulatoria
-- ============================================================
INSERT INTO lista_regulatoria (codigo, nombre, fuente, activo)
SELECT 'OFAC_SDN', 'Lista OFAC SDN', 'OFICIAL', true
WHERE NOT EXISTS (SELECT 1 FROM lista_regulatoria WHERE codigo = 'OFAC_SDN');

INSERT INTO lista_regulatoria (codigo, nombre, fuente, activo)
SELECT 'PEP_NACIONAL', 'Lista PEP Nacional', 'OFICIAL', true
WHERE NOT EXISTS (SELECT 1 FROM lista_regulatoria WHERE codigo = 'PEP_NACIONAL');

INSERT INTO lista_regulatoria (codigo, nombre, fuente, activo)
SELECT 'LISTA_NEGRA_INTERNA', 'Lista Negra Interna', 'INTERNA', true
WHERE NOT EXISTS (SELECT 1 FROM lista_regulatoria WHERE codigo = 'LISTA_NEGRA_INTERNA');

-- ============================================================
-- 11. calendario_riesgo
-- ============================================================
INSERT INTO calendario_riesgo (fecha, tipo_dia, descripcion, activo)
SELECT '2025-01-01', 'FERIADO', 'Año Nuevo', true
WHERE NOT EXISTS (SELECT 1 FROM calendario_riesgo WHERE fecha = '2025-01-01');

INSERT INTO calendario_riesgo (fecha, tipo_dia, descripcion, activo)
SELECT '2025-02-24', 'FERIADO', 'Carnaval', true
WHERE NOT EXISTS (SELECT 1 FROM calendario_riesgo WHERE fecha = '2025-02-24');

INSERT INTO calendario_riesgo (fecha, tipo_dia, descripcion, activo)
SELECT '2025-02-25', 'FERIADO', 'Carnaval', true
WHERE NOT EXISTS (SELECT 1 FROM calendario_riesgo WHERE fecha = '2025-02-25');

INSERT INTO calendario_riesgo (fecha, tipo_dia, descripcion, activo)
SELECT '2025-03-24', 'FERIADO', 'Dia de la Memoria', true
WHERE NOT EXISTS (SELECT 1 FROM calendario_riesgo WHERE fecha = '2025-03-24');

INSERT INTO calendario_riesgo (fecha, tipo_dia, descripcion, activo)
SELECT '2025-04-02', 'FERIADO', 'Dia del Veterano', true
WHERE NOT EXISTS (SELECT 1 FROM calendario_riesgo WHERE fecha = '2025-04-02');

INSERT INTO calendario_riesgo (fecha, tipo_dia, descripcion, activo)
SELECT '2025-04-18', 'FERIADO', 'Viernes Santo', true
WHERE NOT EXISTS (SELECT 1 FROM calendario_riesgo WHERE fecha = '2025-04-18');

INSERT INTO calendario_riesgo (fecha, tipo_dia, descripcion, activo)
SELECT '2025-05-01', 'FERIADO', 'Dia del Trabajador', true
WHERE NOT EXISTS (SELECT 1 FROM calendario_riesgo WHERE fecha = '2025-05-01');

INSERT INTO calendario_riesgo (fecha, tipo_dia, descripcion, activo)
SELECT '2025-05-25', 'FERIADO', 'Revolucion de Mayo', true
WHERE NOT EXISTS (SELECT 1 FROM calendario_riesgo WHERE fecha = '2025-05-25');

INSERT INTO calendario_riesgo (fecha, tipo_dia, descripcion, activo)
SELECT '2025-06-20', 'FERIADO', 'Dia de la Bandera', true
WHERE NOT EXISTS (SELECT 1 FROM calendario_riesgo WHERE fecha = '2025-06-20');

INSERT INTO calendario_riesgo (fecha, tipo_dia, descripcion, activo)
SELECT '2025-07-09', 'FERIADO', 'Dia de la Independencia', true
WHERE NOT EXISTS (SELECT 1 FROM calendario_riesgo WHERE fecha = '2025-07-09');

INSERT INTO calendario_riesgo (fecha, tipo_dia, descripcion, activo)
SELECT '2025-12-08', 'FERIADO', 'Inmaculada Concepcion', true
WHERE NOT EXISTS (SELECT 1 FROM calendario_riesgo WHERE fecha = '2025-12-08');

INSERT INTO calendario_riesgo (fecha, tipo_dia, descripcion, activo)
SELECT '2025-12-25', 'FERIADO', 'Navidad', true
WHERE NOT EXISTS (SELECT 1 FROM calendario_riesgo WHERE fecha = '2025-12-25');

INSERT INTO calendario_riesgo (fecha, tipo_dia, descripcion, activo)
SELECT '2026-01-01', 'FERIADO', 'Año Nuevo', true
WHERE NOT EXISTS (SELECT 1 FROM calendario_riesgo WHERE fecha = '2026-01-01');

-- ============================================================
-- 12. persona
-- ============================================================
INSERT INTO persona (tipo_persona, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, razon_social, nacionalidad_pais_id, fecha_nacimiento, segmento, activo)
SELECT 'FISICA', 'Juan', 'Carlos', 'Perez', 'Garcia', NULL,
       (SELECT id FROM pais WHERE codigo_iso = 'AR'), '1985-03-15', 'STANDARD', true
WHERE NOT EXISTS (SELECT 1 FROM persona WHERE primer_nombre = 'Juan' AND primer_apellido = 'Perez');

INSERT INTO persona (tipo_persona, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, razon_social, nacionalidad_pais_id, fecha_nacimiento, segmento, activo)
SELECT 'FISICA', 'Maria', 'Elena', 'Garcia', 'Lopez', NULL,
       (SELECT id FROM pais WHERE codigo_iso = 'AR'), '1990-07-22', 'VIP', true
WHERE NOT EXISTS (SELECT 1 FROM persona WHERE primer_nombre = 'Maria' AND primer_apellido = 'Garcia');

INSERT INTO persona (tipo_persona, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, razon_social, nacionalidad_pais_id, fecha_nacimiento, segmento, activo)
SELECT 'JURIDICA', 'Empresa', NULL, 'Ejemplo S.A.', NULL, 'Empresa Ejemplo S.A.',
       (SELECT id FROM pais WHERE codigo_iso = 'AR'), NULL, 'CORPORATE', true
WHERE NOT EXISTS (SELECT 1 FROM persona WHERE razon_social = 'Empresa Ejemplo S.A.');

-- ============================================================
-- 13. perfil_cliente
-- ============================================================
INSERT INTO perfil_cliente (persona_id, promedio_mensual, cantidad_operaciones_mensual, horario_habitual_desde, horario_habitual_hasta, fecha_calculo, activo)
SELECT (SELECT id FROM persona WHERE primer_nombre = 'Juan' AND primer_apellido = 'Perez'),
       150000.00, 15, '08:00:00'::time, '18:00:00'::time, NOW(), true
WHERE NOT EXISTS (SELECT 1 FROM perfil_cliente WHERE persona_id = (SELECT id FROM persona WHERE primer_nombre = 'Juan' AND primer_apellido = 'Perez'));

INSERT INTO perfil_cliente (persona_id, promedio_mensual, cantidad_operaciones_mensual, horario_habitual_desde, horario_habitual_hasta, fecha_calculo, activo)
SELECT (SELECT id FROM persona WHERE primer_nombre = 'Maria' AND primer_apellido = 'Garcia'),
       500000.00, 30, '09:00:00'::time, '17:00:00'::time, NOW(), true
WHERE NOT EXISTS (SELECT 1 FROM perfil_cliente WHERE persona_id = (SELECT id FROM persona WHERE primer_nombre = 'Maria' AND primer_apellido = 'Garcia'));

INSERT INTO perfil_cliente (persona_id, promedio_mensual, cantidad_operaciones_mensual, horario_habitual_desde, horario_habitual_hasta, fecha_calculo, activo)
SELECT (SELECT id FROM persona WHERE razon_social = 'Empresa Ejemplo S.A.'),
       2000000.00, 50, '08:00:00'::time, '20:00:00'::time, NOW(), true
WHERE NOT EXISTS (SELECT 1 FROM perfil_cliente WHERE persona_id = (SELECT id FROM persona WHERE razon_social = 'Empresa Ejemplo S.A.'));

-- ============================================================
-- 14. documento
-- ============================================================
INSERT INTO documento (persona_id, tipo_documento_id, numero_documento, pais_emisor_id, es_principal, activo)
SELECT (SELECT id FROM persona WHERE primer_nombre = 'Juan' AND primer_apellido = 'Perez'),
       (SELECT id FROM tipo_documento WHERE codigo = 'DNI'), '12345678',
       (SELECT id FROM pais WHERE codigo_iso = 'AR'), true, true
WHERE NOT EXISTS (
  SELECT 1 FROM documento WHERE tipo_documento_id = (SELECT id FROM tipo_documento WHERE codigo = 'DNI')
  AND numero_documento = '12345678'
);

INSERT INTO documento (persona_id, tipo_documento_id, numero_documento, pais_emisor_id, es_principal, activo)
SELECT (SELECT id FROM persona WHERE primer_nombre = 'Maria' AND primer_apellido = 'Garcia'),
       (SELECT id FROM tipo_documento WHERE codigo = 'DNI'), '23456789',
       (SELECT id FROM pais WHERE codigo_iso = 'AR'), true, true
WHERE NOT EXISTS (
  SELECT 1 FROM documento WHERE tipo_documento_id = (SELECT id FROM tipo_documento WHERE codigo = 'DNI')
  AND numero_documento = '23456789'
);

INSERT INTO documento (persona_id, tipo_documento_id, numero_documento, pais_emisor_id, es_principal, activo)
SELECT (SELECT id FROM persona WHERE razon_social = 'Empresa Ejemplo S.A.'),
       (SELECT id FROM tipo_documento WHERE codigo = 'CUIT'), '30-12345678-9',
       (SELECT id FROM pais WHERE codigo_iso = 'AR'), true, true
WHERE NOT EXISTS (
  SELECT 1 FROM documento WHERE tipo_documento_id = (SELECT id FROM tipo_documento WHERE codigo = 'CUIT')
  AND numero_documento = '30-12345678-9'
);

-- ============================================================
-- 15. horario_riesgo
-- ============================================================
INSERT INTO horario_riesgo (nombre, hora_desde, hora_hasta, nivel_riesgo_id, activo)
SELECT 'Horario Nocturno', '22:00:00'::time, '05:00:00'::time,
       (SELECT id FROM nivel_riesgo WHERE codigo = 'ALTO'), true
WHERE NOT EXISTS (SELECT 1 FROM horario_riesgo WHERE nombre = 'Horario Nocturno');

INSERT INTO horario_riesgo (nombre, hora_desde, hora_hasta, nivel_riesgo_id, activo)
SELECT 'Madrugada', '00:00:00'::time, '04:00:00'::time,
       (SELECT id FROM nivel_riesgo WHERE codigo = 'CRITICO'), true
WHERE NOT EXISTS (SELECT 1 FROM horario_riesgo WHERE nombre = 'Madrugada');

INSERT INTO horario_riesgo (nombre, hora_desde, hora_hasta, nivel_riesgo_id, activo)
SELECT 'Fin de Semana', '08:00:00'::time, '20:00:00'::time,
       (SELECT id FROM nivel_riesgo WHERE codigo = 'MEDIO'), true
WHERE NOT EXISTS (SELECT 1 FROM horario_riesgo WHERE nombre = 'Fin de Semana');

-- ============================================================
-- 16. pais_riesgo
-- ============================================================
INSERT INTO pais_riesgo (pais_id, lista_regulatoria_id, nivel_riesgo_id, motivo, fecha_inicio, activo)
SELECT (SELECT id FROM pais WHERE codigo_iso = 'PY'),
       (SELECT id FROM lista_regulatoria WHERE codigo = 'OFAC_SDN'),
       (SELECT id FROM nivel_riesgo WHERE codigo = 'ALTO'),
       'Paraguay - Alto riesgo segun OFAC', '2024-01-01', true
WHERE NOT EXISTS (
  SELECT 1 FROM pais_riesgo
  WHERE pais_id = (SELECT id FROM pais WHERE codigo_iso = 'PY')
  AND lista_regulatoria_id = (SELECT id FROM lista_regulatoria WHERE codigo = 'OFAC_SDN')
);

INSERT INTO pais_riesgo (pais_id, lista_regulatoria_id, nivel_riesgo_id, motivo, fecha_inicio, activo)
SELECT (SELECT id FROM pais WHERE codigo_iso = 'US'),
       (SELECT id FROM lista_regulatoria WHERE codigo = 'OFAC_SDN'),
       (SELECT id FROM nivel_riesgo WHERE codigo = 'MEDIO'),
       'EEUU - Monitoreo general OFAC', '2024-01-01', true
WHERE NOT EXISTS (
  SELECT 1 FROM pais_riesgo
  WHERE pais_id = (SELECT id FROM pais WHERE codigo_iso = 'US')
  AND lista_regulatoria_id = (SELECT id FROM lista_regulatoria WHERE codigo = 'OFAC_SDN')
);

INSERT INTO pais_riesgo (pais_id, lista_regulatoria_id, nivel_riesgo_id, motivo, fecha_inicio, activo)
SELECT (SELECT id FROM pais WHERE codigo_iso = 'BR'),
       (SELECT id FROM lista_regulatoria WHERE codigo = 'LISTA_NEGRA_INTERNA'),
       (SELECT id FROM nivel_riesgo WHERE codigo = 'MEDIO'),
       'Brasil - Riesgo medio operativo', '2024-06-01', true
WHERE NOT EXISTS (
  SELECT 1 FROM pais_riesgo
  WHERE pais_id = (SELECT id FROM pais WHERE codigo_iso = 'BR')
  AND lista_regulatoria_id = (SELECT id FROM lista_regulatoria WHERE codigo = 'LISTA_NEGRA_INTERNA')
);

-- ============================================================
-- 17. control_importe
-- ============================================================
INSERT INTO control_importe (producto_id, moneda_id, monto_minimo, monto_maximo, nivel_riesgo_id, prioridad, activo)
SELECT (SELECT id FROM producto WHERE codigo = 'CTA_CTE'),
       (SELECT id FROM moneda WHERE codigo_iso = 'ARS'),
       500000.00, 9999999.00,
       (SELECT id FROM nivel_riesgo WHERE codigo = 'ALTO'), 1, true
WHERE NOT EXISTS (
  SELECT 1 FROM control_importe
  WHERE producto_id = (SELECT id FROM producto WHERE codigo = 'CTA_CTE')
  AND moneda_id = (SELECT id FROM moneda WHERE codigo_iso = 'ARS')
  AND monto_minimo = 500000.00
);

INSERT INTO control_importe (producto_id, moneda_id, monto_minimo, monto_maximo, nivel_riesgo_id, prioridad, activo)
SELECT (SELECT id FROM producto WHERE codigo = 'CTA_CTE'),
       (SELECT id FROM moneda WHERE codigo_iso = 'USD'),
       10000.00, 999999.00,
       (SELECT id FROM nivel_riesgo WHERE codigo = 'CRITICO'), 1, true
WHERE NOT EXISTS (
  SELECT 1 FROM control_importe
  WHERE producto_id = (SELECT id FROM producto WHERE codigo = 'CTA_CTE')
  AND moneda_id = (SELECT id FROM moneda WHERE codigo_iso = 'USD')
  AND monto_minimo = 10000.00
);

INSERT INTO control_importe (producto_id, moneda_id, monto_minimo, monto_maximo, nivel_riesgo_id, prioridad, activo)
SELECT (SELECT id FROM producto WHERE codigo = 'CAJA_AHORRO'),
       (SELECT id FROM moneda WHERE codigo_iso = 'ARS'),
       300000.00, 9999999.00,
       (SELECT id FROM nivel_riesgo WHERE codigo = 'MEDIO'), 2, true
WHERE NOT EXISTS (
  SELECT 1 FROM control_importe
  WHERE producto_id = (SELECT id FROM producto WHERE codigo = 'CAJA_AHORRO')
  AND moneda_id = (SELECT id FROM moneda WHERE codigo_iso = 'ARS')
  AND monto_minimo = 300000.00
);

INSERT INTO control_importe (producto_id, moneda_id, monto_minimo, monto_maximo, nivel_riesgo_id, prioridad, activo)
SELECT (SELECT id FROM producto WHERE codigo = 'PLAZO_FIJO'),
       (SELECT id FROM moneda WHERE codigo_iso = 'ARS'),
       1000000.00, 99999999.00,
       (SELECT id FROM nivel_riesgo WHERE codigo = 'MEDIO'), 3, true
WHERE NOT EXISTS (
  SELECT 1 FROM control_importe
  WHERE producto_id = (SELECT id FROM producto WHERE codigo = 'PLAZO_FIJO')
  AND moneda_id = (SELECT id FROM moneda WHERE codigo_iso = 'ARS')
  AND monto_minimo = 1000000.00
);

-- ============================================================
-- 18. control_frecuencia
-- ============================================================
INSERT INTO control_frecuencia (producto_id, cantidad_operaciones, ventana_tiempo, unidad_tiempo, nivel_riesgo_id, activo)
SELECT (SELECT id FROM producto WHERE codigo = 'CTA_CTE'), 5, 1, 'HORAS',
       (SELECT id FROM nivel_riesgo WHERE codigo = 'ALTO'), true
WHERE NOT EXISTS (
  SELECT 1 FROM control_frecuencia
  WHERE producto_id = (SELECT id FROM producto WHERE codigo = 'CTA_CTE')
  AND cantidad_operaciones = 5
);

INSERT INTO control_frecuencia (producto_id, cantidad_operaciones, ventana_tiempo, unidad_tiempo, nivel_riesgo_id, activo)
SELECT (SELECT id FROM producto WHERE codigo = 'CTA_CTE'), 20, 1, 'DIAS',
       (SELECT id FROM nivel_riesgo WHERE codigo = 'MEDIO'), true
WHERE NOT EXISTS (
  SELECT 1 FROM control_frecuencia
  WHERE producto_id = (SELECT id FROM producto WHERE codigo = 'CTA_CTE')
  AND cantidad_operaciones = 20
);

INSERT INTO control_frecuencia (producto_id, cantidad_operaciones, ventana_tiempo, unidad_tiempo, nivel_riesgo_id, activo)
SELECT (SELECT id FROM producto WHERE codigo = 'TARJETA_CREDITO'), 10, 1, 'HORAS',
       (SELECT id FROM nivel_riesgo WHERE codigo = 'ALTO'), true
WHERE NOT EXISTS (
  SELECT 1 FROM control_frecuencia
  WHERE producto_id = (SELECT id FROM producto WHERE codigo = 'TARJETA_CREDITO')
  AND cantidad_operaciones = 10
);

-- ============================================================
-- 19. cliente_pep
-- ============================================================
INSERT INTO cliente_pep (persona_id, tipo_documento_id, numero_documento, cargo, institucion, tipo_pep, nivel_riesgo_id, fecha_inicio, fuente, activo)
SELECT (SELECT id FROM persona WHERE primer_nombre = 'Maria' AND primer_apellido = 'Garcia'),
       (SELECT id FROM tipo_documento WHERE codigo = 'DNI'), '23456789',
       'Director de Compliance', 'Banco Central', 'NACIONAL',
       (SELECT id FROM nivel_riesgo WHERE codigo = 'ALTO'), '2020-01-01', 'INTERNA', true
WHERE NOT EXISTS (
  SELECT 1 FROM cliente_pep
  WHERE persona_id = (SELECT id FROM persona WHERE primer_nombre = 'Maria' AND primer_apellido = 'Garcia')
);

-- ============================================================
-- 20. cliente_observado
-- ============================================================
INSERT INTO cliente_observado (persona_id, motivo, nivel_riesgo_id, fecha_inicio, activo)
SELECT (SELECT id FROM persona WHERE primer_nombre = 'Juan' AND primer_apellido = 'Perez'),
       'FRAUDE_PREVIO',
       (SELECT id FROM nivel_riesgo WHERE codigo = 'ALTO'), '2023-06-15', true
WHERE NOT EXISTS (
  SELECT 1 FROM cliente_observado
  WHERE persona_id = (SELECT id FROM persona WHERE primer_nombre = 'Juan' AND primer_apellido = 'Perez')
);

-- ============================================================
-- 21. elemento_lista
-- ============================================================
INSERT INTO elemento_lista (lista_regulatoria_id, tipo_elemento, valor_identificador, fecha_incorporacion, activo)
SELECT (SELECT id FROM lista_regulatoria WHERE codigo = 'OFAC_SDN'), 'PERSONA', 'JOSE_MARTINEZ', '2020-01-01', true
WHERE NOT EXISTS (
  SELECT 1 FROM elemento_lista
  WHERE lista_regulatoria_id = (SELECT id FROM lista_regulatoria WHERE codigo = 'OFAC_SDN')
  AND valor_identificador = 'JOSE_MARTINEZ'
);

INSERT INTO elemento_lista (lista_regulatoria_id, tipo_elemento, valor_identificador, fecha_incorporacion, activo)
SELECT (SELECT id FROM lista_regulatoria WHERE codigo = 'OFAC_SDN'), 'ENTIDAD', 'BANCO_XYZ_LTDA', '2019-05-15', true
WHERE NOT EXISTS (
  SELECT 1 FROM elemento_lista
  WHERE lista_regulatoria_id = (SELECT id FROM lista_regulatoria WHERE codigo = 'OFAC_SDN')
  AND valor_identificador = 'BANCO_XYZ_LTDA'
);

INSERT INTO elemento_lista (lista_regulatoria_id, tipo_elemento, valor_identificador, fecha_incorporacion, activo)
SELECT (SELECT id FROM lista_regulatoria WHERE codigo = 'LISTA_NEGRA_INTERNA'), 'PERSONA', 'PEDRO_RAMIREZ', '2024-03-01', true
WHERE NOT EXISTS (
  SELECT 1 FROM elemento_lista
  WHERE lista_regulatoria_id = (SELECT id FROM lista_regulatoria WHERE codigo = 'LISTA_NEGRA_INTERNA')
  AND valor_identificador = 'PEDRO_RAMIREZ'
);

-- ============================================================
-- 22. reglas_riesgo
-- ============================================================
INSERT INTO reglas_riesgo (codigo, nombre, descripcion, tipo_regla, severidad, prioridad, condicion, escenario_id, score_base, creada_por, version, activa, estado)
SELECT 'RIESGO_MONTO', 'Riesgo por Monto', 'Evalua el riesgo basado en el monto de la transaccion',
       'Monto', 'ALTA', 1, 'monto > 1000000',
       (SELECT id FROM escenario WHERE codigo = 'LAVADO_DINERO'), 30.00,
       (SELECT id FROM usuarios WHERE email = 'admin@antifraude.com'), 1, true, 'ACTIVA'
WHERE NOT EXISTS (SELECT 1 FROM reglas_riesgo WHERE codigo = 'RIESGO_MONTO');

INSERT INTO reglas_riesgo (codigo, nombre, descripcion, tipo_regla, severidad, prioridad, condicion, escenario_id, score_base, creada_por, version, activa, estado)
SELECT 'RIESGO_PAIS', 'Riesgo por Pais', 'Evalua el riesgo segun pais de origen/destino',
       'Pais', 'ALTA', 2, 'paisOrigen in (''US'', ''PY'', ''BR'')',
       (SELECT id FROM escenario WHERE codigo = 'LAVADO_DINERO'), 25.00,
       (SELECT id FROM usuarios WHERE email = 'admin@antifraude.com'), 1, true, 'ACTIVA'
WHERE NOT EXISTS (SELECT 1 FROM reglas_riesgo WHERE codigo = 'RIESGO_PAIS');

INSERT INTO reglas_riesgo (codigo, nombre, descripcion, tipo_regla, severidad, prioridad, condicion, escenario_id, score_base, creada_por, version, activa, estado)
SELECT 'RIESGO_CANAL', 'Riesgo por Canal', 'Evalua el riesgo segun el canal utilizado',
       'Canal', 'MEDIA', 3, 'canal in (''WEB'', ''MOVIL'')',
       (SELECT id FROM escenario WHERE codigo = 'FRAUDE_CLASICO'), 20.00,
       (SELECT id FROM usuarios WHERE email = 'admin@antifraude.com'), 1, true, 'ACTIVA'
WHERE NOT EXISTS (SELECT 1 FROM reglas_riesgo WHERE codigo = 'RIESGO_CANAL');

INSERT INTO reglas_riesgo (codigo, nombre, descripcion, tipo_regla, severidad, prioridad, condicion, escenario_id, score_base, creada_por, version, activa, estado)
SELECT 'RIESGO_HORARIO', 'Riesgo por Horario', 'Evalua el riesgo segun horario de la operacion',
       'Horario', 'MEDIA', 4, 'hora between 0 and 5',
       (SELECT id FROM escenario WHERE codigo = 'FRAUDE_CLASICO'), 15.00,
       (SELECT id FROM usuarios WHERE email = 'admin@antifraude.com'), 1, true, 'ACTIVA'
WHERE NOT EXISTS (SELECT 1 FROM reglas_riesgo WHERE codigo = 'RIESGO_HORARIO');

INSERT INTO reglas_riesgo (codigo, nombre, descripcion, tipo_regla, severidad, prioridad, condicion, escenario_id, score_base, creada_por, version, activa, estado)
SELECT 'RIESGO_LISTAS', 'Riesgo por Listas Regulatorias', 'Evalua si el cliente figura en listas',
       'Listas', 'CRITICA', 0, 'coincideLista == true',
       (SELECT id FROM escenario WHERE codigo = 'LAVADO_DINERO'), 50.00,
       (SELECT id FROM usuarios WHERE email = 'admin@antifraude.com'), 1, true, 'ACTIVA'
WHERE NOT EXISTS (SELECT 1 FROM reglas_riesgo WHERE codigo = 'RIESGO_LISTAS');

INSERT INTO reglas_riesgo (codigo, nombre, descripcion, tipo_regla, severidad, prioridad, condicion, escenario_id, score_base, creada_por, version, activa, estado)
SELECT 'RIESGO_PEP', 'Riesgo por PEP', 'Evalua si el cliente es Persona Politicamente Expuesta',
       'PEP', 'ALTA', 0, 'esPEP == true',
       (SELECT id FROM escenario WHERE codigo = 'LAVADO_DINERO'), 40.00,
       (SELECT id FROM usuarios WHERE email = 'admin@antifraude.com'), 1, true, 'ACTIVA'
WHERE NOT EXISTS (SELECT 1 FROM reglas_riesgo WHERE codigo = 'RIESGO_PEP');

INSERT INTO reglas_riesgo (codigo, nombre, descripcion, tipo_regla, severidad, prioridad, condicion, escenario_id, score_base, creada_por, version, activa, estado)
SELECT 'RIESGO_OBSERVADO', 'Riesgo por Cliente Observado', 'Evalua si el cliente tiene observaciones previas',
       'Observado', 'ALTA', 0, 'esObservado == true',
       (SELECT id FROM escenario WHERE codigo = 'OPERACIONES_SOSPECHOSAS'), 35.00,
       (SELECT id FROM usuarios WHERE email = 'admin@antifraude.com'), 1, true, 'ACTIVA'
WHERE NOT EXISTS (SELECT 1 FROM reglas_riesgo WHERE codigo = 'RIESGO_OBSERVADO');

INSERT INTO reglas_riesgo (codigo, nombre, descripcion, tipo_regla, severidad, prioridad, condicion, escenario_id, score_base, creada_por, version, activa, estado)
SELECT 'RIESGO_FRECUENCIA', 'Riesgo por Frecuencia', 'Evalua riesgo por alta frecuencia de operaciones',
       'Frecuencia', 'MEDIA', 5, 'frecuencia > 5 en 1 hora',
       (SELECT id FROM escenario WHERE codigo = 'FRAUDE_CLASICO'), 20.00,
       (SELECT id FROM usuarios WHERE email = 'admin@antifraude.com'), 1, true, 'ACTIVA'
WHERE NOT EXISTS (SELECT 1 FROM reglas_riesgo WHERE codigo = 'RIESGO_FRECUENCIA');

-- ============================================================
-- 23. transacciones
-- ============================================================
INSERT INTO transacciones (transaction_uuid, codigo, identificador_documento, cuenta_origen, cuenta_destino, monto, moneda_id, moneda_codigo, canal_id, canal_codigo, tipo_transaccion, pais_origen, fecha_transaccion, score_riesgo, nivel_riesgo_id, estado_evaluacion, procesada, fecha_procesamiento)
SELECT gen_random_uuid(), 'TXN-001', '12345678', 'CUENTA-001', 'CUENTA-002', 50000.00,
       (SELECT id FROM moneda WHERE codigo_iso = 'ARS'), 'ARS',
       (SELECT id FROM canal WHERE codigo = 'SUCURSAL'), 'SUCURSAL',
       'TRANSFERENCIA', 'Argentina', NOW() - INTERVAL '2 hours', 15.00,
       (SELECT id FROM nivel_riesgo WHERE codigo = 'BAJO'), 'APROBADA', true, NOW() - INTERVAL '1 hour'
WHERE NOT EXISTS (SELECT 1 FROM transacciones WHERE codigo = 'TXN-001');

INSERT INTO transacciones (transaction_uuid, codigo, identificador_documento, cuenta_origen, cuenta_destino, monto, moneda_id, moneda_codigo, canal_id, canal_codigo, tipo_transaccion, pais_origen, fecha_transaccion, score_riesgo, nivel_riesgo_id, estado_evaluacion, procesada)
SELECT gen_random_uuid(), 'TXN-002', '23456789', 'CUENTA-003', 'CUENTA-004', 1500000.00,
       (SELECT id FROM moneda WHERE codigo_iso = 'ARS'), 'ARS',
       (SELECT id FROM canal WHERE codigo = 'WEB'), 'WEB',
       'TRANSFERENCIA', 'Argentina', NOW() - INTERVAL '1 hour', 75.00,
       (SELECT id FROM nivel_riesgo WHERE codigo = 'ALTO'), 'SOSPECHOSA', false
WHERE NOT EXISTS (SELECT 1 FROM transacciones WHERE codigo = 'TXN-002');

INSERT INTO transacciones (transaction_uuid, codigo, identificador_documento, cuenta_origen, cuenta_destino, monto, moneda_id, moneda_codigo, canal_id, canal_codigo, tipo_transaccion, pais_origen, fecha_transaccion, score_riesgo, nivel_riesgo_id, estado_evaluacion, procesada)
SELECT gen_random_uuid(), 'TXN-003', '12345678', 'CUENTA-001', 'CUENTA-005', 300000.00,
       (SELECT id FROM moneda WHERE codigo_iso = 'ARS'), 'ARS',
       (SELECT id FROM canal WHERE codigo = 'WEB'), 'WEB',
       'TRANSFERENCIA', 'Argentina', NOW() - INTERVAL '30 minutes', 45.00,
       (SELECT id FROM nivel_riesgo WHERE codigo = 'BAJO'), 'PENDIENTE', false
WHERE NOT EXISTS (SELECT 1 FROM transacciones WHERE codigo = 'TXN-003');

INSERT INTO transacciones (transaction_uuid, codigo, cuenta_origen, cuenta_destino, monto, moneda_id, moneda_codigo, canal_id, canal_codigo, tipo_transaccion, pais_origen_ref, pais_origen, fecha_transaccion, estado_evaluacion, procesada)
SELECT gen_random_uuid(), 'TXN-004', 'CUENTA-006', 'CUENTA-007', 25000.00,
       (SELECT id FROM moneda WHERE codigo_iso = 'USD'), 'USD',
       (SELECT id FROM canal WHERE codigo = 'WEB'), 'WEB',
       'TRANSFERENCIA_INTERNACIONAL', (SELECT id FROM pais WHERE codigo_iso = 'US'), 'Estados Unidos',
       NOW() - INTERVAL '15 minutes', 'PENDIENTE', false
WHERE NOT EXISTS (SELECT 1 FROM transacciones WHERE codigo = 'TXN-004');

INSERT INTO transacciones (transaction_uuid, codigo, cuenta_origen, cuenta_destino, monto, moneda_id, moneda_codigo, canal_id, canal_codigo, tipo_transaccion, pais_origen, fecha_transaccion, score_riesgo, nivel_riesgo_id, estado_evaluacion, procesada, fecha_procesamiento)
SELECT gen_random_uuid(), 'TXN-005', 'CUENTA-008', 'CUENTA-009', 100000.00,
       (SELECT id FROM moneda WHERE codigo_iso = 'USD'), 'USD',
       (SELECT id FROM canal WHERE codigo = 'SUCURSAL'), 'SUCURSAL',
       'RETIRO', 'Argentina', NOW() - INTERVAL '5 minutes', 15.00,
       (SELECT id FROM nivel_riesgo WHERE codigo = 'BAJO'), 'APROBADA', true, NOW()
WHERE NOT EXISTS (SELECT 1 FROM transacciones WHERE codigo = 'TXN-005');
