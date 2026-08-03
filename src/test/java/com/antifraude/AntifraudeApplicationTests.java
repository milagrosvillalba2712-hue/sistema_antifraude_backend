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
import com.antifraude.external.BcpSancionesClient;
import com.antifraude.external.ExternalProviderException;
import com.antifraude.external.NonRetryableExternalException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.RecordedRequest;

import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"app.seed.enabled=false", "app.scheduler.enabled=false"})
@Testcontainers
class AntifraudeApplicationTests {

    static final MockWebServer EXTERNAL = startExternalServer();

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
        registry.add("app.external.identificaciones.url", () -> EXTERNAL.url("/").toString());
        registry.add("app.external.sanciones.url", () -> EXTERNAL.url("/").toString());
        registry.add("app.external.pep.url", () -> EXTERNAL.url("/").toString());
        registry.add("app.external.identificaciones.api-key", () -> "backend-contract-key");
        registry.add("app.external.sanciones.api-key", () -> "backend-contract-key");
        registry.add("app.external.pep.api-key", () -> "backend-contract-key");
    }

    @Autowired
    private AlertaService alertaService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired private BcpSancionesClient sancionesClient;
    @Autowired private CircuitBreakerRegistry circuitBreakers;

    @Test
    void contextLoads() {
    }

    @Test
    void alertaFiltrosCargaSinError() {
        alertaService.obtenerFiltros();
    }

    @Test
    void baselineCanonicaTieneSeisVersionesUnicas() {
        Integer total = jdbcTemplate.queryForObject("select count(*) from flyway_schema_history where success", Integer.class);
        Integer duplicadas = jdbcTemplate.queryForObject("""
                select count(*) from (
                    select version from flyway_schema_history
                    where version is not null group by version having count(*) > 1
                ) d
                """, Integer.class);
        assertThat(total).isEqualTo(6);
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

    @Test
    void clienteExternoPropagaCorrelationIdYDeserializaContrato() throws Exception {
        circuitBreakers.circuitBreaker("sanciones").reset();
        var result = sancionesClient.consultar("200");
        var request = EXTERNAL.takeRequest();
        assertThat(result.match()).isTrue();
        assertThat(request.getHeader("X-API-Key")).isEqualTo("backend-contract-key");
        assertThat(request.getHeader("X-Correlation-Id")).isNotBlank();
        assertThat(request.getHeader("Authorization")).isNull();
    }

    @Test
    void retrySoloSeAplicaAErroresTransitorios() throws Exception {
        circuitBreakers.circuitBreaker("sanciones").reset();
        int before = EXTERNAL.getRequestCount();
        assertThat(sancionesClient.consultar("retry").match()).isFalse();
        assertThat(EXTERNAL.getRequestCount() - before).isEqualTo(3);

        circuitBreakers.circuitBreaker("sanciones").reset();
        before = EXTERNAL.getRequestCount();
        assertThatThrownBy(() -> sancionesClient.consultar("unauthorized"))
                .isInstanceOf(NonRetryableExternalException.class);
        assertThat(EXTERNAL.getRequestCount() - before).isEqualTo(1);
    }

    @Test
    void timeoutTotalPermaneceDebajoDeCincoSegundos() {
        circuitBreakers.circuitBreaker("sanciones").reset();
        long start = System.nanoTime();
        assertThatThrownBy(() -> sancionesClient.consultar("timeout")).isInstanceOf(ExternalProviderException.class);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(elapsedMs).isLessThan(5_000);
    }

    @Test
    void circuitBreakerAbreTrasFallosRepetidos() {
        var circuit = circuitBreakers.circuitBreaker("sanciones");
        circuit.reset();
        assertThatThrownBy(() -> sancionesClient.consultar("always-unavailable"))
                .isInstanceOf(ExternalProviderException.class);
        assertThat(circuit.getState()).isEqualTo(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN);
        assertThatThrownBy(() -> sancionesClient.consultar("always-unavailable"))
                .isInstanceOf(CallNotPermittedException.class);
        circuit.reset();
    }

    private static MockWebServer startExternalServer() {
        try {
            MockWebServer server = new MockWebServer();
            java.util.concurrent.atomic.AtomicInteger retryCalls = new java.util.concurrent.atomic.AtomicInteger();
            server.setDispatcher(new Dispatcher() {
                @Override public MockResponse dispatch(RecordedRequest request) {
                    String path = request.getPath();
                    if (path != null && path.endsWith("/200")) return json(200, true);
                    if (path != null && path.endsWith("/unauthorized")) return new MockResponse().setResponseCode(401);
                    if (path != null && path.endsWith("/always-unavailable")) return new MockResponse().setResponseCode(503);
                    if (path != null && path.endsWith("/retry") && retryCalls.incrementAndGet() < 3)
                        return new MockResponse().setResponseCode(503);
                    if (path != null && path.endsWith("/timeout"))
                        return json(200, false).setHeadersDelay(2, java.util.concurrent.TimeUnit.SECONDS);
                    return json(200, false);
                }
                private MockResponse json(int status, boolean sanctioned) {
                    return new MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json")
                            .setBody("{\"sancionado\":" + sanctioned + ",\"fuente\":\"BCP_SIMULADO\"}");
                }
            });
            server.start();
            return server;
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
