package com.antifraude.exception;

import lombok.Getter;

@Getter
public class AuthenticationErrorException extends RuntimeException {
    private final String code;

    public AuthenticationErrorException(String code, String message) {
        super(message);
        this.code = code;
    }

    public AuthenticationErrorException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
