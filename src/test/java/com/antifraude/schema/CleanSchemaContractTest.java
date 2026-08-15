package com.antifraude.schema;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CleanSchemaContractTest {

    private final String schemaSql = read("src/main/resources/db/clean-schema/V10__clean_core_schema_paraguay.sql");
    private final String verificationSql = read("src/main/resources/db/clean-schema/V11__clean_schema_verification.sql");

    @Test
    void transaccionesTieneTenantParticionRlsYPkCompuesta() {
        assertThat(schemaSql).contains("empresa_id uuid not null");
        assertThat(schemaSql).contains("primary key (id, fecha_transaccion)");
        assertThat(schemaSql).contains("partition by range (fecha_transaccion)");
        assertThat(schemaSql).contains("alter table transacciones enable row level security");
        assertThat(schemaSql).contains("current_setting('app.current_empresa_id', true)::uuid");
        assertThat(schemaSql).contains("create table if not exists transacciones_default partition of transacciones default");
    }

    @Test
    void tablasHijasUsanFechaTransaccionYFkCompuesta() {
        for (String table : List.of("alertas_antifraude", "ejecucion_reglas", "evaluaciones_riesgo")) {
            assertThat(schemaSql).contains("create table if not exists " + table);
        }
        assertThat(schemaSql).contains("foreign key (transaccion_id, fecha_transaccion)");
        assertThat(schemaSql).contains("references transacciones(id, fecha_transaccion) on delete restrict on update cascade");
        assertThat(schemaSql).contains("create or replace function fn_set_fecha_transaccion()");
        assertThat(schemaSql).contains("before insert on alertas_antifraude");
        assertThat(schemaSql).contains("before insert on ejecucion_reglas");
        assertThat(schemaSql).contains("before insert on evaluaciones_riesgo");
    }

    @Test
    void tablasCrudTienenAuditoriaYTriggerComun() {
        assertThat(schemaSql).contains("create or replace function fn_set_audit_fields()");
        assertThat(schemaSql).contains("fecha_hora_creacion timestamptz not null default now()");
        assertThat(schemaSql).contains("fecha_hora_modificacion timestamptz");
        assertThat(schemaSql).contains("usuario_creacion_id uuid references usuarios(id) on delete restrict on update cascade");
        assertThat(schemaSql).contains("usuario_modificacion_id uuid references usuarios(id) on delete restrict on update cascade");
        assertThat(verificationSql).contains("vw_crud_audit_columns_control");
    }

    @Test
    void piiSeDefineComoCifradoYHashNoTextoPlano() {
        for (String column : List.of(
                "documento_remitente_enc bytea",
                "documento_remitente_hash bytea",
                "documento_beneficiario_enc bytea",
                "documento_beneficiario_hash bytea",
                "cuenta_origen_enc bytea",
                "cuenta_origen_hash bytea",
                "cuenta_destino_hash bytea",
                "pan_token_hash bytea",
                "qr_payload_hash bytea")) {
            assertThat(schemaSql).contains(column);
        }
        assertThat(schemaSql).doesNotContain("pan_completo");
        assertThat(schemaSql).contains("aes-256-gcm");
        assertThat(schemaSql).contains("hmac-sha256");
    }

    private static String read(String path) {
        try {
            return Files.readString(Path.of(path)).toLowerCase();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer " + path, e);
        }
    }
}
