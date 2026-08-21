package com.antifraude.licensing;

import com.antifraude.audit.AuditoriaService;
import com.antifraude.dto.ApiErrorDescriptor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminEmpresaObservabilityService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AuditoriaService auditoriaService;

    public AdminEmpresaObservabilityService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, AuditoriaService auditoriaService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.auditoriaService = auditoriaService;
    }

    public Map<String, Object> overview(UUID empresaId) {
        Map<String, Object> database = databaseMetrics();
        Map<String, Object> latency = apiLatency(empresaId);
        Map<String, Object> uptime = uptime();
        List<Map<String, Object>> traffic = trafficTrend24h(empresaId);
        List<Map<String, Object>> errors = errorHistory(empresaId);
        Map<String, Object> usage = latestUsage(empresaId);

        return mapOf(
                "database", database,
                "apiLatency", latency,
                "activeConnections", database.get("activeConnections"),
                "systemUptime", uptime,
                "trafficTrend24h", traffic,
                "errorTelemetry", errors,
                "usage", usage,
                "generatedAt", OffsetDateTime.now()
        );
    }

    private Map<String, Object> databaseMetrics() {
        Integer activeConnections = queryInt("select count(*) from pg_stat_activity where datname = current_database()", 0);
        Integer maxConnections = queryInt("select current_setting('max_connections')::int", 100);
        double load = maxConnections != null && maxConnections > 0
                ? Math.round((activeConnections * 10000.0) / maxConnections) / 100.0
                : 0.0;
        return mapOf(
                "label", "Presion Por Conexiones",
                "loadPercent", load,
                "activeConnections", activeConnections,
                "maxConnections", maxConnections,
                "description", "Porcentaje calculado con pg_stat_activity sobre max_connections. No representa CPU ni I/O."
        );
    }

    private Map<String, Object> apiLatency(UUID empresaId) {
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                select
                  coalesce(round(avg(duracion_ms))::int, 0) as avg_ms,
                  coalesce(round(percentile_cont(0.95) within group (order by duracion_ms))::int, 0) as p95_ms,
                  count(*)::int as total
                from api_evento
                where empresa_id = ? and fecha_evento >= now() - interval '24 hours'
                  and duracion_ms is not null
                """, empresaId);
        return mapOf(
                "avgMs", row.get("avg_ms"),
                "p95Ms", row.get("p95_ms"),
                "totalSamples", row.get("total"),
                "scope", "Requests internos y llamadas externas registrados por api_evento en las ultimas 24 horas"
        );
    }

    private Map<String, Object> uptime() {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        Duration duration = Duration.ofMillis(uptimeMs);
        return mapOf(
                "uptimeMs", uptimeMs,
                "uptimeSeconds", duration.toSeconds(),
                "display", displayDuration(duration),
                "description", "Tiempo transcurrido desde que el backend cliente esta levantado. Sirve para diagnosticar reinicios inesperados."
        );
    }

    private List<Map<String, Object>> trafficTrend24h(UUID empresaId) {
        return jdbcTemplate.queryForList("""
                with hours as (
                  select generate_series(
                    date_trunc('hour', now()) - interval '23 hours',
                    date_trunc('hour', now()),
                    interval '1 hour'
                  ) as bucket
                ),
                auditoria as (
                  select date_trunc('hour', fecha_evento) as bucket, count(*)::int as total
                  from auditoria_sistema
                  where empresa_id = ? and fecha_evento >= now() - interval '24 hours'
                  group by 1
                ),
                api as (
                  select date_trunc('hour', fecha_evento) as bucket,
                         count(*)::int as total,
                         count(*) filter (where origen = 'INTERNA')::int as internas,
                         count(*) filter (where origen = 'EXTERNA')::int as externas,
                         count(*) filter (where resultado = 'ERROR')::int as errores
                  from api_evento
                  where empresa_id = ? and fecha_evento >= now() - interval '24 hours'
                  group by 1
                )
                select
                  h.bucket,
                  coalesce(a.total, 0) as auditoria,
                  coalesce(api.externas, 0) as consultas_externas,
                  coalesce(api.internas, 0) as api_internas,
                  coalesce(api.externas, 0) as api_externas,
                  coalesce(api.errores, 0) as api_errores,
                  coalesce(api.total, 0) as api_total,
                  coalesce(a.total, 0) + coalesce(api.total, 0) as total
                from hours h
                left join auditoria a on a.bucket = h.bucket
                left join api on api.bucket = h.bucket
                order by h.bucket
                """, empresaId, empresaId);
    }

    private List<Map<String, Object>> errorHistory(UUID empresaId) {
        return apiErrors(empresaId, null, null, null, null);
    }

    public List<Map<String, Object>> apiErrors(UUID empresaId, Integer statusHttp, String origen,
                                               OffsetDateTime desde, OffsetDateTime hasta) {
        StringBuilder sql = new StringBuilder("""
                with eventos as (
                  select
                    id,
                    'api_evento' as fuente,
                    origen as tipo,
                    coalesce(codigo_error, 'HTTP_' || status_http, 'API_ERROR') as codigo,
                    coalesce(mensaje, categoria_error, 'Error de API') as mensaje,
                    servicio as origen,
                    endpoint,
                    coalesce(correlation_id, request_id, referencia_id) as referencia,
                    status_http,
                    duracion_ms,
                    fecha_evento as fecha
                  from api_evento
                  where empresa_id = ?
                    and resultado = 'ERROR'
                )
                select
                  id,
                  fuente,
                  tipo,
                  codigo,
                  mensaje,
                  origen,
                  endpoint,
                  referencia,
                  status_http,
                  duracion_ms,
                  fecha
                from eventos
                where 1 = 1
                """);
        java.util.List<Object> params = new java.util.ArrayList<>();
        params.add(empresaId);
        if (statusHttp != null) {
            sql.append(" and status_http = ?\n");
            params.add(statusHttp);
        }
        if (origen != null && !origen.isBlank()) {
            sql.append(" and origen = ?\n");
            params.add(origen);
        }
        if (desde != null) {
            sql.append(" and fecha >= ?\n");
            params.add(desde);
        }
        if (hasta != null) {
            sql.append(" and fecha <= ?\n");
            params.add(hasta);
        }
        sql.append("""
                order by fecha desc
                limit 80
                """);
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public List<String> origenesError(UUID empresaId) {
        return jdbcTemplate.queryForList("""
                select distinct servicio
                from api_evento
                where empresa_id = ? and resultado = 'ERROR'
                order by servicio
                """, String.class, empresaId);
    }

    public List<Map<String, Object>> recentApiErrors(UUID empresaId) {
        return errorHistory(empresaId);
    }

    public List<Map<String, Object>> ultimasConsultasExternas(UUID empresaId, int limit) {
        return jdbcTemplate.queryForList("""
                select
                  id,
                  endpoint as tipo_consulta,
                  servicio as proveedor,
                  status_http as status_http,
                  duracion_ms as duracion_ms,
                  intentos,
                  resultado,
                  resultado_funcional as resultado_funcional,
                  categoria_error as categoria_error,
                  estado,
                  correlation_id as correlation_id,
                  fecha_evento as fecha_consulta
                from api_evento
                where empresa_id = ? and origen = 'EXTERNA'
                order by fecha_evento desc
                limit ?
                """, empresaId, limit);
    }

    public Map<String, Object> configuracionLocal(UUID empresaId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select id, tipo, codigo, nombre, descripcion, estado, editable, orden, detalle_json::text as detalle
                from admin_empresa_configuracion_local
                where empresa_id = ?
                  and estado <> 'ELIMINADO'
                order by tipo, orden, codigo
                """, empresaId);
        List<Map<String, Object>> normalizedRows = rows.stream()
                .map(this::normalizarConfiguracionLocal)
                .toList();
        return mapOf(
                "parametrosEditables", normalizedRows.stream()
                        .filter(row -> "PARAMETRO".equalsIgnoreCase(String.valueOf(row.get("tipo"))))
                        .toList(),
                "jobs", normalizedRows.stream()
                        .filter(row -> "JOB".equalsIgnoreCase(String.valueOf(row.get("tipo"))))
                        .toList()
        );
    }

    @Transactional
    public Map<String, Object> actualizarParametro(UUID empresaId, String codigo, Map<String, Object> nuevoDetalle, UUID usuarioId, String ip, String userAgent) {
        String sql = """
                update admin_empresa_configuracion_local
                set detalle_json = detalle_json || ?::jsonb,
                    fecha_hora_modificacion = now()
                where empresa_id = ?
                  and codigo = ?
                  and tipo = 'PARAMETRO'
                  and editable = true
                  and estado <> 'ELIMINADO'
                returning id, tipo, codigo, nombre, descripcion, estado, editable, orden, detalle_json::text as detalle
                """;
        Map<String, Object> row = jdbcTemplate.queryForMap(sql, toJson(nuevoDetalle), empresaId, codigo);
        Map<String, Object> normalized = normalizarConfiguracionLocal(row);

        auditoriaService.registrar(usuarioId, empresaId, "ACTUALIZAR_PARAMETRO_CONFIGURACION",
                "Actualizacion de parametro " + codigo + " desde Admin Empresa",
                ip, userAgent,
                "admin_empresa_configuracion_local", (UUID) row.get("id"), null, toJson(nuevoDetalle));

        return normalized;
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new RuntimeException("Error serializando JSON", e);
        }
    }

    private Map<String, Object> normalizarConfiguracionLocal(Map<String, Object> row) {
        Map<String, Object> normalized = new LinkedHashMap<>(row);
        Object detalle = row.get("detalle");
        if (detalle instanceof String json && !json.isBlank()) {
            try {
                normalized.put("detalle", objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {}));
            } catch (Exception exception) {
                normalized.put("detalle", Map.of());
            }
        }
        return normalized;
    }

    public List<ApiErrorDescriptor> catalogoErrores() {
        return jdbcTemplate.query("""
                select origen, tipo_origen, api, codigo_error, status_code, mensaje, detalles, categoria,
                       coalesce(fecha_hora_modificacion, fecha_hora_creacion) as fecha
                from api_error_catalogo
                where activo = true
                order by origen, codigo_error
                """, (rs, rowNum) -> new ApiErrorDescriptor(
                rs.getString("origen"),
                rs.getString("tipo_origen"),
                rs.getString("api"),
                rs.getString("codigo_error"),
                rs.getInt("status_code"),
                rs.getString("mensaje"),
                rs.getString("detalles"),
                rs.getString("categoria"),
                rs.getObject("fecha", OffsetDateTime.class)
        ));
    }

    private Map<String, Object> latestUsage(UUID empresaId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select anio, mes, usuarios_activos, transacciones_procesadas, consultas_kyc, alertas_generadas, reportes_generados
                from uso_suscripcion
                where empresa_id = ?
                order by anio desc, mes desc, id desc
                limit 1
                """, empresaId);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private Integer queryInt(String sql, int fallback) {
        try {
            Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
            return value != null ? value : fallback;
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private String displayDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.minusDays(days).toHours();
        long minutes = duration.minusDays(days).minusHours(hours).toMinutes();
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }
}
