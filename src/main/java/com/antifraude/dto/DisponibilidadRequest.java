package com.antifraude.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record DisponibilidadRequest(
        @NotBlank String tipoEstado,
        @NotNull LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        Boolean esProgramado,
        String motivo) {
}
