-- GH-212: accounts already stamped with the platform tenant keep platform-wide read access.
--
-- PR #211 stopped new accounts being stamped with the platform tenant, and
-- V2026_09_05_1300__backfill_tenant_ids.sql repaired the rows whose owner could be derived from a
-- parent. app_user rows could not be: an admin account has no column naming the company that
-- created it, and guessing would be worse than leaving them visible.
--
-- Tenant 0 is the sentinel that disables the Hibernate tenant filter and makes
-- TenantOwnershipGuard return without checking, so every TENANT_ADMIN and BUILDING_STAFF account
-- stamped 0 can still read every company's data.
--
-- These accounts are locked rather than left working. They hold cross-tenant read access they were
-- never meant to have, and the account's own company cannot be recovered from the database. A
-- platform admin reassigns each one through GET/POST /api/admin/unassigned-admins, which sets the
-- tenant, unlocks the account and retires its existing tokens.
--
-- Locking, not disabling: the password hash is untouched, so reassignment restores the account as
-- it was. This is the same mechanism TenantAdminService.deactivateTenantAdmin and
-- V2026_09_05_1000__disable_seeded_admin.sql already use.

UPDATE app_user
SET account_locked_until = TIMESTAMP '9999-12-31 23:59:59',
    updated_at           = CURRENT_TIMESTAMP
WHERE tenant_id = 0
  AND user_role IN ('TENANT_ADMIN', 'BUILDING_STAFF')
  AND (account_locked_until IS NULL OR account_locked_until < TIMESTAMP '9999-12-31 23:59:59');

-- Deliberately NOT locked here: user_role = 'ADMIN'.
--
-- ADMIN is the deprecated platform-admin role kept for the migration window — UserRole.isAdmin()
-- groups it with PLATFORM_ADMIN, the frontend's RoleService.isPlatformAdmin() returns true for it,
-- and V2026_03_19_0002__fix_platform_admin_tenant_id.sql deliberately moved platform operators to
-- tenant 0. So an ADMIN row at tenant 0 is most likely a legitimate platform operator, and locking
-- it could lock out the very account needed to perform the reassignments below.
--
-- They are still worth reviewing by hand, because a tenant-scoped admin created before the role
-- split would look identical. List them with:
--   SELECT id, email, user_role FROM app_user WHERE tenant_id = 0 AND user_role = 'ADMIN';
-- and either move each to PLATFORM_ADMIN or assign it a company through the endpoint above.
--
-- Note also that a locked account's already-issued JWT still carries tenant 0 until it expires;
-- the reassignment endpoint revokes those explicitly rather than waiting them out.
