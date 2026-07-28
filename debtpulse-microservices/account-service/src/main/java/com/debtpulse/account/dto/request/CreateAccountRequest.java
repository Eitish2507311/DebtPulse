package com.debtpulse.account.dto.request;

import com.debtpulse.common.validation.Phone;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Payload to onboard a delinquent account. Bucket is derived server-side from {@code dpd}. */
public record CreateAccountRequest(
        @NotBlank(message = "Loan reference is required")
        String loanRef,

        @NotBlank(message = "Borrower name is required")
        String borrowerName,

        @Phone
        String phone,

        String address,

        String branchId,

        @NotNull(message = "Principal amount is required")
        @Positive(message = "Principal amount must be positive")
        BigDecimal principalAmount,

        @NotNull(message = "Total overdue is required")
        @PositiveOrZero(message = "Total overdue cannot be negative")
        BigDecimal totalOverdue,

        @NotNull(message = "DPD is required")
        @PositiveOrZero(message = "DPD cannot be negative")
        Integer dpd
) {}
