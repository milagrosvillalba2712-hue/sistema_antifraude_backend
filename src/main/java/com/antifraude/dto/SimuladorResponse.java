package com.antifraude.dto;

import java.math.BigDecimal;
import java.util.List;

public record SimuladorResponse(
        BigDecimal scoreTotal,
        String nivelRiesgo,
        boolean requiereAccionInmediata,
        String observaciones,
        String estado,
        String estadoEvaluacion,
        List<ReglaResultado> reglasEjecutadas,
        List<String> accionesSugeridas
) {
    public record ReglaResultado(
            String codigo,
            String nombre,
            boolean cumplida,
            BigDecimal score,
            String severidad
    ) {}
}
