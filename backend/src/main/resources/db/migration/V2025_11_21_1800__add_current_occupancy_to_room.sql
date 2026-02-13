-- Add current_occupancy column to room table with default value
ALTER TABLE room ADD COLUMN IF NOT EXISTS current_occupancy INTEGER DEFAULT 0;

-- Update existing rows to set current_occupancy to 0 if it's NULL
UPDATE room SET current_occupancy = 0 WHERE current_occupancy IS NULL;

-- Now make the column NOT NULL
ALTER TABLE room ALTER COLUMN current_occupancy SET NOT NULL;

