\set ON_ERROR_STOP on

-- Ejecutar como superusuario de PostgreSQL. Los valores se reciben con:
-- psql --set=owner_password=... --set=app_password=... --set=readonly_password=... -f scripts/provision-postgresql-roles.sql
SELECT format('CREATE ROLE regula_owner LOGIN PASSWORD %L', :'owner_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'regula_owner') \gexec

SELECT format('CREATE ROLE regula_app_login LOGIN PASSWORD %L', :'app_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'regula_app_login') \gexec

SELECT format('CREATE ROLE regula_readonly_login LOGIN PASSWORD %L', :'readonly_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'regula_readonly_login') \gexec

GRANT regula_app TO regula_app_login;
GRANT regula_readonly TO regula_readonly_login;
ALTER ROLE regula_app_login NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;
ALTER ROLE regula_readonly_login NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;

