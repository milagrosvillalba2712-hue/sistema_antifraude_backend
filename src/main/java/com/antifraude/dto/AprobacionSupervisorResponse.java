package com.antifraude.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AprobacionSupervisorResponse(
        Long id,
        Long alertaId,
        Long resolucionId,
        UUID supervisorId,
        String supervisorNombre,
        String estado,
        String observacion,
        String motivoRechazo,
        String faltantes,
        OffsetDateTime fechaSolicitud,
        OffsetDateTime fechaAprobacion) {
}
