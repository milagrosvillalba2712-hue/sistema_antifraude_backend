package com.antifraude.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record HistorialAsignacionResponse(
        Long id,
        Long alertaId,
        UUID usuarioOrigenId,
        String usuarioOrigenNombre,
        UUID usuarioDestinoId,
        String usuarioDestinoNombre,
        LocalDateTime fecha,
        String motivo,
        String tipo) {
}
