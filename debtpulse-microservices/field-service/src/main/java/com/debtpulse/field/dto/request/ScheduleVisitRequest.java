package com.debtpulse.field.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Payload to schedule a new field visit against a delinquent account. */
public record ScheduleVisitRequest(

        @NotBlank(message = "Account id is required")
        String accountId,

        @NotBlank(message = "Officer id is required")
        String officerId,

        @NotNull(message = "Scheduled date is required")
        @FutureOrPresent(message = "Scheduled date cannot be in the past")
        LocalDate scheduledDate,

        String nextActionRequired
) {}
