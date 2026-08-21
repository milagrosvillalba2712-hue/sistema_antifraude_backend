package com.antifraude.licensing;

import com.antifraude.audit.AuditoriaService;
import com.antifraude.exception.BusinessException;
import com.antifraude.security.tenant.RlsContextService;
import com.antifraude.security.tenant.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Ejecuta los jobs de licenciamiento de una empresa dentro de su propio
 * contexto de tenant (RLS). Lee la configuracion de admin_empresa_configuracion_local
 * (tipo JOB), despacha los ejecutores vencidos y persiste el resultado
 * (ultima/proxima ejecucion, resultado y detalle) en detalle_json.
 */
@Service
public class LicensingJobRunner {

    private final JdbcTemplate jdbcTemplate;
    private final RlsContextService rlsContextService;
    private final InstalacionLocalRepository instalacionRepository;
    private final ObjectMapper objectMapper;
    private final AuditoriaService auditoriaService;
    private final Map<String, LicensingJob> jobsPorCodigo;

    public LicensingJobRunner(JdbcTemplate jdbcTemplate,
                              RlsContextService rlsContextService,
                              InstalacionLocalRepository instalacionRepository,
                              ObjectMapper objectMapper,
                              AuditoriaService auditoriaService,
                              List<LicensingJob> jobs) {
        this.jdbcTemplate = jdbcTemplate;
        this.rlsContextService = rlsContextService;
        this.instalacionRepository = instalacionRepository;
        this.objectMapper = objectMapper;
        this.auditoriaService = auditoriaService;
        this.jobsPorCodigo = jobs.stream()
                .collect(Collectors.toMap(LicensingJob::codigo, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * Ejecuta los jobs ACTIVO vencidos de la empresa. Se invoca desde el
     * coordinador; cada llamada corre en su propia transaccion con el RLS de
     * la empresa aplicado.
     */
    @Transactional
    public void ejecutarEmpresa(UUID empresaId) {
        TenantContext.setEmpresaId(empresaId);

        UUID systemUserId = resolverSystemUserId(empresaId);
        TenantContext.setUsuarioId(systemUserId);
        rlsContextService.apply(empresaId, systemUserId);

        InstalacionLocal instalacion = instalacionRepository.findTopByEmpresaIdOrderByActivadaEnDesc(empresaId)
                .orElse(null);
        if (instalacion == null) {
            return;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select id, codigo, detalle_json as detalle
                from admin_empresa_configuracion_local
                where empresa_id = ? and tipo = 'JOB' and estado = 'ACTIVO'
                order by orden, codigo""", empresaId);
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            String codigo = String.valueOf(row.get("codigo"));
            Map<String, Object> detalle = parseDetalle(row.get("detalle"));
            if (!debeEjecutar(detalle)) {
                continue;
            }
LicensingJob job = jobsPorCodigo.get(codigo);
            if (job == null) {
                continue;
            }
            ejecutarYRegistrar(empresaId, instalacion, id, codigo, detalle, job, false);
        }
    }

    /**
     * Ejecuta un job puntual (ejecucion manual desde Admin Empresa) y persiste
     * el resultado. No exige que el job este ACTIVO.
     */
    @Transactional
    public LicensingJob.ResultadoJob ejecutarAhora(UUID empresaId, String codigo) {
        TenantContext.setEmpresaId(empresaId);

        UUID systemUserId = resolverSystemUserId(empresaId);
        TenantContext.setUsuarioId(systemUserId);
        rlsContextService.apply(empresaId, systemUserId);

        InstalacionLocal instalacion = instalacionRepository.findTopByEmpresaIdOrderByActivadaEnDesc(empresaId)
                .orElseThrow(() -> new BusinessException("INSTALACION_NO_ENCONTRADA",
                        "No existe una instalacion local para la empresa"));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select id, codigo, detalle_json as detalle
                from admin_empresa_configuracion_local
                where empresa_id = ? and tipo = 'JOB' and codigo = ?""", empresaId, codigo);
        if (rows.isEmpty()) {
            throw new BusinessException("JOB_DESCONOCIDO", "El job no esta configurado para esta empresa");
        }
        LicensingJob job = jobsPorCodigo.get(codigo);
        if (job == null) {
            throw new BusinessException("JOB_DESCONOCIDO", "No existe ejecutor para el job " + codigo);
        }
        Map<String, Object> row = rows.get(0);
        Long id = ((Number) row.get("id")).longValue();
        Map<String, Object> detalle = parseDetalle(row.get("detalle"));
        LicensingJob.ResultadoJob resultado = ejecutarYRegistrar(empresaId, instalacion, id, codigo, detalle, job, true);
        Map<String, Object> refrescado = consultarDetalle(empresaId, codigo);
        return new LicensingJob.ResultadoJob(resultado.resultado(), String.valueOf(refrescado.getOrDefault("ultimoDetalle", resultado.detalle())));
    }

    public boolean existeJob(String codigo) {
        return jobsPorCodigo.containsKey(codigo);
    }

    private LicensingJob.ResultadoJob ejecutarYRegistrar(UUID empresaId, InstalacionLocal instalacion, Long id,
                                            String codigo, Map<String, Object> detalle, LicensingJob job, boolean manual) {
        try {
            LicensingJob.ResultadoJob resultado = job.ejecutar(new LicensingJob.ContextoJob(empresaId, instalacion));
            persistirResultado(empresaId, id, codigo, detalle, resultado, manual, TenantContext.getUsuarioId());
            return resultado;
        } catch (RuntimeException exception) {
            LicensingJob.ResultadoJob resultado = new LicensingJob.ResultadoJob("ERROR", exception.getClass().getSimpleName());
            persistirResultado(empresaId, id, codigo, detalle, resultado, manual, TenantContext.getUsuarioId());
            return resultado;
        }
    }

    private void persistirResultado(UUID empresaId, Long id, String codigo, Map<String, Object> detalle,
                                    LicensingJob.ResultadoJob resultado, boolean manual, UUID usuarioId) {
        OffsetDateTime ahora = OffsetDateTime.now();
        Map<String, Object> nuevo = new LinkedHashMap<>(detalle);
        nuevo.put("ultimaEjecucion", ahora);
        nuevo.put("proximaEjecucion", calcularProxima(ahora, detalle));
        nuevo.put("ultimoResultado", resultado.resultado());
        nuevo.put("ultimoDetalle", resultado.detalle());
        jdbcTemplate.update("""
                update admin_empresa_configuracion_local
                set detalle_json = ?::jsonb, fecha_hora_modificacion = now()
                where id = ?""", safeJson(nuevo), id);

        auditoriaService.registrar(usuarioId, empresaId, manual ? "EJECUTAR_JOB_LOCAL" : "EJECUTAR_JOB_AUTOMATICO",
                (manual ? "Ejecucion manual" : "Ejecucion automatica") + " del job local " + codigo,
                null, null,
                "admin_empresa_configuracion_local", id,
                null, safeJson(Map.of("resultado", resultado.resultado(), "detalle", resultado.detalle())));
    }

    private boolean debeEjecutar(Map<String, Object> detalle) {
        Object proxima = detalle.get("proximaEjecucion");
        if (proxima == null) {
            return true;
        }
        try {
            OffsetDateTime proximaEjecucion = OffsetDateTime.parse(String.valueOf(proxima));
            return !proximaEjecucion.isAfter(OffsetDateTime.now());
        } catch (RuntimeException exception) {
            return true;
        }
    }

    private OffsetDateTime calcularProxima(OffsetDateTime ahora, Map<String, Object> detalle) {
        String cron = stringValue(detalle.get("cron"));
        if (StringUtils.hasText(cron)) {
            try {
                return CronExpression.parse(cron).next(OffsetDateTime.now());
            } catch (RuntimeException exception) {
                return ahora.plusHours(1);
            }
        }
        long valor = Math.max(1, numberValue(detalle.get("frecuenciaValor"), 1));
        String unidad = String.valueOf(detalle.getOrDefault("frecuenciaUnidad", "HORAS"));
        return switch (unidad.toUpperCase()) {
            case "MINUTOS" -> ahora.plusMinutes(valor);
            case "DIAS" -> {
                String hora = stringValue(detalle.get("hora"));
                if (hora != null) {
                    yield proximaOcurrenciaDiaria(ahora, hora);
                }
                yield ahora.plusDays(valor);
            }
            default -> ahora.plusHours(valor);
        };
    }

    private OffsetDateTime proximaOcurrenciaDiaria(OffsetDateTime ahora, String hora) {
        String[] partes = hora.split(":");
        int horas = Integer.parseInt(partes[0]);
        int minutos = partes.length > 1 ? Integer.parseInt(partes[1]) : 0;
        OffsetDateTime hoy = ahora.withHour(horas).withMinute(minutos).withSecond(0).withNano(0);
        return hoy.isAfter(ahora) ? hoy : hoy.plusDays(1);
    }

    private Map<String, Object> consultarDetalle(UUID empresaId, String codigo) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select detalle_json as detalle
                from admin_empresa_configuracion_local
                where empresa_id = ? and tipo = 'JOB' and codigo = ?""", empresaId, codigo);
        if (rows.isEmpty()) {
            return Map.of();
        }
        return parseDetalle(rows.get(0).get("detalle"));
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
                return new LinkedHashMap<>();
            }
        }
        return new LinkedHashMap<>();
    }

    private String safeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private String stringValue(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return String.valueOf(value);
    }

    private long numberValue(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private UUID resolverSystemUserId(UUID empresaId) {
        // Buscar usuario sistema en usuarios (tabla sin RLS)
        String sqlUser = "SELECT id FROM usuarios WHERE email ILIKE 'system@%.local' LIMIT 1";
        try {
            return jdbcTemplate.queryForObject(sqlUser, UUID.class);
        } catch (Exception e) {
            throw new BusinessException("SYSTEM_USER_NO_ENCONTRADO",
                    "No existe usuario de sistema (system@*.local) en la tabla usuarios. Verifique seed.");
        }
    }
}
