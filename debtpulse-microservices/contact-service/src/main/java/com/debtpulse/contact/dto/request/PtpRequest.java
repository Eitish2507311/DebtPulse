package com.debtpulse.contact.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload to record a promise-to-pay. {@code agentId} is optional — when the caller is a
 * COLLECTIONS_AGENT the service attributes the PTP to that agent automatically.
 */
public record PtpRequest(
        @NotBlank(message = "Account id is required")
        String accountId,

        String agentId,

        @NotNull(message = "PTP date is required")
        LocalDate ptpDate,

        @NotNull(message = "PTP amount is required")
        @Positive(message = "PTP amount must be positive")
        BigDecimal ptpAmount,

        @NotNull(message = "Commitment date is required")
        LocalDate commitmentDate
) {}
