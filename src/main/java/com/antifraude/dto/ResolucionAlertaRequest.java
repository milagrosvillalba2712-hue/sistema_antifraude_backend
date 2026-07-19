package com.antifraude.dto;

public record ResolucionAlertaRequest(String resultado, String conclusion, String decision,
                                      String justificacion, String evidenciaDescripcion,
                                      String contactoCliente, Boolean fondosRetenidos,
                                      Boolean movimientoLiberable, Boolean requiereRos,
                                      Boolean requiereBloqueo, Boolean requiereEscalamientoLegal) {
}
