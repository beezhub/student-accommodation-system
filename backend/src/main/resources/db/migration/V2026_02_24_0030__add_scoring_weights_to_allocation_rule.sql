-- TASK-2.11: Add owner-configurable scoring weight columns to allocation_rules.
-- These columns power OwnerConfigurableScoringStrategy (v2.0 scoring).
-- All new columns are nullable with sane defaults so existing rows are unaffected.

ALTER TABLE allocation_rules
    ADD COLUMN IF NOT EXISTS fcfs_weight              NUMERIC(5,2) NOT NULL DEFAULT 1.0,
    ADD COLUMN IF NOT EXISTS nsfas_priority_weight    NUMERIC(5,2) NOT NULL DEFAULT 1.0,
    ADD COLUMN IF NOT EXISTS university_pref_weight   NUMERIC(5,2) NOT NULL DEFAULT 1.0,
    ADD COLUMN IF NOT EXISTS priority_institution_ids TEXT          NULL;

-- priority_institution_ids: comma-separated institution IDs that earn the university preference bonus.
-- NULL means "any institution" (no bonus applied).
