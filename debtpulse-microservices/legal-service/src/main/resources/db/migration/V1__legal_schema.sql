-- ==========================================================================
-- legal-service schema (debtpulse_legal) : Legal Proceedings Management (2.6)
-- ==========================================================================

CREATE TABLE IF NOT EXISTS legal_case (
    case_id          VARCHAR(36) NOT NULL PRIMARY KEY,
    account_id       VARCHAR(36),
    legal_officer_id VARCHAR(36),
    case_type        VARCHAR(40),
    filing_date      DATE,
    court_name       VARCHAR(150),
    case_number      VARCHAR(80),
    status           VARCHAR(30) DEFAULT 'FILED',
    created_at       DATETIME,
    INDEX idx_legal_case_account (account_id),
    INDEX idx_legal_case_officer (legal_officer_id),
    INDEX idx_legal_case_status (status)
);

CREATE TABLE IF NOT EXISTS court_hearing (
    hearing_id        VARCHAR(36) NOT NULL PRIMARY KEY,
    case_id           VARCHAR(36) NOT NULL,
    hearing_date      DATE,
    hearing_outcome   VARCHAR(30),
    next_hearing_date DATE,
    notes             VARCHAR(1000),
    created_at        DATETIME,
    INDEX idx_hearing_case (case_id),
    INDEX idx_hearing_date (hearing_date),
    INDEX idx_hearing_next_date (next_hearing_date),
    CONSTRAINT fk_hearing_case FOREIGN KEY (case_id) REFERENCES legal_case (case_id)
);

CREATE TABLE IF NOT EXISTS recovery_order (
    order_id          VARCHAR(36) NOT NULL PRIMARY KEY,
    case_id           VARCHAR(36) NOT NULL,
    order_type        VARCHAR(40),
    issued_date       DATE,
    execution_deadline DATE,
    status            VARCHAR(30) DEFAULT 'ISSUED',
    created_at        DATETIME,
    INDEX idx_order_case (case_id),
    INDEX idx_order_status (status),
    CONSTRAINT fk_order_case FOREIGN KEY (case_id) REFERENCES legal_case (case_id)
);
