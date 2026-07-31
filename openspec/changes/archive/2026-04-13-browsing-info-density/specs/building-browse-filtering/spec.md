## ADDED Requirements

### Requirement: Building browse supports server-side pagination
The building browse page SHALL use a paginated API endpoint (`GET /api/building/browse`) instead of loading all buildings in a single request. The endpoint SHALL accept `page` (0-indexed), `size` (default 12), and `sort` parameters. The frontend SHALL implement a "Load more" button or infinite scroll to fetch additional pages.

#### Scenario: First page of buildings loads on browse page
- **WHEN** a user navigates to the building browse page
- **THEN** the frontend requests `GET /api/building/browse?page=0&size=12`
- **AND** the response includes `content` (building array), `totalElements`, `totalPages`, `number` (current page)

#### Scenario: User loads more buildings
- **WHEN** a user clicks "Load more" or scrolls to the bottom of the building list
- **THEN** the frontend requests the next page (`page=1`, etc.)
- **AND** the new buildings are appended to the existing list

#### Scenario: All buildings have been loaded
- **WHEN** the current page is the last page (`number >= totalPages - 1`)
- **THEN** the "Load more" button is hidden or disabled

---

### Requirement: Building browse supports filtering by price range
The browse page SHALL include a price range filter. The filter SHALL send `minPrice` and `maxPrice` query parameters to the paginated endpoint. The server SHALL return only buildings that have at least one room with `monthly_rate` within the specified range. Buildings with no priced rooms SHALL be excluded from price-filtered results.

#### Scenario: User filters by minimum price
- **WHEN** a user sets a minimum price filter of R2,000
- **THEN** the API request includes `minPrice=2000`
- **AND** only buildings with at least one room priced at R2,000 or above are returned

#### Scenario: User filters by price range
- **WHEN** a user sets a price range of R1,500 to R3,000
- **THEN** the API request includes `minPrice=1500&maxPrice=3000`
- **AND** only buildings with at least one room priced between R1,500 and R3,000 are returned

#### Scenario: Price filter excludes unpriced buildings
- **WHEN** a user applies any price filter
- **THEN** buildings with no rooms that have `monthly_rate` set SHALL NOT appear in results

---

### Requirement: Building browse supports filtering by availability
The browse page SHALL include an availability toggle filter. When enabled, the filter SHALL send `available=true` to the paginated endpoint. The server SHALL return only buildings that have at least one room with status `AVAILABLE`.

#### Scenario: User filters for available buildings only
- **WHEN** a user enables the "Available only" filter
- **THEN** the API request includes `available=true`
- **AND** only buildings with at least one AVAILABLE room are returned

#### Scenario: Availability filter is off by default
- **WHEN** a user first loads the browse page without applying filters
- **THEN** all buildings are shown regardless of room availability

---

### Requirement: Building browse supports filtering by institution
The browse page SHALL include an institution filter (dropdown or chip list). The filter SHALL send `institutionId` to the paginated endpoint. The server SHALL return only buildings associated with the selected institution.

#### Scenario: User filters by institution
- **WHEN** a user selects "University of Cape Town" from the institution filter
- **THEN** the API request includes `institutionId=5`
- **AND** only buildings associated with that institution are returned

---

### Requirement: Filter state is reflected in the URL
Active filters SHALL be reflected as query parameters in the browser URL, enabling deep-linking to filtered views and browser back/forward navigation through filter changes.

#### Scenario: Applying filters updates the URL
- **WHEN** a user applies a price filter of R1,500-R3,000 and selects institution ID 5
- **THEN** the browser URL updates to include `?minPrice=1500&maxPrice=3000&institutionId=5`

#### Scenario: Loading a URL with filter parameters applies filters
- **WHEN** a user navigates to `/buildings?minPrice=2000&available=true`
- **THEN** the browse page loads with the price filter set to R2,000 minimum and the availability toggle enabled
- **AND** the building list shows filtered results matching those parameters

---

### Requirement: Server-side building browse endpoint uses JPA Specification
The `GET /api/building/browse` endpoint SHALL use Spring Data JPA `Specification` to compose filter predicates. The endpoint SHALL accept: `page`, `size`, `sort`, `minPrice`, `maxPrice`, `available`, `institutionId`. All filter parameters are optional — when omitted, no filtering is applied for that dimension.

#### Scenario: Multiple filters combine with AND logic
- **WHEN** a request includes `minPrice=2000&available=true&institutionId=3`
- **THEN** the server returns buildings that satisfy ALL conditions: have a room priced >= 2000 AND have an available room AND are associated with institution 3

#### Scenario: No filters returns all buildings paginated
- **WHEN** a request includes only `page=0&size=12` with no filter parameters
- **THEN** the server returns the first 12 buildings ordered by the default sort
