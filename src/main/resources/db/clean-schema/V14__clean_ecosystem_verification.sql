-- ============================================================================
-- Regula AML - Clean ecosystem verification V14
-- Vistas de control para verificar regula_clean completo.
-- ============================================================================

CREATE OR REPLACE VIEW vw_ecosystem_counts_control AS
SELECT *
FROM (
    VALUES
    ('SaaS/licenciamiento', 'empresa', (SELECT count(*) FROM empresa)),
    ('SaaS/licenciamiento', 'plan_licencia', (SELECT count(*) FROM plan_licencia)),
    ('SaaS/licenciamiento', 'suscripcion', (SELECT count(*) FROM suscripcion)),
    ('SaaS/licenciamiento', 'contrato', (SELECT count(*) FROM contrato)),
    ('SaaS/licenciamiento', 'pago', (SELECT count(*) FROM pago)),
    ('SaaS/licenciamiento', 'uso_suscripcion', (SELECT count(*) FROM uso_suscripcion)),
    ('RBAC', 'usuarios', (SELECT count(*) FROM usuarios)),
    ('RBAC', 'rol', (SELECT count(*) FROM rol)),
    ('RBAC', 'permiso', (SELECT count(*) FROM permiso)),
    ('RBAC', 'rol_permiso', (SELECT count(*) FROM rol_permiso)),
    ('RBAC', 'usuario_empresa', (SELECT count(*) FROM usuario_empresa)),
    ('RBAC', 'perfil_usuario', (SELECT count(*) FROM perfil_usuario)),
    ('RBAC', 'disponibilidad_usuario', (SELECT count(*) FROM disponibilidad_usuario)),
    ('RBAC', 'horario_laboral_usuario', (SELECT count(*) FROM horario_laboral_usuario)),
    ('Catalogos base', 'pais', (SELECT count(*) FROM pais)),
    ('Catalogos base', 'moneda', (SELECT count(*) FROM moneda)),
    ('Catalogos base', 'nivel_riesgo', (SELECT count(*) FROM nivel_riesgo)),
    ('Catalogos base', 'tipo_documento', (SELECT count(*) FROM tipo_documento)),
    ('Catalogos Paraguay', 'tipo_transaccion', (SELECT count(*) FROM tipo_transaccion)),
    ('Catalogos Paraguay', 'canal_transaccion', (SELECT count(*) FROM canal_transaccion)),
    ('Catalogos Paraguay', 'banco_emisor', (SELECT count(*) FROM banco_emisor)),
    ('Catalogos Paraguay', 'procesadora_tarjeta', (SELECT count(*) FROM procesadora_tarjeta)),
    ('Catalogos Paraguay', 'empe_operador', (SELECT count(*) FROM empe_operador)),
    ('Fuentes/listas', 'fuente_datos_riesgo', (SELECT count(*) FROM fuente_datos_riesgo)),
    ('Fuentes/listas', 'lista_regulatoria', (SELECT count(*) FROM lista_regulatoria)),
    ('Fuentes/listas', 'sujeto_riesgo', (SELECT count(*) FROM sujeto_riesgo)),
    ('Fuentes/listas', 'sujeto_riesgo_alias', (SELECT count(*) FROM sujeto_riesgo_alias)),
    ('Fuentes/listas', 'sujeto_riesgo_documento', (SELECT count(*) FROM sujeto_riesgo_documento)),
    ('Fuentes/listas', 'sujeto_riesgo_relacion', (SELECT count(*) FROM sujeto_riesgo_relacion)),
    ('Fuentes/listas', 'pais_riesgo', (SELECT count(*) FROM pais_riesgo)),
    ('KYC', 'persona', (SELECT count(*) FROM persona)),
    ('KYC', 'documento', (SELECT count(*) FROM documento)),
    ('KYC', 'perfil_cliente', (SELECT count(*) FROM perfil_cliente)),
    ('KYC', 'cliente_pep', (SELECT count(*) FROM cliente_pep)),
    ('KYC', 'cliente_observado', (SELECT count(*) FROM cliente_observado)),
    ('Motor', 'escenario', (SELECT count(*) FROM escenario)),
    ('Motor', 'accion', (SELECT count(*) FROM accion)),
    ('Motor', 'reglas_riesgo', (SELECT count(*) FROM reglas_riesgo)),
    ('Motor', 'control_importe', (SELECT count(*) FROM control_importe)),
    ('Motor', 'control_frecuencia', (SELECT count(*) FROM control_frecuencia)),
    ('Motor', 'horario_riesgo', (SELECT count(*) FROM horario_riesgo)),
    ('Motor', 'calendario_riesgo', (SELECT count(*) FROM calendario_riesgo)),
    ('Operaciones', 'transacciones', (SELECT count(*) FROM transacciones)),
    ('Operaciones', 'transaccion_detalle_snapshot', (SELECT count(*) FROM transaccion_detalle_snapshot)),
    ('Operaciones', 'ejecucion_reglas', (SELECT count(*) FROM ejecucion_reglas)),
    ('Operaciones', 'evaluaciones_riesgo', (SELECT count(*) FROM evaluaciones_riesgo)),
    ('Alertas/casos', 'alertas_antifraude', (SELECT count(*) FROM alertas_antifraude)),
    ('Alertas/casos', 'hallazgo_alerta', (SELECT count(*) FROM hallazgo_alerta)),
    ('Alertas/casos', 'coincidencia_lista_alerta', (SELECT count(*) FROM coincidencia_lista_alerta)),
    ('Alertas/casos', 'cliente_snapshot_alerta', (SELECT count(*) FROM cliente_snapshot_alerta)),
    ('Alertas/casos', 'consulta_kyc_alerta', (SELECT count(*) FROM consulta_kyc_alerta)),
    ('Alertas/casos', 'historial_asignacion', (SELECT count(*) FROM historial_asignacion)),
    ('Alertas/casos', 'estadistica_carga_analista', (SELECT count(*) FROM estadistica_carga_analista)),
    ('Alertas/casos', 'caso', (SELECT count(*) FROM caso)),
    ('Alertas/casos', 'caso_alerta', (SELECT count(*) FROM caso_alerta)),
    ('Alertas/casos', 'actuacion', (SELECT count(*) FROM actuacion)),
    ('Alertas/casos', 'comentario_caso', (SELECT count(*) FROM comentario_caso)),
    ('Alertas/casos', 'evidencia', (SELECT count(*) FROM evidencia)),
    ('Alertas/casos', 'evidencia_alerta', (SELECT count(*) FROM evidencia_alerta)),
    ('Alertas/casos', 'historial_estado_caso', (SELECT count(*) FROM historial_estado_caso)),
    ('Alertas/casos', 'resolucion_alerta', (SELECT count(*) FROM resolucion_alerta)),
    ('Alertas/casos', 'aprobacion_supervisor', (SELECT count(*) FROM aprobacion_supervisor)),
    ('Alertas/casos', 'decision_caso', (SELECT count(*) FROM decision_caso)),
    ('Alertas/casos', 'reportes_ros', (SELECT count(*) FROM reportes_ros)),
    ('Auditoria/externos', 'servicio_externo', (SELECT count(*) FROM servicio_externo)),
    ('Auditoria/externos', 'api_evento', (SELECT count(*) FROM api_evento)),
    ('Auditoria/externos', 'auditoria_sistema', (SELECT count(*) FROM auditoria_sistema))
) AS t(grupo_funcional, tabla, total)
ORDER BY grupo_funcional, tabla;

CREATE OR REPLACE VIEW vw_ecosystem_rls_control AS
WITH expected(table_name) AS (
    VALUES
    ('persona'), ('transacciones'), ('alertas_antifraude'), ('ejecucion_reglas'), ('evaluaciones_riesgo'),
    ('suscripcion'), ('contrato'), ('pago'), ('uso_suscripcion'), ('usuario_empresa'), ('perfil_usuario'),
    ('disponibilidad_usuario'), ('horario_laboral_usuario'), ('documento'), ('perfil_cliente'),
    ('cliente_pep'), ('cliente_observado'), ('escenario'), ('accion'), ('reglas_riesgo'),
    ('control_importe'), ('control_frecuencia'), ('horario_riesgo'), ('calendario_riesgo'),
    ('hallazgo_alerta'), ('coincidencia_lista_alerta'), ('transaccion_detalle_snapshot'),
    ('cliente_snapshot_alerta'), ('consulta_kyc_alerta'), ('historial_asignacion'),
    ('estadistica_carga_analista'), ('caso'), ('caso_alerta'), ('actuacion'), ('comentario_caso'),
    ('evidencia'), ('evidencia_alerta'), ('historial_estado_caso'), ('resolucion_alerta'),
    ('aprobacion_supervisor'), ('decision_caso'), ('reportes_ros'), ('api_evento'),
    ('auditoria_sistema')
)
SELECT
    e.table_name,
    c.relrowsecurity AS rls_habilitado,
    count(pol.polname) AS total_policies
FROM expected e
JOIN pg_class c ON c.relname = e.table_name
JOIN pg_namespace n ON n.oid = c.relnamespace AND n.nspname = 'public'
LEFT JOIN pg_policy pol ON pol.polrelid = c.oid
GROUP BY e.table_name, c.relrowsecurity
ORDER BY e.table_name;

CREATE OR REPLACE VIEW vw_ecosystem_audit_columns_control AS
WITH expected(table_name) AS (
    VALUES
    ('empresa'), ('usuarios'), ('moneda'), ('persona'), ('tipo_transaccion'), ('canal_transaccion'),
    ('banco_emisor'), ('procesadora_tarjeta'), ('empe_operador'), ('transacciones'),
    ('alertas_antifraude'), ('ejecucion_reglas'), ('evaluaciones_riesgo'),
    ('plan_licencia'), ('suscripcion'), ('contrato'), ('pago'), ('uso_suscripcion'),
    ('rol'), ('permiso'), ('rol_permiso'), ('usuario_empresa'), ('perfil_usuario'),
    ('disponibilidad_usuario'), ('horario_laboral_usuario'), ('tipo_documento'), ('documento'),
    ('perfil_cliente'), ('cliente_pep'), ('cliente_observado'), ('fuente_datos_riesgo'),
    ('lista_regulatoria'), ('sujeto_riesgo'), ('sujeto_riesgo_alias'), ('sujeto_riesgo_documento'),
    ('sujeto_riesgo_relacion'), ('pais_riesgo'), ('escenario'), ('accion'), ('reglas_riesgo'),
    ('control_importe'), ('control_frecuencia'), ('horario_riesgo'), ('calendario_riesgo'),
    ('hallazgo_alerta'), ('coincidencia_lista_alerta'), ('transaccion_detalle_snapshot'),
    ('cliente_snapshot_alerta'), ('consulta_kyc_alerta'), ('historial_asignacion'),
    ('estadistica_carga_analista'), ('caso'), ('caso_alerta'), ('actuacion'), ('comentario_caso'),
    ('evidencia'), ('evidencia_alerta'), ('historial_estado_caso'), ('resolucion_alerta'),
    ('aprobacion_supervisor'), ('decision_caso'), ('reportes_ros'), ('servicio_externo'),
    ('api_evento')
),
required_columns(column_name) AS (
    VALUES ('fecha_hora_creacion'), ('fecha_hora_modificacion'), ('usuario_creacion_id'), ('usuario_modificacion_id')
)
SELECT
    e.table_name,
    r.column_name,
    EXISTS (
        SELECT 1
        FROM information_schema.columns c
        WHERE c.table_schema = 'public'
          AND c.table_name = e.table_name
          AND c.column_name = r.column_name
    ) AS existe
FROM expected e
CROSS JOIN required_columns r
ORDER BY e.table_name, r.column_name;

CREATE OR REPLACE VIEW vw_ecosystem_legacy_tables_control AS
SELECT v.table_name, to_regclass('public.' || v.table_name) IS NOT NULL AS existe
FROM (VALUES ('auditoria'), ('estadisticas_cliente'), ('pais_catalogo_ext'), ('moneda_catalogo_ext')) AS v(table_name);

CREATE OR REPLACE VIEW vw_alertas_flujo_funcional_control AS
SELECT
    (SELECT count(*) FROM alertas_antifraude) AS alertas,
    (SELECT count(*) FROM alertas_antifraude WHERE analista_asignado_id IS NOT NULL) AS alertas_asignadas,
    (SELECT count(*) FROM hallazgo_alerta) AS hallazgos,
    (SELECT count(*) FROM cliente_snapshot_alerta) AS snapshots_cliente,
    (SELECT count(*) FROM transaccion_detalle_snapshot) AS snapshots_transaccion,
    (SELECT count(*) FROM evidencia_alerta) AS evidencias_alerta,
    (SELECT count(*) FROM resolucion_alerta) AS resoluciones,
    (SELECT count(*) FROM aprobacion_supervisor) AS aprobaciones,
    (SELECT count(*) FROM auditoria_sistema) AS auditorias;

CREATE OR REPLACE VIEW vw_rbac_funcional_control AS
SELECT
    (SELECT count(*) FROM usuarios) AS usuarios,
    (SELECT count(*) FROM rol) AS roles,
    (SELECT count(*) FROM permiso) AS permisos,
    (SELECT count(*) FROM rol_permiso) AS rol_permiso,
    (SELECT count(*) FROM usuario_empresa) AS usuario_empresa,
    (SELECT count(*) FROM disponibilidad_usuario WHERE estado = 'DISPONIBLE') AS analistas_disponibles;
