package com.antifraude.dto;

import java.time.LocalDateTime;

public record AprobacionSupervisorResponse(
        Long id,
        Long alertaId,
        Long resolucionId,
        Long supervisorId,
        String supervisorNombre,
        String estado,
        String observacion,
        String motivoRechazo,
        String faltantes,
        LocalDateTime fechaSolicitud,
        LocalDateTime fechaAprobacion) {
}
