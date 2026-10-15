package com.robustcode.delivery.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * Base class for business exceptions that map to a specific HTTP status.
 * The message is returned to the client, so it must never contain secrets.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
