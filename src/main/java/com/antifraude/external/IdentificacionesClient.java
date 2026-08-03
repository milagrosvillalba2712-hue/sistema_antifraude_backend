package com.antifraude.external;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class IdentificacionesClient {
    private final ProviderHttpClient http;
    private final RestClient client;
    public IdentificacionesClient(ProviderHttpClient http, @Qualifier("identificacionesRestClient") RestClient client) {
        this.http = http; this.client = client;
    }
    @Retry(name="identificaciones") @CircuitBreaker(name="identificaciones") @Bulkhead(name="identificaciones")
    public ProviderResult<IdentidadResponse> consultar(String document) {
        return http.get(client, "IDENTIFICACIONES", "/api/v1/identidades/{documento}", document,
                IdentidadResponse.class, IdentidadResponse::antecedentes);
    }
}
