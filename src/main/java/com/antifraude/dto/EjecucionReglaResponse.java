package com.antifraude.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        LocalDateTime fechaHoraEjecucion
) {
}
