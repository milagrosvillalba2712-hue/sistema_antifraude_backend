package com.antifraude.dto;

import java.util.List;
import java.util.UUID;

public record LoginResponse(String token, String tipo, UUID usuarioId, String email, String rol,
                            UUID empresaId, Long rolId, List<String> permisos) {
}
