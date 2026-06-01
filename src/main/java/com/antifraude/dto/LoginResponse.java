package com.antifraude.dto;

public record LoginResponse(String token, String tipo, String email, String rol) {
}
