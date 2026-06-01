package com.antifraude.dto;

import java.time.LocalDateTime;

public record ReglaRiesgoResponse(
        Long id,
        String nombre,
        String descripcion,
        String tipoRegla,
        String severidad,
        String condicion,
        Boolean activa,
        Long creadaPor,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaModificacion) {
}
