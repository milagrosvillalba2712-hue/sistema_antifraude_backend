package com.antifraude.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String field;
    private final Object value;

    public ResourceNotFoundException(String resourceName, String field, Object value) {
        super(String.format("%s no encontrado con %s: '%s'", resourceName, field, value));
        this.resourceName = resourceName;
        this.field = field;
        this.value = value;
    }
}
