## MODIFIED Requirements

### Requirement: Public routes require no authentication guards
The Angular routing configuration SHALL include `/buildings` and `/buildings/:id` routes with no `AuthGuard`, `StudentGuard`, or `EmailVerifiedGuard` applied, consistent with existing public routes such as `/home`, `/login`, and `/signup`.

#### Scenario: Unauthenticated user navigates to /buildings
- **WHEN** a user who is not logged in navigates to `/buildings`
- **THEN** the Angular router loads the `BuildingBrowseComponent` without redirecting to `/login`

#### Scenario: Unauthenticated user navigates to /buildings/:id
- **WHEN** a user who is not logged in navigates to `/buildings/42`
- **THEN** the Angular router loads the `BuildingDetailComponent` without redirecting to `/login`

#### Scenario: Authenticated user can also access public building routes
- **WHEN** a logged-in student navigates to `/buildings`
- **THEN** the page loads normally (guards do not block authenticated users)
