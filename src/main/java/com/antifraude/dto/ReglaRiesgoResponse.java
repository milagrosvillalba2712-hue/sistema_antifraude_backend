package com.antifraude.dto;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record ReglaRiesgoResponse(
        Long id,
        Long escenarioId,
        String escenarioNombre,
        String codigo,
        String nombre,
        String descripcion,
        String tipoRegla,
        String severidad,
        Integer prioridad,
        BigDecimal score,
        Integer version,
        String estado,
        String condicion,
        String condicionesJson,
        String accionesJson,
        Boolean activa,
        Long creadaPor,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaModificacion) {
}
