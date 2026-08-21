package com.antifraude.licensing;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Validacion programada de licencia: consulta el Control Plane, renueva el
 * lease firmado cuando es posible y reevalua la politica local (OPERATIVO /
 * SOLO_LECTURA / BLOQUEADO). Reemplaza al scheduler diario fijo y lo hace
 * configurable (frecuencia y ejecucion manual).
 */
@Component
public class ValidacionLicenciaJob implements LicensingJob {

    private final LicenciaLocalRepository licenciaRepository;
    private final LicensingOnlineService onlineService;
    private final LicensingLocalService licensingService;

    public ValidacionLicenciaJob(LicenciaLocalRepository licenciaRepository,
                                 LicensingOnlineService onlineService,
                                 LicensingLocalService licensingService) {
        this.licenciaRepository = licenciaRepository;
        this.onlineService = onlineService;
        this.licensingService = licensingService;
    }

    @Override
    public String codigo() {
        return "VALIDACION_LICENCIA";
    }

    @Override
    public ResultadoJob ejecutar(ContextoJob contexto) {
        InstalacionLocal instalacion = contexto.instalacion();
        LicenciaLocal licencia = licenciaRepository.findTopByInstalacionIdOrderByEmitidaEnDesc(instalacion.getId())
                .orElse(null);
        if (licencia == null) {
            return new ResultadoJob("SIN_LICENCIA", "No existe licencia emitida para la instalacion");
        }

        LicensingOnlineService.ResultadoValidacionOnline resultado = onlineService.validarYRenovar(instalacion);
        licencia.setUltimaValidacionEn(OffsetDateTime.now());
        licenciaRepository.save(licencia);
        licensingService.registrarEvento(instalacion, "VALIDACION_PROGRAMADA", resultado.modo().name(),
                Map.of("motivo", String.valueOf(resultado.motivo()),
                        "detalle", String.valueOf(resultado.detalle()),
                        "online", resultado.online()));
        return new ResultadoJob(resultado.modo().name(), resultado.detalle());
    }
}