-- Add decline tracking columns to room_allocation table
ALTER TABLE room_allocation ADD COLUMN decline_reason VARCHAR(50);
ALTER TABLE room_allocation ADD COLUMN decline_reason_text VARCHAR(500);
