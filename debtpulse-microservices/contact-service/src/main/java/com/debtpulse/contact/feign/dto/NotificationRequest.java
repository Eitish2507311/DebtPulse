package com.debtpulse.contact.feign.dto;

/**
 * Local copy of notification-service's {@code NotificationRequest} JSON (INTERNAL_CONTRACTS.md).
 * {@code category} carries a {@code NotifCategory} enum NAME (contact-service sends {@code "PTP"}).
 */
public record NotificationRequest(
        String userId,
        String message,
        String category
) {}
