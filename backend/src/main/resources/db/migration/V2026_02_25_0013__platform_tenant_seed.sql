-- =============================================================================
-- TASK-2.02: Student Platform-Level Identity (Sentinel Tenant)
--
-- ADR-001: Students are platform-level entities and must not be scoped to any
-- building-owner tenant.  We represent this with a synthetic PLATFORM tenant
-- row at id = 0.  Because the column is BIGSERIAL the sequence starts at 1, so
-- id = 0 never collides with an auto-generated value.
--
-- Step 1: Insert the PLATFORM sentinel tenant (id=0).
-- Step 2: Migrate all existing student app_user records to tenant_id = 0.
-- =============================================================================

-- Step 1: PLATFORM sentinel row
-- BIGSERIAL default starts at 1, so id=0 is permanently reserved for this row.
INSERT INTO tenant (id, name, config, is_active, created_at, updated_at)
VALUES (0, 'PLATFORM', '{}', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Step 2: Re-assign all existing student users to the PLATFORM sentinel.
-- Identifies students via the student profile join table (user_id FK).
UPDATE app_user
SET tenant_id = 0
WHERE id IN (SELECT user_id FROM student);
