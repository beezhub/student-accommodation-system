-- Create password_reset_token table
COMMENT ON COLUMN password_reset_token.used IS 'Flag to indicate if token has been used';
COMMENT ON COLUMN password_reset_token.expiry_date IS 'Expiration date of the token';
COMMENT ON COLUMN password_reset_token.token IS 'Unique token for password reset';
COMMENT ON TABLE password_reset_token IS 'Stores password reset tokens for user password recovery';
-- Add comment for documentation

CREATE INDEX IF NOT EXISTS idx_password_reset_token_expiry ON password_reset_token(expiry_date);
CREATE INDEX IF NOT EXISTS idx_password_reset_token_user_id ON password_reset_token(user_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_token_token ON password_reset_token(token);
-- Create index for faster token lookups

);
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    expiry_date TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    id BIGSERIAL PRIMARY KEY,
CREATE TABLE IF NOT EXISTS password_reset_token (

