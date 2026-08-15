-- Recupera cuentas sinteticas de Regula Clean para pruebas de compatibilidad.
-- Todas se asignan al unico tenant de esta instalacion on-premise demo.
SELECT set_config('app.current_empresa_id', '00000000-0000-0000-0000-000000000001', false);

-- Nomenclatura productiva única: ADMINISTRADOR / SUPERVISOR / ANALISTA / AUDITOR.
-- (ADMIN_GENERAL, ADMIN_EMPRESA y GERENTE_SUPERVISOR fueron retirados del cliente.)
INSERT INTO rol(codigo,nombre,descripcion,alcance,tipo) VALUES
('ADMINISTRADOR','Administrador de la instalación','Configuración, usuarios y operación integral','EMPRESA','EMPRESA'),
('SUPERVISOR','Supervisor de cumplimiento','Supervisión de reglas, alertas y decisiones','EMPRESA','EMPRESA'),
('ANALISTA','Analista','Compatibilidad Regula Clean','EMPRESA','EMPRESA'),
('AUDITOR','Auditor','Compatibilidad Regula Clean','EMPRESA','EMPRESA')
ON CONFLICT(codigo) DO UPDATE SET nombre=EXCLUDED.nombre,descripcion=EXCLUDED.descripcion;

INSERT INTO rol_permiso(rol_id,permiso_id)
SELECT r.id,p.id FROM rol r CROSS JOIN permiso p
WHERE r.codigo='ADMINISTRADOR'
   OR (r.codigo='SUPERVISOR' AND p.codigo NOT IN ('EMPRESAS_EDITAR','PAGOS_GESTIONAR'))
   OR (r.codigo='ANALISTA' AND p.codigo IN ('CATALOGOS_VER','ALERTAS_VER','ALERTAS_ASIGNAR',
       'ALERTAS_RESOLVER','CASOS_VER','CASOS_GESTIONAR','REPORTES_VER','REPORTES_GENERAR','REGLAS_VER'))
   OR (r.codigo='AUDITOR' AND p.codigo IN ('CATALOGOS_VER','ALERTAS_VER','CASOS_VER','REPORTES_VER','AUDITORIA_VER'))
ON CONFLICT DO NOTHING;

WITH cuentas(email,nombre,rol_codigo) AS (VALUES
('andres.caballero@cliente.local','Andres Caballero Torres','ANALISTA'),
('beatriz.morales@cliente.local','Beatriz Morales Duarte','AUDITOR'),
('carlos.rios@cliente.local','Carlos Rios Pereira','ANALISTA'),
('claudia.vera@cliente.local','Claudia Vera Gimenez','SUPERVISOR'),
('diego.benitez@cliente.local','Diego Benitez Aquino','ANALISTA'),
('elena.caceres@cliente.local','Elena Caceres Ortiz','ADMINISTRADOR'),
('esteban.galeano@cliente.local','Esteban Galeano Rojas','ANALISTA'),
('fernando.vera@cliente.local','Fernando Vera Benitez','ANALISTA'),
('gabriel.torres@cliente.local','Gabriel Torres Franco','AUDITOR'),
('hector.sosa@cliente.local','Hector Sosa Franco','SUPERVISOR'),
('jose.aquino@cliente.local','Jose Aquino Silva','ANALISTA'),
('karina.mendez@cliente.local','Karina Mendez Duarte','ANALISTA'),
('lorena.silva@cliente.local','Lorena Silva Medina','AUDITOR'),
('maria.riveros@cliente.local','Maria Riveros Lopez','ANALISTA'),
('martin.ferreira@cliente.local','Martin Ferreira Acosta','ADMINISTRADOR'),
('noelia.rojas@cliente.local','Noelia Rojas Caballero','ANALISTA'),
('paola.duarte@cliente.local','Paola Duarte Vera','ADMINISTRADOR'),
('patricia.nunez@cliente.local','Patricia Nunez Gimenez','ANALISTA'),
('sofia.ortiz@cliente.local','Sofia Ortiz Riveros','SUPERVISOR'),
('valeria.romero@cliente.local','Valeria Romero Sosa','ANALISTA')
)
INSERT INTO usuarios(id,email,nombre,password_hash,activo)
SELECT md5(c.email)::uuid,c.email,c.nombre,crypt('Regula2026!',gen_salt('bf')),true FROM cuentas c
ON CONFLICT(id) DO UPDATE SET email=EXCLUDED.email,nombre=EXCLUDED.nombre,password_hash=EXCLUDED.password_hash,
activo=true,intentos_fallidos=0,bloqueado_hasta=NULL;

WITH cuentas(email,rol_codigo) AS (VALUES
('andres.caballero@cliente.local','ANALISTA'),
('beatriz.morales@cliente.local','AUDITOR'),('carlos.rios@cliente.local','ANALISTA'),
('claudia.vera@cliente.local','SUPERVISOR'),('diego.benitez@cliente.local','ANALISTA'),
('elena.caceres@cliente.local','ADMINISTRADOR'),('esteban.galeano@cliente.local','ANALISTA'),
('fernando.vera@cliente.local','ANALISTA'),('gabriel.torres@cliente.local','AUDITOR'),
('hector.sosa@cliente.local','SUPERVISOR'),('jose.aquino@cliente.local','ANALISTA'),
('karina.mendez@cliente.local','ANALISTA'),('lorena.silva@cliente.local','AUDITOR'),
('maria.riveros@cliente.local','ANALISTA'),
('martin.ferreira@cliente.local','ADMINISTRADOR'),('noelia.rojas@cliente.local','ANALISTA'),
('paola.duarte@cliente.local','ADMINISTRADOR'),('patricia.nunez@cliente.local','ANALISTA'),
('sofia.ortiz@cliente.local','SUPERVISOR'),
('valeria.romero@cliente.local','ANALISTA'))
INSERT INTO usuario_empresa(empresa_id,usuario_id,rol_id,estado)
SELECT '00000000-0000-0000-0000-000000000001',u.id,r.id,'ACTIVO'
FROM cuentas c JOIN usuarios u ON u.email=c.email JOIN rol r ON r.codigo=c.rol_codigo
ON CONFLICT DO NOTHING;

INSERT INTO perfil_usuario(empresa_id,usuario_id,cargo,area,telefono)
SELECT DISTINCT ON (u.id) '00000000-0000-0000-0000-000000000001',u.id,
CASE r.codigo WHEN 'ANALISTA' THEN 'Analista AML' WHEN 'AUDITOR' THEN 'Auditor interno'
WHEN 'SUPERVISOR' THEN 'Supervisor de cumplimiento' ELSE 'Administrador' END,
CASE r.codigo WHEN 'AUDITOR' THEN 'Auditoria' WHEN 'ANALISTA' THEN 'Prevencion' ELSE 'Cumplimiento' END,NULL
FROM usuarios u JOIN usuario_empresa ue ON ue.usuario_id=u.id
JOIN rol r ON r.id=ue.rol_id
WHERE ue.empresa_id='00000000-0000-0000-0000-000000000001'
AND (u.email LIKE '%@cliente.local' OR u.email LIKE '%@regula.local')
ORDER BY u.id, CASE WHEN r.codigo IN ('ADMINISTRADOR','SUPERVISOR') THEN 0 ELSE 1 END
ON CONFLICT(empresa_id,usuario_id) DO UPDATE SET cargo=EXCLUDED.cargo,area=EXCLUDED.area;

INSERT INTO disponibilidad_usuario(empresa_id,usuario_id,estado,carga_actual,capacidad_maxima)
SELECT '00000000-0000-0000-0000-000000000001',u.id,'DISPONIBLE',0,20
FROM usuarios u JOIN usuario_empresa ue ON ue.usuario_id=u.id JOIN rol r ON r.id=ue.rol_id
WHERE ue.empresa_id='00000000-0000-0000-0000-000000000001' AND r.codigo='ANALISTA'
ON CONFLICT(empresa_id,usuario_id) DO UPDATE SET estado='DISPONIBLE',capacidad_maxima=20;
