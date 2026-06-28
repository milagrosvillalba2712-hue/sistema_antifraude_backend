package com.antifraude.dto;

import java.time.LocalDateTime;

public record DisponibilidadResponse(
        Long id,
        Long usuarioId,
        String tipoEstado,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        Boolean esProgramado,
        String motivo,
        Boolean activo) {
}
