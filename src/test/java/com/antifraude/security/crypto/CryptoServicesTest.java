package com.antifraude.security.crypto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CryptoServicesTest {

    @Test
    void hmacNormalizaYGeneraHashDeterministico() {
        HmacHashService service = new HmacHashService("hmac_secret_key_default_change_in_production");

        String hashUno = service.hmacHex("  José   Pérez  ");
        String hashDos = service.hmacHex("JOSE PEREZ");

        assertThat(hashUno).isNotBlank();
        assertThat(hashUno).isEqualTo(hashDos);
    }

    @Test
    void aesGcmCifraYDescifraSinExponerTextoPlano() {
        AesGcmCryptoService service = new AesGcmCryptoService("aes_secret_key_default_change_in_production");

        byte[] encrypted = service.encryptToBytes("001-123456-9");
        String decrypted = service.decryptFromBytes(encrypted);

        assertThat(encrypted).isNotEmpty();
        assertThat(new String(encrypted)).doesNotContain("001-123456-9");
        assertThat(decrypted).isEqualTo("001-123456-9");
    }
}
