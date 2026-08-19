package com.antifraude.auth;

import com.antifraude.exception.BusinessException;

import java.util.Set;

/**
 * Politica de contrasena corporativa: composicion obligatoria + bloqueo de
 * contrasenas comunes y predecibles. Alineada con OWASP Authentication Cheat Sheet
 * y balanceada con usabilidad para el contexto Paraguay.
 */
public final class PasswordPolicy {

    private static final int MIN_LENGTH = 12;
    private static final int MAX_LENGTH = 128;

    private static final Set<String> CONTRASENAS_COMUNES = Set.of(
            "password", "password1", "password12", "password123", "password1234",
            "contraseña", "contraseña1", "contraseña12", "contraseña123",
            "123456", "12345678", "123456789", "1234567890",
            "qwerty", "qwerty123", "qwerty1234",
            "abc123", "abcdef", "abcdefg",
            "admin", "admin123", "admin1234", "administrator",
            "letmein", "welcome", "monkey", "dragon",
            "master", "login", "changeme", "shadow",
            "samsung", "sunshine", "princess", "football",
            "baseball", "soccer", "hockey", "batman",
            "access", "hello", "charlie", "donald",
            "passw0rd", "p@ssw0rd", "p@ssword", "p@ssword1",
            "iloveyou", "trustno1", "summer", "winter",
            "spring", "autumn", "michael", "jennifer",
            "thomas", "jordan", "superman", "harley",
            "ranger", "buster", "thunder", "ginger",
            "hammer", "silver", "phoenix", "camaro",
            "secret", "internet", "computer", "whatever",
            "ninja", "mustang", "jesus", "pepper",
            "zxcvbn", "zaq1zaq1", "asd123", "qwe123",
            "loveyou", "babygirl", "maggie", "joshua",
            "andrea", "nicole", "daniel", "jessica",
            "madison", "ashley", "samantha", "brittany",
            "regula2026", "regula123", "santaclara",
            "financiera", "antifraude"
    );

    private PasswordPolicy() {
    }

    /**
     * Valida la contrasena segun la politica corporativa.
     * @param password la contrasena a validar (texto plano)
     * @param contexto opcional: email o nombre del usuario para evitar contrasenas contextuales
     */
    public static void validar(String password, String... contexto) {
        if (password == null) {
            throw new BusinessException("PASSWORD_INVALIDO",
                    "La contrasena no puede estar vacia");
        }

        if (password.length() < MIN_LENGTH) {
            throw new BusinessException("PASSWORD_INVALIDO",
                    "La contrasena debe tener al menos " + MIN_LENGTH + " caracteres");
        }

        if (password.length() > MAX_LENGTH) {
            throw new BusinessException("PASSWORD_INVALIDO",
                    "La contrasena no debe exceder " + MAX_LENGTH + " caracteres");
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new BusinessException("PASSWORD_INVALIDO",
                    "La contrasena debe contener al menos una letra mayuscula");
        }

        if (!password.matches(".*[a-z].*")) {
            throw new BusinessException("PASSWORD_INVALIDO",
                    "La contrasena debe contener al menos una letra minuscula");
        }

        if (!password.matches(".*\\d.*")) {
            throw new BusinessException("PASSWORD_INVALIDO",
                    "La contrasena debe contener al menos un numero");
        }

        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?].*")) {
            throw new BusinessException("PASSWORD_INVALIDO",
                    "La contrasena debe contener al menos un caracter especial (!@#$%^&*()_+-=[]{}|;:,.<>?)");
        }

        String lower = password.toLowerCase();
        for (String comun : CONTRASENAS_COMUNES) {
            if (lower.contains(comun)) {
                throw new BusinessException("PASSWORD_INVALIDO",
                        "La contrasena es demasiado comun o predecible. Elige una contrasena mas segura");
            }
        }

        if (contexto != null) {
            for (String ctx : contexto) {
                if (ctx != null && !ctx.isEmpty() && lower.contains(ctx.toLowerCase())) {
                    throw new BusinessException("PASSWORD_INVALIDO",
                            "La contrasena no debe contener tu correo o nombre de usuario");
                }
            }
        }
    }

    /** Overload sin contexto para compatibilidad con codigo existente. */
    public static void validar(String password) {
        validar(password, (String[]) null);
    }
}