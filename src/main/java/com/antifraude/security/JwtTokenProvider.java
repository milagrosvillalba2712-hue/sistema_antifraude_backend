package com.antifraude.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email, String rol) {
        return generateToken(email, null, rol, null, null, List.of());
    }

    public String generateToken(String email, UUID usuarioId, String rol, UUID empresaId, Long rolId, List<String> permisos) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(email)
                .claim("usuarioId", usuarioId != null ? usuarioId.toString() : null)
                .claim("rol", rol)
                .claim("empresaId", empresaId != null ? empresaId.toString() : null)
                .claim("rolId", rolId)
                .claim("permisos", permisos)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public Optional<UUID> getUuidClaim(String token, String claimName) {
        try {
            Object value = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get(claimName);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(UUID.fromString(String.valueOf(value)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
