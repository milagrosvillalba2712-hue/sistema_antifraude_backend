package com.antifraude.licensing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Valida periodicamente las licencias locales instaladas (2.4): recorre todas
 * las instalaciones y reevalua su estado segun vigencia/gracia. No bloquea ni
 * muta: evalua y deja el resultado auditable. La politica offline/online queda
 * a cargo del endpoint /validate y del filtro cuando este habilitado.
 */
@Component
@ConditionalOnProperty(name = "app.licenses.local.scheduler.enabled", havingValue = "true")
public class LicenciaLocalScheduler {

    private static final Logger log = LoggerFactory.getLogger(LicenciaLocalScheduler.class);

    private final InstalacionLocalRepository instalacionRepository;
    private final LicensingValidationService validationService;
    private final LicensingLocalService licensingService;

    public LicenciaLocalScheduler(InstalacionLocalRepository instalacionRepository,
                                  LicensingValidationService validationService,
                                  LicensingLocalService licensingService) {
        this.instalacionRepository = instalacionRepository;
        this.validationService = validationService;
        this.licensingService = licensingService;
    }

    @Scheduled(cron = "0 15 3 * * *")
    public void validarInstalaciones() {
        List<InstalacionLocal> instalaciones = instalacionRepository.findAll();
        int operativas = 0;
        int enGracia = 0;
        int bloqueadas = 0;
        for (InstalacionLocal instalacion : instalaciones) {
            try {
                LicensingValidationService.ResultadoValidacion resultado =
                        validationService.validar(instalacion.getId(), false);
                switch (resultado.modo()) {
                    case OPERATIVO -> operativas++;
                    case SOLO_LECTURA -> {
                        enGracia++;
                        licensingService.registrarEvento(instalacion, "VALIDACION_PROGRAMADA",
                                "SOLO_LECTURA", java.util.Map.of("detalle", resultado.detalle()));
                    }
                    case BLOQUEADO -> {
                        bloqueadas++;
                        licensingService.registrarEvento(instalacion, "VALIDACION_PROGRAMADA",
                                "BLOQUEADA", java.util.Map.of("detalle", resultado.detalle()));
                        log.warn("[LICENCIA] Instalacion bloqueada - {}", instalacion.getIdentificadorInstalacion());
                    }
                }
            } catch (RuntimeException exception) {
                log.info("[LICENCIA] Validacion programada omitida para {} ({})",
                        instalacion.getIdentificadorInstalacion(), exception.getClass().getSimpleName());
            }
        }
        log.info("[LICENCIA] Validacion programada: {} instalaciones | {} operativas | {} en gracia | {} bloqueadas",
                instalaciones.size(), operativas, enGracia, bloqueadas);
    }
}