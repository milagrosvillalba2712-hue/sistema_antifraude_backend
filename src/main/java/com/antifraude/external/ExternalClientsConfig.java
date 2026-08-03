package com.antifraude.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.security.KeyStore;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

@Configuration
public class ExternalClientsConfig {
    @Value("${app.external.tls.trust-store:}") private String trustStore;
    @Value("${app.external.tls.trust-store-password:}") private String trustStorePassword;
    @Bean("identificacionesRestClient")
    RestClient identities(@Value("${app.external.identificaciones.url}") String url,
                          @Value("${app.external.identificaciones.api-key}") String key) {
        return client(url, key);
    }

    @Bean("sancionesRestClient")
    RestClient sanctions(@Value("${app.external.sanciones.url}") String url,
                         @Value("${app.external.sanciones.api-key}") String key) {
        return client(url, key);
    }

    @Bean("pepRestClient")
    RestClient pep(@Value("${app.external.pep.url}") String url,
                   @Value("${app.external.pep.api-key}") String key) {
        return client(url, key);
    }

    private RestClient client(String url, String key) {
        HttpClient.Builder httpBuilder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2));
        if (url.startsWith("https://")) httpBuilder.sslContext(sslContext());
        HttpClient httpClient = httpBuilder.build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        // Tres intentos más backoff permanecen por debajo del límite total de cinco segundos.
        requestFactory.setReadTimeout(Duration.ofMillis(1_200));
        return RestClient.builder().baseUrl(url).requestFactory(requestFactory)
                .defaultHeader("X-API-Key", key).build();
    }

    private SSLContext sslContext() {
        if (trustStore.isBlank() || trustStorePassword.isBlank()) {
            throw new IllegalStateException("HTTPS externo requiere truststore académico explícito");
        }
        try (var input = new org.springframework.core.io.DefaultResourceLoader().getResource(trustStore).getInputStream()) {
            KeyStore store = KeyStore.getInstance("PKCS12");
            store.load(input, trustStorePassword.toCharArray());
            TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init(store);
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(null, factory.getTrustManagers(), null);
            return context;
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo cargar el truststore externo", exception);
        }
    }
}
