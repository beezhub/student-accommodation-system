## Context

The system is a multi-tenant student accommodation platform. Each **tenant** is an institution (university, college) identified by `tenant_id`. The `app_user` table already carries a `tenant_id` column and a `user_role` enum. Flyway migrations manage all schema changes. The multi-tenancy filter (`TenantFilterConfig`, `TenantContext`) scopes Hibernate queries to the active tenant automatically.

Current roles:
- `PLATFORM_ADMIN` — full access, `tenant_id = 0` (PLATFORM tenant)
- `STUDENT` — applicant access, `tenant_id = 0` (students belong to PLATFORM tenant)
- `BUILDING_STAFF` — partial operational access
- `ADMIN` — deprecated legacy role

There is no role for a user who administers a single tenant's accommodation operations. All admin actions currently require `PLATFORM_ADMIN`, meaning the platform operator acts as every institution's admin.

## Goals / Non-Goals

**Goals:**
- Introduce `TENANT_ADMIN` role — an institutional user with admin privileges scoped to their own tenant
- Provide a `PLATFORM_ADMIN`-only provisioning API to create, list, and deactivate `TENANT_ADMIN` users per tenant
- Enforce strict tenant isolation: `TENANT_ADMIN` can only read/write data in their own `tenant_id`
- Define the exact admin dashboard sections a `TENANT_ADMIN` can access vs. those reserved for `PLATFORM_ADMIN`
- Forced password change on first login for provisioned accounts

**Non-Goals:**
- Self-registration for `TENANT_ADMIN` (always provisioned by platform)
- `TENANT_ADMIN` creating other `TENANT_ADMIN` accounts
- Fine-grained permissions within a tenant (e.g., read-only vs. full admin) — all `TENANT_ADMIN` users have the same permission set within their tenant
- Multi-tenant membership (one admin user belongs to exactly one tenant)

## Decisions

### Decision 1: Reuse `AppUser` + add `TENANT_ADMIN` to `UserRole` enum

**Choice:** Add `TENANT_ADMIN` to the existing `UserRole` enum. No new table needed.

**Rationale:** `app_user` already has `tenant_id` and `user_role`. A `TENANT_ADMIN` user is simply an `AppUser` with `user_role = TENANT_ADMIN` and a non-zero `tenant_id`. Creating a separate table would duplicate the user lifecycle (auth, locking, password reset) already built for `AppUser`.

**Alternative considered:** A separate `tenant_admin` table with a FK to `app_user`. Rejected — unnecessary indirection when the existing `user_role` column is sufficient.

### Decision 2: `TENANT_ADMIN` provisioned only by `PLATFORM_ADMIN`

**Choice:** New endpoints `GET/POST /api/admin/tenants/{tenantId}/admins` and `DELETE /api/admin/tenants/{tenantId}/admins/{userId}` — all require `PLATFORM_ADMIN`.

**Rationale:** Prevents tenants from expanding their own admin count without oversight. Matches the institutional onboarding model where the platform operator controls who can administer a tenant.

**Alternative considered:** Allow `TENANT_ADMIN` to invite other `TENANT_ADMIN` users within their tenant. Rejected for now — adds complexity and weakens platform control. Can be added later as a feature flag.

### Decision 3: Tenant isolation via `TenantContext` + `@PreAuthorize` checks

**Choice:** When a `TENANT_ADMIN` calls any admin endpoint, the backend resolves their `tenant_id` from the JWT/`UserDetails` and sets `TenantContext` accordingly. Existing Hibernate tenant filter then scopes all queries automatically. For write operations, an explicit `@PreAuthorize` check ensures the target resource belongs to the caller's tenant.

**Rationale:** The tenant filter is already in place for PLATFORM use. `TENANT_ADMIN` simply activates the same filter with their own `tenant_id` instead of bypassing it (as `PLATFORM_ADMIN` currently does).

**Alternative considered:** Separate service-layer "tenant check" methods duplicated per endpoint. Rejected — the existing Hibernate filter + a `TenantContextLoader` aspect is cleaner and centrally enforceable.

### Decision 4: Dashboard scoping via frontend role check + backend enforcement

**Choice:** The Angular admin sidebar hides PLATFORM-only sections (`Tenant Management`, `System Settings`, `Feature Flags`, `Activity Logs`) when `currentUser.role === 'TENANT_ADMIN'`. Backend always enforces; frontend is convenience only.

**`TENANT_ADMIN` has access to:**
| Module | Access |
|---|---|
| Dashboard (stats for their tenant) | ✅ |
| Buildings (their tenant only) | ✅ |
| Rooms (their tenant only) | ✅ |
| Applications (their tenant only) | ✅ |
| Allocations / Staged Allocations (their tenant) | ✅ |
| Documents (their tenant only) | ✅ |
| Notifications (their tenant only) | ✅ |
| Students (their tenant only) | ✅ |
| Allocation Rules (their tenant) | ✅ |
| Tenant Management | ❌ PLATFORM_ADMIN only |
| System Settings | ❌ PLATFORM_ADMIN only |
| Feature Flags | ❌ PLATFORM_ADMIN only |
| Activity Logs | ❌ PLATFORM_ADMIN only |
| User Management (tenant admin users) | ❌ PLATFORM_ADMIN only |

### Decision 5: Forced password change flow for provisioned accounts

**Choice:** `PLATFORM_ADMIN` sets an initial password; `must_change_password = true` is set at creation. The JWT interceptor on the frontend redirects to a password-change page when this flag is present in the token claims.

**Rationale:** `must_change_password` already exists on `AppUser`. No schema change needed.

## Risks / Trade-offs

- **Risk: `PLATFORM_ADMIN` inadvertently assigns `TENANT_ADMIN` to wrong tenant** → Mitigation: The create endpoint validates the `tenantId` path param against existing tenants and rejects if the tenant does not exist or is inactive.
- **Risk: Hibernate tenant filter bypass via native queries** → Mitigation: Native queries in the codebase must be audited; add integration tests that assert cross-tenant data is unreachable from a `TENANT_ADMIN` token.
- **Risk: Activity logs hidden from `TENANT_ADMIN` may reduce auditability for the institution** → Trade-off accepted for now; a scoped activity log (filtered to their tenant) can be added in a future change.

## Migration Plan

1. Add `TENANT_ADMIN` to `UserRole` enum (backward-compatible — existing rows unaffected)
2. Flyway migration: no schema change required (columns exist); optionally add a DB index on `(tenant_id, user_role)` for provisioning query performance
3. Deploy backend with new endpoints and updated `SecurityConfig`
4. Deploy frontend with updated role guards and sidebar visibility logic
5. Rollback: remove `TENANT_ADMIN` enum value and revert `SecurityConfig` — no data migration needed if no `TENANT_ADMIN` users have been created

## Open Questions

- Should `TENANT_ADMIN` see a read-only view of **Activity Logs** scoped to their tenant? (deferred)
- Should there be a maximum number of `TENANT_ADMIN` users per tenant? (deferred — no limit imposed initially)
- Email notification to the provisioned `TENANT_ADMIN` user with login credentials? (assumed yes — reuse existing notification infrastructure)
