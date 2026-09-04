## Why

The codebase has two coexisting multi-tenancy models with no canonical strategy document, causing confusion about which model to apply and when. Additionally, the frontend `TenantService` calls `GET /api/admin/tenants` and `GET /api/admin/tenants/{id}` — but no `TenantController` exists in the backend, causing 404 errors and breaking the tenant-selector dropdown in the Create User dialog.

## What Changes

- Document the canonical dual-model multi-tenancy strategy (tenant-scoped model for org users, platform-level model for students/platform admins)
- Implement `TenantController` to expose `GET /api/admin/tenants` and `GET /api/admin/tenants/{id}` endpoints for `PLATFORM_ADMIN` role
- Frontend `TenantService` calls are already correct — they just need the backend to exist

## Capabilities

### New Capabilities

- `multi-tenancy-strategy`: Documents the dual-model tenancy design — when tenant-scoped model applies, when platform-level sentinel applies, and how `TenantFilterConfig` activates Hibernate filters per request
- `tenant-management-api`: New `TenantController` exposing read-only tenant list and detail endpoints secured to `PLATFORM_ADMIN`

### Modified Capabilities

- `frontend-multi-tenancy`: Tenant-selector dropdown in `CreateUserDialogComponent` now has a real API to call — update spec to reflect the working API contract

## Impact

- **New file**: `backend/src/main/java/com/beezhub/student_accommodation/controller/TenantController.java`
- **Modified**: Spring Security config to permit `PLATFORM_ADMIN` access to `/api/admin/tenants/**`
- **No schema changes**: `tenant` table and all `tenant_id` FK columns are already in place via Flyway migrations
- **No frontend changes**: `TenantService` already calls the correct endpoints
