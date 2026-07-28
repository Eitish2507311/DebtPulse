-- ==========================================================================
-- Separate allocation rules from escalation rules (parity with the monolith).
--   auto_escalate = 0 : initial-allocation rule (assign to a collection agent)
--   auto_escalate = 1 : escalation rule (move a stagnating account to a higher role)
-- Existing rows default to 0 (initial-allocation) which is the safe, non-escalating choice.
-- ==========================================================================

ALTER TABLE allocation_rule ADD COLUMN auto_escalate BIT(1) NOT NULL DEFAULT 0;
