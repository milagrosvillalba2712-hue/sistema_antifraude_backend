CREATE TABLE IF NOT EXISTS api_error_catalogo (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    origen VARCHAR(120) NOT NULL,
    tipo_origen VARCHAR(20) NOT NULL DEFAULT 'INTERNA',
    api VARCHAR(100) NOT NULL,
    codigo_error VARCHAR(100) NOT NULL,
    status_code INTEGER NOT NULL,
    mensaje TEXT NOT NULL,
    detalles TEXT NOT NULL,
    categoria VARCHAR(80) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT true,
    fecha_hora_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_hora_modificacion TIMESTAMPTZ,
    usuario_creacion_id UUID REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id UUID REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT uk_api_error_catalogo UNIQUE (origen, codigo_error)
);

ALTER TABLE api_error_catalogo
    ADD COLUMN IF NOT EXISTS tipo_origen VARCHAR(20) NOT NULL DEFAULT 'INTERNA';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_api_error_catalogo_tipo_origen'
          AND conrelid = 'api_error_catalogo'::regclass
    ) THEN
        ALTER TABLE api_error_catalogo
            ADD CONSTRAINT ck_api_error_catalogo_tipo_origen
            CHECK (tipo_origen IN ('INTERNA', 'EXTERNA', 'CONTROL_PLANE'));
    END IF;
END $$;

DROP TRIGGER IF EXISTS trg_audit_api_error_catalogo ON api_error_catalogo;
CREATE TRIGGER trg_audit_api_error_catalogo
    BEFORE INSERT OR UPDATE ON api_error_catalogo
    FOR EACH ROW EXECUTE FUNCTION fn_set_audit_fields();

CREATE INDEX IF NOT EXISTS ix_api_error_catalogo_categoria
    ON api_error_catalogo (categoria, status_code, activo);

COMMENT ON TABLE api_error_catalogo IS 'Catalogo persistente de errores conocidos por API. Alimenta el monitor visual de errores sin listas hardcodeadas.';
COMMENT ON COLUMN api_error_catalogo.origen IS 'Servicio o proveedor que originó el error, sin prefijos visuales. Ejemplo: AUTH, ALERTAS, IDENTIFICACIONES_SANDBOX.';
COMMENT ON COLUMN api_error_catalogo.tipo_origen IS 'Clasificación técnica separada del origen visible: INTERNA, EXTERNA o CONTROL_PLANE.';
COMMENT ON COLUMN api_error_catalogo.codigo_error IS 'Codigo funcional o tecnico usado para mapear errores del backend, mocks o proveedores externos.';

UPDATE api_error_catalogo
SET tipo_origen = 'INTERNA',
    origen = replace(origen, 'INTERNA:', '')
WHERE origen LIKE 'INTERNA:%'
  AND NOT EXISTS (
      SELECT 1
      FROM api_error_catalogo clean
      WHERE clean.origen = replace(api_error_catalogo.origen, 'INTERNA:', '')
        AND clean.codigo_error = api_error_catalogo.codigo_error
  );

UPDATE api_error_catalogo
SET tipo_origen = 'EXTERNA',
    origen = replace(origen, 'EXTERNA:', '')
WHERE origen LIKE 'EXTERNA:%'
  AND NOT EXISTS (
      SELECT 1
      FROM api_error_catalogo clean
      WHERE clean.origen = replace(api_error_catalogo.origen, 'EXTERNA:', '')
        AND clean.codigo_error = api_error_catalogo.codigo_error
  );

DELETE FROM api_error_catalogo old
WHERE (old.origen LIKE 'INTERNA:%' OR old.origen LIKE 'EXTERNA:%')
  AND EXISTS (
      SELECT 1
      FROM api_error_catalogo clean
      WHERE clean.origen IN (replace(old.origen, 'INTERNA:', ''), replace(old.origen, 'EXTERNA:', ''))
        AND clean.codigo_error = old.codigo_error
  );

INSERT INTO api_error_catalogo(origen, tipo_origen, api, codigo_error, status_code, mensaje, detalles, categoria)
VALUES
('AUTH','INTERNA','AUTH','UNAUTHORIZED',401,'Token de autenticación requerido o inválido','Sesión no autenticada o token expirado.','SEGURIDAD'),
('AUTH','INTERNA','AUTH','BAD_CREDENTIALS',401,'Email o contraseña incorrectos','Credenciales inválidas en inicio de sesión.','SEGURIDAD'),
('AUTH','INTERNA','AUTH','ACCOUNT_LOCKED',403,'La cuenta fue bloqueada por exceso de intentos fallidos','Bloqueo temporal de seguridad.','SEGURIDAD'),
('AUTH','INTERNA','AUTH','ACCESS_DENIED',403,'No tiene permisos para acceder a este recurso','Permiso ausente para la operación solicitada.','SEGURIDAD'),
('ADMIN_EMPRESA','INTERNA','ADMIN_EMPRESA','TENANT_REQUERIDO',400,'No se pudo resolver la empresa del usuario autenticado','El token no contiene empresa activa.','TENANT'),
('ADMIN_EMPRESA','INTERNA','ADMIN_EMPRESA','EMPRESA_NO_ENCONTRADA',404,'Empresa no encontrada','La empresa del tenant no existe o no está activa.','TENANT'),
('LICENCIAMIENTO','INTERNA','LICENCIAMIENTO','EMPRESA_INACTIVA',400,'La empresa no se encuentra activa','El control de licencia bloqueó la operación.','LICENCIA'),
('LICENCIAMIENTO','INTERNA','LICENCIAMIENTO','MODULO_NO_INCLUIDO',400,'El módulo no está incluido en el plan contratado','El plan vigente no habilita el módulo solicitado.','LICENCIA'),
('LICENCIAMIENTO','INTERNA','LICENCIAMIENTO','LIMITE_TRANSACCIONES_PLAN',429,'Se superó el límite mensual de transacciones del plan','Consumo mensual excedido.','CUOTA'),
('LICENCIAMIENTO','INTERNA','LICENCIAMIENTO','LIMITE_KYC_PLAN',429,'Se superó el límite mensual de consultas KYC del plan','Consumo mensual excedido.','CUOTA'),
('LICENCIAMIENTO','INTERNA','LICENCIAMIENTO','LIMITE_REPORTES_PLAN',429,'Se superó el límite mensual de reportes del plan','Consumo mensual excedido.','CUOTA'),
('LICENCIAMIENTO','INTERNA','LICENCIAMIENTO','MODO_SOLO_LECTURA',403,'Licencia en período de gracia: solo lectura','El cliente puede consultar, pero no mutar información.','LICENCIA'),
('ALERTAS','INTERNA','ALERTAS','ALERTA_NO_ENCONTRADA',404,'Alerta no encontrada','No existe la alerta solicitada para la empresa actual.','NEGOCIO'),
('ALERTAS','INTERNA','ALERTAS','ALERTA_CERRADA',400,'La alerta cerrada no puede modificarse','Se intentó mutar una alerta ya finalizada.','NEGOCIO'),
('TRANSACCIONES','INTERNA','TRANSACCIONES','TRANSACCION_INVALIDA',400,'La transacción no cumple el contrato requerido','Faltan datos obligatorios o hay valores inválidos.','VALIDACION'),
('MOTOR_REGLAS','INTERNA','MOTOR_REGLAS','REGLA_INVALIDA',400,'La regla no cumple el contrato requerido','Condición, acción o catálogo inválido.','VALIDACION'),
('REPORTES','INTERNA','REPORTES','REPORT_GENERATION_ERROR',400,'Error al generar reporte ROS','No se pudo construir el reporte solicitado.','NEGOCIO'),
('API_INTERNA','INTERNA','API_INTERNA','VALIDATION_ERROR',400,'Errores de validación en los campos enviados','El payload no cumple validaciones de entrada.','VALIDACION'),
('API_INTERNA','INTERNA','API_INTERNA','INTERNAL_ERROR',500,'Error interno del servidor','Excepción no controlada. Revisar logs backend.','SISTEMA'),
('PROVEEDOR_EXTERNO','EXTERNA','PROVEEDOR_EXTERNO','TIMEOUT',504,'Timeout al consultar proveedor externo','La API externa no respondió dentro del tiempo máximo.','EXTERNA'),
('PROVEEDOR_EXTERNO','EXTERNA','PROVEEDOR_EXTERNO','HTTP_TRANSITORIO',502,'Error transitorio del proveedor externo','El proveedor respondió 429 o 5xx y se aplicó retry.','EXTERNA'),
('PROVEEDOR_EXTERNO','EXTERNA','PROVEEDOR_EXTERNO','HTTP_NO_TRANSITORIO',400,'Error no transitorio del proveedor externo','El proveedor externo respondió 4xx no recuperable.','EXTERNA'),
('PROVEEDOR_EXTERNO','EXTERNA','PROVEEDOR_EXTERNO','CONEXION_O_RESPUESTA',503,'Fallo de conexión o respuesta externa','No se pudo conectar, parsear o completar la respuesta del proveedor.','EXTERNA'),
('IDENTIFICACIONES','EXTERNA','IDENTIFICACIONES','EXTERNAL_TIMEOUT',504,'Timeout al consultar identificaciones','El proveedor de identidad/KYC no respondió dentro del tiempo máximo.','EXTERNA'),
('IDENTIFICACIONES','EXTERNA','IDENTIFICACIONES','EXTERNAL_4XX',400,'Proveedor externo rechazó la consulta','El documento o payload fue rechazado por el proveedor.','EXTERNA'),
('IDENTIFICACIONES','EXTERNA','IDENTIFICACIONES','EXTERNAL_5XX',502,'Proveedor externo respondió con error','Fallo del proveedor externo de identidad/KYC.','EXTERNA'),
('BCP_SANCIONES','EXTERNA','BCP_SANCIONES','EXTERNAL_TIMEOUT',504,'Timeout al consultar listas o sanciones','La fuente externa de sanciones no respondió a tiempo.','EXTERNA'),
('SEPRELAD_PEP','EXTERNA','SEPRELAD_PEP','EXTERNAL_TIMEOUT',504,'Timeout al consultar PEP','La fuente externa PEP no respondió a tiempo.','EXTERNA'),
('CONTROL_PLANE','CONTROL_PLANE','CONTROL_PLANE','CONTROL_PLANE_NO_DISPONIBLE',503,'Control Plane no disponible','No se pudo validar licencia o sincronizar catálogos.','EXTERNA'),
('CATALOG_SYNC','CONTROL_PLANE','CATALOG_SYNC','CATALOGO_SYNC_ERROR',503,'No se pudo sincronizar catálogos','Fallo de comunicación o versionamiento de catálogos.','EXTERNA'),
('PEP_SANDBOX','EXTERNA','PEP_SANDBOX','SERVICIO_NO_DISPONIBLE',503,'Servicio PEP sandbox no disponible','El mock o proveedor PEP no respondió para la prueba.','EXTERNA'),
('SANCIONES_SANDBOX','EXTERNA','SANCIONES_SANDBOX','SERVICIO_NO_DISPONIBLE',503,'Servicio de sanciones sandbox no disponible','El mock o proveedor de sanciones no respondió para la prueba.','EXTERNA'),
('IDENTIFICACIONES_SANDBOX','EXTERNA','IDENTIFICACIONES_SANDBOX','SERVICIO_NO_DISPONIBLE',503,'Servicio de identificaciones sandbox no disponible','El mock o proveedor de identificaciones no respondió para la prueba.','EXTERNA')
ON CONFLICT(origen, codigo_error) DO UPDATE
SET api = EXCLUDED.api,
    tipo_origen = EXCLUDED.tipo_origen,
    status_code = EXCLUDED.status_code,
    mensaje = EXCLUDED.mensaje,
    detalles = EXCLUDED.detalles,
    categoria = EXCLUDED.categoria,
    activo = true;
