package com.antifraude.dto;

public record CatalogoResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        Boolean activo
) {
}
