package com.antifraude.dto;

import com.antifraude.validation.ValidPartyInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@ValidPartyInfo
public record TransaccionRequest(
        @NotBlank String transactionUuid,
        @NotBlank String documentoRemitente,
        String tipoDocumentoRemitente,
        Long tipoDocumentoRemitenteId,
        @NotBlank String paisEmisorDocumentoRemitente,
        @NotBlank String documentoBeneficiario,
        String tipoDocumentoBeneficiario,
        Long tipoDocumentoBeneficiarioId,
        @NotBlank String paisEmisorDocumentoBeneficiario,
        @NotBlank String cuentaOrigen,
        @NotBlank String cuentaDestino,
        @NotNull @Positive BigDecimal monto,
        String moneda,
        String canal,
        @NotBlank String tipoTransaccion,
        String ipOrigen,
        String paisOrigen,
        String paisDestino,
        @NotNull OffsetDateTime fechaTransaccion,
        Long productoId,
        Long personaRemitenteId,
        Long personaBeneficiarioId,
        String tipoPersonaRemitente,
        String nombreCompletoRemitente,
        String tipoPersonaBeneficiario,
        String nombreCompletoBeneficiario) {
}
