package com.debtpulse.legal.mapper;

import com.debtpulse.legal.dto.response.CourtHearingDto;
import com.debtpulse.legal.dto.response.LegalCaseDto;
import com.debtpulse.legal.dto.response.RecoveryOrderDto;
import com.debtpulse.legal.entity.CourtHearing;
import com.debtpulse.legal.entity.LegalCase;
import com.debtpulse.legal.entity.RecoveryOrder;
import org.springframework.stereotype.Component;

/** Converts legal-service entities to their read DTOs (plain component, no MapStruct). */
@Component
public class LegalMapper {

    public LegalCaseDto toDto(LegalCase c) {
        if (c == null) return null;
        return new LegalCaseDto(
                c.getCaseId(),
                c.getAccountId(),
                c.getLegalOfficerId(),
                c.getCaseType(),
                c.getFilingDate(),
                c.getCourtName(),
                c.getCaseNumber(),
                c.getStatus(),
                c.getCreatedAt()
        );
    }

    public CourtHearingDto toDto(CourtHearing h) {
        if (h == null) return null;
        return new CourtHearingDto(
                h.getHearingId(),
                h.getLegalCase() == null ? null : h.getLegalCase().getCaseId(),
                h.getHearingDate(),
                h.getHearingOutcome(),
                h.getNextHearingDate(),
                h.getNotes()
        );
    }

    public RecoveryOrderDto toDto(RecoveryOrder o) {
        if (o == null) return null;
        return new RecoveryOrderDto(
                o.getOrderId(),
                o.getLegalCase() == null ? null : o.getLegalCase().getCaseId(),
                o.getOrderType(),
                o.getIssuedDate(),
                o.getExecutionDeadline(),
                o.getStatus()
        );
    }
}
