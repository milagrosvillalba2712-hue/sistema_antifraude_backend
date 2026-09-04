package com.antifraude.dto;

public record TipoDocumentoCatalogoResponse(
        Long id,
        String codigo,
        String codigoTecnico,
        String sigla,
        String nombre,
        String paisCodigo,
        String paisNombre,
        String tipoPersona,
        String formatoRegex,
        String fuenteOficialCita) {
}
