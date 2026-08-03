package com.antifraude.external;

public record ProviderResult<T>(String provider, T body, String correlationId, int statusHttp,
                                long durationMs, int attempts, boolean match) {
}
