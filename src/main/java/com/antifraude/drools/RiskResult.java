package com.antifraude.drools;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de la evaluación de riesgo por Drools.
 * Incluye score total, nivel de riesgo, y lista de reglas que dispararon.
 */
public record RiskResult(
        BigDecimal scoreTotal,
        String nivelRiesgo,
        List<ReglaDisparada> reglasDisparadas,
        boolean requiereAccionInmediata,
        String observaciones
) {
    public static RiskResult vacio() {
        return new RiskResult(BigDecimal.ZERO, "BAJO", List.of(), false, null);
    }

    public static RiskResult desdeScore(BigDecimal score) {
        String nivel = calcularNivel(score);
        return new RiskResult(score, nivel, List.of(), false, null);
    }

    private static String calcularNivel(BigDecimal score) {
        if (score.compareTo(new BigDecimal("70")) >= 0) return "CRITICO";
        if (score.compareTo(new BigDecimal("50")) >= 0) return "ALTO";
        if (score.compareTo(new BigDecimal("30")) >= 0) return "MEDIO";
        return "BAJO";
    }

    public record ReglaDisparada(
            Long reglaId,
            String codigo,
            String descripcion,
            BigDecimal score,
            String severidad,
            String accionRecomendada
    ) {}
}
