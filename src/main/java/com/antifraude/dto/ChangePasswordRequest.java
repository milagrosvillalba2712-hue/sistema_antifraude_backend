package com.antifraude.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank String passwordActual,
        @NotBlank String nuevaPassword) {
}