package com.antifraude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ApiErrorDescriptor(
        String origen,
        String api,
        @JsonProperty("codigo_error")
        String codigoError,
        @JsonProperty("status_code")
        int statusCode,
        String mensaje,
        String detalles,
        String categoria
) {
}
