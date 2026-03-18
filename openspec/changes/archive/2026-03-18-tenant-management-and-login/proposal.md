## Why

The platform has a complete multi-tenancy data layer (Tenant entity, TenantService, TenantController) but no way for a Platform Admin to create or manage tenants through the UI, and no way to create or authenticate as a tenant-scoped admin user. Tenant management is a prerequisite for onboarding new institutions to the system.

## What Changes

- **New admin UI page** for tenant management: list, create, edit, and activate/deactivate tenants (CRUD)
- **Backend CRUD endpoints** for tenants: `POST`, `PUT /{id}`, `DELETE /{id}` (the existing GET endpoints remain unchanged)
- **Tenant admin user support**: When a Platform Admin creates a user with the `ADMIN` role, the UI must require a tenant selection (not `tenantId=0`), so that user is scoped to a specific institution
- **Sidebar navigation entry** for "Tenants" visible only to `PLATFORM_ADMIN` users
- **Login works as-is** — tenant-scoped admin users already use the same JWT auth flow; the gap is only that they cannot currently be created through the UI

## Capabilities

### New Capabilities
- `tenant-crud`: Full CRUD API (create, update, deactivate/activate) for tenants, restricted to `PLATFORM_ADMIN`
- `tenant-management-ui`: Angular admin page at `/admin/tenants` for listing, creating, editing, and toggling tenant active state; sidebar entry visible only to Platform Admins

### Modified Capabilities
- `tenant-management-api`: **Add** POST (create), PUT (update), and PATCH (toggle active) endpoints to the existing read-only spec
- `frontend-multi-tenancy`: **Add** requirement that creating an `ADMIN` user requires a tenant selection (currently only `BUILDING_OWNER` had this restriction)

## Impact

- Backend: `TenantController` — new write endpoints; `TenantService` — new create/update/deactivate methods; new `TenantRequestDto` / `TenantResponseDto`; new Flyway migration to add any missing constraints
- Frontend: new `tenant-management/` feature module with list + form dialog; `admin-sidebar` nav update; `UserManagementComponent` create-user dialog update to require tenant for `ADMIN` role; `TenantService` — add create/update/toggle methods
- No breaking changes to existing GET endpoints or authentication flow
