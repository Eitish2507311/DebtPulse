-- ==========================================================================
-- account-service schema (debtpulse_account) : Delinquent Loan Portfolio (2.2)
-- ==========================================================================

CREATE TABLE IF NOT EXISTS delinquent_account (
    account_id             VARCHAR(36)   NOT NULL PRIMARY KEY,
    loan_ref               VARCHAR(80)   UNIQUE,
    borrower_name          VARCHAR(150),
    phone                  VARCHAR(30),
    address                VARCHAR(255),
    branch_id              VARCHAR(20),
    principal_amount       DECIMAL(18,2),
    total_overdue          DECIMAL(18,2),
    dpd                    INT,
    bucket                 VARCHAR(20),
    days_in_current_bucket INT           DEFAULT 0,
    status                 VARCHAR(20)   DEFAULT 'ACTIVE',
    assigned_agent_id      VARCHAR(36),
    created_at             DATETIME,
    updated_at             DATETIME,
    INDEX idx_account_status (status),
    INDEX idx_account_bucket (bucket),
    INDEX idx_account_agent (assigned_agent_id)
);

CREATE TABLE IF NOT EXISTS collateral_asset (
    asset_id            VARCHAR(36)  NOT NULL PRIMARY KEY,
    account_id          VARCHAR(36),
    asset_type          VARCHAR(20),
    description         VARCHAR(255),
    estimated_value     DECIMAL(18,2),
    verification_status VARCHAR(20)  DEFAULT 'UNVERIFIED',
    last_verified_date  DATETIME,
    INDEX idx_collateral_account (account_id)
);

CREATE TABLE IF NOT EXISTS allocation_rule (
    rule_id                  VARCHAR(36) NOT NULL PRIMARY KEY,
    name                     VARCHAR(150),
    strategy                 VARCHAR(30),
    bucket                   VARCHAR(20),
    target_role              VARCHAR(30),
    days_in_bucket_threshold INT,
    branch_id                VARCHAR(20),
    priority                 INT         DEFAULT 0,
    active                   BIT(1)      DEFAULT 1,
    INDEX idx_rule_active (active)
);
