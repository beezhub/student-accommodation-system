## 1. Backend — Security & Public Access

- [x] 1.1 Update `SecurityConfig.java` to add `GET /api/building`, `GET /api/building/all`, `GET /api/building/search`, `GET /api/building/{id}`, `GET /api/building/by-institution/{institutionId}`, and `GET /api/building/{buildingId}/images` to the `permitAll()` list
- [x] 1.2 Review `TenantContext` / tenant filter resolution to ensure it does not throw when no JWT is present (make tenant resolution fault-tolerant for unauthenticated requests)
- [ ] 1.3 Verify with an integration test or manual test that `GET /api/building` returns `200` without an `Authorization` header

## 2. Backend — Building Image Cap

- [x] 2.1 In `BuildingImageService.uploadImages()`, query the current image count for the building before processing uploads
- [x] 2.2 If `existingCount + incomingCount > 10`, throw a `400 Bad Request` exception with message `"Building cannot have more than 10 images (current: X, requested: Y)"`
- [x] 2.3 Write a unit test for `uploadImages()` covering: upload within limit, upload that reaches exactly 10, upload that exceeds limit

## 3. Backend — Database Migration (Building Preferences)

- [x] 3.1 Create Flyway migration `V{YYYY_MM_DD_HHMM}__add_application_building_preference.sql` creating table `application_building_preference (id, application_id FK, building_id FK, preference_order INT NOT NULL CHECK (preference_order BETWEEN 1 AND 3), UNIQUE(application_id, preference_order))`
- [x] 3.2 Create `ApplicationBuildingPreference` JPA entity with fields: `id`, `application` (ManyToOne), `building` (ManyToOne), `preferenceOrder`
- [x] 3.3 Add `@OneToMany(cascade=ALL, orphanRemoval=true)` collection `buildingPreferences` to the `Application` entity

## 4. Backend — Application API Update

- [x] 4.1 Add `preferredBuildingIds: List<Long>` field to `ApplicationRequest` DTO with a `@Size(max = 3)` validation constraint
- [x] 4.2 Update `ApplicationService.createApplication()` to iterate `preferredBuildingIds`, create `ApplicationBuildingPreference` records with correct `preferenceOrder`, and persist them
- [x] 4.3 Add `preferredBuildings: List<BuildingPreferenceResponse>` to `ApplicationResponse` DTO (each entry: `buildingId`, `buildingName`, `preferenceOrder`)
- [x] 4.4 Update `ApplicationMapper` to map the new `buildingPreferences` collection to/from the DTO
- [x] 4.5 Update `AllocationScoringService` to read from `application.buildingPreferences` (ordered) in addition to / instead of `preferredBuildingId` when scoring building preference match
- [x] 4.6 Write unit tests for `ApplicationService` covering: 1 preference, 3 preferences, 4 preferences (expect 400), empty preferences

## 5. Frontend — Angular Public Building Browse

- [x] 5.1 Create `frontend/src/app/features/buildings/` directory structure with `building-browse/` and `building-detail/` subdirectories
- [x] 5.2 Create `BuildingBrowseComponent` (standalone) displaying a paginated/searchable grid of building cards (name, address, primary image or placeholder)
- [x] 5.3 Create `BuildingDetailComponent` (standalone) displaying building details, description, address, and an image gallery with next/previous navigation
- [x] 5.4 Add public routes in `app-routing.module.ts`: `{ path: 'buildings', loadComponent: () => BuildingBrowseComponent }` and `{ path: 'buildings/:id', loadComponent: () => BuildingDetailComponent }` — no guards
- [x] 5.5 Add a "Browse Buildings" navigation link to `HomeComponent` or the top navigation so unauthenticated users can discover the page

## 6. Frontend — Application Form Multi-Building Preference

- [x] 6.1 Update `NewApplicationComponent` form group: replace `preferredBuildingId` with `preferredBuildingIds: FormArray` (max 3 items, no duplicates)
- [x] 6.2 Implement the building preference selector UI: a dropdown to add a building, an ordered list showing selected buildings (1st, 2nd, 3rd) with a remove button per item
- [x] 6.3 Disable the add-building control when 3 buildings are already selected; show "Maximum of 3 buildings reached"
- [x] 6.4 Prevent duplicate selections: filter the dropdown options to exclude already-selected buildings
- [x] 6.5 Update `onSubmit()` to send `preferredBuildingIds` (array of IDs in order) in the application payload instead of `preferredBuildingId`
- [x] 6.6 Update `ApplicationDetailsComponent` to display the ordered building preferences list (if returned in `ApplicationResponse`)

## 7. Frontend — Admin Image Upload Cap Feedback

- [x] 7.1 Update `BuildingImageDialogComponent` to display current image count (e.g., "6 of 10 images used") fetched on dialog open
- [x] 7.2 Disable the file upload input and upload button when the building already has 10 images; display "Maximum of 10 images reached"
- [x] 7.3 Handle the `400` error response from `uploadImages` API and surface the backend error message to the admin via snackbar

## 8. Testing & Verification

- [x] 8.1 Manually verify unauthenticated access to `/buildings` and `/buildings/:id` Angular routes
- [x] 8.2 Manually verify unauthenticated `GET /api/building` API call returns `200 OK`
- [x] 8.3 Test image upload cap: verify 11th image upload returns a `400` error in the admin UI
- [x] 8.4 Test application form: submit application with 3 building preferences and verify they appear in the application detail view
- [x] 8.5 Run backend tests: `./gradlew test` — ensure no regressions
- [x] 8.6 Run frontend lint: `npm run lint` — ensure no lint errors in new components
