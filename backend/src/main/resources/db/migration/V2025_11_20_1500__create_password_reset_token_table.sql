-- Create password_reset_token table
CREATE TABLE IF NOT EXISTS password_reset_token (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

-- Create index for faster token lookups
CREATE INDEX IF NOT EXISTS idx_password_reset_token_token ON password_reset_token(token);
CREATE INDEX IF NOT EXISTS idx_password_reset_token_user_id ON password_reset_token(user_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_token_expiry ON password_reset_token(expiry_date);

-- Add comment for documentation
COMMENT ON TABLE password_reset_token IS 'Stores password reset tokens for user password recovery';
COMMENT ON COLUMN password_reset_token.token IS 'Unique token for password reset';
COMMENT ON COLUMN password_reset_token.expiry_date IS 'Expiration date of the token';
COMMENT ON COLUMN password_reset_token.used IS 'Flag to indicate if token has been used';

