-- Create email_verification_token table
CREATE TABLE IF NOT EXISTS email_verification_token (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

-- Create index for faster token lookups
CREATE INDEX IF NOT EXISTS idx_email_verification_token_token ON email_verification_token(token);
CREATE INDEX IF NOT EXISTS idx_email_verification_token_user_id ON email_verification_token(user_id);
CREATE INDEX IF NOT EXISTS idx_email_verification_token_expiry ON email_verification_token(expiry_date);

-- Add comment for documentation
COMMENT ON TABLE email_verification_token IS 'Stores email verification tokens for user email confirmation';
COMMENT ON COLUMN email_verification_token.token IS 'Unique token for email verification';
COMMENT ON COLUMN email_verification_token.expiry_date IS 'Expiration date of the token';
COMMENT ON COLUMN email_verification_token.used IS 'Flag to indicate if token has been used';

