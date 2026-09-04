## Context

`DocumentType` is currently a flat, globally-shared table — `typeName` is unique system-wide, and `isRequired` is a single global flag. Every tenant sees the same document types with the same required status, which prevents tenant admins from configuring which documents their students need to submit.

The system already has a well-established cascade/inheritance model for per-tenant configuration: `system_setting` uses a composite PK `(tenant_id, setting_key)` where `tenant_id = 0` holds platform defaults and tenant rows override them. The multi-tenancy infrastructure (Hibernate `tenantFilter`, `TenantContext`, `TenantListener`) is already in place and applies to any entity that carries a `tenant_id` column.

## Goals / Non-Goals

**Goals:**
- Tenant admins can define their own required document types independently of other tenants.
- Platform admins continue to manage platform-level document types (`tenant_id = 0`) that serve as inherited defaults.
- Tenant admins can add tenant-specific types and shadow (override) platform types by creating a type with the same name at their tenant scope.
- The application submission document completeness check resolves required types from the application's target tenant (derived from the selected buildings), passed explicitly — not from `TenantContext`.
- The completeness check uses name-based matching (`type_name`) rather than ID-based matching — a student's uploaded document (linked to a platform type ID) must satisfy a shadow requirement (different ID, same name).
- The merged view queries bypass the Hibernate filter and use explicit `WHERE tenant_id IN (0, targetTenantId)` predicates. **The Hibernate filter is NOT relied on for document type lookups** — student requests run with `TenantContext = 0` and `TenantFilterAspect` explicitly skips the filter for `tenant_id = 0`.
- `document.tenant_id` remains `0` (student-owned). Documents are the student's property; a separate change will address the `@Filter` on `Document` that currently causes a latent admin visibility bug.

**Non-Goals:**
- Per-student or per-application document type overrides (not in scope).
- Retroactively re-checking already-submitted applications when a tenant changes their document type config.
- Document type versioning or change history.

## Decisions

### 1. Add `tenant_id` to `document_type`, following the same dual-column pattern as other tenant-aware entities

Each `document_type` row is owned by a tenant. Platform defaults live at `tenant_id = 0`. The uniqueness constraint changes from globally unique `type_name` to `(tenant_id, type_name)`.

**Why over a separate override/config table:** The cascade model (same pattern as `system_setting`) keeps queries simple. The merged view uses a single `WHERE tenant_id IN (0, targetTenantId)` query with in-memory deduplication by `type_name` — straightforward and indexed. Note: the Hibernate filter is **not** used for these queries because student requests deliberately skip the filter (`TenantContext = 0`).

**Why over no-`tenant_id` + pivot table:** Avoids a new relationship to reason about, avoids changes to query signatures, and stays consistent with how other entities are scoped.

### 2. Merged view uses explicit `IN (0, targetTenantId)` — not the Hibernate filter

When `DocumentTypeService` resolves document types for a tenant, it queries `WHERE tenant_id IN (0, targetTenantId)` directly and deduplicates in-memory by `type_name` (tenant row wins). This bypasses the Hibernate `tenantFilter` entirely, which is necessary because:
- Student requests have `TenantContext = 0` → `TenantFilterAspect` skips the filter
- The submission check needs the **application's** target tenant ID, not the student's TenantContext

The semantics are the same as `system_setting.getAllSettings()` but the query is explicit rather than filter-driven.

**Why shadowing over hard-copy-on-provision:** Tenants don't need to maintain a full copy of platform types. Platform updates (new platform types, changed descriptions) propagate automatically unless the tenant has explicitly shadowed them.

### 3. Tenant admins cannot edit or delete platform types (`tenant_id = 0`)

The service enforces that a `TENANT_ADMIN` may only mutate rows where `tenantId == TenantContext.getTenantId()`. `PLATFORM_ADMIN` continues to manage `tenant_id = 0` rows via `/api/platform/document-types`.

**Why:** Prevents accidental cross-tenant pollution. Consistent with how `system_setting` splits platform and tenant admin API routes.

### 4. Flyway migration assigns all existing rows to `tenant_id = 0`

Existing document types become platform defaults. No tenant data is lost, and all tenants automatically inherit the existing catalog until they create overrides.

## Risks / Trade-offs

- **Shadow semantics add a mental model cost** → Mitigated by clear UI labels ("Platform default" vs "Custom") and documenting the behaviour.
- **Merged view query bypasses the Hibernate filter** → This is intentional and correct. All document type service methods must use explicit `IN (0, tenantId)` queries; any future developer adding a filter-reliant query would silently get wrong results for student contexts.
- **Name-based matching is looser than ID-based** → A tenant creating a shadow type with a slightly different name (e.g. "National Id" vs "National ID") would not shadow correctly. Mitigated by case-insensitive name normalisation on create and a UI warning when a shadow name closely matches a platform name.
- **Student needs to know target tenant's required types before uploading** → Surfaced on the application form's per-tenant requirements panel (once buildings are selected). The standalone documents page remains platform-types-only for the general portfolio.
- **Existing integration tests** seed document types without `tenant_id`; they will need updating to set `tenant_id = 0` (platform) or the current tenant ID.

## Migration Plan

1. Flyway migration: add `tenant_id BIGINT NOT NULL DEFAULT 0` to `document_type`, add FK to `tenant(id)`, drop the old `UNIQUE(type_name)` constraint, add `UNIQUE(tenant_id, type_name)`.
2. Deploy backend (Flyway runs migration automatically).
3. No data backfill needed — existing rows get `tenant_id = 0` via the column default.
4. Rollback: reverse migration removes the column and restores the old unique constraint.

## Open Questions

- Should tenants be allowed to **delete** platform types from their effective view (i.e., create a "disabled" shadow row) or only override `isRequired`? Currently designed as override-only; disabling is out of scope.
- Should the student-facing `GET /api/document-type/required` endpoint accept `?tenantId=` as a query param, or be a separate route? **Decision: accept `?tenantId=` param** — the application form passes `applicationTenantId` (known from building selection); calls without the param return platform defaults only.

## Related Work (Separate Change Required)

`Document` entity has `@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")` and student documents are stamped `tenant_id = 0` by `TenantListener`. Admin (e.g. `tenant_id = 42`) queries through `findAllByStudent_AppUser_Id` have the Hibernate filter active — returning zero results because student documents are `tenant_id = 0`. This is a latent bug: document access by ownership (`student_id`) should not be filtered by tenant. The fix (remove `@Filter` from `Document`, scope via `student_id` only) should be a separate change to avoid conflating concerns.
