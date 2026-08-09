package com.antifraude.security.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HexFormat;

@Service
public class HmacHashService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] key;

    public HmacHashService(@Value("${app.hmac.secret:${app.aes.secret}}") String secret) {
        if (!StringUtils.hasText(secret) || secret.length() < 32) {
            throw new IllegalArgumentException("app.hmac.secret debe tener al menos 32 caracteres");
        }
        this.key = secret.getBytes(StandardCharsets.UTF_8);
    }

    public byte[] hmacBytes(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            return mac.doFinal(normalize(value).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular HMAC-SHA256", e);
        }
    }

    public String hmacHex(String value) {
        byte[] hash = hmacBytes(value);
        return hash == null ? null : HexFormat.of().formatHex(hash);
    }

    public String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().toUpperCase();
        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.replaceAll("\\s+", " ");
    }
}
