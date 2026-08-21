package com.antifraude.licensing;

import com.antifraude.audit.AuditoriaService;
import com.antifraude.config.ClientIpResolver;
import com.antifraude.exception.BusinessException;
import com.antifraude.security.tenant.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Configuracion y ejecucion manual de los jobs locales de licenciamiento
 * (agente on-premise) desde Admin Empresa. La escritura sobre
 * admin_empresa_configuracion_local corre dentro del contexto RLS de la
 * peticion (TenantTransactionFilter).
 */
@RestController
@RequestMapping("/api/admin-empresa/configuracion/jobs")
@Transactional(readOnly = true)
public class AdminEmpresaJobsController {

    private static final Set<String> UNIDADES = Set.of("MINUTOS", "HORAS", "DIAS");
    private static final Set<String> ESTADOS = Set.of("ACTIVO", "INACTIVO");

    private final JdbcTemplate jdbcTemplate;
    private final LicensingJobRunner jobRunner;
    private final AuditoriaService auditoriaService;
    private final ObjectMapper objectMapper;

    public AdminEmpresaJobsController(JdbcTemplate jdbcTemplate,
                                      LicensingJobRunner jobRunner,
                                      AuditoriaService auditoriaService,
                                      ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jobRunner = jobRunner;
        this.auditoriaService = auditoriaService;
        this.objectMapper = objectMapper;
    }

    @PatchMapping("/{codigo}")
    @Transactional
    public ResponseEntity<Map<String, Object>> actualizar(@PathVariable String codigo,
                                                          @RequestBody Map<String, Object> body,
                                                          HttpServletRequest request) {
        UUID empresaId = empresaActual();
        Map<String, Object> row = cargarJob(empresaId, codigo);
        if (!Boolean.TRUE.equals(row.get("editable"))) {
            throw new BusinessException("JOB_NO_EDITABLE", "Este job no es editable");
        }
        Map<String, Object> detalle = parseDetalle(row.get("detalle"));

        String estado = stringValue(body.get("estado"));
        if (estado != null && !ESTADOS.contains(estado)) {
            throw new BusinessException("ESTADO_JOB_INVALIDO", "Estado valido: ACTIVO o INACTIVO");
        }
        if (body.get("frecuencia") instanceof Map<?, ?> frecuencia) {
            validarFrecuencia(frecuencia);
            detalle.put("frecuenciaValor", frecuencia.get("valor"));
            detalle.put("frecuenciaUnidad", String.valueOf(frecuencia.get("unidad")));
            detalle.put("hora", frecuencia.get("hora"));
            detalle.put("cron", frecuencia.get("cron"));
            detalle.put("proximaEjecucion", null);
        }

        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("update admin_empresa_configuracion_local set detalle_json = ?::jsonb");
        params.add(safeJson(detalle));
        if (estado != null) {
            sql.append(", estado = ?");
            params.add(estado);
        }
        sql.append(", fecha_hora_modificacion = now() where id = ?");
        params.add(row.get("id"));
        jdbcTemplate.update(sql.toString(), params.toArray());

        auditoriaService.registrar(TenantContext.getUsuarioId(), empresaId, "CONFIGURAR_JOB_LOCAL",
                "Configuracion del job local " + codigo, ClientIpResolver.resolve(request),
                request.getHeader("User-Agent"), "admin_empresa_configuracion_local", row.get("id"),
                null, safeJson(Map.of("estado", estado != null ? estado : row.get("estado"))));

        return ResponseEntity.ok(mapOf(
                "codigo", codigo,
                "estado", estado != null ? estado : row.get("estado"),
                "detalle", detalle
        ));
    }

    @PostMapping("/{codigo}/ejecutar")
    @Transactional
    public ResponseEntity<Map<String, Object>> ejecutar(@PathVariable String codigo,
                                                        HttpServletRequest request) {
        UUID empresaId = empresaActual();
        cargarJob(empresaId, codigo);
        LicensingJob.ResultadoJob resultado = jobRunner.ejecutarAhora(empresaId, codigo);

        auditoriaService.registrar(TenantContext.getUsuarioId(), empresaId, "EJECUTAR_JOB_LOCAL",
                "Ejecucion manual del job local " + codigo, ClientIpResolver.resolve(request),
                request.getHeader("User-Agent"), "admin_empresa_configuracion_local", codigo,
                null, safeJson(Map.of("resultado", resultado.resultado())));

        Map<String, Object> detalle = consultarDetalle(empresaId, codigo);
        return ResponseEntity.ok(mapOf(
                "codigo", codigo,
                "resultado", resultado.resultado(),
                "detalle", resultado.detalle(),
                "ultimaEjecucion", detalle.get("ultimaEjecucion"),
                "proximaEjecucion", detalle.get("proximaEjecucion")
        ));
    }

    private Map<String, Object> cargarJob(UUID empresaId, String codigo) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select id, codigo, editable, estado, detalle_json as detalle
                from admin_empresa_configuracion_local
                where empresa_id = ? and tipo = 'JOB' and codigo = ? and estado <> 'ELIMINADO'""",
                empresaId, codigo);
        if (rows.isEmpty()) {
            throw new BusinessException("JOB_DESCONOCIDO", "El job no esta configurado para esta empresa");
        }
        return rows.get(0);
    }

    private Map<String, Object> consultarDetalle(UUID empresaId, String codigo) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select detalle_json as detalle
                from admin_empresa_configuracion_local
                where empresa_id = ? and tipo = 'JOB' and codigo = ?""", empresaId, codigo);
        return rows.isEmpty() ? Map.of() : parseDetalle(rows.get(0).get("detalle"));
    }

    private void validarFrecuencia(Map<?, ?> frecuencia) {
        Object cron = frecuencia.get("cron");
        if (cron != null && StringUtils.hasText(String.valueOf(cron))) {
            try {
                CronExpression.parse(String.valueOf(cron));
            } catch (RuntimeException exception) {
                throw new BusinessException("CRON_INVALIDO", "Expresion cron invalida");
            }
            return;
        }
        if (!(frecuencia.get("valor") instanceof Number valor) || valor.longValue() <= 0) {
            throw new BusinessException("FRECUENCIA_INVALIDA", "La frecuencia requiere un valor positivo");
        }
        if (!UNIDADES.contains(String.valueOf(frecuencia.get("unidad")))) {
            throw new BusinessException("FRECUENCIA_INVALIDA", "Unidad valida: MINUTOS, HORAS o DIAS");
        }
    }

    private UUID empresaActual() {
        UUID empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) {
            throw new BusinessException("TENANT_REQUERIDO", "No se pudo resolver la empresa del usuario autenticado");
        }
        return empresaId;
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

    private String stringValue(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return String.valueOf(value);
    }

    private String safeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }
}
