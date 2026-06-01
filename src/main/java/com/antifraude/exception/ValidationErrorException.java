package com.antifraude.exception;

import lombok.Getter;
import java.util.Map;

@Getter
public class ValidationErrorException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public ValidationErrorException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors;
    }
}
