package com.debtpulse.legal.dto.request;

import com.debtpulse.common.enums.HearingOutcome;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Payload to record a court hearing against a specific case ({@code caseId}). */
public record CourtHearingRequest(

        @NotBlank(message = "Case id is required")
        String caseId,

        @NotNull(message = "Hearing date is required")
        LocalDate hearingDate,

        @NotNull(message = "Hearing outcome is required")
        HearingOutcome hearingOutcome,

        LocalDate nextHearingDate,

        String notes
) {}
