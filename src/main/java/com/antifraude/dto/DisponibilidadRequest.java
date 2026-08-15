package com.antifraude.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record DisponibilidadRequest(
        @NotBlank String tipoEstado,
        @NotNull OffsetDateTime fechaInicio,
        OffsetDateTime fechaFin,
        Boolean esProgramado,
        String motivo) {
}
