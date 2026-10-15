package com.robustcode.delivery.common.exception;

import org.springframework.http.HttpStatus;

/**
 * The request is well-formed but violates a business rule (400).
 */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
