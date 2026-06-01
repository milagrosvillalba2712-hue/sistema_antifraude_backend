package com.antifraude.dto;

public record KycResponse(
        String identificadorDocumento,
        String tipoConsulta,
        Boolean resultado,
        String mensaje) {
}
