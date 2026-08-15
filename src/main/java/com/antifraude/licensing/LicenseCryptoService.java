package com.antifraude.licensing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    /**
     * Verifica leases nuevos firmados por el Control Plane con RS256 y conserva
     * compatibilidad con el HMAC local usado en la demo previa.
     */
    public boolean verificar(String payload, String firma, String fingerprintHash, String kid, String jwksJson) {
        if (StringUtils.hasText(kid) && !"hmac-sha256-v1".equals(kid)) {
            return verificarRs256(payload, firma, kid, jwksJson);
        }
        return verificar(payload, firma, fingerprintHash);
    }

    private boolean verificarRs256(String signingInput, String firmaBase64Url, String kid, String jwksJson) {
        if (!StringUtils.hasText(signingInput) || !StringUtils.hasText(firmaBase64Url)
                || !StringUtils.hasText(kid) || !StringUtils.hasText(jwksJson)) {
            return false;
        }
        try {
            PublicKey publicKey = publicKeyFromJwks(kid, jwksJson);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getUrlDecoder().decode(firmaBase64Url));
        } catch (Exception e) {
            return false;
        }
    }

    private PublicKey publicKeyFromJwks(String kid, String jwksJson) throws Exception {
        JsonNode keys = objectMapper.readTree(jwksJson).path("keys");
        for (JsonNode key : keys) {
            if (kid.equals(key.path("kid").asText()) && "RSA".equals(key.path("kty").asText())) {
                BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(key.path("n").asText()));
                BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(key.path("e").asText()));
                return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
            }
        }
        throw new IllegalArgumentException("No existe clave publica para kid " + kid);
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
