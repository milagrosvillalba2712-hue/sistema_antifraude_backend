package com.antifraude.assignment;

import com.antifraude.alerts.AlertaRepository;
import com.antifraude.profile.DisponibilidadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AssignmentScheduler {

    private static final Logger log = LoggerFactory.getLogger(AssignmentScheduler.class);

    private final AssignmentEngine assignmentEngine;
    private final AlertaRepository alertaRepository;
    private final DisponibilidadService disponibilidadService;

    public AssignmentScheduler(AssignmentEngine assignmentEngine,
                                AlertaRepository alertaRepository,
                                DisponibilidadService disponibilidadService) {
        this.assignmentEngine = assignmentEngine;
        this.alertaRepository = alertaRepository;
        this.disponibilidadService = disponibilidadService;
    }

    @Scheduled(fixedRate = 300000)
    public void autoAsignarPendientes() {
        long pendientes = alertaRepository.countByEstado("PENDIENTE");
        if (pendientes > 0) {
            log.info("[SCHEDULER] {} alertas pendientes, ejecutando auto-asignacion", pendientes);
            assignmentEngine.rebalancearTodos();
        }
    }

    @Scheduled(fixedRate = 60000)
    public void verificarRebalanceo() {
        log.debug("[SCHEDULER] Verificando necesidad de rebalanceo...");
    }
}
