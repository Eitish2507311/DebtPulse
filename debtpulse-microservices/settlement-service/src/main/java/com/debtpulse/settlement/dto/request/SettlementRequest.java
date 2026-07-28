package com.debtpulse.settlement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload to create/update a settlement proposal.
 *
 * <p>{@code officerId} is never supplied by the client — it is taken from the authenticated user.
 * {@code haircutPercent} and the required approval chain (L1/L2/L3) are computed server-side from
 * the outstanding and settlement amounts; the client does NOT choose the approval level (this
 * prevents a large-haircut proposal from being routed to a too-junior approver).</p>
 */
public record SettlementRequest(
        @NotBlank(message = "Account id is required")
        String accountId,

        @NotNull(message = "Total outstanding is required")
        @Positive(message = "Total outstanding must be positive")
        BigDecimal totalOutstanding,

        @NotNull(message = "Settlement amount is required")
        @Positive(message = "Settlement amount must be positive")
        BigDecimal settlementAmount,

        @NotNull(message = "Payment deadline is required")
        LocalDate paymentDeadline,

        @Size(max = 1000, message = "Remarks must be at most 1000 characters")
        String notes
) {}
