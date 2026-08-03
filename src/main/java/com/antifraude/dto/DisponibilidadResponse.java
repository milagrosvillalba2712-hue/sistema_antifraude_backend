package com.antifraude.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DisponibilidadResponse(
        Long id,
        UUID usuarioId,
        String tipoEstado,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        Boolean esProgramado,
        String motivo,
        Boolean activo) {
}
