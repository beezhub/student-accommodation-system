-- Create academic_year table
-- This is the foundation for scoping applications and allocations per academic year
CREATE TABLE academic_year (
    id SERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    application_open_date DATE NOT NULL,
    application_close_date DATE NOT NULL,
    is_current BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Ensure only one academic year can be marked as current at a time
CREATE UNIQUE INDEX idx_academic_year_current 
ON academic_year(is_current) WHERE is_current = TRUE;

-- Index for common queries
CREATE INDEX idx_academic_year_dates ON academic_year(application_open_date, application_close_date);

COMMENT ON TABLE academic_year IS 'Stores academic year configurations with application window dates';
COMMENT ON COLUMN academic_year.name IS 'Display name for the academic year, e.g., 2026';
COMMENT ON COLUMN academic_year.is_current IS 'Only one row can have this set to TRUE';
