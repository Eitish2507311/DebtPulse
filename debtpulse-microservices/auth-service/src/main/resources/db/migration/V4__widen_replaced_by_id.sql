-- ==========================================================================
-- Widen refresh_token.replaced_by_id to hold a SHA-256 hex hash (64 chars).
-- TokenServiceImpl.refresh() writes sha256(nextRefreshToken) into this column to
-- record rotation lineage; the original VARCHAR(36) truncated it and every
-- refresh failed with "Data too long for column 'replaced_by_id'". Match the
-- token_hash column's width (VARCHAR(64)).
-- ==========================================================================

ALTER TABLE refresh_token MODIFY COLUMN replaced_by_id VARCHAR(64);
