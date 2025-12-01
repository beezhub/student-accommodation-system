-- Seed initial admin user
-- Default credentials:
-- Email: admin@studentaccommodation.com
-- Password: Admin@123456 (MUST be changed on first login)
--
-- IMPORTANT: The BCrypt hash below is for the password "Admin@123456"
-- BCrypt hash strength: 10 rounds
-- Hash generated using bcrypt with salt rounds = 10
-- This password MUST be changed immediately on first login

INSERT INTO app_user (
    email,
    user_password,
    user_role,
    first_name,
    last_name,
    email_verified,
    must_change_password,
    created_at,
    updated_at
)
SELECT
    'admin@studentaccommodation.com',
    '$2b$10$u3zQI5mTrJaHKxmC72CAmOmvPysTBXsB6JwsGaEWK8CQQ7DEQua6u',
    'ADMIN',
    'System',
    'Administrator',
    true,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM app_user WHERE email = 'admin@studentaccommodation.com'
);
