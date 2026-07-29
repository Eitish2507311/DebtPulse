package com.debtpulse.notification.service;

import com.debtpulse.common.enums.NotifCategory;
import com.debtpulse.common.enums.NotifStatus;
import com.debtpulse.notification.dto.response.NotificationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * Notification business operations. The current-user API methods take the caller's userId
 * (resolved from {@code AuthContext} by the controller) and enforce ownership; the
 * {@link #create} method is used by the internal Feign endpoint.
 */
public interface NotificationService {

    /** Create a new UNREAD notification for {@code userId} (called from the internal Feign API). */
    NotificationDto create(String userId, String message, NotifCategory category);

    /**
     * Page of the given user's notifications (newest first), optionally narrowed by category,
     * status and a created-date range. Any filter left {@code null} is ignored.
     */
    Page<NotificationDto> listForUser(String userId, NotifCategory category, NotifStatus status,
                                      LocalDate from, LocalDate to, Pageable pageable);

    /** A single notification owned by {@code userId}. */
    NotificationDto getById(String userId, String notificationId);

    /** Count of the user's UNREAD notifications. */
    long unreadCount(String userId);

    /** Mark one of the user's notifications as READ. */
    NotificationDto markRead(String userId, String notificationId);

    /** Dismiss (delete) one of the user's notifications so it is cleared from their history. */
    void dismiss(String userId, String notificationId);

    /** Mark all of the user's UNREAD notifications as READ; returns how many changed. */
    long markAllRead(String userId);
}
