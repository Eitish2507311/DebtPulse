-- ==========================================================================
-- field-service schema (debtpulse_field) : Field Recovery Management (2.4)
-- ==========================================================================

CREATE TABLE IF NOT EXISTS field_visit (
    visit_id             VARCHAR(36)  NOT NULL PRIMARY KEY,
    account_id           VARCHAR(36),
    officer_id           VARCHAR(36),
    scheduled_date       DATE,
    visit_date           DATE,
    borrower_met         BIT(1),
    asset_sighted        BIT(1),
    outcome_summary      VARCHAR(1000),
    next_action_required VARCHAR(255),
    status               VARCHAR(20)  DEFAULT 'SCHEDULED',
    created_at           DATETIME,
    INDEX idx_visit_account (account_id),
    INDEX idx_visit_officer (officer_id),
    INDEX idx_visit_status_date (status, scheduled_date)
);

CREATE TABLE IF NOT EXISTS asset_verification_report (
    report_id         VARCHAR(36)    NOT NULL PRIMARY KEY,
    visit_id          VARCHAR(36),
    asset_id          VARCHAR(36),
    asset_condition   VARCHAR(20),
    current_location  VARCHAR(255),
    realisable_value  DECIMAL(15,2),
    remarks           VARCHAR(1000),
    verified_by_id    VARCHAR(36),
    verification_date DATE,
    created_at        DATETIME,
    INDEX idx_avr_visit (visit_id),
    INDEX idx_avr_asset (asset_id)
);
