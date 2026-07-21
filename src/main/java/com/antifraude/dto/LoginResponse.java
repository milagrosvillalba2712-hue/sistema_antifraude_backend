package com.antifraude.dto;

import java.util.List;

public record LoginResponse(String token, String tipo, Long usuarioId, String email, String rol,
                            Long empresaId, Long rolId, List<String> permisos) {
}
