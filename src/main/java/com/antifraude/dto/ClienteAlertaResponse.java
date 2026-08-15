package com.antifraude.dto;

import java.util.Map;

public record ClienteAlertaResponse(
        String documento,
        String personaRemitente,
        String personaBeneficiario,
        String pep,
        String observado,
        String listas,
        String fuente,
        Map<String, Object> personal,
        Map<String, Object> laboral,
        Map<String, Object> academico,
        Map<String, Object> familiar,
        Map<String, Object> judicialRegulatorio) {
}
