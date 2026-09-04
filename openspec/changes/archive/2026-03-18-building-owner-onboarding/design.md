## Context

The student accommodation system currently supports two actor types: `STUDENT` and `ADMIN`. Buildings and rooms are managed exclusively by admins. As the platform scales to serve multiple institutions, external property providers (building owners) need a self-service path to register and manage their offerings without requiring full admin access.

The existing `building_owner` and `building_staff` entities exist in the schema but have no authentication or portal — they are data-only records. This design activates them as first-class authenticated actors.

## Goals / Non-Goals

**Goals:**
- Self-service registration for building owners with email verification
- Admin review and approve/reject workflow for owner registrations
- Authenticated owner portal scoped strictly to the owner's buildings
- Role-based access: `BUILDING_OWNER` cannot access student data or cross tenant boundaries
- Email notifications at each lifecycle event (submitted, approved, rejected)

**Non-Goals:**
- Building owners creating or deleting rooms (admin responsibility)
- Multi-building-owner organisations with sub-user management (future scope)
- Owner billing or contract management
- SSO / federated identity for owners

## Decisions

### 1. Reuse `app_user` table for owner accounts

**Decision:** Building owners authenticate via the same `app_user` table with a new `ROLE_BUILDING_OWNER` authority.

**Rationale:** Reusing the existing JWT infrastructure, Spring Security filter chain, and `UserDetailsService` avoids duplicating auth logic. A new `owner_profile` table stores the owner-specific fields (organisation, contact phone, verification documents).

**Alternative considered:** Separate `owner_user` table with its own auth chain — rejected because it doubles security surface area and JWT handling complexity.

---

### 2. Two-phase registration: pending → approved

**Decision:** Owner registration creates a record in `owner_registration` with status `PENDING`. Admin approval promotes it to a live `app_user` + `owner_profile`.

**Rationale:** Prevents unvetted owners from accessing the system. Keeps the review queue separate from live user records until explicitly approved.

**Alternative considered:** Immediate account creation with a "restricted" flag — rejected because it complicates permission checks throughout the system.

---

### 3. Owner access scoped by `building.owner_id`

**Decision:** All owner-facing API endpoints filter data by `building.owner_id = currentUser.ownerId`. No owner can see data outside their assigned buildings.

**Rationale:** Simplest enforcement model — the FK is already on the `building` table. Spring Security `@PreAuthorize` expressions can check ownership inline.

**Alternative considered:** Row-level security in PostgreSQL — rejected as over-engineering for the current scale; application-level checks are sufficient and more testable.

---

### 4. Owner portal as a separate Angular feature module

**Decision:** New `features/owner/` module with its own routing, lazy-loaded, guarded by a new `OwnerGuard`.

**Rationale:** Keeps owner UI fully isolated from admin and student modules. Follows the existing module-per-actor pattern (`features/admin/`, `features/dashboard/`).

---

### 5. Flyway migrations for schema changes

**Decision:** Two new migrations:
- `owner_registration` table (pending applications)
- `owner_profile` table (approved owner data linked to `app_user`)
- FK on `building.owner_id` references `owner_profile`

**Rationale:** Consistent with the project's migration-first schema management. No manual DB edits.

## Risks / Trade-offs

| Risk | Mitigation |
|------|-----------|
| Owner registers with email already used as student/admin | Unique constraint on `app_user.email`; registration endpoint returns `409 Conflict` with clear message |
| Admin workload if registrations surge | Add bulk approve action in admin queue; consider auto-approval for verified institutional domains in future |
| Owner gains access to building data before admin approves | `PENDING` records have no `app_user` row; no JWT can be issued until approval promotes the record |
| Email verification link expiry | Reuse existing `EmailVerificationToken` mechanism with 24-hour expiry |

## Migration Plan

1. Deploy Flyway migrations (additive — no existing tables altered destructively)
2. Deploy backend with new endpoints and `BUILDING_OWNER` role (backwards compatible; existing roles unaffected)
3. Deploy frontend with new owner registration page and admin review queue
4. Seed existing `building_owner` records into `owner_profile` if any exist (one-time data migration script)

**Rollback:** Migrations are additive. Rolling back the app version leaves the new tables empty and harmless. A compensating migration can drop the new tables if needed.

## Open Questions

- Should building owners be able to upload a logo/profile image during registration? *(Assume no for MVP)*
- Is email domain allow-listing needed (e.g., only `@university.ac.uk`)? *(Assume no — admin review is the gate)*
- Should rejected registrations be re-submittable or require a new registration? *(Assume re-submittable after admin feedback)*
