package com.antifraude.licensing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

/**
 * Cliente opcional del control plane central. Cuando no hay URL configurada
 * devuelve {@code online=false} (modo offline); ante fallo de red tambien lo
 * hace, dejando que la politica de gracia local decida (ADR-002). Nunca
 * lanza hacia el flujo de licencia: la indisponibilidad es un dato, no un
 * bloqueo.
 */
@Component
public class LicensingControlPlaneClient {

    private static final Logger log = LoggerFactory.getLogger(LicensingControlPlaneClient.class);

    private final RestClient restClient;
    private final String apiKey;
    private final boolean habilitado;

    public LicensingControlPlaneClient(@Value("${app.licenses.control-plane.url:}") String url,
                                       @Value("${app.licenses.control-plane.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.habilitado = StringUtils.hasText(url);
        this.restClient = habilitado ? RestClient.builder().baseUrl(url).build() : null;
    }

    public RespuestaControlPlane validar(UUID instalacionId, String fingerprintHash) {
        if (!habilitado || restClient == null || !StringUtils.hasText(fingerprintHash)) {
            return RespuestaControlPlane.respuestaOffline();
        }
        try {
            restClient.post()
                    .uri("/api/v1/licencias/validar")
                    .header("X-API-Key", apiKey)
                    .body(Map.of("instalacionId", instalacionId.toString(), "fingerprintHash", fingerprintHash))
                    .retrieve()
                    .body(RespuestaControlPlane.class);
            return RespuestaControlPlane.respuestaOnline();
        } catch (RuntimeException exception) {
            log.info("[LICENCIA] Control plane indisponible - instalacion {} - {}", instalacionId,
                    exception.getClass().getSimpleName());
            return RespuestaControlPlane.respuestaOffline();
        }
    }

    public boolean habilitado() {
        return habilitado;
    }

    public record RespuestaControlPlane(boolean online, String estado, String motivo) {
        static RespuestaControlPlane respuestaOnline() {
            return new RespuestaControlPlane(true, "VALIDO", null);
        }

        static RespuestaControlPlane respuestaOffline() {
            return new RespuestaControlPlane(false, null, "SIN_CONECTIVIDAD");
        }
    }
}