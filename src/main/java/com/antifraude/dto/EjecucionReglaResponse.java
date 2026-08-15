package com.antifraude.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record EjecucionReglaResponse(
        Long id,
        Long transaccionId,
        String transaccionCodigo,
        Long reglaId,
        String reglaCodigo,
        String reglaNombre,
        Integer versionReglaEvaluada,
        Boolean resultadoBooleano,
        BigDecimal scoreAportado,
        String accionesGeneradas,
        Long tiempoEjecucionMs,
        OffsetDateTime fechaHoraEjecucion
) {
}
