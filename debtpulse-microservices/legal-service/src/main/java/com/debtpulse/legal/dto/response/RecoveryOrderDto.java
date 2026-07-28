package com.debtpulse.legal.dto.response;

import com.debtpulse.common.enums.OrderStatus;
import com.debtpulse.common.enums.OrderType;

import java.time.LocalDate;

/** Read projection of a {@link com.debtpulse.legal.entity.RecoveryOrder}. */
public record RecoveryOrderDto(
        String orderId,
        String caseId,
        OrderType orderType,
        LocalDate issuedDate,
        LocalDate executionDeadline,
        OrderStatus status
) {}
