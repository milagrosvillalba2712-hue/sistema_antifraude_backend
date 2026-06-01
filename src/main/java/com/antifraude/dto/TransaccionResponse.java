package com.antifraude.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransaccionResponse(
        Long id,
        String transactionUuid,
        String identificadorDocumento,
        BigDecimal monto,
        String moneda,
        String canal,
        String tipoTransaccion,
        String estado,
        BigDecimal scoreRiesgo,
        LocalDateTime fechaTransaccion,
        LocalDateTime fechaProcesamiento) {
}
