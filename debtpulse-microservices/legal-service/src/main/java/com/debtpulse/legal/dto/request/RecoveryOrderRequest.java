package com.debtpulse.legal.dto.request;

import com.debtpulse.common.enums.OrderStatus;
import com.debtpulse.common.enums.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Payload to issue a recovery order for a specific case ({@code caseId}). */
public record RecoveryOrderRequest(

        @NotBlank(message = "Case id is required")
        String caseId,

        @NotNull(message = "Order type is required")
        OrderType orderType,

        @NotNull(message = "Issued date is required")
        LocalDate issuedDate,

        @NotNull(message = "Execution deadline is required")
        LocalDate executionDeadline,

        OrderStatus status
) {}
