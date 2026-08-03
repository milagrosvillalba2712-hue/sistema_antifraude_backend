package com.antifraude.external;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BcpSancionesClient {
    private final ProviderHttpClient http; private final RestClient client;
    public BcpSancionesClient(ProviderHttpClient http, @Qualifier("sancionesRestClient") RestClient client) {
        this.http=http; this.client=client;
    }
    @Retry(name="sanciones") @CircuitBreaker(name="sanciones") @Bulkhead(name="sanciones")
    public ProviderResult<SancionesResponse> consultar(String document) {
        return http.get(client, "BCP", "/api/v1/sanciones/{documento}", document,
                SancionesResponse.class, SancionesResponse::sancionado);
    }
}
