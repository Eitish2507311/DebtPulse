package com.debtpulse.legal.dto.request;

import com.debtpulse.common.enums.CaseStatus;
import com.debtpulse.common.enums.CaseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

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
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9/\\-]{2,39}$",
                message = "Case number must be 3–40 characters using letters, digits, '/' or '-' "
                        + "(e.g. CS/2026/123)")
        String caseNumber,

        CaseStatus status
) {}
