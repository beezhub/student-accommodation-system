-- =============================================================================
-- V2026_02_19_2101 - Reference data and admin seed
-- =============================================================================
-- Depends on: V2026_02_19_2100 (full schema must exist first)
-- All inserts are idempotent (ON CONFLICT DO NOTHING or WHERE NOT EXISTS).
-- =============================================================================


-- Default tenant (id=1 pinned so backfill sub-selects are deterministic)
INSERT INTO tenant (id, name, config, is_active)
VALUES (1, 'Default Tenant', '{"offer_expiry_days": 7, "max_decline_count": 2}'::jsonb, true)
ON CONFLICT (name) DO NOTHING;


-- Institutions
INSERT INTO institution (name, address, phone_number, email) VALUES
    ('University of Example', '123 Example St, City, Country', '123-456-7890', 'UniversityOfExample@example.com'),
    ('Example College',       '456 Example Ave, City, Country', '987-654-3210', 'ExampleCollege@example.com')
ON CONFLICT (name) DO NOTHING;


-- Years of study  (no unique constraint → guard with NOT EXISTS)
INSERT INTO year_of_study (year)
SELECT unnest(ARRAY['First Year','Second Year','Third Year','Fourth Year','Postgraduate'])
WHERE NOT EXISTS (SELECT 1 FROM year_of_study LIMIT 1);


-- Document types
INSERT INTO document_type (type_name, description, is_required) VALUES
    ('ID Document',              'National ID or Passport',             TRUE),
    ('Proof of Address',         'Utility bill or bank statement',      TRUE),
    ('Proof of Enrollment',      'Current enrollment verification',     TRUE),
    ('Financial Aid Application','Application for financial assistance', FALSE),
    ('Medical Certificate',      'Health clearance certificate',        FALSE)
ON CONFLICT (type_name) DO NOTHING;


-- Accommodation types
INSERT INTO accommodation_type (name, description, is_active) VALUES
    ('Single Room',       'Private room for one occupant',              TRUE),
    ('Double Room',       'Shared room for two occupants',              TRUE),
    ('Studio Apartment',  'Self-contained studio with kitchenette',     TRUE),
    ('En-suite Room',     'Private room with attached bathroom',        TRUE)
ON CONFLICT (name) DO NOTHING;


-- Academic year 2026 (application window extended to June 2026)
INSERT INTO academic_year
    (name, start_date, end_date, application_open_date, application_close_date, is_current, tenant_id)
VALUES
    ('2026', '2026-02-01', '2026-11-30', '2025-09-01', '2026-06-30', TRUE,
     (SELECT id FROM tenant WHERE name = 'Default Tenant'))
ON CONFLICT (name) DO NOTHING;


-- System settings
INSERT INTO system_settings (setting_key, setting_value, description, tenant_id)
VALUES (
    'auto_allocation_enabled',
    'false',
    'Enable or disable automatic room allocation for approved applications',
    (SELECT id FROM tenant WHERE name = 'Default Tenant')
)
ON CONFLICT (setting_key) DO NOTHING;


-- Default admin user
-- Email:    admin@studentaccommodation.com
-- Password: Admin@123456  (BCrypt 10 rounds — MUST be changed on first login)
INSERT INTO app_user
    (email, user_password, user_role, first_name, last_name,
     email_verified, must_change_password, tenant_id, created_at, updated_at)
SELECT
    'admin@studentaccommodation.com',
    '$2b$10$u3zQI5mTrJaHKxmC72CAmOmvPysTBXsB6JwsGaEWK8CQQ7DEQua6u',
    'ADMIN',
    'System',
    'Administrator',
    true,
    true,
    (SELECT id FROM tenant WHERE name = 'Default Tenant'),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM app_user WHERE email = 'admin@studentaccommodation.com'
);


-- FF4J feature flags
-- FF4jConfig reads these at startup and calls ff4j.enable/disable; the row must exist.
-- 'enable' is set false here; FF4jConfig will update it based on application.properties.
INSERT INTO ff4j_features (uid, enable, description)
VALUES ('auto-allocation', false, 'Enable automatic room allocation for approved applications')
ON CONFLICT (uid) DO NOTHING;
