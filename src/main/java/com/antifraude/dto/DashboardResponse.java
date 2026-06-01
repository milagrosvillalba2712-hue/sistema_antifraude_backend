package com.antifraude.dto;

import java.math.BigDecimal;
import java.util.Map;

public record DashboardResponse(
        Long totalTransacciones,
        Long transaccionesSospechosas,
        Long alertasPendientes,
        Long alertasResueltas,
        BigDecimal promedioScoreRiesgo,
        Map<String, Long> transaccionesPorEstado,
        Map<String, Long> alertasPorPrioridad) {
}
