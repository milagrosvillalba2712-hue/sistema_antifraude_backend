package com.antifraude.dto;

import java.time.LocalDateTime;

public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        String rol,
        Boolean activo,
        Integer intentosFallidos,
        LocalDateTime fechaCreacion) {
}
