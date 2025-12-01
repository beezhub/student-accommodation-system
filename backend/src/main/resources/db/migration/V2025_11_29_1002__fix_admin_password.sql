-- Fix admin user password if it was created with incorrect hash
-- This migration updates the admin user's password hash to the correct value
-- Password: Admin@123456
-- Correct BCrypt hash: $2b$10$u3zQI5mTrJaHKxmC72CAmOmvPysTBXsB6JwsGaEWK8CQQ7DEQua6u

UPDATE app_user
SET user_password = '$2b$10$u3zQI5mTrJaHKxmC72CAmOmvPysTBXsB6JwsGaEWK8CQQ7DEQua6u',
    must_change_password = true,
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'admin@studentaccommodation.com'
  AND user_password != '$2b$10$u3zQI5mTrJaHKxmC72CAmOmvPysTBXsB6JwsGaEWK8CQQ7DEQua6u';
