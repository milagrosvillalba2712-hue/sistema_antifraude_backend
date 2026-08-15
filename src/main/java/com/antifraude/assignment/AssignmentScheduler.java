package com.antifraude.assignment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true")
public class AssignmentScheduler {

    private static final Logger log = LoggerFactory.getLogger(AssignmentScheduler.class);

    @Scheduled(fixedRate = 60000)
    public void verificarRebalanceo() {
        log.debug("[SCHEDULER] Verificando necesidad de rebalanceo...");
    }
}
