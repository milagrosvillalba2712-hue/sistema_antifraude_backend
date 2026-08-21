package com.antifraude.licensing;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reporte periodico del consumo mensual de la empresa hacia el Control Plane
 * (telemetria de uso). Usa los contadores agregados de uso_suscripcion del
 * periodo vigente; nunca envia datos personales ni detalle transaccional.
 */
@Component
public class ConsumoJob implements LicensingJob {

    private final UsoSuscripcionRepository usoSuscripcionRepository;
    private final LicensingControlPlaneClient controlPlaneClient;
    private final LicensingLocalService licensingService;

    public ConsumoJob(UsoSuscripcionRepository usoSuscripcionRepository,
                      LicensingControlPlaneClient controlPlaneClient,
                      LicensingLocalService licensingService) {
        this.usoSuscripcionRepository = usoSuscripcionRepository;
        this.controlPlaneClient = controlPlaneClient;
        this.licensingService = licensingService;
    }

    @Override
    public String codigo() {
        return "LICENSE_USAGE_SYNC";
    }

    @Override
    public ResultadoJob ejecutar(ContextoJob contexto) {
        InstalacionLocal instalacion = contexto.instalacion();
        List<UsoSuscripcion> usos = usoSuscripcionRepository
                .findByEmpresaIdOrderByAnioDescMesDesc(contexto.empresaId());
        UsoSuscripcion uso = usos.isEmpty() ? null : usos.get(0);

        Map<String, Object> usage = new LinkedHashMap<>();
        String periodo = "sin-consumo";
        if (uso != null) {
            periodo = uso.getAnio() + "-" + uso.getMes();
            usage.put("anio", uso.getAnio());
            usage.put("mes", uso.getMes());
            usage.put("usuariosActivos", uso.getUsuariosActivos());
            usage.put("transaccionesProcesadas", uso.getTransaccionesProcesadas());
            usage.put("consultasKyc", uso.getConsultasKyc());
            usage.put("alertasGeneradas", uso.getAlertasGeneradas());
            usage.put("reportesGenerados", uso.getReportesGenerados());
        }

        Map<String, Object> controlPlane = controlPlaneClient.reportUsage(instalacion.getId(), usage);
        boolean online = Boolean.TRUE.equals(controlPlane.get("online"));
        licensingService.registrarEvento(instalacion, "CONSUMO_REPORTADO", online ? "OK" : "SIN_CONECTIVIDAD",
                Map.of("periodo", periodo, "controlPlane", online ? "ONLINE" : "OFFLINE"));
        return online
                ? new ResultadoJob("OK", "Consumo del periodo " + periodo + " reportado al Control Plane")
                : new ResultadoJob("SIN_CONECTIVIDAD", "Control Plane no disponible; consumo no reportado");
    }
}