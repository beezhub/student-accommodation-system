-- Add must_change_password column to app_user table
ALTER TABLE app_user
ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT false;
