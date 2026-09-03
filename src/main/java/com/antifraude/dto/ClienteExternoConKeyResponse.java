package com.antifraude.dto;

import com.antifraude.common.entity.ClienteExterno;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClienteExternoConKeyResponse(
        UUID id,
        String codigo,
        String nombre,
        UUID empresaId,
        String empresaNombre,
        String apiKey,
        String apiKeyPrefix,
        String apiKeyLast4,
        String[] scopes,
        Integer rateLimitPerMinute,
        OffsetDateTime fechaExpiracion,
        boolean activo,
        OffsetDateTime fechaHoraCreacion,
        String mensaje
) {
    public static ClienteExternoConKeyResponse crear(String apiKey, ClienteExterno entity) {
        return new ClienteExternoConKeyResponse(
                entity.getId(),
                entity.getCodigo(),
                entity.getNombre(),
                entity.getEmpresa() != null ? entity.getEmpresa().getId() : null,
                entity.getEmpresa() != null ? entity.getEmpresa().getNombre() : null,
                apiKey,
                entity.getApiKeyPrefix(),
                entity.getApiKeyLast4(),
                entity.getScopes(),
                entity.getRateLimitPerMinute(),
                entity.getFechaExpiracion(),
                entity.getActivo(),
                entity.getFechaHoraCreacion(),
                "Guarda esta API key de forma segura. No se volvera a mostrar."
        );
    }
}
