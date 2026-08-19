package com.antifraude.licensing;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Heartbeat periodico hacia el Control Plane (agente on-premise). Actualiza el
 * latido local y reporta al Control Plane; la indisponibilidad del control
 * plane se registra como resultado SIN_CONECTIVIDAD, no como fallo.
 */
@Component
public class HeartbeatJob implements LicensingJob {

    private final InstalacionLocalRepository instalacionRepository;
    private final LicensingControlPlaneClient controlPlaneClient;
    private final LicensingLocalService licensingService;

    public HeartbeatJob(InstalacionLocalRepository instalacionRepository,
                        LicensingControlPlaneClient controlPlaneClient,
                        LicensingLocalService licensingService) {
        this.instalacionRepository = instalacionRepository;
        this.controlPlaneClient = controlPlaneClient;
        this.licensingService = licensingService;
    }

    @Override
    public String codigo() {
        return "HEARTBEAT";
    }

    @Override
    public ResultadoJob ejecutar(ContextoJob contexto) {
        InstalacionLocal instalacion = contexto.instalacion();
        instalacion.setUltimoHeartbeatEn(OffsetDateTime.now());
        instalacionRepository.save(instalacion);

        Map<String, Object> controlPlane = controlPlaneClient.reportHeartbeat(instalacion.getId());
        boolean online = Boolean.TRUE.equals(controlPlane.get("online"));
        licensingService.registrarEvento(instalacion, "HEARTBEAT", online ? "OK" : "SIN_CONECTIVIDAD",
                Map.of("controlPlane", online ? "ONLINE" : "OFFLINE",
                        "estado", String.valueOf(controlPlane.getOrDefault("estado", "SIN_CONECTIVIDAD"))));
        return online
                ? new ResultadoJob("OK", "Heartbeat reportado al Control Plane")
                : new ResultadoJob("SIN_CONECTIVIDAD", "Control Plane no disponible; heartbeat solo local");
    }
}