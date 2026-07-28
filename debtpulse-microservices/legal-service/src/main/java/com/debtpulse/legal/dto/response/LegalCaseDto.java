package com.debtpulse.legal.dto.response;

import com.debtpulse.common.enums.CaseStatus;
import com.debtpulse.common.enums.CaseType;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read projection of a {@link com.debtpulse.legal.entity.LegalCase}. */
public record LegalCaseDto(
        String caseId,
        String accountId,
        String legalOfficerId,
        CaseType caseType,
        LocalDate filingDate,
        String courtName,
        String caseNumber,
        CaseStatus status,
        LocalDateTime createdAt
) {}
