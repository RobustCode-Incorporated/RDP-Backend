package com.robustcode.delivery.common.exception;

import org.springframework.http.HttpStatus;

/**
 * The caller is authenticated but not allowed to access the resource (403).
 */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
