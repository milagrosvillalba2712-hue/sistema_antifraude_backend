package com.antifraude.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PerfilResponse(
        Long id,
        UUID usuarioId,
        String nombreVisible,
        String imagenPerfil,
        String estado,
        String estadoPersonalizado,
        LocalDateTime ultimaActualizacionEstado) {
}
