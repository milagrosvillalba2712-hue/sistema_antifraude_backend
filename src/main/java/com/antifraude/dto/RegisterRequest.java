package com.antifraude.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank String nombre,
        @NotBlank String password,
        @NotBlank String codigoInvitacion,
        @AssertTrue(message = "Debe aceptar los Terminos y Condiciones") boolean aceptoTerminos,
        @AssertTrue(message = "Debe aceptar la Politica de Privacidad") boolean aceptoPrivacidad,
        String recaptchaToken,
        String fotoPerfilUrl) {
}