package com.debtpulse.settlement.mapper;

import com.debtpulse.settlement.dto.response.RestructuringResponse;
import com.debtpulse.settlement.entity.RestructuringProposal;
import org.springframework.stereotype.Component;

/** Converts {@link RestructuringProposal} entities to their response projection. */
@Component
public class RestructuringMapper {

    public RestructuringResponse toDto(RestructuringProposal r) {
        if (r == null) return null;
        return new RestructuringResponse(
                r.getRestructureId(),
                r.getAccountId(),
                r.getOfficerId(),
                r.getRevisedTenure(),
                r.getRevisedEmi(),
                r.getWaiverAmount(),
                r.getStartDate(),
                r.getApprovedById(),
                r.getStatus(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
