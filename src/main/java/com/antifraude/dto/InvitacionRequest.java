package com.antifraude.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InvitacionRequest(
        @NotBlank String rol,
        @NotNull UUID empresaId,
        @Email String email) {
}