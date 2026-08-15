package com.antifraude.observability;

import com.antifraude.config.ClientIpResolver;
import com.antifraude.security.tenant.TenantContext;
import com.antifraude.security.tenant.RlsContextService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ApiEventoService {

    private static final Logger log = LoggerFactory.getLogger(ApiEventoService.class);

    public static final String ATTR_CODIGO_ERROR = "regula.api.codigo_error";
    public static final String ATTR_CATEGORIA_ERROR = "regula.api.categoria_error";
    public static final String ATTR_MENSAJE_ERROR = "regula.api.mensaje_error";

    private final JdbcTemplate jdbcTemplate;
    private final RlsContextService rlsContextService;

    public ApiEventoService(JdbcTemplate jdbcTemplate, RlsContextService rlsContextService) {
        this.jdbcTemplate = jdbcTemplate;
        this.rlsContextService = rlsContextService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarEntrada(HttpServletRequest request, int status, long duracionMs) {
        if (request == null || request.getRequestURI() == null || !request.getRequestURI().startsWith("/api/")) {
            return;
        }
        String codigo = attr(request, ATTR_CODIGO_ERROR);
        String categoria = attr(request, ATTR_CATEGORIA_ERROR);
        String mensaje = attr(request, ATTR_MENSAJE_ERROR);
        String resultado = status >= 400 ? "ERROR" : "EXITOSO";
        registrar(TenantContext.getEmpresaId(), TenantContext.getUsuarioId(), "INTERNA", "ENTRANTE",
                servicioInterno(request.getRequestURI()), request.getRequestURI(), request.getMethod(), status,
                codigo, mensaje, resultado, categoria, duracionMs,
                request.getHeader("X-Correlation-Id"), requestId(request), ClientIpResolver.resolve(request),
                request.getHeader("User-Agent"), "api_endpoint", request.getRequestURI(), "{}",
                null, null, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarExterna(UUID empresaId, UUID usuarioId, String proveedor, String endpoint,
                                 Integer status, long duracionMs, String correlationId, String codigoError,
                                 String mensaje, boolean exitoso, String categoriaError, String referenciaEntidad,
                                 String referenciaId) {
        registrarExterna(empresaId, usuarioId, proveedor, endpoint, status, duracionMs, correlationId, codigoError,
                mensaje, exitoso, categoriaError, referenciaEntidad, referenciaId, null, null, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarExterna(UUID empresaId, UUID usuarioId, String proveedor, String endpoint,
                                 Integer status, long duracionMs, String correlationId, String codigoError,
                                 String mensaje, boolean exitoso, String categoriaError, String referenciaEntidad,
                                 String referenciaId, String documentoHash, Integer intentos,
                                 String resultadoFuncional, String estado) {
        registrar(empresaId, usuarioId, "EXTERNA", "SALIENTE", safe(proveedor, "PROVEEDOR_EXTERNO"), endpoint, "GET",
                status, codigoError, mensaje, exitoso ? "EXITOSO" : "ERROR", categoriaError, duracionMs,
                correlationId, null, null, null, referenciaEntidad, referenciaId, "{}",
                documentoHash, intentos, resultadoFuncional, estado);
    }

    private void registrar(UUID empresaId, UUID usuarioId, String origen, String direccion, String servicio,
                           String endpoint, String metodo, Integer status, String codigoError, String mensaje,
                           String resultado, String categoriaError, Long duracionMs, String correlationId,
                           String requestId, String ip, String userAgent, String referenciaEntidad,
                           String referenciaId, String detalleJson, String documentoHash, Integer intentos,
                           String resultadoFuncional, String estado) {
        try {
            rlsContextService.apply(empresaId, usuarioId);
            jdbcTemplate.update("""
                    insert into api_evento (
                        empresa_id, usuario_id, origen, direccion, servicio, endpoint, metodo_http,
                        status_http, codigo_error, mensaje, resultado, categoria_error, duracion_ms,
                        correlation_id, request_id, ip_origen, user_agent, referencia_entidad,
                        referencia_id, detalle_json, documento_hash, intentos, resultado_funcional, estado
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, cast(? as jsonb),
                               ?, ?, ?, ?)
                    """, empresaId, usuarioId, origen, direccion, servicio, endpoint, metodo, status, codigoError,
                    mensaje, resultado, categoriaError, duracionMs, correlationId, requestId, ip, userAgent,
                    referenciaEntidad, referenciaId, detalleJson != null ? detalleJson : "{}",
                    documentoHash, intentos, resultadoFuncional, estado);
        } catch (RuntimeException exception) {
            log.warn("[API_EVENTO] No se pudo registrar telemetria {} {}: {}", origen, servicio, exception.getMessage());
        }
    }

    private String attr(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value != null ? String.valueOf(value) : null;
    }

    private String requestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        return requestId != null && !requestId.isBlank() ? requestId : request.getHeader("X-Correlation-Id");
    }

    private String servicioInterno(String path) {
        if (path.startsWith("/api/auth")) return "AUTH";
        if (path.startsWith("/api/admin-empresa")) return "ADMIN_EMPRESA";
        if (path.startsWith("/api/alertas")) return "ALERTAS";
        if (path.startsWith("/api/kyc")) return "KYC";
        if (path.startsWith("/api/transacciones")) return "TRANSACCIONES";
        if (path.startsWith("/api/rule-engine")) return "MOTOR_REGLAS";
        if (path.startsWith("/api/admin")) return "ADMIN";
        return "SISTEMA_ANTIFRAUDE";
    }

    private String safe(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
