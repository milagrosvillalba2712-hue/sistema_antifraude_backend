CREATE TABLE instalacion_local (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id uuid NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    identificador_instalacion varchar(120) NOT NULL UNIQUE,
    fingerprint_hash varchar(128) NOT NULL,
    clave_publica_pem text NOT NULL,
    estado varchar(30) NOT NULL DEFAULT 'PENDIENTE',
    version_producto varchar(40),
    activada_en timestamptz,
    ultimo_heartbeat_en timestamptz,
    clon_detectado boolean NOT NULL DEFAULT false,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    CONSTRAINT ck_instalacion_local_estado CHECK (estado IN ('PENDIENTE','ACTIVA','SUSPENDIDA','REVOCADA'))
);

CREATE TABLE licencia_local (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    instalacion_id uuid NOT NULL REFERENCES instalacion_local(id) ON DELETE CASCADE ON UPDATE CASCADE,
    suscripcion_referencia varchar(120) NOT NULL,
    plan_codigo varchar(40) NOT NULL,
    plan_version integer NOT NULL,
    estado varchar(30) NOT NULL,
    emitida_en timestamptz NOT NULL,
    vence_en timestamptz NOT NULL,
    dias_gracia integer NOT NULL,
    modulos_json jsonb NOT NULL DEFAULT '[]'::jsonb,
    limites_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    lease_payload text NOT NULL,
    lease_firma text NOT NULL,
    kid_firma varchar(120) NOT NULL,
    ultima_validacion_en timestamptz,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    CONSTRAINT ck_licencia_local_estado CHECK (estado IN ('ACTIVA','GRACIA','VENCIDA','SUSPENDIDA','REVOCADA')),
    CONSTRAINT ck_licencia_local_fechas CHECK (vence_en > emitida_en),
    CONSTRAINT ck_licencia_local_gracia CHECK (dias_gracia BETWEEN 0 AND 90)
);

CREATE INDEX idx_licencia_local_instalacion_estado
    ON licencia_local (instalacion_id, estado, vence_en DESC);

CREATE TABLE consumo_licencia_local (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    instalacion_id uuid NOT NULL REFERENCES instalacion_local(id) ON DELETE CASCADE ON UPDATE CASCADE,
    anio integer NOT NULL,
    mes integer NOT NULL,
    usuarios_activos integer NOT NULL DEFAULT 0,
    transacciones_procesadas bigint NOT NULL DEFAULT 0,
    consultas_kyc bigint NOT NULL DEFAULT 0,
    alertas_generadas bigint NOT NULL DEFAULT 0,
    reportes_generados bigint NOT NULL DEFAULT 0,
    fecha_hora_modificacion timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_consumo_licencia_local_periodo UNIQUE (instalacion_id, anio, mes),
    CONSTRAINT ck_consumo_licencia_local_mes CHECK (mes BETWEEN 1 AND 12),
    CONSTRAINT ck_consumo_licencia_local_no_negativo CHECK (
        usuarios_activos >= 0 AND transacciones_procesadas >= 0 AND consultas_kyc >= 0
        AND alertas_generadas >= 0 AND reportes_generados >= 0
    )
);

CREATE TABLE evento_licencia_local (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    instalacion_id uuid NOT NULL REFERENCES instalacion_local(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    licencia_id uuid REFERENCES licencia_local(id) ON DELETE SET NULL ON UPDATE CASCADE,
    tipo_evento varchar(50) NOT NULL,
    resultado varchar(30) NOT NULL,
    correlation_id varchar(120),
    detalle_sanitizado_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    fecha_evento timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_evento_licencia_local_instalacion_fecha
    ON evento_licencia_local (instalacion_id, fecha_evento DESC);

COMMENT ON TABLE instalacion_local IS 'Identidad criptografica local de esta instalacion on-premise; no contiene transacciones.';
COMMENT ON TABLE licencia_local IS 'Cache local del lease firmado emitido por el futuro control plane.';
COMMENT ON TABLE consumo_licencia_local IS 'Contadores mensuales agregados; nunca almacena documentos ni detalle transaccional.';
COMMENT ON TABLE evento_licencia_local IS 'Auditoria sanitizada de activacion, renovacion, validacion y revocacion.';

-- Roles de grupo. Los usuarios LOGIN y sus secretos se crean en el instalador,
-- nunca dentro de una migracion ni en el repositorio.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'regula_app') THEN
        CREATE ROLE regula_app NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'regula_readonly') THEN
        CREATE ROLE regula_readonly NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS;
    END IF;
END $$;

REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO regula_app, regula_readonly;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO regula_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO regula_app;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO regula_readonly;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO regula_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO regula_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT ON TABLES TO regula_readonly;
