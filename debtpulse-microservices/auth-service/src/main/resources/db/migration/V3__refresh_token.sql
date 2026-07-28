-- ==========================================================================
-- Refresh-token / session store (P1 auth platform). Raw tokens are never stored —
-- only their SHA-256 hash. Rotation lineage + sliding idle window are tracked here.
-- ==========================================================================

CREATE TABLE IF NOT EXISTS refresh_token (
    id                VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id           VARCHAR(36),
    token_hash        VARCHAR(64)  NOT NULL UNIQUE,
    session_id        VARCHAR(36),
    issued_at         DATETIME,
    expires_at        DATETIME,
    last_activity_at  DATETIME,
    revoked           BOOLEAN      DEFAULT FALSE,
    replaced_by_id    VARCHAR(36),
    INDEX idx_rt_user (user_id),
    INDEX idx_rt_session (session_id)
);
