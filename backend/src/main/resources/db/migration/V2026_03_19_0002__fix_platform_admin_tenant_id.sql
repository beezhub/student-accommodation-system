-- =============================================================================
-- Fix PLATFORM_ADMIN tenant_id to use the PLATFORM sentinel (id=0).
--
-- The original seed (V2026_02_19_2101) created the admin user with tenant_id=1
-- (Default Tenant). The role migration (V2026_02_25_0001) updated the role to
-- PLATFORM_ADMIN but did not update tenant_id.
--
-- PLATFORM_ADMIN is a platform-level operator and must have tenant_id=0 so that
-- TenantFilterAspect skips the Hibernate tenant filter, giving them visibility
-- across all tenants.
-- =============================================================================

UPDATE app_user
SET tenant_id = 0
WHERE user_role = 'PLATFORM_ADMIN';
