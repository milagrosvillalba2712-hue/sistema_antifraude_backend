package com.antifraude.config;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;

@Configuration
public class DroolsConfig {

    private static final Logger log = LoggerFactory.getLogger(DroolsConfig.class);
    private static final String RULES_PATH = "rules/";

    @Bean
    public KieContainer kieContainer() throws IOException {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:" + RULES_PATH + "**/*.drl");
        for (Resource resource : resources) {
            String path = resource.getURI().toString();
            int idx = path.indexOf("rules/");
            String relativePath = idx >= 0 ? path.substring(idx) : "rules/" + resource.getFilename();
            // fraud-rules.drl es el conjunto legacy de respaldo y duplica monto,
            // internacionalidad y horario ya cubiertos por rules/domain.
            if ("rules/fraud-rules.drl".equals(relativePath)) {
                log.info("[DROOLS] Omitiendo reglas fallback legacy duplicadas: {}", relativePath);
                continue;
            }
            log.info("[DROOLS] Cargando regla: {}", relativePath);
            kieFileSystem.write(ResourceFactory.newClassPathResource(relativePath, "UTF-8"));
        }
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();
        KieModule kieModule = kieBuilder.getKieModule();
        log.info("[DROOLS] KieContainer construido - {} reglas cargadas", resources.length);
        return kieServices.newKieContainer(kieModule.getReleaseId());
    }
}
