package com.antifraude.auth;

import com.antifraude.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Emision y consumo de tokens de un solo uso. El token en claro solo existira en la
 * respuesta de creacion / en el enlace enviado; en BD queda exclusivamente su SHA-256.
 */
@Service
public class AuthTokenService {

    private static final Logger log = LoggerFactory.getLogger(AuthTokenService.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthTokenRepository authTokenRepository;

    public AuthTokenService(AuthTokenRepository authTokenRepository) {
        this.authTokenRepository = authTokenRepository;
    }

    public String crearInvitacion(UUID empresaId, Long rolId, String email, int minutosVida) {
        return emitir(null, AuthToken.TIPO_INVITACION, empresaId, rolId, email, minutosVida);
    }

    public String crearVerificacion(UUID usuarioId, String email, int minutosVida) {
        return emitir(usuarioId, AuthToken.TIPO_VERIFICACION, null, null, email, minutosVida);
    }

    public String crearRecuperacion(UUID usuarioId, String email, int minutosVida) {
        return emitir(usuarioId, AuthToken.TIPO_RESET, null, null, email, minutosVida);
    }

    @Transactional
    public AuthToken consumir(String tipo, String codigo) {
        String hash = hash(codigo);
        AuthToken token = authTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException("TOKEN_NO_VALIDO",
                        "El enlace/codigo no es valido o ya fue utilizado"));
        if (!tipo.equals(token.getTipo())) {
            throw new BusinessException("TOKEN_NO_VALIDO",
                    "El enlace/codigo no es valido o ya fue utilizado");
        }
        if (!token.esValido()) {
            throw new BusinessException("TOKEN_NO_VALIDO",
                    "El enlace/codigo ha expirado o ya fue utilizado");
        }
        token.setUsadoEn(OffsetDateTime.now());
        authTokenRepository.save(token);
        log.info("[AUTH-TOKEN] Consumido token tipo {} - usuario: {}", tipo, token.getUsuarioId());
        return token;
    }

    @Transactional
    public void revocarPorUsuario(UUID usuarioId, String tipo) {
        var tokensVivos = authTokenRepository.findAll().stream()
                .filter(t -> usuarioId.equals(t.getUsuarioId()) && tipo.equals(t.getTipo()))
                .filter(t -> t.getUsadoEn() == null)
                .toList();
        tokensVivos.forEach(token -> {
            token.setRevocado(true);
            authTokenRepository.save(token);
        });
    }

    public static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el hash del token", e);
        }
    }

    /** Busca un token por tipo y codigo sin consumirlo (para validacion publica). */
    public AuthToken buscarPorTipoYCodigo(String tipo, String codigo) {
        String hashValue = hash(codigo);
        return authTokenRepository.findByTokenHash(hashValue)
                .filter(t -> tipo.equals(t.getTipo()))
                .orElse(null);
    }

    private String emitir(UUID usuarioId, String tipo, UUID empresaId, Long rolId, String email, int minutosVida) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        StringBuilder token = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            token.append(String.format("%02x", b));
        }
        String tokenClaro = token.toString();
        AuthToken authToken = AuthToken.builder()
                .tipo(tipo)
                .tokenHash(hash(tokenClaro))
                .usuarioId(usuarioId)
                .empresaId(empresaId)
                .rolId(rolId)
                .email(email)
                .expiraEn(OffsetDateTime.now().plusMinutes(minutosVida))
                .build();
        authTokenRepository.save(authToken);
        log.info("[AUTH-TOKEN] Emitido token tipo {} - usuario: {} - expira: {}",
                tipo, usuarioId, authToken.getExpiraEn());
        return tokenClaro;
    }
}