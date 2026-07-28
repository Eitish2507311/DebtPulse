-- ==========================================================================
-- settlement-service schema (debtpulse_settlement)
-- Settlement & Restructuring Management (2.5)
-- ==========================================================================

CREATE TABLE IF NOT EXISTS settlement_proposal (
    proposal_id        VARCHAR(36)   NOT NULL PRIMARY KEY,
    account_id         VARCHAR(36),
    officer_id         VARCHAR(36),
    total_outstanding  DECIMAL(15,2),
    settlement_amount  DECIMAL(15,2),
    haircut_percent    DECIMAL(6,2),
    payment_deadline   DATE,
    approval_level     VARCHAR(10),
    approved_by_id     VARCHAR(36),
    status             VARCHAR(20)   DEFAULT 'DRAFT',
    created_at         DATETIME,
    updated_at         DATETIME,
    INDEX idx_settlement_account (account_id),
    INDEX idx_settlement_status (status)
);

CREATE TABLE IF NOT EXISTS approval_step (
    step_id        VARCHAR(36)   NOT NULL PRIMARY KEY,
    settlement_id  VARCHAR(36),
    approver_id    VARCHAR(36),
    level          VARCHAR(10),
    decision       VARCHAR(10),
    decided_at     DATETIME,
    comments       VARCHAR(1000),
    INDEX idx_step_settlement (settlement_id),
    CONSTRAINT fk_step_settlement FOREIGN KEY (settlement_id)
        REFERENCES settlement_proposal (proposal_id)
);

CREATE TABLE IF NOT EXISTS restructuring_proposal (
    restructure_id  VARCHAR(36)   NOT NULL PRIMARY KEY,
    account_id      VARCHAR(36),
    officer_id      VARCHAR(36),
    revised_tenure  INT,
    revised_emi     DECIMAL(15,2),
    waiver_amount   DECIMAL(15,2),
    start_date      DATE,
    approved_by_id  VARCHAR(36),
    status          VARCHAR(20)   DEFAULT 'DRAFT',
    created_at      DATETIME,
    updated_at      DATETIME,
    INDEX idx_restructure_account (account_id),
    INDEX idx_restructure_status (status)
);
