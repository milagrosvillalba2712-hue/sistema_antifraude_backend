package com.antifraude.dto;

import jakarta.validation.constraints.NotNull;

public record AceptaTerminosRequestDTO(
        @NotNull Long documentoLegalId,
        @NotNull Boolean acepto
) {
}
