package com.antifraude.auth;

import com.antifraude.exception.QuotaExceededException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting en memoria por IP para rutas sensibles de auth (login, registro,
 * recuperacion). Ventana deslizante; ante exceso responde 429. No reemplaza al
 * lockout por cuenta (5 intentos / 15 min) que vive en la entidad Usuario.
 */
@Service
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_SECONDS = 900;

    private final Map<String, Deque<Long>> intentosPorIp = new ConcurrentHashMap<>();

    public void verificar(String ip) {
        String clave = ip == null || ip.isBlank() ? "desconocida" : ip;
        long ahora = Instant.now().getEpochSecond();
        long corte = ahora - WINDOW_SECONDS;

        Deque<Long> marca = intentosPorIp.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (marca) {
            while (!marca.isEmpty() && marca.peekFirst() < corte) {
                marca.pollFirst();
            }
            if (marca.size() >= MAX_ATTEMPTS) {
                throw new QuotaExceededException("RATE_LIMIT_AUTH",
                        "Demasiados intentos. Reintenta en unos minutos");
            }
            marca.addLast(ahora);
        }
    }
}