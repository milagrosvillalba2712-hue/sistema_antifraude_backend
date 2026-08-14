package com.antifraude.licensing;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class LicenseCryptoServiceTest {

    @Test
    void verificaLeaseRs256ConJwksYRechazaPayloadAlterado() throws Exception {
        LicenseCryptoService service = new LicenseCryptoService("hmac_secret_key_default_change_in_production");
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String kid = "regula-test-rs256";
        String header = base64Url("{\"alg\":\"RS256\",\"kid\":\"regula-test-rs256\",\"typ\":\"REGULA-LEASE\"}");
        String payload = base64Url("{\"sub\":\"empresa-demo\",\"plan\":\"PREMIUM\"}");
        String signingInput = header + "." + payload;
        String signature = sign(signingInput, keyPair);
        String jwks = jwks(kid, (RSAPublicKey) keyPair.getPublic());

        assertThat(service.verificar(signingInput, signature, "fingerprint-demo", kid, jwks)).isTrue();
        assertThat(service.verificar(header + "." + base64Url("{\"sub\":\"otra\"}"), signature,
                "fingerprint-demo", kid, jwks)).isFalse();
    }

    private static String sign(String signingInput, KeyPair keyPair) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
    }

    private static String jwks(String kid, RSAPublicKey publicKey) {
        String n = Base64.getUrlEncoder().withoutPadding().encodeToString(unsigned(publicKey.getModulus().toByteArray()));
        String e = Base64.getUrlEncoder().withoutPadding().encodeToString(unsigned(publicKey.getPublicExponent().toByteArray()));
        return "{\"keys\":[{\"kty\":\"RSA\",\"use\":\"sig\",\"alg\":\"RS256\",\"kid\":\"" + kid
                + "\",\"n\":\"" + n + "\",\"e\":\"" + e + "\"}]}";
    }

    private static String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] unsigned(byte[] bytes) {
        if (bytes.length > 1 && bytes[0] == 0) {
            return java.util.Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return bytes;
    }
}
