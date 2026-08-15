package com.antifraude.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PerfilResponse(
        Long id,
        UUID usuarioId,
        String nombreVisible,
        String imagenPerfil,
        String estado,
        String estadoPersonalizado,
        OffsetDateTime ultimaActualizacionEstado) {
}
