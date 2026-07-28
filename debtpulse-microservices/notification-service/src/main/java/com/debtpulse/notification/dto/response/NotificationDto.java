package com.debtpulse.notification.dto.response;

import java.time.LocalDateTime;

/** Serialization-friendly view of a {@code Notification} (category/status as enum names). */
public record NotificationDto(
        String notificationId,
        String userId,
        String message,
        String category,
        String status,
        LocalDateTime createdDate
) {}
