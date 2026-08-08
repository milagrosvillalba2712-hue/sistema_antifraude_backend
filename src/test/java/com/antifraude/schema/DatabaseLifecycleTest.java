package com.antifraude.schema;

import db.productmigration.V1__Canonical_core_and_transactions;
import db.productmigration.V2__Canonical_ecosystem;
import db.productmigration.V3__Canonical_hardening;
import db.productmigration.V4__Canonical_jpa_alignment;
import db.productmigration.V5__Local_installation_and_licensing;
import db.productmigration.V6__External_api_audit;
import db.productmigration.V7__Remove_external_response_payload;
import db.productmigration.V8__Preauthenticated_user_tenant_lookup;
import db.productmigration.V9__Canonical_fk_indexes;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseLifecycleTest {

    @Test
    void perfilDemoCargaSeedsEnUbicacionSeparada() throws Exception {
        try (PostgreSQLContainer<?> postgres = database("regula_demo")) {
            postgres.start();
            Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/productmigration", "classpath:db/demo")
                    .load()
                    .migrate();

            assertThat(queryInt(postgres, "select count(*) from empresa where codigo='FINANCIERA_SANTA_CLARA'"))
                    .isEqualTo(1);
            assertThat(queryInt(postgres, "select count(*) from pais where codigo_iso='PY'"))
                    .isEqualTo(1);
            assertThat(queryInt(postgres, "select count(*) from moneda where codigo_iso='PYG'"))
                    .isEqualTo(1);
            assertThat(queryInt(postgres, "select count(*) from usuarios where email like '%@demo.regula.local' or email like '%@cliente.local'"))
                    .isZero();
            assertThat(queryInt(postgres, "select count(*) from usuarios where email like '%@santaclara.local'"))
                    .isEqualTo(29);
            assertThat(queryInt(postgres, "select count(*) from usuario_empresa"))
                    .isEqualTo(32);
            assertThat(queryInt(postgres, "select count(*) from transacciones where codigo like 'TX-CONTROL-%'"))
                    .isEqualTo(7);
            assertThat(queryInt(postgres, "select count(*) from alertas_antifraude where codigo like 'ALT-TX-CONTROL-%'"))
                    .isEqualTo(6);
            assertThat(queryInt(postgres, "select count(*) from caso where codigo like 'CAS-ALT-TX-CONTROL-%'"))
                    .isEqualTo(6);
            assertThat(queryInt(postgres, "select count(*) from reportes_ros where codigo='ROS-CONTROL-001'"))
                    .isEqualTo(1);
            assertThat(queryInt(postgres, "select count(*) from licencia_local"))
                    .isEqualTo(1);
            assertThat(queryInt(postgres, "select count(*) from transacciones"))
                    .isEqualTo(87);
            assertThat(queryInt(postgres, "select count(*) from plan_licencia"))
                    .isEqualTo(3);
            assertThat(queryInt(postgres, "select count(*) from consultas_externas"))
                    .isEqualTo(12);
        }
    }

    @Test
    void actualizaDesdeLaUltimaBaselineSoportada() throws Exception {
        try (PostgreSQLContainer<?> postgres = database("regula_upgrade")) {
            postgres.start();

            migrate(postgres,
                    new V1__Canonical_core_and_transactions(),
                    new V2__Canonical_ecosystem(),
                    new V3__Canonical_hardening(),
                    new V4__Canonical_jpa_alignment());
            assertThat(historyCount(postgres)).isEqualTo(4);

            migrate(postgres,
                    new V1__Canonical_core_and_transactions(),
                    new V2__Canonical_ecosystem(),
                    new V3__Canonical_hardening(),
                    new V4__Canonical_jpa_alignment(),
                    new V5__Local_installation_and_licensing(),
                    new V6__External_api_audit(),
                    new V7__Remove_external_response_payload(),
                    new V8__Preauthenticated_user_tenant_lookup(),
                    new V9__Canonical_fk_indexes());

            assertThat(historyCount(postgres)).isEqualTo(9);
            assertThat(tableExists(postgres, "licencia_local")).isTrue();
            assertThat(queryInt(postgres, "select count(*) from information_schema.columns where table_name='consultas_externas' and column_name='respuesta_json'"))
                    .isZero();
            assertThat(indexExists(postgres, "ix_evidencia_caso")).isTrue();
            assertThat(indexExists(postgres, "ix_sujeto_riesgo_alias_sujeto")).isTrue();
        }
    }

    @Test
    void backupYRestauracionConservanEsquemaYDatos() throws Exception {
        try (PostgreSQLContainer<?> postgres = database("regula_restore")) {
            postgres.start();
            migrate(postgres,
                    new V1__Canonical_core_and_transactions(),
                    new V2__Canonical_ecosystem(),
                    new V3__Canonical_hardening(),
                    new V4__Canonical_jpa_alignment(),
                    new V5__Local_installation_and_licensing(),
                    new V6__External_api_audit(),
                    new V7__Remove_external_response_payload(),
                    new V8__Preauthenticated_user_tenant_lookup(),
                    new V9__Canonical_fk_indexes());

            execute(postgres, "insert into empresa(id,codigo,nombre,ruc,estado) values " +
                    "('00000000-0000-0000-0000-000000000099','BACKUP_TEST','Backup test','80000099-9','ACTIVA')");

            assertSuccessful(postgres.execInContainer("pg_dump", "-U", postgres.getUsername(),
                    "-d", postgres.getDatabaseName(), "-Fc", "-f", "/tmp/regula.dump"));
            execute(postgres, "drop schema public cascade; create schema public");
            assertThat(tableExists(postgres, "empresa")).isFalse();
            assertSuccessful(postgres.execInContainer("pg_restore", "-U", postgres.getUsername(),
                    "-d", postgres.getDatabaseName(), "--no-owner", "--no-acl", "/tmp/regula.dump"));

            assertThat(tableExists(postgres, "empresa")).isTrue();
            assertThat(queryInt(postgres, "select count(*) from empresa where codigo='BACKUP_TEST'"))
                    .isEqualTo(1);
            assertThat(historyCount(postgres)).isEqualTo(9);
        }
    }

    private static PostgreSQLContainer<?> database(String name) {
        return new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName(name)
                .withUsername("regula_owner")
                .withPassword("regula_test_password");
    }

    private static void migrate(PostgreSQLContainer<?> postgres,
                                org.flywaydb.core.api.migration.JavaMigration... migrations) {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/no-automatic-discovery")
                .javaMigrations(migrations)
                .load()
                .migrate();
    }

    private static int historyCount(PostgreSQLContainer<?> postgres) throws Exception {
        return queryInt(postgres, "select count(*) from flyway_schema_history where success");
    }

    private static boolean tableExists(PostgreSQLContainer<?> postgres, String table) throws Exception {
        return queryInt(postgres, "select count(*) from information_schema.tables where table_schema='public' and table_name='" + table + "'") == 1;
    }

    private static boolean indexExists(PostgreSQLContainer<?> postgres, String index) throws Exception {
        return queryInt(postgres, "select count(*) from pg_indexes where schemaname='public' and indexname='" + index + "'") == 1;
    }

    private static int queryInt(PostgreSQLContainer<?> postgres, String sql) throws Exception {
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var statement = connection.createStatement();
             var result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }

    private static void execute(PostgreSQLContainer<?> postgres, String sql) throws Exception {
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static void assertSuccessful(Container.ExecResult result) {
        assertThat(result.getExitCode()).describedAs(result.getStderr()).isZero();
    }
}
