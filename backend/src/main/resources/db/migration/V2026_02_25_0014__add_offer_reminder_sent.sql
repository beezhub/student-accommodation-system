-- TASK-3.10: Add reminder_24h_sent flag to room_allocation
-- Tracks whether a 24-hour offer-expiry reminder has already been dispatched
-- for a given allocation, preventing duplicate reminders on re-runs.
ALTER TABLE room_allocation
    ADD COLUMN reminder_24h_sent BOOLEAN NOT NULL DEFAULT false;
