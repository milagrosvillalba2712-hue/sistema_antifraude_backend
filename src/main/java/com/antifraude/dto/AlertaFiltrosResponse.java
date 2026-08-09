package com.antifraude.dto;

import java.util.List;

public record AlertaFiltrosResponse(
        List<FilterOptionResponse> severidades,
        List<FilterOptionResponse> estados,
        List<FilterOptionResponse> escenarios,
        List<FilterOptionResponse> analistas,
        List<FilterOptionResponse> rangosFecha,
        List<FilterOptionResponse> ordenes,
        List<Integer> tamanosPagina) {
}
