package com.antifraude.dto;

import java.time.LocalDateTime;

public record AlertaResponse(
        Long id,
        Long transaccionId,
        Long reglaId,
        String prioridad,
        String estado,
        String observacion,
        Long asignadoA,
        LocalDateTime fechaGeneracion,
        LocalDateTime fechaResolucion) {
}
