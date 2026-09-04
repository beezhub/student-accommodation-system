## MODIFIED Requirements

### Requirement: Frontend public building browse page exists
The Angular application SHALL provide a public route at `/buildings` that displays a searchable list of buildings with their primary image and key details, accessible without login. The page SHALL include a map/list toggle that switches between card layout and interactive Google Map view. Each building card SHALL include a favorite toggle icon (heart). The default view MUST be list.

#### Scenario: Unauthenticated visitor navigates to /buildings
- **WHEN** a user navigates to `/buildings` without being logged in
- **THEN** the building browse page loads successfully without redirecting to login

#### Scenario: Buildings are displayed with name, address, and primary image
- **WHEN** the building browse page loads
- **THEN** each building card displays the building name, address, and primary image (or a placeholder if no image)

#### Scenario: Visitor searches for a building by name
- **WHEN** the user types a building name into the search field on the browse page
- **THEN** the list filters to show only matching buildings

#### Scenario: Map/list toggle switches view
- **WHEN** a user clicks the map toggle icon
- **THEN** the card list is replaced by an interactive Google Map with building markers and the URL includes `view=map`

#### Scenario: Favorite icon on browse cards
- **WHEN** a building card is rendered
- **THEN** it includes a favorite toggle icon (heart outline for not favorited, filled heart for favorited)

### Requirement: Frontend public building detail page exists
The Angular application SHALL provide a public route at `/buildings/:id` that displays building details and an image gallery, accessible without login. The detail page SHALL include a favorite toggle button and a "Notify me when available" button when the building is at capacity (authenticated users only).

#### Scenario: Visitor opens a building detail page
- **WHEN** a user navigates to `/buildings/42` without being logged in
- **THEN** the building detail page loads displaying the building name, description, address, and all images in a gallery

#### Scenario: Building with no images shows a placeholder
- **WHEN** a user opens the detail page for a building that has no images
- **THEN** the gallery section displays a placeholder image or "No images available" message

#### Scenario: Image gallery is navigable
- **WHEN** a building detail page has multiple images
- **THEN** the user can navigate between images (next/previous controls or thumbnail strip)

#### Scenario: Favorite button on detail page
- **WHEN** a user views a building detail page
- **THEN** the page header includes a favorite toggle button

#### Scenario: Notify me button on full building
- **WHEN** an authenticated student views a building detail page with 0 available rooms
- **THEN** a "Notify me when available" button is displayed
