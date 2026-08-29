package com.antifraude.drools;

import com.antifraude.alerts.AlertaService;
import com.antifraude.rules.EjecucionRegla;
import com.antifraude.rules.EjecucionReglaRepository;
import com.antifraude.rules.ReglaRiesgo;
import com.antifraude.rules.ReglaRiesgoService;
import com.antifraude.transactions.Transaccion;
import com.antifraude.drools.fact.ControlImporteFact;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
    private final DroolsScoreConfigService droolsScoreConfigService;

    public DroolsService(KieContainer kieContainer, ReglaRiesgoService reglaRiesgoService,
                         AlertaService alertaService,
                         EjecucionReglaRepository ejecucionReglaRepository,
                         ConditionEvaluator conditionEvaluator,
                         DroolsScoreConfigService droolsScoreConfigService) {
        this.kieContainer = kieContainer;
        this.reglaRiesgoService = reglaRiesgoService;
        this.alertaService = alertaService;
        this.ejecucionReglaRepository = ejecucionReglaRepository;
        this.conditionEvaluator = conditionEvaluator;
        this.droolsScoreConfigService = droolsScoreConfigService;
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

            List<RiskResult.ReglaDisparada> reglasDrools = new ArrayList<>();
            kieSession.addEventListener(new DefaultAgendaEventListener() {
                private BigDecimal prevScore = BigDecimal.ZERO;

                @Override
                public void beforeMatchFired(org.kie.api.event.rule.BeforeMatchFiredEvent event) {
                    prevScore = tracker.getScore();
                }

                @Override
                public void afterMatchFired(org.kie.api.event.rule.AfterMatchFiredEvent event) {
                    BigDecimal delta = tracker.getScore().subtract(prevScore);
                    String nombreRegla = event.getMatch().getRule().getName();
                    if (delta.signum() != 0) {
                        reglasDrools.add(toReglaDisparadaDrools(nombreRegla, delta));
                    }
                }
            });

            kieSession.fireAllRules();
            BigDecimal score = tracker.getScore();
            List<RiskResult.ReglaDisparada> reglasGuiadas = evaluarReglasGuiadas(context);
            List<RiskResult.ReglaDisparada> reglasDisparadas = new ArrayList<>(reglasDrools);
            reglasDisparadas.addAll(reglasGuiadas);
            BigDecimal scoreGuiado = reglasGuiadas.stream()
                    .map(RiskResult.ReglaDisparada::score)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            score = score.add(scoreGuiado);

            if (score.compareTo(BigDecimal.ZERO) == 0) {
                score = calcularScoreDefault(context);
                log.debug("[DROOLS] Score default aplicado: {}", score);
            }

            String nivel = calcularNivel(score, context.getConfig());
            boolean requiereAlerta = requiereAlerta(score, reglasDisparadas, context.getConfig());
            log.info("[DROOLS] Score final para UUID: {} - Score: {} - Nivel: {}",
                    context.getTransaccionFact().getTransactionUuid(), score, nivel);

            if (requiereAlerta) {
                log.warn("[DROOLS] Riesgo accionable detectado - UUID: {} - Score: {} - Generando alertas",
                        context.getTransaccionFact().getTransactionUuid(), score);
                crearAlertasDesdeResultado(context.getTransaccion(), reglasDisparadas, score, nivel);
            }

            return new RiskResult(score, nivel, reglasDisparadas, requiereAlerta, null,
                    context.getCoincidenciasListas());
        } finally {
            kieSession.dispose();
        }
    }

    private RiskResult.ReglaDisparada toReglaDisparadaDrools(String nombreRegla, BigDecimal delta) {
        return new RiskResult.ReglaDisparada(
                null,
                "DROOLS",
                codigoRegla(nombreRegla),
                nombreRegla,
                delta,
                severidadRegla(nombreRegla),
                null);
    }

    private String codigoRegla(String nombreRegla) {
        return nombreRegla.toUpperCase(java.util.Locale.ROOT)
                .replaceAll("[^\\p{Alnum}]+", "_")
                .replaceAll("(^_|_$)", "");
    }

    private String severidadRegla(String nombreRegla) {
        if (nombreRegla.contains("negro") || nombreRegla.contains("negra") || nombreRegla.contains("observado")
                || nombreRegla.contains("PEP") || nombreRegla.contains("muy alto")) {
            return "CRITICA";
        }
        if (nombreRegla.contains("alto")) {
            return "ALTA";
        }
        return "MEDIA";
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
                        "CONFIGURABLE",
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
                    .reglaCodigo(regla.getCodigo())
                    .transaccion(context.getTransaccion())
                    .scoreRegla(score)
                    .cumplida(cumplida)
                    .scoreGenerado(score)
                    .resultadoEvaluacion(cumplida ? "CUMPLIO" : "NO_CUMPLIO")
                    .condicionEvaluada(regla.getCondicion())
                    .tiempoEjecucionMs(tiempoMs)
                    .fechaEjecucion(OffsetDateTime.now())
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
        transaccion.setScoreRiesgo(score);

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
            // Las reglas DRL estáticas no tienen id en BD; solo las guiadas (CONFIGURABLE) generan alerta por regla.
            if (disparada.reglaId() == null) {
                continue;
            }
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

    private BigDecimal calcularScoreDefault(RiskContext context) {
        com.antifraude.drools.fact.TransaccionFact t = context.getTransaccionFact();
        BigDecimal score = BigDecimal.ZERO;
        BigDecimal scoreControlImporte = scorePorControlImporte(context);
        if (scoreControlImporte.compareTo(BigDecimal.ZERO) > 0) {
            score = score.add(scoreControlImporte);
            log.debug("[DROOLS] +{} por control de importe multimoneda", scoreControlImporte);
        }
        if (t.isEsInternacional()) {
            score = score.add(new BigDecimal("20"));
            log.debug("[DROOLS] +20 por transaccion internacional: {} -> {}",
                    t.getPaisOrigenCodigo(), t.getPaisDestinoCodigo());
        }
        if (t.getCanalCodigo() != null && "TRANSFERENCIA_INTERNACIONAL".equalsIgnoreCase(t.getCanalCodigo())) {
            score = score.add(new BigDecimal("25"));
            log.debug("[DROOLS] +25 por transferencia internacional");
        }
        return score;
    }

    private BigDecimal scorePorControlImporte(RiskContext context) {
        if (context.getTransaccionFact() == null || context.getTransaccionFact().getMonto() == null
                || context.getTransaccionFact().getMonedaCodigo() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal score = BigDecimal.ZERO;
        String moneda = context.getTransaccionFact().getMonedaCodigo();
        for (ControlImporteFact control : context.getControlesImporte()) {
            if (control.getMonedaCodigo() == null || control.getMontoMaximo() == null) {
                continue;
            }
            if (!control.getMonedaCodigo().equalsIgnoreCase(moneda)) {
                continue;
            }
            if (context.getTransaccionFact().getMonto().compareTo(control.getMontoMaximo()) <= 0) {
                continue;
            }
            String severidad = control.getSeveridad() != null ? control.getSeveridad().toUpperCase() : "MEDIA";
            DroolsScoreConfig config = context.getConfig();
            BigDecimal candidato = "CRITICA".equals(severidad) ? config.getUmbralCritico()
                    : ("ALTA".equals(severidad) ? config.getUmbralAlto() : config.getUmbralMedio());
            if (candidato.compareTo(score) > 0) {
                score = candidato;
            }
        }
        return score;
    }

    private BigDecimal calcularScoreDefault(Transaccion t) {
        BigDecimal score = BigDecimal.ZERO;
        String paisOrigen = t.getPaisOrigen();
        String paisDestino = t.getPaisDestinoRef() != null ? t.getPaisDestinoRef().getCodigoIso() : null;
        if (paisOrigen != null && paisDestino != null && !paisOrigen.equalsIgnoreCase(paisDestino)) {
            score = score.add(new BigDecimal("20"));
            log.debug("[DROOLS] +20 por transaccion internacional: {} -> {}", paisOrigen, paisDestino);
        }
        if (t.getCanal() != null && "TRANSFERENCIA_INTERNACIONAL".equalsIgnoreCase(t.getCanal())) {
            score = score.add(new BigDecimal("25"));
            log.debug("[DROOLS] +25 por transferencia internacional");
        }
        return score;
    }

    private String calcularNivel(BigDecimal score, DroolsScoreConfig config) {
        if (score.compareTo(config.getUmbralCritico()) >= 0) return "CRITICO";
        if (score.compareTo(config.getUmbralAlto()) >= 0) return "ALTO";
        if (score.compareTo(config.getUmbralMedio()) >= 0) return "MEDIO";
        return "BAJO";
    }

    private boolean requiereAlerta(BigDecimal score, List<RiskResult.ReglaDisparada> reglasDisparadas, DroolsScoreConfig config) {
        if (score.compareTo(config.getUmbralCritico()) >= 0) {
            return true;
        }
        return reglasDisparadas.stream().anyMatch(regla -> {
            String severidad = regla.severidad();
            return severidad != null && (severidad.equalsIgnoreCase("ALTA")
                    || severidad.equalsIgnoreCase("ALTO")
                    || severidad.equalsIgnoreCase("CRITICA")
                    || severidad.equalsIgnoreCase("CRÍTICA")
                    || severidad.equalsIgnoreCase("CRITICO")
                    || severidad.equalsIgnoreCase("CRÍTICO"));
        });
    }

    public static class ScoreTracker {
        private BigDecimal score = BigDecimal.ZERO;
        public void addScore(double valor) { this.score = this.score.add(BigDecimal.valueOf(valor)); }
        public void addScore(BigDecimal valor) { this.score = this.score.add(valor); }
        public BigDecimal getScore() { return score; }
    }
}
