package com.antifraude.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(2)
public class SqlSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SqlSeedRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;

    public SqlSeedRunner(JdbcTemplate jdbcTemplate, ResourceLoader resourceLoader) {
        this.jdbcTemplate = jdbcTemplate;
        this.resourceLoader = resourceLoader;
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM nivel_riesgo", Integer.class);
            if (count != null && count > 0) {
                log.info("[SEED] Datos de catalogo ya existen, omitiendo...");
                return;
            }
        } catch (Exception e) {
            log.warn("[SEED] No se pudo verificar existencia de datos: {}", e.getMessage());
        }

        log.info("[SEED] Ejecutando V2__seed.sql (usuarios)...");
        executeSqlFile("classpath:db/migration/V2__seed.sql");

        log.info("[SEED] Ejecutando V3__seed_catalog.sql (catalogo y datos de prueba)...");
        executeSqlFile("classpath:db/migration/V3__seed_catalog.sql");

        log.info("[SEED] Inicializacion completada exitosamente");
    }

    private void executeSqlFile(String location) {
        try {
            Resource resource = resourceLoader.getResource(location);
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(resource);
            populator.setSeparator(";");
            populator.setContinueOnError(true);
            populator.setIgnoreFailedDrops(true);
            DatabasePopulatorUtils.execute(populator, jdbcTemplate.getDataSource());
        } catch (Exception e) {
            log.warn("[SEED] Error ejecutando {}: {}", location, e.getMessage());
        }
    }
}
