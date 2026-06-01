package com.antifraude.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransaccionRequest(
        @NotBlank String transactionUuid,
        @NotBlank String identificadorDocumento,
        @NotBlank String cuentaOrigen,
        @NotBlank String cuentaDestino,
        @NotNull @Positive BigDecimal monto,
        String moneda,
        String canal,
        @NotBlank String tipoTransaccion,
        String ipOrigen,
        String paisOrigen,
        @NotNull LocalDateTime fechaTransaccion) {
}
