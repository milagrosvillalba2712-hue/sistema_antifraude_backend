package com.antifraude.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        String nombre,
        String email,
        String rol,
        UUID empresaId,
        String empresaNombre,
        Boolean activo,
        Integer intentosFallidos,
        OffsetDateTime fechaCreacion) {
}
