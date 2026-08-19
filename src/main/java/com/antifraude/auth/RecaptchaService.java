package com.antifraude.auth;

import com.antifraude.exception.BusinessException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Verificacion de Google reCAPTCHA v3. En modo demo (sin secret key configurada),
 * se salta la verificacion para permitir pruebas locales.
 */
@Service
public class RecaptchaService {

    private static final Logger log = LoggerFactory.getLogger(RecaptchaService.class);
    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final double DEFAULT_THRESHOLD = 0.5;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${recaptcha.secret-key:}")
    private String secretKey;

    @Value("${recaptcha.threshold:0.5}")
    private double threshold;

    /**
     * Verifica el token de reCAPTCHA v3 contra Google.
     * Si no hay secret key configurada (modo demo), siempre retorna true.
     */
    public boolean verificar(String token, String expectedAction) {
        if (secretKey == null || secretKey.isBlank()) {
            log.debug("[RECAPTCHA] Modo demo: sin secret key, verificacion saltada");
            return true;
        }

        if (token == null || token.isBlank()) {
            throw new BusinessException("CAPTCHA_REQUERIDO",
                    "La verificacion de captcha es obligatoria");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // MultiValueMap para que RestTemplate codifique correctamente el token
            // (los tokens de reCAPTCHA contienen +, /, = que rompen un body armado a mano)
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("secret", secretKey);
            form.add("response", token);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(VERIFY_URL, request, String.class);

            if (response.getBody() == null) {
                log.error("[RECAPTCHA] Respuesta vacia de Google");
                throw new BusinessException("CAPTCHA_ERROR",
                        "Error al verificar el captcha. Intenta nuevamente");
            }

            RecaptchaResponse recaptcha = objectMapper.readValue(response.getBody(), RecaptchaResponse.class);

            if (!recaptcha.success()) {
                log.warn("[RECAPTCHA] Verificacion fallida: {}", recaptcha.errorCodes());
                throw new BusinessException("CAPTCHA_FALLIDO",
                        "La verificacion del captcha fallo. Intenta nuevamente");
            }

            if (recaptcha.action() != null) {
                // Respuesta v3: validar action y score
                if (expectedAction != null && !expectedAction.equals(recaptcha.action())) {
                    log.warn("[RECAPTCHA] Action mismatch: esperado={}, recibido={}", expectedAction, recaptcha.action());
                    throw new BusinessException("CAPTCHA_FALLIDO",
                            "Error de verificacion del captcha");
                }
                if (recaptcha.score() < threshold) {
                    log.warn("[RECAPTCHA] Score bajo: {} < {}", recaptcha.score(), threshold);
                    throw new BusinessException("CAPTCHA_FALLIDO",
                            "La verificacion del captcha fallo. Intenta nuevamente");
                }
            }
            // Respuesta v2 (checkbox): sin action/score, alcanza con success=true

            log.debug("[RECAPTCHA] Verificacion exitosa: score={}, action={}", recaptcha.score(), recaptcha.action());
            return true;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[RECAPTCHA] Error al verificar: {}", e.getMessage());
            throw new BusinessException("CAPTCHA_ERROR",
                    "Error al verificar el captcha. Intenta nuevamente");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RecaptchaResponse(
            boolean success,
            double score,
            String action,
            String challenge_ts,
            String hostname,
            @JsonProperty("error-codes") List<String> errorCodes
    ) {
    }
}
