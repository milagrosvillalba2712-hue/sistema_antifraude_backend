package com.antifraude.licensing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Purga local de retencion: elimina eventos y logs de aplicacion de esta
 * empresa con antiguedad mayor a la configurada (diasRetencion en la fila JOB
 * de admin_empresa_configuracion_local, con fallback a app.logs.retention-days).
 * Borra por empresa (RLS) y tambien las filas globales sin empresa.
 */
@Component
public class LogRetentionJob implements LicensingJob {

    private static final int RETENCION_POR_DEFECTO = 30;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final long diasRetencionDefault;

    public LogRetentionJob(JdbcTemplate jdbcTemplate,
                           ObjectMapper objectMapper,
                           @Value("${app.logs.retention-days:30}") long diasRetencionDefault) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.diasRetencionDefault = diasRetencionDefault;
    }

    @Override
    public String codigo() {
        return "LOG_RETENTION_PURGE";
    }

    @Override
    public ResultadoJob ejecutar(ContextoJob contexto) {
        long dias = leerDiasRetencion(contexto.empresaId());
        OffsetDateTime corte = OffsetDateTime.now().minusDays(dias);
        UUID empresaId = contexto.empresaId();

        int apiEventos = jdbcTemplate.update("""
                delete from api_evento
                where fecha_evento < ? and (empresa_id is null or empresa_id = ?)""",
                corte, empresaId);
        int logsApp = jdbcTemplate.update("""
                delete from app_log
                where fecha < ? and (empresa_id is null or empresa_id = ?)""",
                corte, empresaId);

        return new ResultadoJob("OK", "Purga de retencion (" + dias + " dias): " + apiEventos
                + " api_evento(s) y " + logsApp + " app_log eliminados");
    }

    private long leerDiasRetencion(UUID empresaId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select detalle_json as detalle
                from admin_empresa_configuracion_local
                where empresa_id = ? and tipo = 'JOB' and codigo = ?""",
                empresaId, codigo());
        if (rows.isEmpty()) {
            return diasRetencionDefault;
        }
        Map<String, Object> detalle = parseDetalle(rows.get(0).get("detalle"));
        Object dias = detalle.get("diasRetencion");
        if (dias instanceof Number number) {
            return number.longValue();
        }
        if (dias instanceof String texto && !texto.isBlank()) {
            try {
                return Long.parseLong(texto);
            } catch (NumberFormatException exception) {
                // se usa el default
            }
        }
        return diasRetencionDefault;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseDetalle(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        if (value != null && !String.valueOf(value).isBlank()) {
            try {
                return objectMapper.readValue(String.valueOf(value), new TypeReference<Map<String, Object>>() {});
            } catch (Exception exception) {
                return Map.of();
            }
        }
        return Map.of();
    }
}