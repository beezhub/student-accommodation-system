## 1. Application Tenant Assignment (Backend)

- [x] 1.1 Add validation in `ApplicationCoreService.submitApplication()`: require at least one building preference (reject with `400` if none)
- [x] 1.2 After resolving building preferences, collect all distinct `tenant_id` values from the preference buildings and reject with `400` if more than one distinct tenant is found
- [x] 1.3 Explicitly call `application.setTenantId(building.getTenantId())` on the first building preference before `applicationRepository.save()`, bypassing `TenantListener` automatic assignment
- [x] 1.4 Update `ApplicationRequest` DTO validation: add a custom validator or `@AssertTrue` that requires at least one of `preferredBuildingId` or `preferredBuildingIds` to be non-empty
- [x] 1.5 Relax the duplicate-application check from per-student global to per-student-per-tenant: replace `existsByStudent_IdAndStatus(studentId, PENDING)` with `existsByStudent_IdAndTenantIdAndStatus(studentId, tenantId, PENDING)` — add the repository method if needed
- [x] 1.6 Write unit tests for the cross-tenant rejection, the tenant assignment, and the per-tenant duplicate check in `ApplicationCoreServiceTest`

## 2. Backfill Migration

- [x] 2.1 Create Flyway migration `V2026_03_19_HHMM__backfill_application_tenant_id.sql` that updates `application.tenant_id` from the first `application_building_preference` (lowest `preference_order`) joined to `building.tenant_id` where `application.tenant_id = 0`

## 3. Controller / Security Audit (Backend)

- [x] 3.1 Audit `ActivityLogController` — confirm all methods have `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` (already done; verify no gaps)
- [x] 3.2 Audit `SystemSettingController` — add `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` to all methods if not already present
- [x] 3.3 Audit `FeatureController` — add `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` to all methods if not already present
- [x] 3.4 Audit `TenantController` and `TenantAdminController` — confirm all methods restrict to `PLATFORM_ADMIN`
- [x] 3.5 Audit `AdminStatsController` — confirm it uses the Hibernate filter (no cross-tenant stats leak to TENANT_ADMIN); if the stats queries bypass the filter, add explicit `tenantId` filtering in the service
- [x] 3.6 Audit `UserManagementController` — restrict user-listing and user-management endpoints to `PLATFORM_ADMIN`; TENANT_ADMIN user management (their own tenant's admins) is handled via `TenantAdminController` only
- [x] 3.7 Write integration tests: log in as TENANT_ADMIN → assert `403` on `/api/admin/activity-logs`, `/api/admin/settings`, `/api/admin/features`, `/api/admin/tenants`

## 4. Tenant-Scoped Data Verification (Backend Integration Tests)

- [x] 4.1 Write integration test: seed two tenants, seed buildings and applications for each; TENANT_ADMIN of tenant A calls `GET /api/admin/applications` → only tenant A's applications returned
- [x] 4.2 Write integration test: TENANT_ADMIN requests application by ID belonging to another tenant → `404 Not Found`
- [x] 4.3 Write integration test: TENANT_ADMIN calls `GET /api/admin/buildings` → only their tenant's buildings returned

## 5. Frontend — Sidebar and Route Guards

- [x] 5.1 Add a `platformOnly: boolean` property to the nav group/item interface in `admin-sidebar.component.ts`
- [x] 5.2 Mark the Tenant Management, System Settings, Feature Flags, Activity Logs, and User Management nav groups as `platformOnly: true`
- [x] 5.3 Inject `AuthService` into `AdminSidebarComponent`; expose `isPlatformAdmin$` observable and use it in the template to conditionally render `platformOnly` groups
- [x] 5.4 Create (or extend) a `PlatformAdminGuard` that returns `false` and redirects to `/admin` if the user's role is `TENANT_ADMIN`
- [x] 5.5 Apply `PlatformAdminGuard` to the routes for `/admin/tenants`, `/admin/settings`, `/admin/feature-flags`, `/admin/activity-logs`, and `/admin/users` in `app-routing.module.ts`

## 6. Frontend — First-Login Password Change

- [x] 6.1 After a successful login API response, check `mustChangePassword` in the JWT payload (extract via `AuthService.decodeToken()` or a dedicated method)
- [x] 6.2 If `mustChangePassword === true` and role is `TENANT_ADMIN`, navigate to `/auth/change-password` instead of `/admin`
- [x] 6.3 Add a `MustChangePasswordGuard` on `/admin/**` routes that redirects to `/auth/change-password` if the flag is still `true`
- [x] 6.4 On successful password change, clear the `mustChangePassword` flag from the local auth state and allow normal navigation

## 7. Smoke Test & Cleanup

- [ ] 7.1 End-to-end smoke test: create a tenant + TENANT_ADMIN via Swagger; log in as TENANT_ADMIN; verify dashboard loads with scoped data; verify platform-only links are absent; verify 403 on direct platform-only API calls
- [ ] 7.2 Verify that PLATFORM_ADMIN still sees all tenants' data (no regression)
- [x] 7.3 Remove any dead code or commented-out role checks left over from the audit
