-- Poblacion academica realista para una instalacion on-premise aislada.
-- Solo se carga con el perfil demo. Todos los sujetos, documentos y eventos son sinteticos.
-- Estas filas minimas hacen que el repeatable sea autonomo aun cuando Flyway lo
-- ordene antes del seed de escenarios deterministas.
INSERT INTO empresa(id,codigo,nombre,ruc,estado)
VALUES('00000000-0000-0000-0000-000000000001','REGULA_DEMO','Empresa academica Regula','80000000-0','ACTIVA')
ON CONFLICT(id) DO UPDATE
SET codigo = EXCLUDED.codigo,
    nombre = EXCLUDED.nombre,
    ruc = EXCLUDED.ruc,
    estado = EXCLUDED.estado;
INSERT INTO usuarios(id,email,nombre,password_hash,activo) VALUES
('00000000-0000-0000-0000-000000000101','admin@demo.regula.local','Administracion Academica',crypt('RegulaDemo2026!',gen_salt('bf')),true),
('00000000-0000-0000-0000-000000000102','analista@demo.regula.local','Analista Academico',crypt('RegulaDemo2026!',gen_salt('bf')),true)
ON CONFLICT(id) DO UPDATE SET email=EXCLUDED.email,nombre=EXCLUDED.nombre,activo=true;
SELECT set_config('app.current_empresa_id','00000000-0000-0000-0000-000000000001',false);
SELECT set_config('app.current_usuario_id','00000000-0000-0000-0000-000000000101',false);

-- Catalogos Paraguay derivados del ultimo corte Regula Clean.
INSERT INTO moneda(codigo_iso,nombre,nombre_en,fuente,activo) VALUES
('PYG','Guarani Paraguayo','Paraguayan guarani','ISO 4217 demo',true),
('USD','Dolar Estadounidense','US dollar','ISO 4217 demo',true),
('BRL','Real Brasileno','Brazilian real','ISO 4217 demo',true),
('EUR','Euro','Euro','ISO 4217 demo',true)
ON CONFLICT(codigo_iso) DO UPDATE SET nombre=EXCLUDED.nombre,activo=true;

INSERT INTO pais(codigo_iso,codigo_iso3,nombre,activo) VALUES
('PY','PRY','Paraguay',true),('AR','ARG','Argentina',true),('BR','BRA','Brasil',true),
('US','USA','Estados Unidos',true),('ES','ESP','Espana',true),('GB','GBR','Reino Unido',true),
('HK','HKG','Hong Kong',true)
ON CONFLICT(codigo_iso) DO UPDATE SET codigo_iso3=EXCLUDED.codigo_iso3,nombre=EXCLUDED.nombre,activo=true;

INSERT INTO nivel_riesgo(codigo,nombre,orden) VALUES
('BAJO','Bajo',1),('MEDIO','Medio',2),('ALTO','Alto',3),('CRITICO','Critico',4)
ON CONFLICT(codigo) DO UPDATE SET nombre=EXCLUDED.nombre,orden=EXCLUDED.orden;

INSERT INTO canal_transaccion(codigo,nombre,descripcion) VALUES
('SPI','Sistema De Pagos Instantaneos','Transferencias 24/7 Paraguay'),
('LBTR','Liquidacion Bruta En Tiempo Real','Transferencias interbancarias de alto valor'),
('ACH','Camara Automatizada','Pagos por lotes'),('EMPE','Medio De Pago Electronico','Billeteras'),
('TARJETA','Tarjetas','Credito, debito y prepaga'),('QR','Pago QR','QR interoperable'),
('ATM','Cajero Automatico','Retiros y depositos'),('CAJA','Sucursal','Operacion presencial'),
('REMESA','Remesas','Envio y cobro de remesas'),('CAMBIO','Cambio','Operacion FX'),
('COMEX','Comercio Exterior','Pagos internacionales')
ON CONFLICT(codigo) DO UPDATE SET nombre=EXCLUDED.nombre,descripcion=EXCLUDED.descripcion;

INSERT INTO tipo_transaccion(codigo,nombre,categoria,descripcion) VALUES
('PY_SPI_TRANSFER','Transferencia SPI','SPI','Pago instantaneo nacional'),
('PY_SPI_ALIAS_TRANSFER','Transferencia SPI Con Alias','SPI','Pago mediante alias'),
('PY_LBTR_HIGH_VALUE','Transferencia Alto Valor LBTR','LBTR','Transferencia de alto valor'),
('PY_ACH_BATCH_CREDIT','Credito ACH Por Lotes','ACH','Credito masivo'),
('PY_PAYROLL_ACH','Pago De Salarios ACH','ACH','Nomina'),
('PY_QR_EMV_PAYMENT','Pago QR EMV','QR','Pago QR interoperable'),
('PY_CARD_PURCHASE_DEBIT','Compra Tarjeta Debito','TARJETA','Compra con debito'),
('PY_CARD_PURCHASE_CREDIT','Compra Tarjeta Credito','TARJETA','Compra con credito'),
('PY_ATM_WITHDRAWAL','Retiro ATM','ATM','Retiro de efectivo'),
('PY_CASH_IN_BRANCH','Deposito Efectivo Sucursal','CAJA','Deposito presencial'),
('PY_EMPE_WALLET_TOPUP','Carga Billetera EMPE','EMPE','Carga de dinero electronico'),
('PY_EMPE_WALLET_P2P','Transferencia Billetera P2P','EMPE','Transferencia P2P'),
('PY_REMITTANCE_SEND','Envio De Remesa','REMESA','Envio internacional'),
('PY_REMITTANCE_RECEIVE','Cobro De Remesa','REMESA','Cobro local'),
('PY_FX_EXCHANGE','Cambio De Moneda','CAMBIO','Operacion FX'),
('PY_TRADE_FINANCE_PAYMENT','Pago Comercio Exterior','COMEX','Pago importacion/exportacion')
ON CONFLICT(codigo) DO UPDATE SET nombre=EXCLUDED.nombre,categoria=EXCLUDED.categoria,descripcion=EXCLUDED.descripcion;

INSERT INTO producto(codigo,nombre,activo) VALUES
('CUENTA_CORRIENTE','Cuenta Corriente',true),
('CAJA_AHORRO','Caja De Ahorro',true),
('TARJETA_CREDITO','Tarjeta De Credito',true),
('BILLETERA','Billetera Electronica',true)
ON CONFLICT(codigo) DO UPDATE SET nombre=EXCLUDED.nombre,activo=true;

INSERT INTO banco_emisor(codigo,nombre,tipo_entidad,participante_sipap) VALUES
('BCP','Banco Central Del Paraguay','BCP',true),
('BANCO_DEMO_REGULA','Banco Demo Regula','BANCO',true),
('FINANCIERA_DEMO','Financiera Demo','FINANCIERA',true),
('COOP_DEMO','Cooperativa Demo','COOPERATIVA',true),
('EMPE_DEMO','EMPE Demo Patrocinada','EMPE',true)
ON CONFLICT(codigo) DO UPDATE SET nombre=EXCLUDED.nombre,tipo_entidad=EXCLUDED.tipo_entidad,participante_sipap=EXCLUDED.participante_sipap;

INSERT INTO procesadora_tarjeta(codigo,nombre) VALUES
('BANCARD','Bancard'),('PROCARD','Procard'),('PROCESADORA_DEMO','Procesadora Demo')
ON CONFLICT(codigo) DO UPDATE SET nombre=EXCLUDED.nombre;

INSERT INTO empe_operador(codigo,nombre,entidad_patrocinadora_id)
SELECT v.codigo,v.nombre,b.id FROM (VALUES
('TIGO_MONEY','Tigo Money'),('PERSONAL_PAY','Personal Pay Demo'),
('ZIMPLE','Zimple Demo'),('MANGO','Mango Demo'))v(codigo,nombre)
JOIN banco_emisor b ON b.codigo='EMPE_DEMO'
ON CONFLICT(codigo) DO UPDATE SET nombre=EXCLUDED.nombre,entidad_patrocinadora_id=EXCLUDED.entidad_patrocinadora_id;

INSERT INTO tipo_documento(codigo,nombre,descripcion,pais_relacion_id,tipo_persona,fuente_oficial,activo) VALUES
('CI_PY','Cedula De Identidad','Documento paraguayo',(SELECT id FROM pais WHERE codigo_iso='PY'),'FISICA','Identificaciones demo',true),
('RUC_PY','Registro Unico Del Contribuyente','Identificador tributario',(SELECT id FROM pais WHERE codigo_iso='PY'),'JURIDICA','DNIT demo',true),
('PASAPORTE','Pasaporte','Documento de viaje',NULL,'FISICA','Proveedor demo',true)
ON CONFLICT(codigo) DO UPDATE SET nombre=EXCLUDED.nombre,activo=true;

-- RBAC completo para recorrer la aplicacion como una instalacion real.
INSERT INTO rol(codigo,nombre,descripcion,alcance,tipo) VALUES
('ADMIN_DEMO','Administrador demo','Rol integral no productivo','EMPRESA','EMPRESA'),
('ANALISTA_DEMO','Analista demo','Rol de investigacion no productivo','EMPRESA','EMPRESA')
ON CONFLICT(codigo) DO UPDATE SET nombre=EXCLUDED.nombre,descripcion=EXCLUDED.descripcion;
INSERT INTO permiso(codigo,nombre,descripcion,modulo,accion)
SELECT v.codigo,v.nombre,'Permiso funcional del entorno demo',v.modulo,v.accion FROM (VALUES
('EMPRESAS_VER','Ver Empresas','SaaS','VER'),('EMPRESAS_EDITAR','Editar Empresas','SaaS','EDITAR'),
('LICENCIAS_VER','Ver Licencias','Licencias','VER'),('LICENCIAS_GESTIONAR','Gestionar Licencias','Licencias','GESTIONAR'),
('PAGOS_VER','Ver Pagos','Pagos','VER'),('PAGOS_GESTIONAR','Gestionar Pagos','Pagos','GESTIONAR'),
('USUARIOS_VER','Ver Usuarios','Usuarios','VER'),('USUARIOS_CREAR','Crear Usuarios','Usuarios','CREAR'),
('USUARIOS_EDITAR','Editar Usuarios','Usuarios','EDITAR'),('REGLAS_VER','Ver Reglas','Motor','VER'),
('REGLAS_CREAR','Crear Reglas','Motor','CREAR'),('REGLAS_EDITAR','Editar Reglas','Motor','EDITAR'),
('REGLAS_ACTIVAR','Activar Reglas','Motor','ACTIVAR'),('CATALOGOS_VER','Ver Catalogos','Catalogos','VER'),
('ALERTAS_VER','Ver Alertas','Alertas','VER'),('ALERTAS_ASIGNAR','Asignar Alertas','Alertas','ASIGNAR'),
('ALERTAS_RESOLVER','Resolver Alertas','Alertas','RESOLVER'),('ALERTAS_APROBAR','Aprobar Alertas','Alertas','APROBAR'),
('CASOS_VER','Ver Casos','Casos','VER'),('CASOS_GESTIONAR','Gestionar Casos','Casos','GESTIONAR'),
('REPORTES_VER','Ver Reportes','Reportes','VER'),('REPORTES_GENERAR','Generar Reportes','Reportes','GENERAR'),
('AUDITORIA_VER','Ver Auditoria','Auditoria','VER')
)v(codigo,nombre,modulo,accion)
ON CONFLICT(codigo) DO UPDATE SET nombre=EXCLUDED.nombre,modulo=EXCLUDED.modulo,accion=EXCLUDED.accion;

INSERT INTO rol_permiso(rol_id,permiso_id)
SELECT r.id,p.id FROM rol r CROSS JOIN permiso p
WHERE (r.codigo='ADMIN_DEMO')
   OR (r.codigo='ANALISTA_DEMO' AND p.codigo IN ('ALERTAS_VER','ALERTAS_ASIGNAR','ALERTAS_RESOLVER','CASOS_VER','CASOS_GESTIONAR','REPORTES_VER','AUDITORIA_VER'))
ON CONFLICT DO NOTHING;

INSERT INTO usuarios(id,email,nombre,password_hash,activo) VALUES
('00000000-0000-0000-0000-000000000103','supervisor@demo.regula.local','Supervisor Cumplimiento Demo',crypt('RegulaDemo2026!',gen_salt('bf')),true),
('00000000-0000-0000-0000-000000000104','auditor@demo.regula.local','Auditor Interno Demo',crypt('RegulaDemo2026!',gen_salt('bf')),true),
('00000000-0000-0000-0000-000000000105','analista2@demo.regula.local','Segundo Analista Demo',crypt('RegulaDemo2026!',gen_salt('bf')),true)
ON CONFLICT(id) DO UPDATE SET email=EXCLUDED.email,nombre=EXCLUDED.nombre,activo=true;

INSERT INTO usuario_empresa(empresa_id,usuario_id,rol_id,estado)
SELECT '00000000-0000-0000-0000-000000000001',u.id,r.id,'ACTIVO'
FROM (VALUES
('supervisor@demo.regula.local','ADMIN_DEMO'),('auditor@demo.regula.local','ANALISTA_DEMO'),
('analista2@demo.regula.local','ANALISTA_DEMO'))v(email,rol_codigo)
JOIN usuarios u ON u.email=v.email JOIN rol r ON r.codigo=v.rol_codigo
ON CONFLICT DO NOTHING;

INSERT INTO perfil_usuario(empresa_id,usuario_id,cargo,area,telefono)
SELECT '00000000-0000-0000-0000-000000000001',u.id,v.cargo,v.area,v.telefono
FROM (VALUES
('admin@demo.regula.local','Administrador de plataforma','Tecnologia','+595981000101'),
('supervisor@demo.regula.local','Oficial de cumplimiento','Cumplimiento','+595981000103'),
('analista@demo.regula.local','Analista AML I','Prevencion','+595981000102'),
('analista2@demo.regula.local','Analista AML II','Prevencion','+595981000105'),
('auditor@demo.regula.local','Auditor interno','Auditoria','+595981000104'))v(email,cargo,area,telefono)
JOIN usuarios u ON u.email=v.email
ON CONFLICT(empresa_id,usuario_id) DO UPDATE SET cargo=EXCLUDED.cargo,area=EXCLUDED.area,telefono=EXCLUDED.telefono;

INSERT INTO disponibilidad_usuario(empresa_id,usuario_id,estado,carga_actual,capacidad_maxima)
SELECT '00000000-0000-0000-0000-000000000001',u.id,'DISPONIBLE',v.carga,v.capacidad
FROM (VALUES('analista@demo.regula.local',3,20),('analista2@demo.regula.local',2,20),('supervisor@demo.regula.local',1,30))v(email,carga,capacidad)
JOIN usuarios u ON u.email=v.email
ON CONFLICT(empresa_id,usuario_id) DO UPDATE SET estado=EXCLUDED.estado,carga_actual=EXCLUDED.carga_actual,capacidad_maxima=EXCLUDED.capacidad_maxima;

INSERT INTO horario_laboral_usuario(empresa_id,usuario_id,dia_semana,hora_inicio,hora_fin)
SELECT '00000000-0000-0000-0000-000000000001',u.id,d,'08:00','17:00'
FROM usuarios u CROSS JOIN generate_series(1,5)d
WHERE u.email IN ('analista@demo.regula.local','analista2@demo.regula.local','supervisor@demo.regula.local')
ON CONFLICT DO NOTHING;

-- Modelo comercial y simulacion local de una licencia Professional.
INSERT INTO plan_licencia(codigo,nombre,descripcion,precio_anual,moneda_id,limite_usuarios,limite_transacciones_mes,limite_consultas_kyc_mes,modulos_json,limite_consultas_kyc_mensuales,limite_reportes_mensuales,limite_transacciones_mensuales,modulos_incluidos_json) VALUES
('CORE','Core','Operacion antifraude esencial',12000,(SELECT id FROM moneda WHERE codigo_iso='USD'),15,100000,2000,'["TRANSACCIONES","ALERTAS"]',2000,100,100000,'["TRANSACCIONES","ALERTAS"]'),
('PROFESSIONAL','Professional','Operacion AML completa',24000,(SELECT id FROM moneda WHERE codigo_iso='USD'),50,1000000,20000,'["TRANSACCIONES","ALERTAS","KYC","CASOS","ROS"]',20000,1000,1000000,'["TRANSACCIONES","ALERTAS","KYC","CASOS","ROS"]'),
('ENTERPRISE','Enterprise','Capacidad e integraciones personalizadas',0,(SELECT id FROM moneda WHERE codigo_iso='USD'),500,10000000,200000,'["TODOS"]',200000,10000,10000000,'["TODOS"]')
ON CONFLICT(codigo) DO UPDATE SET nombre=EXCLUDED.nombre,precio_anual=EXCLUDED.precio_anual,limite_usuarios=EXCLUDED.limite_usuarios,limite_transacciones_mes=EXCLUDED.limite_transacciones_mes,limite_consultas_kyc_mes=EXCLUDED.limite_consultas_kyc_mes;

INSERT INTO suscripcion(empresa_id,plan_licencia_id,codigo,estado,fecha_inicio,fecha_fin,renovacion_automatica,observacion)
SELECT '00000000-0000-0000-0000-000000000001',id,'SUB-DEMO-2026','ACTIVA',DATE '2026-01-01',DATE '2026-12-31',true,'Suscripcion sintetica para pruebas' FROM plan_licencia WHERE codigo='PROFESSIONAL'
ON CONFLICT(codigo) DO UPDATE SET estado=EXCLUDED.estado,fecha_fin=EXCLUDED.fecha_fin;

INSERT INTO contrato(empresa_id,suscripcion_id,numero_contrato,tipo_contrato,estado,fecha_firma,fecha_vigencia_desde,fecha_vigencia_hasta,documento_referencia,hash_documento,observaciones)
SELECT s.empresa_id,s.id,'CTR-DEMO-2026-001','LICENCIA_ANUAL','VIGENTE',DATE '2025-12-15',s.fecha_inicio,s.fecha_fin,'contratos/demo-2026.pdf',hmac('CTR-DEMO-2026-001','regula-demo-hmac-key','sha256'),'Documento inexistente; metadato sintetico' FROM suscripcion s WHERE s.codigo='SUB-DEMO-2026'
ON CONFLICT(numero_contrato) DO UPDATE SET estado=EXCLUDED.estado,fecha_vigencia_hasta=EXCLUDED.fecha_vigencia_hasta;

INSERT INTO pago(empresa_id,suscripcion_id,codigo,fecha_pago,monto,moneda_id,estado,metodo_pago,comprobante_referencia)
SELECT s.empresa_id,s.id,'PAG-DEMO-2026-001',TIMESTAMPTZ '2025-12-20 10:00:00-03',24000,(SELECT id FROM moneda WHERE codigo_iso='USD'),'CONFIRMADO','TRANSFERENCIA','COMP-DEMO-001' FROM suscripcion s WHERE s.codigo='SUB-DEMO-2026'
ON CONFLICT(codigo) DO UPDATE SET estado=EXCLUDED.estado,monto=EXCLUDED.monto;

INSERT INTO uso_suscripcion(empresa_id,suscripcion_id,periodo,usuarios_activos,transacciones_procesadas,consultas_kyc,alertas_generadas,reportes_generados,consumo_json,anio,mes)
SELECT s.empresa_id,s.id,make_date(2026,m,1),5,1000*m,20*m,5*m,m/2,jsonb_build_object('demo',true,'mes',m),2026,m FROM suscripcion s CROSS JOIN generate_series(1,8)m WHERE s.codigo='SUB-DEMO-2026'
ON CONFLICT(empresa_id,suscripcion_id,periodo) DO UPDATE SET transacciones_procesadas=EXCLUDED.transacciones_procesadas,consultas_kyc=EXCLUDED.consultas_kyc;

INSERT INTO instalacion_local(id,empresa_id,identificador_instalacion,fingerprint_hash,clave_publica_pem,estado,version_producto,activada_en,ultimo_heartbeat_en)
VALUES('00000000-0000-0000-0000-000000009001','00000000-0000-0000-0000-000000000001','INST-DEMO-ASUNCION-01','sha256:demo-fingerprint-no-productivo','-----BEGIN PUBLIC KEY-----\nDEMO-NO-CRIPTOGRAFICO\n-----END PUBLIC KEY-----','ACTIVA','1.0.0-demo',TIMESTAMPTZ '2026-01-02 09:00:00-03',TIMESTAMPTZ '2026-08-05 21:00:00-03')
ON CONFLICT(id) DO UPDATE SET empresa_id=EXCLUDED.empresa_id,identificador_instalacion=EXCLUDED.identificador_instalacion,estado=EXCLUDED.estado,version_producto=EXCLUDED.version_producto,ultimo_heartbeat_en=EXCLUDED.ultimo_heartbeat_en;

INSERT INTO licencia_local(id,instalacion_id,suscripcion_referencia,plan_codigo,plan_version,estado,emitida_en,vence_en,dias_gracia,modulos_json,limites_json,lease_payload,lease_firma,kid_firma,ultima_validacion_en)
VALUES('00000000-0000-0000-0000-000000009101','00000000-0000-0000-0000-000000009001','SUB-DEMO-2026','PROFESSIONAL',1,'ACTIVA',TIMESTAMPTZ '2026-08-05 20:00:00-03',TIMESTAMPTZ '2026-12-31 23:59:59-03',15,'["TRANSACCIONES","ALERTAS","KYC","CASOS","ROS"]','{"usuarios":50,"transaccionesMes":1000000,"consultasKycMes":20000}','eyJkZW1vIjp0cnVlLCJub1ZhbGlkYXJGaXJtYSI6dHJ1ZX0','DEMO_SIGNATURE_NOT_CRYPTOGRAPHIC','demo-key-2026',TIMESTAMPTZ '2026-08-05 21:00:00-03')
ON CONFLICT(id) DO UPDATE SET suscripcion_referencia=EXCLUDED.suscripcion_referencia,plan_codigo=EXCLUDED.plan_codigo,estado=EXCLUDED.estado,emitida_en=EXCLUDED.emitida_en,vence_en=EXCLUDED.vence_en,ultima_validacion_en=EXCLUDED.ultima_validacion_en;

INSERT INTO evento_licencia_local(instalacion_id,licencia_id,tipo_evento,resultado,correlation_id,detalle_sanitizado_json,fecha_evento)
SELECT '00000000-0000-0000-0000-000000009001','00000000-0000-0000-0000-000000009101',v.tipo,v.resultado,v.correlation_id,'{"demo":true}',v.fecha
FROM (VALUES
('ACTIVACION','EXITOSA','demo-license-activation',TIMESTAMPTZ '2026-01-02 09:00:00-03'),
('HEARTBEAT','EXITOSO','demo-license-heartbeat',TIMESTAMPTZ '2026-08-05 21:00:00-03'))v(tipo,resultado,correlation_id,fecha)
WHERE NOT EXISTS(SELECT 1 FROM evento_licencia_local e WHERE e.correlation_id=v.correlation_id);

-- Personas, documentos y perfiles sinteticos adicionales.
INSERT INTO persona(empresa_id,tipo_persona,nombre_razon_social,documento_hash)
SELECT '00000000-0000-0000-0000-000000000001',CASE WHEN n%7=0 THEN 'JURIDICA' ELSE 'FISICA' END,
       CASE WHEN n%7=0 THEN 'Empresa Sintetica '||n||' S.A.' ELSE 'Cliente Sintetico '||lpad(n::text,3,'0') END,
       hmac('DOC-SINTETICO-'||n,'regula-demo-hmac-key','sha256') FROM generate_series(1,60)n
ON CONFLICT(empresa_id,documento_hash) DO NOTHING;

INSERT INTO documento(empresa_id,persona_id,tipo_documento_id,pais_emisor_id,numero_documento_enc,numero_documento_hash,es_principal,estado)
SELECT p.empresa_id,p.id,td.id,(SELECT id FROM pais WHERE codigo_iso='PY'),NULL,hmac('DOCUMENTO-'||p.id,'regula-demo-hmac-key','sha256'),true,'VIGENTE'
FROM persona p JOIN tipo_documento td ON td.codigo=CASE WHEN p.tipo_persona='JURIDICA' THEN 'RUC_PY' ELSE 'CI_PY' END
WHERE p.empresa_id='00000000-0000-0000-0000-000000000001'
ON CONFLICT(empresa_id,numero_documento_hash) DO NOTHING;

INSERT INTO perfil_cliente(empresa_id,persona_id,nivel_riesgo_id,segmento,actividad_economica,ingreso_mensual_estimado,volumen_mensual_esperado,cantidad_operaciones_mensual,perfil_json)
SELECT p.empresa_id,p.id,nr.id,CASE WHEN p.id%3=0 THEN 'EMPRESARIAL' ELSE 'PERSONAL' END,'Actividad economica sintetica',5000000+(p.id%10)*1000000,12000000+(p.id%20)*2000000,10+(p.id%30),jsonb_build_object('demo',true)
FROM persona p JOIN nivel_riesgo nr ON nr.codigo=CASE WHEN p.id%10=0 THEN 'ALTO' WHEN p.id%3=0 THEN 'MEDIO' ELSE 'BAJO' END
WHERE p.empresa_id='00000000-0000-0000-0000-000000000001'
ON CONFLICT(empresa_id,persona_id) DO UPDATE SET nivel_riesgo_id=EXCLUDED.nivel_riesgo_id,segmento=EXCLUDED.segmento;

-- Fuentes, listas y screening sintetico. No representan listas regulatorias reales.
INSERT INTO fuente_datos_riesgo(codigo,nombre,organismo,url_oficial,licencia_uso,frecuencia_actualizacion) VALUES
('LISTA_INTERNA_DEMO','Lista Interna Sintetica','Regula Demo','https://demo.invalid/lista','Solo tesis','Manual'),
('PEP_DEMO','PEP Sinteticos','Regula Demo','https://demo.invalid/pep','Solo tesis','Manual'),
('RIESGO_PAIS_DEMO','Riesgo Pais Sintetico','Regula Demo','https://demo.invalid/paises','Solo tesis','Mensual')
ON CONFLICT(codigo) DO UPDATE SET nombre=EXCLUDED.nombre;

INSERT INTO lista_regulatoria(fuente_datos_riesgo_id,codigo,nombre,tipo_lista,alcance,url_descarga,licencia_uso,activa,fecha_ultima_revision)
SELECT f.id,f.codigo||'_LIST',f.nombre,CASE WHEN f.codigo='PEP_DEMO' THEN 'PEP' WHEN f.codigo='RIESGO_PAIS_DEMO' THEN 'PAIS_RIESGO' ELSE 'SANCIONES' END,'ACADEMICO',f.url_oficial,'Solo datos sinteticos',true,DATE '2026-08-01'
FROM fuente_datos_riesgo f
ON CONFLICT(codigo) DO UPDATE SET nombre=EXCLUDED.nombre,fecha_ultima_revision=EXCLUDED.fecha_ultima_revision;

INSERT INTO elemento_lista(lista_regulatoria_id,tipo_elemento,valor_identificador,activo)
SELECT l.id,'DOCUMENTO','DEMO-ELEMENTO-'||n,true FROM lista_regulatoria l CROSS JOIN generate_series(1,5)n
ON CONFLICT DO NOTHING;

INSERT INTO sujeto_riesgo(lista_regulatoria_id,codigo,tipo_sujeto,nombre_normalizado,pais_id,tipo_riesgo,severidad,estado,detalle_json)
SELECT l.id,'SR-DEMO-'||n,CASE WHEN n%5=0 THEN 'ENTIDAD' ELSE 'PERSONA' END,'Sujeto Riesgo Sintetico '||n,(SELECT id FROM pais WHERE codigo_iso=CASE WHEN n%4=0 THEN 'BR' ELSE 'PY' END),CASE WHEN n%6=0 THEN 'PEP' ELSE 'LISTA_INTERNA' END,CASE WHEN n%7=0 THEN 'CRITICA' WHEN n%3=0 THEN 'ALTA' ELSE 'MEDIA' END,'ACTIVO',jsonb_build_object('demo',true)
FROM generate_series(1,40)n JOIN lista_regulatoria l ON l.codigo=CASE WHEN n%6=0 THEN 'PEP_DEMO_LIST' ELSE 'LISTA_INTERNA_DEMO_LIST' END
ON CONFLICT(codigo) DO UPDATE SET nombre_normalizado=EXCLUDED.nombre_normalizado,severidad=EXCLUDED.severidad;

INSERT INTO sujeto_riesgo_alias(sujeto_riesgo_id,alias_normalizado,tipo_alias)
SELECT s.id,s.nombre_normalizado||' Alias','ALIAS' FROM sujeto_riesgo s
WHERE NOT EXISTS(SELECT 1 FROM sujeto_riesgo_alias a WHERE a.sujeto_riesgo_id=s.id AND a.alias_normalizado=s.nombre_normalizado||' Alias');
INSERT INTO sujeto_riesgo_documento(sujeto_riesgo_id,tipo_documento_id,pais_emisor_id,numero_documento_hash,documento_enmascarado)
SELECT s.id,(SELECT id FROM tipo_documento WHERE codigo='CI_PY'),COALESCE(s.pais_id,(SELECT id FROM pais WHERE codigo_iso='PY')),hmac(s.codigo||'-DOC','regula-demo-hmac-key','sha256'),'***'||right(s.codigo,4) FROM sujeto_riesgo s
WHERE NOT EXISTS(SELECT 1 FROM sujeto_riesgo_documento d WHERE d.sujeto_riesgo_id=s.id AND d.numero_documento_hash=hmac(s.codigo||'-DOC','regula-demo-hmac-key','sha256'));
INSERT INTO sujeto_riesgo_relacion(sujeto_origen_id,sujeto_destino_id,tipo_relacion,descripcion)
SELECT s1.id,s2.id,'RELACION_COMERCIAL','Relacion sintetica' FROM sujeto_riesgo s1 JOIN sujeto_riesgo s2 ON s2.codigo='SR-DEMO-'||((substring(s1.codigo from '[0-9]+')::int%40)+1) WHERE s1.codigo IN ('SR-DEMO-1','SR-DEMO-2','SR-DEMO-3','SR-DEMO-4','SR-DEMO-5')
AND NOT EXISTS(SELECT 1 FROM sujeto_riesgo_relacion r WHERE r.sujeto_origen_id=s1.id AND r.sujeto_destino_id=s2.id AND r.tipo_relacion='RELACION_COMERCIAL');
INSERT INTO pais_riesgo(pais_id,categoria,severidad,motivo,fecha_inicio,activo,lista_regulatoria_id,nivel_riesgo_id)
SELECT p.id,'MONITOREO_DEMO',CASE WHEN p.codigo_iso='HK' THEN 'ALTA' ELSE 'MEDIA' END,'Clasificacion sintetica',DATE '2026-01-01',true,l.id,n.id FROM pais p CROSS JOIN lista_regulatoria l CROSS JOIN nivel_riesgo n WHERE p.codigo_iso IN ('HK','BR','AR') AND l.codigo='RIESGO_PAIS_DEMO_LIST' AND n.codigo=CASE WHEN p.codigo_iso='HK' THEN 'ALTO' ELSE 'MEDIO' END ON CONFLICT DO NOTHING;

-- Motor parametrizado.
INSERT INTO escenario(empresa_id,codigo,nombre,descripcion,severidad_base,activo)
SELECT '00000000-0000-0000-0000-000000000001','ESC-PY-'||lpad(n::text,2,'0'),'Escenario Paraguay '||n,'Escenario sintetico parametrizable',CASE WHEN n%4=0 THEN 'CRITICA' WHEN n%3=0 THEN 'ALTA' ELSE 'MEDIA' END,true FROM generate_series(1,12)n
ON CONFLICT(empresa_id,codigo) DO UPDATE SET nombre=EXCLUDED.nombre,severidad_base=EXCLUDED.severidad_base;
INSERT INTO accion(empresa_id,codigo,nombre,descripcion,tipo_accion,requiere_supervisor,activo)
SELECT '00000000-0000-0000-0000-000000000001','ACC-PY-'||lpad(n::text,2,'0'),'Accion Operativa '||n,'Accion sintetica',CASE WHEN n%4=0 THEN 'BLOQUEO' ELSE 'GENERAR_ALERTA' END,n%4=0,true FROM generate_series(1,12)n
ON CONFLICT(empresa_id,codigo) DO UPDATE SET nombre=EXCLUDED.nombre,tipo_accion=EXCLUDED.tipo_accion;
INSERT INTO reglas_riesgo(empresa_id,escenario_id,accion_id,codigo,nombre,descripcion,severidad,score_base,condiciones_json,acciones_json,activa,estado,version,condicion,parametros,prioridad,tipo_regla)
SELECT '00000000-0000-0000-0000-000000000001',e.id,a.id,'REG-PY-'||lpad(n::text,2,'0'),'Regla Paraguay '||n,'Regla demo',CASE WHEN n%4=0 THEN 'CRITICA' WHEN n%3=0 THEN 'ALTA' ELSE 'MEDIA' END,20+n*3,jsonb_build_object('montoMayorA',n*1000000),jsonb_build_object('accion','ALERTAR'),true,'ACTIVA',1,'monto > '||(n*1000000),jsonb_build_object('umbral',n*1000000),n,'MONTO'
FROM generate_series(1,20)n JOIN escenario e ON e.codigo='ESC-PY-'||lpad(((n-1)%12+1)::text,2,'0') AND e.empresa_id='00000000-0000-0000-0000-000000000001' JOIN accion a ON a.codigo='ACC-PY-'||lpad(((n-1)%12+1)::text,2,'0') AND a.empresa_id=e.empresa_id
ON CONFLICT(empresa_id,codigo,version) DO UPDATE SET activa=true,condiciones_json=EXCLUDED.condiciones_json;
INSERT INTO control_importe(empresa_id,codigo,nombre,tipo_transaccion_id,moneda_id,monto_minimo,monto_maximo,severidad)
SELECT '00000000-0000-0000-0000-000000000001','CIMP-'||lpad(n::text,2,'0'),'Control Importe '||n,t.id,m.id,n*1000000,n*1000000+900000,CASE WHEN n>8 THEN 'ALTA' ELSE 'MEDIA' END FROM generate_series(1,12)n CROSS JOIN LATERAL(SELECT id FROM tipo_transaccion WHERE codigo='PY_SPI_TRANSFER')t CROSS JOIN LATERAL(SELECT id FROM moneda WHERE codigo_iso='PYG')m ON CONFLICT(empresa_id,codigo) DO UPDATE SET monto_minimo=EXCLUDED.monto_minimo;
INSERT INTO control_frecuencia(empresa_id,codigo,nombre,ventana_minutos,cantidad_maxima,monto_acumulado_maximo,severidad)
SELECT '00000000-0000-0000-0000-000000000001','CFREC-'||lpad(n::text,2,'0'),'Control Frecuencia '||n,n*15,3+n%5,n*5000000,CASE WHEN n>8 THEN 'ALTA' ELSE 'MEDIA' END FROM generate_series(1,12)n ON CONFLICT(empresa_id,codigo) DO UPDATE SET ventana_minutos=EXCLUDED.ventana_minutos;
INSERT INTO horario_riesgo(empresa_id,codigo,nombre,hora_inicio,hora_fin,severidad)
SELECT '00000000-0000-0000-0000-000000000001','HR-'||lpad(n::text,2,'0'),'Horario Riesgoso '||n,make_time((n-1)%24,0,0),make_time(n%24,0,0),CASE WHEN n<=5 THEN 'ALTA' ELSE 'MEDIA' END FROM generate_series(1,12)n ON CONFLICT(empresa_id,codigo) DO UPDATE SET hora_inicio=EXCLUDED.hora_inicio;
INSERT INTO calendario_riesgo(empresa_id,fecha,nombre,tipo_evento,severidad)
SELECT '00000000-0000-0000-0000-000000000001',DATE '2026-01-01'+n*20,'Evento Riesgo '||n,CASE WHEN n%2=0 THEN 'FERIADO' ELSE 'CIERRE_MES' END,CASE WHEN n%4=0 THEN 'ALTA' ELSE 'MEDIA' END FROM generate_series(1,12)n ON CONFLICT(empresa_id,fecha,tipo_evento) DO UPDATE SET nombre=EXCLUDED.nombre;

-- Volumen transaccional repartido en meses para probar particiones y dashboard.
INSERT INTO transacciones(fecha_transaccion,empresa_id,codigo,tipo_transaccion_id,canal_transaccion_id,infraestructura_pago,monto,moneda_id,estado,estado_evaluacion,procesada,nombre_remitente,nombre_beneficiario,score_riesgo,nivel_riesgo_id,reglas_disparadas_json)
SELECT TIMESTAMPTZ '2026-01-01 08:00:00-03'+(n*interval '3 days'),
 '00000000-0000-0000-0000-000000000001','TX-PY-REAL-'||lpad(n::text,4,'0'),tt.id,ct.id,
 CASE WHEN n%4=0 THEN 'SIPAP' WHEN n%4=1 THEN 'TARJETA' WHEN n%4=2 THEN 'EMPE' ELSE 'CAJA' END,
 250000+n*475000,m.id,'COMPLETADA','EVALUADA',true,'Cliente Sintetico '||lpad(((n-1)%60+1)::text,3,'0'),'Beneficiario Sintetico '||n,
 CASE WHEN n%10=0 THEN 92 WHEN n%5=0 THEN 75 WHEN n%3=0 THEN 48 ELSE 18 END,nr.id,
 CASE WHEN n%5=0 THEN jsonb_build_array(jsonb_build_object('codigo','REG-PY-05','score',75)) ELSE '[]'::jsonb END
FROM generate_series(1,80)n
JOIN tipo_transaccion tt ON tt.codigo=CASE WHEN n%4=0 THEN 'PY_SPI_TRANSFER' WHEN n%4=1 THEN 'PY_CARD_PURCHASE_DEBIT' WHEN n%4=2 THEN 'PY_EMPE_WALLET_P2P' ELSE 'PY_CASH_IN_BRANCH' END
JOIN canal_transaccion ct ON ct.codigo=CASE WHEN n%4=0 THEN 'SPI' WHEN n%4=1 THEN 'TARJETA' WHEN n%4=2 THEN 'EMPE' ELSE 'CAJA' END
JOIN moneda m ON m.codigo_iso='PYG'
JOIN nivel_riesgo nr ON nr.codigo=CASE WHEN n%10=0 THEN 'CRITICO' WHEN n%5=0 THEN 'ALTO' WHEN n%3=0 THEN 'MEDIO' ELSE 'BAJO' END
WHERE NOT EXISTS(SELECT 1 FROM transacciones x WHERE x.empresa_id='00000000-0000-0000-0000-000000000001' AND x.codigo='TX-PY-REAL-'||lpad(n::text,4,'0'));

INSERT INTO evaluaciones_riesgo(empresa_id,transaccion_id,fecha_transaccion,score_total,nivel_riesgo_id,resultado,detalle)
SELECT t.empresa_id,t.id,t.fecha_transaccion,t.score_riesgo,t.nivel_riesgo_id,CASE WHEN t.score_riesgo>=70 THEN 'ALERTA' ELSE 'APROBADA' END,jsonb_build_object('demo',true,'score',t.score_riesgo) FROM transacciones t WHERE t.codigo LIKE 'TX-PY-REAL-%' ON CONFLICT DO NOTHING;
INSERT INTO ejecucion_reglas(empresa_id,transaccion_id,fecha_transaccion,regla_codigo,cumplida,score_generado,detalle)
SELECT t.empresa_id,t.id,t.fecha_transaccion,CASE WHEN t.score_riesgo>=70 THEN 'REG-PY-05' ELSE 'REG-PY-01' END,t.score_riesgo>=70,t.score_riesgo,jsonb_build_object('demo',true) FROM transacciones t WHERE t.codigo LIKE 'TX-PY-REAL-%' ON CONFLICT DO NOTHING;
INSERT INTO transaccion_detalle_snapshot(empresa_id,transaccion_id,fecha_transaccion,snapshot_json,fuente)
SELECT t.empresa_id,t.id,t.fecha_transaccion,jsonb_build_object('codigo',t.codigo,'monto',t.monto,'demo',true),'CORE_TRANSACCIONAL_DEMO' FROM transacciones t WHERE t.codigo LIKE 'TX-PY-%' ON CONFLICT DO NOTHING;

INSERT INTO servicio_externo(codigo,nombre,tipo_servicio,url_base,estado,configuracion_json) VALUES
('IDENTIFICACIONES_MOCK','Identificaciones Mock','IDENTIDAD','https://localhost:8443','ACTIVO','{"demo":true}'),
('SANCIONES_MOCK','Sanciones Mock','SANCIONES','https://localhost:8443','ACTIVO','{"demo":true}'),
('PEP_MOCK','PEP Mock','PEP','https://localhost:8443','ACTIVO','{"demo":true}')
ON CONFLICT(codigo) DO UPDATE SET estado=EXCLUDED.estado,url_base=EXCLUDED.url_base;

INSERT INTO api_evento(empresa_id,origen,direccion,servicio,endpoint,metodo_http,status_http,mensaje,resultado,duracion_ms,correlation_id,referencia_entidad,referencia_id,detalle_json,fecha_evento,documento_hash,intentos,resultado_funcional,estado)
SELECT '00000000-0000-0000-0000-000000000001','EXTERNA','SALIENTE',s.codigo,s.tipo_servicio,'GET',200,'Consulta externa simulada','EXITOSO',80+n*7,'seed-ext-'||lpad(n::text,3,'0'),'api_externa','seed-ext-'||lpad(n::text,3,'0'),jsonb_build_object('origenSeed','demo_realistic','piiReal',false),now() - ((12-n) * interval '1 hour'),
       encode(hmac('documento-demo-'||n,'regula-demo-hmac-key','sha256'),'hex'),1,'SIN_COINCIDENCIAS','COMPLETADA'
FROM generate_series(1,12)n
JOIN servicio_externo s ON s.codigo=CASE WHEN n%3=0 THEN 'PEP_MOCK' WHEN n%3=1 THEN 'IDENTIFICACIONES_MOCK' ELSE 'SANCIONES_MOCK' END
WHERE NOT EXISTS(SELECT 1 FROM api_evento a WHERE a.correlation_id='seed-ext-'||lpad(n::text,3,'0'));

INSERT INTO auditoria_sistema(empresa_id,usuario_id,accion,descripcion,entidad_afectada,entidad_id,valor_nuevo_json,direccion_ip,user_agent)
SELECT '00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000101','SEED_REALISTA','Poblacion completa sintetica para pruebas','demo_population','REALISTIC_V1',jsonb_build_object('demo',true,'piiReal',false),'127.0.0.1','RegulaRealisticSeed/1'
WHERE NOT EXISTS(SELECT 1 FROM auditoria_sistema WHERE accion='SEED_REALISTA' AND entidad_id='REALISTIC_V1');
