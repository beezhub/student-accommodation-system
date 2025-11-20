-- Add email_verified column to app_user table
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_app_user_email_verified ON app_user(email_verified);

-- Add comment for documentation
COMMENT ON COLUMN app_user.email_verified IS 'Flag to indicate if user email has been verified';

-- Update existing users to have email_verified as true (assuming they are already registered)
UPDATE app_user SET email_verified = FALSE WHERE email_verified IS NULL;

