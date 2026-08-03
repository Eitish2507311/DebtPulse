package com.debtpulse.settlement.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
        @Min(value = 1, message = "Revised tenure must be at least 1 month")
        @Max(value = 360, message = "Revised tenure cannot exceed 360 months (30 years)")
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
