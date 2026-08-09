package com.antifraude.dto;

public record EvidenciaAlertaRequest(
        String nombre,
        String descripcion,
        String tipo,
        String extension,
        String mimeType,
        Long tamanoBytes,
        String referenciaArchivo,
        String estado) {
}
