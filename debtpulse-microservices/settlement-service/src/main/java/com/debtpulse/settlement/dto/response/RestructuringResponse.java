package com.debtpulse.settlement.dto.response;

import com.debtpulse.common.enums.RestructuringStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Restructuring proposal projection. */
public record RestructuringResponse(
        String restructureId,
        String accountId,
        String officerId,
        Integer revisedTenure,
        BigDecimal revisedEmi,
        BigDecimal waiverAmount,
        LocalDate startDate,
        String approvedById,
        RestructuringStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
