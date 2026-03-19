-- Fix system_settings seed: move auto_allocation_enabled to the PLATFORM tenant (tenant_id = 0).
-- The original seed (V2026_02_19_2101) inserted this row under 'Default Tenant'.
-- With the cascade lookup model, platform-level defaults must live at tenant_id = 0.

DELETE FROM system_settings
WHERE setting_key = 'auto_allocation_enabled'
  AND tenant_id != 0;

INSERT INTO system_settings (setting_key, setting_value, description, tenant_id)
VALUES (
    'auto_allocation_enabled',
    'false',
    'Enable or disable automatic room allocation for approved applications',
    0
)
ON CONFLICT (tenant_id, setting_key) DO NOTHING;
