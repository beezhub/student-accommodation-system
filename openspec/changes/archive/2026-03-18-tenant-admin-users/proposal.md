## Why

Currently the system has a `PLATFORM_ADMIN` role for the platform operator and a `STUDENT` role, but no role for the staff of a specific tenant (institution) who need to manage day-to-day accommodation operations — reviewing applications, allocating rooms, managing buildings, and handling documents — scoped to their institution only. Without tenant-scoped admin users, every institution must rely on the platform operator for all administrative actions, which does not scale.

## What Changes

- Introduce a new `TENANT_ADMIN` role for users who administer a specific tenant's accommodation operations
- Add a `TENANT_ADMIN` provisioning flow: `PLATFORM_ADMIN` creates tenant admin accounts for a given tenant
- Enforce tenant-scoped data isolation for `TENANT_ADMIN` users in all backend endpoints — they can only see and act on data belonging to their own `tenant_id`
- Expose a scoped admin dashboard view for `TENANT_ADMIN` that shows only their tenant's data
- Define the exact set of dashboard modules accessible to `TENANT_ADMIN` vs `PLATFORM_ADMIN` (see Capabilities)

## Capabilities

### New Capabilities

- `tenant-admin-provisioning`: `PLATFORM_ADMIN` can create, view, update, and deactivate `TENANT_ADMIN` users scoped to a specific tenant. Includes invite-by-email flow with a forced password-change on first login.
- `tenant-admin-dashboard-access`: `TENANT_ADMIN` users can access a scoped admin dashboard limited to their tenant's buildings, rooms, applications, allocations, documents, and notifications. Platform-level and cross-tenant sections are hidden/forbidden.

### Modified Capabilities

- `tenant-management-api`: Add a sub-resource endpoint `GET /api/admin/tenants/{id}/admins` and `POST /api/admin/tenants/{id}/admins` so `PLATFORM_ADMIN` can list and create `TENANT_ADMIN` users for a tenant.

## Impact

**Backend:**
- `UserRole` enum: add `TENANT_ADMIN`
- `SecurityConfig`: extend `hasRole` guards to allow `TENANT_ADMIN` access to building, room, application, allocation, document, and notification endpoints — but only for their `tenant_id`
- New `TenantAdminController` + `TenantAdminService` for provisioning
- All existing admin service methods must enforce `TenantContext` filtering when caller is `TENANT_ADMIN`
- New Flyway migration for any schema changes (none expected — `app_user.tenant_id` + `user_role` already handle the data model)

**Frontend:**
- `UserRole` model + auth guard updated to recognise `TENANT_ADMIN`
- Admin sidebar: conditionally hide platform-only sections (`Tenant Management`, `System Settings`, `Feature Flags`, `Activity Logs`) when logged in as `TENANT_ADMIN`
- Routing guards updated: `TENANT_ADMIN` can activate admin routes for the scoped modules

**APIs affected:** `/api/admin/tenants`, all building/room/application/allocation/document/notification endpoints
