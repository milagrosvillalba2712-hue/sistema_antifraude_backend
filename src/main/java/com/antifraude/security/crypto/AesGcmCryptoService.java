package com.antifraude.security.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class AesGcmCryptoService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKeySpec keySpec;

    public AesGcmCryptoService(@Value("${app.aes.secret}") String secret) {
        if (!StringUtils.hasText(secret) || secret.length() < 32) {
            throw new IllegalArgumentException("app.aes.secret debe tener al menos 32 caracteres");
        }
        this.keySpec = new SecretKeySpec(sha256(secret), "AES");
    }

    public byte[] encryptToBytes(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

            return ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv)
                    .put(encrypted)
                    .array();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cifrar el valor sensible", e);
        }
    }

    public String decryptFromBytes(byte[] encryptedValue) {
        if (encryptedValue == null || encryptedValue.length == 0) {
            return null;
        }
        try {
            ByteBuffer buffer = ByteBuffer.wrap(encryptedValue);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo descifrar el valor sensible", e);
        }
    }

    public String encryptToBase64(String value) {
        byte[] encrypted = encryptToBytes(value);
        return encrypted == null ? null : Base64.getEncoder().encodeToString(encrypted);
    }

    public String decryptFromBase64(String encryptedValue) {
        if (!StringUtils.hasText(encryptedValue)) {
            return null;
        }
        return decryptFromBytes(Base64.getDecoder().decode(encryptedValue));
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo derivar la llave AES", e);
        }
    }
}
