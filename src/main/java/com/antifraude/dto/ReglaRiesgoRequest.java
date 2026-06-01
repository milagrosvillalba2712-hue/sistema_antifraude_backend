package com.antifraude.dto;

import jakarta.validation.constraints.NotBlank;

public record ReglaRiesgoRequest(
        @NotBlank String nombre,
        String descripcion,
        String tipoRegla,
        String severidad,
        @NotBlank String condicion,
        Boolean activa,
        Long creadaPor) {
}
