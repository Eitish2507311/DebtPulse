package com.debtpulse.field.dto.request;

import java.time.LocalDate;

/**
 * Payload to complete a field visit. {@code visitDate} defaults to today when omitted.
 */
public record CompleteVisitRequest(
        LocalDate visitDate,
        Boolean borrowerMet,
        Boolean assetSighted,
        String outcomeSummary,
        String nextActionRequired
) {}
