package com.antifraude.dto;

public record ServicioExternoAlertaResponse(
        String servicio,
        String estado,
        String mensaje) {
}
