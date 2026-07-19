package com.antifraude.dto;

public record AnalistaDisponibleResponse(Long usuarioId, String nombre, String email, String estado,
                                          long alertasActivas, boolean disponible) {
}
