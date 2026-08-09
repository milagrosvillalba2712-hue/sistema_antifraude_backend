package com.antifraude.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

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
        OffsetDateTime fechaTransaccion,
        String nivelRiesgo,
        java.math.BigDecimal score,
        String severidad,
        String prioridad,
        String estado,
        String observacion,
        UUID asignadoA,
        String asignadoNombre,
        OffsetDateTime fechaGeneracion,
        OffsetDateTime fechaResolucion) {
}
