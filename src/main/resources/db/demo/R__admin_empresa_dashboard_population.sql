-- Poblacion demo realista para tablero Admin Empresa.
-- Datos sinteticos: no representan personas, clientes, proveedores ni eventos productivos reales.

INSERT INTO empresa(id,codigo,nombre,ruc,estado,email_contacto,telefono_contacto)
VALUES(
    '00000000-0000-0000-0000-000000000001',
    'FINANCIERA_SANTA_CLARA',
    'Financiera Santa Clara S.A.E.C.A.',
    '80012345-6',
    'ACTIVA',
    'contacto@santaclara.local',
    '+595212000001'
)
ON CONFLICT(id) DO UPDATE
SET codigo = EXCLUDED.codigo,
    nombre = EXCLUDED.nombre,
    ruc = EXCLUDED.ruc,
    estado = EXCLUDED.estado,
    email_contacto = EXCLUDED.email_contacto,
    telefono_contacto = EXCLUDED.telefono_contacto;

DO $$
BEGIN
    PERFORM set_config('app.current_empresa_id', '00000000-0000-0000-0000-000000000001', false);
    PERFORM set_config('app.current_usuario_id', '10000000-0000-0000-0000-000000000001', false);
END $$;

INSERT INTO usuarios(id,email,nombre,password_hash,activo,intentos_fallidos,bloqueado_hasta)
VALUES
('10000000-0000-0000-0000-000000000001','administrador@santaclara.local','Natalia Ferreira',crypt('Regula2026!', gen_salt('bf')),true,0,NULL),
('10000000-0000-0000-0000-000000000002','supervisor@santaclara.local','Miguel Benítez',crypt('Regula2026!', gen_salt('bf')),true,0,NULL),
('10000000-0000-0000-0000-000000000003','analista@santaclara.local','Laura Giménez',crypt('Regula2026!', gen_salt('bf')),true,0,NULL),
('10000000-0000-0000-0000-000000000004','auditor@santaclara.local','Rodrigo Caballero',crypt('Regula2026!', gen_salt('bf')),true,0,NULL),
('10000000-0000-0000-0000-000000000005','soporte.api@santaclara.local','Sofía Duarte',crypt('Regula2026!', gen_salt('bf')),true,0,NULL),
('00000000-0000-0000-0000-000000000099','system@santaclara.local','Sistema Automático',crypt(gen_random_uuid()::text, gen_salt('bf')),true,0,NULL)
ON CONFLICT(id) DO UPDATE
SET email = EXCLUDED.email,
    nombre = EXCLUDED.nombre,
    password_hash = EXCLUDED.password_hash,
    activo = true,
    intentos_fallidos = 0,
    bloqueado_hasta = NULL;

INSERT INTO usuario_empresa(empresa_id,usuario_id,rol_id,estado,activo)
SELECT '00000000-0000-0000-0000-000000000001', u.id, r.id, 'ACTIVO', true
FROM (VALUES
('administrador@santaclara.local','ADMINISTRADOR'),
('supervisor@santaclara.local','SUPERVISOR'),
('analista@santaclara.local','ANALISTA'),
('auditor@santaclara.local','AUDITOR'),
('soporte.api@santaclara.local','ADMINISTRADOR'),
('system@santaclara.local','ADMINISTRADOR')
) v(email, rol_codigo)
JOIN usuarios u ON u.email = v.email
JOIN rol r ON r.codigo = v.rol_codigo
ON CONFLICT(empresa_id,usuario_id,rol_id) DO UPDATE
SET estado = 'ACTIVO', activo = true;

INSERT INTO perfil_usuario(empresa_id,usuario_id,cargo,area,telefono)
SELECT '00000000-0000-0000-0000-000000000001', u.id, v.cargo, v.area, v.telefono
FROM (VALUES
('administrador@santaclara.local','Administradora de empresa','Administración','+595981110001'),
('supervisor@santaclara.local','Gerente de cumplimiento','Cumplimiento','+595981110002'),
('analista@santaclara.local','Analista AML senior','Monitoreo','+595981110003'),
('auditor@santaclara.local','Auditor interno','Auditoría','+595981110004'),
('soporte.api@santaclara.local','Responsable de integraciones','Tecnología','+595981110005')
) v(email,cargo,area,telefono)
JOIN usuarios u ON u.email = v.email
ON CONFLICT(empresa_id,usuario_id) DO UPDATE
SET cargo = EXCLUDED.cargo, area = EXCLUDED.area, telefono = EXCLUDED.telefono;

WITH plan AS (
    SELECT id FROM plan_licencia WHERE codigo IN ('PROFESIONAL','PROFESSIONAL') ORDER BY codigo = 'PROFESIONAL' DESC LIMIT 1
)
INSERT INTO suscripcion(empresa_id,plan_licencia_id,codigo,estado,fecha_inicio,fecha_fin,renovacion_automatica,observacion)
SELECT '00000000-0000-0000-0000-000000000001', id, 'SUB-SCL-2026-001', 'ACTIVA',
       DATE '2026-01-01', DATE '2026-12-31', true, 'Licencia anual activa de Financiera Santa Clara'
FROM plan
ON CONFLICT(codigo) DO UPDATE
SET estado = EXCLUDED.estado,
    fecha_inicio = EXCLUDED.fecha_inicio,
    fecha_fin = EXCLUDED.fecha_fin,
    renovacion_automatica = EXCLUDED.renovacion_automatica,
    observacion = EXCLUDED.observacion;

INSERT INTO pago(empresa_id,suscripcion_id,codigo,fecha_pago,monto,moneda_id,estado,metodo_pago,comprobante_referencia)
SELECT s.empresa_id, s.id, v.codigo, v.fecha_pago, v.monto, m.id, v.estado, v.metodo, v.comprobante
FROM suscripcion s
JOIN moneda m ON m.codigo_iso = 'USD'
JOIN (VALUES
('PAG-SCL-2026-001', TIMESTAMPTZ '2025-12-20 10:00:00-03', 24000.00, 'CONFIRMADO', 'TRANSFERENCIA_BANCARIA', 'SCL-COMP-2025-001'),
('PAG-SCL-2026-002', TIMESTAMPTZ '2026-06-30 15:20:00-03', 0.00, 'PROGRAMADO', 'PAGO_ONLINE_PENDIENTE', 'SCL-RENOVACION-2026-002')
) v(codigo,fecha_pago,monto,estado,metodo,comprobante) ON true
WHERE s.codigo = 'SUB-SCL-2026-001'
ON CONFLICT(codigo) DO UPDATE
SET estado = EXCLUDED.estado,
    monto = EXCLUDED.monto,
    metodo_pago = EXCLUDED.metodo_pago,
    comprobante_referencia = EXCLUDED.comprobante_referencia;

INSERT INTO uso_suscripcion(empresa_id,suscripcion_id,periodo,usuarios_activos,transacciones_procesadas,consultas_kyc,alertas_generadas,reportes_generados,consumo_json,anio,mes)
SELECT s.empresa_id, s.id, make_date(2026, m, 1), 5 + (m % 3), 8500 + (m * 930),
       120 + (m * 18), 26 + (m * 3), 2 + (m % 4),
       jsonb_build_object('origen','seed_dashboard','escenario','aceptacion_admin_empresa'),
       2026, m
FROM suscripcion s
CROSS JOIN generate_series(1, 8) m
WHERE s.codigo = 'SUB-SCL-2026-001'
ON CONFLICT(empresa_id,suscripcion_id,periodo) DO UPDATE
SET usuarios_activos = EXCLUDED.usuarios_activos,
    transacciones_procesadas = EXCLUDED.transacciones_procesadas,
    consultas_kyc = EXCLUDED.consultas_kyc,
    alertas_generadas = EXCLUDED.alertas_generadas,
    reportes_generados = EXCLUDED.reportes_generados,
    consumo_json = EXCLUDED.consumo_json,
    anio = EXCLUDED.anio,
    mes = EXCLUDED.mes;

INSERT INTO admin_empresa_configuracion_local(empresa_id,tipo,codigo,nombre,descripcion,estado,editable,orden,detalle_json)
VALUES
('00000000-0000-0000-0000-000000000001','PARAMETRO','SYNC_CATALOGOS_FRECUENCIA','Frecuencia De Sincronización De Catálogos','Define cada cuánto la instalación cliente solicita catálogos permitidos al Control Plane.','ACTIVO',true,10,'{"valorActual":"Cada 6 horas","recomendado":"Cada 6 a 12 horas","impacto":"Actualiza listas, países de riesgo y catálogos AML"}'),
('00000000-0000-0000-0000-000000000001','PARAMETRO','RETENCION_AUDITORIA','Retención De Auditoría Local','Tiempo durante el cual se conservan eventos funcionales y técnicos para revisión interna.','ACTIVO',true,20,'{"valorActual":"5 años","base":"Buenas prácticas AML y trazabilidad de soporte"}'),
('00000000-0000-0000-0000-000000000001','PARAMETRO','MODO_GRACIA_LICENCIA','Modo De Gracia De Licencia','Permite operar temporalmente si el Control Plane no responde y la licencia todavía está dentro del período permitido.','ACTIVO',false,30,'{"valorActual":"15 días","control":"Validación criptográfica local"}'),
('00000000-0000-0000-0000-000000000001','PARAMETRO','ALERTA_API_EXTERNA','Umbral De Alerta Para APIs Externas','Dispara advertencia si una API externa supera el porcentaje permitido de errores.','ACTIVO',true,40,'{"valorActual":"10%","ventana":"1 hora"}')
ON CONFLICT(empresa_id,tipo,codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    estado = EXCLUDED.estado,
    editable = EXCLUDED.editable,
    orden = EXCLUDED.orden,
    detalle_json = EXCLUDED.detalle_json;

-- Jobs configurables del agente on-premise. editable=true para administrarlos
-- desde Configuracion Local. En el conflicto se conservan estado y los campos
-- de ejecucion (ultimaEjecucion/proximaEjecucion/ultimoResultado/ultimoDetalle)
-- para que la configuracion y el historial del administrador sobrevivan a cada
-- arranque; solo se propagan los defaults de frecuencia.
INSERT INTO admin_empresa_configuracion_local(empresa_id,tipo,codigo,nombre,descripcion,estado,editable,orden,detalle_json)
VALUES
('00000000-0000-0000-0000-000000000001','JOB','HEARTBEAT','Heartbeat Al Control Plane','Reporta latido de la instalacion al Control Plane y actualiza el ultimo heartbeat local.','ACTIVO',true,10,'{"frecuenciaValor":5,"frecuenciaUnidad":"MINUTOS","ultimaEjecucion":"2026-08-14T08:00:00-03:00","proximaEjecucion":"2026-08-15T00:00:00-03:00","ultimoResultado":"OK"}'),
('00000000-0000-0000-0000-000000000001','JOB','VALIDACION_LICENCIA','Validación Y Renovación De Licencia','Valida el lease contra el Control Plane, renueva la licencia firmada si es posible y reevalúa la política local.','ACTIVO',true,20,'{"frecuenciaValor":1,"frecuenciaUnidad":"HORAS","ultimaEjecucion":"2026-08-14T08:00:00-03:00","proximaEjecucion":"2026-08-15T00:00:00-03:00","ultimoResultado":"OPERATIVO"}'),
('00000000-0000-0000-0000-000000000001','JOB','LICENSE_USAGE_SYNC','Sincronización De Consumo','Reporta consumo mensual local al Control Plane para control de plan, auditoría y tablero administrativo.','ACTIVO',true,30,'{"frecuenciaValor":1,"frecuenciaUnidad":"HORAS","ultimaEjecucion":"2026-08-14T08:10:00-03:00","proximaEjecucion":"2026-08-15T00:00:00-03:00","ultimoResultado":"OK"}'),
('00000000-0000-0000-0000-000000000001','JOB','CATALOG_SYNC','Sincronización De Catálogos','Descarga manifiesto y versiones permitidas de catálogos AML desde el Control Plane.','ACTIVO',true,40,'{"frecuenciaValor":6,"frecuenciaUnidad":"HORAS","ultimaEjecucion":"2026-08-14T08:00:00-03:00","proximaEjecucion":"2026-08-15T00:00:00-03:00","ultimoResultado":"OK"}'),
('00000000-0000-0000-0000-000000000001','JOB','EXTERNAL_API_HEALTH_CHECK','Verificación De APIs Externas','Consulta disponibilidad de proveedores sandbox: identificaciones, sanciones, PEP y servicios de soporte.','ACTIVO',true,50,'{"frecuenciaValor":10,"frecuenciaUnidad":"MINUTOS","ultimaEjecucion":"2026-08-14T08:15:00-03:00","proximaEjecucion":"2026-08-15T00:00:00-03:00","ultimoResultado":"ADVERTENCIA","ultimoDetalle":"Sanciones con latencia alta"}'),
('00000000-0000-0000-0000-000000000001','JOB','SUSCRIPCION_ESTADOS','Estados De Suscripción','Revisa vencimiento y renovación automática de suscripciones (POR_VENCER / VENCIDA / ACTIVA / CERRADA).','ACTIVO',true,60,'{"frecuenciaValor":1,"frecuenciaUnidad":"DIAS","hora":"03:30","ultimaEjecucion":"2026-08-14T03:30:00-03:00","proximaEjecucion":"2026-08-15T00:00:00-03:00","ultimoResultado":"OK"}'),
('00000000-0000-0000-0000-000000000001','JOB','AUDIT_RETENTION_CHECK','Control De Retención De Auditoría','Verifica volumen y antigüedad de eventos auditables conservados localmente.','ACTIVO',true,70,'{"frecuenciaValor":1,"frecuenciaUnidad":"DIAS","hora":"07:00","ultimaEjecucion":"2026-08-14T07:00:00-03:00","proximaEjecucion":"2026-08-15T00:00:00-03:00","ultimoResultado":"OK"}'),
('00000000-0000-0000-0000-000000000001','JOB','LOG_RETENTION_PURGE','Purga De Retención De Logs','Elimina eventos API y logs de aplicacion (app_log) anteriores al periodo de retencion configurado.','ACTIVO',true,80,'{"frecuenciaValor":1,"frecuenciaUnidad":"DIAS","hora":"05:45","diasRetencion":30,"ultimaEjecucion":"2026-08-14T05:45:00-03:00","proximaEjecucion":"2026-08-15T00:00:00-03:00","ultimoResultado":"OK"}')
ON CONFLICT(empresa_id,tipo,codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    editable = EXCLUDED.editable,
    orden = EXCLUDED.orden,
    detalle_json = COALESCE(admin_empresa_configuracion_local.detalle_json, '{}'::jsonb)
        || (EXCLUDED.detalle_json - 'ultimaEjecucion' - 'ultimoResultado' - 'proximaEjecucion' - 'ultimoDetalle');

INSERT INTO api_evento(
    empresa_id, usuario_id, origen, direccion, servicio, endpoint, metodo_http, status_http,
    codigo_error, mensaje, resultado, categoria_error, duracion_ms, correlation_id, request_id,
    ip_origen, user_agent, referencia_entidad, referencia_id, detalle_json, fecha_evento
)
SELECT '00000000-0000-0000-0000-000000000001',
       CASE WHEN n % 5 = 0 THEN '10000000-0000-0000-0000-000000000005'::uuid ELSE '10000000-0000-0000-0000-000000000001'::uuid END,
       CASE WHEN n % 4 = 0 THEN 'EXTERNA' ELSE 'INTERNA' END,
       CASE WHEN n % 4 = 0 THEN 'SALIENTE' ELSE 'ENTRANTE' END,
       CASE
           WHEN n % 6 = 0 THEN 'IDENTIFICACIONES_SANDBOX'
           WHEN n % 6 = 1 THEN 'ADMIN_EMPRESA'
           WHEN n % 6 = 2 THEN 'ALERTAS'
           WHEN n % 6 = 3 THEN 'KYC'
           WHEN n % 6 = 4 THEN 'SANCIONES_SANDBOX'
           ELSE 'MOTOR_REGLAS'
       END,
       CASE
           WHEN n % 6 = 0 THEN '/api/v1/clientes/100/perfil'
           WHEN n % 6 = 1 THEN '/api/admin-empresa/system-overview'
           WHEN n % 6 = 2 THEN '/api/alertas'
           WHEN n % 6 = 3 THEN '/api/kyc/consultar'
           WHEN n % 6 = 4 THEN '/api/v1/sanciones/200'
           ELSE '/api/rule-engine/facts'
       END,
       'GET',
       CASE WHEN n IN (7, 19, 31, 43, 55) THEN 503 WHEN n IN (12, 36, 48) THEN 504 ELSE 200 END,
       CASE WHEN n IN (7, 19, 31, 43, 55) THEN 'SERVICIO_NO_DISPONIBLE'
            WHEN n IN (12, 36, 48) THEN 'TIMEOUT'
            ELSE NULL END,
       CASE WHEN n IN (7, 19, 31, 43, 55) THEN 'Proveedor externo no disponible en ventana de prueba'
            WHEN n IN (12, 36, 48) THEN 'Timeout al consultar proveedor externo'
            ELSE 'Solicitud procesada correctamente' END,
       CASE WHEN n IN (7, 12, 19, 31, 36, 43, 48, 55) THEN 'ERROR' ELSE 'EXITOSO' END,
       CASE WHEN n IN (7, 19, 31, 43, 55) THEN 'CONEXION_O_RESPUESTA'
            WHEN n IN (12, 36, 48) THEN 'TIMEOUT'
            ELSE NULL END,
       CASE WHEN n % 4 = 0 THEN 180 + (n * 11) ELSE 32 + (n * 3) END,
       'scl-api-' || lpad(n::text, 3, '0'),
       'scl-req-' || lpad(n::text, 3, '0'),
       '127.0.0.1',
       'RegulaWeb/aceptacion',
       CASE WHEN n % 4 = 0 THEN 'api_externa' ELSE 'api_endpoint' END,
       'SCL-EVENT-' || lpad(n::text, 3, '0'),
       jsonb_build_object('origenSeed','dashboard_admin_empresa','piiReal',false),
       now() - ((60 - n) * interval '5 minutes')
FROM generate_series(1, 60) n
WHERE NOT EXISTS (
    SELECT 1 FROM api_evento e WHERE e.correlation_id = 'scl-api-' || lpad(n::text, 3, '0')
);

INSERT INTO servicio_externo(codigo,nombre,tipo_servicio,url_base,estado,configuracion_json)
VALUES
('IDENTIFICACIONES_SANDBOX','Identificaciones Sandbox','IDENTIDAD','https://localhost:8443','ACTIVO','{"demo":true,"fuente":"mock_regula"}'),
('SANCIONES_SANDBOX','Sanciones Sandbox','SANCIONES','https://localhost:8443','ACTIVO','{"demo":true,"fuente":"mock_regula"}'),
('PEP_SANDBOX','PEP Sandbox','PEP','https://localhost:8443','ACTIVO','{"demo":true,"fuente":"mock_regula"}')
ON CONFLICT(codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    tipo_servicio = EXCLUDED.tipo_servicio,
    url_base = EXCLUDED.url_base,
    estado = EXCLUDED.estado,
    configuracion_json = EXCLUDED.configuracion_json;

INSERT INTO api_evento(
    empresa_id, origen, direccion, servicio, endpoint, metodo_http, status_http,
    codigo_error, mensaje, resultado, categoria_error, duracion_ms, correlation_id,
    referencia_entidad, referencia_id, detalle_json, fecha_evento,
    documento_hash, intentos, resultado_funcional, estado
)
SELECT '00000000-0000-0000-0000-000000000001',
       'EXTERNA',
       'SALIENTE',
       s.codigo,
       CASE WHEN n % 4 = 0 THEN 'KYC_PERFIL'
            WHEN n % 4 = 1 THEN 'SCREENING_LISTAS'
            WHEN n % 4 = 2 THEN 'PEP'
            ELSE 'RIESGO_PAIS' END,
       'GET',
       CASE WHEN n IN (4, 19) THEN 503 WHEN n IN (9, 24) THEN 504 WHEN n = 14 THEN 429 ELSE 200 END,
       CASE WHEN n IN (4, 19) THEN 'SERVICIO_NO_DISPONIBLE'
            WHEN n IN (9, 24) THEN 'TIMEOUT'
            WHEN n = 14 THEN 'RATE_LIMIT'
            ELSE NULL END,
       CASE WHEN n IN (4, 19) THEN 'Proveedor externo no disponible en ventana de prueba'
            WHEN n IN (9, 24) THEN 'Timeout al consultar proveedor externo'
            WHEN n = 14 THEN 'Límite de consultas superado'
            ELSE 'Solicitud procesada correctamente' END,
       CASE WHEN n IN (4, 9, 14, 19, 24) THEN 'ERROR' ELSE 'EXITOSO' END,
       CASE WHEN n IN (4, 19) THEN 'SERVICIO_NO_DISPONIBLE'
            WHEN n IN (9, 24) THEN 'TIMEOUT'
            WHEN n = 14 THEN 'RATE_LIMIT'
            ELSE NULL END,
       CASE WHEN n IN (4, 9, 14, 19, 24) THEN 900 + (n * 8) ELSE 75 + (n * 6) END,
       'scl-ext-' || lpad(n::text, 3, '0'),
       'api_externa',
       'scl-ext-' || lpad(n::text, 3, '0'),
       jsonb_build_object('origenSeed','dashboard_admin_empresa','piiReal',false),
       now() - ((24 - n) * interval '7 minutes'),
       encode(hmac('scl-documento-' || n, 'regula-demo-hmac-key', 'sha256'), 'hex'),
       CASE WHEN n IN (9, 24) THEN 3 ELSE 1 END,
       CASE WHEN n % 7 = 0 THEN 'COINCIDENCIA_REVISAR' ELSE 'SIN_COINCIDENCIA' END,
       CASE WHEN n IN (4, 9, 14, 19, 24) THEN 'ERROR' ELSE 'COMPLETADA' END
FROM generate_series(1, 24) n
JOIN servicio_externo s ON s.codigo = CASE WHEN n % 3 = 0 THEN 'PEP_SANDBOX'
                                           WHEN n % 3 = 1 THEN 'IDENTIFICACIONES_SANDBOX'
                                           ELSE 'SANCIONES_SANDBOX' END
WHERE NOT EXISTS (
    SELECT 1 FROM api_evento a WHERE a.correlation_id = 'scl-ext-' || lpad(n::text, 3, '0')
);

INSERT INTO auditoria_sistema(empresa_id,usuario_id,accion,descripcion,entidad_afectada,entidad_id,valor_nuevo_json,direccion_ip,user_agent,fecha_evento)
SELECT '00000000-0000-0000-0000-000000000001',
       u.id,
       v.accion,
       v.descripcion,
       v.entidad,
       v.entidad_id,
       jsonb_build_object('origen','seed_dashboard','piiReal',false,'detalle',v.descripcion),
       '127.0.0.1',
       'RegulaWeb/aceptacion',
       now() - (v.orden * interval '17 minutes')
FROM (VALUES
(1,'LOGIN_EXITOSO','Inicio de sesión de administradora de empresa','usuarios','administrador@santaclara.local','administrador@santaclara.local'),
(2,'CONSULTAR_DASHBOARD','Consulta de tablero Admin Empresa','admin_empresa','dashboard','administrador@santaclara.local'),
(3,'CONSULTAR_ALERTAS','Revisión de alertas recientes','alertas_antifraude','listado','analista@santaclara.local'),
(4,'REASIGNAR_ALERTA','Reasignación de alerta por disponibilidad operativa','historial_asignacion','ALT-SCL-001','supervisor@santaclara.local'),
(5,'CONSULTAR_KYC','Consulta KYC mediante proveedor sandbox','api_externa','KYC_PERFIL','analista@santaclara.local'),
(6,'EXPORTAR_REPORTE','Generación de reporte operativo para revisión','reportes_ros','ROS-SCL-001','auditor@santaclara.local'),
(7,'SINCRONIZAR_CATALOGOS','Ejecución de sincronización manual de catálogos','admin_empresa_configuracion_local','CATALOG_SYNC','soporte.api@santaclara.local'),
(8,'VALIDAR_LICENCIA','Validación manual de licencia local','licencia_local','SCL-ASUNCION-01','administrador@santaclara.local')
) v(orden,accion,descripcion,entidad,entidad_id,email)
JOIN usuarios u ON u.email = v.email
WHERE NOT EXISTS (
    SELECT 1 FROM auditoria_sistema a
    WHERE a.accion = v.accion AND a.entidad_afectada = v.entidad AND a.entidad_id = v.entidad_id
);
