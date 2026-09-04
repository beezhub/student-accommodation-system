## Context

### Domain clarification
A **Tenant** is a property company that registers and manages buildings on the platform. A tenant can serve multiple academic institutions. An **Institution** is a university or college that students are enrolled in. Students browse buildings by filtering on their institution; those buildings may belong to any tenant that has registered buildings serving that institution. Tenant and Institution are therefore distinct concepts — a tenant is not a university.

A student submits one application **per tenant** (property company). If they want to be considered by two property companies, they submit two separate applications. This keeps each application cleanly scoped to one tenant from the moment of submission.

### Technical state
The multi-tenancy plumbing is complete: `TenantContext` is populated from the JWT `tenantId` claim in `JwtAuthenticationFilter`, and `TenantFilterConfig` enables the Hibernate `tenantFilter` for every request where `tenantId != null && tenantId != 0`. Entities that implement `TenantAware` (including `Application`, `Building`, `Room`, `AcademicYear`, etc.) carry an `@Filter` annotation that appends `WHERE tenant_id = :tenantId` to queries.

The gap: students carry `tenant_id = 0` (the platform sentinel), so `TenantListener.setTenantId()` stamps `0` onto every application they submit. Because `0` is the platform sentinel, the Hibernate filter is **not** activated for PLATFORM_ADMIN, and TENANT_ADMIN (who has a non-zero `tenantId`) sees **no** applications — the filter restricts them to records matching their `tenantId`, but applications have `0`.

There are also secondary gaps: some admin controllers explicitly restrict to `PLATFORM_ADMIN` (which is correct), but the `SecurityConfig` grants all of `/api/admin/**` to both roles — the per-controller `@PreAuthorize` annotations are the actual enforcement point and they are inconsistently applied. The Angular frontend has no role-based sidebar filtering or route guards for TENANT_ADMIN.

### Document scope (out of scope for this change)
`Document` currently implements `TenantAware` which is architecturally incorrect — documents are student-owned and platform-level. That will be corrected in a separate `document-pack` change. For this change, document scoping is left as-is.

## Goals / Non-Goals

**Goals:**
- TENANT_ADMIN sees only their tenant's buildings, rooms, applications, allocations, and documents via the Hibernate filter
- Applications submitted by students are stamped with the correct non-zero `tenant_id` at creation time
- Platform-only endpoints (`/activity-logs`, `/settings`, `/features`, `/tenants`) return `403` for TENANT_ADMIN
- Angular sidebar hides platform-only items for TENANT_ADMIN; direct-URL navigation is blocked by a route guard
- TENANT_ADMIN is redirected to change-password on first login when `mustChangePassword = true`

**Non-Goals:**
- Changing how PLATFORM_ADMIN sees data (they continue to see all tenants)
- Multi-tenant building preferences (a student applying to buildings across different tenants in one application)
- Student-facing UI changes
- Changes to the allocation scoring or batch job logic
- Document pack / platform-level document scoping (tracked separately)

## Decisions

### Decision 1 — Application tenant assignment: derive from first building preference (Option A)

**Chosen**: At `ApplicationCoreService.submitApplication()`, after resolving building preferences, read `building.getTenantId()` from the first (highest-priority) preferred building and explicitly set `application.setTenantId(tenantId)` before saving. The `TenantListener` will skip the automatic assignment because `tenantId` is no longer `null` on persist.

**Validation rule**: All buildings in the preference list must belong to the same tenant. If they span multiple tenants, return `400 Bad Request` with a clear message.

**Why over Option B (explicit institutionId field)**: Buildings already have `tenant_id`. Adding an `institutionId` field to the application form duplicates the association that the building preference already implies. It also changes the student-facing API unnecessarily.

**Why over Option C (custom join query, skip Hibernate filter)**: Bypassing the established filter pattern creates two code paths for tenant enforcement — increasing maintenance burden and the risk of future regressions. The Hibernate filter approach is cleaner and consistent with all other tenant-scoped entities.

**Edge case — no building preference**: If a student submits an application without any building preference (open/any allocation), the `tenant_id` cannot be inferred from a building. Two sub-options:
- *Reject*: Require at least one building preference. Simplest. Students must commit to at least one building.
- *Defer*: Accept with `tenant_id = 0` (platform-level) and assign to a tenant when a PLATFORM_ADMIN manually assigns a room.

**Recommendation**: Reject. Without a building preference the system cannot determine which property company the student is applying to. This is consistent with the domain model: an application is a request to a specific property company, identified by their buildings.

**Duplicate application constraint update**: The current code rejects any second `PENDING` application from the same student globally. Under the new model — one application per tenant — the constraint should be relaxed to: one `PENDING` application per student **per tenant**. The check `existsByStudent_IdAndStatus(studentId, PENDING)` becomes `existsByStudent_IdAndTenantIdAndStatus(studentId, tenantId, PENDING)`.

---

### Decision 2 — Controller enforcement strategy: per-method `@PreAuthorize`

Use `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` on endpoints that must be platform-only (activity logs, settings, feature flags, tenant management). Use `@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")` — or rely on the security filter chain's `hasAnyRole` rule — for endpoints that both roles share.

The role hierarchy (`PLATFORM_ADMIN > TENANT_ADMIN`) means `hasRole('TENANT_ADMIN')` would also pass for PLATFORM_ADMIN, but `hasRole('PLATFORM_ADMIN')` does **not** satisfy `hasRole('TENANT_ADMIN')` checks. For shared endpoints, either explicit `hasAnyRole` or removing the method-level annotation (falling back to the filter chain's `hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')`) is correct.

**Why not a custom `@TenantAdminOnly` / `@PlatformAdminOnly` annotation**: The existing `@PreAuthorize` is well-understood, IDE-indexed, and does not need a custom annotation. The number of affected methods is small (~5 platform-only controllers).

---

### Decision 3 — Frontend guard: role-aware `AdminGuard`

Extend the existing `admin.guard.ts` (or create a `TenantAdminGuard`) to check the user's role from the JWT/auth service. Platform-only routes (`/admin/tenants`, `/admin/settings`, `/admin/feature-flags`, `/admin/activity-logs`) get an additional guard that redirects TENANT_ADMIN to `/admin`.

Sidebar items in `admin-sidebar.component.ts` already use `navGroups`; add a `platformOnly: boolean` flag to each nav item group. The template conditionally renders groups based on `isPlatformAdmin$ | async`.

---

### Decision 4 — `mustChangePassword` redirect

On successful login, the JWT payload contains `mustChangePassword: true/false`. The Angular `AuthInterceptor` (or a login-response handler in `AuthService`) checks this flag. If true and the role is `TENANT_ADMIN`, redirect to `/auth/change-password` before allowing any other navigation. A `CanActivate` guard on all `/admin/**` routes can enforce this by checking `authService.mustChangePassword()`.

## Risks / Trade-offs

- **Students without building preferences cannot submit** → Mitigation: add clear validation error in UI and API. Future enhancement: allow open preferences when a platform-level workflow supports it.
- **Applications with `tenant_id = 0` already in the DB** → Mitigation: one-time migration script to backfill `tenant_id` on existing applications from their building preference (if available), or leave them visible only to PLATFORM_ADMIN. Add to migration plan.
- **Role hierarchy edge case in `@PreAuthorize`**: Because `PLATFORM_ADMIN > TENANT_ADMIN`, `hasRole('TENANT_ADMIN')` is satisfied by both roles. Review every method that uses `hasRole('TENANT_ADMIN')` to confirm PLATFORM_ADMIN should also pass.

## Migration Plan

1. Deploy backend changes (application tenant assignment, controller `@PreAuthorize` audit).
2. Run backfill migration for existing applications that have `tenant_id = 0` and a resolvable building preference.
3. Deploy frontend changes (sidebar filtering, route guards, password-change redirect).
4. Smoke test: log in as TENANT_ADMIN → verify scoped data, verify 403 on platform endpoints, verify first-login redirect.

## Open Questions

1. ~~**Application with no building preference**~~ — **Resolved**: Reject with `400`. A building preference is required to identify the property company.

2. ~~**Backfill strategy**~~ — **Resolved**: Flyway migration that updates `tenant_id` from the first building preference record where `application.tenant_id = 0`.

3. **AcademicYear scoping**: `AcademicYear` is `TenantAware`. Should TENANT_ADMIN manage their own academic years, or should there be platform-level academic years shared across tenants?
   *Out of scope for this change* — existing tests use tenant-specific academic years; preserve that behaviour.

4. **Document scoping**: `Document` is incorrectly `TenantAware`. Deferred to `document-pack` change.
