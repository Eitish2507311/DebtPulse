-- ==========================================================================
-- contact-service schema (debtpulse_contact) : Contact & Follow-Up Mgmt (2.3)
-- Cross-service references (account_id, agent_id) are plain ids — no FKs.
-- ==========================================================================

CREATE TABLE IF NOT EXISTS contact_attempt (
    contact_id    VARCHAR(36)  NOT NULL PRIMARY KEY,
    account_id    VARCHAR(36),
    agent_id      VARCHAR(36),
    contact_date  DATETIME,
    channel       VARCHAR(20),
    outcome       VARCHAR(20),
    notes         VARCHAR(1000),
    status        VARCHAR(20)  DEFAULT 'LOGGED',
    created_at    DATETIME,
    INDEX idx_contact_account (account_id),
    INDEX idx_contact_agent (agent_id)
);

CREATE TABLE IF NOT EXISTS promise_to_pay (
    ptp_id             VARCHAR(36)    NOT NULL PRIMARY KEY,
    account_id         VARCHAR(36),
    agent_id           VARCHAR(36),
    ptp_date           DATE,
    ptp_amount         DECIMAL(15,2),
    commitment_date    DATE,
    actual_paid_amount DECIMAL(15,2),
    status             VARCHAR(20)    DEFAULT 'ACTIVE',
    created_at         DATETIME,
    INDEX idx_ptp_account (account_id),
    INDEX idx_ptp_status (status),
    INDEX idx_ptp_commitment (commitment_date)
);

CREATE TABLE IF NOT EXISTS borrower_contact (
    contact_record_id VARCHAR(36)  NOT NULL PRIMARY KEY,
    account_id        VARCHAR(36),
    contact_type      VARCHAR(20),
    name              VARCHAR(150),
    phone             VARCHAR(30),
    relationship      VARCHAR(60),
    status            VARCHAR(20)  DEFAULT 'ACTIVE',
    created_at        DATETIME,
    INDEX idx_borrower_account (account_id)
);
