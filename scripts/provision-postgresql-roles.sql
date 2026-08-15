\set ON_ERROR_STOP on

-- Bootstrap de cluster. Ejecutar una sola vez como superusuario sobre la base postgres.
-- Recibe db_name, owner_password, app_password y readonly_password mediante variables psql.
SELECT format('CREATE ROLE regula_owner LOGIN PASSWORD %L', :'owner_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'regula_owner') \gexec

SELECT 'CREATE ROLE regula_app NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS'
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'regula_app') \gexec

SELECT 'CREATE ROLE regula_readonly NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS'
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'regula_readonly') \gexec

-- Rol técnico usado únicamente por tareas batch explícitas que deben atravesar RLS.
-- Su membresía no se concede al backend ni al usuario de soporte.
SELECT 'CREATE ROLE regula_batch NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT BYPASSRLS'
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'regula_batch') \gexec

SELECT format('CREATE ROLE regula_app_login LOGIN PASSWORD %L', :'app_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'regula_app_login') \gexec

SELECT format('CREATE ROLE regula_readonly_login LOGIN PASSWORD %L', :'readonly_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'regula_readonly_login') \gexec

-- Permite rotar credenciales volviendo a ejecutar el bootstrap sin recrear datos.
SELECT format('ALTER ROLE regula_owner PASSWORD %L', :'owner_password') \gexec
SELECT format('ALTER ROLE regula_app_login PASSWORD %L', :'app_password') \gexec
SELECT format('ALTER ROLE regula_readonly_login PASSWORD %L', :'readonly_password') \gexec

GRANT regula_app TO regula_app_login;
GRANT regula_readonly TO regula_readonly_login;
ALTER ROLE regula_app_login NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
ALTER ROLE regula_readonly_login NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
ALTER ROLE regula_owner NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
ALTER ROLE regula_batch NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION BYPASSRLS;

SELECT format('CREATE DATABASE %I OWNER regula_owner', :'db_name')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'db_name') \gexec
SELECT format('ALTER DATABASE %I OWNER TO regula_owner', :'db_name') \gexec
SELECT format('REVOKE CONNECT ON DATABASE %I FROM PUBLIC', :'db_name') \gexec
SELECT format('GRANT CONNECT ON DATABASE %I TO regula_owner, regula_app_login, regula_readonly_login', :'db_name') \gexec
