package com.antifraude.dto;

import java.time.LocalDateTime;

public record AlertaResponse(
        Long id,
        String codigo,
        Long transaccionId,
        Long reglaId,
        String reglaNombre,
        Long escenarioId,
        String escenarioNombre,
        String clienteDocumento,
        String clienteNombre,
        java.math.BigDecimal monto,
        String moneda,
        String canal,
        String paisOrigen,
        LocalDateTime fechaTransaccion,
        String nivelRiesgo,
        java.math.BigDecimal score,
        String severidad,
        String prioridad,
        String estado,
        String observacion,
        Long asignadoA,
        String asignadoNombre,
        LocalDateTime fechaGeneracion,
        LocalDateTime fechaResolucion) {
}
