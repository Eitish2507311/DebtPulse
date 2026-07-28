-- ==========================================================================
-- Settlement approval workflow: the required approval chain (L1->L2->L3) is now
-- derived server-side from the haircut and approved sequentially. Track the level
-- currently awaiting a decision, plus optional officer remarks.
-- ==========================================================================

ALTER TABLE settlement_proposal ADD COLUMN current_step VARCHAR(10);
ALTER TABLE settlement_proposal ADD COLUMN notes        VARCHAR(1000);
