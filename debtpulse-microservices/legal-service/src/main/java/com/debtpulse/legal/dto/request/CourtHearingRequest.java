package com.debtpulse.legal.dto.request;

import com.debtpulse.common.enums.HearingOutcome;
import com.debtpulse.common.enums.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Payload to schedule or conclude a court hearing for a case ({@code caseId}).
 *
 * <p>{@code hearingOutcome} is optional: a <em>scheduled</em> hearing that has not yet been held has
 * no outcome, and recording one drives the case lifecycle (e.g. {@code ORDER_PASSED} → the case is
 * decreed). When the outcome is {@code ORDER_PASSED}, {@code orderType} and {@code executionDeadline}
 * are required so the resulting recovery order can be issued in the same step.</p>
 */
public record CourtHearingRequest(

        @NotBlank(message = "Case id is required")
        String caseId,

        @NotNull(message = "Hearing date is required")
        LocalDate hearingDate,

        /** Result of the hearing; {@code null} while the hearing is only scheduled (not yet held). */
        HearingOutcome hearingOutcome,

        LocalDate nextHearingDate,

        String notes,

        /** Type of order the court passed — required only when {@code hearingOutcome == ORDER_PASSED}. */
        OrderType orderType,

        /** Deadline to execute the passed order — required only when {@code hearingOutcome == ORDER_PASSED}. */
        LocalDate executionDeadline
) {}
