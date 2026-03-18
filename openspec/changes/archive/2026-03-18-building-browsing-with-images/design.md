## Context

The system already has `Building`, `BuildingImage` entities, `BuildingService`, `BuildingImageService`, and `BuildingController` with full CRUD and image management endpoints. However, all building-read endpoints currently require authentication (`anyRequest().authenticated()`), preventing unauthenticated students from browsing buildings. The application form (`NewApplicationComponent`) already loads buildings and uses a single `preferredBuildingId` field.

**Current state:**
- `GET /api/building`, `/api/building/{id}`, `/api/building/{buildingId}/images` all require a valid JWT
- `BuildingImageService` enforces no hard cap on image count per building
- `NewApplicationComponent` form has a single `preferredBuildingId` — not multi-select
- No public-facing `/buildings` route exists in the Angular router
- `application` table stores one `preferred_building_id` (FK to `building`)

## Goals / Non-Goals

**Goals:**
- Make building list and detail endpoints (including images) publicly accessible without JWT
- Enforce a maximum of 10 images per building on upload
- Add a public Angular route `/buildings` and `/buildings/:id` requiring no auth guards
- Update the application form to support up to 3 preferred buildings (ordered preference)
- Update `SecurityConfig` to permit the relevant `GET /api/building/**` paths publicly

**Non-Goals:**
- Redesigning the admin building management UI beyond adding image upload enforcement feedback
- Changing MinIO storage structure or presigned URL generation
- Adding image reordering drag-and-drop (display order remains insertion-order)
- Room-level browsing (buildings only)
- Internationalisation or multi-language support

## Decisions

### 1. Make building GET endpoints public via SecurityConfig (not a separate public API)

**Decision:** Permit `GET /api/building/**` and `GET /api/building/{id}/images` in `SecurityConfig.permitAll()`.

**Rationale:** These endpoints already exist and are functionally complete; creating a duplicate `/api/public/building/**` namespace would duplicate code with no benefit. Permit-listing only GET methods preserves admin-only write endpoints.

**Alternative considered:** Separate `/api/public/**` prefix — rejected because it would require duplicating controllers and service calls.

### 2. Enforce 10-image cap in `BuildingImageService.uploadImages()`

**Decision:** Before uploading, count existing images for the building. Reject the entire batch if `existing + incoming > 10`, throwing a `400 Bad Request` with a clear message.

**Rationale:** Cap logic belongs in the service layer, not the controller, keeping the controller thin. Rejecting the whole batch (rather than partial upload) avoids partial-state confusion for admins.

**Alternative considered:** Silent truncation (upload only up to the remaining slots) — rejected because silent data loss is worse UX than an explicit error.

### 3. Store up to 3 preferred buildings as an ordered list in the application

**Decision:** Add a new `application_building_preference` join table with columns `(application_id, building_id, preference_order)` via a Flyway migration. The `Application` entity gains a `@OneToMany` collection of `ApplicationBuildingPreference` entities. The existing `preferred_building_id` column is deprecated (kept for backward compatibility but no longer written to by new code).

**Rationale:** A proper join table with `preference_order` is normalised, queryable, and extensible. It avoids array/JSON columns which complicate JPA mapping.

**Alternative considered:** JSON array column on `application` — rejected due to poor queryability and type-safety.

### 4. Angular public building browse as new standalone components with no guards

**Decision:** Create `BuildingBrowseComponent` (list) and `BuildingDetailComponent` (detail/gallery) as standalone Angular components under `features/buildings/`. Add routes `buildings` and `buildings/:id` with no guards. Reuse the `BuildingService` already present in `core/services/`.

**Rationale:** No new service layer needed; existing `BuildingService` methods (`searchBuildings`, `getBuildingById`, `getImages`) suffice once the backend endpoints are public. Standalone components keep the bundle lazy-loaded.

### 5. Application form multi-building preference: ordered chip/list selector (max 3)

**Decision:** Replace the existing `preferredBuildingId` single select in `NewApplicationComponent` with a multi-select component that maintains insertion order, enforces max 3, and shows the selection as an ordered list. The form field becomes `preferredBuildingIds: string[]`.

**Rationale:** Order matters (1st, 2nd, 3rd preference) for the allocation scoring algorithm. A chip-based ordered list communicates order visually.

## Risks / Trade-offs

- **Public endpoints and tenant isolation:** Building endpoints use `@Filter(tenantFilter)` at the JPA level. Unauthenticated requests won't have a tenant context set by the `JwtAuthenticationFilter`. The `TenantFilter` or `TenantContext` must be reviewed to ensure it doesn't throw when no JWT is present — a tenant identifier (e.g., subdomain or `X-Tenant-ID` header) must still be resolvable for public routes, or the filter must be made optional.
  → **Mitigation:** Make tenant resolution fault-tolerant for unauthenticated requests; fall back to a default or resolve via subdomain. This must be verified before going live.

- **Presigned URL expiry on public pages:** `BuildingImageService` generates short-lived MinIO presigned URLs. Public users browsing buildings will see expired images if they leave a tab open.
  → **Mitigation:** Acceptable for now; URLs expire after the MinIO default (7 days by default). If needed, a proxy endpoint can be added later.

- **Backward compatibility of `preferred_building_id`:** Existing applications in the DB use the old column. Allocation logic that reads `preferred_building_id` must be updated to also read from the new preference table, or a view/migration must back-fill data.
  → **Mitigation:** Keep old column; new applications write only to the new table; allocation service updated to prefer new table, falling back to old column.

- **Breaking change in application payload:** Frontend sends `preferredBuildingIds` (array); backend `ApplicationRequest` DTO must be updated. Old mobile clients (if any) sending `preferredBuildingId` will silently lose their preference.
  → **Mitigation:** Accept both fields in the DTO during a transition period; deprecate `preferredBuildingId`.

## Migration Plan

1. Write Flyway migration `V{date}__add_application_building_preference.sql` creating `application_building_preference` table
2. Update `SecurityConfig` to `permitAll()` on `GET /api/building/**`
3. Update `BuildingImageService` to enforce 10-image cap
4. Update `Application` entity, `ApplicationRequest` DTO, `ApplicationService`, and `AllocationScoringService` to handle multi-building preferences
5. Create Angular `BuildingBrowseComponent` and `BuildingDetailComponent`; add public routes
6. Update `NewApplicationComponent` with multi-building preference selector
7. Test unauthenticated access to building endpoints end-to-end
8. Test image cap enforcement in admin building management

**Rollback:** All changes are additive except the `SecurityConfig` permit change. Reverting that single line restores auth requirements on building endpoints. The DB migration is additive (new table, old column untouched).

## Open Questions

- Does the tenant filter need a fallback strategy for unauthenticated requests, or are buildings globally accessible across all tenants on the public browse page?
- Should the 3 preferred buildings be shown to the student after submission (in application detail view)?
- Is a `displayOrder` update API needed for building images (drag-to-reorder), or is insertion order sufficient for this iteration?
