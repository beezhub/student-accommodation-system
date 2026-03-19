## Context

System settings are currently stored in `system_settings` with `tenant_id` already present and a composite unique constraint `(tenant_id, setting_key)`. The Hibernate tenant filter scopes all queries to the active tenant. The service exposes a single flat key-value API locked to `PLATFORM_ADMIN`. The seed inserts `auto_allocation_enabled` under "Default Tenant" rather than the PLATFORM tenant (`tenant_id = 0`).

The goal is to introduce a two-tier cascade model: platform defaults at `tenant_id = 0`, tenant overrides at the tenant's own ID, with ADMIN users able to manage their own tier.

## Goals / Non-Goals

**Goals:**
- Cascade lookup: tenant row → platform row → null
- Merged `getAllSettings()` view
- Split API: `/api/platform/system-settings` (PLATFORM_ADMIN) and `/api/admin/system-settings` (ADMIN + PLATFORM_ADMIN)
- ADMIN can create, update, and delete overrides for their own tenant
- Fix seed to target `tenant_id = 0`

**Non-Goals:**
- Defining which settings are "overridable" vs "platform-only" at a type-system level (enforced by convention/UI, not code guard)
- Cascading more than two levels (tenant → platform is sufficient)
- Frontend implementation of the platform settings management page (backend API only; frontend settings hub for ADMIN is in scope)

## Decisions

### D1: Bypass Hibernate filter for platform fallback lookup
The Hibernate `@TenantFilter` scopes all queries to the active `tenant_id`. For the fallback query (looking up `tenant_id = 0`) we must bypass the filter.

**Approach:** Use a native query or temporarily disable the filter on the `EntityManager` session for the fallback read. Preferred: inject `EntityManager`, disable filter, execute find, re-enable. This is a localised pattern already used elsewhere in the codebase.

**Alternative considered:** Store platform settings in a separate table. Rejected — unnecessary complexity; the composite unique constraint already handles co-existence of platform and tenant rows for the same key.

### D2: Service method split vs single method with role parameter
`SystemSettingService` will expose:
- `getSetting(String key)` — cascade lookup (tenant → platform), used internally and by the ADMIN-facing controller
- `getPlatformSetting(String key)` — reads `tenant_id = 0` directly, used by the PLATFORM_ADMIN-facing controller
- `getAllSettings()` — merged map for current tenant
- `getAllPlatformSettings()` — platform-only map
- `updateSetting(String key, String value)` — upsert for current tenant (used by ADMIN endpoint)
- `updatePlatformSetting(String key, String value)` — upsert for `tenant_id = 0` (used by PLATFORM_ADMIN endpoint)
- `deleteSetting(String key)` — remove current tenant's override

### D3: New controller vs extending existing SystemSettingController
Two options:
1. Add platform endpoints to `SystemSettingController` with separate `@PreAuthorize`
2. Create a new `PlatformSettingController`

**Decision:** New `PlatformSettingController` at `/api/platform/system-settings`. Keeps the two access tiers clearly separated at the class level, easier to audit. `SystemSettingController` keeps the `/api/admin/` prefix and opens to `ADMIN` role.

### D4: DELETE endpoint returns 404 on missing override
Rather than silently succeeding (idempotent delete), return `404 Not Found` when no tenant override row exists. Rationale: callers invoking DELETE likely expect to revert something they set; a 404 surfaces incorrect assumptions about state.

### D5: Frontend scope
The ADMIN-facing settings hub already exists. Changes needed:
- `SystemSettingsService` updates `updateSetting` to also call the new DELETE endpoint for clearing overrides
- The auto-allocation toggle remains read-only for ADMIN (it is a platform setting; ADMIN can read the cascaded value but the toggle calls platform endpoint, which 403s for ADMIN — UI should hide or disable edit for that key)
- No new PLATFORM_ADMIN frontend page in this change (PLATFORM_ADMIN can use the existing page or Swagger for now)

## Risks / Trade-offs

- **Filter bypass coupling** → The EntityManager filter-disable pattern must be used carefully to avoid cross-tenant data leaks. Mitigation: always re-enable filter in a `finally` block; add integration test asserting tenant isolation.
- **Seed idempotency** → The existing seed used `ON CONFLICT (setting_key) DO NOTHING` (old constraint). After the unique constraint fix to `(tenant_id, setting_key)`, the conflict target must be updated in the corrective migration. Mitigation: new Flyway migration re-inserts under `tenant_id = 0` with correct conflict target.
- **ADMIN overriding platform kill-switch** → Currently no enforcement prevents an ADMIN from overriding `auto_allocation_enabled` at tenant level, potentially enabling allocation when platform wants it off. Mitigation: out of scope for this change; document as a known limitation and address with a setting-tier enum in a follow-up if needed.

## Migration Plan

1. New Flyway migration: delete existing `auto_allocation_enabled` row (if `tenant_id != 0`) and re-insert under `tenant_id = 0`.
2. Deploy backend with new service/controller logic.
3. No frontend deploy-order dependency; old frontend still works (existing endpoint signature unchanged for ADMIN GET/PUT).

## Open Questions

- Should `PLATFORM_ADMIN` calling `PUT /api/admin/system-settings/{key}` write to the platform tenant or to their own JWT's tenant context? Decision here: it writes to their JWT tenant context. To manage platform settings they use `/api/platform/` endpoint.
