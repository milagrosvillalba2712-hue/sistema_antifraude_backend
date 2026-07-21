package com.antifraude.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record TransaccionAlertaResponse(
        Long id,
        String codigo,
        String transactionUuid,
        String identificadorDocumento,
        String cuentaOrigen,
        String cuentaDestino,
        BigDecimal monto,
        String moneda,
        String canal,
        String tipoTransaccion,
        String ipOrigen,
        String paisOrigen,
        LocalDateTime fechaTransaccion,
        BigDecimal scoreRiesgo,
        String nivelRiesgo,
        String estadoEvaluacion,
        Map<String, Object> remitente,
        Map<String, Object> beneficiario,
        Map<String, Object> operacion,
        Map<String, Object> controlSeguimiento,
        Map<String, Object> internacional) {
}
