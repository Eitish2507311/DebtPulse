package com.debtpulse.analytics.exception;

/** Thrown for invalid credentials or a forbidden action. Mapped to HTTP 403. */
public class UnauthorizedActionException extends RuntimeException {
    public UnauthorizedActionException(String message) {
        super(message);
    }
}
