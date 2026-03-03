-- Add nullable template_data JSONB column to notification table.
-- This stores the email template variables at send time, enabling
-- NotificationRetryService to reconstruct and dispatch failed emails
-- without re-fetching related entities.
ALTER TABLE notification
    ADD COLUMN IF NOT EXISTS template_data jsonb;
