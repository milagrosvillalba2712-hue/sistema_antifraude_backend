package com.antifraude.drools;

import com.antifraude.alerts.AlertaService;
import com.antifraude.rules.EjecucionRegla;
import com.antifraude.rules.EjecucionReglaRepository;
import com.antifraude.rules.ReglaRiesgo;
import com.antifraude.rules.ReglaRiesgoService;
import com.antifraude.transactions.Transaccion;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DroolsService {

    private static final Logger log = LoggerFactory.getLogger(DroolsService.class);

    private final KieContainer kieContainer;
    private final ReglaRiesgoService reglaRiesgoService;
    private final AlertaService alertaService;
    private final EjecucionReglaRepository ejecucionReglaRepository;
    private final ConditionEvaluator conditionEvaluator;

    public DroolsService(KieContainer kieContainer, ReglaRiesgoService reglaRiesgoService,
                         AlertaService alertaService,
                         EjecucionReglaRepository ejecucionReglaRepository,
                         ConditionEvaluator conditionEvaluator) {
        this.kieContainer = kieContainer;
        this.reglaRiesgoService = reglaRiesgoService;
        this.alertaService = alertaService;
        this.ejecucionReglaRepository = ejecucionReglaRepository;
        this.conditionEvaluator = conditionEvaluator;
    }

    /**
     * Evalúa riesgo usando RiskContext completo (nueva arquitectura).
     * Pre-calcula todo, Drools solo consume RiskContext.
     */
    public RiskResult evaluar(RiskContext context) {
        log.debug("[DROOLS] Evaluando transaccion UUID: {}", context.getTransaccionFact().getTransactionUuid());
        KieSession kieSession = kieContainer.newKieSession();
        try {
            ScoreTracker tracker = new ScoreTracker();
            kieSession.setGlobal("scoreTracker", tracker);

            kieSession.insert(context);

            kieSession.fireAllRules();
            BigDecimal score = tracker.getScore();
            List<RiskResult.ReglaDisparada> reglasDisparadas = evaluarReglasGuiadas(context);
            BigDecimal scoreGuiado = reglasDisparadas.stream()
                    .map(RiskResult.ReglaDisparada::score)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            score = score.add(scoreGuiado);

            if (score.compareTo(BigDecimal.ZERO) == 0) {
                score = calcularScoreDefault(context.getTransaccionFact());
                log.debug("[DROOLS] Score default aplicado: {}", score);
            }

            String nivel = calcularNivel(score);
            log.info("[DROOLS] Score final para UUID: {} - Score: {} - Nivel: {}",
                    context.getTransaccionFact().getTransactionUuid(), score, nivel);

            if (score.compareTo(new BigDecimal("70")) >= 0) {
                log.warn("[DROOLS] Score alto detectado - UUID: {} - Score: {} - Generando alertas",
                        context.getTransaccionFact().getTransactionUuid(), score);
                crearAlertasDesdeResultado(context.getTransaccion(), reglasDisparadas, score, nivel);
            }

            return new RiskResult(score, nivel, reglasDisparadas, score.compareTo(new BigDecimal("70")) >= 0, null);
        } finally {
            kieSession.dispose();
        }
    }

    private List<RiskResult.ReglaDisparada> evaluarReglasGuiadas(RiskContext context) {
        List<RiskResult.ReglaDisparada> reglasDisparadas = new ArrayList<>();
        List<ReglaRiesgo> reglasActivas = reglaRiesgoService.listarActivas();
        for (ReglaRiesgo regla : reglasActivas) {
            long inicio = System.currentTimeMillis();
            boolean cumplida = conditionEvaluator.evaluate(regla.getCondicionesJson(), context);
            BigDecimal score = cumplida && regla.getScoreBase() != null ? regla.getScoreBase() : BigDecimal.ZERO;
            registrarEjecucion(context, regla, cumplida, score, System.currentTimeMillis() - inicio);
            if (cumplida) {
                reglasDisparadas.add(new RiskResult.ReglaDisparada(
                        regla.getId(),
                        regla.getCodigo(),
                        regla.getNombre(),
                        score,
                        regla.getSeveridad(),
                        regla.getAccionesJson()));
            }
        }
        return reglasDisparadas;
    }

    private void registrarEjecucion(RiskContext context, ReglaRiesgo regla, boolean cumplida, BigDecimal score, Long tiempoMs) {
        try {
            if (context.getTransaccion() == null || regla == null) {
                return;
            }
            EjecucionRegla ejecucion = EjecucionRegla.builder()
                    .regla(regla)
                    .transaccion(context.getTransaccion())
                    .scoreRegla(score)
                    .cumplida(cumplida)
                    .scoreGenerado(score)
                    .resultadoEvaluacion(cumplida ? "CUMPLIO" : "NO_CUMPLIO")
                    .condicionEvaluada(regla.getCondicion())
                    .tiempoEjecucionMs(tiempoMs)
                    .fechaEjecucion(LocalDateTime.now())
                    .detalle(regla.getAccionesJson())
                    .build();
            ejecucionReglaRepository.save(ejecucion);
            log.debug("[DROOLS] Ejecucion registrada - Regla: {} - Cumplida: {}", regla.getCodigo(), cumplida);
        } catch (Exception e) {
            log.warn("[DROOLS] No se pudo registrar ejecucion: {}", e.getMessage());
        }
    }

    private void crearAlertasDesdeResultado(Transaccion transaccion, List<RiskResult.ReglaDisparada> reglasDisparadas, BigDecimal score, String nivel) {
        if (transaccion == null) {
            log.warn("[DROOLS] No se genero alerta porque la transaccion es nula");
            return;
        }

        String prioridad;
        if ("CRITICO".equals(nivel)) {
            prioridad = "CRITICA";
        } else if ("ALTO".equals(nivel)) {
            prioridad = "ALTA";
        } else {
            prioridad = "MEDIA";
        }

        int alertasCreadas = 0;
        for (RiskResult.ReglaDisparada disparada : reglasDisparadas) {
            if ("ALTA".equals(disparada.severidad()) || "CRITICA".equals(disparada.severidad())) {
                ReglaRiesgo regla = reglaRiesgoService.buscarPorId(disparada.reglaId());
                alertaService.crearAlerta(transaccion, regla, prioridad);
                alertasCreadas++;
            }
        }

        if (alertasCreadas == 0) {
            log.warn("[DROOLS] Score alto sin regla guiada critica - Transaccion ID: {} - Score: {} - Generando alerta general",
                    transaccion.getId(), score);
            alertaService.crearAlerta(transaccion, null, prioridad);
        }
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

    private BigDecimal calcularScoreDefault(com.antifraude.drools.fact.TransaccionFact t) {
        BigDecimal score = BigDecimal.ZERO;
        if (t.getMonto() != null && t.getMonto().compareTo(new BigDecimal("10000")) > 0) {
            score = score.add(new BigDecimal("30"));
            log.debug("[DROOLS] +30 por monto alto: {}", t.getMonto());
        }
        if (t.getPaisOrigenCodigo() != null && !t.getPaisOrigenCodigo().equalsIgnoreCase("PRY")) {
            score = score.add(new BigDecimal("20"));
            log.debug("[DROOLS] +20 por pais internacional: {}", t.getPaisOrigenCodigo());
        }
        if (t.getCanalCodigo() != null && "TRANSFERENCIA_INTERNACIONAL".equalsIgnoreCase(t.getCanalCodigo())) {
            score = score.add(new BigDecimal("25"));
            log.debug("[DROOLS] +25 por transferencia internacional");
        }
        return score;
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

    private String calcularNivel(BigDecimal score) {
        if (score.compareTo(new BigDecimal("70")) >= 0) return "CRITICO";
        if (score.compareTo(new BigDecimal("50")) >= 0) return "ALTO";
        if (score.compareTo(new BigDecimal("30")) >= 0) return "MEDIO";
        return "BAJO";
    }

    public static class ScoreTracker {
        private BigDecimal score = BigDecimal.ZERO;
        public void addScore(double valor) { this.score = this.score.add(BigDecimal.valueOf(valor)); }
        public BigDecimal getScore() { return score; }
    }
}
