package com.antifraude.external;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
class ProviderHttpClient {
    <T> ProviderResult<T> get(RestClient client, String provider, String path, String document,
                              Class<T> type, java.util.function.Predicate<T> match) {
        String correlation = UUID.randomUUID().toString();
        long logicalStart = System.nanoTime();
        ExternalProviderException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                T body = client.get().uri(builder -> builder.path(path).build(document))
                        .header("X-Correlation-Id", correlation).retrieve().body(type);
                return new ProviderResult<>(provider, body, correlation, 200,
                        elapsed(logicalStart), attempt, match.test(body));
            } catch (RestClientResponseException exception) {
                if (exception.getStatusCode().is4xxClientError()) {
                    throw new NonRetryableExternalException(provider, correlation, exception.getStatusCode().value(),
                            elapsed(logicalStart), "HTTP_NO_TRANSITORIO", exception, attempt);
                }
                last = new ExternalProviderException(provider, correlation, exception.getStatusCode().value(),
                        elapsed(logicalStart), "HTTP_TRANSITORIO", exception, attempt);
            } catch (Exception exception) {
                String category = hasCause(exception, SocketTimeoutException.class) ? "TIMEOUT" : "CONEXION_O_RESPUESTA";
                last = new ExternalProviderException(provider, correlation, 0, elapsed(logicalStart), category, exception, attempt);
            }
            if (attempt < 3) backoff(attempt);
        }
        throw last;
    }

    private void backoff(int attempt) {
        long base = 250L * (1L << (attempt - 1));
        long jitter = ThreadLocalRandom.current().nextLong(-base / 4, base / 4 + 1);
        try {
            Thread.sleep(base + jitter);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrumpido", exception);
        }
    }

    private boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (type.isInstance(current)) return true;
        }
        return false;
    }

    private long elapsed(long start) { return (System.nanoTime() - start) / 1_000_000; }
}
