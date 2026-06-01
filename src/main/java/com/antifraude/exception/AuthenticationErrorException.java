package com.antifraude.exception;

public class AuthenticationErrorException extends RuntimeException {

    public AuthenticationErrorException(String message) {
        super(message);
    }

    public AuthenticationErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
