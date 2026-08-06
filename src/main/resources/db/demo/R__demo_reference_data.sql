-- Datos exclusivamente académicos. Esta ubicación solo se activa con el perfil demo.
INSERT INTO empresa (id, codigo, nombre, ruc, estado)
VALUES ('00000000-0000-0000-0000-000000000001', 'REGULA_DEMO', 'Empresa académica Regula', '80000000-0', 'ACTIVA')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO pais (codigo_iso, codigo_iso3, nombre, activo)
VALUES ('PY', 'PRY', 'Paraguay', true)
ON CONFLICT (codigo_iso) DO UPDATE
SET codigo_iso3 = EXCLUDED.codigo_iso3,
    nombre = EXCLUDED.nombre,
    activo = EXCLUDED.activo;

INSERT INTO moneda (codigo_iso, nombre, nombre_en, fuente, activo)
VALUES ('PYG', 'Guaraní paraguayo', 'Paraguayan guarani', 'ISO 4217 - demo', true)
ON CONFLICT (codigo_iso) DO UPDATE
SET nombre = EXCLUDED.nombre,
    nombre_en = EXCLUDED.nombre_en,
    fuente = EXCLUDED.fuente,
    activo = EXCLUDED.activo;

-- Todo lo siguiente es sintético y exclusivo del perfil demo.
SELECT set_config('app.current_empresa_id', '00000000-0000-0000-0000-000000000001', false);
SELECT set_config('app.current_usuario_id', '00000000-0000-0000-0000-000000000101', false);

INSERT INTO usuarios (id, email, nombre, password_hash, activo)
VALUES
  ('00000000-0000-0000-0000-000000000101', 'admin@demo.regula.local', 'Administración Académica', crypt('RegulaDemo2026!', gen_salt('bf')), true),
  ('00000000-0000-0000-0000-000000000102', 'analista@demo.regula.local', 'Analista Académico', crypt('RegulaDemo2026!', gen_salt('bf')), true)
ON CONFLICT (email) DO UPDATE SET nombre=EXCLUDED.nombre, activo=true;

INSERT INTO rol (codigo, nombre, descripcion, alcance, tipo)
VALUES
  ('ADMIN_DEMO', 'Administrador demo', 'Rol no productivo para defensa académica', 'EMPRESA', 'EMPRESA'),
  ('ANALISTA_DEMO', 'Analista demo', 'Rol no productivo para investigación académica', 'EMPRESA', 'EMPRESA')
ON CONFLICT (codigo) DO UPDATE SET nombre=EXCLUDED.nombre, descripcion=EXCLUDED.descripcion;

INSERT INTO permiso (codigo, nombre, descripcion, modulo, accion)
VALUES
  ('DEMO_ADMIN', 'Administrar demo', 'Permiso académico integral', 'Demo', 'GESTIONAR'),
  ('DEMO_INVESTIGAR', 'Investigar demo', 'Permiso académico de investigación', 'Demo', 'RESOLVER')
ON CONFLICT (codigo) DO UPDATE SET nombre=EXCLUDED.nombre;

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM rol r CROSS JOIN permiso p
WHERE (r.codigo='ADMIN_DEMO' AND p.codigo IN ('DEMO_ADMIN','DEMO_INVESTIGAR'))
   OR (r.codigo='ANALISTA_DEMO' AND p.codigo='DEMO_INVESTIGAR')
ON CONFLICT DO NOTHING;

INSERT INTO usuario_empresa (empresa_id, usuario_id, rol_id, estado)
SELECT '00000000-0000-0000-0000-000000000001', u.id, r.id, 'ACTIVO'
FROM (VALUES ('admin@demo.regula.local','ADMIN_DEMO'),('analista@demo.regula.local','ANALISTA_DEMO')) v(email,rol_codigo)
JOIN usuarios u ON u.email=v.email JOIN rol r ON r.codigo=v.rol_codigo
ON CONFLICT DO NOTHING;

INSERT INTO nivel_riesgo (codigo,nombre,orden) VALUES
('BAJO','Bajo',1),('MEDIO','Medio',2),('ALTO','Alto',3)
ON CONFLICT (codigo) DO NOTHING;
INSERT INTO tipo_transaccion (codigo,nombre,categoria) VALUES ('TRANSFERENCIA_DEMO','Transferencia académica','TRANSFERENCIA') ON CONFLICT (codigo) DO NOTHING;
INSERT INTO canal_transaccion (codigo,nombre) VALUES ('API_DEMO','API académica') ON CONFLICT (codigo) DO NOTHING;
INSERT INTO producto (codigo,nombre) VALUES ('CUENTA_DEMO','Cuenta académica') ON CONFLICT (codigo) DO NOTHING;

INSERT INTO persona (empresa_id,tipo_persona,nombre_razon_social,documento_hash)
SELECT '00000000-0000-0000-0000-000000000001','FISICA',v.nombre,hmac(v.documento,'regula-demo-only','sha256')
FROM (VALUES
 ('DOC-DEMO-LEGITIMO','Persona Legítima Demo'),
 ('DOC-DEMO-FRAUDE','Persona Fraude Demo'),
 ('DOC-DEMO-SANCION','Persona Sancionada Demo'),
 ('DOC-DEMO-PEP','Persona PEP Demo'),
 ('DOC-DEMO-COMBINADO','Persona Combinada Demo'),
 ('DOC-DEMO-FALSO-POSITIVO','Persona Falso Positivo Demo'),
 ('DOC-DEMO-ROS','Persona ROS Demo')
) v(documento,nombre)
ON CONFLICT (empresa_id,documento_hash) DO NOTHING;

INSERT INTO cliente_pep (empresa_id,persona_id,tipo_pep,cargo,institucion,fecha_inicio,estado,detalle_json)
SELECT p.empresa_id,p.id,'NACIONAL','Cargo sintético','Institución sintética',DATE '2025-01-01','ACTIVO','{"demo":true}'
FROM persona p WHERE p.nombre_razon_social IN ('Persona PEP Demo','Persona Combinada Demo')
  AND NOT EXISTS (SELECT 1 FROM cliente_pep x WHERE x.empresa_id=p.empresa_id AND x.persona_id=p.id);

INSERT INTO cliente_observado (empresa_id,persona_id,motivo,severidad,estado,observacion)
SELECT p.empresa_id,p.id,'Lista académica sintética','ALTA','ACTIVO','Nunca usar como dato regulatorio real'
FROM persona p WHERE p.nombre_razon_social IN ('Persona Sancionada Demo','Persona Combinada Demo','Persona ROS Demo')
  AND NOT EXISTS (SELECT 1 FROM cliente_observado x WHERE x.empresa_id=p.empresa_id AND x.persona_id=p.id);

INSERT INTO escenario (empresa_id,codigo,nombre,descripcion,activo)
VALUES ('00000000-0000-0000-0000-000000000001','ESC_DEMO','Escenarios de tesis','Casos deterministas y sintéticos',true)
ON CONFLICT (empresa_id,codigo) DO NOTHING;
INSERT INTO accion (empresa_id,codigo,nombre,tipo_accion,descripcion,activo)
VALUES ('00000000-0000-0000-0000-000000000001','REVISAR_DEMO','Revisión académica','GENERAR_ALERTA','Acción de demostración',true)
ON CONFLICT (empresa_id,codigo) DO NOTHING;
INSERT INTO reglas_riesgo (empresa_id,escenario_id,accion_id,codigo,nombre,severidad,score_base,condiciones_json,acciones_json,activa,estado,version,condicion,parametros,prioridad,tipo_regla)
SELECT '00000000-0000-0000-0000-000000000001',e.id,a.id,'REGLA_DEMO_001','Regla determinista académica','ALTA',85,'{"demo":true}','{"accion":"alertar"}',true,'ACTIVA',1,'monto > 10000000','{"umbral":10000000}',1,'MONTO'
FROM escenario e JOIN accion a ON a.empresa_id=e.empresa_id
WHERE e.codigo='ESC_DEMO' AND a.codigo='REVISAR_DEMO'
  AND NOT EXISTS (SELECT 1 FROM reglas_riesgo r WHERE r.empresa_id=e.empresa_id AND r.codigo='REGLA_DEMO_001');

INSERT INTO transacciones (fecha_transaccion,empresa_id,codigo,tipo_transaccion_id,canal_transaccion_id,infraestructura_pago,monto,moneda_id,estado,estado_evaluacion,procesada,persona_remitente_id,nombre_remitente,nombre_beneficiario)
SELECT TIMESTAMPTZ '2026-08-01 12:00:00-03' + (v.orden||' hours')::interval,
       '00000000-0000-0000-0000-000000000001',v.codigo,tt.id,ct.id,'API',v.monto,m.id,'PROCESADA','EVALUADA',true,p.id,p.nombre_razon_social,'Beneficiario Sintético'
FROM (VALUES
 (1,'TX-DEMO-LEGITIMO','Persona Legítima Demo',1000000::numeric),
 (2,'TX-DEMO-FRAUDE','Persona Fraude Demo',25000000::numeric),
 (3,'TX-DEMO-SANCION','Persona Sancionada Demo',5000000::numeric),
 (4,'TX-DEMO-PEP','Persona PEP Demo',7000000::numeric),
 (5,'TX-DEMO-COMBINADO','Persona Combinada Demo',30000000::numeric),
 (6,'TX-DEMO-FALSO-POSITIVO','Persona Falso Positivo Demo',11000000::numeric),
 (7,'TX-DEMO-ROS','Persona ROS Demo',50000000::numeric)
) v(orden,codigo,persona_nombre,monto)
JOIN tipo_transaccion tt ON tt.codigo='TRANSFERENCIA_DEMO'
JOIN canal_transaccion ct ON ct.codigo='API_DEMO'
JOIN moneda m ON m.codigo_iso='PYG'
JOIN persona p ON p.nombre_razon_social=v.persona_nombre AND p.empresa_id='00000000-0000-0000-0000-000000000001'
WHERE NOT EXISTS (SELECT 1 FROM transacciones t WHERE t.empresa_id='00000000-0000-0000-0000-000000000001' AND t.codigo=v.codigo);

INSERT INTO alertas_antifraude (empresa_id,transaccion_id,fecha_transaccion,codigo,severidad,score,motivo,estado,analista_asignado_id)
SELECT t.empresa_id,t.id,t.fecha_transaccion,'ALT-'||t.codigo,
       CASE WHEN t.codigo IN ('TX-DEMO-COMBINADO','TX-DEMO-ROS') THEN 'CRITICA' ELSE 'ALTA' END,
       CASE WHEN t.codigo='TX-DEMO-FALSO-POSITIVO' THEN 55 ELSE 90 END,
       replace(t.codigo,'TX-DEMO-','Escenario académico: '),'PENDIENTE_APROBACION',u.id
FROM transacciones t JOIN usuarios u ON u.email='analista@demo.regula.local'
WHERE t.codigo <> 'TX-DEMO-LEGITIMO' AND t.codigo LIKE 'TX-DEMO-%'
  AND NOT EXISTS (SELECT 1 FROM alertas_antifraude a WHERE a.empresa_id=t.empresa_id AND a.codigo='ALT-'||t.codigo);

INSERT INTO caso (empresa_id,codigo,titulo,descripcion,estado,severidad,responsable_id)
SELECT a.empresa_id,'CAS-'||a.codigo,'Investigación '||a.codigo,'Caso completamente sintético','EN_INVESTIGACION',a.severidad,a.analista_asignado_id
FROM alertas_antifraude a WHERE a.codigo LIKE 'ALT-TX-DEMO-%'
ON CONFLICT (empresa_id,codigo) DO NOTHING;
INSERT INTO caso_alerta (empresa_id,caso_id,alerta_id)
SELECT c.empresa_id,c.id,a.id FROM caso c JOIN alertas_antifraude a ON c.codigo='CAS-'||a.codigo AND c.empresa_id=a.empresa_id
ON CONFLICT DO NOTHING;
INSERT INTO evidencia (empresa_id,caso_id,nombre,descripcion,tipo_archivo,extension,mime_type,estado,referencia_archivo,cargado_por_id)
SELECT c.empresa_id,c.id,'evidencia-'||c.codigo||'.txt','Metadato sintético; no contiene archivo real','TEXTO','txt','text/plain','CARGADA','demo/'||c.codigo||'.txt',c.responsable_id
FROM caso c WHERE c.codigo LIKE 'CAS-ALT-TX-DEMO-%' AND NOT EXISTS (SELECT 1 FROM evidencia e WHERE e.caso_id=c.id AND e.nombre='evidencia-'||c.codigo||'.txt');
INSERT INTO resolucion_alerta (empresa_id,alerta_id,analista_id,resultado,conclusion,justificacion,requiere_ros,estado,decision,evidencia_descripcion,usuario_id)
SELECT a.empresa_id,a.id,a.analista_asignado_id,
       CASE WHEN a.codigo='ALT-TX-DEMO-FALSO-POSITIVO' THEN 'FALSO_POSITIVO' ELSE 'FRAUDE_CONFIRMADO' END,
       'Resolución académica determinista','Basada exclusivamente en escenario sintético',a.codigo='ALT-TX-DEMO-ROS','PENDIENTE_APROBACION',
       CASE WHEN a.codigo='ALT-TX-DEMO-FALSO-POSITIVO' THEN 'LIBERAR' ELSE 'ESCALAR' END,'Evidencia sintética',a.analista_asignado_id
FROM alertas_antifraude a WHERE a.codigo LIKE 'ALT-TX-DEMO-%'
  AND NOT EXISTS (SELECT 1 FROM resolucion_alerta r WHERE r.alerta_id=a.id);
INSERT INTO reportes_ros (empresa_id,caso_id,alerta_id,codigo,estado,descripcion_sospecha,soporte_referencia,reporte_json,generado_por,nombre_archivo)
SELECT c.empresa_id,c.id,a.id,'ROS-DEMO-001','BORRADOR','Sospecha exclusivamente académica','demo/ros', '{"demo":true}',c.responsable_id,'ros-demo-001.json'
FROM caso c JOIN caso_alerta ca ON ca.caso_id=c.id JOIN alertas_antifraude a ON a.id=ca.alerta_id
WHERE a.codigo='ALT-TX-DEMO-ROS'
ON CONFLICT (empresa_id,codigo) DO NOTHING;
INSERT INTO auditoria_sistema (empresa_id,usuario_id,accion,descripcion,entidad_afectada,entidad_id,valor_nuevo_json,direccion_ip,user_agent)
SELECT '00000000-0000-0000-0000-000000000001',u.id,'SEED_DEMO','Carga reproducible de escenarios sintéticos','demo','REGULA_DEMO','{"demo":true}','127.0.0.1','RegulaDemoSeed/1'
FROM usuarios u WHERE u.email='admin@demo.regula.local'
  AND NOT EXISTS (SELECT 1 FROM auditoria_sistema x WHERE x.accion='SEED_DEMO' AND x.entidad_id='REGULA_DEMO');
