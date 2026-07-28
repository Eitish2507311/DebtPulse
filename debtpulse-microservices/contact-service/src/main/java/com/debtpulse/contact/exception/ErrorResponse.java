package com.debtpulse.contact.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Standard error envelope returned by every REST endpoint in this service.
 * Null fields (ruleCode, fieldErrors) are omitted from the JSON payload.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        String ruleCode,
        Map<String, String> fieldErrors
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(OffsetDateTime.now(), status, error, message, path, null, null);
    }

    public static ErrorResponse withRule(int status, String error, String message, String path, String ruleCode) {
        return new ErrorResponse(OffsetDateTime.now(), status, error, message, path, ruleCode, null);
    }

    public static ErrorResponse withFieldErrors(int status, String error, String message, String path,
                                                Map<String, String> fieldErrors) {
        return new ErrorResponse(OffsetDateTime.now(), status, error, message, path, null, fieldErrors);
    }
}
