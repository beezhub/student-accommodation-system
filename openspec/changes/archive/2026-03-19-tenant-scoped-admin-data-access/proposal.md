## Why

The multi-tenancy infrastructure (Hibernate `tenantFilter`, `TenantContext`, JWT `tenantId` claim) is fully wired up, but TENANT_ADMIN users currently see **zero data** because all applications are stamped with `tenant_id = 0` at submission time (students carry the platform sentinel). This change closes the gap between the specified behaviour in existing specs and what the system actually enforces at runtime.

## What Changes

- **Application tenant assignment**: Define and implement how a submitted application acquires a non-zero `tenant_id` so the Hibernate filter exposes it to the correct TENANT_ADMIN. (Design discussed below — see open question.)
- **Controller/service audit**: Identify every `/api/admin/**` endpoint that either over-restricts to `PLATFORM_ADMIN` only (blocking TENANT_ADMIN) or under-restricts (leaking cross-tenant data). Apply correct `@PreAuthorize` annotations and/or service-layer ownership checks.
- **Platform-only endpoint enforcement**: Ensure that tenant management, system settings, feature flags, and activity logs respond `403` for TENANT_ADMIN — the security config allows `/api/admin/**` to both roles but individual controllers must restrict.
- **Frontend route guards**: Hide platform-only sidebar items for TENANT_ADMIN; add route guards that redirect `/admin/tenants`, `/admin/settings`, `/admin/feature-flags`, `/admin/activity-logs` back to `/admin` when the logged-in role is TENANT_ADMIN.
- **First-login password change**: Enforce the `mustChangePassword` redirect for TENANT_ADMIN users on the Angular side.

## Capabilities

### New Capabilities

- `tenant-application-assignment`: Defines the rule for how a student application receives a non-zero `tenant_id` at submission time, so it becomes visible to the owning TENANT_ADMIN via the Hibernate filter.

### Modified Capabilities

*(No existing spec-level requirements are changing — all required behaviours are already captured in `tenant-admin-dashboard-access`, `multi-tenancy-strategy`, `tenant-admin-provisioning`, and `frontend-multi-tenancy`. This change implements them.)*

## Open Design Question — Application Scoping

Applications are submitted by students (platform-level, `tenant_id = 0`). The `TenantListener` would currently stamp `tenant_id = 0` onto every application, making it invisible to any TENANT_ADMIN.

Three candidate approaches:

| Option | Mechanism | Trade-offs |
|--------|-----------|------------|
| **A — Derive from first building preference** | At submission, read `tenantId` from the first (or primary) preferred building. Set that as the application's `tenant_id`. | Simple. A student can only apply to buildings of one tenant per application. Multi-tenant preferences are rejected at validation. |
| **B — Explicit institution selection** | Add an `institutionId` field to the application form. The institution maps 1-to-1 with a tenant. The application's `tenant_id` is set from that institution's `tenantId`. | Clear intent. Requires the student to consciously choose an institution before selecting buildings. |
| **C — Platform-level + filtered join** | Applications stay at `tenant_id = 0`. TENANT_ADMIN access is via a custom query that joins `application → building_preference → building` and filters by building's `tenant_id`. The Hibernate filter is bypassed for applications. | No data model change. More complex queries; the Hibernate filter approach is not used for applications. |

**Recommendation**: Option A is the least disruptive. Buildings already carry `tenant_id`; the first building preference is the natural anchor. Validation should reject a preference list that spans multiple tenants.

> **Discussion needed**: Please confirm which option you prefer before the design is finalised.

## Impact

- **Backend**: `ApplicationCoreService.submitApplication()`, `TenantListener`, admin controllers (`ActivityLogController`, `SystemSettingController`, `FeatureController`, `ApplicationController`, `RoomAllocationController`, `DocumentController`, `BuildingController`, `AdminStatsController`)
- **Frontend**: `admin-sidebar.component.ts` nav group filtering, `app-routing.module.ts` route guards, auth interceptor for `mustChangePassword`
- **Database**: No schema changes required (application table already has `tenant_id`)
- **No breaking API changes** for existing PLATFORM_ADMIN flows
