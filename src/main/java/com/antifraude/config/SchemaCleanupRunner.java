package com.antifraude.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class SchemaCleanupRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaCleanupRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaCleanupRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("""
                    DO $$
                    DECLARE constraint_name text;
                    BEGIN
                        IF EXISTS (
                            SELECT 1 FROM information_schema.columns
                            WHERE table_name = 'usuarios' AND column_name = 'rol'
                        ) THEN
                            FOR constraint_name IN
                                SELECT con.conname
                                FROM pg_constraint con
                                JOIN pg_class rel ON rel.oid = con.conrelid
                                JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY(con.conkey)
                                WHERE rel.relname = 'usuarios'
                                  AND att.attname = 'rol'
                                  AND con.contype = 'c'
                            LOOP
                                EXECUTE format('ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS %I', constraint_name);
                            END LOOP;
                            ALTER TABLE usuarios ALTER COLUMN rol DROP NOT NULL;
                        END IF;
                    END $$;
                    """);
        } catch (Exception e) {
            log.warn("[SCHEMA] No se pudo sanear columna legacy usuarios.rol: {}", e.getMessage());
        }
    }
}
