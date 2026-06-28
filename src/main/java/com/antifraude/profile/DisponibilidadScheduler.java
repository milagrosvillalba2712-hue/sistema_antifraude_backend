package com.antifraude.profile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DisponibilidadScheduler {

    private static final Logger log = LoggerFactory.getLogger(DisponibilidadScheduler.class);

    private final DisponibilidadService disponibilidadService;

    public DisponibilidadScheduler(DisponibilidadService disponibilidadService) {
        this.disponibilidadService = disponibilidadService;
    }

    @Scheduled(fixedRate = 60000)
    public void procesarProgramaciones() {
        log.debug("[SCHEDULER] Verificando disponibilidades programadas...");
        disponibilidadService.procesarProgramacionesPendientes();
    }
}
