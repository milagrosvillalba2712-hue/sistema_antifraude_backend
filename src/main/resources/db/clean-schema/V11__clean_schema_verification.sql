-- =============================================================================
-- REGULA AML PARAGUAY - VERIFICACION OPERATIVA DE ESQUEMA LIMPIO
-- Ejecutar despues de V10__clean_core_schema_paraguay.sql.
-- =============================================================================

CREATE OR REPLACE VIEW vw_crud_audit_columns_control AS
WITH tablas_crud(tabla) AS (
    VALUES
        ('empresa'),
        ('usuarios'),
        ('moneda'),
        ('persona'),
        ('tipo_transaccion'),
        ('canal_transaccion'),
        ('banco_emisor'),
        ('procesadora_tarjeta'),
        ('empe_operador'),
        ('transacciones'),
        ('alertas_antifraude'),
        ('ejecucion_reglas'),
        ('evaluaciones_riesgo')
),
columnas_requeridas(columna) AS (
    VALUES
        ('fecha_hora_creacion'),
        ('fecha_hora_modificacion'),
        ('usuario_creacion_id'),
        ('usuario_modificacion_id')
)
SELECT t.tabla,
       c.columna,
       EXISTS (
           SELECT 1
             FROM information_schema.columns ic
            WHERE ic.table_schema = current_schema()
              AND ic.table_name = t.tabla
              AND ic.column_name = c.columna
       ) AS existe
  FROM tablas_crud t
 CROSS JOIN columnas_requeridas c;

CREATE OR REPLACE VIEW vw_rls_control AS
SELECT c.relname AS tabla,
       c.relrowsecurity AS rls_habilitado,
       count(p.polname) AS total_policies
  FROM pg_class c
  JOIN pg_namespace n ON n.oid = c.relnamespace
  LEFT JOIN pg_policy p ON p.polrelid = c.oid
 WHERE n.nspname = current_schema()
   AND c.relname IN ('persona', 'transacciones', 'alertas_antifraude', 'ejecucion_reglas', 'evaluaciones_riesgo')
 GROUP BY c.relname, c.relrowsecurity;

CREATE OR REPLACE VIEW vw_transacciones_particiones_control AS
SELECT inhrelid::regclass::text AS particion
  FROM pg_inherits
 WHERE inhparent = 'transacciones'::regclass
 ORDER BY particion;

CREATE OR REPLACE VIEW vw_fk_compuesta_transaccion_control AS
SELECT conname AS constraint_name,
       conrelid::regclass::text AS tabla_hija,
       confrelid::regclass::text AS tabla_padre,
       array_length(conkey, 1) AS columnas_fk
  FROM pg_constraint
 WHERE contype = 'f'
   AND confrelid = 'transacciones'::regclass
   AND conrelid::regclass::text IN ('alertas_antifraude', 'ejecucion_reglas', 'evaluaciones_riesgo');

CREATE OR REPLACE VIEW vw_pii_columns_control AS
SELECT table_name,
       column_name,
       data_type
  FROM information_schema.columns
 WHERE table_schema = current_schema()
   AND table_name = 'transacciones'
   AND column_name IN (
       'documento_remitente_enc',
       'documento_remitente_hash',
       'documento_beneficiario_enc',
       'documento_beneficiario_hash',
       'cuenta_origen_enc',
       'cuenta_origen_hash',
       'cuenta_destino_enc',
       'cuenta_destino_hash',
       'alias_emisor_hash',
       'alias_receptor_hash',
       'wallet_origen_hash',
       'wallet_destino_hash',
       'telefono_linea_hash',
       'cheque_numero_hash',
       'pan_token_hash',
       'qr_payload_hash'
   );

-- Consultas esperadas:
-- 1. Auditoria CRUD completa:
--    SELECT * FROM vw_crud_audit_columns_control WHERE existe = false;
--    Debe retornar 0 filas.
--
-- 2. RLS activo y con policies:
--    SELECT * FROM vw_rls_control WHERE rls_habilitado = false OR total_policies = 0;
--    Debe retornar 0 filas.
--
-- 3. Particiones mensuales:
--    SELECT * FROM vw_transacciones_particiones_control;
--    Debe incluir transacciones_2026_01 ... transacciones_2027_12 y transacciones_default.
--
-- 4. FK compuesta:
--    SELECT * FROM vw_fk_compuesta_transaccion_control WHERE columnas_fk <> 2;
--    Debe retornar 0 filas.
--
-- 5. PII protegida:
--    SELECT * FROM vw_pii_columns_control;
--    Debe retornar las columnas *_enc y *_hash definidas para busqueda/cifrado.
