## ADDED Requirements

### Requirement: Building Maps Config Endpoint
The system SHALL provide a `/maps-config` endpoint on the `BuildingController` to serve map configuration data required by the frontend `building.service.ts`.

#### Scenario: Frontend requests maps config
- **WHEN** the frontend calls `GET /api/buildings/maps-config`
- **THEN** the backend responds with the map configuration payload

### Requirement: Activity Log Retrieval by ID
The system SHALL provide an endpoint in `ActivityLogController` to fetch a single activity log by its ID.

#### Scenario: Admin fetches specific activity log
- **WHEN** an admin makes a request to `GET /api/admin/activity-logs/{id}`
- **THEN** the backend returns the activity log details for that ID

### Requirement: Complete Student Service Integration
The frontend `StudentService` SHALL support the full CRUD lifecycle provided by `StudentController`.

#### Scenario: Frontend deletes a student profile
- **WHEN** the application invokes the `deleteStudent` method in `StudentService`
- **THEN** a `DELETE /api/student/{id}` request is made to the backend

### Requirement: Complete Application Service Integration
The frontend `application.service.ts` SHALL accurately map to the `ApplicationController` endpoints without path conflicts.

#### Scenario: Frontend fetches applications by status
- **WHEN** the frontend calls `getApplicationByStatus`
- **THEN** a request is made to `GET /api/applications/status/{status}` instead of the incorrect `GET /api/applications/{status}` path

### Requirement: Staged Allocation Controller Consolidation
The system SHALL consolidate staged allocation logic by removing duplicated endpoints from `RoomAllocationController`.

#### Scenario: Admin proposes batch allocation
- **WHEN** the admin proposes an allocation
- **THEN** the request strictly uses the unified endpoint in `StagedAllocationController`, and the redundant endpoint in `RoomAllocationController` is removed
