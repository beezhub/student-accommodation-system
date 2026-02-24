-- TASK-1.03: Fix system_settings.setting_key uniqueness scope (global → per-tenant)
-- The previous UNIQUE(setting_key) constraint allowed only one value per key across all tenants.
-- Replace with a composite UNIQUE(tenant_id, setting_key) so each tenant can independently
-- configure settings without conflicting with other tenants.

ALTER TABLE system_settings DROP CONSTRAINT IF EXISTS system_settings_setting_key_key;

ALTER TABLE system_settings
    ADD CONSTRAINT uq_system_settings_tenant_key UNIQUE (tenant_id, setting_key);
