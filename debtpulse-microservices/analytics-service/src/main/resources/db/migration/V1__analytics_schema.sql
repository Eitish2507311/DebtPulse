-- ==========================================================================
-- analytics-service schema (debtpulse_analytics) : Recovery Analytics (2.7)
-- ==========================================================================

CREATE TABLE IF NOT EXISTS recovery_report (
    report_id      VARCHAR(36) NOT NULL PRIMARY KEY,
    scope          VARCHAR(60),
    metrics        TEXT,
    generated_date DATETIME
);
