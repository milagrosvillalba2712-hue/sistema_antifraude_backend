package com.antifraude.licensing;

import com.antifraude.dto.ErrorResponse;
import com.antifraude.exception.BusinessException;
import com.antifraude.security.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;

/**
 * Enforcement de licencias por request:
 *
 * 1) SaaS (Fase 1): exige empresa ACTIVA con suscripcion vigente.
 * 2) Local on-premise (Fase 2, opcional via app.licenses.local.enforcement.enabled):
 *    - OPERATIVO: se deja pasar.
 *    - SOLO_LECTURA (vencido/offline dentro de gracia): solo metodos de
 *      lectura (GET/HEAD/OPTIONS); las mutaciones responden 403.
 *    - BLOQUEADO (vencido sin gracia o revocado): 403 para todo.
 */
@Component
public class LicenciaFilter extends OncePerRequestFilter {

    private final EnforcementService enforcementService;
    private final LicensingValidationService validationService;
    private final InstalacionLocalRepository instalacionRepository;
    private final LicenciaLocalRepository licenciaRepository;
    private final ObjectMapper objectMapper;
    private final boolean enforcementEnabled;
    private final boolean localEnforcementEnabled;

    public LicenciaFilter(EnforcementService enforcementService,
                          LicensingValidationService validationService,
                          InstalacionLocalRepository instalacionRepository,
                          LicenciaLocalRepository licenciaRepository,
                          ObjectMapper objectMapper,
                          @Value("${app.licenses.enforcement.enabled:true}") boolean enforcementEnabled,
                          @Value("${app.licenses.local.enforcement.enabled:false}") boolean localEnforcementEnabled) {
        this.enforcementService = enforcementService;
        this.validationService = validationService;
        this.instalacionRepository = instalacionRepository;
        this.licenciaRepository = licenciaRepository;
        this.objectMapper = objectMapper;
        this.enforcementEnabled = enforcementEnabled;
        this.localEnforcementEnabled = localEnforcementEnabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if ((!enforcementEnabled && !localEnforcementEnabled)
                || "OPTIONS".equalsIgnoreCase(request.getMethod()) || esRutaPublica(request)) {
            chain.doFilter(request, response);
            return;
        }

        UUID empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) {
            chain.doFilter(request, response);
            return;
        }

        if (enforcementEnabled) {
            try {
                enforcementService.verificarSuscripcionVigente(empresaId);
            } catch (BusinessException exception) {
                escribirError(response, HttpServletResponse.SC_FORBIDDEN, exception.getCode(),
                        exception.getMessage(), request.getRequestURI());
                return;
            }
        }

        if (localEnforcementEnabled) {
            Optional<InstalacionLocal> instalacionOpt =
                    instalacionRepository.findTopByEmpresaIdOrderByActivadaEnDesc(empresaId);
            if (instalacionOpt.isPresent()) {
                UUID instalacionId = instalacionOpt.get().getId();
                if (licenciaVigente(instalacionId) != null) {
                    LicensingValidationService.ResultadoValidacion resultado =
                            validationService.validar(instalacionId, false);
                    if (resultado.modo() == LicensingValidationService.Modo.BLOQUEADO) {
                        escribirError(response, HttpServletResponse.SC_FORBIDDEN, resultado.motivo(),
                                resultado.detalle(), request.getRequestURI());
                        return;
                    }
                    if (resultado.modo() == LicensingValidationService.Modo.SOLO_LECTURA
                            && !esLectura(request.getMethod())) {
                        escribirError(response, HttpServletResponse.SC_FORBIDDEN, "MODO_SOLO_LECTURA",
                                "Licencia en periodo de gracia: solo operaciones de lectura disponibles",
                                request.getRequestURI());
                        return;
                    }
                }
            }
        }

        chain.doFilter(request, response);
    }

    private LicenciaLocal licenciaVigente(UUID instalacionId) {
        return licenciaRepository.findTopByInstalacionIdOrderByEmitidaEnDesc(instalacionId).orElse(null);
    }

    private boolean esLectura(String method) {
        return "GET".equalsIgnoreCase(method)
                || "HEAD".equalsIgnoreCase(method)
                || "OPTIONS".equalsIgnoreCase(method);
    }

    private void escribirError(HttpServletResponse response, int status, String code,
                               String message, String path) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(
                status,
                code,
                message,
                path,
                "INTERNA:LICENCIAMIENTO",
                Map.of("filtro", "LicenciaFilter")
        ));
    }

    private boolean esRutaPublica(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/")
                || path.startsWith("/api/licensing-local")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/api-docs")
                || path.equals("/error");
    }
}
