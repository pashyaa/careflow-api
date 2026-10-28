package com.careflow.serviceops.exception;

import java.util.Map;

/** Business-level validation failure that maps to HTTP 400 with per-field errors. */
public class FieldValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public FieldValidationException(Map<String, String> errors) {
        super("One or more request fields are invalid.");
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}