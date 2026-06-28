package com.antifraude.dto;

import jakarta.validation.constraints.NotNull;

public record ReasignarAlertaRequest(
        @NotNull Long analistaId,
        String motivo) {
}
