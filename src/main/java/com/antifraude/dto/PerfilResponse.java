package com.antifraude.dto;

import java.time.LocalDateTime;

public record PerfilResponse(
        Long id,
        Long usuarioId,
        String nombreVisible,
        String imagenPerfil,
        String estado,
        String estadoPersonalizado,
        LocalDateTime ultimaActualizacionEstado) {
}
