package com.debtpulse.notification.dto.request;

import com.debtpulse.common.enums.NotifCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload other microservices POST to {@code /api/internal/notifications} (over Feign)
 * to raise an in-app alert for a user.
 *
 * <p>{@code category} is bound to the {@link NotifCategory} enum: consumers send the exact
 * enum NAME as a JSON string (e.g. {@code "PTP"}), keeping the wire contract byte-for-byte
 * compatible with their local {@code NotificationRequest(userId, message, category)} record.</p>
 */
public record NotificationRequest(

        @NotBlank(message = "User id is required")
        String userId,

        @NotBlank(message = "Message is required")
        String message,

        @NotNull(message = "Category is required")
        NotifCategory category
) {}
