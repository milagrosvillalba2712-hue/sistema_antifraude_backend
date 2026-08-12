package com.antifraude.licensing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true")
public class EstadoSuscripcionScheduler {

    private static final Logger log = LoggerFactory.getLogger(EstadoSuscripcionScheduler.class);

    private final JdbcTemplate jdbcTemplate;

    public EstadoSuscripcionScheduler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void actualizarEstadosSuscripciones() {
        LocalDate hoy = LocalDate.now();
        LocalDate umbralPorVencer = hoy.plusDays(30);
        List<UUID> empresas = jdbcTemplate.query("select id from empresa order by id",
                (rs, rowNum) -> UUID.fromString(rs.getString(1)));
        int cambios = 0;
        for (UUID empresaId : empresas) {
            jdbcTemplate.update("select set_config('app.current_empresa_id', ?, true)", empresaId.toString());

            cambios += jdbcTemplate.update("""
                    update suscripcion set estado = 'POR_VENCER'
                    where empresa_id = ? and estado = 'ACTIVA'
                      and fecha_fin >= ? and fecha_fin < ?""",
                    empresaId, hoy, umbralPorVencer);

            cambios += jdbcTemplate.update("""
                    update suscripcion set estado = 'VENCIDA'
                    where empresa_id = ? and estado in ('ACTIVA', 'POR_VENCER')
                      and fecha_fin < ?""",
                    empresaId, hoy);

            cambios += jdbcTemplate.update("""
                    update suscripcion set estado = 'ACTIVA', fecha_inicio = ?, fecha_fin = ?
                    where empresa_id = ? and estado = 'VENCIDA'
                      and renovacion_automatica and fecha_fin >= ?""",
                    empresaId, hoy, hoy.plusYears(1), hoy.minusDays(15));

            cambios += jdbcTemplate.update("""
                    update suscripcion set estado = 'CERRADA'
                    where empresa_id = ? and estado = 'VENCIDA'
                      and (renovacion_automatica is not true or fecha_fin < ?)""",
                    empresaId, hoy.minusDays(15));
        }
        if (cambios > 0) {
            log.info("[SCHEDULER] Estados de suscripcion actualizados: {} cambios en {} empresas", cambios, empresas.size());
        }
    }
}