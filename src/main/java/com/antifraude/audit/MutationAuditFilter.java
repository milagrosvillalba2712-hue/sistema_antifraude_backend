package com.antifraude.audit;

import com.antifraude.users.Usuario;
import com.antifraude.users.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

@Component
public class MutationAuditFilter extends OncePerRequestFilter {

    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final AuditoriaService auditoriaService;
    private final UsuarioRepository usuarioRepository;

    public MutationAuditFilter(AuditoriaService auditoriaService, UsuarioRepository usuarioRepository) {
        this.auditoriaService = auditoriaService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request);
        try {
            filterChain.doFilter(wrapped, response);
        } finally {
            if (shouldAudit(wrapped, response)) {
                audit(wrapped, response);
            }
        }
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
        String body = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);
        String payload = body.isBlank() ? null : body;
        UUID usuarioId = usuario.map(Usuario::getId).orElse(null);
        auditoriaService.registrar(usuarioId, null, "HTTP_MUTACION",
                request.getMethod() + " " + request.getRequestURI() + " respondio " + response.getStatus(),
                request.getRemoteAddr(), request.getHeader("User-Agent"),
                request.getRequestURI(), null, null, payload);
    }
}
