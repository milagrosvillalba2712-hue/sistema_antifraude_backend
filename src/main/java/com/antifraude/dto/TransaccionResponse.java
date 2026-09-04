package com.antifraude.dto;

import com.antifraude.transactions.Transaccion.EstadoEvaluacion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransaccionResponse(
        Long id,
        String transactionUuid,
        String codigo,
        String identificadorDocumento,
        String documentoRemitente,
        String tipoDocumentoRemitente,
        String paisEmisorDocumentoRemitente,
        String documentoBeneficiario,
        String tipoDocumentoBeneficiario,
        String paisEmisorDocumentoBeneficiario,
        BigDecimal monto,
        String moneda,
        String canal,
        String tipoTransaccion,
        String estado,
        String estadoEvaluacion,
        BigDecimal scoreRiesgo,
        OffsetDateTime fechaTransaccion,
        OffsetDateTime fechaProcesamiento,
        String personaRemitenteNombre,
        String personaBeneficiarioNombre,
        String productoNombre,
        String paisOrigenNombre,
        String paisDestinoNombre,
        String nivelRiesgoCodigo,
        String entidadOrigenTipo,
        String entidadOrigenCodigo,
        String entidadOrigenNombre,
        String entidadDestinoTipo,
        String entidadDestinoCodigo,
        String entidadDestinoNombre,
        String referenciaExterna) {
}
