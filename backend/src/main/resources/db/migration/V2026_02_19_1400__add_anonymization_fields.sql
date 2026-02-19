-- Add is_anonymized column to app_user table
ALTER TABLE app_user
ADD COLUMN is_anonymized BOOLEAN DEFAULT FALSE;

-- Create index for faster filtering of anonymized users
CREATE INDEX idx_app_user_is_anonymized ON app_user(is_anonymized);
