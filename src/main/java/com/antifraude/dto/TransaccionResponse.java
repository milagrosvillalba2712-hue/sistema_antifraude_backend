package com.antifraude.dto;

import com.antifraude.transactions.Transaccion.EstadoEvaluacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransaccionResponse(
        Long id,
        String transactionUuid,
        String codigo,
        String identificadorDocumento,
        BigDecimal monto,
        String moneda,
        String canal,
        String tipoTransaccion,
        String estado,
        String estadoEvaluacion,
        BigDecimal scoreRiesgo,
        LocalDateTime fechaTransaccion,
        LocalDateTime fechaProcesamiento,
        String personaRemitenteNombre,
        String personaBeneficiarioNombre,
        String productoNombre,
        String paisOrigenNombre,
        String paisDestinoNombre,
        String nivelRiesgoCodigo) {
}
