-- Baseline funcional de una instalación on-premise para pruebas de aceptación.
-- Los nombres de personas y operaciones son ficticios; los catálogos se basan en
-- estándares públicos. Ninguna fila representa una coincidencia regulatoria oficial.
SELECT set_config('app.current_empresa_id','00000000-0000-0000-0000-000000000001',false);
SELECT set_config('app.current_usuario_id','00000000-0000-0000-0000-000000000101',false);

UPDATE empresa SET codigo='FINANCIERA_SANTA_CLARA', nombre='Financiera Santa Clara S.A.E.C.A.',
 ruc='80012345-6' WHERE id='00000000-0000-0000-0000-000000000001';

UPDATE moneda SET fuente='ISO 4217' WHERE fuente ILIKE '%demo%';
UPDATE tipo_documento SET fuente_oficial=CASE codigo
 WHEN 'CI_PY' THEN 'Departamento de Identificaciones - Policía Nacional'
 WHEN 'RUC_PY' THEN 'Dirección Nacional de Ingresos Tributarios'
 WHEN 'PASAPORTE' THEN 'Departamento de Identificaciones - Policía Nacional'
 ELSE fuente_oficial END;

-- Catálogo transaccional paraguayo completo para una instalación inicial.
INSERT INTO canal_transaccion(codigo,nombre,descripcion) VALUES
('DEPO','Depositaria de valores','Liquidación y custodia de valores')
ON CONFLICT(codigo) DO UPDATE SET nombre=EXCLUDED.nombre,descripcion=EXCLUDED.descripcion;

INSERT INTO tipo_transaccion(codigo,nombre,categoria,descripcion) VALUES
('PY_SPI_PAYMENT_REQUEST','Solicitud de pago SPI','SPI','Solicitud de pago autorizada por cliente'),
('PY_SPI_PAYMENT_INITIATION','Inicio de pago SPI','SPI','Pago iniciado por proveedor autorizado'),
('PY_SPI_RETURN','Devolución de fondos SPI','SPI','Devolución de transferencia'),
('PY_ACH_BATCH_DEBIT','Débito ACH por lotes','ACH','Débito masivo o recurrente'),
('PY_DIRECT_DEBIT_SERVICE','Débito mensual habitual','ACH','Débito recurrente de servicios'),
('PY_NFC_PAYMENT','Pago NFC','TARJETA','Pago mediante dispositivo NFC'),
('PY_CARD_REFUND','Devolución de tarjeta','TARJETA','Reembolso de una compra'),
('PY_CARD_CHARGEBACK','Contracargo de tarjeta','TARJETA','Disputa o contracargo'),
('PY_ATM_DEPOSIT','Depósito en cajero automático','ATM','Depósito por ATM'),
('PY_CASH_OUT_BRANCH','Extracción de efectivo en sucursal','CAJA','Retiro presencial'),
('PY_EMPE_WALLET_WITHDRAWAL','Retiro de billetera EMPE','EMPE','Retiro desde billetera'),
('PY_NON_BANK_TRANSFER','Transferencia electrónica no bancaria','EMPE','Transferencia procesada por EMPE'),
('PY_CHEQUE_DEPOSIT','Depósito de cheque','CHEQUE','Depósito y compensación de cheque'),
('PY_CHEQUE_PAYMENT','Pago o emisión de cheque','CHEQUE','Pago mediante cheque'),
('PY_TAX_PAYMENT','Pago de tributos','GOBIERNO','Pago a una entidad pública'),
('PY_GOVERNMENT_DISBURSEMENT','Pago estatal o beneficio','GOBIERNO','Desembolso estatal'),
('PY_SECURITIES_SETTLEMENT','Liquidación de valores','DEPO','Liquidación de valores')
ON CONFLICT(codigo) DO UPDATE SET nombre=EXCLUDED.nombre,categoria=EXCLUDED.categoria,descripcion=EXCLUDED.descripcion;

-- Roles que corresponden con los flujos y permisos del backend.
INSERT INTO rol(codigo,nombre,descripcion,alcance,tipo) VALUES
('ADMINISTRADOR','Administrador de la instalación','Configuración, usuarios y operación integral','EMPRESA','EMPRESA'),
('SUPERVISOR','Supervisor de cumplimiento','Supervisión de reglas, alertas y decisiones','EMPRESA','EMPRESA')
ON CONFLICT(codigo) DO UPDATE SET nombre=EXCLUDED.nombre,descripcion=EXCLUDED.descripcion;

INSERT INTO rol_permiso(rol_id,permiso_id)
SELECT r.id,p.id FROM rol r CROSS JOIN permiso p
WHERE r.codigo='ADMINISTRADOR'
 OR (r.codigo='SUPERVISOR' AND p.codigo NOT IN ('EMPRESAS_EDITAR','PAGOS_GESTIONAR'))
ON CONFLICT DO NOTHING;

WITH cuentas(id,email,nombre,rol_codigo,cargo,area) AS (VALUES
('10000000-0000-0000-0000-000000000001'::uuid,'administrador@santaclara.local','Natalia Ferreira','ADMINISTRADOR','Administradora de plataforma','Tecnología'),
('10000000-0000-0000-0000-000000000002'::uuid,'supervisor@santaclara.local','Miguel Benítez','SUPERVISOR','Oficial de cumplimiento','Cumplimiento'),
('10000000-0000-0000-0000-000000000003'::uuid,'analista@santaclara.local','Laura Giménez','ANALISTA','Analista AML','Prevención'),
('10000000-0000-0000-0000-000000000004'::uuid,'auditor@santaclara.local','Rodrigo Caballero','AUDITOR','Auditor interno','Auditoría')
)
INSERT INTO usuarios(id,email,nombre,password_hash,activo,intentos_fallidos,bloqueado_hasta)
SELECT id,email,nombre,crypt('Regula2026!',gen_salt('bf')),true,0,NULL FROM cuentas
ON CONFLICT(email) DO UPDATE SET nombre=EXCLUDED.nombre,password_hash=EXCLUDED.password_hash,
 activo=true,intentos_fallidos=0,bloqueado_hasta=NULL;

WITH cuentas(email,rol_codigo) AS (VALUES
('administrador@santaclara.local','ADMINISTRADOR'),
('supervisor@santaclara.local','SUPERVISOR'),
('analista@santaclara.local','ANALISTA'),
('auditor@santaclara.local','AUDITOR')
)
INSERT INTO usuario_empresa(empresa_id,usuario_id,rol_id,estado)
SELECT '00000000-0000-0000-0000-000000000001',u.id,r.id,'ACTIVO'
FROM cuentas c JOIN usuarios u ON u.email=c.email JOIN rol r ON r.codigo=c.rol_codigo
ON CONFLICT DO NOTHING;

INSERT INTO perfil_usuario(empresa_id,usuario_id,cargo,area,telefono)
SELECT '00000000-0000-0000-0000-000000000001',u.id,c.cargo,c.area,NULL
FROM (VALUES
('administrador@santaclara.local','Administradora de plataforma','Tecnología'),
('supervisor@santaclara.local','Oficial de cumplimiento','Cumplimiento'),
('analista@santaclara.local','Analista AML','Prevención'),
('auditor@santaclara.local','Auditor interno','Auditoría')) c(email,cargo,area)
JOIN usuarios u ON u.email=c.email
ON CONFLICT(empresa_id,usuario_id) DO UPDATE SET cargo=EXCLUDED.cargo,area=EXCLUDED.area;

INSERT INTO disponibilidad_usuario(empresa_id,usuario_id,estado,carga_actual,capacidad_maxima)
SELECT '00000000-0000-0000-0000-000000000001',u.id,'DISPONIBLE',0,20
FROM usuarios u WHERE u.email='analista@santaclara.local'
ON CONFLICT(empresa_id,usuario_id) DO UPDATE SET estado='DISPONIBLE',capacidad_maxima=20;

-- Contrato y licencia local con identificadores propios de la empresa instalada.
UPDATE suscripcion SET codigo='SUB-SCL-2026-001',observacion='Licencia anual Professional'
 WHERE codigo='SUB-DEMO-2026';
UPDATE contrato SET numero_contrato='SCL-REGULA-2026-001',
 documento_referencia='contratos/SCL-REGULA-2026-001.pdf',
 observaciones='Contrato de simulación de aceptación',
 hash_documento=hmac('SCL-REGULA-2026-001','regula-local-test-key','sha256')
 WHERE numero_contrato='CTR-DEMO-2026-001';
UPDATE pago SET codigo='PAG-SCL-2026-001',comprobante_referencia='TRX-SCL-2025-001'
 WHERE codigo='PAG-DEMO-2026-001';
UPDATE uso_suscripcion SET consumo_json=consumo_json-'demo';
UPDATE instalacion_local SET identificador_instalacion='SCL-ASUNCION-01',
 fingerprint_hash='sha256:installation-fingerprint-placeholder',
 clave_publica_pem='-----BEGIN PUBLIC KEY-----\nPENDING-ACTIVATION\n-----END PUBLIC KEY-----',
 version_producto='1.0.0' WHERE identificador_instalacion='INST-DEMO-ASUNCION-01';
UPDATE licencia_local SET suscripcion_referencia='SUB-SCL-2026-001',
 lease_payload='eyJtb2RvIjoic2ltdWxhY2lvbi1sb2NhbCJ9',
 lease_firma='PENDING-CONTROL-PLANE-SIGNATURE',kid_firma='local-acceptance-key'
 WHERE suscripcion_referencia='SUB-DEMO-2026';
UPDATE evento_licencia_local SET correlation_id=replace(correlation_id,'demo-license','scl-license'),
 detalle_sanitizado_json=detalle_sanitizado_json-'demo';

-- Sustituye etiquetas académicas visibles conservando el origen ficticio de forma explícita.
UPDATE usuarios SET
 email=CASE email
   WHEN 'admin@demo.regula.local' THEN 'operaciones.plataforma@santaclara.local'
   WHEN 'analista@demo.regula.local' THEN 'analista.control@santaclara.local'
   WHEN 'supervisor@demo.regula.local' THEN 'supervisor.control@santaclara.local'
   WHEN 'auditor@demo.regula.local' THEN 'auditor.control@santaclara.local'
   WHEN 'analista2@demo.regula.local' THEN 'analista2.control@santaclara.local'
   ELSE replace(email,'@cliente.local','@santaclara.local') END,
 nombre=replace(replace(nombre,' Demo',''),'Académico','de Cumplimiento')
WHERE email LIKE '%@demo.regula.local' OR email LIKE '%@cliente.local';
UPDATE rol SET codigo=replace(codigo,'_DEMO','_BASE'),nombre=replace(nombre,' demo',' base'),
 descripcion=replace(replace(descripcion,'demo','instalación'),'académica','operativa') WHERE codigo LIKE '%DEMO%';
UPDATE permiso SET codigo=replace(codigo,'DEMO','CONTROL'),nombre=replace(nombre,'demo','control'),
 descripcion=replace(replace(descripcion,'demo','control'),'académico','operativo'),modulo=replace(modulo,'Demo','Control')
WHERE codigo ILIKE '%demo%' OR nombre ILIKE '%demo%' OR descripcion ILIKE '%demo%' OR modulo ILIKE '%demo%';
UPDATE canal_transaccion SET codigo='API_CONTROL',nombre='API de integración',descripcion='Canal API para pruebas de aceptación'
WHERE codigo='API_DEMO';
UPDATE tipo_transaccion SET codigo='TRANSFERENCIA_CONTROL',nombre='Transferencia de control',
 descripcion='Transferencia ficticia para validación funcional' WHERE codigo='TRANSFERENCIA_DEMO';
UPDATE producto SET codigo='CUENTA_CONTROL',nombre='Cuenta transaccional' WHERE codigo='CUENTA_DEMO';
UPDATE banco_emisor SET codigo=replace(codigo,'DEMO','SCL'),nombre=replace(nombre,'Demo','Santa Clara') WHERE codigo LIKE '%DEMO%';
UPDATE procesadora_tarjeta SET codigo='PROCESADORA_LOCAL',nombre='Procesadora local de pruebas' WHERE codigo='PROCESADORA_DEMO';
UPDATE empe_operador SET nombre=replace(nombre,' Demo','') WHERE nombre ILIKE '%demo%';
UPDATE persona SET nombre_razon_social=replace(replace(replace(replace(nombre_razon_social,'Sintética','Ficticia'),'Sintetica','Ficticia'),'Sintetico','Ficticio'),' Demo','');
UPDATE perfil_cliente SET actividad_economica='Actividad declarada por el cliente',perfil_json=perfil_json-'demo';
UPDATE fuente_datos_riesgo SET codigo=replace(codigo,'_DEMO','_CONTROL'),
 nombre=replace(replace(nombre,'Sintetica','de control'),'Sinteticos','de control'),
 organismo='Control interno Santa Clara',url_oficial=NULL,licencia_uso='Datos ficticios para validación interna';
UPDATE lista_regulatoria SET codigo=replace(codigo,'_DEMO','_CONTROL'),alcance='CONTROL_INTERNO',
 url_descarga=NULL,licencia_uso='Datos ficticios; no es una lista oficial';
UPDATE elemento_lista SET valor_identificador=replace(valor_identificador,'DEMO-ELEMENTO','CONTROL-ELEMENTO');
UPDATE sujeto_riesgo SET codigo=replace(codigo,'SR-DEMO','SR-CONTROL'),
 nombre_normalizado=replace(nombre_normalizado,'Sintetico','Ficticio'),detalle_json=detalle_json-'demo';
UPDATE pais_riesgo SET categoria=replace(categoria,'_DEMO','_REFERENCIAL'),motivo='Clasificación interna de pruebas; validar contra fuente vigente';
UPDATE escenario SET codigo=replace(codigo,'DEMO','CONTROL'),nombre=replace(nombre,'de tesis','de control'),
 descripcion=replace(replace(descripcion,'sintéticos','ficticios'),'académicos','de control')
WHERE codigo ILIKE '%demo%' OR nombre ILIKE '%tesis%' OR descripcion ILIKE '%sint%tico%';
UPDATE accion SET codigo=replace(codigo,'DEMO','CONTROL'),nombre=replace(nombre,'académica','de control'),
 descripcion=replace(descripcion,'demostración','validación') WHERE codigo ILIKE '%demo%' OR nombre ILIKE '%acad%mica%';
UPDATE reglas_riesgo SET codigo=replace(codigo,'DEMO','CONTROL'),nombre=replace(nombre,'académica','de control'),
 descripcion=replace(descripcion,'demo','preinstalada'),condiciones_json=condiciones_json-'demo'
WHERE codigo ILIKE '%demo%' OR nombre ILIKE '%acad%mica%' OR descripcion ILIKE '%demo%' OR condiciones_json ? 'demo';
UPDATE transacciones SET codigo=replace(codigo,'TX-DEMO','TX-CONTROL'),
 nombre_remitente=replace(replace(nombre_remitente,'Sintética','Ficticia'),'Sintetico','Ficticio'),
 nombre_beneficiario=replace(replace(nombre_beneficiario,'Sintético','Ficticio'),'Sintetico','Ficticio');
UPDATE transacciones SET nombre_remitente=replace(nombre_remitente,' Demo',''),
 nombre_beneficiario=replace(nombre_beneficiario,' Demo','')
WHERE nombre_remitente ILIKE '%demo%' OR nombre_beneficiario ILIKE '%demo%';
UPDATE alertas_antifraude SET codigo=replace(codigo,'TX-DEMO','TX-CONTROL'),
 motivo=replace(replace(motivo,'académico','de control'),'Demo','Control') WHERE codigo LIKE '%DEMO%' OR motivo ILIKE '%demo%' OR motivo ILIKE '%acad%mico%';
UPDATE caso SET codigo=replace(codigo,'TX-DEMO','TX-CONTROL'),titulo=replace(titulo,'TX-DEMO','TX-CONTROL'),
 descripcion=replace(descripcion,'sintético','ficticio') WHERE codigo LIKE '%DEMO%' OR descripcion ILIKE '%sint%tico%';
UPDATE evidencia SET nombre=replace(nombre,'TX-DEMO','TX-CONTROL'),descripcion='Evidencia ficticia para validación del flujo',
 referencia_archivo=replace(replace(referencia_archivo,'demo/','control/'),'TX-DEMO','TX-CONTROL');
UPDATE resolucion_alerta SET conclusion=replace(conclusion,'académica','de control'),
 justificacion='Decisión preconfigurada sobre datos ficticios',evidencia_descripcion='Evidencia ficticia';
UPDATE reportes_ros SET codigo=replace(codigo,'DEMO','CONTROL'),descripcion_sospecha='Caso ficticio para validar la generación de ROS',
 soporte_referencia='control/ros',reporte_json=reporte_json-'demo',nombre_archivo=replace(nombre_archivo,'demo','control');
UPDATE evaluaciones_riesgo SET detalle=detalle-'demo';
UPDATE ejecucion_reglas SET detalle=detalle-'demo';
UPDATE transaccion_detalle_snapshot SET snapshot_json=snapshot_json-'demo',fuente='CORE_TRANSACCIONAL';
UPDATE hallazgo_alerta SET detalle_json=detalle_json-'demo';
UPDATE hallazgo_alerta SET descripcion=replace(replace(descripcion,'TX-DEMO','TX-CONTROL'),'sintetico','ficticio');
UPDATE coincidencia_lista_alerta SET detalle_json=detalle_json-'demo';
UPDATE cliente_snapshot_alerta SET snapshot_json=snapshot_json-'demo';
UPDATE cliente_snapshot_alerta SET snapshot_json=jsonb_set(jsonb_set(snapshot_json,'{nombre}',to_jsonb(replace(snapshot_json->>'nombre',' Demo',''))),'{perfil}','"ficticio"');
UPDATE historial_asignacion SET motivo=replace(motivo,' demo',''),observacion='Asignación inicial de control';
UPDATE historial_estado_caso SET motivo=replace(motivo,' demo','');
UPDATE servicio_externo SET codigo=replace(codigo,'_MOCK','_SANDBOX'),nombre=replace(nombre,'Mock','Sandbox'),
 configuracion_json='{"modo":"SANDBOX","piiEnLogs":false}'::jsonb;
UPDATE consultas_externas SET correlation_id=replace(correlation_id,'seed-ext','scl-ext');
UPDATE auditoria_sistema SET accion=replace(accion,'DEMO','CONTROL'),
 descripcion=replace(replace(descripcion,'demo','control'),'sintética','ficticia'),
 entidad_afectada=replace(entidad_afectada,'demo','control'),entidad_id=replace(entidad_id,'DEMO','CONTROL'),
 valor_nuevo_json=valor_nuevo_json-'demo',user_agent=replace(user_agent,'Demo','Control')
WHERE accion ILIKE '%demo%' OR descripcion ILIKE '%demo%' OR entidad_afectada ILIKE '%demo%' OR entidad_id ILIKE '%demo%';

-- Los registros de control nunca se presentan como sanciones/PEP oficiales.
UPDATE cliente_pep SET cargo='Cargo ficticio de control',institucion='Institución ficticia',detalle_json=detalle_json-'demo';
UPDATE cliente_observado SET motivo='Coincidencia en lista interna de control',observacion='No corresponde a una lista oficial';

INSERT INTO auditoria_sistema(empresa_id,usuario_id,accion,descripcion,entidad_afectada,entidad_id,valor_nuevo_json,direccion_ip,user_agent)
SELECT '00000000-0000-0000-0000-000000000001',u.id,'BASELINE_INSTALACION',
'Catálogos, reglas y escenarios iniciales instalados','instalacion','SCL-ASUNCION-01',
'{"origen":"DATOS_FICTICIOS_CONTROLADOS","piiReal":false}'::jsonb,'127.0.0.1','RegulaInstaller/1.0'
FROM usuarios u WHERE u.email='administrador@santaclara.local'
AND NOT EXISTS(SELECT 1 FROM auditoria_sistema WHERE accion='BASELINE_INSTALACION' AND entidad_id='SCL-ASUNCION-01');
