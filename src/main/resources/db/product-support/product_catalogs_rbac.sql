-- ============================================================================
-- V13: Catalogos productivos minimos + RBAC (roles/permisos) por plan
-- Base de decisiones: MDS/PLAN_ACCION_MODELO_NEGOCIO.md Fase 0 (0.2, 0.3, 0.4).
-- Cierra la brecha productiva: V10 solo sembraba moneda USD/PYG; una
-- instalacion nueva arrancaba sin pais/canal/producto/nivel_riesgo/listas ni
-- roles para asignar (precios por rol de V10 quedaban vacios).
-- Nomenclatura unificada (0.4): roles de aplicacion = ADMINISTRADOR, SUPERVISOR,
-- ANALISTA, AUDITOR (mismos codigos que SecurityConfig, ROLE_ y el frontend).
-- Idempotente sobre bases ya pobladas (demO o legacy).
-- ============================================================================

-- 1) Monedas operativas adicionales (USD/PYG ya provistas por V10).
INSERT INTO moneda (codigo_iso, nombre, nombre_en, fuente, activo)
SELECT v.codigo_iso, v.nombre, v.nombre_en, v.fuente, true
FROM (VALUES
  ('ARS', 'Peso argentino', 'Argentine Peso', 'ISO 4217'),
  ('BRL', 'Real brasileno', 'Brazilian Real', 'ISO 4217'),
  ('EUR', 'Euro', 'Euro', 'ISO 4217'),
  ('UYU', 'Peso uruguayo', 'Uruguayan Peso', 'ISO 4217')
) v(codigo_iso, nombre, nombre_en, fuente)
ON CONFLICT (codigo_iso) DO UPDATE SET nombre = EXCLUDED.nombre, fuente = EXCLUDED.fuente;

-- 2) Paises operativos y jurisdicciones de riesgo.
INSERT INTO pais (codigo_iso, codigo_iso3, nombre, activo)
SELECT v.codigo_iso, v.codigo_iso3, v.nombre, true
FROM (VALUES
  ('PY','PRY','Paraguay'), ('AR','ARG','Argentina'), ('BR','BRA','Brasil'),
  ('UY','URY','Uruguay'), ('CL','CHL','Chile'), ('PE','PER','Peru'),
  ('BO','BOL','Bolivia'), ('EC','ECU','Ecuador'), ('CO','COL','Colombia'),
  ('US','USA','Estados Unidos'), ('MX','MEX','Mexico'), ('ES','ESP','Espana'),
  ('GB','GBR','Reino Unido'), ('DE','DEU','Alemania'), ('FR','FRA','Francia'),
  ('HK','HKG','Hong Kong'), ('SG','SGP','Singapur'), ('PA','PAN','Panama'),
  ('KY','CYM','Islas Caiman'), ('AE','ARE','Emiratos Arabes Unidos'),
  ('VE','VEN','Venezuela'), ('KP','PRK','Corea del Norte'), ('IR','IRN','Iran'),
  ('SY','SYR','Siria'), ('CU','CUB','Cuba'), ('RU','RUS','Rusia')
) v(codigo_iso, codigo_iso3, nombre)
ON CONFLICT (codigo_iso) DO UPDATE SET codigo_iso3 = EXCLUDED.codigo_iso3, nombre = EXCLUDED.nombre, activo = true;

-- 3) Niveles de riesgo.
INSERT INTO nivel_riesgo (codigo, nombre, orden, activo)
SELECT v.codigo, v.nombre, v.orden, true
FROM (VALUES
  ('BAJO', 'Bajo', 1), ('MEDIO', 'Medio', 2), ('ALTO', 'Alto', 3), ('CRITICO', 'Critico', 4)
) v(codigo, nombre, orden)
ON CONFLICT (codigo) DO UPDATE SET nombre = EXCLUDED.nombre, orden = EXCLUDED.orden, activo = true;

-- 4) Canales de transaccion.
INSERT INTO canal_transaccion (codigo, nombre, descripcion, activo)
SELECT v.codigo, v.nombre, v.descripcion, true
FROM (VALUES
  ('SUCURSAL', 'Sucursal', 'Operacion presencial'),
  ('CAJERO_AUTOMATICO', 'Cajero Automatico', 'Retiros y depositos ATM'),
  ('BANCA_ONLINE', 'Banca Online', 'Operaciones por web'),
  ('BANCA_MOVIL', 'Banca Movil', 'Operaciones por app'),
  ('TRANSFERENCIA', 'Transferencia', 'Transferencias interbancarias'),
  ('CHEQUE', 'Cheque', 'Emision y cobro de cheques'),
  ('PAGO_MOVIL', 'Pago Movil', 'Pagos por billetera'),
  ('REMESA', 'Remesas', 'Envio y cobro de remesas')
) v(codigo, nombre, descripcion)
ON CONFLICT (codigo) DO UPDATE SET nombre = EXCLUDED.nombre, descripcion = EXCLUDED.descripcion, activo = true;

-- 5) Tipos de transaccion.
INSERT INTO tipo_transaccion (codigo, nombre, categoria, descripcion, activo)
SELECT v.codigo, v.nombre, v.categoria, v.descripcion, true
FROM (VALUES
  ('TRANSFERENCIA', 'Transferencia', 'TRANSFERENCIA', 'Transferencia entre cuentas'),
  ('TRANSFERENCIA_INTERNACIONAL', 'Transferencia Internacional', 'TRANSFERENCIA', 'SWIFT / pagos transfronterizos'),
  ('PAGO_TARJETA', 'Pago Con Tarjeta', 'PAGO', 'Compra con credito o debito'),
  ('RETIRO_EFECTIVO', 'Retiro De Efectivo', 'RETIRO', 'Retiro en cajero o caja'),
  ('DEPOSITO_EFECTIVO', 'Deposito De Efectivo', 'DEPOSITO', 'Deposito en efectivo'),
  ('CHEQUE', 'Cheque', 'CHEQUE', 'Emision o cobro de cheque'),
  ('PAGO_MOVIL', 'Pago Movil', 'PAGO', 'Pago mediante billetera'),
  ('REMESA', 'Remesa', 'REMESA', 'Envio o cobro de remesas')
) v(codigo, nombre, categoria, descripcion)
ON CONFLICT (codigo) DO UPDATE SET nombre = EXCLUDED.nombre, categoria = EXCLUDED.categoria, descripcion = EXCLUDED.descripcion, activo = true;

-- 6) Productos.
INSERT INTO producto (codigo, nombre, activo)
SELECT v.codigo, v.nombre, true
FROM (VALUES
  ('CUENTA_CORRIENTE', 'Cuenta Corriente'),
  ('CUENTA_AHORRO', 'Cuenta De Ahorro'),
  ('TARJETA_CREDITO', 'Tarjeta De Credito'),
  ('TARJETA_DEBITO', 'Tarjeta De Debito'),
  ('PRESTAMO', 'Prestamo'),
  ('TRANSFERENCIA_INT', 'Transferencia Internacional')
) v(codigo, nombre)
ON CONFLICT (codigo) DO UPDATE SET nombre = EXCLUDED.nombre, activo = true;

-- 7) Fuentes de datos regulatorias (metadata de catalogos; no sujetos reales).
INSERT INTO fuente_datos_riesgo (codigo, nombre, organismo, licencia_uso, frecuencia_actualizacion, activo)
SELECT v.codigo, v.nombre, v.organismo, v.licencia_uso, v.frecuencia, true
FROM (VALUES
  ('SEPRELAD', 'Lista SEPRELAD (PY)', 'SEPRELAD Paraguay', 'Uso regulatorio interno', 'DIARIA'),
  ('ONU_SC', 'Lista del Consejo de Seguridad de la ONU', 'Naciones Unidas', 'Uso regulatorio interno', 'DIARIA'),
  ('OFAC', 'Lista SDN de la OFAC', 'Departamento del Tesoro de EE.UU.', 'Uso regulatorio interno', 'DIARIA'),
  ('PEP', 'Listado PEP por pais', 'Fuentes oficiales nacionales', 'Uso regulatorio interno', 'MENSUAL')
) v(codigo, nombre, organismo, licencia_uso, frecuencia)
ON CONFLICT (codigo) DO UPDATE SET nombre = EXCLUDED.nombre, organismo = EXCLUDED.organismo, activo = true;

-- 8) Listas regulatorias (sancion, PEP y riesgo pais).
INSERT INTO lista_regulatoria (fuente_datos_riesgo_id, codigo, nombre, tipo_lista, alcance, activa)
SELECT f.id, v.codigo, v.nombre, v.tipo_lista, v.alcance, true
FROM (VALUES
  ('SEPRELAD', 'SEPRELAD_LIST', 'Lista de prevencion de lavado (PY)', 'SANCION', 'PY'),
  ('ONU_SC', 'ONU_SC_LIST', 'Consejo de Seguridad de la ONU', 'SANCION', 'GLOBAL'),
  ('OFAC', 'OFAC_SDN', 'Specially Designated Nationals', 'SANCION', 'US'),
  ('PEP', 'PEP_LIST', 'Personas Expuestas Politicamente', 'PEP', 'GLOBAL'),
  (NULL, 'RIESGO_PAIS', 'Riesgo por jurisdiccion', 'PAIS', 'GLOBAL')
) v(fuente_codigo, codigo, nombre, tipo_lista, alcance)
LEFT JOIN fuente_datos_riesgo f ON f.codigo = v.fuente_codigo
ON CONFLICT (codigo) DO UPDATE SET nombre = EXCLUDED.nombre, tipo_lista = EXCLUDED.tipo_lista, alcance = EXCLUDED.alcance, activa = true;

-- 9) Riesgo por pais (catalogo de jurisdicciones, segun el modelo de negocio).
INSERT INTO pais_riesgo (pais_id, lista_regulatoria_id, nivel_riesgo_id, categoria, severidad, motivo, fecha_inicio, activo)
SELECT p.id, l.id, nr.id, v.categoria, v.severidad, v.motivo, DATE '2026-01-01', true
FROM (VALUES
  ('KP','ALTO_RIESGO','CRITICO','CRITICA','Sanciones del Consejo de Seguridad de la ONU'),
  ('IR','ALTO_RIESGO','CRITICO','CRITICA','Sanciones financieras internacionales'),
  ('SY','ALTO_RIESGO','CRITICO','CRITICA','Sanciones del Consejo de Seguridad de la ONU'),
  ('CU','ALTO_RIESGO','ALTO','ALTA','Sanciones economicas de EE.UU.'),
  ('VE','MONITOREO','ALTO','ALTA','Riesgo politico y regulatorio'),
  ('PA','MONITOREO','ALTO','ALTA','Jurisdiccion con reforzamiento de controles'),
  ('KY','MONITOREO','ALTO','ALTA','Jurisdiccion offshore de interes'),
  ('HK','MONITOREO','MEDIO','MEDIA','Flujo internacional relevante')
) v(codigo_pais, categoria, nivel, severidad, motivo)
JOIN pais p ON p.codigo_iso = v.codigo_pais
JOIN lista_regulatoria l ON l.codigo = 'RIESGO_PAIS'
JOIN nivel_riesgo nr ON nr.codigo = v.nivel
ON CONFLICT (pais_id, lista_regulatoria_id, fecha_inicio) DO UPDATE SET
  categoria = EXCLUDED.categoria, severidad = EXCLUDED.severidad, motivo = EXCLUDED.motivo, activo = true;

-- ============================================================================
-- RBAC (Fase 0.3): roles de aplicacion y permisos funcionales.
-- ============================================================================

-- 9) Roles de aplicacion (unificados; ver 0.4).
INSERT INTO rol (codigo, nombre, descripcion, alcance, tipo, activo)
SELECT v.codigo, v.nombre, v.descripcion, v.alcance, 'EMPRESA', true
FROM (VALUES
  ('ADMINISTRADOR', 'Administrador', 'Acceso total de administracion y licencias', 'GLOBAL'),
  ('SUPERVISOR', 'Supervisor', 'Reglas, simulacion, casos y reportes', 'EMPRESA'),
  ('ANALISTA', 'Analista', 'Investigacion de casos y reportes', 'EMPRESA'),
  ('AUDITOR', 'Auditor', 'Auditoria y reportes de licencia', 'EMPRESA')
) v(codigo, nombre, descripcion, alcance)
ON CONFLICT (codigo) DO UPDATE SET nombre = EXCLUDED.nombre, descripcion = EXCLUDED.descripcion, alcance = EXCLUDED.alcance, activo = true;

-- 10) Permisos funcionales (los que gatean SecurityConfig y @PreAuthorize).
INSERT INTO permiso (codigo, nombre, descripcion, modulo, accion)
SELECT v.codigo, v.nombre, v.descripcion, v.modulo, v.accion
FROM (VALUES
  ('LICENCIAS_VER', 'Ver Licencias', 'Consulta de suscripciones, planes y uso', 'Licencias', 'VER'),
  ('LICENCIAS_GESTIONAR', 'Gestionar Licencias', 'Instalar/activar/heartbeat on-premise', 'Licencias', 'GESTIONAR'),
  ('USUARIOS_VER', 'Ver Usuarios', 'Listar y consultar usuarios', 'Usuarios', 'VER'),
  ('USUARIOS_CREAR', 'Crear Usuarios', 'Alta de usuarios e invitaciones', 'Usuarios', 'CREAR'),
  ('USUARIOS_EDITAR', 'Editar Usuarios', 'Editar/desactivar usuarios', 'Usuarios', 'EDITAR'),
  ('REGLAS_VER', 'Ver Reglas', 'CRUD de reglas, simulador y escenarios', 'Motor', 'VER'),
  ('CASOS_VER', 'Ver Casos', 'Investigacion de casos', 'Casos', 'VER'),
  ('REPORTES_VER', 'Ver Reportes', 'Descarga de reportes ROS y licencia', 'Reportes', 'VER'),
  ('AUDITORIA_VER', 'Ver Auditoria', 'Acceso al registro de auditoria', 'Auditoria', 'VER'),
  ('ALERTAS_VER', 'Ver Alertas', 'Consulta de alertas', 'Alertas', 'VER'),
  ('KYC_VER', 'Ver KYC', 'Consultas KYC', 'KYC', 'VER'),
  ('DASHBOARD_VER', 'Ver Dashboard', 'Indicadores del dashboard', 'Dashboard', 'VER'),
  ('SIMULADOR_VER', 'Ver Simulador', 'Evaluaciones sin persistencia', 'Motor', 'VER'),
  ('CATALOGOS_VER', 'Ver Catalogos', 'Consulta de catalogos operativos', 'Catalogos', 'VER')
) v(codigo, nombre, descripcion, modulo, accion)
ON CONFLICT (codigo) DO UPDATE SET nombre = EXCLUDED.nombre, modulo = EXCLUDED.modulo, accion = EXCLUDED.accion;

-- 11) Efectividad de permisos por rol.
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON true
WHERE (r.codigo = 'ADMINISTRADOR')
   OR (r.codigo = 'SUPERVISOR' AND p.codigo IN
       ('REGLAS_VER','SIMULADOR_VER','CASOS_VER','REPORTES_VER','AUDITORIA_VER','ALERTAS_VER','KYC_VER','DASHBOARD_VER','CATALOGOS_VER'))
   OR (r.codigo = 'ANALISTA' AND p.codigo IN
       ('CASOS_VER','REPORTES_VER','ALERTAS_VER','KYC_VER','DASHBOARD_VER','SIMULADOR_VER'))
   OR (r.codigo = 'AUDITOR' AND p.codigo IN
       ('AUDITORIA_VER','REPORTES_VER','DASHBOARD_VER'))
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

-- ============================================================================
-- Precios por rol adicional (Fase 0.3): alinear a la nomenclatura unificada.
-- V10 los inserto con codigos provisionales (ADMIN_EMPRESA/GERENTE_SUPERVISOR);
-- aqui se migran a ADMINISTRADOR/SUPERVISOR/ANALISTA/AUDITOR.
-- ============================================================================

-- Quitar filas provisionales (si existieran de una base legacy).
DELETE FROM plan_plan_precios_rol
WHERE rol_id IN (
    SELECT id FROM rol WHERE codigo IN ('ADMIN_EMPRESA','GERENTE_SUPERVISOR')
);

-- Insertar precios productivos: Admin 600, Gerente/Supervisor 550, Analista 400, Auditor 350.
INSERT INTO plan_plan_precios_rol (plan_licencia_id, rol_id, precio_anual, activo)
SELECT p.id, r.id, v.precio, true
FROM (VALUES
  ('BASICO', 'ADMINISTRADOR', 600), ('BASICO', 'SUPERVISOR', 550),
  ('BASICO', 'ANALISTA', 400),  ('BASICO', 'AUDITOR', 350),
  ('ESTANDAR', 'ADMINISTRADOR', 600), ('ESTANDAR', 'SUPERVISOR', 550),
  ('ESTANDAR', 'ANALISTA', 400), ('ESTANDAR', 'AUDITOR', 350),
  ('PREMIUM', 'ADMINISTRADOR', 600), ('PREMIUM', 'SUPERVISOR', 550),
  ('PREMIUM', 'ANALISTA', 400), ('PREMIUM', 'AUDITOR', 350)
) v(plan, rol, precio)
JOIN plan_licencia p ON p.codigo = v.plan
JOIN rol r ON r.codigo = v.rol
ON CONFLICT (plan_licencia_id, rol_id) DO UPDATE SET precio_anual = EXCLUDED.precio_anual, activo = EXCLUDED.activo;