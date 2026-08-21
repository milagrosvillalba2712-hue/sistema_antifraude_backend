-- V28: listas de control propias del cliente.
-- Separan decisiones internas de la entidad financiera de los catalogos globales
-- publicados por Regula/Control Plane.

CREATE TABLE IF NOT EXISTS lista_control_cliente (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    tipo_lista varchar(20) NOT NULL,
    codigo varchar(80) NOT NULL,
    nombre varchar(160) NOT NULL,
    descripcion text,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVA',
    prioridad integer NOT NULL DEFAULT 50,
    fecha_vigencia_desde date,
    fecha_vigencia_hasta date,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    CONSTRAINT ck_lista_control_cliente_tipo CHECK (tipo_lista IN ('WHITELIST', 'BLACKLIST')),
    CONSTRAINT ck_lista_control_cliente_estado CHECK (estado IN ('ACTIVA', 'INACTIVA', 'VENCIDA')),
    CONSTRAINT uk_lista_control_cliente_empresa_codigo UNIQUE (empresa_id, tipo_lista, codigo)
);

CREATE TABLE IF NOT EXISTS elemento_lista_control_cliente (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    lista_control_cliente_id bigint NOT NULL REFERENCES lista_control_cliente(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    tipo_entidad varchar(30) NOT NULL DEFAULT 'PERSONA',
    tipo_identificador varchar(40) NOT NULL DEFAULT 'NOMBRE',
    valor_original text NOT NULL,
    valor_normalizado text NOT NULL,
    valor_hash bytea,
    nombre_mostrado varchar(180),
    documento_mostrado varchar(80),
    motivo text,
    observacion text,
    fuente varchar(120) NOT NULL DEFAULT 'CLIENTE',
    severidad varchar(20) NOT NULL DEFAULT 'Media',
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_vigencia_desde date,
    fecha_vigencia_hasta date,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    CONSTRAINT ck_elemento_lista_control_tipo_entidad CHECK (tipo_entidad IN ('PERSONA', 'EMPRESA', 'CUENTA', 'DOCUMENTO', 'WALLET', 'ALIAS')),
    CONSTRAINT ck_elemento_lista_control_tipo_identificador CHECK (tipo_identificador IN ('NOMBRE', 'DOCUMENTO', 'CUENTA', 'WALLET', 'ALIAS')),
    CONSTRAINT ck_elemento_lista_control_estado CHECK (estado IN ('ACTIVO', 'INACTIVO', 'VENCIDO')),
    CONSTRAINT uk_elemento_lista_control_valor UNIQUE (empresa_id, lista_control_cliente_id, tipo_identificador, valor_normalizado)
);

CREATE TABLE IF NOT EXISTS importacion_lista_control_cliente (
    id bigserial PRIMARY KEY,
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    lista_control_cliente_id bigint REFERENCES lista_control_cliente(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    nombre_archivo varchar(220) NOT NULL,
    tipo_archivo varchar(20) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'RECIBIDA',
    total_registros integer NOT NULL DEFAULT 0,
    registros_validos integer NOT NULL DEFAULT 0,
    registros_invalidos integer NOT NULL DEFAULT 0,
    errores_json jsonb NOT NULL DEFAULT '[]'::jsonb,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    CONSTRAINT ck_importacion_lista_control_estado CHECK (estado IN ('RECIBIDA', 'PROCESADA', 'PROCESADA_CON_ERRORES', 'RECHAZADA'))
);

CREATE INDEX IF NOT EXISTS ix_lista_control_cliente_empresa_tipo
    ON lista_control_cliente (empresa_id, tipo_lista, estado);
CREATE INDEX IF NOT EXISTS ix_elemento_lista_control_lookup
    ON elemento_lista_control_cliente (empresa_id, tipo_identificador, valor_normalizado, estado);
CREATE INDEX IF NOT EXISTS ix_elemento_lista_control_hash
    ON elemento_lista_control_cliente (empresa_id, tipo_identificador, valor_hash)
    WHERE valor_hash IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_importacion_lista_control_empresa
    ON importacion_lista_control_cliente (empresa_id, fecha_hora_creacion DESC);

ALTER TABLE lista_control_cliente ENABLE ROW LEVEL SECURITY;
ALTER TABLE lista_control_cliente FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON lista_control_cliente;
CREATE POLICY tenant_isolation ON lista_control_cliente
    FOR ALL
    USING (empresa_id = NULLIF(current_setting('app.current_empresa_id', true), '')::uuid)
    WITH CHECK (empresa_id = NULLIF(current_setting('app.current_empresa_id', true), '')::uuid);

ALTER TABLE elemento_lista_control_cliente ENABLE ROW LEVEL SECURITY;
ALTER TABLE elemento_lista_control_cliente FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON elemento_lista_control_cliente;
CREATE POLICY tenant_isolation ON elemento_lista_control_cliente
    FOR ALL
    USING (empresa_id = NULLIF(current_setting('app.current_empresa_id', true), '')::uuid)
    WITH CHECK (empresa_id = NULLIF(current_setting('app.current_empresa_id', true), '')::uuid);

ALTER TABLE importacion_lista_control_cliente ENABLE ROW LEVEL SECURITY;
ALTER TABLE importacion_lista_control_cliente FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON importacion_lista_control_cliente;
CREATE POLICY tenant_isolation ON importacion_lista_control_cliente
    FOR ALL
    USING (empresa_id = NULLIF(current_setting('app.current_empresa_id', true), '')::uuid)
    WITH CHECK (empresa_id = NULLIF(current_setting('app.current_empresa_id', true), '')::uuid);

COMMENT ON TABLE lista_control_cliente IS 'Listas blancas y negras administradas por cada empresa cliente. No son catalogos maestros de Regula.';
COMMENT ON TABLE elemento_lista_control_cliente IS 'Elementos tenant-scoped usados por el motor de reglas para permitir o bloquear sujetos, documentos, cuentas, aliases o wallets.';
COMMENT ON TABLE importacion_lista_control_cliente IS 'Bitacora de importaciones masivas CSV/XLSX de listas de control del cliente.';
COMMENT ON COLUMN elemento_lista_control_cliente.valor_hash IS 'HMAC-SHA256 deterministico del valor original para busqueda exacta sin exponer PII en claro.';

