-- Performance index for provisioning queries that filter by tenant_id + user_role
-- (e.g. listing all TENANT_ADMIN users for a given tenant)
CREATE INDEX IF NOT EXISTS idx_app_user_tenant_id_user_role
    ON app_user (tenant_id, user_role);
