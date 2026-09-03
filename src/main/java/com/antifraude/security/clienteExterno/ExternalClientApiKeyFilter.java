package com.antifraude.security.clienteExterno;

import com.antifraude.common.entity.ClienteExterno;
import com.antifraude.security.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ExternalClientApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ExternalClientApiKeyFilter.class);
    private static final String HEADER_API_KEY = "X-API-Key";
    private static final String HEADER_REQUEST_ID = "X-Request-ID";

    private final ClienteExternoService clienteExternoService;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public ExternalClientApiKeyFilter(ClienteExternoService clienteExternoService,
                                      ObjectMapper objectMapper,
                                      @Value("${app.external-clients.enabled:true}") boolean enabled) {
        this.clienteExternoService = clienteExternoService;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String apiKey = request.getHeader(HEADER_API_KEY);

        if (!StringUtils.hasText(apiKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        long start = System.currentTimeMillis();
        String requestId = request.getHeader(HEADER_REQUEST_ID);
        String ip = resolveClientIp(request);

        Optional<ClienteExterno> clienteOpt = clienteExternoService.validarApiKey(apiKey);

        if (clienteOpt.isEmpty()) {
            long duracion = System.currentTimeMillis() - start;
            escribirError(response, HttpServletResponse.SC_UNAUTHORIZED, "API_KEY_INVALIDA",
                    "API key invalida o cliente externo inactivo", path);
            log.warn("[API-KEY] key={} ip={} path={} duracion={}ms - INVALIDA",
                    maskKey(apiKey), ip, path, duracion);
            return;
        }

        ClienteExterno cliente = clienteOpt.get();

        List<SimpleGrantedAuthority> authorities = Arrays.stream(cliente.getScopes())
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope.toUpperCase()))
                .collect(Collectors.toList());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "CLIENTE_EXTERNO:" + cliente.getCodigo(),
                        null,
                        authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        TenantContext.setEmpresaId(cliente.getEmpresa().getId());
        TenantContext.setUsuarioId(null);

        log.info("[API-KEY] cliente={} prefix={} path={} metodo={} ip={}",
                cliente.getCodigo(), cliente.getApiKeyPrefix(), path, request.getMethod(), ip);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duracion = System.currentTimeMillis() - start;
            int status = response.getStatus();
            clienteExternoService.registrarAuditoriaAsync(
                    cliente.getId(), path, request.getMethod(), ip,
                    status, duracion, null, requestId);
            TenantContext.clear();
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwarded)) {
            return xForwarded.split(",")[0].trim();
        }
        String xReal = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xReal)) {
            return xReal;
        }
        return request.getRemoteAddr();
    }

    private void escribirError(HttpServletResponse response, int status, String code,
                              String message, String path) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "status", status,
                "error", code,
                "message", message,
                "path", path,
                "timestamp", System.currentTimeMillis()
        ));
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 8) return "****";
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/api-docs")
                || path.equals("/error")
                || path.startsWith("/actuator/health");
    }
}
