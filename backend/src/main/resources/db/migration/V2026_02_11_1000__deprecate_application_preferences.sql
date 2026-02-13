-- Deprecate unused application preference fields
-- These fields are not used in allocation logic and will be removed in future migration

-- Mark columns as deprecated
COMMENT ON COLUMN application.preferred_accommodation_type IS 'DEPRECATED: No longer used in allocation logic. Will be removed in V2026_03_01_1000.';
COMMENT ON COLUMN application.preferred_move_in_date IS 'DEPRECATED: No longer used in allocation logic. Will be removed in V2026_03_01_1000.';
COMMENT ON COLUMN application.preferred_duration_months IS 'DEPRECATED: No longer used in allocation logic. Will be removed in V2026_03_01_1000.';

-- Create archive table for historical data preservation
CREATE TABLE IF NOT EXISTS application_preferences_archive (
    application_id BIGINT PRIMARY KEY,
    preferred_accommodation_type VARCHAR(50),
    preferred_move_in_date DATE,
    preferred_duration_months INTEGER,
    archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_archive_application FOREIGN KEY (application_id)
        REFERENCES application(id) ON DELETE CASCADE
);

-- Archive existing non-null data before code changes
INSERT INTO application_preferences_archive
    (application_id, preferred_accommodation_type, preferred_move_in_date, preferred_duration_months)
SELECT id, preferred_accommodation_type, preferred_move_in_date, preferred_duration_months
FROM application
WHERE preferred_accommodation_type IS NOT NULL
   OR preferred_move_in_date IS NOT NULL
   OR preferred_duration_months IS NOT NULL;
