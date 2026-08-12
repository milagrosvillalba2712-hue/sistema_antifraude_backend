package com.antifraude.ruleengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineCatalogosContractTest {

    @Test
    void todoCatalogoDeFactsEsResolublePorElRegistroDeEntidades() throws Exception {
        RuleEngineEntityController entityController = new RuleEngineEntityController(null, null, null,
                new ObjectMapper());
        Method buildEntities = RuleEngineEntityController.class.getDeclaredMethod("buildEntities");
        buildEntities.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Class<?>> entities = (Map<String, Class<?>>) buildEntities.invoke(entityController);

        Set<String> catalogos = new RuleEngineFactsController().listarFacts().getBody().stream()
                .map(RuleEngineFactsController.FactDefinition::catalogo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        assertThat(catalogos).allMatch(entities::containsKey);
    }
}
