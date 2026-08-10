-- Fase 5 — Ciclo de vida del usuario (registro por invitacion, verificacion de email
-- y recuperacion de contrasena). Los tokens de una sola vez se guardan HASHEADOS
-- (SHA-256); nunca en claro. La columna contrasena_cambiada_en permite invalidar
-- sesiones JWT emitidas antes de un cambio de contrasena.

ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS email_verificado boolean NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS contrasena_cambiada_en timestamptz;

CREATE TABLE IF NOT EXISTS auth_token (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo varchar(20) NOT NULL,
    token_hash varchar(64) NOT NULL,
    usuario_id uuid,
    empresa_id uuid,
    rol_id bigint,
    email varchar(150),
    expira_en timestamptz NOT NULL,
    creado_en timestamptz NOT NULL DEFAULT now(),
    usado_en timestamptz,
    ip varchar(64),
    user_agent varchar(255),
    revocado boolean NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_auth_token_hash ON auth_token(token_hash);
CREATE INDEX IF NOT EXISTS ix_auth_token_usuario ON auth_token(usuario_id);
CREATE INDEX IF NOT EXISTS ix_auth_token_vencimiento ON auth_token(tipo, expira_en);