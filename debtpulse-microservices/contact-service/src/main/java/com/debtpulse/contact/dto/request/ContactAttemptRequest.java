package com.debtpulse.contact.dto.request;

import com.debtpulse.common.enums.ContactChannel;
import com.debtpulse.common.enums.ContactOutcome;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Payload to log a contact attempt. {@code agentId} is optional — when the caller is a
 * COLLECTIONS_AGENT the service attributes the attempt to that agent automatically.
 */
public record ContactAttemptRequest(
        @NotBlank(message = "Account id is required")
        String accountId,

        String agentId,

        LocalDateTime contactDate,

        @NotNull(message = "Channel is required")
        ContactChannel channel,

        @NotNull(message = "Outcome is required")
        ContactOutcome outcome,

        String notes
) {}
