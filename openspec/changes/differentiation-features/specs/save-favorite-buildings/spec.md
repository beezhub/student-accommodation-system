## ADDED Requirements

### Requirement: Anonymous user can favorite buildings via localStorage
The system SHALL allow unauthenticated users to favorite buildings from the browse and detail pages. Favorites MUST be stored in localStorage under the key `beezhub_favorites` as a JSON array of building IDs.

#### Scenario: Anonymous user favorites a building from browse card
- **WHEN** an unauthenticated user clicks the favorite icon on a building browse card
- **THEN** the building ID is added to the `beezhub_favorites` localStorage array and the icon toggles to a filled state

#### Scenario: Anonymous user unfavorites a building
- **WHEN** an unauthenticated user clicks the filled favorite icon on a previously favorited building
- **THEN** the building ID is removed from the `beezhub_favorites` localStorage array and the icon toggles to an outline state

#### Scenario: Favorite state persists across page loads for anonymous user
- **WHEN** an unauthenticated user returns to the browse page after favoriting buildings
- **THEN** the favorite icons for previously favorited buildings SHALL display in their filled state

### Requirement: Authenticated user can favorite buildings via backend
The system SHALL provide `POST /api/favorites/{buildingId}` and `DELETE /api/favorites/{buildingId}` endpoints for authenticated students to add and remove building favorites. Favorites MUST be persisted in the `building_favorite` table.

#### Scenario: Authenticated student favorites a building
- **WHEN** an authenticated student calls `POST /api/favorites/42`
- **THEN** the system creates a `building_favorite` record and returns `201 Created`

#### Scenario: Authenticated student unfavorites a building
- **WHEN** an authenticated student calls `DELETE /api/favorites/42`
- **THEN** the system deletes the `building_favorite` record and returns `204 No Content`

#### Scenario: Duplicate favorite is idempotent
- **WHEN** an authenticated student calls `POST /api/favorites/42` for a building already favorited
- **THEN** the system returns `200 OK` without creating a duplicate record

#### Scenario: Unfavoriting a non-favorited building
- **WHEN** an authenticated student calls `DELETE /api/favorites/99` for a building not in their favorites
- **THEN** the system returns `204 No Content` (idempotent)

### Requirement: Favorites sync on login
The system SHALL provide a `POST /api/favorites/sync` endpoint that accepts a JSON array of building IDs and merges them with the student's existing server-side favorites. This endpoint MUST be called after login or signup if localStorage favorites exist.

#### Scenario: Sync merges anonymous favorites with server favorites
- **WHEN** a student logs in with `beezhub_favorites` containing `[1, 2, 3]` and already has building 2 favorited server-side
- **THEN** the sync endpoint creates records for buildings 1 and 3, keeps building 2, and returns the merged list

#### Scenario: localStorage is cleared after successful sync
- **WHEN** the sync endpoint returns successfully
- **THEN** the frontend SHALL remove the `beezhub_favorites` key from localStorage

### Requirement: Favorites list page
The system SHALL provide a `/favorites` route accessible to authenticated students that displays all their favorited buildings in a card layout matching the browse page style.

#### Scenario: Student views favorites page with saved buildings
- **WHEN** an authenticated student navigates to `/favorites`
- **THEN** the page displays their favorited buildings as browse-style cards with unfavorite action available

#### Scenario: Student views favorites page with no saved buildings
- **WHEN** an authenticated student navigates to `/favorites` with no favorites
- **THEN** the page displays an empty state: "No saved buildings yet. Browse buildings to save your favorites."

#### Scenario: Unauthenticated user redirected from favorites page
- **WHEN** an unauthenticated user navigates to `/favorites`
- **THEN** they are redirected to the login page with a returnUrl of `/favorites`

### Requirement: Get all favorites endpoint
The system SHALL provide a `GET /api/favorites` endpoint that returns all building IDs favorited by the authenticated student.

#### Scenario: Student retrieves their favorites
- **WHEN** an authenticated student calls `GET /api/favorites`
- **THEN** the system returns `200 OK` with an array of building summary objects (id, name, address, primaryImage, minMonthlyRate)

### Requirement: Favorites feed into application building preferences
When a student starts an application and has existing favorites, the application building preference step SHALL pre-populate the preference list with their favorited buildings (up to the maximum allowed preferences).

#### Scenario: Student with 3 favorites starts application
- **WHEN** a student with 3 favorited buildings reaches the building preference step in the application wizard
- **THEN** the preference list SHALL be pre-populated with their 3 favorited buildings, which the student can reorder or remove
