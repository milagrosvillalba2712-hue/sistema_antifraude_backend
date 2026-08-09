package com.antifraude.dto;

import java.math.BigDecimal;

public record HallazgoAlertaResponse(
        Long id,
        String tipo,
        String titulo,
        String descripcion,
        String severidad,
        BigDecimal score,
        String fuente,
        String detalleJson,
        ReglaAlertaResponse regla) {
}
