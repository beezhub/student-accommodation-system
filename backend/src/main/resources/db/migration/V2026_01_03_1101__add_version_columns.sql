-- Add optimistic locking columns to key tables
ALTER TABLE application ADD COLUMN IF NOT EXISTS version BIGINT;
ALTER TABLE room ADD COLUMN IF NOT EXISTS version BIGINT;

