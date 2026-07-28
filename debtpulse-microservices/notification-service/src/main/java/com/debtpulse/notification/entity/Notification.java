package com.debtpulse.notification.entity;

import com.debtpulse.common.id.BusinessId;

import com.debtpulse.common.enums.NotifCategory;
import com.debtpulse.common.enums.NotifStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A single in-app notification for one user (2.8 Notifications &amp; Alerts).
 *
 * <p>Owned exclusively by notification-service. {@code userId} is stored as a plain string
 * (the auth-service user id / JWT subject) rather than a FK — no cross-service relations.</p>
 */
@Entity
@Table(name = "notification")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @BusinessId(prefix = "NOT")
    private String notificationId;

    /** Recipient user id (auth-service user / JWT subject). */
    private String userId;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    private NotifCategory category;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NotifStatus status = NotifStatus.UNREAD;

    private LocalDateTime createdDate;
}
