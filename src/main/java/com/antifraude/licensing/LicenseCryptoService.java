package com.antifraude.licensing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Criptografía de licenciamiento local: huella (SHA-256) y firma simétrica
 * HMAC-SHA256 de los leases. La clave de firma deriva de la huella de la
 * instalación + secreto de despliegue, de modo que un lease firmado sea
 * verificable localmente sin depender del control plane.
 */
@Service
public class LicenseCryptoService {

    static final String KID_FIRMA = "hmac-sha256-v1";

    private final byte[] secretKey;

    public LicenseCryptoService(@Value("${app.hmac.secret:${app.aes.secret}}") String secret) {
        if (!StringUtils.hasText(secret) || secret.length() < 32) {
            throw new IllegalArgumentException("Se requiere app.hmac.secret (al menos 32 caracteres) para firmar leases");
        }
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
    }

    public static String generarCorrelationId(UUID instalacionId) {
        return UUID.randomUUID() + "-" + instalacionId;
    }

    /** Huella SHA-256 hex de la identidad de máquina enviada por el instalador. */
    public String fingerprint(String identidadMaquina) {
        if (!StringUtils.hasText(identidadMaquina)) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] raw = digest.digest(identidadMaquina.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    /** Firma HMAC-SHA256 de un payload con clave derivada de la huella. */
    public String firmar(String payload, String fingerprintHash) {
        SecretKeySpec key = new SecretKeySpec(derivarClave(fingerprintHash), "HmacSHA256");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el lease", e);
        }
    }

    /** Verifica de forma constante-tiempo una firma HMAC-SHA256 de un payload. */
    public boolean verificar(String payload, String firmaHex, String fingerprintHash) {
        if (!StringUtils.hasText(firmaHex) || !StringUtils.hasText(fingerprintHash)) {
            return false;
        }
        String esperada = firmar(payload, fingerprintHash);
        if (esperada.length() != firmaHex.length()) {
            return false;
        }
        byte[] a = esperada.getBytes(StandardCharsets.UTF_8);
        byte[] b = firmaHex.getBytes(StandardCharsets.UTF_8);
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }

    private byte[] derivarClave(String fingerprintHash) {
        Mac mac;
        try {
            mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo derivar la clave de firma", e);
        }
        return mac.doFinal(fingerprintHash.getBytes(StandardCharsets.UTF_8));
    }
}