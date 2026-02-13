-- Fix version columns to have proper defaults for optimistic locking
-- Update NULL version values to 0 for existing records
UPDATE application SET version = 0 WHERE version IS NULL;
UPDATE room SET version = 0 WHERE version IS NULL;

-- Set default values for future inserts
ALTER TABLE application ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE room ALTER COLUMN version SET DEFAULT 0;

-- Make columns NOT NULL
ALTER TABLE application ALTER COLUMN version SET NOT NULL;
ALTER TABLE room ALTER COLUMN version SET NOT NULL;
