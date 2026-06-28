package com.antifraude.dto;

import java.time.LocalDateTime;

public record HistorialAsignacionResponse(
        Long id,
        Long alertaId,
        Long usuarioOrigenId,
        String usuarioOrigenNombre,
        Long usuarioDestinoId,
        String usuarioDestinoNombre,
        LocalDateTime fecha,
        String motivo,
        String tipo) {
}
