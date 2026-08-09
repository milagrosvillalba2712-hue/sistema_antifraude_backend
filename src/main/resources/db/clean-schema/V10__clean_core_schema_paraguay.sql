-- =============================================================================
-- REGULA AML PARAGUAY - REFACTORIZACION LIMPIA DE TRANSACCIONES
-- PostgreSQL 14+
-- Objetivo: modelo multi-tenant UUID, RLS, particionamiento mensual, PII protegida
-- y soporte de tipos transaccionales Paraguay definidos en TIPOS_TRANSACCIONES_AML.md.
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- -----------------------------------------------------------------------------
-- 1. Infraestructura base minima
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS empresa (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo varchar(40) NOT NULL UNIQUE,
    nombre varchar(180) NOT NULL,
    ruc varchar(40),
    estado varchar(30) NOT NULL DEFAULT 'ACTIVA',
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid,
    usuario_modificacion_id uuid,
    CONSTRAINT ck_empresa_estado CHECK (estado IN ('ACTIVA', 'SUSPENDIDA', 'CANCELADA'))
);

CREATE TABLE IF NOT EXISTS usuarios (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email varchar(180) NOT NULL UNIQUE,
    nombre varchar(180) NOT NULL,
    password_hash text NOT NULL,
    activo boolean NOT NULL DEFAULT true,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

ALTER TABLE empresa
    DROP CONSTRAINT IF EXISTS fk_empresa_usuario_creacion;

ALTER TABLE empresa
    ADD CONSTRAINT fk_empresa_usuario_creacion
    FOREIGN KEY (usuario_creacion_id) REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE empresa
    DROP CONSTRAINT IF EXISTS fk_empresa_usuario_modificacion;

ALTER TABLE empresa
    ADD CONSTRAINT fk_empresa_usuario_modificacion
    FOREIGN KEY (usuario_modificacion_id) REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE;

CREATE TABLE IF NOT EXISTS pais (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_iso char(2) NOT NULL UNIQUE,
    codigo_iso3 char(3),
    nombre varchar(120) NOT NULL,
    activo boolean NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS moneda (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_iso varchar(3) NOT NULL UNIQUE,
    nombre varchar(120) NOT NULL,
    nombre_en varchar(120),
    fuente varchar(120),
    activo boolean NOT NULL DEFAULT true,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS nivel_riesgo (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo varchar(20) NOT NULL UNIQUE,
    nombre varchar(80) NOT NULL,
    orden smallint NOT NULL,
    activo boolean NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS persona (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    tipo_persona varchar(20) NOT NULL,
    nombre_razon_social varchar(220) NOT NULL,
    documento_hash bytea,
    activo boolean NOT NULL DEFAULT true,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_persona_tipo CHECK (tipo_persona IN ('FISICA', 'JURIDICA'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_persona_empresa_documento_hash
ON persona (empresa_id, documento_hash);

-- -----------------------------------------------------------------------------
-- 2. Catalogos transaccionales Paraguay
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS tipo_transaccion (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo varchar(50) NOT NULL UNIQUE,
    nombre varchar(160) NOT NULL,
    categoria varchar(60) NOT NULL,
    descripcion text,
    activo boolean NOT NULL DEFAULT true,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS canal_transaccion (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo varchar(30) NOT NULL UNIQUE,
    nombre varchar(120) NOT NULL,
    descripcion text,
    activo boolean NOT NULL DEFAULT true,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS banco_emisor (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo varchar(40) NOT NULL UNIQUE,
    nombre varchar(180) NOT NULL,
    tipo_entidad varchar(40) NOT NULL,
    participante_sipap boolean NOT NULL DEFAULT false,
    activo boolean NOT NULL DEFAULT true,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_banco_emisor_tipo CHECK (tipo_entidad IN ('BANCO', 'FINANCIERA', 'COOPERATIVA', 'EMPE', 'PSP', 'BCP', 'OTRA'))
);

CREATE TABLE IF NOT EXISTS procesadora_tarjeta (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo varchar(40) NOT NULL UNIQUE,
    nombre varchar(160) NOT NULL,
    activo boolean NOT NULL DEFAULT true,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS empe_operador (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo varchar(40) NOT NULL UNIQUE,
    nombre varchar(160) NOT NULL,
    entidad_patrocinadora_id bigint REFERENCES banco_emisor(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    activo boolean NOT NULL DEFAULT true,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- -----------------------------------------------------------------------------
-- 3. Auditoria CRUD automatica
-- -----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_set_audit_fields()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    v_usuario_id uuid;
BEGIN
    v_usuario_id := NULLIF(current_setting('app.current_usuario_id', true), '')::uuid;

    IF TG_OP = 'INSERT' THEN
        NEW.fecha_hora_creacion := COALESCE(NEW.fecha_hora_creacion, now());
        NEW.usuario_creacion_id := COALESCE(NEW.usuario_creacion_id, v_usuario_id);
    ELSIF TG_OP = 'UPDATE' THEN
        NEW.fecha_hora_modificacion := now();
        NEW.usuario_modificacion_id := COALESCE(v_usuario_id, NEW.usuario_modificacion_id);
    END IF;

    RETURN NEW;
END;
$$;

-- -----------------------------------------------------------------------------
-- 4. Tabla particionada de transacciones
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS transacciones (
    id bigint GENERATED ALWAYS AS IDENTITY,
    fecha_transaccion timestamptz NOT NULL,
    transaction_uuid uuid NOT NULL DEFAULT gen_random_uuid(),
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    codigo varchar(60) NOT NULL,

    fecha_procesamiento timestamptz,
    fecha_liquidacion timestamptz,
    estado varchar(30) NOT NULL DEFAULT 'PENDIENTE',
    estado_evaluacion varchar(30) NOT NULL DEFAULT 'PENDIENTE',
    procesada boolean NOT NULL DEFAULT false,

    tipo_transaccion_id bigint NOT NULL REFERENCES tipo_transaccion(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    canal_transaccion_id bigint NOT NULL REFERENCES canal_transaccion(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    infraestructura_pago varchar(30) NOT NULL,
    modulo_sipap varchar(30),
    subtipo_transaccion varchar(60),

    monto numeric(18,2) NOT NULL,
    moneda_id bigint NOT NULL REFERENCES moneda(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    monto_destino numeric(18,2),
    moneda_destino_id bigint REFERENCES moneda(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    tipo_cambio numeric(18,8),
    comision numeric(18,2),
    impuesto numeric(18,2),
    monto_total_debitado numeric(18,2),

    persona_remitente_id bigint REFERENCES persona(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    persona_beneficiario_id bigint REFERENCES persona(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    nombre_remitente varchar(180),
    nombre_beneficiario varchar(180),
    documento_remitente_enc bytea,
    documento_remitente_hash bytea,
    documento_beneficiario_enc bytea,
    documento_beneficiario_hash bytea,
    cuenta_origen_enc bytea,
    cuenta_origen_hash bytea,
    cuenta_destino_enc bytea,
    cuenta_destino_hash bytea,
    pais_origen_id bigint REFERENCES pais(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    pais_destino_id bigint REFERENCES pais(id) ON DELETE RESTRICT ON UPDATE CASCADE,

    end_to_end_id varchar(80),
    codigo_camara varchar(40),
    spi_reference varchar(80),
    spi_return_reference varchar(80),
    alias_emisor_tipo varchar(20),
    alias_emisor_hash bytea,
    alias_receptor_tipo varchar(20),
    alias_receptor_hash bytea,
    pisp_id varchar(80),
    subparticipante_id varchar(80),
    entidad_patrocinadora_id varchar(80),

    numero_comprobante varchar(80),
    requiere_declaracion_fondos boolean NOT NULL DEFAULT false,
    declaracion_fondos_ref varchar(120),
    depositante_tercero boolean NOT NULL DEFAULT false,
    documento_depositante_enc bytea,
    documento_depositante_hash bytea,
    terminal_id varchar(80),
    sucursal_codigo varchar(60),

    empe_operador_id bigint REFERENCES empe_operador(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    wallet_origen_hash bytea,
    wallet_destino_hash bytea,
    telefono_linea_hash bytea,
    agente_empe_id varchar(80),

    cheque_numero_hash bytea,
    banco_emisor_id bigint REFERENCES banco_emisor(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    cuenta_libradora_hash bytea,
    tipo_cheque varchar(30),
    estado_clearing varchar(30),
    cheque_truncado boolean NOT NULL DEFAULT false,

    procesadora_tarjeta_id bigint REFERENCES procesadora_tarjeta(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    tipo_tarjeta varchar(20),
    mcc varchar(4),
    comercio_id varchar(80),
    nombre_comercio varchar(180),
    pos_id varchar(80),
    canal_tarjeta varchar(30),
    pan_token_hash bytea,
    pan_last4 char(4),
    auth_code varchar(30),
    qr_standard varchar(30),
    qr_hub_reference varchar(80),
    qr_payload_hash bytea,
    nfc_token_reference varchar(120),

    remesadora_id varchar(80),
    pais_corredor_remesa char(2),
    remittance_payout_method varchar(40),
    trade_invoice_number varchar(80),
    trade_goods_description varchar(250),
    customs_declaration varchar(120),
    swift_bic_origen varchar(11),
    swift_bic_destino varchar(11),

    score_riesgo numeric(8,2),
    nivel_riesgo_id bigint REFERENCES nivel_riesgo(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    datos_especificos jsonb NOT NULL DEFAULT '{}'::jsonb,
    riesgo_paraguay_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    screening_result_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    reglas_disparadas_json jsonb NOT NULL DEFAULT '[]'::jsonb,

    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,

    PRIMARY KEY (id, fecha_transaccion),
    CONSTRAINT uk_transacciones_empresa_codigo_fecha UNIQUE (empresa_id, codigo, fecha_transaccion),
    CONSTRAINT uk_transacciones_uuid_fecha UNIQUE (transaction_uuid, fecha_transaccion),
    CONSTRAINT ck_transacciones_estado CHECK (estado IN ('PENDIENTE', 'PROCESADA', 'COMPLETADA', 'RECHAZADA', 'REVERSADA', 'OBSERVADA', 'CANCELADA')),
    CONSTRAINT ck_transacciones_estado_eval CHECK (estado_evaluacion IN ('PENDIENTE', 'EN_PROCESO', 'EVALUADA', 'APROBADA', 'RECHAZADA', 'REVISION_MANUAL', 'SOSPECHOSA'))
) PARTITION BY RANGE (fecha_transaccion);

COMMENT ON TABLE transacciones IS 'Tabla particionada de movimientos financieros Regula AML Paraguay. Usa PK compuesta por particionamiento, RLS por empresa y PII cifrada/hasheada.';
COMMENT ON COLUMN transacciones.empresa_id IS 'Tenant UUID obligatorio. La policy RLS compara contra app.current_empresa_id.';
COMMENT ON COLUMN transacciones.fecha_transaccion IS 'Fecha de negocio y clave de particion mensual RANGE.';
COMMENT ON COLUMN transacciones.documento_remitente_enc IS 'Documento remitente cifrado en aplicacion con AES-256-GCM; no indexar ni exponer en DTO.';
COMMENT ON COLUMN transacciones.documento_remitente_hash IS 'HMAC-SHA256 deterministico del documento remitente para busqueda exacta sin descifrar.';
COMMENT ON COLUMN transacciones.documento_beneficiario_enc IS 'Documento beneficiario cifrado en aplicacion con AES-256-GCM.';
COMMENT ON COLUMN transacciones.documento_beneficiario_hash IS 'HMAC-SHA256 deterministico del documento beneficiario para busqueda exacta.';
COMMENT ON COLUMN transacciones.cuenta_origen_enc IS 'Cuenta origen cifrada en aplicacion con AES-256-GCM.';
COMMENT ON COLUMN transacciones.cuenta_origen_hash IS 'HMAC-SHA256 deterministico de cuenta origen para busqueda exacta.';
COMMENT ON COLUMN transacciones.cuenta_destino_enc IS 'Cuenta destino cifrada en aplicacion con AES-256-GCM.';
COMMENT ON COLUMN transacciones.cuenta_destino_hash IS 'HMAC-SHA256 deterministico de cuenta destino para busqueda exacta.';
COMMENT ON COLUMN transacciones.pan_token_hash IS 'Token/hash de PAN. Nunca guardar PAN completo; conservar solo last4 para soporte.';
COMMENT ON COLUMN transacciones.datos_especificos IS 'Metadatos variables por instrumento Paraguay: SPI, QR, EMPE, tarjetas, cheques, remesas, FX y comex.';

-- -----------------------------------------------------------------------------
-- 5. Particiones mensuales
-- -----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_create_transacciones_month_partition(p_month date)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    v_start date := date_trunc('month', p_month)::date;
    v_end date := (date_trunc('month', p_month) + interval '1 month')::date;
    v_partition_name text := 'transacciones_' || to_char(date_trunc('month', p_month), 'YYYY_MM');
BEGIN
    EXECUTE format(
        'CREATE TABLE IF NOT EXISTS %I PARTITION OF transacciones FOR VALUES FROM (%L) TO (%L)',
        v_partition_name,
        v_start,
        v_end
    );
END;
$$;

DO $$
DECLARE
    v_month date := DATE '2026-01-01';
BEGIN
    WHILE v_month < DATE '2028-01-01' LOOP
        PERFORM fn_create_transacciones_month_partition(v_month);
        v_month := (v_month + interval '1 month')::date;
    END LOOP;
END;
$$;

CREATE TABLE IF NOT EXISTS transacciones_default PARTITION OF transacciones DEFAULT;

-- -----------------------------------------------------------------------------
-- 6. Tablas hijas con FK compuesta a transacciones
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS alertas_antifraude (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    transaccion_id bigint NOT NULL,
    fecha_transaccion timestamptz NOT NULL,
    codigo varchar(60) NOT NULL,
    severidad varchar(30) NOT NULL,
    score numeric(8,2) NOT NULL DEFAULT 0,
    estado varchar(30) NOT NULL DEFAULT 'NUEVA',
    descripcion text,
    motivo text,
    reglas_disparadas_json jsonb NOT NULL DEFAULT '[]'::jsonb,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_alerta_transaccion FOREIGN KEY (transaccion_id, fecha_transaccion)
        REFERENCES transacciones(id, fecha_transaccion) ON DELETE RESTRICT ON UPDATE CASCADE
);

ALTER TABLE alertas_antifraude ADD COLUMN IF NOT EXISTS score numeric(8,2) NOT NULL DEFAULT 0;
ALTER TABLE alertas_antifraude ADD COLUMN IF NOT EXISTS motivo text;
ALTER TABLE alertas_antifraude ADD COLUMN IF NOT EXISTS reglas_disparadas_json jsonb NOT NULL DEFAULT '[]'::jsonb;
CREATE UNIQUE INDEX IF NOT EXISTS ux_alertas_antifraude_codigo ON alertas_antifraude (codigo);

CREATE TABLE IF NOT EXISTS ejecucion_reglas (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    transaccion_id bigint NOT NULL,
    fecha_transaccion timestamptz NOT NULL,
    regla_codigo varchar(80) NOT NULL,
    cumplida boolean NOT NULL DEFAULT false,
    score_generado numeric(8,2) NOT NULL DEFAULT 0,
    detalle jsonb NOT NULL DEFAULT '{}'::jsonb,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_ejecucion_transaccion FOREIGN KEY (transaccion_id, fecha_transaccion)
        REFERENCES transacciones(id, fecha_transaccion) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ejecucion_reglas_tx_regla
ON ejecucion_reglas (empresa_id, transaccion_id, fecha_transaccion, regla_codigo);

CREATE TABLE IF NOT EXISTS evaluaciones_riesgo (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    transaccion_id bigint NOT NULL,
    fecha_transaccion timestamptz NOT NULL,
    score_total numeric(8,2) NOT NULL DEFAULT 0,
    nivel_riesgo_id bigint REFERENCES nivel_riesgo(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    resultado varchar(30) NOT NULL,
    detalle jsonb NOT NULL DEFAULT '{}'::jsonb,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_evaluacion_transaccion FOREIGN KEY (transaccion_id, fecha_transaccion)
        REFERENCES transacciones(id, fecha_transaccion) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_evaluaciones_riesgo_tx
ON evaluaciones_riesgo (empresa_id, transaccion_id, fecha_transaccion);

CREATE OR REPLACE FUNCTION fn_set_fecha_transaccion()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    v_fecha timestamptz;
    v_empresa uuid;
BEGIN
    SELECT t.fecha_transaccion, t.empresa_id
      INTO v_fecha, v_empresa
      FROM transacciones t
     WHERE t.id = NEW.transaccion_id
     ORDER BY t.fecha_transaccion DESC
     LIMIT 1;

    IF v_fecha IS NULL THEN
        RAISE EXCEPTION 'No existe transaccion_id=%', NEW.transaccion_id;
    END IF;

    IF NEW.fecha_transaccion IS NULL THEN
        NEW.fecha_transaccion := v_fecha;
    ELSIF NEW.fecha_transaccion <> v_fecha THEN
        RAISE EXCEPTION 'fecha_transaccion inconsistente para transaccion_id=%', NEW.transaccion_id;
    END IF;

    IF NEW.empresa_id IS NULL THEN
        NEW.empresa_id := v_empresa;
    ELSIF NEW.empresa_id <> v_empresa THEN
        RAISE EXCEPTION 'empresa_id inconsistente para transaccion_id=%', NEW.transaccion_id;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_alertas_set_fecha_transaccion ON alertas_antifraude;
DROP TRIGGER IF EXISTS trg_ejecucion_set_fecha_transaccion ON ejecucion_reglas;
DROP TRIGGER IF EXISTS trg_evaluacion_set_fecha_transaccion ON evaluaciones_riesgo;

CREATE TRIGGER trg_alertas_set_fecha_transaccion
BEFORE INSERT ON alertas_antifraude
FOR EACH ROW EXECUTE FUNCTION fn_set_fecha_transaccion();

CREATE TRIGGER trg_ejecucion_set_fecha_transaccion
BEFORE INSERT ON ejecucion_reglas
FOR EACH ROW EXECUTE FUNCTION fn_set_fecha_transaccion();

CREATE TRIGGER trg_evaluacion_set_fecha_transaccion
BEFORE INSERT ON evaluaciones_riesgo
FOR EACH ROW EXECUTE FUNCTION fn_set_fecha_transaccion();

-- -----------------------------------------------------------------------------
-- 7. Triggers de auditoria CRUD
-- -----------------------------------------------------------------------------

DROP TRIGGER IF EXISTS trg_audit_empresa ON empresa;
DROP TRIGGER IF EXISTS trg_audit_usuarios ON usuarios;
DROP TRIGGER IF EXISTS trg_audit_moneda ON moneda;
DROP TRIGGER IF EXISTS trg_audit_persona ON persona;
DROP TRIGGER IF EXISTS trg_audit_tipo_transaccion ON tipo_transaccion;
DROP TRIGGER IF EXISTS trg_audit_canal_transaccion ON canal_transaccion;
DROP TRIGGER IF EXISTS trg_audit_banco_emisor ON banco_emisor;
DROP TRIGGER IF EXISTS trg_audit_procesadora_tarjeta ON procesadora_tarjeta;
DROP TRIGGER IF EXISTS trg_audit_empe_operador ON empe_operador;
DROP TRIGGER IF EXISTS trg_audit_transacciones ON transacciones;
DROP TRIGGER IF EXISTS trg_audit_alertas_antifraude ON alertas_antifraude;
DROP TRIGGER IF EXISTS trg_audit_ejecucion_reglas ON ejecucion_reglas;
DROP TRIGGER IF EXISTS trg_audit_evaluaciones_riesgo ON evaluaciones_riesgo;

CREATE TRIGGER trg_audit_empresa BEFORE INSERT OR UPDATE ON empresa FOR EACH ROW EXECUTE FUNCTION fn_set_audit_fields();
CREATE TRIGGER trg_audit_usuarios BEFORE INSERT OR UPDATE ON usuarios FOR EACH ROW EXECUTE FUNCTION fn_set_audit_fields();
CREATE TRIGGER trg_audit_moneda BEFORE INSERT OR UPDATE ON moneda FOR EACH ROW EXECUTE FUNCTION fn_set_audit_fields();
CREATE TRIGGER trg_audit_persona BEFORE INSERT OR UPDATE ON persona FOR EACH ROW EXECUTE FUNCTION fn_set_audit_fields();
CREATE TRIGGER trg_audit_tipo_transaccion BEFORE INSERT OR UPDATE ON tipo_transaccion FOR EACH ROW EXECUTE FUNCTION fn_set_audit_fields();
CREATE TRIGGER trg_audit_canal_transaccion BEFORE INSERT OR UPDATE ON canal_transaccion FOR EACH ROW EXECUTE FUNCTION fn_set_audit_fields();
CREATE TRIGGER trg_audit_banco_emisor BEFORE INSERT OR UPDATE ON banco_emisor FOR EACH ROW EXECUTE FUNCTION fn_set_audit_fields();
CREATE TRIGGER trg_audit_procesadora_tarjeta BEFORE INSERT OR UPDATE ON procesadora_tarjeta FOR EACH ROW EXECUTE FUNCTION fn_set_audit_fields();
CREATE TRIGGER trg_audit_empe_operador BEFORE INSERT OR UPDATE ON empe_operador FOR EACH ROW EXECUTE FUNCTION fn_set_audit_fields();
CREATE TRIGGER trg_audit_transacciones BEFORE INSERT OR UPDATE ON transacciones FOR EACH ROW EXECUTE FUNCTION fn_set_audit_fields();
CREATE TRIGGER trg_audit_alertas_antifraude BEFORE INSERT OR UPDATE ON alertas_antifraude FOR EACH ROW EXECUTE FUNCTION fn_set_audit_fields();
CREATE TRIGGER trg_audit_ejecucion_reglas BEFORE INSERT OR UPDATE ON ejecucion_reglas FOR EACH ROW EXECUTE FUNCTION fn_set_audit_fields();
CREATE TRIGGER trg_audit_evaluaciones_riesgo BEFORE INSERT OR UPDATE ON evaluaciones_riesgo FOR EACH ROW EXECUTE FUNCTION fn_set_audit_fields();

-- -----------------------------------------------------------------------------
-- 8. RLS multi-tenant
-- -----------------------------------------------------------------------------

ALTER TABLE persona ENABLE ROW LEVEL SECURITY;
ALTER TABLE transacciones ENABLE ROW LEVEL SECURITY;
ALTER TABLE alertas_antifraude ENABLE ROW LEVEL SECURITY;
ALTER TABLE ejecucion_reglas ENABLE ROW LEVEL SECURITY;
ALTER TABLE evaluaciones_riesgo ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_persona ON persona;
DROP POLICY IF EXISTS tenant_isolation_transacciones ON transacciones;
DROP POLICY IF EXISTS tenant_isolation_alertas_antifraude ON alertas_antifraude;
DROP POLICY IF EXISTS tenant_isolation_ejecucion_reglas ON ejecucion_reglas;
DROP POLICY IF EXISTS tenant_isolation_evaluaciones_riesgo ON evaluaciones_riesgo;

CREATE POLICY tenant_isolation_persona ON persona
    USING (empresa_id = current_setting('app.current_empresa_id', true)::uuid)
    WITH CHECK (empresa_id = current_setting('app.current_empresa_id', true)::uuid);

CREATE POLICY tenant_isolation_transacciones ON transacciones
    USING (empresa_id = current_setting('app.current_empresa_id', true)::uuid)
    WITH CHECK (empresa_id = current_setting('app.current_empresa_id', true)::uuid);

CREATE POLICY tenant_isolation_alertas_antifraude ON alertas_antifraude
    USING (empresa_id = current_setting('app.current_empresa_id', true)::uuid)
    WITH CHECK (empresa_id = current_setting('app.current_empresa_id', true)::uuid);

CREATE POLICY tenant_isolation_ejecucion_reglas ON ejecucion_reglas
    USING (empresa_id = current_setting('app.current_empresa_id', true)::uuid)
    WITH CHECK (empresa_id = current_setting('app.current_empresa_id', true)::uuid);

CREATE POLICY tenant_isolation_evaluaciones_riesgo ON evaluaciones_riesgo
    USING (empresa_id = current_setting('app.current_empresa_id', true)::uuid)
    WITH CHECK (empresa_id = current_setting('app.current_empresa_id', true)::uuid);

-- -----------------------------------------------------------------------------
-- 9. Indices antifraude/AML
-- -----------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_transacciones_empresa_fecha ON transacciones (empresa_id, fecha_transaccion DESC);
CREATE INDEX IF NOT EXISTS idx_transacciones_empresa_tipo_fecha ON transacciones (empresa_id, tipo_transaccion_id, fecha_transaccion DESC);
CREATE INDEX IF NOT EXISTS idx_transacciones_cuenta_origen_hash_fecha ON transacciones (empresa_id, cuenta_origen_hash, fecha_transaccion DESC);
CREATE INDEX IF NOT EXISTS idx_transacciones_cuenta_destino_hash_fecha ON transacciones (empresa_id, cuenta_destino_hash, fecha_transaccion DESC);
CREATE INDEX IF NOT EXISTS idx_transacciones_doc_remitente_hash_fecha ON transacciones (empresa_id, documento_remitente_hash, fecha_transaccion DESC);
CREATE INDEX IF NOT EXISTS idx_transacciones_doc_benef_hash_fecha ON transacciones (empresa_id, documento_beneficiario_hash, fecha_transaccion DESC);
CREATE INDEX IF NOT EXISTS idx_transacciones_spi_reference ON transacciones (empresa_id, spi_reference);
CREATE INDEX IF NOT EXISTS idx_transacciones_end_to_end ON transacciones (empresa_id, end_to_end_id);
CREATE INDEX IF NOT EXISTS idx_transacciones_qr_hash ON transacciones (empresa_id, qr_payload_hash);
CREATE INDEX IF NOT EXISTS idx_transacciones_datos_gin ON transacciones USING gin (datos_especificos);
CREATE INDEX IF NOT EXISTS idx_transacciones_riesgo_py_gin ON transacciones USING gin (riesgo_paraguay_json);
CREATE INDEX IF NOT EXISTS idx_transacciones_screening_gin ON transacciones USING gin (screening_result_json);
CREATE INDEX IF NOT EXISTS idx_transacciones_reglas_gin ON transacciones USING gin (reglas_disparadas_json);

CREATE INDEX IF NOT EXISTS idx_alertas_empresa_fecha ON alertas_antifraude (empresa_id, fecha_hora_creacion DESC);
CREATE INDEX IF NOT EXISTS idx_ejecucion_empresa_tx ON ejecucion_reglas (empresa_id, transaccion_id, fecha_transaccion);
CREATE INDEX IF NOT EXISTS idx_evaluaciones_empresa_tx ON evaluaciones_riesgo (empresa_id, transaccion_id, fecha_transaccion);

-- -----------------------------------------------------------------------------
-- 10. Verificacion operativa para particion default
-- -----------------------------------------------------------------------------

CREATE OR REPLACE VIEW vw_transacciones_default_control AS
SELECT count(*) AS total_en_default
FROM transacciones_default;
