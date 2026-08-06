package com.antifraude.security.tenant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RlsContextService {

    private final JdbcTemplate jdbcTemplate;

    public RlsContextService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void applyCurrentContext() {
        apply(TenantContext.getEmpresaId(), TenantContext.getUsuarioId());
    }

    public void apply(UUID empresaId, UUID usuarioId) {
        if (empresaId != null) {
            jdbcTemplate.queryForObject("select set_config('app.current_empresa_id', ?, true)", String.class,
                    empresaId.toString());
        }
        if (usuarioId != null) {
            jdbcTemplate.queryForObject("select set_config('app.current_usuario_id', ?, true)", String.class,
                    usuarioId.toString());
        }
    }
}
