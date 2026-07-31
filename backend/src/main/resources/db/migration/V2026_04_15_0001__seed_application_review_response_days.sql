-- Seed platform default for application review response days (business days)
INSERT INTO system_settings (setting_key, setting_value, description, tenant_id)
VALUES (
    'application_review_response_days',
    '2',
    'Expected application review turnaround shown to students, in business days',
    0
)
ON CONFLICT (tenant_id, setting_key) DO NOTHING;
