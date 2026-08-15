package com.antifraude.dto;

import java.util.List;

public record AlertaDetalleResponse(AlertaResponse alerta,
                                    TransaccionAlertaResponse transaccion,
                                    ReglaAlertaResponse regla,
                                    List<ReglaAlertaResponse> reglasDisparadas,
                                    List<HallazgoAlertaResponse> hallazgosRegulatorios,
                                    ClienteAlertaResponse cliente,
                                    List<TransaccionAlertaResponse> historialTransaccional,
                                    List<ServicioExternoAlertaResponse> serviciosExternos,
                                    List<TimelineEventResponse> timeline,
                                    List<TimelineEventResponse> accionesTimeline,
                                    List<EvidenciaAlertaResponse> evidencias,
                                    ResolucionAlertaResponse resolucion,
                                    AprobacionSupervisorResponse aprobacion,
                                    List<String> accionesDisponibles) {
}
