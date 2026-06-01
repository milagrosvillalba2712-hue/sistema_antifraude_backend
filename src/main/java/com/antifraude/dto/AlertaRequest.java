package com.antifraude.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AlertaRequest(
        @NotNull Long transaccionId,
        @NotNull Long reglaId,
        @NotBlank String prioridad,
        String observacion) {
}
