-- Datos exclusivamente académicos. Esta ubicación solo se activa con el perfil demo.
INSERT INTO empresa (id, codigo, nombre, ruc, estado)
VALUES ('00000000-0000-0000-0000-000000000001', 'REGULA_DEMO', 'Empresa académica Regula', '80000000-0', 'ACTIVA')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO pais (codigo_iso, codigo_iso3, nombre, activo)
VALUES ('PY', 'PRY', 'Paraguay', true)
ON CONFLICT (codigo_iso) DO UPDATE
SET codigo_iso3 = EXCLUDED.codigo_iso3,
    nombre = EXCLUDED.nombre,
    activo = EXCLUDED.activo;

INSERT INTO moneda (codigo_iso, nombre, nombre_en, fuente, activo)
VALUES ('PYG', 'Guaraní paraguayo', 'Paraguayan guarani', 'ISO 4217 - demo', true)
ON CONFLICT (codigo_iso) DO UPDATE
SET nombre = EXCLUDED.nombre,
    nombre_en = EXCLUDED.nombre_en,
    fuente = EXCLUDED.fuente,
    activo = EXCLUDED.activo;

