package com.antifraude.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank String codigo,
        @NotBlank String nuevaPassword) {
}