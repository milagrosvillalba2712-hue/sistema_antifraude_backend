package com.antifraude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record ApiErrorDescriptor(
        String origen,
        @JsonProperty("tipo_origen")
        String tipoOrigen,
        String api,
        @JsonProperty("codigo_error")
        String codigoError,
        @JsonProperty("status_code")
        int statusCode,
        String mensaje,
        String detalles,
        String categoria,
        OffsetDateTime fecha
) {
        public ApiErrorDescriptor(String origen, String api, String codigoError, int statusCode, String mensaje,
                                  String detalles, String categoria) {
                this(origen, null, api, codigoError, statusCode, mensaje, detalles, categoria, null);
        }
}
