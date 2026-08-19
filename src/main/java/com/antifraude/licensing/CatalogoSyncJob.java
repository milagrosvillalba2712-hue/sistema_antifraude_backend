package com.antifraude.licensing;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Sincronizacion periodica de catalogos AML desde el Control Plane (manifiesto
 * versionado permitido por el plan). Registra si el manifiesto se recibio o si
 * el Control Plane estaba indisponible.
 */
@Component
public class CatalogoSyncJob implements LicensingJob {

    private final LicensingControlPlaneClient controlPlaneClient;
    private final LicensingLocalService licensingService;

    public CatalogoSyncJob(LicensingControlPlaneClient controlPlaneClient,
                           LicensingLocalService licensingService) {
        this.controlPlaneClient = controlPlaneClient;
        this.licensingService = licensingService;
    }

    @Override
    public String codigo() {
        return "CATALOG_SYNC";
    }

    @Override
    public ResultadoJob ejecutar(ContextoJob contexto) {
        InstalacionLocal instalacion = contexto.instalacion();
        Map<String, Object> manifest = controlPlaneClient.catalogManifest();
        boolean online = Boolean.TRUE.equals(manifest.get("online"));
        licensingService.registrarEvento(instalacion, "CATALOG_SYNC", online ? "OK" : "SIN_CONECTIVIDAD",
                Map.of("manifestOnline", online,
                        "versiones", manifest.getOrDefault("versiones", java.util.List.of())));
        return online
                ? new ResultadoJob("OK", "Manifest de catalogos recibido desde Control Plane")
                : new ResultadoJob("SIN_CONECTIVIDAD", "Control Plane no disponible; catalogos sin sincronizar");
    }
}