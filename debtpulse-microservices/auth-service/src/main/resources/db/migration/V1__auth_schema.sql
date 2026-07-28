-- ==========================================================================
-- auth-service schema (debtpulse_auth) : Identity & Access Management (2.1)
-- ==========================================================================

CREATE TABLE IF NOT EXISTS users (
    user_id               VARCHAR(36)  NOT NULL PRIMARY KEY,
    full_name             VARCHAR(150),
    email                 VARCHAR(150) UNIQUE,
    phone                 VARCHAR(30),
    password_hash         VARCHAR(255),
    role                  VARCHAR(30),
    branch_id             VARCHAR(20),
    status                VARCHAR(20)  DEFAULT 'ACTIVE',
    created_at            DATETIME,
    reset_token           VARCHAR(100),
    reset_token_expiry    DATETIME,
    failed_login_attempts INT          DEFAULT 0,
    locked_until          DATETIME
);

CREATE TABLE IF NOT EXISTS audit_log (
    audit_id       VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id        VARCHAR(36),
    action         VARCHAR(120),
    entity_type    VARCHAR(80),
    record_id      VARCHAR(80),
    source_service VARCHAR(60),
    timestamp      DATETIME,
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_entity (entity_type, record_id)
);
