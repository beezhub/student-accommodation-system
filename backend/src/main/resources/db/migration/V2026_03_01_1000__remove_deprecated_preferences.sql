-- Remove deprecated application preference columns
-- Deploy this migration after 2+ weeks of stable operation with code changes

-- Remove deprecated columns (after successful code deployment and monitoring period)
ALTER TABLE application
    DROP COLUMN IF EXISTS preferred_accommodation_type,
    DROP COLUMN IF EXISTS preferred_move_in_date,
    DROP COLUMN IF EXISTS preferred_duration_months;

-- Keep application_preferences_archive table for historical audit purposes
