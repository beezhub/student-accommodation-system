## 1. Backend — Role & Auth

- [x] 1.1 Add `TENANT_ADMIN` to the `UserRole` enum and update `isAdmin()` helper to include it
- [x] 1.2 Update `SecurityConfig` to allow `TENANT_ADMIN` access to building, room, application, allocation, document, and notification endpoints (alongside `PLATFORM_ADMIN`)
- [x] 1.3 Update `SecurityConfig` to keep tenant management, system settings, feature flags, and activity log endpoints restricted to `PLATFORM_ADMIN` only
- [x] 1.4 Update `CustomUserDetailsService` / JWT claim builder to include `tenantId` in the token for `TENANT_ADMIN` users
- [x] 1.5 Add a `TenantContextLoader` mechanism (AOP or filter) that sets `TenantContext` from the authenticated user's `tenantId` when the caller is a `TENANT_ADMIN`

## 2. Backend — Provisioning API

- [x] 2.1 Create `TenantAdminRequestDto` (firstName, lastName, email, password) and `TenantAdminResponseDto` (id, email, firstName, lastName, createdAt, mustChangePassword, accountLockedUntil)
- [x] 2.2 Create `TenantAdminService` with methods: `createTenantAdmin(tenantId, request)`, `listTenantAdmins(tenantId)`, `deactivateTenantAdmin(tenantId, userId)`
- [x] 2.3 Implement `createTenantAdmin`: validate tenant exists and is active, check email uniqueness, create `AppUser` with `user_role=TENANT_ADMIN`, `tenant_id=tenantId`, `must_change_password=true`, `email_verified=true`
- [x] 2.4 Implement `listTenantAdmins`: query `AppUser` by `tenant_id` and `user_role=TENANT_ADMIN`
- [x] 2.5 Implement `deactivateTenantAdmin`: verify user belongs to tenant as `TENANT_ADMIN`, set `account_locked_until` to far-future date
- [x] 2.6 Create `TenantAdminController` with endpoints: `GET /api/admin/tenants/{tenantId}/admins`, `POST /api/admin/tenants/{tenantId}/admins`, `DELETE /api/admin/tenants/{tenantId}/admins/{userId}` — all secured with `PLATFORM_ADMIN`
- [x] 2.7 Register welcome email notification: on successful `createTenantAdmin`, send async welcome email with login URL and password-change prompt

## 3. Backend — Tenant Isolation Enforcement

- [x] 3.1 Audit all admin service methods (BuildingService, RoomService, ApplicationService, RoomAllocationService, StagedAllocationService, DocumentService, NotificationService) to verify they respect `TenantContext` filtering when active
- [x] 3.2 Add explicit tenant ownership check in service methods that accept a resource ID (e.g., `getBuilding(id)`) — return `404` if the resource's `tenant_id` does not match the active tenant context
- [ ] 3.3 Write integration tests asserting that a `TENANT_ADMIN` JWT cannot retrieve or modify resources belonging to a different tenant

## 4. Backend — Flyway Migration

- [x] 4.1 Create Flyway migration `V2026_03_18_0001__add_tenant_id_user_role_index.sql` adding an index on `app_user(tenant_id, user_role)` to support efficient provisioning queries (optional but recommended)

## 5. Frontend — Auth & Routing

- [x] 5.1 Add `TENANT_ADMIN` to the `UserRole` TypeScript enum / model
- [x] 5.2 Update `AdminGuard` to allow both `PLATFORM_ADMIN` and `TENANT_ADMIN` to activate admin routes
- [x] 5.3 Create or update `PlatformAdminGuard` (or equivalent) to restrict platform-only routes (`/admin/tenants`, `/admin/settings`, `/admin/feature-flags`, `/admin/activity-logs`, `/admin/users`) to `PLATFORM_ADMIN` only
- [x] 5.4 Update `app-routing.module.ts` to apply `PlatformAdminGuard` on platform-only routes and redirect `TENANT_ADMIN` to `/admin` if they navigate there directly

## 6. Frontend — Admin Sidebar

- [x] 6.1 Update the `navGroups` array in `admin-sidebar.component.ts` to conditionally hide platform-only nav items (`Tenant Management`, `System Settings`, `Feature Flags`, `Activity Logs`, `User Management`) when `currentUser.role === 'TENANT_ADMIN'`
- [x] 6.2 Verify the dashboard stats component fetches from tenant-scoped endpoints (stats should already be scoped via backend; confirm no platform-wide aggregation is shown to `TENANT_ADMIN`)

## 7. Frontend — Forced Password Change

- [x] 7.1 Update the auth token interceptor / login flow to check `mustChangePassword` claim in the JWT and redirect to `/change-password` for `TENANT_ADMIN` users before any other navigation
- [x] 7.2 Ensure the change-password form clears the `must_change_password` flag on the backend upon successful submission (this should already be handled by the existing password-change endpoint; verify and wire up if not)

## 8. Frontend — Tenant Admin Management UI (Platform Admin)

- [x] 8.1 Create `TenantAdminManagementComponent` under `features/admin/tenant-management/` — a sub-page accessible when viewing a specific tenant, listing its admin users
- [x] 8.2 Add "Create Admin User" form/dialog with fields: firstName, lastName, email, password — calling `POST /api/admin/tenants/{tenantId}/admins`
- [x] 8.3 Add "Deactivate" action per admin user row — calling `DELETE /api/admin/tenants/{tenantId}/admins/{userId}` with confirmation dialog
- [x] 8.4 Display `mustChangePassword` status and `accountLockedUntil` in the admin user list table

## 9. Testing

- [x] 9.1 Integration test: `PLATFORM_ADMIN` can create, list, and deactivate a `TENANT_ADMIN` via provisioning API
- [x] 9.2 Integration test: `TENANT_ADMIN` can access their tenant's buildings, rooms, applications, and allocations
- [x] 9.3 Integration test: `TENANT_ADMIN` cannot access another tenant's resources (expect 404)
- [x] 9.4 Integration test: `TENANT_ADMIN` receives `403` on platform-only endpoints (tenant management, settings, feature flags, activity logs)
- [x] 9.5 Frontend unit test: `AdminGuard` allows `TENANT_ADMIN`; `PlatformAdminGuard` blocks `TENANT_ADMIN`
- [x] 9.6 Frontend unit test: sidebar hides platform-only nav items for `TENANT_ADMIN`
