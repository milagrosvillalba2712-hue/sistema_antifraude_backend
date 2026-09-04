ALTER TABLE empresa
    ADD COLUMN IF NOT EXISTS direccion_contacto VARCHAR(250);

UPDATE empresa
SET direccion_contacto = COALESCE(NULLIF(direccion_contacto, ''), 'Av. Mariscal Lopez 1234, Asuncion')
WHERE id = '00000000-0000-0000-0000-000000000001';

COMMENT ON COLUMN empresa.direccion_contacto IS 'Direccion comercial o fiscal informativa de la empresa local. La fuente comercial para recibos es Control Plane.';
