package com.debtpulse.legal.dto.response;

import com.debtpulse.common.enums.HearingOutcome;

import java.time.LocalDate;

/** Read projection of a {@link com.debtpulse.legal.entity.CourtHearing}. */
public record CourtHearingDto(
        String hearingId,
        String caseId,
        LocalDate hearingDate,
        HearingOutcome hearingOutcome,
        LocalDate nextHearingDate,
        String notes
) {}
