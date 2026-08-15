package com.antifraude.audit;

import com.antifraude.users.Usuario;
import com.antifraude.users.UsuarioRepository;
import com.antifraude.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Auditoria de mutaciones HTTP por niveles (Fase 3, item 3.3):
 *  - BASICA:  solo accion, descripcion, entidad e id.
 *  - AVANZADA: suma direccion IP y User-Agent (comportamiento por defecto).
 *  - TOTAL:    ademas captura el cuerpo de la peticion con redaccion DLP.
 * Nivel configurable via app.audit.nivel (AUDIT_NIVEL).
 */
@Component
public class MutationAuditFilter extends OncePerRequestFilter {

    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final int MAX_BODY_BYTES = 8000;

    private final AuditoriaService auditoriaService;
    private final UsuarioRepository usuarioRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final String nivel;

    public MutationAuditFilter(AuditoriaService auditoriaService, UsuarioRepository usuarioRepository,
                               JwtTokenProvider jwtTokenProvider,
                               @Value("${app.audit.nivel:avanzada}") String nivel) {
        this.auditoriaService = auditoriaService;
        this.usuarioRepository = usuarioRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.nivel = nivel;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request, bodyCacheSize());
        try {
            filterChain.doFilter(wrapped, response);
        } finally {
            if (shouldAudit(wrapped, response)) {
                audit(wrapped, response);
            }
        }
    }

    private int bodyCacheSize() {
        return "total".equalsIgnoreCase(nivel) ? MAX_BODY_BYTES : 0;
    }

    private boolean shouldAudit(HttpServletRequest request, HttpServletResponse response) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/")
                && !uri.startsWith("/api/auth/login")
                && !uri.startsWith("/api/auditoria")
                && MUTATING_METHODS.contains(request.getMethod())
                && response.getStatus() < 400;
    }

    private void audit(ContentCachingRequestWrapper request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication != null ? authentication.getName() : null;
        Optional<Usuario> usuario = email != null ? usuarioRepository.findByEmail(email) : Optional.empty();
        UUID usuarioId = usuario.map(Usuario::getId).orElse(null);
        String authorization = request.getHeader("Authorization");
        UUID empresaId = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            empresaId = jwtTokenProvider.getUuidClaim(authorization.substring(7), "empresaId").orElse(null);
        }
        String descripcion = request.getMethod() + " " + request.getRequestURI() + " respondio " + response.getStatus();
        String direccionIp = "avanzada".equalsIgnoreCase(nivel) || "total".equalsIgnoreCase(nivel)
                ? request.getRemoteAddr() : null;
        String userAgent = "avanzada".equalsIgnoreCase(nivel) || "total".equalsIgnoreCase(nivel)
                ? request.getHeader("User-Agent") : null;
        String cuerpo = "total".equalsIgnoreCase(nivel) ? cuerpoRedactado(request) : null;
        auditoriaService.registrar(usuarioId, empresaId, "HTTP_MUTACION", descripcion,
                direccionIp, userAgent, request.getRequestURI(), null, null, cuerpo);
    }

    private String cuerpoRedactado(ContentCachingRequestWrapper request) {
        byte[] cuerpo = request.getContentAsByteArray();
        if (cuerpo == null || cuerpo.length == 0) {
            return null;
        }
        return AuditoriaDlpRedactor.enmascarar(new String(cuerpo, StandardCharsets.UTF_8));
    }
}