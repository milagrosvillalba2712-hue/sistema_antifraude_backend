package com.antifraude.licensing;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/**
 * Mantenimiento de estados de suscripcion (POR_VENCER / VENCIDA / ACTIVA /
 * CERRADA). Migra la logica del scheduler diario fijo al sistema de jobs
 * configurable. Requiere RLS de la empresa activa (lo provee el runner).
 */
@Component
public class SuscripcionEstadosJob implements LicensingJob {

    private final JdbcTemplate jdbcTemplate;
    private final LicensingLocalService licensingService;

    public SuscripcionEstadosJob(JdbcTemplate jdbcTemplate,
                                 LicensingLocalService licensingService) {
        this.jdbcTemplate = jdbcTemplate;
        this.licensingService = licensingService;
    }

    @Override
    public String codigo() {
        return "SUSCRIPCION_ESTADOS";
    }

    @Override
    public ResultadoJob ejecutar(ContextoJob contexto) {
        LocalDate hoy = LocalDate.now();
        LocalDate umbralPorVencer = hoy.plusDays(30);
        int cambios = 0;

        cambios += jdbcTemplate.update("""
                update suscripcion set estado = 'POR_VENCER'
                where empresa_id = ? and estado = 'ACTIVA'
                  and fecha_fin >= ? and fecha_fin < ?""",
                contexto.empresaId(), hoy, umbralPorVencer);

        cambios += jdbcTemplate.update("""
                update suscripcion set estado = 'VENCIDA'
                where empresa_id = ? and estado in ('ACTIVA', 'POR_VENCER')
                  and fecha_fin < ?""",
                contexto.empresaId(), hoy);

        cambios += jdbcTemplate.update("""
                update suscripcion set estado = 'ACTIVA', fecha_inicio = ?, fecha_fin = ?
                where empresa_id = ? and estado = 'VENCIDA'
                  and renovacion_automatica and fecha_fin >= ?""",
                hoy, hoy.plusYears(1), contexto.empresaId(), hoy.minusDays(15));

        cambios += jdbcTemplate.update("""
                update suscripcion set estado = 'CERRADA'
                where empresa_id = ? and estado = 'VENCIDA'
                  and (renovacion_automatica is not true or fecha_fin < ?)""",
                contexto.empresaId(), hoy.minusDays(15));

        licensingService.registrarEvento(contexto.instalacion(), "SUSCRIPCION_ESTADOS",
                cambios > 0 ? "OK" : "SIN_CAMBIOS", Map.of("cambios", cambios));
        return new ResultadoJob(cambios > 0 ? "OK" : "SIN_CAMBIOS",
                cambios > 0 ? cambios + " suscripcion(es) actualizada(s)" : "Sin cambios de estado");
    }
}
