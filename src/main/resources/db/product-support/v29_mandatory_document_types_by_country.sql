-- V29: tipos de documento obligatorios por pais en transacciones.
-- Metodologia: conservar fuente oficial/cita, regex operativa y jurisdiccion.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE tipo_documento ADD COLUMN IF NOT EXISTS pais_id bigint REFERENCES pais(id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE tipo_documento ADD COLUMN IF NOT EXISTS sigla varchar(20);
ALTER TABLE tipo_documento ADD COLUMN IF NOT EXISTS codigo_tecnico varchar(40);
ALTER TABLE tipo_documento ADD COLUMN IF NOT EXISTS fuente_oficial_cita text;
ALTER TABLE tipo_documento ADD COLUMN IF NOT EXISTS formato_regex varchar(220);
ALTER TABLE tipo_documento ADD COLUMN IF NOT EXISTS estado_activo boolean NOT NULL DEFAULT true;

UPDATE tipo_documento
SET pais_id = pais_relacion_id
WHERE pais_id IS NULL
  AND EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'tipo_documento'
      AND column_name = 'pais_relacion_id'
  );

UPDATE tipo_documento
SET codigo_tecnico = COALESCE(codigo_tecnico, codigo),
    sigla = COALESCE(sigla, codigo),
    estado_activo = COALESCE(estado_activo, activo, true);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tipo_documento_codigo_tecnico
    ON tipo_documento (codigo_tecnico)
    WHERE codigo_tecnico IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_tipo_documento_pais_sigla
    ON tipo_documento (pais_id, sigla)
    WHERE estado_activo = true;

WITH documentos(codigo, sigla, nombre, pais_iso, tipo_persona, formato_regex, fuente, cita) AS (
    VALUES
    ('PASS', 'PASS', 'Pasaporte ordinario legible por maquina', NULL, 'FISICA', '^[A-Z0-9]{6,12}$', 'ICAO Doc 9303', 'ICAO, Doc 9303 Machine Readable Travel Documents: https://www.icao.int/publications/doc-series/doc-9303'),
    ('CI_PY', 'CI', 'Cedula de Identidad Civil', 'PY', 'FISICA', '^\\d{5,10}$', 'Policia Nacional - Departamento de Identificaciones', 'Departamento de Identificaciones, Cedula de Identidad: https://www.policianacional.gov.py/identificaciones/cedula-de-identidad/'),
    ('RUC_PY', 'RUC', 'Registro Unico de Contribuyentes', 'PY', 'JURIDICA', '^\\d{5,12}-?[0-9A-Z]$', 'DNIT', 'DNIT, inscripcion en el RUC y Resolucion General 50/20: https://www.dnit.gov.py/en/web/portal-institucional/w/resolucion-general-n-50-20'),
    ('CRP_PY', 'CRP', 'Carnet de Residencia Permanente', 'PY', 'FISICA', '^[A-Z0-9-]{4,20}$', 'Direccion Nacional de Migraciones', 'Ley 6984/2022 de Migraciones, carnets de residencia: https://www.bacn.gov.py/leyes-paraguayas/10973/ley-n-6984-de-migraciones'),
    ('CRT_PY', 'CRT', 'Carnet de Residencia Temporal', 'PY', 'FISICA', '^[A-Z0-9-]{4,20}$', 'Direccion Nacional de Migraciones', 'Direccion Nacional de Migraciones, Residencias: https://migraciones.gov.py/residencias/'),
    ('CRE_PY', 'CRE', 'Carnet de Residencia Espontanea u Ocasional', 'PY', 'FISICA', '^[A-Z0-9-]{4,20}$', 'Direccion Nacional de Migraciones', 'Ley 6984/2022 de Migraciones, Residencia Espontanea u Ocasional: https://www.bacn.gov.py/leyes-paraguayas/10973/ley-n-6984-de-migraciones'),
    ('LIC_PY', 'LIC', 'Licencia de Conducir', 'PY', 'FISICA', '^[A-Z0-9-]{4,20}$', 'Agencia Nacional de Transito y Seguridad Vial', 'Ley 5016/2014 Nacional de Transito y Seguridad Vial, licencia de conducir: https://www.bacn.gov.py/leyes-paraguayas/4418/ley-n-5016-nacional-de-transito-y-seguridad-vial'),
    ('DNI_AR', 'DNI', 'Documento Nacional de Identidad', 'AR', 'FISICA', '^\\d{7,9}$', 'RENAPER / Argentina.gob.ar', 'Argentina.gob.ar, DNI vigente y normativa RENAPER: https://www.migraciones.gov.ar/ejemplar_dni/'),
    ('CUIT_AR', 'CUIT', 'Clave Unica de Identificacion Tributaria', 'AR', 'JURIDICA', '^\\d{2}-?\\d{8}-?\\d$', 'ARCA', 'Argentina.gob.ar, Resolucion General 5803/2025 ARCA CUIT: https://www.argentina.gob.ar/normativa/nacional/resoluci%C3%B3n-5803-2025-421823/texto'),
    ('CUIL_AR', 'CUIL', 'Codigo Unico de Identificacion Laboral', 'AR', 'FISICA', '^\\d{2}-?\\d{8}-?\\d$', 'RENAPER / ANSES', 'Argentina.gob.ar, Resolucion 2566/2014 incorpora CUIL al DNI: https://www.argentina.gob.ar/normativa/nacional/resoluci%C3%B3n-2566-2014-235019/texto'),
    ('CDI_AR', 'CDI', 'Clave de Identificacion', 'AR', 'FISICA', '^\\d{2}-?\\d{8}-?\\d$', 'ARCA', 'Argentina.gob.ar, Resolucion General 1065/2001 codigos documentales: https://www.argentina.gob.ar/normativa/nacional/resoluci%C3%B3n-1065-2001-68320/texto'),
    ('CPF_BR', 'CPF', 'Cadastro de Pessoas Fisicas', 'BR', 'FISICA', '^\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}$', 'Receita Federal / gov.br', 'gov.br, Cadastro de Pessoas Fisicas CPF: https://www.gov.br/pt-br/lgpd/cadastro-de-pessoas-fisicas-cpf'),
    ('CNPJ_BR', 'CNPJ', 'Cadastro Nacional da Pessoa Juridica', 'BR', 'JURIDICA', '^\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}$', 'Receita Federal / gov.br', 'Receita Federal, CNPJ en servicios gov.br: https://www.gov.br/'),
    ('RG_BR', 'RG', 'Registro Geral', 'BR', 'FISICA', '^[A-Z0-9.-]{5,20}$', 'Organos de identificacion estaduales Brasil', 'gov.br, servicios de identificacion y CPF relacionados: https://www.gov.br/pt-br/lgpd/cadastro-de-pessoas-fisicas-cpf'),
    ('DNI_PE', 'DNI', 'Documento Nacional de Identidad', 'PE', 'FISICA', '^\\d{8}$', 'RENIEC', 'RENIEC, identidad y DNI: https://www.reniec.gob.pe/'),
    ('DNID_PE', 'DNId', 'Documento Nacional de Identidad Digital', 'PE', 'FISICA', '^\\d{8}$', 'RENIEC', 'RENIEC, DNI digital: https://identidad.reniec.gob.pe/dnid'),
    ('RUC_PE', 'RUC', 'Registro Unico de Contribuyentes', 'PE', 'JURIDICA', '^\\d{11}$', 'SUNAT', 'SUNAT, Registro Unico de Contribuyentes: https://www.sunat.gob.pe/legislacion/ruc/index.html'),
    ('CC_CO', 'CC', 'Cedula de Ciudadania', 'CO', 'FISICA', '^\\d{6,10}$', 'Registraduria Nacional del Estado Civil', 'Registraduria Nacional del Estado Civil, identificacion ciudadana: https://www.registraduria.gov.co/'),
    ('CE_CO', 'CE', 'Cedula de Extranjeria', 'CO', 'FISICA', '^[A-Z0-9]{5,20}$', 'Migracion Colombia', 'Migracion Colombia, cedula de extranjeria: https://www.migracioncolombia.gov.co/'),
    ('NIT_CO', 'NIT', 'Numero de Identificacion Tributaria', 'CO', 'JURIDICA', '^\\d{5,12}-?\\d$', 'DIAN', 'DIAN, Registro Unico Tributario y NIT: https://www.dian.gov.co/'),
    ('CI_UY', 'CI', 'Cedula de Identidad', 'UY', 'FISICA', '^\\d{1,8}-?\\d$', 'Direccion Nacional de Identificacion Civil Uruguay', 'DNIC Uruguay, cedula de identidad: https://www.gub.uy/ministerio-interior/'),
    ('RUT_UY', 'RUT', 'Registro Unico Tributario', 'UY', 'JURIDICA', '^\\d{12}$', 'Direccion General Impositiva Uruguay', 'DGI Uruguay, RUT: https://www.gub.uy/direccion-general-impositiva/'),
    ('RUN_CL', 'RUN', 'Rol Unico Nacional', 'CL', 'FISICA', '^\\d{1,2}\\.?\\d{3}\\.?\\d{3}-?[0-9Kk]$', 'Servicio de Registro Civil e Identificacion Chile', 'Registro Civil Chile, cedula/RUN: https://www.registrocivil.cl/'),
    ('RUT_CL', 'RUT', 'Rol Unico Tributario', 'CL', 'JURIDICA', '^\\d{1,2}\\.?\\d{3}\\.?\\d{3}-?[0-9Kk]$', 'Servicio de Impuestos Internos Chile', 'SII Chile, RUT e inicio de actividades: https://www.sii.cl/'),
    ('CI_BO', 'CI', 'Cedula de Identidad', 'BO', 'FISICA', '^\\d{5,12}$', 'SEGIP Bolivia', 'SEGIP Bolivia, cedula de identidad: https://www.segip.gob.bo/'),
    ('NIT_BO', 'NIT', 'Numero de Identificacion Tributaria', 'BO', 'JURIDICA', '^\\d{5,15}$', 'Servicio de Impuestos Nacionales Bolivia', 'SIN Bolivia, NIT: https://www.impuestos.gob.bo/'),
    ('CI_EC', 'CI', 'Cedula de Identidad', 'EC', 'FISICA', '^\\d{10}$', 'Registro Civil Ecuador', 'Registro Civil Ecuador, identificacion: https://www.registrocivil.gob.ec/'),
    ('RUC_EC', 'RUC', 'Registro Unico de Contribuyentes', 'EC', 'JURIDICA', '^\\d{13}$', 'SRI Ecuador', 'SRI Ecuador, RUC: https://www.sri.gob.ec/'),
    ('CURP_MX', 'CURP', 'Clave Unica de Registro de Poblacion', 'MX', 'FISICA', '^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]\\d$', 'RENAPO Mexico', 'Gobierno de Mexico, CURP: https://www.gob.mx/curp/'),
    ('RFC_MX', 'RFC', 'Registro Federal de Contribuyentes', 'MX', 'JURIDICA', '^[A-Z&Ñ]{3,4}\\d{6}[A-Z0-9]{3}$', 'SAT Mexico', 'SAT Mexico, RFC: https://www.sat.gob.mx/'),
    ('INE_MX', 'INE', 'Credencial para Votar', 'MX', 'FISICA', '^[A-Z0-9]{10,20}$', 'Instituto Nacional Electoral Mexico', 'INE Mexico, credencial para votar: https://www.ine.mx/')
)
INSERT INTO tipo_documento (codigo, codigo_tecnico, sigla, nombre, descripcion, pais_id, tipo_persona, fuente_oficial, fuente_oficial_cita, formato_regex, activo, estado_activo)
SELECT d.codigo,
       d.codigo,
       d.sigla,
       d.nombre,
       'Tipo documental oficial utilizado para KYC, transacciones AML y screening por pais.',
       p.id,
       d.tipo_persona,
       d.fuente,
       d.cita,
       d.formato_regex,
       true,
       true
FROM documentos d
LEFT JOIN pais p ON p.codigo_iso = d.pais_iso
ON CONFLICT (codigo) DO UPDATE SET
    codigo_tecnico = EXCLUDED.codigo_tecnico,
    sigla = EXCLUDED.sigla,
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    pais_id = COALESCE(EXCLUDED.pais_id, tipo_documento.pais_id),
    tipo_persona = EXCLUDED.tipo_persona,
    fuente_oficial = EXCLUDED.fuente_oficial,
    fuente_oficial_cita = EXCLUDED.fuente_oficial_cita,
    formato_regex = EXCLUDED.formato_regex,
    activo = true,
    estado_activo = true;

ALTER TABLE transacciones ADD COLUMN IF NOT EXISTS tipo_documento_remitente_id bigint REFERENCES tipo_documento(id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE transacciones ADD COLUMN IF NOT EXISTS pais_emisor_documento_remitente_id bigint REFERENCES pais(id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE transacciones ADD COLUMN IF NOT EXISTS tipo_documento_beneficiario_id bigint REFERENCES tipo_documento(id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE transacciones ADD COLUMN IF NOT EXISTS pais_emisor_documento_beneficiario_id bigint REFERENCES pais(id) ON DELETE RESTRICT ON UPDATE CASCADE;

UPDATE transacciones
SET documento_remitente_hash = COALESCE(documento_remitente_hash, hmac('legacy-remitente-' || id::text, 'regula-migration-hmac-key', 'sha256')),
    documento_beneficiario_hash = COALESCE(documento_beneficiario_hash, hmac('legacy-beneficiario-' || id::text, 'regula-migration-hmac-key', 'sha256')),
    tipo_documento_remitente_id = COALESCE(tipo_documento_remitente_id, (SELECT id FROM tipo_documento WHERE codigo = 'CI_PY')),
    tipo_documento_beneficiario_id = COALESCE(tipo_documento_beneficiario_id, (SELECT id FROM tipo_documento WHERE codigo = 'CI_PY')),
    pais_emisor_documento_remitente_id = COALESCE(pais_emisor_documento_remitente_id, (SELECT id FROM pais WHERE codigo_iso = 'PY')),
    pais_emisor_documento_beneficiario_id = COALESCE(pais_emisor_documento_beneficiario_id, (SELECT id FROM pais WHERE codigo_iso = 'PY'))
WHERE documento_remitente_hash IS NULL
   OR documento_beneficiario_hash IS NULL
   OR tipo_documento_remitente_id IS NULL
   OR tipo_documento_beneficiario_id IS NULL
   OR pais_emisor_documento_remitente_id IS NULL
   OR pais_emisor_documento_beneficiario_id IS NULL;

ALTER TABLE transacciones ALTER COLUMN documento_remitente_hash SET NOT NULL;
ALTER TABLE transacciones ALTER COLUMN documento_beneficiario_hash SET NOT NULL;
ALTER TABLE transacciones ALTER COLUMN tipo_documento_remitente_id SET NOT NULL;
ALTER TABLE transacciones ALTER COLUMN pais_emisor_documento_remitente_id SET NOT NULL;
ALTER TABLE transacciones ALTER COLUMN tipo_documento_beneficiario_id SET NOT NULL;
ALTER TABLE transacciones ALTER COLUMN pais_emisor_documento_beneficiario_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS ix_tx_doc_remitente_screening
    ON transacciones (empresa_id, tipo_documento_remitente_id, documento_remitente_hash, fecha_transaccion DESC);
CREATE INDEX IF NOT EXISTS ix_tx_doc_beneficiario_screening
    ON transacciones (empresa_id, tipo_documento_beneficiario_id, documento_beneficiario_hash, fecha_transaccion DESC);
CREATE INDEX IF NOT EXISTS ix_tx_pais_doc_remitente
    ON transacciones (empresa_id, pais_emisor_documento_remitente_id, fecha_transaccion DESC);
CREATE INDEX IF NOT EXISTS ix_tx_pais_doc_beneficiario
    ON transacciones (empresa_id, pais_emisor_documento_beneficiario_id, fecha_transaccion DESC);

COMMENT ON COLUMN tipo_documento.pais_id IS 'Pais o jurisdiccion emisora del tipo documental. NULL indica documento global como pasaporte.';
COMMENT ON COLUMN tipo_documento.sigla IS 'Sigla oficial o comun visible para el usuario, por ejemplo CI, RUC, DNI, CPF.';
COMMENT ON COLUMN tipo_documento.codigo_tecnico IS 'Codigo tecnico estable usado por APIs y seeds, por ejemplo CI_PY o RUC_PY.';
COMMENT ON COLUMN tipo_documento.fuente_oficial_cita IS 'Cita bibliografica o URL oficial usada para justificar el tipo documental.';
COMMENT ON COLUMN tipo_documento.formato_regex IS 'Expresion regular operativa para validacion preliminar del numero documental.';
COMMENT ON COLUMN tipo_documento.estado_activo IS 'Indica si el tipo documental esta habilitado para nuevas operaciones.';
COMMENT ON COLUMN transacciones.tipo_documento_remitente_id IS 'Tipo de documento obligatorio del remitente de la transaccion.';
COMMENT ON COLUMN transacciones.pais_emisor_documento_remitente_id IS 'Pais emisor obligatorio del documento del remitente.';
COMMENT ON COLUMN transacciones.tipo_documento_beneficiario_id IS 'Tipo de documento obligatorio del beneficiario de la transaccion.';
COMMENT ON COLUMN transacciones.pais_emisor_documento_beneficiario_id IS 'Pais emisor obligatorio del documento del beneficiario.';
