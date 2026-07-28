package com.debtpulse.field.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read projection of a {@link com.debtpulse.field.entity.FieldVisit}. */
public record FieldVisitDto(
        String visitId,
        String accountId,
        String officerId,
        LocalDate scheduledDate,
        LocalDate visitDate,
        Boolean borrowerMet,
        Boolean assetSighted,
        String outcomeSummary,
        String nextActionRequired,
        String status,
        LocalDateTime createdAt
) {}
