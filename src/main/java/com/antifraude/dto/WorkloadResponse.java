package com.antifraude.dto;

public record WorkloadResponse(
        Long usuarioId,
        String nombre,
        Integer alertasAsignadas,
        Integer alertasPendientes,
        Long tiempoPromedioResolucion) {
}
