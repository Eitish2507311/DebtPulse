package com.debtpulse.field.exception;

/** Thrown when a domain/business rule is violated. Mapped to HTTP 400 with a rule code. */
public class BusinessRuleException extends RuntimeException {
    private final String ruleCode;

    public BusinessRuleException(String message, String ruleCode) {
        super(message);
        this.ruleCode = ruleCode;
    }

    public String getRuleCode() {
        return ruleCode;
    }
}
