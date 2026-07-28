package com.debtpulse.settlement.feign.dto;

/**
 * Local copy of notification-service's {@code NotificationRequest} JSON contract
 * (see INTERNAL_CONTRACTS: {@code POST /api/internal/notifications}).
 * {@code category} carries a {@code NotifCategory} enum NAME (e.g. {@code SETTLEMENT}).
 */
public record NotificationRequest(
        String userId,
        String message,
        String category
) {}
