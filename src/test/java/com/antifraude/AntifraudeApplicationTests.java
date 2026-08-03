package com.antifraude;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.antifraude.alerts.AlertaService;

import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"app.seed.enabled=false", "app.scheduler.enabled=false"})
@Testcontainers
class AntifraudeApplicationTests {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("regula_test")
            .withUsername("regula_owner")
            .withPassword("regula_test_password");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private AlertaService alertaService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void alertaFiltrosCargaSinError() {
        alertaService.obtenerFiltros();
    }

    @Test
    void baselineCanonicaTieneCincoVersionesUnicas() {
        Integer total = jdbcTemplate.queryForObject("select count(*) from flyway_schema_history where success", Integer.class);
        Integer duplicadas = jdbcTemplate.queryForObject("""
                select count(*) from (
                    select version from flyway_schema_history
                    where version is not null group by version having count(*) > 1
                ) d
                """, Integer.class);
        assertThat(total).isEqualTo(5);
        assertThat(duplicadas).isZero();
    }

    @Test
    void transaccionesPermaneceParticionadaYRlsForzado() {
        String relkind = jdbcTemplate.queryForObject(
                "select relkind::text from pg_class where oid = 'public.transacciones'::regclass", String.class);
        Boolean rlsForzado = jdbcTemplate.queryForObject(
                "select relrowsecurity and relforcerowsecurity from pg_class where oid = 'public.transacciones'::regclass",
                Boolean.class);
        assertThat(relkind).isEqualTo("p");
        assertThat(rlsForzado).isTrue();
    }

    @Test
    void rolAplicacionNoPuedeLeerOtraEmpresa() {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("insert into empresa(id, codigo, nombre, ruc, estado) values " +
                        "('00000000-0000-0000-0000-000000000001','TENANT_A','Tenant A','80000001-1','ACTIVA')," +
                        "('00000000-0000-0000-0000-000000000002','TENANT_B','Tenant B','80000002-2','ACTIVA')");
                statement.execute("insert into caso(empresa_id,codigo,titulo,estado,severidad) values " +
                        "('00000000-0000-0000-0000-000000000001','CASO-A','Caso A','NUEVO','MEDIA')," +
                        "('00000000-0000-0000-0000-000000000002','CASO-B','Caso B','NUEVO','MEDIA')");
                statement.execute("set role regula_app");
                statement.execute("select set_config('app.current_empresa_id','00000000-0000-0000-0000-000000000001',false)");
                try (var result = statement.executeQuery("select count(*) from caso")) {
                    result.next();
                    assertThat(result.getInt(1)).isEqualTo(1);
                } finally {
                    statement.execute("reset role");
                }
            }
            return null;
        });
    }

    @Test
    void modeloLocalNoContieneDatosTransaccionales() {
        Integer tablas = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema='public' and table_name in
                ('instalacion_local','licencia_local','consumo_licencia_local','evento_licencia_local')
                """, Integer.class);
        Integer columnasProhibidas = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.columns
                where table_schema='public'
                  and table_name in ('instalacion_local','licencia_local','consumo_licencia_local','evento_licencia_local')
                  and column_name in ('documento','numero_documento','transaccion_id','alerta_id','caso_id')
                """, Integer.class);
        assertThat(tablas).isEqualTo(4);
        assertThat(columnasProhibidas).isZero();
    }

    @Test
    void perfilProductivoNoCargaDatosDemo() {
        Integer empresasDemo = jdbcTemplate.queryForObject(
                "select count(*) from empresa where codigo = 'REGULA_DEMO'", Integer.class);
        assertThat(empresasDemo).isZero();
    }
}
