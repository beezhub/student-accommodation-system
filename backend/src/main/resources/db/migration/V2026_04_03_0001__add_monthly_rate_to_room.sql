-- Add monthly_rate column to room table for room-level pricing
ALTER TABLE room ADD COLUMN monthly_rate NUMERIC(10,2);
