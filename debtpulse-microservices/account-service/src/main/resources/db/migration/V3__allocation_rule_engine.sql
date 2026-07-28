-- ==========================================================================
-- Allocation rule engine (P3): richer, config-driven matching + targeting.
--   min_dpd           : minimum DPD before the rule applies
--   grace_period_days : DPD grace before any action
--   capacity_limit    : max accounts a single target user may hold
-- ==========================================================================

ALTER TABLE allocation_rule ADD COLUMN min_dpd            INT;
ALTER TABLE allocation_rule ADD COLUMN grace_period_days  INT;
ALTER TABLE allocation_rule ADD COLUMN capacity_limit     INT;
