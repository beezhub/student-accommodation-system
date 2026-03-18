## Context

The student accommodation system supports multiple institutions (tenants). The database has a `tenant` table and `tenant_id` FK columns on 14 core tables. Two distinct tenancy models exist:

1. **Tenant-Scoped Model** — used for org-level users (`BUILDING_OWNER`, `BUILDING_STAFF`) and their data. Entities implement `TenantAware` and carry Hibernate `@Filter(name="tenantFilter", condition="tenant_id = :tenantId")`. The `TenantFilterConfig` interceptor activates the filter per HTTP request when a real (non-zero) tenant ID is present in `TenantContext`.

2. **Platform-Level Model** — used for students and `PLATFORM_ADMIN`. Uses `TenantContext.PLATFORM_TENANT_ID = 0L` as a sentinel. When tenant ID is `0L` or `null`, the Hibernate filter is **not** activated, giving platform-level (cross-tenant) visibility. Students are scoped by `student_id`/`user_id` ownership at the service layer.

The gap: `TenantService` on the frontend calls `GET /api/admin/tenants` and `GET /api/admin/tenants/{id}` (returns 404 today) to populate the tenant-selector dropdown used when `PLATFORM_ADMIN` creates new users.

## Goals / Non-Goals

**Goals:**
- Document the dual-model tenancy strategy as the canonical design
- Close the 404 gap by implementing `TenantController` with read-only tenant list and detail endpoints
- Secure the new endpoints to `PLATFORM_ADMIN` only

**Non-Goals:**
- Tenant CRUD (create/update/delete tenants) — out of scope for this change
- Changing how students or `BUILDING_OWNER` users are scoped — the existing models remain as-is
- Frontend changes — `TenantService` already calls the correct endpoints

## Decisions

### Decision 1: Keep dual-model tenancy as the permanent design

**Chosen:** Maintain sentinel `0L` for students/platform admins and Hibernate filter for org users.

**Rationale:** Students are platform-level entities that can apply to any institution. Forcing them into a single tenant would break the core value proposition of the system. The dual model is already implemented in the DB schema and service layer — changing it would be a large, risky migration with no benefit.

**Alternatives considered:**
- _Single-model (all entities tenant-scoped)_: Would require students to be re-scoped per application, adding complexity and preventing multi-institution applications.
- _Row-level security in PostgreSQL_: More powerful but requires DB-level changes and complicates local dev.

### Decision 2: PLATFORM_ADMIN is always cross-tenant

**Chosen:** `PLATFORM_ADMIN` always operates with sentinel `0L` — no single-tenant impersonation mode.

**Rationale:** Platform admins manage the platform itself, not a specific institution. Adding impersonation adds complexity (context-switching UI, audit trail, accidental data leaks) with low benefit since admins can already filter by tenant in UI.

**Alternatives considered:**
- _Impersonation mode_: Admin temporarily assumes a tenant ID. Rejected due to complexity and security surface area.

### Decision 3: TenantController is read-only, PLATFORM_ADMIN only

**Chosen:** Expose `GET /api/admin/tenants` (list) and `GET /api/admin/tenants/{id}` (detail). No write endpoints in this change.

**Rationale:** The only immediate consumer is the tenant-selector dropdown in Create User dialog, which only needs to read tenants. Write operations (create/update/delete tenant) are a separate concern with more complex business rules.

**Security:** Endpoints secured to `ROLE_PLATFORM_ADMIN` via Spring Security. The `TenantFilterConfig` interceptor will NOT activate the Hibernate tenant filter for platform admin requests (sentinel `0L`), so the list query will correctly return all tenants.

## Risks / Trade-offs

- **[Risk] TenantController bypasses tenant filter** → By design. The filter is not activated for `0L` sentinel, so `tenantRepository.findAll()` returns all tenants. This is correct behavior for a platform admin but must not be exposed to non-platform-admin roles. Mitigation: strict `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` on all endpoints.

- **[Trade-off] No pagination on tenant list** → Acceptable for now. Institutions are unlikely to number in the thousands. Add pagination if needed in a future change.

- **[Risk] Existing `frontend-multi-tenancy` spec references tenant selector without an API contract** → This change closes the gap; the delta spec formalizes the API contract.

## Migration Plan

1. Add `TenantController` — no DB changes required
2. Update Spring Security `SecurityConfig` to permit `/api/admin/tenants/**` for `PLATFORM_ADMIN`
3. No Flyway migrations needed
4. No frontend changes needed

Rollback: Remove `TenantController` and revert security config. No data is mutated.

## Open Questions

- None. All decisions confirmed by the user.
