-- ============================================================================
-- V24: Limites de roles por plan, solicitudes de roles adicionales y pagos.
-- Agrega columnas de limite por rol a plan_licencia, crea tabla
-- solicitud_roles y pago para el flujo de compra de roles adicionales.
-- ============================================================================

-- 1. Limites de roles por plan en plan_licencia
ALTER TABLE plan_licencia
    ADD COLUMN IF NOT EXISTS limite_administradores integer DEFAULT 1,
    ADD COLUMN IF NOT EXISTS limite_supervisores integer DEFAULT 1,
    ADD COLUMN IF NOT EXISTS limite_analistas integer DEFAULT 2,
    ADD COLUMN IF NOT EXISTS limite_auditores integer DEFAULT 1;

-- Actualizar limites existentes segun los planes conocidos
UPDATE plan_licencia SET
    limite_administradores = 1,
    limite_supervisores = 1,
    limite_analistas = CASE codigo
        WHEN 'BASICO' THEN 2
        WHEN 'ESTANDAR' THEN 4
        WHEN 'PREMIUM' THEN 50
        ELSE 2
    END,
    limite_auditores = CASE codigo
        WHEN 'BASICO' THEN 1
        WHEN 'ESTANDAR' THEN 2
        WHEN 'PREMIUM' THEN 20
        ELSE 1
    END
WHERE codigo IN ('BASICO', 'ESTANDAR', 'PREMIUM');

-- 2. Tabla de solicitudes de roles adicionales
CREATE TABLE IF NOT EXISTS solicitud_roles (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    estado varchar(30) NOT NULL DEFAULT 'PENDIENTE',
    rol_solicitado varchar(60) NOT NULL,
    cantidad integer NOT NULL DEFAULT 1,
    precio_unitario numeric(18,2) NOT NULL DEFAULT 0,
    precio_total numeric(18,2) NOT NULL DEFAULT 0,
    observacion text,
    aprobado_por uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    aprobado_en timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id),
    usuario_modificacion_id uuid REFERENCES usuarios(id),
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    CONSTRAINT ck_solicitud_roles_estado CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA', 'PAGADA', 'CANCELADA')),
    CONSTRAINT ck_solicitud_roles_cantidad CHECK (cantidad > 0)
);

CREATE INDEX IF NOT EXISTS idx_solicitud_roles_empresa ON solicitud_roles(empresa_id, estado);

-- 3. Tabla de pagos (entity JPA faltante - alinear con la existente en V1)
-- La tabla pago ya existe en V10, solo agregamos columnas para el nuevo modelo
ALTER TABLE pago
    ADD COLUMN IF NOT EXISTS solicitud_roles_id bigint REFERENCES solicitud_roles(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD COLUMN IF NOT EXISTS concepto varchar(120),
    ADD COLUMN IF NOT EXISTS metodo_pago varchar(60) DEFAULT 'SIMULADO',
    ADD COLUMN IF NOT EXISTS referencia_externa varchar(200);

-- 4. Tabla de detalle de roles adquiridos por empresa (tracking de roles comprados extra)
CREATE TABLE IF NOT EXISTS roles_adquiridos (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    solicitud_roles_id bigint NOT NULL REFERENCES solicitud_roles(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    rol_codigo varchar(60) NOT NULL,
    cantidad integer NOT NULL DEFAULT 1,
    fecha_adquisicion timestamptz NOT NULL DEFAULT now(),
    activo boolean NOT NULL DEFAULT true,
    CONSTRAINT ck_roles_adquiridos_cantidad CHECK (cantidad > 0)
);

CREATE INDEX IF NOT EXISTS idx_roles_adquiridos_empresa ON roles_adquiridos(empresa_id, activo);
