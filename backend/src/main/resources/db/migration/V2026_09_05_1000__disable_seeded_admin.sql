-- Neutralise the seeded platform administrator outside demo environments.
--
-- V2026_02_19_2101__seed_reference_data.sql inserts admin@studentaccommodation.com with a BCrypt
-- hash of a password published in that file's own comment. Flyway migrations are immutable, so the
-- account is created on every fresh database, including NONPROD and PROD. Its must_change_password
-- flag was only ever enforced by the Angular route guard, so the published credential granted full
-- platform-admin API access.
--
-- This migration replaces the hash with a value no password can produce and locks the account, so
-- the published credential cannot authenticate. Set the Flyway placeholder demoSeed to true
-- (DEMO_SEED=true) to keep the demo login working locally.
--
-- Provision the real first administrator with ADMIN_BOOTSTRAP_EMAIL / ADMIN_BOOTSTRAP_PASSWORD,
-- which AdminBootstrap applies at startup.

UPDATE app_user
SET user_password       = 'DISABLED-NO-LOGIN',
    account_locked_until = TIMESTAMP '9999-12-31 23:59:59',
    must_change_password = true,
    updated_at           = CURRENT_TIMESTAMP
WHERE email = 'admin@studentaccommodation.com'
  AND lower('${demoSeed}') <> 'true';
