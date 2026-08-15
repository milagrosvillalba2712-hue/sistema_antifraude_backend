package com.antifraude.external;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SepreladPepClient {
    private final ProviderHttpClient http; private final RestClient client;
    public SepreladPepClient(ProviderHttpClient http, @Qualifier("pepRestClient") RestClient client) {
        this.http=http; this.client=client;
    }
    @CircuitBreaker(name="pep") @Bulkhead(name="pep")
    public ProviderResult<PepResponse> consultar(String document) {
        return http.get(client, "SEPRELAD", "/api/v1/personas-expuestas/{documento}", document,
                PepResponse.class, PepResponse::pep);
    }
}
