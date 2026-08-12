-- ============================================================================
-- V11: Limites por plan (reglas / historial transaccional / escenarios-perfiles)
--      y formato soportado del reporte ROS (CSV baseline / JSON / XML Premium).
-- Base de decisiones: MDS/PLAN_ACCION_MODELO_NEGOCIO.md Fase 3 (3.1, 3.4, 3.5, 3.6).
-- Idempotente sobre la base poblada y el esquema clean.
-- ============================================================================

-- Limites del plan. NULL = sin limite / fair-use.
ALTER TABLE plan_licencia
    ADD COLUMN IF NOT EXISTS limite_reglas integer,
    ADD COLUMN IF NOT EXISTS limite_historial_transaccional integer,
    ADD COLUMN IF NOT EXISTS limite_escenarios integer;

-- Formato canonico del reporte ROS. Valor por defecto CSV (incluido en todos los planes).
ALTER TABLE reportes_ros
    ADD COLUMN IF NOT EXISTS formato varchar(20) NOT NULL DEFAULT 'CSV';

-- Semilla de limites por plan: Basico incluye 5 reglas / 5 tx historial / 5 escenarios;
-- Estandar 15 / 50 / 15; Premium fair-use 50 / 50 / 50 (vease modulos ROS_JSON_XML y
-- BENEFICIARIO_FINAL solo en Premium).
UPDATE plan_licencia AS p
SET limite_reglas = v.reglas,
    limite_historial_transaccional = v.historial,
    limite_escenarios = v.escenarios
FROM (VALUES
    ('BASICO',   5,  5,  5),
    ('ESTANDAR', 15, 50, 15),
    ('PREMIUM',  50, 50, 50)
) AS v(codigo, reglas, historial, escenarios)
WHERE p.codigo = v.codigo;