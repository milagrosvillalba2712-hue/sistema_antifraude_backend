package com.antifraude.dto;

import java.util.UUID;

public record AnalistaDisponibleResponse(UUID usuarioId, String nombre, String email, String estado,
                                          long alertasActivas, boolean disponible) {
}
