package com.antifraude.dto;

import java.time.LocalDateTime;

public record EvidenciaAlertaResponse(
        Long id,
        String nombre,
        String descripcion,
        String tipo,
        String extension,
        String mimeType,
        Long tamanoBytes,
        String estado,
        String referenciaArchivo,
        String cargadoPor,
        LocalDateTime fechaCarga) {
}
