package com.debtpulse.contact.dto.response;

import java.time.LocalDateTime;

/** Read projection of a {@link com.debtpulse.contact.entity.ContactAttempt}. */
public record ContactAttemptDto(
        String contactId,
        String accountId,
        String agentId,
        LocalDateTime contactDate,
        String channel,
        String outcome,
        String notes,
        String status,
        LocalDateTime createdAt
) {}
