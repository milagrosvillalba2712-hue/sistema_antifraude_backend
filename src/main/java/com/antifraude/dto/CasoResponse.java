package com.antifraude.dto;

import com.antifraude.common.entity.Caso.EstadoCaso;
import com.antifraude.common.entity.Caso.PrioridadCaso;
import com.antifraude.common.entity.Caso.ResultadoInvestigacion;

import java.time.LocalDateTime;
import java.util.UUID;

public record CasoResponse(
        Long id,
        String codigo,
        String titulo,
        String descripcion,
        EstadoCaso estado,
        PrioridadCaso prioridad,
        Integer score,
        UUID usuarioAnalistaId,
        String usuarioAnalistaNombre,
        LocalDateTime fechaApertura,
        LocalDateTime fechaCierre,
        ResultadoInvestigacion resultado,
        String observaciones) {
}
