-- V26: pagos pendientes y pagos unicos por usuarios adicionales.
-- Un pago iniciado en Stripe Checkout todavia no tiene fecha_pago, y un pago
-- one-time por roles adicionales no necesariamente pertenece a una suscripcion.

ALTER TABLE pago
    ALTER COLUMN fecha_pago DROP NOT NULL,
    ALTER COLUMN suscripcion_id DROP NOT NULL;

COMMENT ON COLUMN pago.fecha_pago IS
    'Fecha efectiva de confirmacion del pago. Puede ser NULL mientras Stripe Checkout este pendiente.';

COMMENT ON COLUMN pago.suscripcion_id IS
    'Suscripcion asociada cuando el pago corresponde a licencia. Puede ser NULL para pagos one-time, como usuarios o roles adicionales.';
