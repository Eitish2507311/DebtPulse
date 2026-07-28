package com.debtpulse.settlement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Payload to create/update a restructuring proposal. {@code officerId} is taken from the user. */
public record RestructuringRequest(
        @NotBlank(message = "Account id is required")
        String accountId,

        @NotNull(message = "Revised tenure is required")
        @Positive(message = "Revised tenure must be positive")
        Integer revisedTenure,

        @NotNull(message = "Revised EMI is required")
        @Positive(message = "Revised EMI must be positive")
        BigDecimal revisedEmi,

        @NotNull(message = "Waiver amount is required")
        @PositiveOrZero(message = "Waiver amount cannot be negative")
        BigDecimal waiverAmount,

        @NotNull(message = "Start date is required")
        LocalDate startDate
) {}
