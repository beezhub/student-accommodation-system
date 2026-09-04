## ADDED Requirements

### Requirement: Building browse page has map/list toggle
The building browse page SHALL provide a toggle control that switches between list view (existing card layout) and map view. The default view MUST be list. The selected view MUST be preserved in the URL query parameter `view=map|list`.

#### Scenario: User switches to map view
- **WHEN** a user clicks the map toggle on the building browse page
- **THEN** the card list is replaced by a full-width interactive Google Map with building markers and the URL updates to include `view=map`

#### Scenario: User switches back to list view
- **WHEN** a user clicks the list toggle while in map view
- **THEN** the map is replaced by the card list and the URL updates to `view=list`

#### Scenario: Map view loads from URL
- **WHEN** a user navigates to `/buildings?view=map`
- **THEN** the page loads directly in map view

### Requirement: Map displays building markers
The map view SHALL display a marker for every building that has valid latitude and longitude coordinates. Buildings without coordinates MUST be excluded from the map but remain visible in list view.

#### Scenario: Building with coordinates appears on map
- **WHEN** the map view loads and a building has latitude 34.05 and longitude -118.25
- **THEN** a marker appears at those coordinates on the map

#### Scenario: Building without coordinates excluded from map
- **WHEN** the map view loads and a building has null latitude or longitude
- **THEN** no marker appears for that building, but it is visible when switching to list view

### Requirement: Map markers show building info on click
When a user clicks a map marker, an info window SHALL display a mini building card with: building name, primary image thumbnail, price range (if available), and a "View Details" link to the building detail page.

#### Scenario: User clicks a marker
- **WHEN** a user clicks a building marker on the map
- **THEN** an info window opens showing the building name, thumbnail, price range, and a "View Details" link

#### Scenario: Clicking a different marker closes the previous info window
- **WHEN** a user clicks a second marker while an info window is open
- **THEN** the first info window closes and the second opens

### Requirement: Map markers cluster at zoom levels
When many buildings are near each other, the map SHALL group markers into numbered clusters at lower zoom levels. Clicking a cluster SHALL zoom in to reveal individual markers.

#### Scenario: Nearby buildings cluster
- **WHEN** 10 buildings are within a small geographic area and the map is zoomed out
- **THEN** they display as a single cluster marker showing "10"

#### Scenario: Clicking cluster zooms in
- **WHEN** a user clicks a cluster marker
- **THEN** the map zooms in until individual building markers are visible

### Requirement: Distance-to-campus displayed on building cards and markers
When the student's institution has known coordinates, each building card and map info window SHALL display the distance to campus in kilometers (rounded to 1 decimal place).

#### Scenario: Student with known institution sees distance
- **WHEN** an authenticated student whose institution has coordinates views the browse page
- **THEN** each building card and map marker info window displays "N.N km from campus"

#### Scenario: Distance unavailable for anonymous user
- **WHEN** an unauthenticated user views the browse page
- **THEN** no distance information is displayed (no institution context)

#### Scenario: Distance unavailable when institution lacks coordinates
- **WHEN** an authenticated student's institution has no latitude/longitude
- **THEN** no distance information is displayed

### Requirement: Map respects active filters
The map view SHALL respect the same filters as the list view (price range, availability, institution). Markers SHALL only appear for buildings matching the current filter criteria.

#### Scenario: Price filter applied in map view
- **WHEN** a user sets a price filter of R2000–R4000 in map view
- **THEN** only markers for buildings within that price range are visible on the map
