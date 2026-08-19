package com.antifraude.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contrato de deserializacion de la respuesta de Google siteverify.
 * Google devuelve "error-codes" (con guion) y en v2 no incluye score/action.
 */
class RecaptchaResponseTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void respuestaV2ExitosaSinScoreNiAction() throws Exception {
        var r = om.readValue(
                "{\"success\":true,\"challenge_ts\":\"2026-08-18T23:00:00Z\",\"hostname\":\"localhost\"}",
                RecaptchaService.RecaptchaResponse.class);

        assertTrue(r.success());
        assertNull(r.action()); // v2: sin action -> no se validan action/score
        assertEquals(0.0, r.score());
    }

    @Test
    void respuestaFallidaConErrorCodesConGuion() throws Exception {
        var r = om.readValue(
                "{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}",
                RecaptchaService.RecaptchaResponse.class);

        assertFalse(r.success());
        assertEquals(1, r.errorCodes().size());
        assertEquals("invalid-input-response", r.errorCodes().get(0));
    }

    @Test
    void respuestaV3IncluyeScoreYAction() throws Exception {
        var r = om.readValue(
                "{\"success\":true,\"score\":0.9,\"action\":\"registro\",\"challenge_ts\":\"t\",\"hostname\":\"h\"}",
                RecaptchaService.RecaptchaResponse.class);

        assertTrue(r.success());
        assertEquals(0.9, r.score());
        assertEquals("registro", r.action());
    }
}
