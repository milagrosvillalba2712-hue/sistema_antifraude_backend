package com.antifraude.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DisponibilidadResponse(
        Long id,
        UUID usuarioId,
        String tipoEstado,
        OffsetDateTime fechaInicio,
        OffsetDateTime fechaFin,
        Boolean esProgramado,
        String motivo,
        Boolean activo) {
}
