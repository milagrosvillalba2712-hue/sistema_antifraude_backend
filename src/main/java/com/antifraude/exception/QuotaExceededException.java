package com.antifraude.exception;

import lombok.Getter;

@Getter
public class QuotaExceededException extends RuntimeException {

    private final String code;

    public QuotaExceededException(String code, String message) {
        super(message);
        this.code = code;
    }
}