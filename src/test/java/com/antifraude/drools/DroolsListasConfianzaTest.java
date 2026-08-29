package com.antifraude.drools;

import com.antifraude.alerts.AlertaService;
import com.antifraude.drools.fact.ControlImporteFact;
import com.antifraude.drools.fact.ListaFact;
import com.antifraude.drools.fact.TransaccionFact;
import com.antifraude.rules.EjecucionReglaRepository;
import com.antifraude.rules.ReglaRiesgoService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DroolsListasConfianzaTest {

    private static KieContainer container;

    @BeforeAll
    static void buildContainer() throws IOException {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();
        String[] reglas = {
                "riesgo-pais.drl", "riesgo-horario.drl",
                "riesgo-listas.drl", "riesgo-canal.drl", "riesgo-pep.drl", "riesgo-observado.drl"
        };
        for (String nombre : reglas) {
            String rel = "rules/domain/" + nombre;
            try (InputStream in = DroolsListasConfianzaTest.class.getResourceAsStream("/" + rel)) {
                assertThat(in).as("recurso drl %s", rel).isNotNull();
                kfs.write("src/main/resources/com/antifraude/drools/" + nombre,
                        ks.getResources().newByteArrayResource(in.readAllBytes()));
            }
        }
        KieBuilder builder = ks.newKieBuilder(kfs);
        builder.buildAll();
        assertThat(builder.getResults().getMessages())
                .as("compilacion de reglas DRL")
                .filteredOn(m -> m.getLevel() == org.kie.api.builder.Message.Level.ERROR)
                .isEmpty();
        container = ks.newKieContainer(builder.getKieModule().getReleaseId());
    }

    @Test
    void listaNegraConConfianza70EscalaElScoreA70() {
        KieSession session = container.newKieSession();
        try {
            DroolsService.ScoreTracker tracker = new DroolsService.ScoreTracker();
            session.setGlobal("scoreTracker", tracker);

            RiskContext ctx = contextoBase();
            ListaFact negra = new ListaFact();
            negra.setTipoLista("NEGRA");
            negra.setNombreCompleto("SUJETO X");
            negra.setScoreConfianza(BigDecimal.valueOf(70));
            ctx.getListasNegras().add(negra);

            session.insert(ctx);
            session.fireAllRules();

            assertThat(tracker.getScore()).isEqualByComparingTo(new BigDecimal("70.0"));
        } finally {
            session.dispose();
        }
    }

    @Test
    void listaNegraExactaConfianza100ConservaScoreCompleto() {
        KieSession session = container.newKieSession();
        try {
            DroolsService.ScoreTracker tracker = new DroolsService.ScoreTracker();
            session.setGlobal("scoreTracker", tracker);

            RiskContext ctx = contextoBase();
            ListaFact negra = new ListaFact();
            negra.setTipoLista("NEGRA");
            negra.setScoreConfianza(BigDecimal.valueOf(100));
            ctx.getListasNegras().add(negra);

            session.insert(ctx);
            session.fireAllRules();

            assertThat(tracker.getScore()).isEqualByComparingTo(new BigDecimal("100.0"));
        } finally {
            session.dispose();
        }
    }

    @Test
    void droolsServiceCapturaReglasEstaticasConOrigen() {
        ReglaRiesgoService reglaRiesgoService = mock(ReglaRiesgoService.class);
        when(reglaRiesgoService.listarActivas()).thenReturn(List.of());
        DroolsService service = new DroolsService(container, reglaRiesgoService, mock(AlertaService.class),
                mock(EjecucionReglaRepository.class), mock(ConditionEvaluator.class), configService());

        RiskContext ctx = contextoBase();
        ctx.getTransaccionFact().setEsInternacional(true);
        ctx.getTransaccionFact().setPaisOrigenCodigo("BR");
        ctx.getTransaccionFact().setPaisDestinoCodigo("PY");

        RiskResult result = service.evaluar(ctx);

        assertThat(result.scoreTotal()).isEqualByComparingTo(new BigDecimal("30"));
        assertThat(result.reglasDisparadas()).anyMatch(r ->
                "DROOLS".equals(r.origen())
                        && r.codigo().contains("TRANSACCION_INTERNACIONAL")
                        && r.score().compareTo(new BigDecimal("20")) == 0);
        assertThat(result.coincidenciasListas()).isNotNull();
    }

    @Test
    void transferenciaLocalParaguayNoSumaRiesgoPorPais() {
        ReglaRiesgoService reglaRiesgoService = mock(ReglaRiesgoService.class);
        when(reglaRiesgoService.listarActivas()).thenReturn(List.of());
        DroolsService service = new DroolsService(container, reglaRiesgoService, mock(AlertaService.class),
                mock(EjecucionReglaRepository.class), mock(ConditionEvaluator.class), configService());

        RiskContext ctx = contextoBase();
        ctx.getTransaccionFact().setPaisOrigenCodigo("PY");
        ctx.getTransaccionFact().setPaisDestinoCodigo("PY");
        ctx.getTransaccionFact().setEsInternacional(false);

        RiskResult result = service.evaluar(ctx);

        assertThat(result.scoreTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.reglasDisparadas()).isEmpty();
        assertThat(result.requiereAccionInmediata()).isFalse();
    }

    @Test
    void scoreAltoConSoloReglasDroolsNoPersisteAlertasEnEvaluar() {
        ReglaRiesgoService reglaRiesgoService = mock(ReglaRiesgoService.class);
        when(reglaRiesgoService.listarActivas()).thenReturn(List.of());
        AlertaService alertaService = mock(AlertaService.class);
        EjecucionReglaRepository ejecucionRepository = mock(EjecucionReglaRepository.class);
        DroolsService service = new DroolsService(container, reglaRiesgoService, alertaService,
                ejecucionRepository, mock(ConditionEvaluator.class), configService());

        RiskContext ctx = contextoBase();
        ListaFact negra = new ListaFact();
        negra.setTipoLista("NEGRA");
        negra.setScoreConfianza(BigDecimal.valueOf(100));
        ctx.getListasNegras().add(negra);
        // Transaccion sin persistir (simulador): evaluar no debe persistir alertas ni ejecuciones.
        ctx.setTransaccion(new com.antifraude.transactions.Transaccion());

        RiskResult result = service.evaluar(ctx);

        assertThat(result.scoreTotal()).isGreaterThanOrEqualTo(new BigDecimal("70"));
        // La creacion de alertas es responsabilidad del flujo real (transaccion persistida),
        // no de la evaluacion pura: evaluar no debe invocar servicios de persistencia.
        org.mockito.Mockito.verify(alertaService, org.mockito.Mockito.never())
                .crearAlerta(org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyString());
        org.mockito.Mockito.verify(ejecucionRepository, org.mockito.Mockito.never())
                .save(org.mockito.ArgumentMatchers.any());
    }

    private RiskContext contextoBase() {
        RiskContext ctx = new RiskContext();
        TransaccionFact tx = new TransaccionFact();
        tx.setTransactionUuid("uuid-test");
        tx.setMonto(new BigDecimal("1000"));
        ctx.setTransaccionFact(tx);
        ctx.setConfig(scoreConfig());
        return ctx;
    }

    private DroolsScoreConfigService configService() {
        DroolsScoreConfigService svc = mock(DroolsScoreConfigService.class);
        when(svc.getConfig()).thenReturn(scoreConfig());
        return svc;
    }

    private DroolsScoreConfig scoreConfig() {
        DroolsScoreConfig c = new DroolsScoreConfig();
        c.setPepScore(new BigDecimal("40"));
        c.setObservadoScore(new BigDecimal("60"));
        c.setHorarioRiesgoEmpieza(23);
        c.setHorarioRiesgoTermina(5);
        c.setHorarioScore(new BigDecimal("15"));
        c.setPaisInternacionalScore(new BigDecimal("20"));
        c.setPaisAltoRiesgoScore(new BigDecimal("15"));
        c.setPaisDestinoAltoRiesgoScore(new BigDecimal("15"));
        c.setPaisDestinoDistintoScore(new BigDecimal("10"));
        c.setUmbralCritico(new BigDecimal("70"));
        c.setUmbralAlto(new BigDecimal("50"));
        c.setUmbralMedio(new BigDecimal("30"));
        return c;
    }

    private ControlImporteFact controlImporte(String monedaCodigo, String montoMaximo, String severidad) {
        ControlImporteFact control = new ControlImporteFact();
        control.setMonedaCodigo(monedaCodigo);
        control.setMontoMaximo(new BigDecimal(montoMaximo));
        control.setSeveridad(severidad);
        return control;
    }
}
