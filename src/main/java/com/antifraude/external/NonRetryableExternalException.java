package com.antifraude.external;

public class NonRetryableExternalException extends ExternalProviderException {
    public NonRetryableExternalException(String provider, String correlationId, int statusHttp,
                                         long durationMs, String category, Throwable cause) {
        super(provider, correlationId, statusHttp, durationMs, category, cause);
    }
}
