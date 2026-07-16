package com.antifraude.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ReglaRiesgoRequest(
        Long escenarioId,
        String codigo,
        @NotBlank String nombre,
        String descripcion,
        String tipoRegla,
        String severidad,
        Integer prioridad,
        BigDecimal score,
        BigDecimal scoreBase,
        String condicion,
        Map<String, Object> condiciones,
        List<Map<String, Object>> acciones,
        List<Long> accionIds,
        String estado,
        Boolean activa,
        Long creadaPor) {
}
