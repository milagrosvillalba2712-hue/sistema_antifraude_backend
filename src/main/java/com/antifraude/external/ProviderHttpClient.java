package com.antifraude.external;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.util.UUID;

@Component
class ProviderHttpClient {
    <T> ProviderResult<T> get(RestClient client, String provider, String path, String document,
                              Class<T> type, java.util.function.Predicate<T> match) {
        String correlation = UUID.randomUUID().toString();
        long start = System.nanoTime();
        try {
            T body = client.get()
                    .uri(builder -> builder.path(path).build(document))
                    .header("X-Correlation-Id", correlation)
                    .retrieve().body(type);
            return new ProviderResult<>(provider, body, correlation, 200, elapsed(start), 1, match.test(body));
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                throw new NonRetryableExternalException(provider, correlation, exception.getStatusCode().value(),
                        elapsed(start), "HTTP_NO_TRANSITORIO", exception);
            }
            throw failure(provider, correlation, exception.getStatusCode().value(), elapsed(start), "HTTP_TRANSITORIO", exception);
        } catch (Exception exception) {
            String category = hasCause(exception, SocketTimeoutException.class) ? "TIMEOUT" : "CONEXION_O_RESPUESTA";
            throw failure(provider, correlation, 0, elapsed(start), category, exception);
        }
    }

    private ExternalProviderException failure(String provider, String correlation, int status,
                                               long duration, String category, Throwable cause) {
        return new ExternalProviderException(provider, correlation, status, duration, category, cause);
    }

    private boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (type.isInstance(current)) return true;
        }
        return false;
    }

    private long elapsed(long start) { return (System.nanoTime() - start) / 1_000_000; }
}
