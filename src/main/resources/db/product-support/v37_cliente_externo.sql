-- ============================================================================
-- V37: cliente_externo + cliente_externo_auditoria.
-- API keys para integraciones M2M/B2B de entidades bancarias externas.
-- Las claves se almacenan SOLO como hash bcrypt (api_key_hash); la clave en
-- claro se retorna una unica vez al crear/rotar y nunca se puede recuperar.
-- ============================================================================

CREATE TABLE IF NOT EXISTS cliente_externo (
    id                          uuid PRIMARY KEY,
    codigo                      varchar(40) NOT NULL,
    nombre                      varchar(160) NOT NULL,
    entidad_financiera_id       uuid,
    api_key_hash                varchar(255) NOT NULL,
    api_key_prefix              varchar(16) NOT NULL,
    api_key_last4               varchar(4),
    empresa_id                  uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    scopes                      text[] NOT NULL DEFAULT '{}',
    rate_limit_per_minute       integer NOT NULL DEFAULT 600,
    ip_whitelist                cidr[],
    fecha_expiracion            timestamptz,
    fecha_ultimo_uso            timestamptz,
    activo                      boolean NOT NULL DEFAULT true,
    usuario_creacion_id         uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id     uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    fecha_hora_creacion         timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion     timestamptz,
    CONSTRAINT uk_cliente_externo_codigo UNIQUE (codigo)
);

CREATE TABLE IF NOT EXISTS cliente_externo_auditoria (
    id                      bigserial PRIMARY KEY,
    cliente_externo_id      uuid REFERENCES cliente_externo(id) ON DELETE SET NULL,
    endpoint                varchar(200) NOT NULL,
    metodo_http             varchar(10) NOT NULL,
    ip_origen               inet,
    status                  integer,
    error_code              varchar(60),
    request_id              varchar(60),
    duracion_ms             integer,
    fecha_hora_creacion     timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_cliente_externo_codigo
    ON cliente_externo (codigo);
CREATE INDEX IF NOT EXISTS ix_cliente_externo_empresa
    ON cliente_externo (empresa_id);
CREATE INDEX IF NOT EXISTS ix_cliente_externo_activo
    ON cliente_externo (activo) WHERE activo = true;
CREATE INDEX IF NOT EXISTS ix_cliente_externo_prefix
    ON cliente_externo (api_key_prefix);
CREATE INDEX IF NOT EXISTS ix_cliente_ext_aud_cliente_fecha
    ON cliente_externo_auditoria (cliente_externo_id, fecha_hora_creacion DESC);
CREATE INDEX IF NOT EXISTS ix_cliente_ext_aud_fecha
    ON cliente_externo_auditoria (fecha_hora_creacion DESC);

COMMENT ON TABLE cliente_externo IS 'Clientes externos (entidades bancarias) autorizados a consumir la API via X-API-Key. La clave se almacena solo como hash bcrypt.';
COMMENT ON TABLE cliente_externo_auditoria IS 'Bitacora de acceso a la API con clave de cliente externo (endpoint, ip, status, duracion).';
