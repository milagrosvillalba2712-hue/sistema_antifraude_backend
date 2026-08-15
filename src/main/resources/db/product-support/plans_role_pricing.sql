-- ============================================================================
-- V10: Planes de producto Básico / Estándar / Premium + precios por rol adicional
-- Base de decisiones: MDS/PLAN_ACCION_MODELO_NEGOCIO.md Fase 0 (0.1, 0.3, 0.4)
-- y MDS/ANALISIS_BRECHAS_MODELO_NEGOCIO.md seccion 5.9 (2026-08-09).
-- Toca la base poblada: ver scripts/BK/20260809_pre_fase0_planes_rol (legacy).
-- ============================================================================

-- Guard: garantiza la moneda USD/PYG para los planes (el perfil productivo
-- arranca sin catalogos cargados). Idempotente sobre la base poblada.
INSERT INTO moneda (codigo_iso, nombre, nombre_en, fuente, activo)
SELECT v.codigo_iso, v.nombre, v.nombre_en, v.fuente, true
FROM (VALUES
  ('USD', 'Dolar estadounidense', 'US Dollar', 'ISO 4217'),
  ('PYG', 'Guarani paraguayo', 'Paraguayan Guarani', 'ISO 4217')
) v(codigo_iso, nombre, nombre_en, fuente)
ON CONFLICT (codigo_iso) DO UPDATE SET nombre = EXCLUDED.nombre, fuente = EXCLUDED.fuente;

-- Planes productivos. Nomenclatura unificada: BASICO / ESTANDAR / PREMIUM.
-- Premium usa fair-use (10M tx/mes, 500 usuarios) con SLA documentado.
INSERT INTO plan_licencia (
    codigo, nombre, descripcion, precio_anual, moneda_id,
    limite_usuarios,
    limite_transacciones_mes, limite_transacciones_mensuales,
    limite_consultas_kyc_mes, limite_consultas_kyc_mensuales,
    limite_reportes_mensuales,
    modulos_json, modulos_incluidos_json, activo)
SELECT v.codigo, v.nombre, v.descripcion, v.precio, m.id,
       v.usuarios, v.tx, v.tx, v.kyc, v.kyc, v.reportes,
       v.modulos::jsonb, v.modulos::jsonb, true
FROM (VALUES
  ('BASICO',   'Basico',
   'Plan de entrada: transacciones, alertas, KYC y screening SEPRELAD.',
   8000, 3, 100000, 5000, 500,
   '["TRANSACCIONES","ALERTAS","KYC","SEPRELAD"]'),
  ('ESTANDAR', 'Estandar',
   'Plan operativo: suma screening ONU/OFAC, PEP, riesgo pais y ROS.',
   12000, 6, 500000, 20000, 1000,
   '["TRANSACCIONES","ALERTAS","KYC","SEPRELAD","ONU_OFAC","PEP","RIESGO_PAIS","ROS"]'),
  ('PREMIUM',  'Premium',
   'Fair-use: hasta 10M tx/mes y 500 usuarios. Incluye beneficiario final y ROS JSON/XML.',
   18000, 500, 10000000, 200000, 5000,
   '["TRANSACCIONES","ALERTAS","KYC","SEPRELAD","ONU_OFAC","PEP","RIESGO_PAIS","ROS","BENEFICIARIO_FINAL","ROS_JSON_XML"]')
) v(codigo, nombre, descripcion, precio, usuarios, tx, kyc, reportes, modulos)
JOIN moneda m ON m.codigo_iso = 'USD'
ON CONFLICT (codigo) DO UPDATE SET
  nombre = EXCLUDED.nombre, descripcion = EXCLUDED.descripcion,
  precio_anual = EXCLUDED.precio_anual, moneda_id = EXCLUDED.moneda_id,
  limite_usuarios = EXCLUDED.limite_usuarios,
  limite_transacciones_mes = EXCLUDED.limite_transacciones_mes,
  limite_transacciones_mensuales = EXCLUDED.limite_transacciones_mensuales,
  limite_consultas_kyc_mes = EXCLUDED.limite_consultas_kyc_mes,
  limite_consultas_kyc_mensuales = EXCLUDED.limite_consultas_kyc_mensuales,
  limite_reportes_mensuales = EXCLUDED.limite_reportes_mensuales,
  modulos_json = EXCLUDED.modulos_json,
  modulos_incluidos_json = EXCLUDED.modulos_incluidos_json,
  activo = true;

-- Precios de usuario adicional por rol (Admin 600 / Gerente 550 / Analista 400 / Auditor 350).
CREATE TABLE IF NOT EXISTS plan_plan_precios_rol (
    id bigserial PRIMARY KEY,
    plan_licencia_id bigint NOT NULL REFERENCES plan_licencia(id) ON DELETE CASCADE ON UPDATE CASCADE,
    rol_id bigint NOT NULL REFERENCES rol(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    precio_anual numeric(18,2) NOT NULL DEFAULT 0,
    activo boolean NOT NULL DEFAULT true,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT uq_plan_rol UNIQUE (plan_licencia_id, rol_id)
);

INSERT INTO plan_plan_precios_rol (plan_licencia_id, rol_id, precio_anual, activo)
SELECT p.id, r.id, v.precio, true
FROM (VALUES
  ('BASICO',   'ADMIN_EMPRESA',      600),
  ('BASICO',   'GERENTE_SUPERVISOR', 550),
  ('BASICO',   'ANALISTA',           400),
  ('BASICO',   'AUDITOR',            350),
  ('ESTANDAR', 'ADMIN_EMPRESA',      600),
  ('ESTANDAR', 'GERENTE_SUPERVISOR', 550),
  ('ESTANDAR', 'ANALISTA',           400),
  ('ESTANDAR', 'AUDITOR',            350),
  ('PREMIUM',  'ADMIN_EMPRESA',      600),
  ('PREMIUM',  'GERENTE_SUPERVISOR', 550),
  ('PREMIUM',  'ANALISTA',           400),
  ('PREMIUM',  'AUDITOR',            350)
) v(plan, rol, precio)
JOIN plan_licencia p ON p.codigo = v.plan
JOIN rol r ON r.codigo = v.rol
ON CONFLICT (plan_licencia_id, rol_id) DO UPDATE SET precio_anual = EXCLUDED.precio_anual, activo = EXCLUDED.activo;