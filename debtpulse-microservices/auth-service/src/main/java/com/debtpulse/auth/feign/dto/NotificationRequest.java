package com.debtpulse.auth.feign.dto;

/**
 * Local copy of notification-service's {@code NotificationRequest} JSON (field names
 * byte-for-byte compatible per INTERNAL_CONTRACTS). {@code category} carries a
 * {@code NotifCategory} enum name — auth-service sends {@code "SECURITY"}.
 */
public record NotificationRequest(
        String userId,
        String message,
        String category
) {}
