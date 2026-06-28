package com.antifraude.dto;

import jakarta.validation.constraints.NotNull;

public record AsignarAlertaRequest(
        Long analistaId) {
}
