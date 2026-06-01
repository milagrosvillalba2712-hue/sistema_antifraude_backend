package com.antifraude.drools;

import com.antifraude.alerts.AlertaService;
import com.antifraude.rules.ReglaRiesgo;
import com.antifraude.rules.ReglaRiesgoService;
import com.antifraude.transactions.Transaccion;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DroolsService {

    private static final Logger log = LoggerFactory.getLogger(DroolsService.class);

    private final KieContainer kieContainer;
    private final ReglaRiesgoService reglaRiesgoService;
    private final AlertaService alertaService;

    public DroolsService(KieContainer kieContainer, ReglaRiesgoService reglaRiesgoService,
                         AlertaService alertaService) {
        this.kieContainer = kieContainer;
        this.reglaRiesgoService = reglaRiesgoService;
        this.alertaService = alertaService;
    }

    public BigDecimal evaluarTransaccion(Transaccion transaccion) {
        log.debug("[DROOLS] Evaluando transaccion ID: {} - UUID: {}", transaccion.getId(), transaccion.getTransactionUuid());
        KieSession kieSession = kieContainer.newKieSession();
        try {
            kieSession.insert(transaccion);
            List<ReglaRiesgo> reglasActivas = reglaRiesgoService.listarActivas();
            log.debug("[DROOLS] Reglas activas cargadas: {}", reglasActivas.size());

            for (ReglaRiesgo regla : reglasActivas) {
                kieSession.insert(regla);
            }

            ScoreTracker tracker = new ScoreTracker();
            kieSession.setGlobal("scoreTracker", tracker);
            kieSession.fireAllRules();
            BigDecimal score = tracker.getScore();

            if (score.compareTo(BigDecimal.ZERO) == 0) {
                score = calcularScoreDefault(transaccion);
                log.debug("[DROOLS] Score default aplicado: {}", score);
            }

            log.info("[DROOLS] Score final para transaccion ID: {} - Score: {}", transaccion.getId(), score);

            if (score.compareTo(new BigDecimal("70")) >= 0) {
                log.warn("[DROOLS] Score alto detectado - Transaccion ID: {} - Score: {} - Generando alertas",
                        transaccion.getId(), score);
                for (ReglaRiesgo regla : reglasActivas) {
                    if ("ALTA".equals(regla.getSeveridad())) {
                        alertaService.crearAlerta(transaccion, regla, "ALTA");
                    }
                }
            }
            return score;
        } finally {
            kieSession.dispose();
        }
    }

    private BigDecimal calcularScoreDefault(Transaccion t) {
        BigDecimal score = BigDecimal.ZERO;
        if (t.getMonto() != null && t.getMonto().compareTo(new BigDecimal("10000")) > 0) {
            score = score.add(new BigDecimal("30"));
            log.debug("[DROOLS] +30 por monto alto: {}", t.getMonto());
        }
        if (t.getPaisOrigen() != null && !t.getPaisOrigen().equalsIgnoreCase("NACIONAL")) {
            score = score.add(new BigDecimal("20"));
            log.debug("[DROOLS] +20 por pais internacional: {}", t.getPaisOrigen());
        }
        if (t.getCanal() != null && "TRANSFERENCIA_INTERNACIONAL".equalsIgnoreCase(t.getCanal())) {
            score = score.add(new BigDecimal("25"));
            log.debug("[DROOLS] +25 por transferencia internacional");
        }
        return score;
    }

    public static class ScoreTracker {
        private BigDecimal score = BigDecimal.ZERO;
        public void addScore(double valor) { this.score = this.score.add(BigDecimal.valueOf(valor)); }
        public BigDecimal getScore() { return score; }
    }
}
