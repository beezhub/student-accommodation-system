## Why

System settings are currently accessible only to PLATFORM_ADMIN, with all rows owned by the "Default Tenant" rather than the PLATFORM tenant (`tenant_id = 0`). Now that the system is multi-tenant, institutions (ADMIN role) need to be able to override platform-level defaults for their own tenant while the platform retains the ability to set authoritative defaults — a cascade lookup model.

## What Changes

- **Cascade lookup**: `getSetting(key)` checks the current tenant's row first; if absent, falls back to the PLATFORM tenant (`tenant_id = 0`)
- **Merged view**: `getAllSettings()` returns a merged map — platform defaults overridden by any tenant-specific rows
- **Tenant admin write access**: ADMIN users can create/update/delete setting overrides scoped to their own tenant
- **API split**: Platform settings management moves to `/api/platform/system-settings`; tenant-effective settings exposed at `/api/admin/system-settings`
- **Seed fix**: `auto_allocation_enabled` is re-seeded under `tenant_id = 0` (PLATFORM) rather than "Default Tenant"
- **Frontend**: Tenant admin settings page shows effective (cascade-resolved) values with ability to override; PLATFORM_ADMIN retains a separate platform-settings management view

## Capabilities

### New Capabilities

- `tenant-setting-override`: Tenant admins (ADMIN role) can read their effective settings and write per-tenant overrides that cascade over platform defaults

### Modified Capabilities

- `tenant-management-api`: Platform settings API endpoints are split — platform defaults management moves to a new `/api/platform/` prefix, tenant-effective view stays at `/api/admin/`

## Impact

- **Backend**: `SystemSettingService`, `SystemSettingController` — cascade logic, new endpoints
- **Database**: Seed migration corrected to target PLATFORM tenant; no schema changes required
- **Frontend**: `system-settings` component and `SystemSettingsService` — new endpoint split, override UI for ADMIN role
- **Security**: New `PUT /api/admin/system-settings/{key}` reachable by ADMIN (not just PLATFORM_ADMIN)
