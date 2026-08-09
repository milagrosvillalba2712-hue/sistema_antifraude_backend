package com.antifraude.dto;

import java.util.UUID;

public record WorkloadResponse(
        UUID usuarioId,
        String nombre,
        Integer alertasAsignadas,
        Integer alertasPendientes,
        Long tiempoPromedioResolucion) {
}
