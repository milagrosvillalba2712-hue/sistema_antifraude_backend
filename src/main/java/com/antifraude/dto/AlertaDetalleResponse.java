package com.antifraude.dto;

import java.util.List;
import java.util.Map;

public record AlertaDetalleResponse(AlertaResponse alerta,
                                    Map<String, Object> transaccion,
                                    Map<String, Object> regla,
                                    Map<String, Object> cliente,
                                    List<Map<String, Object>> historialTransaccional,
                                    List<Map<String, Object>> serviciosExternos,
                                    List<TimelineEventResponse> timeline,
                                    ResolucionAlertaResponse resolucion) {
}
