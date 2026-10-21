package com.robustcode.delivery.common.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * Cross-field validation failure detected in a service (400). Rendered like a
 * bean-validation error: message "Validation failed" plus {@code fieldErrors}.
 */
@Getter
public class FieldValidationException extends ApiException {

    private final Map<String, String> fieldErrors;

    public FieldValidationException(Map<String, String> fieldErrors) {
        super(HttpStatus.BAD_REQUEST, "Validation failed");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }
}
