-- Datos exclusivamente académicos. Esta ubicación solo se activa con el perfil demo.
INSERT INTO empresa (id, codigo, nombre, ruc, estado)
VALUES ('00000000-0000-0000-0000-000000000001', 'REGULA_DEMO', 'Empresa académica Regula', '80000000-0', 'ACTIVA')
ON CONFLICT (id) DO UPDATE
SET codigo = EXCLUDED.codigo,
    nombre = EXCLUDED.nombre,
    ruc = EXCLUDED.ruc,
    estado = EXCLUDED.estado;

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
ON CONFLICT (id) DO UPDATE SET email=EXCLUDED.email, nombre=EXCLUDED.nombre, activo=true;

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

-- Las 7 personas de referencia existen antes de R__demo_realistic_population?
-- No: este archivo corre en orden alfabético después de ese; por eso sus
-- documentos se crean aquí para mantener 1:1 con el manifiesto de verificación.
INSERT INTO documento (empresa_id,persona_id,tipo_documento_id,pais_emisor_id,numero_documento_enc,numero_documento_hash,es_principal,estado,activo)
SELECT p.empresa_id,p.id,(SELECT id FROM tipo_documento WHERE codigo='CI_PY'),
       (SELECT id FROM pais WHERE codigo_iso='PY'),NULL,
       hmac('REF-'||p.documento_hash,'regula-demo-hmac-key','sha256'),true,'VIGENTE',true
FROM persona p
WHERE p.empresa_id='00000000-0000-0000-0000-000000000001'
  AND p.nombre_razon_social IN ('Persona Legítima Demo','Persona Fraude Demo','Persona Sancionada Demo',
      'Persona PEP Demo','Persona Combinada Demo','Persona Falso Positivo Demo','Persona ROS Demo')
  AND NOT EXISTS (SELECT 1 FROM documento d WHERE d.empresa_id=p.empresa_id AND d.persona_id=p.id);

-- Perfil de cliente 1:1, igual patrón que R__demo_realistic_population.
INSERT INTO perfil_cliente(empresa_id,persona_id,nivel_riesgo_id,segmento,actividad_economica,ingreso_mensual_estimado,volumen_mensual_esperado,cantidad_operaciones_mensual,perfil_json)
SELECT p.empresa_id,p.id,nr.id,CASE WHEN p.id%3=0 THEN 'EMPRESARIAL' ELSE 'PERSONAL' END,'Actividad economica ficticia',5000000+(p.id%10)*1000000,12000000+(p.id%20)*2000000,10+(p.id%30),jsonb_build_object('demo',true)
FROM persona p JOIN nivel_riesgo nr ON nr.codigo=CASE WHEN p.id%10=0 THEN 'ALTO' WHEN p.id%3=0 THEN 'MEDIO' ELSE 'BAJO' END
WHERE p.empresa_id='00000000-0000-0000-0000-000000000001'
  AND p.nombre_razon_social IN ('Persona Legítima Demo','Persona Fraude Demo','Persona Sancionada Demo',
      'Persona PEP Demo','Persona Combinada Demo','Persona Falso Positivo Demo','Persona ROS Demo')
  AND NOT EXISTS (SELECT 1 FROM perfil_cliente x WHERE x.empresa_id=p.empresa_id AND x.persona_id=p.id);

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

INSERT INTO transacciones (fecha_transaccion,empresa_id,codigo,tipo_transaccion_id,canal_transaccion_id,infraestructura_pago,monto,moneda_id,estado,estado_evaluacion,procesada,persona_remitente_id,nombre_remitente,nombre_beneficiario,remitente_nombre_completo,beneficiario_nombre_completo,documento_remitente_hash,documento_beneficiario_hash,tipo_documento_remitente_id,pais_emisor_documento_remitente_id,tipo_documento_beneficiario_id,pais_emisor_documento_beneficiario_id)
SELECT TIMESTAMPTZ '2026-08-01 12:00:00-03' + (v.orden||' hours')::interval,
       '00000000-0000-0000-0000-000000000001',v.codigo,tt.id,ct.id,'API',v.monto,m.id,'PROCESADA','EVALUADA',true,p.id,p.nombre_razon_social,'Beneficiario Sintético',p.nombre_razon_social,'Beneficiario Sintético',
       p.documento_hash,hmac(v.codigo||'-BENEFICIARIO','regula-demo-hmac-key','sha256'),td.id,py.id,td.id,py.id
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
JOIN tipo_documento td ON td.codigo='CI_PY'
JOIN pais py ON py.codigo_iso='PY'
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

-- Enriquecimiento de las alertas/casos/resoluciones creados arriba. Debe vivir en
-- este archivo (no en R__demo_realistic_population, que corre alfabéticamente
-- antes) para que las alertas ya existan cuando se insertan los hallazgos.
INSERT INTO hallazgo_alerta(empresa_id,alerta_id,transaccion_id,fecha_transaccion,regla_riesgo_id,tipo_hallazgo,descripcion,score,severidad,detalle_json)
SELECT a.empresa_id,a.id,a.transaccion_id,a.fecha_transaccion,r.id,'REGLA_DISPARADA','Hallazgo sintetico '||a.codigo,a.score,a.severidad,jsonb_build_object('demo',true) FROM alertas_antifraude a LEFT JOIN reglas_riesgo r ON r.empresa_id=a.empresa_id AND r.codigo='REG-PY-05' WHERE NOT EXISTS(SELECT 1 FROM hallazgo_alerta h WHERE h.alerta_id=a.id);
INSERT INTO coincidencia_lista_alerta(empresa_id,alerta_id,sujeto_riesgo_id,lista_regulatoria_id,tipo_coincidencia,porcentaje_coincidencia,detalle_json)
SELECT a.empresa_id,a.id,s.id,s.lista_regulatoria_id,'NOMBRE_APROXIMADO',82+(a.id%15),jsonb_build_object('demo',true,'algoritmo','fuzzy-sintetico')
FROM alertas_antifraude a JOIN sujeto_riesgo s ON s.codigo='SR-DEMO-'||((a.id%40)+1)
WHERE NOT EXISTS(SELECT 1 FROM coincidencia_lista_alerta c WHERE c.alerta_id=a.id AND c.sujeto_riesgo_id=s.id);
INSERT INTO cliente_snapshot_alerta(empresa_id,alerta_id,persona_id,snapshot_json)
SELECT a.empresa_id,a.id,t.persona_remitente_id,jsonb_build_object('demo',true,'nombre',t.nombre_remitente,'perfil','sintetico') FROM alertas_antifraude a JOIN transacciones t ON t.id=a.transaccion_id AND t.fecha_transaccion=a.fecha_transaccion ON CONFLICT DO NOTHING;
INSERT INTO consulta_kyc_alerta(empresa_id,alerta_id,proveedor,estado,mensaje,respuesta_json)
SELECT a.empresa_id,a.id,'Mock Externo Regula','COMPLETADA','Consulta sintetica sanitizada','{}' FROM alertas_antifraude a
WHERE NOT EXISTS(SELECT 1 FROM consulta_kyc_alerta k WHERE k.alerta_id=a.id AND k.proveedor='Mock Externo Regula');
INSERT INTO historial_asignacion(empresa_id,alerta_id,usuario_nuevo_id,tipo,motivo,observacion)
SELECT a.empresa_id,a.id,a.analista_asignado_id,'ASIGNACION','Distribucion inicial demo','Asignacion sintetica' FROM alertas_antifraude a WHERE a.analista_asignado_id IS NOT NULL
AND NOT EXISTS(SELECT 1 FROM historial_asignacion h WHERE h.alerta_id=a.id AND h.tipo='ASIGNACION' AND h.motivo='Distribucion inicial demo');
INSERT INTO actuacion(empresa_id,caso_id,usuario_id,tipo_actuacion,descripcion)
SELECT c.empresa_id,c.id,c.responsable_id,'REVISION_INICIAL','Revision de transaccion, perfil y reglas' FROM caso c
WHERE NOT EXISTS(SELECT 1 FROM actuacion a WHERE a.caso_id=c.id AND a.tipo_actuacion='REVISION_INICIAL');
INSERT INTO comentario_caso(empresa_id,caso_id,usuario_id,comentario,visibilidad)
SELECT c.empresa_id,c.id,c.responsable_id,'Validar origen de fondos y consistencia del perfil sintetico.','INTERNA' FROM caso c
WHERE NOT EXISTS(SELECT 1 FROM comentario_caso x WHERE x.caso_id=c.id AND x.comentario='Validar origen de fondos y consistencia del perfil sintetico.');
INSERT INTO evidencia_alerta(empresa_id,alerta_id,evidencia_id)
SELECT e.empresa_id,ca.alerta_id,e.id FROM evidencia e JOIN caso_alerta ca ON ca.caso_id=e.caso_id ON CONFLICT DO NOTHING;
INSERT INTO historial_estado_caso(empresa_id,caso_id,estado_anterior,estado_nuevo,motivo,usuario_id)
SELECT c.empresa_id,c.id,NULL,c.estado,'Estado inicial del caso demo',c.responsable_id FROM caso c
WHERE NOT EXISTS(SELECT 1 FROM historial_estado_caso h WHERE h.caso_id=c.id AND h.motivo='Estado inicial del caso demo');
INSERT INTO aprobacion_supervisor(empresa_id,alerta_id,resolucion_alerta_id,supervisor_id,decision,observacion)
SELECT r.empresa_id,r.alerta_id,r.id,u.id,CASE WHEN r.id%2=0 THEN 'APROBADA' ELSE 'PENDIENTE' END,'Revision sintetica de supervisor' FROM resolucion_alerta r JOIN usuarios u ON u.email='supervisor@demo.regula.local'
WHERE NOT EXISTS(SELECT 1 FROM aprobacion_supervisor a WHERE a.resolucion_alerta_id=r.id AND a.supervisor_id=u.id);
INSERT INTO decision_caso(empresa_id,caso_id,resolucion_alerta_id,decision,descripcion,ejecutada)
SELECT c.empresa_id,c.id,r.id,CASE WHEN r.resultado='FALSO_POSITIVO' THEN 'LIBERAR_MOVIMIENTO' ELSE 'RETENER_Y_REPORTAR' END,'Decision sintetica',false FROM caso c JOIN caso_alerta ca ON ca.caso_id=c.id JOIN resolucion_alerta r ON r.alerta_id=ca.alerta_id WHERE NOT EXISTS(SELECT 1 FROM decision_caso d WHERE d.caso_id=c.id AND d.resolucion_alerta_id=r.id);
INSERT INTO estadistica_carga_analista(empresa_id,usuario_id,periodo,alertas_asignadas,alertas_cerradas,alertas_pendientes,tiempo_promedio_minutos)
SELECT d.empresa_id,d.usuario_id,DATE '2026-08-01',d.carga_actual,d.carga_actual/2,d.carga_actual-d.carga_actual/2,95.5 FROM disponibilidad_usuario d ON CONFLICT(empresa_id,usuario_id,periodo) DO UPDATE SET alertas_asignadas=EXCLUDED.alertas_asignadas;
