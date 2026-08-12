package com.antifraude.external;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class ExternalInvestigationClient {
    private final ProviderHttpClient http;
    private final RestClient client;

    public ExternalInvestigationClient(ProviderHttpClient http,
                                       @Qualifier("identificacionesRestClient") RestClient client) {
        this.http = http;
        this.client = client;
    }

    @SuppressWarnings("unchecked")
    public ProviderResult<Map<String, Object>> consultarPerfil(String documentoSeguro) {
        return (ProviderResult<Map<String, Object>>) (ProviderResult<?>) http.get(client, "KYC_PERFIL",
                "/api/v1/clientes/{documento}/perfil", documentoSeguro, Map.class, body -> true);
    }

    @SuppressWarnings("unchecked")
    public ProviderResult<Map<String, Object>> consultarDocumentos(String documentoSeguro) {
        return (ProviderResult<Map<String, Object>>) (ProviderResult<?>) http.get(client, "KYC_DOCUMENTOS",
                "/api/v1/clientes/{documento}/documentos", documentoSeguro, Map.class, body -> true);
    }

    @SuppressWarnings("unchecked")
    public ProviderResult<Map<String, Object>> consultarHistorial(String documentoSeguro, int limite) {
        int boundedLimit = Math.max(1, Math.min(limite, 50));
        return (ProviderResult<Map<String, Object>>) (ProviderResult<?>) http.get(client, "CORE_HISTORIAL",
                builder -> builder.path("/api/v1/clientes/{documento}/historial-transaccional")
                        .queryParam("limit", boundedLimit)
                        .build(documentoSeguro),
                Map.class, body -> true);
    }

    @SuppressWarnings("unchecked")
    public ProviderResult<Map<String, Object>> consultarScreening(String documentoSeguro) {
        return (ProviderResult<Map<String, Object>>) (ProviderResult<?>) http.get(client, "SCREENING_LISTAS",
                "/api/v1/screening-listas/{documento}", documentoSeguro, Map.class, body -> true);
    }

    @SuppressWarnings("unchecked")
    public ProviderResult<Map<String, Object>> consultarRiesgoPais(String codigoIso) {
        return (ProviderResult<Map<String, Object>>) (ProviderResult<?>) http.get(client, "RIESGO_PAIS",
                "/api/v1/riesgo-pais/{codigoIso}", codigoIso, Map.class, body -> true);
    }

    @SuppressWarnings("unchecked")
    public ProviderResult<Map<String, Object>> consultarBeneficiarioFinal(String rucSeguro) {
        return (ProviderResult<Map<String, Object>>) (ProviderResult<?>) http.get(client, "BENEFICIARIO_FINAL",
                "/api/v1/beneficiario-final/{ruc}", rucSeguro, Map.class, body -> true);
    }

    @SuppressWarnings("unchecked")
    public ProviderResult<Map<String, Object>> consultarEstadoProveedores() {
        return (ProviderResult<Map<String, Object>>) (ProviderResult<?>) http.get(client, "ESTADO_PROVEEDORES",
                builder -> builder.path("/api/v1/proveedores/estado").build(),
                Map.class, body -> true);
    }
}
