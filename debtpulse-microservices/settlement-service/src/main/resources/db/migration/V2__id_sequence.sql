-- Sequence table backing the shared @BusinessId generator (com.debtpulse.common.id).
-- One row per (prefix, year); next_val is bumped atomically via INSERT ... ON DUPLICATE KEY UPDATE.
CREATE TABLE IF NOT EXISTS id_sequence (
    seq_key  VARCHAR(32) NOT NULL,
    seq_year INT         NOT NULL,
    next_val BIGINT      NOT NULL,
    PRIMARY KEY (seq_key, seq_year)
);
