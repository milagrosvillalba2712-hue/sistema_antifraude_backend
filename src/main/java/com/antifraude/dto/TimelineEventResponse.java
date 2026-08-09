package com.antifraude.dto;

import java.time.OffsetDateTime;

public record TimelineEventResponse(
        Long id,
        String tipo,
        String descripcion,
        OffsetDateTime fecha,
        String usuario) {
}
