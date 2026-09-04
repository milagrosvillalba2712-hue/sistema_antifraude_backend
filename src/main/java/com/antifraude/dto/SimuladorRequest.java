package com.antifraude.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
public record SimuladorRequest(
        String productoCodigo,
        String canalCodigo,
        String monedaCodigo,
        @NotNull @Positive BigDecimal monto,
        String paisOrigenCodigo,
        String paisDestinoCodigo,
        @NotBlank String documentoCliente,
        @NotBlank String tipoDocumentoCliente,
        @NotBlank String paisEmisorDocumentoCliente,
        String fechaHora
) {
}
