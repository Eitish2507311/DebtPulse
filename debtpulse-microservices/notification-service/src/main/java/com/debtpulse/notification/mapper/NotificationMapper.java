package com.debtpulse.notification.mapper;

import com.debtpulse.notification.dto.response.NotificationDto;
import com.debtpulse.notification.entity.Notification;
import org.springframework.stereotype.Component;

/** Converts between the {@link Notification} entity and its {@link NotificationDto} view. */
@Component
public class NotificationMapper {

    public NotificationDto toDto(Notification n) {
        if (n == null) return null;
        return new NotificationDto(
                n.getNotificationId(),
                n.getUserId(),
                n.getMessage(),
                n.getCategory() == null ? null : n.getCategory().name(),
                n.getStatus() == null ? null : n.getStatus().name(),
                n.getCreatedDate()
        );
    }
}
