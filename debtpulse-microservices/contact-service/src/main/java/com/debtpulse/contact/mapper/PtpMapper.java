package com.debtpulse.contact.mapper;

import com.debtpulse.contact.dto.response.PtpDto;
import com.debtpulse.contact.entity.PromiseToPay;
import org.springframework.stereotype.Component;

/** Converts between {@link PromiseToPay} and {@link PtpDto}. */
@Component
public class PtpMapper {

    public PtpDto toDto(PromiseToPay e) {
        if (e == null) return null;
        return new PtpDto(
                e.getPtpId(),
                e.getAccountId(),
                e.getAgentId(),
                e.getPtpDate(),
                e.getPtpAmount(),
                e.getCommitmentDate(),
                e.getActualPaidAmount(),
                e.getStatus() == null ? null : e.getStatus().name(),
                e.getCreatedAt()
        );
    }
}
