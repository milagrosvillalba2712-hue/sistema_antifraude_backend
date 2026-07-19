package com.antifraude.assignment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AssignmentScheduler {

    private static final Logger log = LoggerFactory.getLogger(AssignmentScheduler.class);

    @Scheduled(fixedRate = 60000)
    public void verificarRebalanceo() {
        log.debug("[SCHEDULER] Verificando necesidad de rebalanceo...");
    }
}
