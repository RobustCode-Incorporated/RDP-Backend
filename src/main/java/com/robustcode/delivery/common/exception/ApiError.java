package com.robustcode.delivery.common.exception;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Error body returned by every failing API call:
 * {@code {status, error, message, timestamp, path, fieldErrors?}}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(

        int status,

        String error,

        String message,

        Instant timestamp,

        String path,

        Map<String, String> fieldErrors

) {

    public static ApiError of(HttpStatusCode status, String message, String path) {
        return of(status, message, path, null);
    }

    public static ApiError of(
            HttpStatusCode status,
            String message,
            String path,
            Map<String, String> fieldErrors
    ) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        String reason = resolved != null ? resolved.getReasonPhrase() : String.valueOf(status.value());

        return new ApiError(
                status.value(),
                reason,
                message,
                Instant.now(),
                path,
                fieldErrors
        );
    }
}
