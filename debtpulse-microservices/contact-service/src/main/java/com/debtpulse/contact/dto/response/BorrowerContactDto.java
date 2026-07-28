package com.debtpulse.contact.dto.response;

import java.time.LocalDateTime;

/** Read projection of a {@link com.debtpulse.contact.entity.BorrowerContact}. */
public record BorrowerContactDto(
        String contactRecordId,
        String accountId,
        String contactType,
        String name,
        String phone,
        String relationship,
        String status,
        LocalDateTime createdAt
) {}
