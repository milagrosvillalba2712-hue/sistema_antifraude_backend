package com.antifraude.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record ClienteAlertaResponse(
        String documento,
        String personaRemitente,
        String personaBeneficiario,
        String pep,
        String observado,
        String listas,
        String fuente,
        OffsetDateTime fechaConsulta,
        Boolean cacheVigente,
        Boolean puedeActualizar,
        String mensajeConsulta,
        Boolean snapshotDisponible,
        Map<String, Object> personal,
        Map<String, Object> laboral,
        Map<String, Object> academico,
        Map<String, Object> familiar,
        Map<String, Object> judicialRegulatorio) {
}
