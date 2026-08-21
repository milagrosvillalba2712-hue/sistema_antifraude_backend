package com.antifraude.licensing;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Control periodico de retencion de auditoria local: volumen y antiguedad de
 * eventos auditables conservados para la empresa.
 */
@Component
public class AuditRetentionCheckJob implements LicensingJob {

    private final JdbcTemplate jdbcTemplate;
    private final LicensingLocalService licensingService;

    public AuditRetentionCheckJob(JdbcTemplate jdbcTemplate,
                                  LicensingLocalService licensingService) {
        this.jdbcTemplate = jdbcTemplate;
        this.licensingService = licensingService;
    }

    @Override
    public String codigo() {
        return "AUDIT_RETENTION_CHECK";
    }

    @Override
    public ResultadoJob ejecutar(ContextoJob contexto) {
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                select count(*) as total,
                       coalesce(round(extract(epoch from (now() - min(fecha_evento))) / 86400), 0) as dias_mas_antiguo
                from auditoria_sistema
                where empresa_id = ?""", contexto.empresaId());
        long total = ((Number) row.get("total")).longValue();
        long diasMasAntiguo = ((Number) row.get("dias_mas_antiguo")).longValue();
        boolean ok = total <= 100_000;
        licensingService.registrarEvento(contexto.instalacion(), "AUDIT_RETENTION_CHECK",
                ok ? "OK" : "ADVERTENCIA",
                Map.of("eventosAuditables", total, "diasMasAntiguo", diasMasAntiguo));
        return new ResultadoJob(ok ? "OK" : "ADVERTENCIA",
                total + " evento(s) auditables, el mas antiguo de hace " + diasMasAntiguo + " dia(s)");
    }
}