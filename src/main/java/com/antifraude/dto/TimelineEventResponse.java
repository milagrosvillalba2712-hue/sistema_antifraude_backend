package com.antifraude.dto;

import java.time.LocalDateTime;

public record TimelineEventResponse(
        Long id,
        String tipo,
        String descripcion,
        LocalDateTime fecha,
        String usuario) {
}
