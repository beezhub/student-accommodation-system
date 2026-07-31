## ADDED Requirements

### Requirement: Student sees waitlist position on dashboard
The system SHALL display the student's current waitlist position when their application status is `WAITING_LIST`. The position MUST be computed dynamically as a rank ordered by `created_at ASC` within the same tenant and academic year.

#### Scenario: Student with WAITING_LIST status sees position
- **WHEN** a student views their dashboard and their application status is `WAITING_LIST`
- **THEN** the dashboard SHALL display a waitlist status card showing "You are #N in the queue" where N is their computed position

#### Scenario: Student not on waitlist does not see waitlist card
- **WHEN** a student views their dashboard and their application status is not `WAITING_LIST`
- **THEN** no waitlist status card SHALL be displayed

### Requirement: Waitlist card shows queue depth
The waitlist status card SHALL display the total number of students on the waitlist for the same tenant and academic year, giving the student context for their position (e.g., "#3 of 12 students waiting").

#### Scenario: Queue depth displayed alongside position
- **WHEN** the waitlist status card renders
- **THEN** it SHALL show both the student's position and the total queue size in the format "Position #N of M"

### Requirement: Waitlist card shows estimated wait time
The system SHALL display an estimated wait time based on historical allocation frequency. If insufficient historical data exists, the system SHALL display "Estimated wait time unavailable" instead.

#### Scenario: Estimated wait time available
- **WHEN** the system has at least 5 completed allocations in the same academic year
- **THEN** the waitlist card SHALL display "Estimated wait: approximately N days" based on the average time between allocations multiplied by positions ahead

#### Scenario: Insufficient data for estimation
- **WHEN** fewer than 5 completed allocations exist for estimation
- **THEN** the waitlist card SHALL display "Estimated wait time unavailable — allocations are processed regularly"

### Requirement: Waitlist card includes notification promise
The waitlist status card SHALL include a reassurance message confirming that the student will be notified when their status changes.

#### Scenario: Notification promise displayed
- **WHEN** the waitlist status card renders
- **THEN** it SHALL include the text "You'll be notified by email as soon as a room becomes available for you"

### Requirement: Waitlist position API endpoint
The system SHALL provide a `GET /api/waitlist/position` endpoint that returns the authenticated student's waitlist position, queue depth, and estimated wait time. The endpoint MUST return `404` if the student is not on any waitlist.

#### Scenario: Authenticated student on waitlist queries position
- **WHEN** an authenticated student with a `WAITING_LIST` application calls `GET /api/waitlist/position`
- **THEN** the system returns `200 OK` with `{ position: N, totalInQueue: M, estimatedWaitDays: D | null }`

#### Scenario: Student not on waitlist queries position
- **WHEN** an authenticated student without a `WAITING_LIST` application calls `GET /api/waitlist/position`
- **THEN** the system returns `404 Not Found`

#### Scenario: Unauthenticated request to waitlist position
- **WHEN** an unauthenticated user calls `GET /api/waitlist/position`
- **THEN** the system returns `401 Unauthorized`
