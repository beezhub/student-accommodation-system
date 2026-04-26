## ADDED Requirements

### Requirement: Public building list is accessible without authentication
The system SHALL allow any user (unauthenticated) to retrieve a list of buildings via `GET /api/building` and `GET /api/building/all` without providing a JWT token.

#### Scenario: Unauthenticated user requests building list
- **WHEN** a request is made to `GET /api/building` with no `Authorization` header
- **THEN** the system returns `200 OK` with a paginated list of buildings

#### Scenario: Unauthenticated user searches buildings by name
- **WHEN** a request is made to `GET /api/building/search?name=Block` with no `Authorization` header
- **THEN** the system returns `200 OK` with buildings whose name matches the query

### Requirement: Public building detail is accessible without authentication
The system SHALL allow any user (unauthenticated) to retrieve a single building by ID via `GET /api/building/{id}` without a JWT token.

#### Scenario: Unauthenticated user requests a valid building
- **WHEN** a request is made to `GET /api/building/42` with no `Authorization` header
- **THEN** the system returns `200 OK` with the building details

#### Scenario: Unauthenticated user requests a non-existent building
- **WHEN** a request is made to `GET /api/building/9999` with no `Authorization` header
- **THEN** the system returns `404 Not Found`

### Requirement: Public building images are accessible without authentication
The system SHALL allow any user (unauthenticated) to retrieve images for a building via `GET /api/building/{buildingId}/images` without a JWT token.

#### Scenario: Unauthenticated user requests images for a building
- **WHEN** a request is made to `GET /api/building/42/images` with no `Authorization` header
- **THEN** the system returns `200 OK` with the list of image records including presigned URLs

### Requirement: Frontend public building browse page exists
The Angular application SHALL provide a public route at `/buildings` that displays a searchable, filterable, paginated list of buildings with their primary image, key details, pricing, and availability, accessible without login.

#### Scenario: Unauthenticated visitor navigates to /buildings
- **WHEN** a user navigates to `/buildings` without being logged in
- **THEN** the building browse page loads successfully without redirecting to login

#### Scenario: Buildings are displayed with name, address, primary image, pricing, and availability
- **WHEN** the building browse page loads
- **THEN** each building card displays: building name, address, primary image (or placeholder), pricing range (or "Price on request"), available room count with colour indicator, and room capacity type chips

#### Scenario: Visitor searches for a building by name
- **WHEN** the user types a building name into the search field on the browse page
- **THEN** the search query is sent to the server and the list updates to show matching buildings

### Requirement: Frontend public building detail page exists
The Angular application SHALL provide a public route at `/buildings/:id` that displays building details, image gallery, rooms and pricing table, and accessibility features, accessible without login.

#### Scenario: Visitor opens a building detail page
- **WHEN** a user navigates to `/buildings/42` without being logged in
- **THEN** the building detail page loads displaying: building name, description, address, all images in a gallery, institution chips, total floors, rooms and pricing table, availability summary, and accessibility features (if present)

#### Scenario: Building with no images shows a placeholder
- **WHEN** a user opens the detail page for a building that has no images
- **THEN** the gallery section displays a placeholder image or "No images available" message

#### Scenario: Image gallery is navigable
- **WHEN** a building detail page has multiple images
- **THEN** the user can navigate between images (next/previous controls or thumbnail strip)
