package com.robustcode.delivery.common.exception;

import org.springframework.http.HttpStatus;

/**
 * A requested resource does not exist (404).
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
