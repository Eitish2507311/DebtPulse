package com.debtpulse.auth.exception;

/**
 * Thrown when a caller exceeds an allowed request rate (e.g. forgot-password requests per email).
 * Mapped to HTTP 429 Too Many Requests with a rule code (DP5-39).
 */
public class RateLimitExceededException extends RuntimeException {
    private final String ruleCode;

    public RateLimitExceededException(String message, String ruleCode) {
        super(message);
        this.ruleCode = ruleCode;
    }

    public String getRuleCode() {
        return ruleCode;
    }
}
