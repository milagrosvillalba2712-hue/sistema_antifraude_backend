package com.antifraude.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AsignarAlertaRequest(
        UUID analistaId) {
}
