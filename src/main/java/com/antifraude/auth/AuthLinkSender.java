package com.antifraude.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Envio de enlaces de verificacion/recuperacion. En ausencia de infraestructura SMTP
 * (demo) imprime el enlace en consola con el perfil activo; la implementacion real
 * solo deberia sustituir el cuerpo de este servicio.
 */
@Service
public class AuthLinkSender {

    private static final Logger log = LoggerFactory.getLogger(AuthLinkSender.class);

    private final String linkBaseUrl;

    public AuthLinkSender(@Value("${app.auth.link-base-url:http://localhost:5173}") String linkBaseUrl) {
        this.linkBaseUrl = linkBaseUrl;
    }

    public void enviarVerificacion(String email, String codigo) {
        String url = linkBaseUrl + "/verify-email?codigo=" + codigo;
        log.warn("[MAIL-MOCK] Para {} -> verifica tu email en: {}", email, url);
    }

    public void enviarRecuperacion(String email, String codigo) {
        String url = linkBaseUrl + "/reset-password?codigo=" + codigo;
        log.warn("[MAIL-MOCK] Para {} -> restablece tu contrasena en: {}", email, url);
    }
}