package com.debtpulse.legal.feign.dto;

/**
 * Local copy of notification-service's {@code NotificationRequest} JSON contract
 * (INTERNAL_CONTRACTS). {@code category} carries a {@code NotifCategory} enum NAME
 * (this service always sends {@code "LEGAL"}).
 */
public record NotificationRequest(
        String userId,
        String message,
        String category
) {}
