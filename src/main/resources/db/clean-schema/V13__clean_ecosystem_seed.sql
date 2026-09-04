-- ============================================================================
-- Regula AML - Clean ecosystem seed V13
-- Datos demo-productivos coherentes para regula_clean.
-- ============================================================================

BEGIN;

SELECT set_config('app.current_usuario_id', '00000000-0000-0000-0000-000000000001', false);
SELECT set_config('app.current_empresa_id', '11111111-1111-1111-1111-111111111111', false);

UPDATE usuarios
   SET nombre = 'Santiago Duarte Vera'
 WHERE email = 'seed@regula.local';

INSERT INTO empresa (id, codigo, nombre, ruc, estado)
VALUES
  ('00000000-0000-0000-0000-000000000100', 'REGULA_PLATFORM', 'Regula AML Plataforma S.A.', '80100000-1', 'ACTIVA'),
  ('33333333-3333-3333-3333-333333333333', 'COOP_SAN_MIGUEL', 'Cooperativa San Miguel Ltda.', '80033333-3', 'ACTIVA'),
  ('44444444-4444-4444-4444-444444444444', 'REMESAS_DEL_SUR', 'Remesas Del Sur S.A.', '80044444-4', 'ACTIVA'),
  ('55555555-5555-5555-5555-555555555555', 'FINANCIERA_ITAPUA', 'Financiera Itapua S.A.', '80055555-5', 'ACTIVA')
ON CONFLICT (codigo) DO UPDATE SET nombre = EXCLUDED.nombre, ruc = EXCLUDED.ruc, estado = EXCLUDED.estado;

INSERT INTO usuarios (email, nombre, password_hash, activo)
SELECT v.email, v.nombre, crypt('Regula2026!', gen_salt('bf')), true
FROM (VALUES
  ('ana.gimenez@regula.local', 'Ana Gimenez Rojas'),
  ('roberto.ayala@regula.local', 'Roberto Ayala Benitez'),
  ('lucia.rios@regula.local', 'Lucia Rios Caballero'),
  ('martin.ferreira@cliente.local', 'Martin Ferreira Acosta'),
  ('paola.duarte@cliente.local', 'Paola Duarte Vera'),
  ('elena.caceres@cliente.local', 'Elena Caceres Ortiz'),
  ('hector.sosa@cliente.local', 'Hector Sosa Franco'),
  ('sofia.ortiz@cliente.local', 'Sofia Ortiz Riveros'),
  ('claudia.vera@cliente.local', 'Claudia Vera Gimenez'),
  ('diego.benitez@cliente.local', 'Diego Benitez Aquino'),
  ('maria.riveros@cliente.local', 'Maria Riveros Lopez'),
  ('jose.aquino@cliente.local', 'Jose Aquino Silva'),
  ('karina.mendez@cliente.local', 'Karina Mendez Duarte'),
  ('esteban.galeano@cliente.local', 'Esteban Galeano Rojas'),
  ('noelia.rojas@cliente.local', 'Noelia Rojas Caballero'),
  ('carlos.rios@cliente.local', 'Carlos Rios Pereira'),
  ('valeria.romero@cliente.local', 'Valeria Romero Sosa'),
  ('fernando.vera@cliente.local', 'Fernando Vera Benitez'),
  ('patricia.nunez@cliente.local', 'Patricia Nunez Gimenez'),
  ('andres.caballero@cliente.local', 'Andres Caballero Torres'),
  ('beatriz.morales@cliente.local', 'Beatriz Morales Duarte'),
  ('gabriel.torres@cliente.local', 'Gabriel Torres Franco'),
  ('lorena.silva@cliente.local', 'Lorena Silva Medina')
) AS v(email, nombre)
ON CONFLICT (email) DO UPDATE SET nombre = EXCLUDED.nombre, activo = true;

INSERT INTO rol (codigo, nombre, descripcion, alcance) VALUES
('ADMIN_GENERAL', 'Admin General', 'Administra la plataforma Regula y todas las empresas cliente.', 'GLOBAL'),
('ADMIN_EMPRESA', 'Admin Empresa', 'Administra usuarios, suscripcion y configuracion de su empresa.', 'EMPRESA'),
('GERENTE_SUPERVISOR', 'Gerente Supervisor', 'Gestiona cumplimiento, reglas, reportes, alertas y casos.', 'EMPRESA'),
('ANALISTA', 'Analista', 'Investiga alertas, revisa KYC y propone resoluciones.', 'EMPRESA'),
('AUDITOR', 'Auditor', 'Consulta auditoria, evidencia, reportes y trazabilidad.', 'EMPRESA')
ON CONFLICT (codigo) DO UPDATE SET nombre = EXCLUDED.nombre, descripcion = EXCLUDED.descripcion, alcance = EXCLUDED.alcance;

INSERT INTO permiso (codigo, nombre, descripcion, modulo, accion)
SELECT v.codigo, v.nombre, v.descripcion, v.modulo, v.accion
FROM (VALUES
  ('EMPRESAS_VER', 'Ver Empresas', 'Consulta empresas y tenants.', 'SaaS', 'VER'),
  ('EMPRESAS_EDITAR', 'Editar Empresas', 'Crea y modifica empresas.', 'SaaS', 'EDITAR'),
  ('LICENCIAS_VER', 'Ver Licencias', 'Consulta planes, contratos y consumo.', 'Licencias', 'VER'),
  ('LICENCIAS_GESTIONAR', 'Gestionar Licencias', 'Administra planes, contratos, pagos y suscripciones.', 'Licencias', 'GESTIONAR'),
  ('PAGOS_VER', 'Ver Pagos', 'Consulta pagos registrados.', 'Pagos', 'VER'),
  ('PAGOS_GESTIONAR', 'Gestionar Pagos', 'Registra y modifica pagos.', 'Pagos', 'GESTIONAR'),
  ('USUARIOS_VER', 'Ver Usuarios', 'Consulta usuarios y perfiles.', 'Usuarios', 'VER'),
  ('USUARIOS_CREAR', 'Crear Usuarios', 'Crea usuarios de empresa.', 'Usuarios', 'CREAR'),
  ('USUARIOS_EDITAR', 'Editar Usuarios', 'Edita usuarios, roles y disponibilidad.', 'Usuarios', 'EDITAR'),
  ('REGLAS_VER', 'Ver Reglas', 'Consulta reglas y escenarios.', 'Motor', 'VER'),
  ('REGLAS_CREAR', 'Crear Reglas', 'Crea reglas de riesgo.', 'Motor', 'CREAR'),
  ('REGLAS_EDITAR', 'Editar Reglas', 'Modifica reglas de riesgo.', 'Motor', 'EDITAR'),
  ('REGLAS_ACTIVAR', 'Activar Reglas', 'Activa o desactiva reglas.', 'Motor', 'ACTIVAR'),
  ('CATALOGOS_VER', 'Ver Catalogos', 'Consulta catalogos operativos.', 'Catalogos', 'VER'),
  ('CATALOGOS_EDITAR', 'Editar Catalogos', 'Modifica catalogos editables.', 'Catalogos', 'EDITAR'),
  ('ALERTAS_VER', 'Ver Alertas', 'Consulta alertas y detalles.', 'Alertas', 'VER'),
  ('ALERTAS_ASIGNAR', 'Asignar Alertas', 'Asigna o reasigna alertas.', 'Alertas', 'ASIGNAR'),
  ('ALERTAS_RESOLVER', 'Resolver Alertas', 'Propone resolucion formal de alertas.', 'Alertas', 'RESOLVER'),
  ('ALERTAS_APROBAR', 'Aprobar Alertas', 'Aprueba o rechaza resoluciones.', 'Alertas', 'APROBAR'),
  ('CASOS_VER', 'Ver Casos', 'Consulta casos y evidencias.', 'Casos', 'VER'),
  ('CASOS_GESTIONAR', 'Gestionar Casos', 'Gestiona investigaciones, comentarios y evidencias.', 'Casos', 'GESTIONAR'),
  ('CASOS_APROBAR', 'Aprobar Casos', 'Aprueba cierres y escalamiento.', 'Casos', 'APROBAR'),
  ('REPORTES_VER', 'Ver Reportes', 'Consulta reportes y ROS.', 'Reportes', 'VER'),
  ('REPORTES_GENERAR', 'Generar Reportes', 'Genera reportes regulatorios.', 'Reportes', 'GENERAR'),
  ('AUDITORIA_VER', 'Ver Auditoria', 'Consulta logs y trazabilidad.', 'Auditoria', 'VER')
) AS v(codigo, nombre, descripcion, modulo, accion)
ON CONFLICT (codigo) DO UPDATE SET nombre = EXCLUDED.nombre, descripcion = EXCLUDED.descripcion, modulo = EXCLUDED.modulo, accion = EXCLUDED.accion;

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON
    r.codigo = 'ADMIN_GENERAL'
 OR (r.codigo = 'ADMIN_EMPRESA' AND p.codigo IN ('LICENCIAS_VER','PAGOS_VER','USUARIOS_VER','USUARIOS_CREAR','USUARIOS_EDITAR','CATALOGOS_VER','AUDITORIA_VER'))
 OR (r.codigo = 'GERENTE_SUPERVISOR' AND p.codigo IN ('REGLAS_VER','REGLAS_CREAR','REGLAS_EDITAR','REGLAS_ACTIVAR','CATALOGOS_VER','CATALOGOS_EDITAR','ALERTAS_VER','ALERTAS_ASIGNAR','ALERTAS_APROBAR','CASOS_VER','CASOS_GESTIONAR','CASOS_APROBAR','REPORTES_VER','REPORTES_GENERAR','AUDITORIA_VER'))
 OR (r.codigo = 'ANALISTA' AND p.codigo IN ('ALERTAS_VER','ALERTAS_ASIGNAR','ALERTAS_RESOLVER','CASOS_VER','CASOS_GESTIONAR','REPORTES_VER'))
 OR (r.codigo = 'AUDITOR' AND p.codigo IN ('CATALOGOS_VER','ALERTAS_VER','CASOS_VER','REPORTES_VER','AUDITORIA_VER'))
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO usuario_empresa (empresa_id, usuario_id, rol_id, estado)
SELECT
  CASE WHEN r.codigo = 'ADMIN_GENERAL' THEN '00000000-0000-0000-0000-000000000100'::uuid ELSE '11111111-1111-1111-1111-111111111111'::uuid END,
  u.id,
  r.id,
  'ACTIVO'
FROM (
  VALUES
  ('ana.gimenez@regula.local','ADMIN_GENERAL'), ('roberto.ayala@regula.local','ADMIN_GENERAL'), ('lucia.rios@regula.local','ADMIN_GENERAL'),
  ('martin.ferreira@cliente.local','ADMIN_EMPRESA'), ('paola.duarte@cliente.local','ADMIN_EMPRESA'), ('elena.caceres@cliente.local','ADMIN_EMPRESA'),
  ('hector.sosa@cliente.local','GERENTE_SUPERVISOR'), ('sofia.ortiz@cliente.local','GERENTE_SUPERVISOR'), ('claudia.vera@cliente.local','GERENTE_SUPERVISOR'),
  ('diego.benitez@cliente.local','ANALISTA'), ('maria.riveros@cliente.local','ANALISTA'), ('jose.aquino@cliente.local','ANALISTA'),
  ('karina.mendez@cliente.local','ANALISTA'), ('esteban.galeano@cliente.local','ANALISTA'), ('noelia.rojas@cliente.local','ANALISTA'),
  ('carlos.rios@cliente.local','ANALISTA'), ('valeria.romero@cliente.local','ANALISTA'), ('fernando.vera@cliente.local','ANALISTA'),
  ('patricia.nunez@cliente.local','ANALISTA'), ('andres.caballero@cliente.local','ANALISTA'),
  ('beatriz.morales@cliente.local','AUDITOR'), ('gabriel.torres@cliente.local','AUDITOR'), ('lorena.silva@cliente.local','AUDITOR')
) AS m(email, rol_codigo)
JOIN usuarios u ON u.email = m.email
JOIN rol r ON r.codigo = m.rol_codigo
ON CONFLICT (empresa_id, usuario_id, rol_id) DO NOTHING;

INSERT INTO perfil_usuario (empresa_id, usuario_id, cargo, area, telefono)
SELECT ue.empresa_id, ue.usuario_id, r.nombre, CASE WHEN r.codigo = 'ADMIN_GENERAL' THEN 'Plataforma' ELSE 'Cumplimiento AML' END, '+595 21 000 ' || lpad(ue.id::text, 4, '0')
FROM usuario_empresa ue
JOIN rol r ON r.id = ue.rol_id
ON CONFLICT (empresa_id, usuario_id) DO UPDATE SET cargo = EXCLUDED.cargo, area = EXCLUDED.area;

INSERT INTO disponibilidad_usuario (empresa_id, usuario_id, estado, carga_actual, capacidad_maxima)
SELECT ue.empresa_id, ue.usuario_id, 'DISPONIBLE', (ue.id % 6), 20
FROM usuario_empresa ue
JOIN rol r ON r.id = ue.rol_id
WHERE r.codigo = 'ANALISTA'
ON CONFLICT (empresa_id, usuario_id) DO UPDATE SET estado = EXCLUDED.estado, carga_actual = EXCLUDED.carga_actual;

INSERT INTO horario_laboral_usuario (empresa_id, usuario_id, dia_semana, hora_inicio, hora_fin)
SELECT ue.empresa_id, ue.usuario_id, d.dia, '08:00'::time, '17:00'::time
FROM usuario_empresa ue
JOIN rol r ON r.id = ue.rol_id
CROSS JOIN generate_series(1,5) d(dia)
WHERE r.codigo = 'ANALISTA'
ON CONFLICT DO NOTHING;

INSERT INTO plan_licencia (codigo, nombre, descripcion, precio_anual, moneda_id, limite_usuarios, limite_transacciones_mes, limite_consultas_kyc_mes, modulos_json)
VALUES
('BASICO', 'Basico', 'Plan de entrada para tesis y pruebas controladas.', 1200, (SELECT id FROM moneda WHERE codigo_iso = 'USD'), 15, 25000, 1000, '["alertas","kyc","reportes"]'),
('PROFESIONAL', 'Profesional', 'Plan operativo para entidades medianas.', 5400, (SELECT id FROM moneda WHERE codigo_iso = 'USD'), 50, 250000, 10000, '["alertas","kyc","reportes","motor"]'),
('ENTERPRISE', 'Enterprise', 'Plan avanzado con auditoria y multiempresa.', 18000, (SELECT id FROM moneda WHERE codigo_iso = 'USD'), 250, 2000000, 100000, '["alertas","kyc","reportes","motor","api","auditoria"]'),
('ACADEMICO', 'Academico', 'Plan academico para tesis y laboratorio.', 0, (SELECT id FROM moneda WHERE codigo_iso = 'USD'), 30, 50000, 5000, '["alertas","kyc","motor","datasets"]')
ON CONFLICT (codigo) DO UPDATE SET nombre = EXCLUDED.nombre, precio_anual = EXCLUDED.precio_anual;

INSERT INTO suscripcion (empresa_id, plan_licencia_id, codigo, estado, fecha_inicio, fecha_fin, renovacion_automatica, observacion)
SELECT e.id, p.id, 'SUS-' || e.codigo || '-2026',
       CASE WHEN e.codigo = 'REMESAS_DEL_SUR' THEN 'POR_VENCER' WHEN e.codigo = 'FINANCIERA_ITAPUA' THEN 'SUSPENDIDA' ELSE 'ACTIVA' END,
       DATE '2026-01-01', DATE '2026-12-31', true, 'Suscripcion anual demo Regula AML'
FROM empresa e
JOIN plan_licencia p ON p.codigo = CASE WHEN e.codigo = 'REGULA_PLATFORM' THEN 'ENTERPRISE' WHEN e.codigo = 'FINTECH_REGULA_PY' THEN 'PROFESIONAL' ELSE 'ACADEMICO' END
WHERE e.codigo IN ('REGULA_PLATFORM','BANCO_REGULA_PY','FINTECH_REGULA_PY','COOP_SAN_MIGUEL','REMESAS_DEL_SUR','FINANCIERA_ITAPUA')
ON CONFLICT (codigo) DO UPDATE SET estado = EXCLUDED.estado, fecha_fin = EXCLUDED.fecha_fin;

INSERT INTO contrato (empresa_id, suscripcion_id, numero_contrato, tipo_contrato, estado, fecha_firma, fecha_vigencia_desde, fecha_vigencia_hasta, documento_referencia, hash_documento)
SELECT s.empresa_id, s.id, 'CTR-' || s.codigo, 'LICENCIA_ANUAL', 'VIGENTE', DATE '2025-12-20', s.fecha_inicio, s.fecha_fin, 'contratos/' || s.codigo || '.pdf', hmac(s.codigo, 'regula-demo-hmac-key', 'sha256')
FROM suscripcion s
ON CONFLICT (numero_contrato) DO UPDATE SET estado = EXCLUDED.estado;

INSERT INTO pago (empresa_id, suscripcion_id, codigo, fecha_pago, monto, moneda_id, estado, metodo_pago, comprobante_referencia)
SELECT s.empresa_id, s.id, 'PAG-' || s.codigo || '-' || gs.n, make_timestamptz(2026, gs.n, 5, 10, 0, 0, 'America/Asuncion'),
       CASE WHEN p.precio_anual = 0 THEN 0 ELSE round(p.precio_anual / 12, 2) END, p.moneda_id,
       CASE WHEN gs.n <= 6 THEN 'PAGADO' ELSE 'PENDIENTE' END, 'TRANSFERENCIA_BANCARIA', 'COMP-' || s.codigo || '-' || gs.n
FROM suscripcion s
JOIN plan_licencia p ON p.id = s.plan_licencia_id
CROSS JOIN generate_series(1,4) gs(n)
ON CONFLICT (codigo) DO UPDATE SET estado = EXCLUDED.estado, monto = EXCLUDED.monto;

INSERT INTO uso_suscripcion (empresa_id, suscripcion_id, periodo, usuarios_activos, transacciones_procesadas, consultas_kyc, alertas_generadas, reportes_generados, consumo_json, anio, mes)
SELECT s.empresa_id, s.id, make_date(2026, gs.n, 1), 12 + gs.n, 15000 * gs.n, 300 * gs.n, 20 * gs.n, gs.n,
       jsonb_build_object('origen', 'seed_demo', 'periodo', gs.n), 2026, gs.n
FROM suscripcion s
CROSS JOIN generate_series(1,4) gs(n)
ON CONFLICT (empresa_id, suscripcion_id, periodo) DO UPDATE SET
    transacciones_procesadas = EXCLUDED.transacciones_procesadas,
    anio = EXCLUDED.anio,
    mes = EXCLUDED.mes;

INSERT INTO tipo_documento (codigo, nombre, descripcion, pais_relacion_id, tipo_persona, fuente_oficial)
SELECT v.codigo, v.nombre, v.descripcion, p.id, v.tipo_persona, v.fuente
FROM (VALUES
  ('PASS','Pasaporte','Documento de viaje legible por maquina ICAO Doc 9303',NULL,'FISICA','ICAO Doc 9303'),
  ('CI_PY','Cedula De Identidad Paraguay','Documento nacional paraguayo', 'PY','FISICA','Policia Nacional Paraguay'),
  ('RUC_PY','RUC Paraguay','Registro unico del contribuyente', 'PY','JURIDICA','DNIT Paraguay'),
  ('DNI_AR','DNI Argentina','Documento nacional de identidad', 'AR','FISICA','Registro Nacional de las Personas Argentina'),
  ('CUIT_AR','CUIT Argentina','Clave unica de identificacion tributaria', 'AR','JURIDICA','ARCA Argentina'),
  ('CUIL_AR','CUIL Argentina','Clave unica de identificacion laboral', 'AR','FISICA','ANSES Argentina'),
  ('RG_BR','RG Brasil','Registro geral de identidad', 'BR','FISICA','Gobierno Federal Brasil'),
  ('CPF_BR','CPF Brasil','Cadastro de personas fisicas', 'BR','FISICA','Receita Federal Brasil'),
  ('CNPJ_BR','CNPJ Brasil','Cadastro nacional de personas juridicas', 'BR','JURIDICA','Receita Federal Brasil'),
  ('DNI_PE','DNI Peru','Documento nacional de identidad', NULL,'FISICA','RENIEC Peru'),
  ('RUC_PE','RUC Peru','Registro unico de contribuyentes', NULL,'JURIDICA','SUNAT Peru'),
  ('CC_CO','Cedula Colombia','Cedula de ciudadania', NULL,'FISICA','Registraduria Colombia'),
  ('NIT_CO','NIT Colombia','Numero de identificacion tributaria', NULL,'JURIDICA','DIAN Colombia'),
  ('CI_UY','Cedula Uruguay','Cedula de identidad uruguaya', NULL,'FISICA','Direccion Nacional de Identificacion Civil Uruguay'),
  ('RUT_UY','RUT Uruguay','Registro unico tributario', NULL,'JURIDICA','DGI Uruguay'),
  ('RUN_CL','RUN Chile','Rol unico nacional', NULL,'FISICA','Registro Civil Chile'),
  ('RUT_CL','RUT Chile','Rol unico tributario', NULL,'JURIDICA','SII Chile'),
  ('CI_BO','Cedula Bolivia','Cedula de identidad boliviana', NULL,'FISICA','SEGIP Bolivia'),
  ('RUC_EC','RUC Ecuador','Registro unico de contribuyentes', NULL,'JURIDICA','SRI Ecuador'),
  ('SSN_US','SSN Estados Unidos','Social Security Number', 'US','FISICA','Social Security Administration')
) v(codigo, nombre, descripcion, pais_iso, tipo_persona, fuente)
LEFT JOIN pais p ON p.codigo_iso = v.pais_iso
ON CONFLICT (codigo) DO UPDATE SET nombre = EXCLUDED.nombre, pais_relacion_id = EXCLUDED.pais_relacion_id, fuente_oficial = EXCLUDED.fuente_oficial;

INSERT INTO persona (empresa_id, tipo_persona, nombre_razon_social, documento_hash)
SELECT '11111111-1111-1111-1111-111111111111'::uuid,
       CASE WHEN gs.n % 7 = 0 THEN 'JURIDICA' ELSE 'FISICA' END,
       CASE WHEN gs.n % 7 = 0 THEN 'Empresa Demo Operativa ' || gs.n || ' S.A.' ELSE nombres.nombre END,
       hmac('DOC-DEMO-' || gs.n, 'regula-demo-hmac-key', 'sha256')
FROM generate_series(1,34) gs(n)
JOIN LATERAL (
    SELECT (ARRAY['Adriana Lopez Duarte','Bruno Caceres Rojas','Camila Benitez Franco','Daniel Pereira Medina','Emilia Sosa Riveros','Fabian Vera Gomez','Gabriela Aquino Torres','Hugo Ferreira Gimenez','Ines Caballero Ortiz','Javier Romero Silva'])[1 + ((gs.n - 1) % 10)] || ' ' || gs.n AS nombre
) nombres ON true
ON CONFLICT (empresa_id, documento_hash) DO NOTHING;

INSERT INTO documento (empresa_id, persona_id, tipo_documento_id, pais_emisor_id, numero_documento_enc, numero_documento_hash, fecha_emision, fecha_expiracion, es_principal, estado)
SELECT p.empresa_id, p.id, td.id, py.id, pgp_sym_encrypt('DOC-DEMO-' || p.id, 'regula-demo-aes-key'), hmac('DOC-DEMO-' || p.id, 'regula-demo-hmac-key', 'sha256'),
       DATE '2020-01-01' + ((p.id % 700)::int), DATE '2030-01-01' + ((p.id % 700)::int), true, 'VIGENTE'
FROM persona p
JOIN tipo_documento td ON td.codigo = CASE WHEN p.tipo_persona = 'JURIDICA' THEN 'RUC_PY' ELSE 'CI_PY' END
JOIN pais py ON py.codigo_iso = 'PY'
ON CONFLICT (empresa_id, numero_documento_hash) DO NOTHING;

INSERT INTO perfil_cliente (empresa_id, persona_id, nivel_riesgo_id, segmento, actividad_economica, ingreso_mensual_estimado, volumen_mensual_esperado, cantidad_operaciones_mensual, perfil_json)
SELECT p.empresa_id, p.id,
       (SELECT id FROM nivel_riesgo WHERE codigo = CASE WHEN p.id % 11 = 0 THEN 'ALTO' WHEN p.id % 5 = 0 THEN 'MEDIO' ELSE 'BAJO' END),
       CASE WHEN p.tipo_persona = 'JURIDICA' THEN 'Empresa PyME' ELSE 'Persona Fisica Banca Digital' END,
       CASE WHEN p.tipo_persona = 'JURIDICA' THEN 'Comercio e importacion' ELSE 'Servicios profesionales' END,
       4500000 + (p.id * 100000), 25000000 + (p.id * 150000), 15 + (p.id % 20),
       jsonb_build_object('fuente', 'seed_demo', 'perfilEsperado', 'coherente')
FROM persona p
WHERE p.empresa_id = '11111111-1111-1111-1111-111111111111'
ON CONFLICT (empresa_id, persona_id) DO UPDATE SET segmento = EXCLUDED.segmento, perfil_json = EXCLUDED.perfil_json;

INSERT INTO cliente_pep (empresa_id, persona_id, tipo_pep, cargo, institucion, pais_id, fecha_inicio, estado, detalle_json)
SELECT p.empresa_id, p.id, 'RELACIONADO', 'Asesor Municipal', 'Municipalidad Demo', py.id, DATE '2024-01-01', 'ACTIVO', '{"tipo":"demo"}'
FROM persona p
JOIN pais py ON py.codigo_iso = 'PY'
WHERE p.empresa_id = '11111111-1111-1111-1111-111111111111' AND p.id % 9 = 0
ON CONFLICT DO NOTHING;

INSERT INTO cliente_observado (empresa_id, persona_id, motivo, severidad, estado, observacion)
SELECT p.empresa_id, p.id, 'Patron transaccional inusual', 'ALTA', 'ACTIVO', 'Cliente observado para pruebas AML'
FROM persona p
WHERE p.empresa_id = '11111111-1111-1111-1111-111111111111' AND p.id % 8 = 0
ON CONFLICT DO NOTHING;

INSERT INTO fuente_datos_riesgo (codigo, nombre, organismo, url_oficial, licencia_uso, frecuencia_actualizacion)
SELECT v.codigo, v.nombre, v.organismo, v.url, v.licencia, v.frecuencia
FROM (VALUES
('ONU_CS','Lista Consolidada ONU','Naciones Unidas','https://main.un.org/securitycouncil/en/content/un-sc-consolidated-list','Uso publico con atribucion y verificacion oficial','Continua'),
('OFAC_SDN','OFAC SDN','U.S. Treasury OFAC','https://sanctionslistservice.ofac.treas.gov/api/PublicationPreview/exports/SDN.XML','Dominio publico del gobierno de EE.UU. sujeto a terminos oficiales','Continua'),
('UE_SANCTIONS','EU Sanctions Map','Union Europea','https://www.sanctionsmap.eu/','Uso sujeto a terminos UE y atribucion','Continua'),
('FATF_GREY','Jurisdicciones GAFI Monitoreo Incrementado','GAFI/FATF','https://www.fatf-gafi.org/','Uso informativo sujeto a terminos oficiales','3 veces al anio'),
('FATF_BLACK','Jurisdicciones GAFI Alto Riesgo','GAFI/FATF','https://www.fatf-gafi.org/','Uso informativo sujeto a terminos oficiales','3 veces al anio'),
('GAFILAT','Documentos GAFILAT','GAFILAT','https://www.gafilat.org/','Uso informativo con atribucion','Periodica'),
('BCP_PY','Normativa BCP Paraguay','Banco Central del Paraguay','https://www.bcp.gov.py/','Consulta publica oficial','Periodica'),
('SEPRELAD_PY','Resoluciones SEPRELAD','SEPRELAD','https://www.seprelad.gov.py/','Consulta publica oficial','Periodica'),
('BANCO_MUNDIAL','Debarred Firms Banco Mundial','World Bank','https://www.worldbank.org/en/projects-operations/procurement/debarred-firms','Uso publico sujeto a terminos del Banco Mundial','Periodica'),
('BID_SANCTIONS','Sanciones BID','Banco Interamericano de Desarrollo','https://www.iadb.org/en/who-we-are/transparency/sanctioned-firms-and-individuals','Uso publico sujeto a terminos BID','Periodica'),
('INTERPOL','Notificaciones INTERPOL','INTERPOL','https://www.interpol.int/','Uso restringido por terminos oficiales','Continua'),
('FBI','Most Wanted FBI','FBI','https://www.fbi.gov/wanted','Dominio publico sujeto a terminos oficiales','Continua'),
('ICC','Casos Corte Penal Internacional','ICC','https://www.icc-cpi.int/','Uso publico sujeto a terminos ICC','Periodica'),
('LISTA_INTERNA','Lista Interna Demo Regula','Regula AML','https://regula.local/demo','Datos sinteticos para tesis','Manual'),
('PEP_DEMO','PEP Demo Global','Regula AML','https://regula.local/demo-pep','Datos sinteticos para tesis','Manual'),
('RIESGO_PAIS_DEMO','Riesgo Pais Demo','Regula AML','https://regula.local/riesgo-pais','Datos sinteticos para tesis','Manual')
) v(codigo, nombre, organismo, url, licencia, frecuencia)
ON CONFLICT (codigo) DO UPDATE SET nombre = EXCLUDED.nombre, url_oficial = EXCLUDED.url_oficial, licencia_uso = EXCLUDED.licencia_uso;

INSERT INTO lista_regulatoria (fuente_datos_riesgo_id, codigo, nombre, tipo_lista, alcance, url_descarga, licencia_uso, activa, fecha_ultima_revision)
SELECT f.id, f.codigo || '_LIST', f.nombre, CASE WHEN f.codigo LIKE '%PEP%' THEN 'PEP' WHEN f.codigo LIKE '%FATF%' THEN 'PAIS_RIESGO' ELSE 'SANCIONES' END,
       'GLOBAL', f.url_oficial, f.licencia_uso, true, CURRENT_DATE
FROM fuente_datos_riesgo f
ON CONFLICT (codigo) DO UPDATE SET nombre = EXCLUDED.nombre, fecha_ultima_revision = EXCLUDED.fecha_ultima_revision;

INSERT INTO sujeto_riesgo (lista_regulatoria_id, codigo, tipo_sujeto, nombre_normalizado, pais_id, fecha_nacimiento, tipo_riesgo, severidad, estado, detalle_json)
SELECT lr.id, 'SR-DEMO-' || gs.n,
       CASE WHEN gs.n % 5 = 0 THEN 'ENTIDAD' ELSE 'PERSONA' END,
       CASE WHEN gs.n % 5 = 0 THEN 'Entidad Comercial Riesgo Demo ' || gs.n ELSE 'Sujeto Riesgo Demo ' || gs.n END,
       (SELECT id FROM pais WHERE codigo_iso = CASE WHEN gs.n % 4 = 0 THEN 'BR' WHEN gs.n % 3 = 0 THEN 'AR' ELSE 'PY' END),
       CASE WHEN gs.n % 5 = 0 THEN NULL ELSE DATE '1975-01-01' + (gs.n * 53) END,
       CASE WHEN gs.n % 6 = 0 THEN 'PEP_RELACIONADO' WHEN gs.n % 4 = 0 THEN 'SANCION' ELSE 'LISTA_INTERNA' END,
       CASE WHEN gs.n % 7 = 0 THEN 'CRITICA' WHEN gs.n % 3 = 0 THEN 'ALTA' ELSE 'MEDIA' END,
       'ACTIVO',
       jsonb_build_object('demo', true, 'fuente', lr.codigo)
FROM generate_series(1,40) gs(n)
JOIN lista_regulatoria lr ON lr.codigo = CASE WHEN gs.n % 6 = 0 THEN 'PEP_DEMO_LIST' WHEN gs.n % 4 = 0 THEN 'OFAC_SDN_LIST' ELSE 'LISTA_INTERNA_LIST' END
ON CONFLICT (codigo) DO UPDATE SET nombre_normalizado = EXCLUDED.nombre_normalizado, severidad = EXCLUDED.severidad;

INSERT INTO sujeto_riesgo_alias (sujeto_riesgo_id, alias_normalizado, tipo_alias)
SELECT sr.id, sr.nombre_normalizado || ' Alias', 'ALIAS'
FROM sujeto_riesgo sr
ON CONFLICT DO NOTHING;

INSERT INTO sujeto_riesgo_documento (sujeto_riesgo_id, tipo_documento_id, pais_emisor_id, numero_documento_hash, documento_enmascarado)
SELECT sr.id, (SELECT id FROM tipo_documento WHERE codigo = 'CI_PY'), COALESCE(sr.pais_id, (SELECT id FROM pais WHERE codigo_iso = 'PY')),
       hmac(sr.codigo || '-DOC', 'regula-demo-hmac-key', 'sha256'), '***' || right(sr.codigo, 4)
FROM sujeto_riesgo sr
ON CONFLICT DO NOTHING;

INSERT INTO sujeto_riesgo_relacion (sujeto_origen_id, sujeto_destino_id, tipo_relacion, descripcion)
SELECT s1.id, s2.id, 'RELACIONADO_COMERCIALMENTE', 'Relacion demo para pruebas de PEP relacionados y beneficiario final'
FROM sujeto_riesgo s1
JOIN sujeto_riesgo s2 ON s2.codigo = 'SR-DEMO-' || ((substring(s1.codigo from '[0-9]+')::int % 40) + 1)
WHERE s1.codigo IN ('SR-DEMO-1','SR-DEMO-2','SR-DEMO-3','SR-DEMO-4','SR-DEMO-5','SR-DEMO-6','SR-DEMO-7','SR-DEMO-8','SR-DEMO-9','SR-DEMO-10')
ON CONFLICT DO NOTHING;

INSERT INTO pais_riesgo (pais_id, fuente_datos_riesgo_id, categoria, severidad, motivo, fecha_inicio, activo)
SELECT p.id, f.id, CASE WHEN p.codigo_iso IN ('HK') THEN 'MONITOREO_INCREMENTADO' ELSE 'RIESGO_DEMO' END,
       CASE WHEN p.codigo_iso IN ('HK') THEN 'ALTA' ELSE 'MEDIA' END,
       'Registro demo basado en monitoreo AML y fuentes documentadas', DATE '2026-01-01', true
FROM pais p
JOIN fuente_datos_riesgo f ON f.codigo = 'FATF_GREY'
WHERE p.codigo_iso IN ('HK','BR','AR','US')
ON CONFLICT DO NOTHING;

INSERT INTO escenario (empresa_id, codigo, nombre, descripcion, severidad_base)
SELECT '11111111-1111-1111-1111-111111111111', 'ESC-' || lpad(gs.n::text, 2, '0'),
       (ARRAY['SPI Con Alias Nuevo','LBTR Alto Valor','Efectivo Estructurado','EMPE Circular','QR Comercio Sensible','Remesa Repetitiva','FX Reiterado','Cheque Riesgoso','PEP Detectado','Pais Alto Riesgo'])[1 + ((gs.n - 1) % 10)] || ' ' || gs.n,
       'Escenario demo para motor de reglas Paraguay', CASE WHEN gs.n % 4 = 0 THEN 'CRITICA' WHEN gs.n % 3 = 0 THEN 'ALTA' ELSE 'MEDIA' END
FROM generate_series(1,20) gs(n)
ON CONFLICT (empresa_id, codigo) DO UPDATE SET nombre = EXCLUDED.nombre, severidad_base = EXCLUDED.severidad_base;

INSERT INTO accion (empresa_id, codigo, nombre, descripcion, tipo_accion, requiere_supervisor)
SELECT '11111111-1111-1111-1111-111111111111', 'ACC-' || lpad(gs.n::text, 2, '0'),
       (ARRAY['Crear Alerta','Asignar Analista','Solicitar Evidencia','Bloquear Preventivo','Retener Fondos','Liberar Movimiento','Generar ROS','Escalar Legal','Monitorear Cliente','Actualizar KYC'])[1 + ((gs.n - 1) % 10)] || ' ' || gs.n,
       'Accion demo parametrizable del motor AML', CASE WHEN gs.n % 5 = 0 THEN 'REPORTE' WHEN gs.n % 4 = 0 THEN 'BLOQUEO' ELSE 'OPERATIVA' END,
       gs.n % 4 = 0
FROM generate_series(1,20) gs(n)
ON CONFLICT (empresa_id, codigo) DO UPDATE SET nombre = EXCLUDED.nombre, tipo_accion = EXCLUDED.tipo_accion;

INSERT INTO reglas_riesgo (empresa_id, escenario_id, accion_id, codigo, nombre, descripcion, severidad, score_base, condiciones_json, acciones_json, activa, estado, version)
SELECT '11111111-1111-1111-1111-111111111111',
       e.id, a.id, 'REG-PY-' || lpad(gs.n::text, 2, '0'),
       'Regla Paraguay Demo ' || gs.n,
       'Regla activa demo para validar motor Paraguay y hallazgos AML.',
       CASE WHEN gs.n % 4 = 0 THEN 'CRITICA' WHEN gs.n % 3 = 0 THEN 'ALTA' ELSE 'MEDIA' END,
       20 + (gs.n * 3),
       jsonb_build_object('logic','ALL','conditions',jsonb_build_array(jsonb_build_object('fact','monto','operator','>','value',1000000 * gs.n))),
       jsonb_build_array(jsonb_build_object('accion','CREAR_ALERTA','requiereSupervisor', gs.n % 4 = 0)),
       true, 'ACTIVA', 1
FROM generate_series(1,30) gs(n)
JOIN escenario e ON e.empresa_id = '11111111-1111-1111-1111-111111111111' AND e.codigo = 'ESC-' || lpad(((gs.n - 1) % 20 + 1)::text, 2, '0')
JOIN accion a ON a.empresa_id = '11111111-1111-1111-1111-111111111111' AND a.codigo = 'ACC-' || lpad(((gs.n - 1) % 20 + 1)::text, 2, '0')
ON CONFLICT (empresa_id, codigo, version) DO UPDATE SET activa = true, estado = 'ACTIVA', condiciones_json = EXCLUDED.condiciones_json;

INSERT INTO control_importe (empresa_id, codigo, nombre, tipo_transaccion_id, moneda_id, monto_minimo, monto_maximo, severidad)
SELECT '11111111-1111-1111-1111-111111111111', 'CIMP-' || lpad(gs.n::text, 2, '0'), 'Control De Importe ' || gs.n,
       tt.id, m.id, 1000000 * gs.n, 1000000 * gs.n + 900000, CASE WHEN gs.n > 15 THEN 'ALTA' ELSE 'MEDIA' END
FROM generate_series(1,25) gs(n)
JOIN tipo_transaccion tt ON tt.codigo = CASE WHEN gs.n % 3 = 0 THEN 'PY_CASH_IN_BRANCH' WHEN gs.n % 3 = 1 THEN 'PY_SPI_ALIAS_TRANSFER' ELSE 'PY_LBTR_HIGH_VALUE' END
JOIN moneda m ON m.codigo_iso = CASE WHEN gs.n % 4 = 0 THEN 'USD' ELSE 'PYG' END
ON CONFLICT (empresa_id, codigo) DO UPDATE SET monto_minimo = EXCLUDED.monto_minimo;

INSERT INTO control_frecuencia (empresa_id, codigo, nombre, ventana_minutos, cantidad_maxima, monto_acumulado_maximo, severidad)
SELECT '11111111-1111-1111-1111-111111111111', 'CFREC-' || lpad(gs.n::text, 2, '0'), 'Control De Frecuencia ' || gs.n, 15 * gs.n, 3 + (gs.n % 8), 5000000 * gs.n, CASE WHEN gs.n > 15 THEN 'ALTA' ELSE 'MEDIA' END
FROM generate_series(1,25) gs(n)
ON CONFLICT (empresa_id, codigo) DO UPDATE SET ventana_minutos = EXCLUDED.ventana_minutos;

INSERT INTO horario_riesgo (empresa_id, codigo, nombre, hora_inicio, hora_fin, severidad)
SELECT '11111111-1111-1111-1111-111111111111', 'HR-' || lpad(gs.n::text, 2, '0'), 'Horario Riesgoso ' || gs.n,
       make_time((gs.n - 1) % 24, 0, 0), make_time(gs.n % 24, 0, 0), CASE WHEN gs.n BETWEEN 1 AND 5 THEN 'ALTA' ELSE 'MEDIA' END
FROM generate_series(1,24) gs(n)
ON CONFLICT (empresa_id, codigo) DO UPDATE SET hora_inicio = EXCLUDED.hora_inicio;

INSERT INTO calendario_riesgo (empresa_id, fecha, nombre, tipo_evento, severidad)
SELECT '11111111-1111-1111-1111-111111111111', DATE '2026-01-01' + (gs.n * 10), 'Evento Riesgo Demo ' || gs.n, CASE WHEN gs.n % 2 = 0 THEN 'FERIADO' ELSE 'CIERRE_MES' END, CASE WHEN gs.n % 5 = 0 THEN 'ALTA' ELSE 'MEDIA' END
FROM generate_series(1,30) gs(n)
ON CONFLICT (empresa_id, fecha, tipo_evento) DO UPDATE SET nombre = EXCLUDED.nombre;

UPDATE alertas_antifraude a
SET analista_asignado_id = u.id,
    fecha_asignacion = COALESCE(a.fecha_asignacion, now()),
    estado = CASE WHEN a.estado = 'NUEVA' THEN 'ASIGNADA' ELSE a.estado END
FROM usuarios u
WHERE u.email = (ARRAY['diego.benitez@cliente.local','maria.riveros@cliente.local','jose.aquino@cliente.local','karina.mendez@cliente.local','esteban.galeano@cliente.local'])[1 + ((a.id - 1) % 5)]
  AND a.empresa_id = '11111111-1111-1111-1111-111111111111';

INSERT INTO hallazgo_alerta (empresa_id, alerta_id, transaccion_id, fecha_transaccion, regla_riesgo_id, tipo_hallazgo, descripcion, score, severidad, detalle_json)
SELECT a.empresa_id, a.id, a.transaccion_id, a.fecha_transaccion, rr.id,
       'REGLA_DISPARADA', COALESCE(regla->>'codigo','Regla disparada'), COALESCE((regla->>'score')::numeric, a.score), a.severidad,
       jsonb_build_object('fuente','reglas_disparadas_json','regla',regla)
FROM alertas_antifraude a
CROSS JOIN LATERAL jsonb_array_elements(a.reglas_disparadas_json) regla
LEFT JOIN reglas_riesgo rr ON rr.empresa_id = a.empresa_id AND rr.codigo = 'REG-PY-' || lpad(((a.id - 1) % 30 + 1)::text, 2, '0')
ON CONFLICT DO NOTHING;

INSERT INTO coincidencia_lista_alerta (empresa_id, alerta_id, sujeto_riesgo_id, lista_regulatoria_id, tipo_coincidencia, porcentaje_coincidencia, detalle_json)
SELECT a.empresa_id, a.id, sr.id, sr.lista_regulatoria_id, 'NOMBRE_APROXIMADO', 82 + (a.id % 15), jsonb_build_object('algoritmo','demo_fuzzy','alerta',a.codigo)
FROM alertas_antifraude a
JOIN sujeto_riesgo sr ON sr.codigo = 'SR-DEMO-' || ((a.id % 40) + 1)
WHERE a.id % 2 = 0
ON CONFLICT DO NOTHING;

INSERT INTO transaccion_detalle_snapshot (empresa_id, transaccion_id, fecha_transaccion, snapshot_json, fuente)
SELECT t.empresa_id, t.id, t.fecha_transaccion,
       jsonb_build_object('codigo', t.codigo, 'monto', t.monto, 'moneda', m.codigo_iso, 'participantes', jsonb_build_object('remitente', t.nombre_remitente, 'beneficiario', t.nombre_beneficiario)),
       'CORE_TRANSACCIONAL'
FROM transacciones t
JOIN moneda m ON m.id = t.moneda_id
WHERE t.empresa_id = '11111111-1111-1111-1111-111111111111'
ON CONFLICT DO NOTHING;

INSERT INTO cliente_snapshot_alerta (empresa_id, alerta_id, persona_id, snapshot_json)
SELECT a.empresa_id, a.id, t.persona_remitente_id,
       jsonb_build_object('estadoApi','API externa no disponible','personal',jsonb_build_object('nombre',t.nombre_remitente),'laboral',jsonb_build_object('ocupacion','Demo'),'judicial',jsonb_build_object('antecedentes','No verificado'))
FROM alertas_antifraude a
JOIN transacciones t ON t.id = a.transaccion_id AND t.fecha_transaccion = a.fecha_transaccion
ON CONFLICT DO NOTHING;

INSERT INTO consulta_kyc_alerta (empresa_id, alerta_id, proveedor, estado, mensaje, respuesta_json)
SELECT a.empresa_id, a.id, 'Proveedor KYC Demo', CASE WHEN a.id % 3 = 0 THEN 'TIMEOUT' ELSE 'API_NO_DISPONIBLE' END, 'Servicio externo no disponible en ambiente de tesis', '{}'
FROM alertas_antifraude a
ON CONFLICT DO NOTHING;

INSERT INTO historial_asignacion (empresa_id, alerta_id, usuario_anterior_id, usuario_nuevo_id, tipo, motivo, observacion)
SELECT a.empresa_id, a.id, NULL, a.analista_asignado_id, 'ASIGNACION', 'Carga demo inicial', 'Asignacion operativa para pruebas'
FROM alertas_antifraude a
WHERE a.analista_asignado_id IS NOT NULL
ON CONFLICT DO NOTHING;

INSERT INTO caso (empresa_id, codigo, titulo, descripcion, estado, severidad, responsable_id)
SELECT a.empresa_id, 'CAS-' || a.codigo, 'Investigacion ' || a.codigo, a.motivo, CASE WHEN a.estado = 'PENDIENTE_APROBACION' THEN 'PENDIENTE_APROBACION' ELSE 'EN_INVESTIGACION' END, a.severidad, a.analista_asignado_id
FROM alertas_antifraude a
ON CONFLICT (empresa_id, codigo) DO UPDATE SET estado = EXCLUDED.estado, responsable_id = EXCLUDED.responsable_id;

INSERT INTO caso_alerta (empresa_id, caso_id, alerta_id)
SELECT c.empresa_id, c.id, a.id
FROM caso c
JOIN alertas_antifraude a ON c.codigo = 'CAS-' || a.codigo AND c.empresa_id = a.empresa_id
ON CONFLICT (empresa_id, caso_id, alerta_id) DO NOTHING;

INSERT INTO actuacion (empresa_id, caso_id, usuario_id, tipo_actuacion, descripcion)
SELECT c.empresa_id, c.id, c.responsable_id, 'REVISION_INICIAL', 'Revision inicial del caso y validacion de regla disparada.'
FROM caso c
ON CONFLICT DO NOTHING;

INSERT INTO comentario_caso (empresa_id, caso_id, usuario_id, comentario, visibilidad)
SELECT c.empresa_id, c.id, c.responsable_id, 'Se solicita validar origen de fondos y consistencia del perfil transaccional.', 'INTERNA'
FROM caso c
ON CONFLICT DO NOTHING;

INSERT INTO evidencia (empresa_id, caso_id, nombre, descripcion, tipo_archivo, extension, mime_type, tamanio_bytes, estado, hash_archivo, referencia_archivo, cargado_por_id)
SELECT c.empresa_id, c.id, 'Evidencia Demo ' || c.id || '.pdf', 'Metadata de evidencia sintetica para pruebas AML.', 'PDF', 'pdf', 'application/pdf', 102400 + c.id, 'CARGADA', hmac(c.codigo, 'regula-demo-hmac-key', 'sha256'), 'evidencias/' || c.codigo || '.pdf', c.responsable_id
FROM caso c
ON CONFLICT DO NOTHING;

INSERT INTO evidencia_alerta (empresa_id, alerta_id, evidencia_id)
SELECT e.empresa_id, ca.alerta_id, e.id
FROM evidencia e
JOIN caso_alerta ca ON ca.caso_id = e.caso_id
ON CONFLICT (empresa_id, alerta_id, evidencia_id) DO NOTHING;

INSERT INTO historial_estado_caso (empresa_id, caso_id, estado_anterior, estado_nuevo, motivo, usuario_id)
SELECT c.empresa_id, c.id, NULL, c.estado, 'Carga inicial demo de caso AML', c.responsable_id
FROM caso c
ON CONFLICT DO NOTHING;

INSERT INTO resolucion_alerta (empresa_id, alerta_id, analista_id, resultado, conclusion, justificacion, contacto_cliente, fondos_retenidos, fondos_liberables, requiere_ros, requiere_bloqueo, requiere_escalamiento_legal, estado)
SELECT a.empresa_id, a.id, a.analista_asignado_id,
       CASE WHEN a.score >= 85 THEN 'FRAUDE_CONFIRMADO' ELSE 'OPERACION_JUSTIFICADA' END,
       'Conclusion demo: se documenta analisis preliminar con evidencia y se eleva a supervisor.',
       'La decision se basa en score, reglas disparadas e historial transaccional.',
       'Contacto pendiente con cliente.',
       a.score >= 85, a.score < 85, a.score >= 85, a.score >= 90, a.score >= 90,
       'PENDIENTE_APROBACION'
FROM alertas_antifraude a
WHERE a.estado = 'PENDIENTE_APROBACION' AND a.analista_asignado_id IS NOT NULL
ON CONFLICT DO NOTHING;

INSERT INTO aprobacion_supervisor (empresa_id, alerta_id, resolucion_alerta_id, supervisor_id, decision, observacion, motivo_rechazo, faltantes)
SELECT r.empresa_id, r.alerta_id, r.id, u.id,
       CASE WHEN r.id % 2 = 0 THEN 'APROBADA' ELSE 'RECHAZADA' END,
       'Revision demo de supervisor.',
       CASE WHEN r.id % 2 = 0 THEN NULL ELSE 'Falta respaldo documental suficiente.' END,
       CASE WHEN r.id % 2 = 0 THEN NULL ELSE 'Adjuntar comprobante de origen de fondos y comunicacion con cliente.' END
FROM resolucion_alerta r
JOIN usuarios u ON u.email = 'hector.sosa@cliente.local'
ON CONFLICT DO NOTHING;

INSERT INTO decision_caso (empresa_id, caso_id, resolucion_alerta_id, decision, descripcion, ejecutada)
SELECT c.empresa_id, c.id, r.id, CASE WHEN r.resultado = 'FRAUDE_CONFIRMADO' THEN 'RETENER_Y_REPORTAR' ELSE 'LIBERAR_MOVIMIENTO' END,
       'Decision demo asociada a resolucion de alerta.', false
FROM caso c
JOIN caso_alerta ca ON ca.caso_id = c.id
LEFT JOIN resolucion_alerta r ON r.alerta_id = ca.alerta_id
WHERE r.id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM decision_caso d0
      WHERE d0.empresa_id = c.empresa_id
        AND d0.caso_id = c.id
        AND d0.resolucion_alerta_id = r.id
  )
ON CONFLICT DO NOTHING;

INSERT INTO reportes_ros (empresa_id, caso_id, codigo, estado, descripcion_sospecha, soporte_referencia, reporte_json)
SELECT DISTINCT ON (c.empresa_id, c.codigo)
       c.empresa_id,
       c.id,
       'ROS-' || c.codigo,
       'BORRADOR',
       'Reporte ROS demo generado cuando el analisis requiere comunicacion regulatoria.',
       'soportes/' || c.codigo,
       jsonb_build_object('demo', true)
FROM caso c
JOIN decision_caso d ON d.caso_id = c.id AND d.decision = 'RETENER_Y_REPORTAR'
ORDER BY c.empresa_id, c.codigo, d.id
ON CONFLICT (empresa_id, codigo) DO UPDATE SET estado = EXCLUDED.estado;

INSERT INTO estadistica_carga_analista (empresa_id, usuario_id, periodo, alertas_asignadas, alertas_cerradas, alertas_pendientes, tiempo_promedio_minutos)
SELECT du.empresa_id, du.usuario_id, DATE '2026-07-01', du.carga_actual, du.carga_actual / 2, du.carga_actual - (du.carga_actual / 2), 95.5
FROM disponibilidad_usuario du
ON CONFLICT (empresa_id, usuario_id, periodo) DO UPDATE SET alertas_asignadas = EXCLUDED.alertas_asignadas;

INSERT INTO servicio_externo (codigo, nombre, tipo_servicio, url_base, estado, configuracion_json)
VALUES
('KYC_PERSONA', 'Consulta KYC Persona', 'KYC', 'https://api.demo.local/kyc/persona', 'NO_DISPONIBLE', '{"ambiente":"demo"}'),
('SCREENING_LISTAS', 'Screening Listas', 'SCREENING', 'https://api.demo.local/screening', 'NO_DISPONIBLE', '{"ambiente":"demo"}'),
('DOCUMENTOS_CLIENTE', 'Documentos Cliente', 'DOCUMENTOS', 'https://api.demo.local/documentos', 'NO_DISPONIBLE', '{"ambiente":"demo"}'),
('CORE_BANCARIO', 'Core Bancario', 'CORE', 'https://api.demo.local/core', 'NO_DISPONIBLE', '{"ambiente":"demo"}'),
('ROS_REGULATORIO', 'ROS Regulatorio', 'REGULATORIO', 'https://api.demo.local/ros', 'NO_DISPONIBLE', '{"ambiente":"demo"}')
ON CONFLICT (codigo) DO UPDATE SET estado = EXCLUDED.estado;

INSERT INTO api_evento (empresa_id, origen, direccion, servicio, endpoint, metodo_http, status_http, mensaje, resultado, categoria_error, correlation_id, referencia_entidad, referencia_id, detalle_json, estado)
SELECT a.empresa_id, 'EXTERNA', 'SALIENTE', se.codigo, se.tipo_servicio, 'GET', 503, 'Servicio externo no disponible en ambiente demo', 'ERROR', 'CONEXION_O_RESPUESTA', 'seed-' || a.codigo || '-' || se.codigo, 'api_externa', a.id::text, '{}', 'ERROR'
FROM alertas_antifraude a
JOIN cliente_snapshot_alerta cs ON cs.alerta_id = a.id
CROSS JOIN servicio_externo se
WHERE se.codigo IN ('KYC_PERSONA','SCREENING_LISTAS')
ON CONFLICT DO NOTHING;

INSERT INTO auditoria_sistema (empresa_id, usuario_id, accion, descripcion, entidad_afectada, entidad_id, valor_nuevo_json, direccion_ip, user_agent)
SELECT '11111111-1111-1111-1111-111111111111'::uuid,
       COALESCE(a.analista_asignado_id, (SELECT id FROM usuarios WHERE email = 'hector.sosa@cliente.local')),
       'SEED_' || (ARRAY['CREAR','ASIGNAR','EVALUAR','ADJUNTAR_EVIDENCIA','PROPONER_RESOLUCION'])[1 + (gs.n % 5)],
       'Auditoria funcional demo para trazabilidad del ecosistema completo.',
       (ARRAY['alertas_antifraude','caso','evidencia','resolucion_alerta','reglas_riesgo'])[1 + (gs.n % 5)],
       gs.n::text,
       jsonb_build_object('seed', true, 'evento', gs.n),
       '127.0.0.1',
       'RegulaSeed/2026'
FROM generate_series(1,120) gs(n)
LEFT JOIN alertas_antifraude a ON a.id = ((gs.n - 1) % GREATEST((SELECT count(*) FROM alertas_antifraude), 1)) + 1
ON CONFLICT DO NOTHING;

COMMIT;
