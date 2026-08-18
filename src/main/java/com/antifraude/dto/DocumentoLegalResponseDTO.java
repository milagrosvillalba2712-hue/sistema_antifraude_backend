package com.antifraude.dto;

import com.antifraude.common.entity.TipoDocumentoLegal;

import java.time.OffsetDateTime;

public record DocumentoLegalResponseDTO(
        Long id,
        TipoDocumentoLegal tipo,
        Integer version,
        String titulo,
        String contenido,
        String urlDocumento,
        OffsetDateTime fechaPublicacion
) {
}
