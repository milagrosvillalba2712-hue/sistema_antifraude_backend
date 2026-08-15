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
ON CONFLICT(id) DO UPDATE SET email=EXCLUDED.email,nombre=EXCLUDED.nombre,password_hash=EXCLUDED.password_hash,
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
UPDATE suscripcion SET codigo='SUB-SCL-2026-001'
 WHERE codigo='SUB-DEMO-2026'
   AND NOT EXISTS(SELECT 1 FROM suscripcion WHERE codigo='SUB-SCL-2026-001');
UPDATE suscripcion SET observacion='Licencia anual Professional'
 WHERE codigo='SUB-SCL-2026-001';

UPDATE contrato SET numero_contrato='SCL-REGULA-2026-001'
 WHERE numero_contrato='CTR-DEMO-2026-001'
   AND NOT EXISTS(SELECT 1 FROM contrato WHERE numero_contrato='SCL-REGULA-2026-001');
UPDATE contrato SET
 documento_referencia='contratos/SCL-REGULA-2026-001.pdf',
 observaciones='Contrato de simulación de aceptación',
 hash_documento=hmac('SCL-REGULA-2026-001','regula-local-test-key','sha256')
 WHERE numero_contrato='SCL-REGULA-2026-001';

UPDATE pago SET codigo='PAG-SCL-2026-001'
 WHERE codigo='PAG-DEMO-2026-001'
   AND NOT EXISTS(SELECT 1 FROM pago WHERE codigo='PAG-SCL-2026-001');
UPDATE pago SET comprobante_referencia='TRX-SCL-2025-001'
 WHERE codigo='PAG-SCL-2026-001';

UPDATE uso_suscripcion SET consumo_json=consumo_json-'demo';
UPDATE instalacion_local SET identificador_instalacion='SCL-ASUNCION-01'
 WHERE identificador_instalacion='INST-DEMO-ASUNCION-01'
   AND NOT EXISTS(SELECT 1 FROM instalacion_local WHERE identificador_instalacion='SCL-ASUNCION-01');
UPDATE instalacion_local SET
 fingerprint_hash='sha256:installation-fingerprint-placeholder',
 clave_publica_pem='-----BEGIN PUBLIC KEY-----\nPENDING-ACTIVATION\n-----END PUBLIC KEY-----',
 version_producto='1.0.0' WHERE identificador_instalacion='SCL-ASUNCION-01';
UPDATE licencia_local SET suscripcion_referencia='SUB-SCL-2026-001',
 lease_payload='eyJtb2RvIjoic2ltdWxhY2lvbi1sb2NhbCJ9',
 lease_firma='PENDING-CONTROL-PLANE-SIGNATURE',kid_firma='local-acceptance-key'
 WHERE suscripcion_referencia IN ('SUB-DEMO-2026','SUB-SCL-2026-001');
UPDATE evento_licencia_local SET correlation_id=replace(correlation_id,'demo-license','scl-license'),
 detalle_sanitizado_json=detalle_sanitizado_json-'demo'
 WHERE correlation_id LIKE 'demo-license-%'
   AND NOT EXISTS (
       SELECT 1 FROM evento_licencia_local e2
       WHERE e2.correlation_id = replace(evento_licencia_local.correlation_id,'demo-license','scl-license')
   );
UPDATE evento_licencia_local SET detalle_sanitizado_json=detalle_sanitizado_json-'demo'
 WHERE correlation_id LIKE 'scl-license-%';

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
UPDATE rol r SET codigo=replace(r.codigo,'_DEMO','_BASE'),nombre=replace(r.nombre,' demo',' base'),
 descripcion=replace(replace(r.descripcion,'demo','instalación'),'académica','operativa')
WHERE r.codigo LIKE '%DEMO%'
  AND NOT EXISTS(SELECT 1 FROM rol r2 WHERE r2.codigo=replace(r.codigo,'_DEMO','_BASE'));
UPDATE permiso p SET codigo=replace(p.codigo,'DEMO','CONTROL'),nombre=replace(p.nombre,'demo','control'),
 descripcion=replace(replace(descripcion,'demo','control'),'académico','operativo'),modulo=replace(modulo,'Demo','Control')
WHERE (p.codigo ILIKE '%demo%' OR p.nombre ILIKE '%demo%' OR p.descripcion ILIKE '%demo%' OR p.modulo ILIKE '%demo%')
  AND NOT EXISTS(SELECT 1 FROM permiso p2 WHERE p2.codigo=replace(p.codigo,'DEMO','CONTROL'));
UPDATE canal_transaccion SET codigo='API_CONTROL',nombre='API de integración',descripcion='Canal API para pruebas de aceptación'
WHERE codigo='API_DEMO' AND NOT EXISTS(SELECT 1 FROM canal_transaccion WHERE codigo='API_CONTROL');
UPDATE tipo_transaccion SET codigo='TRANSFERENCIA_CONTROL',nombre='Transferencia de control',
 descripcion='Transferencia ficticia para validación funcional'
WHERE codigo='TRANSFERENCIA_DEMO' AND NOT EXISTS(SELECT 1 FROM tipo_transaccion WHERE codigo='TRANSFERENCIA_CONTROL');
UPDATE producto SET codigo='CUENTA_CONTROL',nombre='Cuenta transaccional'
WHERE codigo='CUENTA_DEMO' AND NOT EXISTS(SELECT 1 FROM producto WHERE codigo='CUENTA_CONTROL');
UPDATE banco_emisor b SET codigo=replace(b.codigo,'DEMO','SCL'),nombre=replace(b.nombre,'Demo','Santa Clara')
WHERE b.codigo LIKE '%DEMO%' AND NOT EXISTS(SELECT 1 FROM banco_emisor b2 WHERE b2.codigo=replace(b.codigo,'DEMO','SCL'));
UPDATE procesadora_tarjeta SET codigo='PROCESADORA_LOCAL',nombre='Procesadora local de pruebas'
WHERE codigo='PROCESADORA_DEMO' AND NOT EXISTS(SELECT 1 FROM procesadora_tarjeta WHERE codigo='PROCESADORA_LOCAL');
UPDATE empe_operador SET nombre=replace(nombre,' Demo','') WHERE nombre ILIKE '%demo%';
UPDATE persona SET nombre_razon_social=replace(replace(replace(replace(nombre_razon_social,'Sintética','Ficticia'),'Sintetica','Ficticia'),'Sintetico','Ficticio'),' Demo','');
UPDATE perfil_cliente SET actividad_economica='Actividad declarada por el cliente',perfil_json=perfil_json-'demo';
UPDATE fuente_datos_riesgo f SET codigo=replace(f.codigo,'_DEMO','_CONTROL')
WHERE f.codigo LIKE '%\_DEMO' ESCAPE '\'
  AND NOT EXISTS(SELECT 1 FROM fuente_datos_riesgo f2 WHERE f2.codigo=replace(f.codigo,'_DEMO','_CONTROL'));
UPDATE fuente_datos_riesgo SET
 nombre=replace(replace(nombre,'Sintetica','de control'),'Sinteticos','de control'),
 organismo='Control interno Santa Clara',url_oficial=NULL,licencia_uso='Datos ficticios para validación interna'
WHERE codigo LIKE '%\_CONTROL' ESCAPE '\' OR codigo LIKE '%\_DEMO' ESCAPE '\';
UPDATE lista_regulatoria l SET codigo=replace(l.codigo,'_DEMO','_CONTROL')
WHERE l.codigo LIKE '%\_DEMO%' ESCAPE '\'
  AND NOT EXISTS(SELECT 1 FROM lista_regulatoria l2 WHERE l2.codigo=replace(l.codigo,'_DEMO','_CONTROL'));
UPDATE lista_regulatoria SET alcance='CONTROL_INTERNO',
 url_descarga=NULL,licencia_uso='Datos ficticios; no es una lista oficial'
WHERE codigo LIKE '%\_CONTROL%' ESCAPE '\' OR codigo LIKE '%\_DEMO%' ESCAPE '\';
UPDATE elemento_lista el SET valor_identificador=replace(el.valor_identificador,'DEMO-ELEMENTO','CONTROL-ELEMENTO')
WHERE el.valor_identificador LIKE 'DEMO-ELEMENTO%'
  AND NOT EXISTS (
      SELECT 1 FROM elemento_lista el2
      WHERE el2.lista_regulatoria_id=el.lista_regulatoria_id
        AND el2.valor_identificador=replace(el.valor_identificador,'DEMO-ELEMENTO','CONTROL-ELEMENTO')
  );
UPDATE sujeto_riesgo s SET codigo=replace(s.codigo,'SR-DEMO','SR-CONTROL')
WHERE s.codigo LIKE 'SR-DEMO%'
  AND NOT EXISTS(SELECT 1 FROM sujeto_riesgo s2 WHERE s2.codigo=replace(s.codigo,'SR-DEMO','SR-CONTROL'));
UPDATE sujeto_riesgo SET
 nombre_normalizado=replace(nombre_normalizado,'Sintetico','Ficticio'),detalle_json=detalle_json-'demo'
WHERE codigo LIKE 'SR-CONTROL%' OR codigo LIKE 'SR-DEMO%';
UPDATE pais_riesgo SET categoria=replace(categoria,'_DEMO','_REFERENCIAL'),motivo='Clasificación interna de pruebas; validar contra fuente vigente';
UPDATE escenario e SET codigo=replace(e.codigo,'DEMO','CONTROL')
WHERE e.codigo ILIKE '%demo%'
  AND NOT EXISTS(SELECT 1 FROM escenario e2 WHERE e2.empresa_id=e.empresa_id AND e2.codigo=replace(e.codigo,'DEMO','CONTROL'));
UPDATE escenario SET nombre=replace(nombre,'de tesis','de control'),
 descripcion=replace(replace(descripcion,'sintéticos','ficticios'),'académicos','de control')
WHERE codigo ILIKE '%demo%' OR nombre ILIKE '%tesis%' OR descripcion ILIKE '%sint%tico%';
UPDATE accion a SET codigo=replace(a.codigo,'DEMO','CONTROL')
WHERE a.codigo ILIKE '%demo%'
  AND NOT EXISTS(SELECT 1 FROM accion a2 WHERE a2.empresa_id=a.empresa_id AND a2.codigo=replace(a.codigo,'DEMO','CONTROL'));
UPDATE accion SET nombre=replace(nombre,'académica','de control'),
 descripcion=replace(descripcion,'demostración','validación') WHERE codigo ILIKE '%demo%' OR nombre ILIKE '%acad%mica%';
UPDATE reglas_riesgo r SET codigo=replace(r.codigo,'DEMO','CONTROL')
WHERE r.codigo ILIKE '%demo%'
  AND NOT EXISTS(SELECT 1 FROM reglas_riesgo r2 WHERE r2.empresa_id=r.empresa_id AND r2.codigo=replace(r.codigo,'DEMO','CONTROL') AND r2.version=r.version);
UPDATE reglas_riesgo SET nombre=replace(nombre,'académica','de control'),
 descripcion=replace(descripcion,'demo','preinstalada'),condiciones_json=condiciones_json-'demo'
WHERE codigo ILIKE '%demo%' OR nombre ILIKE '%acad%mica%' OR descripcion ILIKE '%demo%' OR condiciones_json ? 'demo';
UPDATE transacciones t SET codigo=replace(t.codigo,'TX-DEMO','TX-CONTROL')
WHERE t.codigo LIKE 'TX-DEMO%'
  AND NOT EXISTS(SELECT 1 FROM transacciones t2 WHERE t2.codigo=replace(t.codigo,'TX-DEMO','TX-CONTROL'));
UPDATE transacciones SET
 nombre_remitente=replace(replace(nombre_remitente,'Sintética','Ficticia'),'Sintetico','Ficticio'),
 nombre_beneficiario=replace(replace(nombre_beneficiario,'Sintético','Ficticio'),'Sintetico','Ficticio');
UPDATE transacciones SET nombre_remitente=replace(nombre_remitente,' Demo',''),
 nombre_beneficiario=replace(nombre_beneficiario,' Demo','')
WHERE nombre_remitente ILIKE '%demo%' OR nombre_beneficiario ILIKE '%demo%';
UPDATE alertas_antifraude a SET codigo=replace(a.codigo,'TX-DEMO','TX-CONTROL')
WHERE a.codigo LIKE '%DEMO%'
  AND NOT EXISTS(SELECT 1 FROM alertas_antifraude a2 WHERE a2.codigo=replace(a.codigo,'TX-DEMO','TX-CONTROL'));
UPDATE alertas_antifraude SET
 motivo=replace(replace(motivo,'académico','de control'),'Demo','Control') WHERE codigo LIKE '%DEMO%' OR motivo ILIKE '%demo%' OR motivo ILIKE '%acad%mico%';
UPDATE caso c SET codigo=replace(c.codigo,'TX-DEMO','TX-CONTROL')
WHERE c.codigo LIKE '%DEMO%'
  AND NOT EXISTS(SELECT 1 FROM caso c2 WHERE c2.codigo=replace(c.codigo,'TX-DEMO','TX-CONTROL'));
UPDATE caso SET titulo=replace(titulo,'TX-DEMO','TX-CONTROL'),
 descripcion=replace(descripcion,'sintético','ficticio') WHERE codigo LIKE '%DEMO%' OR descripcion ILIKE '%sint%tico%';
UPDATE evidencia SET nombre=replace(nombre,'TX-DEMO','TX-CONTROL'),descripcion='Evidencia ficticia para validación del flujo',
 referencia_archivo=replace(replace(referencia_archivo,'demo/','control/'),'TX-DEMO','TX-CONTROL');
UPDATE resolucion_alerta SET conclusion=replace(conclusion,'académica','de control'),
 justificacion='Decisión preconfigurada sobre datos ficticios',evidencia_descripcion='Evidencia ficticia';
UPDATE reportes_ros rr SET codigo=replace(rr.codigo,'DEMO','CONTROL')
WHERE rr.codigo ILIKE '%demo%'
  AND NOT EXISTS(SELECT 1 FROM reportes_ros rr2 WHERE rr2.codigo=replace(rr.codigo,'DEMO','CONTROL'));
UPDATE reportes_ros SET descripcion_sospecha='Caso ficticio para validar la generación de ROS',
 soporte_referencia='control/ros',reporte_json=reporte_json-'demo',nombre_archivo=replace(nombre_archivo,'demo','control');
UPDATE evaluaciones_riesgo SET detalle=detalle-'demo';
UPDATE ejecucion_reglas SET detalle=detalle-'demo';
UPDATE transaccion_detalle_snapshot SET snapshot_json=snapshot_json-'demo',fuente='CORE_TRANSACCIONAL';
UPDATE hallazgo_alerta SET detalle_json=detalle_json-'demo';
UPDATE hallazgo_alerta SET descripcion=replace(replace(descripcion,'TX-DEMO','TX-CONTROL'),'sintetico','ficticio');
UPDATE coincidencia_lista_alerta SET detalle_json=detalle_json-'demo';
UPDATE cliente_snapshot_alerta SET snapshot_json=coalesce(snapshot_json,'{}'::jsonb)-'demo';
UPDATE cliente_snapshot_alerta
SET snapshot_json=jsonb_set(
        jsonb_set(
            coalesce(snapshot_json,'{}'::jsonb),
            '{nombre}',
            to_jsonb(coalesce(replace(snapshot_json->>'nombre',' Demo',''),'Cliente Ficticio')),
            true
        ),
        '{perfil}',
        to_jsonb('ficticio'::text),
        true
    );
UPDATE historial_asignacion SET motivo=replace(motivo,' demo',''),observacion='Asignación inicial de control';
UPDATE historial_estado_caso SET motivo=replace(motivo,' demo','');
UPDATE servicio_externo
SET nombre = replace(nombre, 'Mock', 'Sandbox'),
    estado = 'ACTIVO',
    configuracion_json = '{"modo":"SANDBOX","piiEnLogs":false}'::jsonb
WHERE codigo LIKE '%\_SANDBOX' ESCAPE '\';

UPDATE servicio_externo
SET estado = 'INACTIVO',
    configuracion_json = '{"modo":"LEGACY_MOCK","piiEnLogs":false}'::jsonb
WHERE codigo LIKE '%\_MOCK' ESCAPE '\'
  AND EXISTS (
      SELECT 1
      FROM servicio_externo sandbox
      WHERE sandbox.codigo = replace(servicio_externo.codigo, '_MOCK', '_SANDBOX')
  );
UPDATE api_evento SET correlation_id=replace(correlation_id,'seed-ext','scl-ext') WHERE correlation_id LIKE 'seed-ext%';
UPDATE auditoria_sistema SET accion=replace(accion,'DEMO','CONTROL'),
 descripcion=replace(replace(descripcion,'demo','control'),'sintética','ficticia'),
 entidad_afectada=replace(entidad_afectada,'demo','control'),entidad_id=replace(entidad_id,'DEMO','CONTROL'),
 valor_nuevo_json=valor_nuevo_json-'demo',user_agent=replace(user_agent,'Demo','Control')
WHERE accion ILIKE '%demo%' OR descripcion ILIKE '%demo%' OR entidad_afectada ILIKE '%demo%' OR entidad_id ILIKE '%demo%';

-- Los registros de control nunca se presentan como sanciones/PEP oficiales.
UPDATE cliente_pep SET cargo='Cargo ficticio de control',institucion='Institución ficticia',detalle_json=detalle_json-'demo';
UPDATE cliente_observado SET motivo='Coincidencia en lista interna de control',observacion='No corresponde a una lista oficial';

-- Unificación de nomenclatura: ADMIN_GENERAL, ADMIN_EMPRESA y GERENTE_SUPERVISOR se retiran
-- del cliente (ADMIN_GENERAL queda únicamente en el Control Plane). Los roles productivos son
-- ADMINISTRADOR, SUPERVISOR, ANALISTA y AUDITOR. Se desactivan afiliaciones, permisos y cuentas legacy.
UPDATE usuario_empresa ue SET activo=false, estado='BAJA'
FROM rol r WHERE ue.rol_id=r.id AND r.codigo IN ('ADMIN_GENERAL','ADMIN_EMPRESA','GERENTE_SUPERVISOR');
DELETE FROM rol_permiso rp USING rol r WHERE rp.rol_id=r.id AND r.codigo IN ('ADMIN_GENERAL','ADMIN_EMPRESA','GERENTE_SUPERVISOR');
UPDATE rol SET activo=false WHERE codigo IN ('ADMIN_GENERAL','ADMIN_EMPRESA','GERENTE_SUPERVISOR');
UPDATE usuarios SET activo=false
WHERE email IN ('ana.gimenez@regula.local','lucia.rios@regula.local','roberto.ayala@regula.local');

INSERT INTO auditoria_sistema(empresa_id,usuario_id,accion,descripcion,entidad_afectada,entidad_id,valor_nuevo_json,direccion_ip,user_agent)
SELECT '00000000-0000-0000-0000-000000000001',u.id,'BASELINE_INSTALACION',
'Catálogos, reglas y escenarios iniciales instalados','instalacion','SCL-ASUNCION-01',
'{"origen":"DATOS_FICTICIOS_CONTROLADOS","piiReal":false}'::jsonb,'127.0.0.1','RegulaInstaller/1.0'
FROM usuarios u WHERE u.email='administrador@santaclara.local'
AND NOT EXISTS(SELECT 1 FROM auditoria_sistema WHERE accion='BASELINE_INSTALACION' AND entidad_id='SCL-ASUNCION-01');
