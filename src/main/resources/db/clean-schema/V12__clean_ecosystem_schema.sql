-- ============================================================================
-- Regula AML - Clean schema extension V12
-- Ecosistema funcional completo sobre regula_clean.
-- La base legacy antifraude queda como referencia temporal, no operativa.
-- ============================================================================

SET client_min_messages TO warning;

-- ----------------------------------------------------------------------------
-- 1. Ajustes incrementales sobre tablas core existentes
-- ----------------------------------------------------------------------------

ALTER TABLE alertas_antifraude
    ADD COLUMN IF NOT EXISTS analista_asignado_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD COLUMN IF NOT EXISTS fecha_asignacion timestamptz,
    ADD COLUMN IF NOT EXISTS fecha_cierre timestamptz,
    ADD COLUMN IF NOT EXISTS resultado varchar(40),
    ADD COLUMN IF NOT EXISTS requiere_aprobacion_supervisor boolean NOT NULL DEFAULT true;

COMMENT ON COLUMN alertas_antifraude.analista_asignado_id IS
    'Analista responsable de investigar la alerta. Solo este usuario puede proponer resolucion, salvo permisos superiores.';

-- ----------------------------------------------------------------------------
-- 2. SaaS, licenciamiento y consumo
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS plan_licencia (
    id bigserial PRIMARY KEY,
    codigo varchar(40) NOT NULL UNIQUE,
    nombre varchar(120) NOT NULL,
    descripcion text,
    precio_anual numeric(18,2) NOT NULL DEFAULT 0,
    moneda_id bigint REFERENCES moneda(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    limite_usuarios integer NOT NULL,
    limite_transacciones_mes integer NOT NULL,
    limite_consultas_kyc_mes integer NOT NULL,
    modulos_json jsonb NOT NULL DEFAULT '[]'::jsonb,
    activo boolean NOT NULL DEFAULT true,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS suscripcion (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    plan_licencia_id bigint NOT NULL REFERENCES plan_licencia(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    codigo varchar(60) NOT NULL UNIQUE,
    estado varchar(30) NOT NULL,
    fecha_inicio date NOT NULL,
    fecha_fin date NOT NULL,
    renovacion_automatica boolean NOT NULL DEFAULT false,
    observacion text,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_suscripcion_fechas CHECK (fecha_fin >= fecha_inicio)
);

CREATE TABLE IF NOT EXISTS contrato (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    suscripcion_id bigint NOT NULL REFERENCES suscripcion(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    numero_contrato varchar(80) NOT NULL UNIQUE,
    tipo_contrato varchar(40) NOT NULL,
    estado varchar(30) NOT NULL,
    fecha_firma date,
    fecha_vigencia_desde date NOT NULL,
    fecha_vigencia_hasta date NOT NULL,
    documento_referencia varchar(220),
    hash_documento bytea,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS pago (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    suscripcion_id bigint NOT NULL REFERENCES suscripcion(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    codigo varchar(80) NOT NULL UNIQUE,
    fecha_pago timestamptz NOT NULL,
    monto numeric(18,2) NOT NULL,
    moneda_id bigint REFERENCES moneda(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    estado varchar(30) NOT NULL,
    metodo_pago varchar(60),
    comprobante_referencia varchar(120),
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS uso_suscripcion (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    suscripcion_id bigint NOT NULL REFERENCES suscripcion(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    periodo date NOT NULL,
    usuarios_activos integer NOT NULL DEFAULT 0,
    transacciones_procesadas integer NOT NULL DEFAULT 0,
    consultas_kyc integer NOT NULL DEFAULT 0,
    alertas_generadas integer NOT NULL DEFAULT 0,
    reportes_generados integer NOT NULL DEFAULT 0,
    consumo_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE (empresa_id, suscripcion_id, periodo)
);

-- ----------------------------------------------------------------------------
-- 3. RBAC completo, perfiles y disponibilidad
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS rol (
    id bigserial PRIMARY KEY,
    codigo varchar(50) NOT NULL UNIQUE,
    nombre varchar(120) NOT NULL,
    descripcion text,
    alcance varchar(30) NOT NULL,
    activo boolean NOT NULL DEFAULT true,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS permiso (
    id bigserial PRIMARY KEY,
    codigo varchar(80) NOT NULL UNIQUE,
    nombre varchar(140) NOT NULL,
    descripcion text,
    modulo varchar(60) NOT NULL,
    accion varchar(40) NOT NULL,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS rol_permiso (
    id bigserial PRIMARY KEY,
    rol_id bigint NOT NULL REFERENCES rol(id) ON DELETE CASCADE ON UPDATE CASCADE,
    permiso_id bigint NOT NULL REFERENCES permiso(id) ON DELETE CASCADE ON UPDATE CASCADE,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE (rol_id, permiso_id)
);

CREATE TABLE IF NOT EXISTS usuario_empresa (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_id uuid NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    rol_id bigint NOT NULL REFERENCES rol(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    estado varchar(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_alta date NOT NULL DEFAULT CURRENT_DATE,
    fecha_baja date,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE (empresa_id, usuario_id, rol_id)
);

CREATE TABLE IF NOT EXISTS perfil_usuario (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_id uuid NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    cargo varchar(120),
    area varchar(120),
    telefono varchar(40),
    zona_horaria varchar(60) NOT NULL DEFAULT 'America/Asuncion',
    preferencias_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE (empresa_id, usuario_id)
);

CREATE TABLE IF NOT EXISTS disponibilidad_usuario (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_id uuid NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    estado varchar(30) NOT NULL DEFAULT 'DISPONIBLE',
    carga_actual integer NOT NULL DEFAULT 0,
    capacidad_maxima integer NOT NULL DEFAULT 20,
    ultima_actualizacion timestamptz NOT NULL DEFAULT now(),
    motivo_no_disponible text,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE (empresa_id, usuario_id)
);

CREATE TABLE IF NOT EXISTS horario_laboral_usuario (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_id uuid NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    dia_semana smallint NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),
    hora_inicio time NOT NULL,
    hora_fin time NOT NULL,
    activo boolean NOT NULL DEFAULT true,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- ----------------------------------------------------------------------------
-- 4. KYC, documentos y perfiles
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS tipo_documento (
    id bigserial PRIMARY KEY,
    codigo varchar(40) NOT NULL UNIQUE,
    nombre varchar(140) NOT NULL,
    descripcion text,
    pais_relacion_id bigint REFERENCES pais(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    tipo_persona varchar(30) NOT NULL DEFAULT 'FISICA',
    fuente_oficial varchar(220),
    activo boolean NOT NULL DEFAULT true,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS documento (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    persona_id bigint NOT NULL REFERENCES persona(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    tipo_documento_id bigint NOT NULL REFERENCES tipo_documento(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    pais_emisor_id bigint REFERENCES pais(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    numero_documento_enc bytea,
    numero_documento_hash bytea NOT NULL,
    fecha_emision date,
    fecha_expiracion date,
    es_principal boolean NOT NULL DEFAULT false,
    estado varchar(30) NOT NULL DEFAULT 'VIGENTE',
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE (empresa_id, numero_documento_hash)
);

CREATE TABLE IF NOT EXISTS perfil_cliente (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    persona_id bigint NOT NULL REFERENCES persona(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    nivel_riesgo_id bigint REFERENCES nivel_riesgo(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    segmento varchar(80),
    actividad_economica varchar(160),
    ingreso_mensual_estimado numeric(18,2),
    volumen_mensual_esperado numeric(18,2),
    cantidad_operaciones_mensual integer,
    perfil_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE (empresa_id, persona_id)
);

CREATE TABLE IF NOT EXISTS cliente_pep (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    persona_id bigint NOT NULL REFERENCES persona(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    tipo_pep varchar(40) NOT NULL,
    cargo varchar(180),
    institucion varchar(180),
    pais_id bigint REFERENCES pais(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    fecha_inicio date,
    fecha_fin date,
    estado varchar(30) NOT NULL DEFAULT 'ACTIVO',
    detalle_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS cliente_observado (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    persona_id bigint NOT NULL REFERENCES persona(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    motivo varchar(180) NOT NULL,
    severidad varchar(30) NOT NULL,
    estado varchar(30) NOT NULL DEFAULT 'ACTIVO',
    observacion text,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

COMMENT ON COLUMN documento.numero_documento_enc IS 'Documento cifrado en capa aplicacion con AES-256-GCM.';
COMMENT ON COLUMN documento.numero_documento_hash IS 'HMAC-SHA256 deterministico para busqueda exacta sin descifrar PII.';

-- ----------------------------------------------------------------------------
-- 5. Fuentes, listas regulatorias y sujetos de riesgo
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS fuente_datos_riesgo (
    id bigserial PRIMARY KEY,
    codigo varchar(60) NOT NULL UNIQUE,
    nombre varchar(180) NOT NULL,
    organismo varchar(180),
    url_oficial text,
    licencia_uso text,
    frecuencia_actualizacion varchar(80),
    activo boolean NOT NULL DEFAULT true,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS lista_regulatoria (
    id bigserial PRIMARY KEY,
    fuente_datos_riesgo_id bigint REFERENCES fuente_datos_riesgo(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    codigo varchar(80) NOT NULL UNIQUE,
    nombre varchar(180) NOT NULL,
    tipo_lista varchar(60) NOT NULL,
    alcance varchar(80),
    url_descarga text,
    licencia_uso text,
    activa boolean NOT NULL DEFAULT true,
    fecha_ultima_revision date,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS sujeto_riesgo (
    id bigserial PRIMARY KEY,
    lista_regulatoria_id bigint REFERENCES lista_regulatoria(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    codigo varchar(80) NOT NULL UNIQUE,
    tipo_sujeto varchar(40) NOT NULL,
    nombre_normalizado varchar(220) NOT NULL,
    pais_id bigint REFERENCES pais(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    fecha_nacimiento date,
    tipo_riesgo varchar(60) NOT NULL,
    severidad varchar(30) NOT NULL,
    estado varchar(30) NOT NULL DEFAULT 'ACTIVO',
    detalle_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS sujeto_riesgo_alias (
    id bigserial PRIMARY KEY,
    sujeto_riesgo_id bigint NOT NULL REFERENCES sujeto_riesgo(id) ON DELETE CASCADE ON UPDATE CASCADE,
    alias_normalizado varchar(220) NOT NULL,
    tipo_alias varchar(40) NOT NULL DEFAULT 'ALIAS',
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS sujeto_riesgo_documento (
    id bigserial PRIMARY KEY,
    sujeto_riesgo_id bigint NOT NULL REFERENCES sujeto_riesgo(id) ON DELETE CASCADE ON UPDATE CASCADE,
    tipo_documento_id bigint REFERENCES tipo_documento(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    pais_emisor_id bigint REFERENCES pais(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    numero_documento_hash bytea NOT NULL,
    documento_enmascarado varchar(80),
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS sujeto_riesgo_relacion (
    id bigserial PRIMARY KEY,
    sujeto_origen_id bigint NOT NULL REFERENCES sujeto_riesgo(id) ON DELETE CASCADE ON UPDATE CASCADE,
    sujeto_destino_id bigint NOT NULL REFERENCES sujeto_riesgo(id) ON DELETE CASCADE ON UPDATE CASCADE,
    tipo_relacion varchar(60) NOT NULL,
    descripcion text,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CHECK (sujeto_origen_id <> sujeto_destino_id)
);

CREATE TABLE IF NOT EXISTS pais_riesgo (
    id bigserial PRIMARY KEY,
    pais_id bigint NOT NULL REFERENCES pais(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    fuente_datos_riesgo_id bigint REFERENCES fuente_datos_riesgo(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    categoria varchar(80) NOT NULL,
    severidad varchar(30) NOT NULL,
    motivo text,
    fecha_inicio date,
    fecha_fin date,
    activo boolean NOT NULL DEFAULT true,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- ----------------------------------------------------------------------------
-- 6. Motor de reglas y controles
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS escenario (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    codigo varchar(60) NOT NULL,
    nombre varchar(160) NOT NULL,
    descripcion text,
    severidad_base varchar(30) NOT NULL DEFAULT 'MEDIA',
    activo boolean NOT NULL DEFAULT true,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE (empresa_id, codigo)
);

CREATE TABLE IF NOT EXISTS accion (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    codigo varchar(60) NOT NULL,
    nombre varchar(160) NOT NULL,
    descripcion text,
    tipo_accion varchar(60) NOT NULL,
    requiere_supervisor boolean NOT NULL DEFAULT false,
    activa boolean NOT NULL DEFAULT true,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE (empresa_id, codigo)
);

CREATE TABLE IF NOT EXISTS reglas_riesgo (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    escenario_id bigint REFERENCES escenario(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    accion_id bigint REFERENCES accion(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    codigo varchar(80) NOT NULL,
    nombre varchar(180) NOT NULL,
    descripcion text,
    severidad varchar(30) NOT NULL,
    score_base numeric(8,2) NOT NULL DEFAULT 0,
    condiciones_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    acciones_json jsonb NOT NULL DEFAULT '[]'::jsonb,
    activa boolean NOT NULL DEFAULT true,
    estado varchar(30) NOT NULL DEFAULT 'ACTIVA',
    version integer NOT NULL DEFAULT 1,
    version_anterior_id bigint REFERENCES reglas_riesgo(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE (empresa_id, codigo, version)
);

CREATE TABLE IF NOT EXISTS control_importe (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    codigo varchar(60) NOT NULL,
    nombre varchar(160) NOT NULL,
    tipo_transaccion_id bigint REFERENCES tipo_transaccion(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    moneda_id bigint REFERENCES moneda(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    monto_minimo numeric(18,2),
    monto_maximo numeric(18,2),
    severidad varchar(30) NOT NULL DEFAULT 'MEDIA',
    activo boolean NOT NULL DEFAULT true,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE (empresa_id, codigo)
);

CREATE TABLE IF NOT EXISTS control_frecuencia (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    codigo varchar(60) NOT NULL,
    nombre varchar(160) NOT NULL,
    ventana_minutos integer NOT NULL,
    cantidad_maxima integer NOT NULL,
    monto_acumulado_maximo numeric(18,2),
    severidad varchar(30) NOT NULL DEFAULT 'MEDIA',
    activo boolean NOT NULL DEFAULT true,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE (empresa_id, codigo)
);

CREATE TABLE IF NOT EXISTS horario_riesgo (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    codigo varchar(60) NOT NULL,
    nombre varchar(160) NOT NULL,
    hora_inicio time NOT NULL,
    hora_fin time NOT NULL,
    dias_semana smallint[] NOT NULL DEFAULT ARRAY[1,2,3,4,5,6,7],
    severidad varchar(30) NOT NULL DEFAULT 'MEDIA',
    activo boolean NOT NULL DEFAULT true,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE (empresa_id, codigo)
);

CREATE TABLE IF NOT EXISTS calendario_riesgo (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    fecha date NOT NULL,
    nombre varchar(160) NOT NULL,
    tipo_evento varchar(60) NOT NULL,
    severidad varchar(30) NOT NULL DEFAULT 'MEDIA',
    activo boolean NOT NULL DEFAULT true,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE (empresa_id, fecha, tipo_evento)
);

-- ----------------------------------------------------------------------------
-- 7. Alertas, casos, evidencias, resoluciones y ROS
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS hallazgo_alerta (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    alerta_id bigint NOT NULL REFERENCES alertas_antifraude(id) ON DELETE CASCADE ON UPDATE CASCADE,
    transaccion_id bigint NOT NULL,
    fecha_transaccion timestamptz NOT NULL,
    regla_riesgo_id bigint REFERENCES reglas_riesgo(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    tipo_hallazgo varchar(60) NOT NULL,
    descripcion text NOT NULL,
    score numeric(8,2) NOT NULL DEFAULT 0,
    severidad varchar(30) NOT NULL,
    detalle_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (transaccion_id, fecha_transaccion) REFERENCES transacciones(id, fecha_transaccion) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS coincidencia_lista_alerta (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    alerta_id bigint NOT NULL REFERENCES alertas_antifraude(id) ON DELETE CASCADE ON UPDATE CASCADE,
    sujeto_riesgo_id bigint REFERENCES sujeto_riesgo(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    lista_regulatoria_id bigint REFERENCES lista_regulatoria(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    tipo_coincidencia varchar(60) NOT NULL,
    porcentaje_coincidencia numeric(5,2) NOT NULL,
    detalle_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS transaccion_detalle_snapshot (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    transaccion_id bigint NOT NULL,
    fecha_transaccion timestamptz NOT NULL,
    snapshot_json jsonb NOT NULL,
    fuente varchar(80) NOT NULL DEFAULT 'CORE_TRANSACCIONAL',
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (transaccion_id, fecha_transaccion) REFERENCES transacciones(id, fecha_transaccion) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS cliente_snapshot_alerta (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    alerta_id bigint NOT NULL REFERENCES alertas_antifraude(id) ON DELETE CASCADE ON UPDATE CASCADE,
    persona_id bigint REFERENCES persona(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    fuente varchar(80) NOT NULL DEFAULT 'API_EXTERNA_NO_DISPONIBLE',
    snapshot_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    fecha_consulta timestamptz NOT NULL DEFAULT now(),
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS consulta_kyc_alerta (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    alerta_id bigint NOT NULL REFERENCES alertas_antifraude(id) ON DELETE CASCADE ON UPDATE CASCADE,
    proveedor varchar(120) NOT NULL,
    estado varchar(40) NOT NULL,
    mensaje text,
    respuesta_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    fecha_consulta timestamptz NOT NULL DEFAULT now(),
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS historial_asignacion (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    alerta_id bigint NOT NULL REFERENCES alertas_antifraude(id) ON DELETE CASCADE ON UPDATE CASCADE,
    usuario_anterior_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_nuevo_id uuid NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    tipo varchar(40) NOT NULL,
    motivo text,
    observacion text,
    fecha_asignacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS estadistica_carga_analista (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_id uuid NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    periodo date NOT NULL,
    alertas_asignadas integer NOT NULL DEFAULT 0,
    alertas_cerradas integer NOT NULL DEFAULT 0,
    alertas_pendientes integer NOT NULL DEFAULT 0,
    tiempo_promedio_minutos numeric(10,2),
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE (empresa_id, usuario_id, periodo)
);

CREATE TABLE IF NOT EXISTS caso (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    codigo varchar(80) NOT NULL,
    titulo varchar(180) NOT NULL,
    descripcion text,
    estado varchar(40) NOT NULL,
    severidad varchar(30) NOT NULL,
    responsable_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    fecha_apertura timestamptz NOT NULL DEFAULT now(),
    fecha_cierre timestamptz,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE (empresa_id, codigo)
);

CREATE TABLE IF NOT EXISTS caso_alerta (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    caso_id bigint NOT NULL REFERENCES caso(id) ON DELETE CASCADE ON UPDATE CASCADE,
    alerta_id bigint NOT NULL REFERENCES alertas_antifraude(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE (empresa_id, caso_id, alerta_id)
);

CREATE TABLE IF NOT EXISTS actuacion (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    caso_id bigint NOT NULL REFERENCES caso(id) ON DELETE CASCADE ON UPDATE CASCADE,
    usuario_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    tipo_actuacion varchar(60) NOT NULL,
    descripcion text NOT NULL,
    fecha_actuacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS comentario_caso (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    caso_id bigint NOT NULL REFERENCES caso(id) ON DELETE CASCADE ON UPDATE CASCADE,
    usuario_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    comentario text NOT NULL,
    visibilidad varchar(30) NOT NULL DEFAULT 'INTERNA',
    fecha_comentario timestamptz NOT NULL DEFAULT now(),
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS evidencia (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    caso_id bigint REFERENCES caso(id) ON DELETE CASCADE ON UPDATE CASCADE,
    nombre varchar(180) NOT NULL,
    descripcion text,
    tipo_archivo varchar(40) NOT NULL,
    extension varchar(12) NOT NULL,
    mime_type varchar(120),
    tamanio_bytes bigint,
    estado varchar(30) NOT NULL DEFAULT 'CARGADA',
    hash_archivo bytea,
    referencia_archivo varchar(260),
    cargado_por_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    fecha_carga timestamptz NOT NULL DEFAULT now(),
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS evidencia_alerta (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    alerta_id bigint NOT NULL REFERENCES alertas_antifraude(id) ON DELETE CASCADE ON UPDATE CASCADE,
    evidencia_id bigint NOT NULL REFERENCES evidencia(id) ON DELETE CASCADE ON UPDATE CASCADE,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE (empresa_id, alerta_id, evidencia_id)
);

CREATE TABLE IF NOT EXISTS historial_estado_caso (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    caso_id bigint NOT NULL REFERENCES caso(id) ON DELETE CASCADE ON UPDATE CASCADE,
    estado_anterior varchar(40),
    estado_nuevo varchar(40) NOT NULL,
    motivo text,
    usuario_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    fecha_cambio timestamptz NOT NULL DEFAULT now(),
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS resolucion_alerta (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    alerta_id bigint NOT NULL REFERENCES alertas_antifraude(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    analista_id uuid NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    resultado varchar(50) NOT NULL,
    conclusion text NOT NULL,
    justificacion text,
    contacto_cliente text,
    fondos_retenidos boolean NOT NULL DEFAULT false,
    fondos_liberables boolean NOT NULL DEFAULT false,
    requiere_ros boolean NOT NULL DEFAULT false,
    requiere_bloqueo boolean NOT NULL DEFAULT false,
    requiere_escalamiento_legal boolean NOT NULL DEFAULT false,
    estado varchar(40) NOT NULL DEFAULT 'PENDIENTE_APROBACION',
    fecha_propuesta timestamptz NOT NULL DEFAULT now(),
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS aprobacion_supervisor (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    alerta_id bigint NOT NULL REFERENCES alertas_antifraude(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    resolucion_alerta_id bigint REFERENCES resolucion_alerta(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    supervisor_id uuid NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    decision varchar(40) NOT NULL,
    observacion text,
    motivo_rechazo text,
    faltantes text,
    fecha_decision timestamptz NOT NULL DEFAULT now(),
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS decision_caso (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    caso_id bigint NOT NULL REFERENCES caso(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    resolucion_alerta_id bigint REFERENCES resolucion_alerta(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    decision varchar(60) NOT NULL,
    descripcion text NOT NULL,
    ejecutada boolean NOT NULL DEFAULT false,
    fecha_decision timestamptz NOT NULL DEFAULT now(),
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS reportes_ros (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    caso_id bigint NOT NULL REFERENCES caso(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    codigo varchar(80) NOT NULL,
    estado varchar(40) NOT NULL,
    fecha_generacion timestamptz NOT NULL DEFAULT now(),
    descripcion_sospecha text NOT NULL,
    soporte_referencia varchar(260),
    reporte_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE (empresa_id, codigo)
);

-- ----------------------------------------------------------------------------
-- 8. Auditoria funcional y servicios externos
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS servicio_externo (
    id bigserial PRIMARY KEY,
    codigo varchar(80) NOT NULL UNIQUE,
    nombre varchar(180) NOT NULL,
    tipo_servicio varchar(60) NOT NULL,
    url_base text,
    estado varchar(30) NOT NULL DEFAULT 'NO_DISPONIBLE',
    configuracion_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS consultas_externas (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    servicio_externo_id bigint REFERENCES servicio_externo(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    persona_id bigint REFERENCES persona(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    alerta_id bigint REFERENCES alertas_antifraude(id) ON DELETE SET NULL ON UPDATE CASCADE,
    estado varchar(40) NOT NULL,
    request_hash bytea,
    respuesta_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    mensaje_error text,
    fecha_consulta timestamptz NOT NULL DEFAULT now(),
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS auditoria_sistema (
    id bigserial PRIMARY KEY,
    empresa_id uuid REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    accion varchar(80) NOT NULL,
    descripcion text,
    entidad_afectada varchar(100) NOT NULL,
    entidad_id varchar(100),
    valor_anterior_json jsonb,
    valor_nuevo_json jsonb,
    direccion_ip varchar(80),
    user_agent text,
    fecha_evento timestamptz NOT NULL DEFAULT now()
);

-- ----------------------------------------------------------------------------
-- 9. RLS, triggers e indices
-- ----------------------------------------------------------------------------

DO $$
DECLARE
    r record;
BEGIN
    FOR r IN
        SELECT unnest(ARRAY[
            'suscripcion','contrato','pago','uso_suscripcion','usuario_empresa','perfil_usuario',
            'disponibilidad_usuario','horario_laboral_usuario','documento','perfil_cliente',
            'cliente_pep','cliente_observado','escenario','accion','reglas_riesgo',
            'control_importe','control_frecuencia','horario_riesgo','calendario_riesgo',
            'hallazgo_alerta','coincidencia_lista_alerta','transaccion_detalle_snapshot',
            'cliente_snapshot_alerta','consulta_kyc_alerta','historial_asignacion',
            'estadistica_carga_analista','caso','caso_alerta','actuacion','comentario_caso',
            'evidencia','evidencia_alerta','historial_estado_caso','resolucion_alerta',
            'aprobacion_supervisor','decision_caso','reportes_ros','consultas_externas'
        ]) AS table_name
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', r.table_name);
        EXECUTE format('DROP POLICY IF EXISTS tenant_isolation_%I ON %I', r.table_name, r.table_name);
        EXECUTE format(
            'CREATE POLICY tenant_isolation_%I ON %I USING (empresa_id = current_setting(''app.current_empresa_id'', true)::uuid) WITH CHECK (empresa_id = current_setting(''app.current_empresa_id'', true)::uuid)',
            r.table_name,
            r.table_name
        );
    END LOOP;

    FOR r IN
        SELECT c.table_name
        FROM information_schema.columns c
        JOIN information_schema.tables t
          ON t.table_schema = c.table_schema
         AND t.table_name = c.table_name
        WHERE c.table_schema = 'public'
          AND c.column_name = 'fecha_hora_creacion'
          AND c.table_name NOT LIKE 'transacciones_%'
          AND t.table_type = 'BASE TABLE'
    LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS trg_audit_%I ON %I', r.table_name, r.table_name);
        EXECUTE format('CREATE TRIGGER trg_audit_%I BEFORE INSERT OR UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION fn_set_audit_fields()', r.table_name, r.table_name);
    END LOOP;
END $$;

CREATE INDEX IF NOT EXISTS ix_suscripcion_empresa_estado ON suscripcion (empresa_id, estado, fecha_fin);
CREATE INDEX IF NOT EXISTS ix_usuario_empresa_empresa_usuario ON usuario_empresa (empresa_id, usuario_id);
CREATE INDEX IF NOT EXISTS ix_documento_persona ON documento (empresa_id, persona_id);
CREATE INDEX IF NOT EXISTS ix_documento_hash ON documento (empresa_id, numero_documento_hash);
CREATE INDEX IF NOT EXISTS ix_reglas_empresa_estado ON reglas_riesgo (empresa_id, estado, activa);
CREATE INDEX IF NOT EXISTS ix_hallazgo_alerta ON hallazgo_alerta (empresa_id, alerta_id);
CREATE INDEX IF NOT EXISTS ix_historial_asignacion_alerta ON historial_asignacion (empresa_id, alerta_id, fecha_asignacion DESC);
CREATE INDEX IF NOT EXISTS ix_caso_empresa_estado ON caso (empresa_id, estado, fecha_apertura DESC);
CREATE INDEX IF NOT EXISTS ix_auditoria_sistema_entidad ON auditoria_sistema (empresa_id, entidad_afectada, fecha_evento DESC);
CREATE INDEX IF NOT EXISTS ix_sujeto_riesgo_nombre ON sujeto_riesgo (nombre_normalizado);
CREATE INDEX IF NOT EXISTS ix_sujeto_alias_nombre ON sujeto_riesgo_alias (alias_normalizado);
CREATE INDEX IF NOT EXISTS ix_perfil_cliente_json ON perfil_cliente USING gin (perfil_json);
CREATE INDEX IF NOT EXISTS ix_reglas_condiciones_json ON reglas_riesgo USING gin (condiciones_json);

COMMENT ON TABLE auditoria_sistema IS 'Auditoria funcional general para toda accion mutante relevante del ecosistema Regula AML.';
COMMENT ON TABLE historial_asignacion IS 'Bitacora operacional exclusiva para asignaciones y reasignaciones de alertas.';
