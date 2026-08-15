package com.antifraude.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SimuladorRequest(
        String productoCodigo,
        String canalCodigo,
        String monedaCodigo,
        BigDecimal monto,
        String paisOrigenCodigo,
        String paisDestinoCodigo,
        String documentoCliente,
        OffsetDateTime fechaHora
) {
}

