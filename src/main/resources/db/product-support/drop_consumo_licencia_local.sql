-- El sistema del cliente usa uso_suscripcion como fuente unica de contadores
-- mensuales contra el plan. consumo_licencia_local era un contador redundante
-- que solo se alimentaba via seed demo y reportaba telemetria duplicada al
-- Control Plane. Se elimina del modelo para no duplicar fuentes de verdad.
DROP TABLE IF EXISTS consumo_licencia_local;
