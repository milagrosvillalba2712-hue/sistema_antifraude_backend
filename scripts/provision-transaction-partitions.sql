-- =====================================================================================
-- provision-transaction-partitions.sql
--
-- Rutina de INSTALADOR / ROL DE MANTENIMIENTO (matriz #9/#10 del plan de mejora
-- estructural). Aprovisiona particiones mensuales de `transacciones` para el mes
-- actual + N meses hacia adelante (rango razonable 12-24).
--
-- No y no manejar @Scheduled de Spring Boot (la app corre como regula_app_login,
-- sin DDL) ni pg_cron (no instalado en el cluster canónico). La invocación periódica
-- es responsabilidad de quien instala/actualiza: se ejecuta inspecteaduramente al
-- final del bootstrap y puede repetirse sin efecto.
--
-- Uso (rol de mantenimiento, dueño del esquema):
--   psql -h localhost -U regula_owner -d <base> -v ON_ERROR_STOP=1 \
--        --set=meses_adelante=24 -f provision-transaction-partitions.sql
--
-- Propiedades:
--   - Idempotente: reutiliza fn_create_transacciones_month_partition (que ya usa
--     CREATE TABLE IF NOT EXISTS). No duplica ni revierte particiones existentes.
--   - Autónomo: no toca cron.job ni extensiones; si pg_cron no existe, funciona igual.
--   - Reporta FILAS: una por partición creada; re-ejecución no produce filas.
-- =====================================================================================

-- Guarda: la tabla padre debe existir y estar particionada (la crea V10 canonical).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'public' AND c.relname = 'transacciones' AND c.relkind = 'p'
    ) THEN
        RAISE EXCEPTION 'transacciones no existe como tabla particionada; migración V10 no aplicada?';
    END IF;
END
$$;

-- Asegura particiones para el mes actual + hasta N meses futuros (idempotente).
--
-- IMPORTANTE: los rangos de las particiones actuales usan timestamptz con offset
-- fijo (sesión UTC-x que creó V10), e.j. FROM ('2028-07-01 03:00:00+00'). Si
-- reutilizáramos fn_create_transacciones_month_partition (date) el literal de
-- medianoche puede caer *dentro* del borde de la partición anterior y disparar
-- "would overlap". Por eso los límites se derivan del extremo de la última
-- partición existente: se crean rangos contiguos, sin solapes y sin depender de
-- la zona horaria de la sesión que ejecuta el instalador.
CREATE OR REPLACE FUNCTION public.fn_asegurar_particiones_transacciones(p_meses_adelante integer DEFAULT 24)
RETURNS TABLE(particion_creada text) AS
$$
DECLARE
    v_inicio timestamptz;
    v_fin timestamptz;
    v_nombre text;
    v_ultima timestamptz;
    v_limite timestamptz;
BEGIN
    IF p_meses_adelante < 12 OR p_meses_adelante > 24 THEN
        RAISE EXCEPTION 'p_meses_adelante fuera de rango razonable (12-24): %', p_meses_adelante;
    END IF;

    -- Extremo superior de la última partición mensual existente (el próximo
    -- rango comienza exactamente ahí, evitando solapamientos por diferencia de TZ).
    SELECT max((regexp_replace(pg_get_expr(c.relpartbound, c.oid),
               '^.* TO \(''(.*)''\)$', '\1'))::timestamptz)
      INTO v_ultima
      FROM pg_inherits i
      JOIN pg_class par ON par.oid = i.inhparent
      JOIN pg_class c ON c.oid = i.inhrelid
      JOIN pg_namespace n ON n.oid = par.relnamespace
      WHERE n.nspname = 'public' AND par.relname = 'transacciones'
        AND c.relpartbound IS NOT NULL
        AND c.relname ~ '^transacciones_[0-9]{4}_[0-9]{2}$';

    v_inicio := COALESCE(v_ultima, date_trunc('month', now()));
    -- Horizonte objetivo = mes actual + N meses. No se crea más allá.
    v_limite := date_trunc('month', now()) + (p_meses_adelante || ' months')::interval;

    WHILE v_inicio < v_limite LOOP
        v_fin := v_inicio + interval '1 month';
        v_nombre := 'transacciones_' || to_char(v_inicio, 'YYYY_MM');
        EXECUTE format('CREATE TABLE IF NOT EXISTS %I PARTITION OF transacciones FOR VALUES FROM (%L) TO (%L)',
                       v_nombre, v_inicio, v_fin);
        particion_creada := v_nombre;
        RETURN NEXT;
        v_inicio := v_fin;
    END LOOP;
    RETURN;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION public.fn_asegurar_particiones_transacciones(integer) IS
    'Rutina de instalador: crea (idempotente) las particiones mensuales de transacciones contiguas a la última existente, para asegurar un horizonte de 12-24 meses a partir de hoy. Devuelve cada partición asegurada. Límites derivados del extremo de la última partición para evitar solapamientos por zona horaria. Ejecuta como rol de mantenimiento; no requiere pg_cron.';

-- Monitoreo: cuántos meses de margen quedan hasta la última partición creada.
CREATE OR REPLACE FUNCTION public.fn_meses_particion_disponibles()
RETURNS integer AS
$$
DECLARE
    v_max_mes date;
    v_meses integer;
BEGIN
    SELECT max(to_date(regexp_replace(c.relname, 'transacciones_', ''), 'YYYY_MM'))
      INTO v_max_mes
      FROM pg_class c
      JOIN pg_namespace n ON n.oid = c.relnamespace
      JOIN pg_inherits i ON i.inhrelid = c.oid
      WHERE n.nspname = 'public'
        AND c.relname ~ '^transacciones_[0-9]{4}_[0-9]{2}$';

    IF v_max_mes IS NULL THEN
        RETURN 0;
    END IF;

    v_meses := (EXTRACT(YEAR FROM v_max_mes) - EXTRACT(YEAR FROM date_trunc('month', now())))::int * 12
              + (EXTRACT(MONTH FROM v_max_mes) - EXTRACT(MONTH FROM date_trunc('month', now())))::int + 1;
    RETURN v_meses;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION public.fn_meses_particion_disponibles() IS
    'Meses de margen entre hoy y la última partición mensual de transacciones creada (para control del horizonte).';

-- -------------------------------------------------------------------------------------
-- Ejecución de instalación: horizonte configurable, default 24 meses (12-24 válido).
-- -------------------------------------------------------------------------------------
\if :{?meses_adelante}
\else
\set meses_adelante 24
\endif

SELECT 'asegurando horizonte a ' || :meses_adelante || ' meses' AS estado;
SELECT public.fn_asegurar_particiones_transacciones(:meses_adelante) AS particion_asegurada;
SELECT public.fn_meses_particion_disponibles() AS meses_margen;