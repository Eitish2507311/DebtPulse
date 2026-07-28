package com.debtpulse.common.enums;

/**
 * Business category of a notification (2.8 Notifications &amp; Alerts). The enum NAME is the
 * exact string other services send on {@code POST /api/internal/notifications}.
 */
public enum NotifCategory {
    PTP,
    FIELD_VISIT,
    SETTLEMENT,
    LEGAL,
    ESCALATION,
    PORTFOLIO,
    SECURITY
}
