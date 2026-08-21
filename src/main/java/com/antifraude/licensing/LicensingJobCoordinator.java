package com.antifraude.licensing;

import com.antifraude.security.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Coordinador del agente on-premise: revisa cada minuto las empresas con
 * instalacion activa y delega en {@link LicensingJobRunner} la ejecucion de
 * los jobs vencidos. Se controla con el switch global app.licenses.jobs.enabled
 * (env LICENSES_JOBS_ENABLED, habilitado por defecto en demo).
 */
@Component
@ConditionalOnProperty(name = "app.licenses.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class LicensingJobCoordinator {

    private static final Logger log = LoggerFactory.getLogger(LicensingJobCoordinator.class);

    private final LicensingJobRunner jobRunner;
    private final JdbcTemplate jdbcTemplate;

    public LicensingJobCoordinator(LicensingJobRunner jobRunner, JdbcTemplate jdbcTemplate) {
        this.jobRunner = jobRunner;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "0 * * * * *")
    public void tick() {
        try {
            List<UUID> empresas = jdbcTemplate.queryForList(
                    "select distinct empresa_id from instalacion_local where estado = 'ACTIVA' order by empresa_id",
                    UUID.class);
            for (UUID empresaId : empresas) {
                try {
                    jobRunner.ejecutarEmpresa(empresaId);
                } catch (RuntimeException exception) {
                    log.warn("[LICENCIA-JOBS] Empresa {} omitida: {} - {}", empresaId,
                            exception.getClass().getSimpleName(), exception.getMessage());
                }
            }
        } catch (RuntimeException exception) {
            log.warn("[LICENCIA-JOBS] Tick omitido: {} - {}", exception.getClass().getSimpleName(), exception.getMessage());
        } finally {
            TenantContext.clear();
        }
    }
}