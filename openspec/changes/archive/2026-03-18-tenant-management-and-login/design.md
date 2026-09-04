## Context

The system already has a `Tenant` JPA entity, `TenantService` (read-only), `TenantController` (GET only), and a frontend `TenantService` that calls the read endpoints. The multi-tenancy architecture uses a sentinel tenant (`id=0`, name="PLATFORM") for platform-level users. Institution tenants have `id > 0`.

The current gap: there is no way to create a new tenant or manage existing ones. Platform Admins cannot onboard new institutions. Tenant-scoped admin users (`ADMIN` role with `tenant_id > 0`) cannot be created through the UI because the create-user dialog does not enforce a tenant selection for the `ADMIN` role.

**Constraints:**
- Only `PLATFORM_ADMIN` users (tenantId=0) may call tenant write endpoints.
- The platform tenant (id=0) must never be deleted or deactivated through the API.
- Flyway manages all schema changes — no manual DB edits.
- Angular 21, Angular Material, existing component patterns (form dialogs, management tables).

## Goals / Non-Goals

**Goals:**
- Add `POST /api/admin/tenants`, `PUT /api/admin/tenants/{id}`, `PATCH /api/admin/tenants/{id}/status` (toggle active) backend endpoints
- Add Angular tenant management page at `/admin/tenants` with list table and create/edit dialog
- Add "Tenants" entry to admin sidebar, visible only to Platform Admins
- Extend the create-user dialog so that creating an `ADMIN` user also requires a tenant selection (not `tenantId=0`)

**Non-Goals:**
- Tenant deletion (deactivation covers the use case; deletion risks FK cascade complexity)
- Subdomain-based tenant routing
- Tenant-specific configuration UI (the `config` JSONB field exists but editing it is out of scope)
- Self-service tenant registration

## Decisions

### Decision 1 — Deactivate, don't delete
Tenants may have associated users, buildings, and applications. Hard-deleting a tenant would require cascading deletes across many FK relationships. Instead the API exposes a PATCH toggle (`isActive`). Deleted state can be revisited in a future change.

**Alternatives considered:** Soft-delete with `deleted_at` timestamp — rejected because the existing schema has no soft-delete pattern anywhere (CLAUDE.md: "Soft deletes are not used").

### Decision 2 — DTO layer for write operations
The current `GET` endpoints return the raw `Tenant` entity. Write operations will use `TenantRequestDto` (name, domain) and return `TenantResponseDto` (id, name, domain, isActive, createdAt, updatedAt) to control what is exposed and allow validation annotations.

### Decision 3 — Sidebar visibility via role check
The "Tenants" sidebar item will only be shown when the logged-in user has `PLATFORM_ADMIN` role. The existing `AuthService.hasRole()` pattern (used elsewhere in the sidebar) will be reused.

### Decision 4 — ADMIN role also requires tenant on create
The existing spec (`frontend-multi-tenancy`) only enforced tenant selection for `BUILDING_OWNER`. We extend the same rule to `ADMIN` role, because a tenant-scoped admin without a tenant assignment would be invalid.

## Risks / Trade-offs

- [Risk] Platform tenant (id=0) protection: a Platform Admin could accidentally deactivate or edit the platform tenant → Mitigation: backend validates `id != 0` on PUT/PATCH, returns `400 Bad Request`.
- [Risk] `name` uniqueness constraint violation on create → Mitigation: backend returns `409 Conflict`; frontend shows snackbar error.
- [Risk] Existing `GET` endpoints return raw `Tenant` entity (including `config` JSONB) → No change to existing responses; only new write endpoints use DTOs. Acceptable for now.

## Migration Plan

1. Add Flyway migration (if any schema changes are needed — currently `Tenant` table schema is complete; no migration required for this change).
2. Deploy backend with new endpoints — fully additive, no breaking changes.
3. Deploy frontend with new tenant management page and sidebar update.
4. Rollback: remove frontend routes and sidebar entry; new backend endpoints are additive so rollback is not required server-side.

## Open Questions

- Should the Platform Admin dashboard show a tenant count widget? Deferred — not in scope for this change.
- Should `domain` be required on tenant create, or optional? Decision: optional (the field is nullable in the DB schema).
