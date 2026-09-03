package com.antifraude.dto;

import com.antifraude.common.entity.ClienteExterno;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;

public record ClienteExternoResponse(
        UUID id,
        String codigo,
        String nombre,
        UUID empresaId,
        String empresaNombre,
        String apiKeyPrefix,
        String apiKeyLast4,
        String[] scopes,
        Integer rateLimitPerMinute,
        OffsetDateTime fechaExpiracion,
        OffsetDateTime fechaUltimoUso,
        boolean activo,
        OffsetDateTime fechaHoraCreacion
) {
    public static ClienteExternoResponse from(ClienteExterno entity) {
        return new ClienteExternoResponse(
                entity.getId(),
                entity.getCodigo(),
                entity.getNombre(),
                entity.getEmpresa() != null ? entity.getEmpresa().getId() : null,
                entity.getEmpresa() != null ? entity.getEmpresa().getNombre() : null,
                entity.getApiKeyPrefix(),
                entity.getApiKeyLast4(),
                entity.getScopes(),
                entity.getRateLimitPerMinute(),
                entity.getFechaExpiracion(),
                entity.getFechaUltimoUso(),
                entity.getActivo(),
                entity.getFechaHoraCreacion()
        );
    }
}
