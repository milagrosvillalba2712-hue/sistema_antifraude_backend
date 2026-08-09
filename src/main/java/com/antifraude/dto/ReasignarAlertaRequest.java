package com.antifraude.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReasignarAlertaRequest(
        @NotNull UUID analistaId,
        String motivo,
        String observacion) {
}
