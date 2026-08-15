package com.antifraude.dto;

import java.math.BigDecimal;

public record ReglaAlertaResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        String severidad,
        Integer prioridad,
        String estado,
        String condicion,
        String condicionesJson,
        String accionesJson,
        BigDecimal scoreBase,
        Long escenarioId,
        String escenarioNombre) {
}
