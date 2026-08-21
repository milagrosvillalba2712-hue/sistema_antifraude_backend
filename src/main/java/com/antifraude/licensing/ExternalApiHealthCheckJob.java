package com.antifraude.licensing;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Verificacion periodica de disponibilidad de proveedores externos sandbox.
 * Consolida el resultado de la ultima ventana de api_evento (origen EXTERNA);
 * no ejecuta pings a proveedores reales, evita latencia y ruido en la demo.
 */
@Component
public class ExternalApiHealthCheckJob implements LicensingJob {

    private final JdbcTemplate jdbcTemplate;
    private final LicensingLocalService licensingService;

    public ExternalApiHealthCheckJob(JdbcTemplate jdbcTemplate,
                                     LicensingLocalService licensingService) {
        this.jdbcTemplate = jdbcTemplate;
        this.licensingService = licensingService;
    }

    @Override
    public String codigo() {
        return "EXTERNAL_API_HEALTH_CHECK";
    }

    @Override
    public ResultadoJob ejecutar(ContextoJob contexto) {
        List<Map<String, Object>> proveedores = jdbcTemplate.queryForList("""
                select servicio as proveedor,
                       count(*) filter (where resultado = 'ERROR') as errores,
                       count(*) filter (where resultado = 'EXITOSO') as exitosas
                from api_evento
                where empresa_id = ? and origen = 'EXTERNA'
                  and fecha_evento >= now() - interval '10 minutes'
                group by servicio
                order by servicio""", contexto.empresaId());
        boolean saludOk = proveedores.isEmpty()
                || proveedores.stream().noneMatch(p -> ((Number) p.get("errores")).intValue() > 0);
        licensingService.registrarEvento(contexto.instalacion(), "EXTERNAL_API_CHECK",
                saludOk ? "OK" : "ADVERTENCIA",
                Map.of("proveedoresRevisados", proveedores.size(), "proveedores", proveedores));
        return new ResultadoJob(saludOk ? "OK" : "ADVERTENCIA",
                proveedores.isEmpty()
                        ? "Sin consultas externas en la ventana"
                        : proveedores.size() + " proveedor(es) revisado(s), sin errores");
    }
}
