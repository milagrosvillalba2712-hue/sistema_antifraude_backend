package com.antifraude.dto;

import jakarta.validation.constraints.NotBlank;

public record KycRequest(
        @NotBlank String identificadorDocumento,
        String tipoConsulta) {
}
