package com.antifraude.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

@Component
@Order(-10)
@ConditionalOnProperty(name = "app.clean-schema.apply", havingValue = "true")
public class CleanSchemaRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CleanSchemaRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;

    public CleanSchemaRunner(JdbcTemplate jdbcTemplate, ResourceLoader resourceLoader) {
        this.jdbcTemplate = jdbcTemplate;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(String... args) {
        log.warn("[CLEAN-SCHEMA] Aplicando esquema limpio Regula AML Paraguay sobre el datasource configurado");
        execute("classpath:db/clean-schema/V10__clean_core_schema_paraguay.sql");
        execute("classpath:db/clean-schema/V12__clean_ecosystem_schema.sql");
        execute("classpath:db/clean-schema/V15__clean_post_audit_hardening.sql");
        execute("classpath:db/clean-schema/V11__clean_schema_verification.sql");
        execute("classpath:db/clean-schema/V14__clean_ecosystem_verification.sql");
        execute("classpath:db/clean-schema/V17__clean_jpa_alignment.sql");
        Integer missingAuditColumns = jdbcTemplate.queryForObject(
                "select count(*) from vw_crud_audit_columns_control where existe = false", Integer.class);
        Integer missingRls = jdbcTemplate.queryForObject(
                "select count(*) from vw_rls_control where rls_habilitado = false or total_policies = 0", Integer.class);
        Integer invalidFk = jdbcTemplate.queryForObject(
                "select count(*) from vw_fk_compuesta_transaccion_control where columnas_fk <> 2", Integer.class);
        Integer piiColumns = jdbcTemplate.queryForObject(
                "select count(*) from vw_pii_columns_control", Integer.class);

        if (missingAuditColumns != null && missingAuditColumns > 0) {
            throw new IllegalStateException("Faltan columnas de auditoria CRUD en el esquema limpio");
        }
        if (missingRls != null && missingRls > 0) {
            throw new IllegalStateException("Faltan policies RLS en tablas tenant");
        }
        if (invalidFk != null && invalidFk > 0) {
            throw new IllegalStateException("Existen FKs a transacciones sin clave compuesta");
        }
        if (piiColumns == null || piiColumns < 16) {
            throw new IllegalStateException("Faltan columnas PII *_enc/*_hash esperadas en transacciones");
        }
        log.warn("[CLEAN-SCHEMA] Esquema limpio aplicado y verificado correctamente");
    }

    private void execute(String location) {
        try {
            Resource resource = resourceLoader.getResource(location);
            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo ejecutar " + location, e);
        }
    }
}
