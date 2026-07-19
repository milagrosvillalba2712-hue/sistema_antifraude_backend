package com.antifraude.dto;

import java.time.LocalDateTime;

public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        String rol,
        Long empresaId,
        String empresaNombre,
        Boolean activo,
        Integer intentosFallidos,
        LocalDateTime fechaCreacion) {
}
