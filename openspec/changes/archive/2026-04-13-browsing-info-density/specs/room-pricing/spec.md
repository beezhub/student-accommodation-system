## ADDED Requirements

### Requirement: Room entity includes a monthly rate field
The `room` table SHALL include a `monthly_rate` column of type `NUMERIC(10,2)`, nullable. The corresponding `Room` JPA entity SHALL have a `monthlyRate` field of type `BigDecimal`. The field SHALL accept values greater than or equal to zero. A null value indicates that pricing has not been set for this room.

#### Scenario: Room is created with a monthly rate
- **WHEN** an admin creates a room with `monthlyRate = 2500.00`
- **THEN** the room is persisted with `monthly_rate = 2500.00` in the database
- **AND** the `RoomResponse` DTO includes `monthlyRate: 2500.00`

#### Scenario: Room is created without a monthly rate
- **WHEN** an admin creates a room without specifying a monthly rate
- **THEN** the room is persisted with `monthly_rate = NULL` in the database
- **AND** the `RoomResponse` DTO includes `monthlyRate: null`

#### Scenario: Room monthly rate is updated
- **WHEN** an admin updates a room's monthly rate from 2500.00 to 3000.00
- **THEN** the room's `monthly_rate` column is updated to 3000.00

#### Scenario: Room monthly rate rejects negative values
- **WHEN** an admin attempts to create or update a room with `monthlyRate = -100.00`
- **THEN** the system SHALL return a validation error (400 Bad Request)

---

### Requirement: Room request DTO accepts monthly rate
The `RoomRequest` DTO SHALL include an optional `monthlyRate` field of type `BigDecimal`. The admin room creation and update endpoints SHALL accept this field and persist it to the room entity.

#### Scenario: Admin creates a room with pricing via API
- **WHEN** a `POST /api/rooms` request includes `"monthlyRate": 2500.00`
- **THEN** the created room has `monthly_rate = 2500.00` in the database

#### Scenario: Admin room management form includes price field
- **WHEN** an admin opens the room create or edit form in the admin UI
- **THEN** the form SHALL include a "Monthly Rate" input field (numeric, optional, min 0)

---

### Requirement: Building API responses include pricing and availability summary
The `BuildingResponse` DTO SHALL include three additional fields computed from the building's rooms: `minMonthlyRate` (minimum `monthly_rate` across all rooms with non-null pricing), `maxMonthlyRate` (maximum `monthly_rate` across all rooms with non-null pricing), and `availableRoomCount` (count of rooms with status `AVAILABLE`). These fields SHALL be present on all building list and detail API responses.

#### Scenario: Building with priced rooms shows price range
- **WHEN** a building has rooms with monthly rates [2000.00, 2500.00, 3000.00]
- **THEN** the building API response includes `minMonthlyRate: 2000.00` and `maxMonthlyRate: 3000.00`

#### Scenario: Building with no priced rooms shows null pricing
- **WHEN** a building has rooms but none have a `monthly_rate` set
- **THEN** the building API response includes `minMonthlyRate: null` and `maxMonthlyRate: null`

#### Scenario: Building availability count reflects room status
- **WHEN** a building has 10 rooms, 3 with status AVAILABLE, 5 OCCUPIED, 2 MAINTENANCE
- **THEN** the building API response includes `availableRoomCount: 3`

#### Scenario: Building with no rooms shows zero availability
- **WHEN** a building has no rooms
- **THEN** the building API response includes `availableRoomCount: 0` and `minMonthlyRate: null` and `maxMonthlyRate: null`

---

### Requirement: Building detail API includes room summaries
The building detail endpoint (`GET /api/building/{id}`) response SHALL include a `rooms` array. Each entry SHALL contain: `roomNumber`, `capacity`, `monthlyRate`, `status`, and `currentOccupancy`. The array SHALL include all rooms belonging to the building.

#### Scenario: Building detail includes room list with pricing
- **WHEN** an unauthenticated user requests `GET /api/building/42`
- **THEN** the response includes a `rooms` array with one entry per room in the building
- **AND** each entry includes `roomNumber`, `capacity`, `monthlyRate` (may be null), `status`, and `currentOccupancy`

#### Scenario: Building with no rooms returns empty room array
- **WHEN** an unauthenticated user requests a building that has no rooms
- **THEN** the response includes `rooms: []`

---

### Requirement: Flyway migration adds monthly_rate column
A Flyway migration SHALL add the `monthly_rate` column to the `room` table. The column SHALL be `NUMERIC(10,2)`, nullable, with no default value. The migration SHALL not modify existing room data.

#### Scenario: Migration adds column without affecting existing data
- **WHEN** the migration runs against a database with existing room records
- **THEN** all existing rooms have `monthly_rate = NULL`
- **AND** no other columns or data are modified
