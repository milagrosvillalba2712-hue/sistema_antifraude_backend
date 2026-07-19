package com.antifraude.dto;

import java.time.LocalDateTime;

public record AlertaResponse(
        Long id,
        String codigo,
        Long transaccionId,
        Long reglaId,
        String reglaNombre,
        java.math.BigDecimal score,
        String prioridad,
        String estado,
        String observacion,
        Long asignadoA,
        String asignadoNombre,
        LocalDateTime fechaGeneracion,
        LocalDateTime fechaResolucion) {
}
