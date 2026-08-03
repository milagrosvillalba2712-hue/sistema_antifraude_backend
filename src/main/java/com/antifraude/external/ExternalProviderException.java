package com.antifraude.external;

public class ExternalProviderException extends RuntimeException {
    private final String provider;
    private final String correlationId;
    private final int statusHttp;
    private final long durationMs;
    private final String category;

    public ExternalProviderException(String provider, String correlationId, int statusHttp,
                                     long durationMs, String category, Throwable cause) {
        super("Fallo del proveedor " + provider + " (" + category + ")", cause);
        this.provider = provider;
        this.correlationId = correlationId;
        this.statusHttp = statusHttp;
        this.durationMs = durationMs;
        this.category = category;
    }

    public String provider() { return provider; }
    public String correlationId() { return correlationId; }
    public int statusHttp() { return statusHttp; }
    public long durationMs() { return durationMs; }
    public String category() { return category; }
}
