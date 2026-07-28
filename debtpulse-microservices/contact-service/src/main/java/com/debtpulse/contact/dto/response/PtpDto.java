package com.debtpulse.contact.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read projection of a {@link com.debtpulse.contact.entity.PromiseToPay}. */
public record PtpDto(
        String ptpId,
        String accountId,
        String agentId,
        LocalDate ptpDate,
        BigDecimal ptpAmount,
        LocalDate commitmentDate,
        BigDecimal actualPaidAmount,
        String status,
        LocalDateTime createdAt
) {}
