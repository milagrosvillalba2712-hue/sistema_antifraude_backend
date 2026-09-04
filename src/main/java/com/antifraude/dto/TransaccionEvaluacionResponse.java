package com.antifraude.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Respuesta result-focused del POST /api/transacciones: expone el resultado
 * y la evaluación de la transferencia, no el eco del body.
 */
public record TransaccionEvaluacionResponse(
        Long id,
        String transactionUuid,
        String codigo,
        String tipoTransaccion,
        String estado,
        String estadoEvaluacion,
        BigDecimal scoreRiesgo,
        String nivelRiesgoCodigo,
        boolean requiereAccionInmediata,
        List<ReglaDisparadaDto> reglasDisparadas,
        List<CoincidenciaDto> screening,
        OffsetDateTime fechaTransaccion,
        OffsetDateTime fechaProcesamiento) {

    public record ReglaDisparadaDto(
            String origen,
            String codigo,
            String descripcion,
            BigDecimal score,
            String severidad) {
    }

    public record CoincidenciaDto(
            String sujeto,
            String campo,
            String parte,
            BigDecimal similitud,
            String severidad,
            String descripcion) {
    }
}
