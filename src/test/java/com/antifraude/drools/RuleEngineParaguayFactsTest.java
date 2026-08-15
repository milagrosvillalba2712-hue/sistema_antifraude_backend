package com.antifraude.drools;

import com.antifraude.drools.fact.TransaccionFact;
import com.antifraude.ruleengine.RuleEngineFactsController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineParaguayFactsTest {

    @Test
    void endpointExponeFactsLocalesParaguay() {
        RuleEngineFactsController controller = new RuleEngineFactsController();

        Set<String> facts = controller.listarFacts().getBody().stream()
                .map(RuleEngineFactsController.FactDefinition::fact)
                .collect(Collectors.toSet());

        assertThat(facts).contains(
                "tipoTransaccion",
                "infraestructuraPago",
                "moduloSipap",
                "esSpi",
                "esLbtr",
                "esEmpe",
                "esQr",
                "esTarjeta",
                "esCheque",
                "esEfectivo",
                "esRemesa",
                "esFx",
                "declaracionFondos",
                "depositanteTercero",
                "empeOperador",
                "procesadoraTarjeta",
                "mcc",
                "paisCorredorRemesa");
    }

    @Test
    void evaluatorEvaluaFactsLocalesParaguay() {
        TransaccionFact tx = new TransaccionFact();
        tx.setMonto(new BigDecimal("250000000"));
        tx.setMonedaCodigo("PYG");
        tx.setTipoTransaccion("PY_SPI_ALIAS_TRANSFER");
        tx.setInfraestructuraPago("SPI");
        tx.setModuloSipap("SPI");
        tx.setRequiereDeclaracionFondos(true);

        RiskContext context = new RiskContext();
        context.setTransaccionFact(tx);

        ConditionEvaluator evaluator = new ConditionEvaluator(new ObjectMapper());
        boolean result = evaluator.evaluate("""
                {"combinador":"ALL","items":[
                  {"fact":"esSpi","operador":"==","valor":true},
                  {"fact":"declaracionFondos","operador":"==","valor":true},
                  {"fact":"monto","operador":">","valor":100000000}
                ]}
                """, context);

        assertThat(result).isTrue();
    }
}
