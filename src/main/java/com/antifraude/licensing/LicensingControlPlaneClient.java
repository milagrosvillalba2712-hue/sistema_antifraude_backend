package com.antifraude.licensing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.math.BigDecimal;

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
    private final ObjectMapper objectMapper;

    public LicensingControlPlaneClient(@Value("${app.licenses.control-plane.url:${LICENSES_CONTROL_PLANE_URL:}}") String url,
                                       @Value("${app.licenses.control-plane.api-key:${LICENSES_CONTROL_PLANE_API_KEY:}}") String apiKey,
                                       ObjectMapper objectMapper,
                                       RestClient.Builder builder) {
        this.apiKey = apiKey;
        this.habilitado = StringUtils.hasText(url);
        this.restClient = habilitado ? builder.baseUrl(url).build() : null;
        this.objectMapper = objectMapper;
    }

    public RespuestaControlPlane validar(UUID instalacionId, String fingerprintHash) {
        Map<String, Object> response = validarLease(instalacionId, fingerprintHash);
        if (Boolean.TRUE.equals(response.get("online"))) {
            return RespuestaControlPlane.respuestaOnline(String.valueOf(response.getOrDefault("estado", "VALIDO")));
        }
        return RespuestaControlPlane.respuestaOffline();
    }

    public Map<String, Object> validarLease(UUID instalacionId, String fingerprintHash) {
        if (!habilitado || restClient == null || !StringUtils.hasText(fingerprintHash)) {
            return offlinePayload("VALIDAR_LICENCIA");
        }
        try {
            Map<?, ?> response = restClient.post()
                    .uri("/api/v1/licencias/validar")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Key", apiKey)
                    .body(Map.of("instalacionId", instalacionId.toString(), "fingerprintHash", fingerprintHash))
                    .retrieve()
                    .body(Map.class);
            return sanitizeMap(response);
        } catch (RuntimeException exception) {
            log.info("[LICENCIA] Control plane indisponible - instalacion {} - {}", instalacionId,
                    exception.getClass().getSimpleName());
            return offlinePayload("VALIDAR_LICENCIA");
        }
    }

    public boolean habilitado() {
        return habilitado;
    }

    public Map<String, Object> catalogManifest() {
        if (!habilitado || restClient == null) {
            return offlinePayload("CATALOG_MANIFEST");
        }
        try {
            Map<?, ?> response = restClient.get()
                    .uri("/api/v1/catalogs/manifest")
                    .header("X-API-Key", apiKey)
                    .retrieve()
                    .body(Map.class);
            return sanitizeMap(response);
        } catch (RuntimeException exception) {
            log.info("[LICENCIA] No se pudo obtener manifest de catalogos - {}", exception.getClass().getSimpleName());
            return offlinePayload("CATALOG_MANIFEST");
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> catalogManifestCatalogs() {
        Map<String, Object> manifest = catalogManifest();
        if (!Boolean.TRUE.equals(manifest.get("online"))) {
            return List.of();
        }
        Object catalogs = manifest.get("catalogs");
        if (catalogs instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> map) {
                    result.add((Map<String, Object>) map);
                }
            }
            return result;
        }
        return List.of();
    }

    /**
     * Descarga el contenido de una version de catalogo del Control Plane.
     * El Control Plane devuelve {@code itemsJson} como String; aqui se parsea
     * a una lista de items. Devuelve un mapa con {@code online}, {@code code},
     * {@code version}, {@code hash} y {@code items} (lista de maps).
     */
    public Map<String, Object> catalogVersion(String code, String version) {
        if (!habilitado || restClient == null || !StringUtils.hasText(code) || !StringUtils.hasText(version)) {
            return offlinePayload("CATALOG_VERSION");
        }
        try {
            Map<?, ?> response = restClient.get()
                    .uri("/api/v1/catalogs/{code}/versions/{version}", code, version)
                    .header("X-API-Key", apiKey)
                    .retrieve()
                    .body(Map.class);
            Map<String, Object> result = sanitizeMap(response);
            String itemsJson = String.valueOf(result.getOrDefault("itemsJson", "[]"));
            result.put("items", parseItems(itemsJson));
            return result;
        } catch (RuntimeException exception) {
            log.info("[LICENCIA] No se pudo descargar catalogo {}/{} - {}", code, version, exception.getClass().getSimpleName());
            return offlinePayload("CATALOG_VERSION");
        }
    }

    private List<Map<String, Object>> parseItems(String itemsJson) {
        if (!StringUtils.hasText(itemsJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(itemsJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception exception) {
            log.warn("[LICENCIA] itemsJson invalido: {}", exception.getMessage());
            return List.of();
        }
    }

    public Map<String, Object> jwks() {
        if (!habilitado || restClient == null) {
            return offlinePayload("JWKS");
        }
        try {
            Map<?, ?> response = restClient.get()
                    .uri("/api/v1/licencias/jwks")
                    .header("X-API-Key", apiKey)
                    .retrieve()
                    .body(Map.class);
            return sanitizeMap(response);
        } catch (RuntimeException exception) {
            log.info("[LICENCIA] No se pudo obtener JWKS de licencias - {}", exception.getClass().getSimpleName());
            return offlinePayload("JWKS");
        }
    }

    public Map<String, Object> configurationPackage() {
        if (!habilitado || restClient == null) {
            return offlinePayload("CONFIGURATION_PACKAGE");
        }
        try {
            Map<?, ?> response = restClient.get()
                    .uri("/api/v1/configuration/package")
                    .header("X-API-Key", apiKey)
                    .retrieve()
                    .body(Map.class);
            return sanitizeMap(response);
        } catch (RuntimeException exception) {
            log.info("[LICENCIA] No se pudo obtener paquete de configuracion - {}", exception.getClass().getSimpleName());
            return offlinePayload("CONFIGURATION_PACKAGE");
        }
    }

    public Map<String, Object> reportHeartbeat(UUID instalacionId) {
        if (!habilitado || restClient == null || instalacionId == null) {
            return offlinePayload("HEARTBEAT");
        }
        try {
            Map<?, ?> response = restClient.post()
                    .uri("/api/v1/telemetry/heartbeat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Key", apiKey)
                    .body(Map.of("instalacionId", instalacionId.toString()))
                    .retrieve()
                    .body(Map.class);
            return sanitizeMap(response);
        } catch (RuntimeException exception) {
            log.info("[LICENCIA] Heartbeat hacia Control Plane no disponible - {}", exception.getClass().getSimpleName());
            return offlinePayload("HEARTBEAT");
        }
    }

    public Map<String, Object> reportUsage(UUID instalacionId, Map<String, Object> usage) {
        if (!habilitado || restClient == null || instalacionId == null) {
            return offlinePayload("USAGE");
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>(usage != null ? usage : Map.of());
            payload.put("instalacionId", instalacionId.toString());
            Map<?, ?> response = restClient.post()
                    .uri("/api/v1/telemetry/usage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Key", apiKey)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
            return sanitizeMap(response);
        } catch (RuntimeException exception) {
            log.info("[LICENCIA] Reporte de uso hacia Control Plane no disponible - {}", exception.getClass().getSimpleName());
            return offlinePayload("USAGE");
        }
    }

    public Map<String, Object> createStripeCheckout(UUID empresaId, Long suscripcionId, String successUrl, String cancelUrl) {
        if (!habilitado || restClient == null || empresaId == null) {
            return offlinePayload("STRIPE_CHECKOUT");
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("empresaId", empresaId.toString());
            if (suscripcionId != null) {
                payload.put("suscripcionId", suscripcionId);
            }
            if (StringUtils.hasText(successUrl)) {
                payload.put("successUrl", successUrl);
            }
            if (StringUtils.hasText(cancelUrl)) {
                payload.put("cancelUrl", cancelUrl);
            }
            log.info("[LICENCIA] Creando checkout Stripe - URL={}/api/v1/billing/checkout-session, apiKey={}, payload={}",
                    restClient != null ? "configured" : "null", apiKey, payload);
            Map<?, ?> response = restClient.post()
                    .uri("/api/v1/billing/checkout-session")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Key", apiKey)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
            log.info("[LICENCIA] Checkout Stripe respuesta: {}", response);
            return sanitizeMap(response);
        } catch (RuntimeException exception) {
            log.error("[LICENCIA] No se pudo crear Checkout Stripe - {} - message: {}", exception.getClass().getSimpleName(), exception.getMessage(), exception);
            return offlinePayload("STRIPE_CHECKOUT");
        }
    }

    public Map<String, Object> createStripeOneTimeCheckout(UUID empresaId,
                                                           BigDecimal monto,
                                                           String moneda,
                                                           String concepto,
                                                           String referenciaExterna,
                                                           Map<String, Object> metadata,
                                                           String successUrl,
                                                           String cancelUrl) {
        if (!habilitado || restClient == null || empresaId == null) {
            return offlinePayload("STRIPE_CHECKOUT_ONE_TIME");
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("empresaId", empresaId.toString());
            payload.put("tipo", "USUARIOS_ADICIONALES");
            payload.put("monto", monto);
            payload.put("moneda", moneda);
            payload.put("concepto", concepto);
            payload.put("referenciaExterna", referenciaExterna);
            payload.put("metadata", metadata != null ? metadata : Map.of());
            if (StringUtils.hasText(successUrl)) {
                payload.put("successUrl", successUrl);
            }
            if (StringUtils.hasText(cancelUrl)) {
                payload.put("cancelUrl", cancelUrl);
            }
            Map<?, ?> response = restClient.post()
                    .uri("/api/v1/billing/checkout-session")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Key", apiKey)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
            return sanitizeMap(response);
        } catch (RuntimeException exception) {
            log.info("[LICENCIA] No se pudo crear Checkout Stripe one-time - {}", exception.getClass().getSimpleName());
            return offlinePayload("STRIPE_CHECKOUT_ONE_TIME");
        }
    }

    public Map<String, Object> getStripeCheckoutSession(String sessionId) {
        if (!habilitado || restClient == null || !StringUtils.hasText(sessionId)) {
            return offlinePayload("STRIPE_CHECKOUT_STATUS");
        }
        try {
            Map<?, ?> response = restClient.get()
                    .uri("/api/v1/billing/checkout-session/{sessionId}", sessionId)
                    .header("X-API-Key", apiKey)
                    .retrieve()
                    .body(Map.class);
            return sanitizeMap(response);
        } catch (RuntimeException exception) {
            log.info("[LICENCIA] No se pudo consultar Checkout Stripe - {}", exception.getClass().getSimpleName());
            return offlinePayload("STRIPE_CHECKOUT_STATUS");
        }
    }

    public Map<String, Object> receipts(UUID empresaId) {
        if (!habilitado || restClient == null || empresaId == null) {
            return offlinePayload("RECEIPTS");
        }
        try {
            Object response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/admin/receipts")
                            .queryParam("empresaId", empresaId.toString())
                            .build())
                    .header("X-API-Key", apiKey)
                    .retrieve()
                    .body(Object.class);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("online", true);
            result.put("items", response);
            return result;
        } catch (RuntimeException exception) {
            log.info("[LICENCIA] No se pudieron obtener recibos - {}", exception.getClass().getSimpleName());
            return offlinePayload("RECEIPTS");
        }
    }

    private Map<String, Object> offlinePayload(String operation) {
        return Map.of(
                "online", false,
                "operation", operation,
                "estado", "SIN_CONECTIVIDAD",
                "mensaje", "Control Plane no configurado o no disponible"
        );
    }

    private Map<String, Object> sanitizeMap(Map<?, ?> response) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (response == null) {
            return result;
        }
        response.forEach((key, value) -> result.put(String.valueOf(key), value));
        result.putIfAbsent("online", true);
        return result;
    }

    public record RespuestaControlPlane(boolean online, String estado, String motivo) {
        static RespuestaControlPlane respuestaOnline(String estado) {
            return new RespuestaControlPlane(true, estado, null);
        }

        static RespuestaControlPlane respuestaOffline() {
            return new RespuestaControlPlane(false, null, "SIN_CONECTIVIDAD");
        }
    }
}
