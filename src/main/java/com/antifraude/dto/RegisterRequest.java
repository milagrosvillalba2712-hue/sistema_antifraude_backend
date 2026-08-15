package com.antifraude.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank String nombre,
        @NotBlank String password,
        @NotBlank String codigoInvitacion) {
}