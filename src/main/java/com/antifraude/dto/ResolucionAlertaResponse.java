package com.antifraude.dto;

import java.time.LocalDateTime;

public record ResolucionAlertaResponse(Long id, Long alertaId, Long usuarioId, String usuarioNombre,
                                       String resultado, String conclusion, String decision,
                                       String justificacion, String evidenciaDescripcion,
                                       String contactoCliente, Boolean fondosRetenidos,
                                       Boolean movimientoLiberable, Boolean requiereRos,
                                       Boolean requiereBloqueo, Boolean requiereEscalamientoLegal,
                                       LocalDateTime fechaResolucion) {
}
