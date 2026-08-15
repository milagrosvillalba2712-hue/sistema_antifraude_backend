package com.antifraude.licensing;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Profile("demo")
public class DemoSeedTimeRefresher implements ApplicationRunner {

    private static final UUID EMPRESA_DEMO = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final JdbcTemplate jdbcTemplate;

    public DemoSeedTimeRefresher(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        jdbcTemplate.queryForObject("select set_config('app.current_empresa_id', ?, true)", Object.class,
                EMPRESA_DEMO.toString());
        reAnchor("scl-ext-%", "now() - ((24 - substring(correlation_id from 'scl-ext-([0-9]+)')::int) * interval '7 minutes')");
        reAnchor("scl-api-%", "now() - ((60 - substring(correlation_id from 'scl-api-([0-9]+)')::int) * interval '5 minutes')");
        reAnchor("seed-ext-%", "now() - ((12 - substring(correlation_id from 'seed-ext-([0-9]+)')::int) * interval '1 hour')");
    }

    private void reAnchor(String prefix, String fechaExpr) {
        jdbcTemplate.update("""
                update api_evento
                set fecha_evento = %s
                where correlation_id like ?
                """.formatted(fechaExpr), prefix);
    }
}