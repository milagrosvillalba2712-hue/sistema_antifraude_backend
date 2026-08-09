package com.antifraude.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record HistorialAsignacionResponse(
        Long id,
        Long alertaId,
        UUID usuarioOrigenId,
        String usuarioOrigenNombre,
        UUID usuarioDestinoId,
        String usuarioDestinoNombre,
        OffsetDateTime fecha,
        String motivo,
        String tipo) {
}
