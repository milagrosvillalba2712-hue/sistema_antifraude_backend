package com.antifraude.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ResolucionAlertaResponse(Long id, Long alertaId, UUID usuarioId, String usuarioNombre,
                                       String resultado, String conclusion, String decision,
                                       String justificacion, String evidenciaDescripcion,
                                       String contactoCliente, Boolean fondosRetenidos,
                                       Boolean movimientoLiberable, Boolean requiereRos,
                                       Boolean requiereBloqueo, Boolean requiereEscalamientoLegal,
                                       OffsetDateTime fechaResolucion) {
}
