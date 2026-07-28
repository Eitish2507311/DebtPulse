-- ==========================================================================
-- notification-service schema (debtpulse_notification) : Notifications & Alerts (2.8)
-- ==========================================================================

CREATE TABLE IF NOT EXISTS notification (
    notification_id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id         VARCHAR(36),
    message         VARCHAR(1000),
    category        VARCHAR(30),
    status          VARCHAR(20) DEFAULT 'UNREAD',
    created_date    DATETIME,
    INDEX idx_notification_user (user_id),
    INDEX idx_notification_user_status (user_id, status)
);
