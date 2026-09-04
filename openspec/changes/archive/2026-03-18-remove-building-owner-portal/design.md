## Context

The student accommodation system was designed with a multi-tenant architecture where each tenant (university/institution) is administered by a `PLATFORM_ADMIN` user. A building owner portal was built on top of this as a separate role, allowing external property owners to register, get approved, and view their buildings. Because the multi-tenancy model already gives admins complete control over buildings and rooms per tenant, the building owner layer is redundant and adds surface area without delivering unique value.

The removal touches every layer of the stack: 4 database tables/columns, 3 backend controllers, 2 services, 2 entities, 4 email templates, a full frontend feature directory, a registration flow, an admin panel sub-feature, a security role, and related tests. The changes are purely deletions — no new behaviour is introduced.

## Goals / Non-Goals

**Goals:**
- Completely remove the `BUILDING_OWNER` role and all code that depends on it
- Remove the 3 owner-related database tables (`building_owner`, `owner_registration`, `building_staff`) and the `building.owner_id` FK column via a clean Flyway migration
- Leave the rest of the system (multi-tenancy, admin panel, student portal, allocation) fully functional
- Ensure no dead code, orphan imports, or broken routes remain after removal

**Non-Goals:**
- Replacing building owner functionality with any alternative feature
- Migrating existing `BUILDING_OWNER` users to another role (they are treated as invalid accounts post-removal)
- Changing any other role, permission, or tenant behavior

## Decisions

### Decision 1: Single Flyway migration to drop all owner tables
Drop all owner-related database objects in one migration rather than multiple. The tables are tightly related (foreign keys between `building_owner`, `owner_registration`, `building_staff`, and `building.owner_id`), so a single migration is safer and easier to reason about.

- **Alternative**: separate migrations per table. Rejected because ordering dependencies between migrations adds risk.
- **Drop order**: `building_staff` → `building.owner_id` column → `building_owner` → `owner_registration` (respects FK constraints).

### Decision 2: Remove `BuildingStaff` together with `BuildingOwner`
`BuildingStaff` has no purpose without `BuildingOwner` (its `owner_id` FK is mandatory). Keeping a table with an FK to a deleted table would break the schema. Both are removed in the same migration.

### Decision 3: No data migration for existing BUILDING_OWNER users
Any `app_user` rows with `user_role = 'BUILDING_OWNER'` or `'LANDLORD'` will be orphaned after the `building_owner` profile table is dropped. These records should be deleted by the migration. Leaving orphaned users with an unrecognised role would cause `ClassCastException`-style failures at login.

The migration will: `DELETE FROM app_user WHERE user_role IN ('BUILDING_OWNER', 'LANDLORD')` before dropping tables.

- **Alternative**: leave orphaned users. Rejected — the auth system maps roles to enums and would throw on an unknown role string.

### Decision 4: Remove `LANDLORD` deprecated enum value simultaneously
The `LANDLORD` role was already deprecated in favour of `BUILDING_OWNER` and has no active users after the V2026_02_25 migration. Keeping the dead enum value in `UserRole.java` after removing the feature it references serves no purpose.

### Decision 5: Delete specs rather than archive them inline
The four spec files in `openspec/specs/` will each receive a delta spec (REMOVED Requirements) so the openspec workflow records the intent. The original spec files remain as historical record; only the delta is added to the change.

## Risks / Trade-offs

- **Risk: Orphaned BUILDING_OWNER users exist in a live database** → Mitigation: the migration explicitly deletes these rows before dropping the FK-constrained tables. Include this step in any deployment runbook.
- **Risk: Dead imports in `SecurityConfig` or `AppUserDetailsService` after deletion** → Mitigation: compile step after deletion will catch missing imports; CI build must pass before merge.
- **Risk: Frontend lazy routes pointing to deleted components cause runtime errors** → Mitigation: delete route declarations before removing components; run `ng build` to verify no broken references.
- **Risk: `TenantFilterIntegrationTest` references `BuildingOwner` entity** → Mitigation: remove the building-owner-specific test cases; the tenant filter itself is still tested via other entities.

## Migration Plan

1. Delete all backend Java source files (entities, repos, services, controllers, DTOs, mapper, email contexts)
2. Update `UserRole` enum, `SecurityConfig`, `AppUserDetailsService`, `Building` entity
3. Delete 4 email templates and clean up `EmailDispatchService` / `EmailSubjects`
4. Delete all frontend feature directories and guard
5. Update `role.model.ts`, `role.service.ts`, routing, sidebar, login, home
6. Add Flyway migration (drop owner tables, delete orphaned users)
7. Build backend (`./gradlew build`) and fix any remaining compile errors
8. Build frontend (`npm run build`) and fix any remaining compile errors
9. Run backend tests (`./gradlew test`) and frontend tests (`npm test`)

**Rollback:** Because this is a database destructive change (DROP TABLE, DELETE rows), a rollback requires restoring from a database snapshot taken before the migration ran. There is no automated rollback. The migration should be deployed during a maintenance window with a verified backup.

## Open Questions

- Are there any live `BUILDING_OWNER` users in production that require a transition/offboarding communication before this is deployed?
- Should the `building.owner_id` column removal be a soft removal first (nulling all values, then dropping in a later migration) or is a direct DROP acceptable given the column is nullable and all values are expected to be NULL in production?
