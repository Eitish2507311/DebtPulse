package com.debtpulse.legal.dto.request;

import com.debtpulse.common.enums.CaseStatus;
import com.debtpulse.common.enums.CaseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Payload to file a legal case or update its details/status. On create the owning
 * {@code legalOfficerId} is taken from the authenticated caller (not this payload);
 * {@code status} is optional and used mainly on update.
 */
public record LegalCaseRequest(

        @NotBlank(message = "Account id is required")
        String accountId,

        @NotNull(message = "Case type is required")
        CaseType caseType,

        @NotNull(message = "Filing date is required")
        LocalDate filingDate,

        @NotBlank(message = "Court name is required")
        String courtName,

        @NotBlank(message = "Case number is required")
        String caseNumber,

        CaseStatus status
) {}
