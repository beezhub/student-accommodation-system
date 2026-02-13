-- Migration: Add Building-Institution relationship and Year of Study to Room Attributes
-- Date: 2026-01-29

-- 1. Create building_institution junction table (many-to-many)
CREATE TABLE IF NOT EXISTS building_institution (
    building_id BIGINT NOT NULL,
    institution_id BIGINT NOT NULL,
    PRIMARY KEY (building_id, institution_id),
    CONSTRAINT fk_building_institution_building FOREIGN KEY (building_id) REFERENCES building(id) ON DELETE CASCADE,
    CONSTRAINT fk_building_institution_institution FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE CASCADE
);

-- Create indexes for the junction table
CREATE INDEX IF NOT EXISTS idx_building_institution_building ON building_institution(building_id);
CREATE INDEX IF NOT EXISTS idx_building_institution_institution ON building_institution(institution_id);

-- 2. Add year of study columns to room_attributes
ALTER TABLE room_attributes ADD COLUMN IF NOT EXISTS min_year_of_study INTEGER NULL;
ALTER TABLE room_attributes ADD COLUMN IF NOT EXISTS max_year_of_study INTEGER NULL;

-- 3. Add check constraints for year of study validation
ALTER TABLE room_attributes ADD CONSTRAINT chk_min_year_of_study CHECK (min_year_of_study IS NULL OR (min_year_of_study >= 1 AND min_year_of_study <= 10));
ALTER TABLE room_attributes ADD CONSTRAINT chk_max_year_of_study CHECK (max_year_of_study IS NULL OR (max_year_of_study >= 1 AND max_year_of_study <= 10));
ALTER TABLE room_attributes ADD CONSTRAINT chk_year_range CHECK (min_year_of_study IS NULL OR max_year_of_study IS NULL OR min_year_of_study <= max_year_of_study);

-- Note: The following columns are kept for backwards compatibility but are no longer used:
-- - room_attributes.desirability_override (scoring removed)
-- - building_attributes.gender_restriction (moved to room-level only)
-- - building_attributes.allow_mixed_gender (moved to room-level only)
-- - building_attributes.desirability_score (scoring removed)
-- 
-- These columns can be removed in a future cleanup migration once verified safe.
