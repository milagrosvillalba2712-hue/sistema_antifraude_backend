package com.antifraude.dto;

import java.util.List;

public record PendienteAceptacionDTO(
        boolean requiereAceptacion,
        List<DocumentoLegalResponseDTO> documentosPendientes
) {
}
