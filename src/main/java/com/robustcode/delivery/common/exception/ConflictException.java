package com.robustcode.delivery.common.exception;

import org.springframework.http.HttpStatus;

/**
 * The request conflicts with the current state, e.g. a duplicate email (409).
 */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
