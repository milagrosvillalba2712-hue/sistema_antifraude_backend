package com.antifraude.auth;

import com.antifraude.exception.BusinessException;

/**
 * Politica de contrasena OWASP / NIST 800-63B: longitud 10-128 sin forzar
 * composicion y sin listas de reuso por ahora.
 */
public final class PasswordPolicy {

    private static final int MIN_LENGTH = 10;
    private static final int MAX_LENGTH = 128;

    private PasswordPolicy() {
    }

    public static void validar(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new BusinessException("PASSWORD_INVALIDO",
                    "La contrasena debe tener al menos " + MIN_LENGTH + " caracteres");
        }
        if (password.length() > MAX_LENGTH) {
            throw new BusinessException("PASSWORD_INVALIDO",
                    "La contrasena no debe exceder " + MAX_LENGTH + " caracteres");
        }
    }
}