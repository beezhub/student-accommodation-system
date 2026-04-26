## ADDED Requirements

### Requirement: "Notify me" button on building detail page
The building detail page SHALL display a "Notify me when available" button when the building has zero available rooms (all rooms at capacity). The button MUST only be visible to authenticated students.

#### Scenario: Building at capacity shows notify button
- **WHEN** an authenticated student views a building detail page and `availableRoomCount` is 0
- **THEN** a "Notify me when available" button is displayed

#### Scenario: Building with availability hides notify button
- **WHEN** a student views a building detail page and `availableRoomCount` is greater than 0
- **THEN** the "Notify me" button is NOT displayed

#### Scenario: Unauthenticated user does not see notify button
- **WHEN** an unauthenticated user views a building at capacity
- **THEN** the "Notify me" button is NOT displayed (login is required)

### Requirement: Student can subscribe to building availability
The system SHALL provide `POST /api/availability-subscriptions/{buildingId}` to create a subscription. Only one active subscription per student per building is allowed.

#### Scenario: Student subscribes to availability
- **WHEN** an authenticated student calls `POST /api/availability-subscriptions/42`
- **THEN** the system creates an `availability_subscription` record and returns `201 Created`

#### Scenario: Duplicate subscription is idempotent
- **WHEN** a student calls `POST /api/availability-subscriptions/42` for a building they are already subscribed to
- **THEN** the system returns `200 OK` without creating a duplicate

### Requirement: Student can unsubscribe from building availability
The system SHALL provide `DELETE /api/availability-subscriptions/{buildingId}` to remove a subscription.

#### Scenario: Student unsubscribes
- **WHEN** an authenticated student calls `DELETE /api/availability-subscriptions/42`
- **THEN** the subscription record is deleted and the system returns `204 No Content`

### Requirement: UI reflects subscription state
After subscribing, the button text MUST change to "Subscribed — we'll notify you" with a filled bell icon. Clicking it again MUST unsubscribe.

#### Scenario: After subscribing, button shows subscribed state
- **WHEN** a student clicks "Notify me when available" and the subscription succeeds
- **THEN** the button text changes to "Subscribed — we'll notify you" with a filled bell icon

#### Scenario: Student clicks subscribed button to unsubscribe
- **WHEN** a student clicks the "Subscribed" button
- **THEN** a confirmation dialog asks "Stop receiving availability notifications for this building?" and on confirm the subscription is removed

### Requirement: Availability notification triggered on room vacancy
When a `Room.status` changes to `AVAILABLE` in a building that was previously at full capacity, the system SHALL send a notification (email and in-app) to all students subscribed to that building. The subscription MUST be marked as notified (`notified_at` set) and considered consumed (one-shot).

#### Scenario: Room becomes available in fully-booked building
- **WHEN** a room in building 42 changes status to `AVAILABLE` and building 42 was previously at 0 available rooms
- **THEN** all students with active subscriptions for building 42 receive a notification and their `notified_at` timestamp is set

#### Scenario: Room becomes available in building with existing availability
- **WHEN** a room in building 42 changes status to `AVAILABLE` but building 42 already had other available rooms
- **THEN** no availability notifications are triggered (the building was not at capacity)

### Requirement: Notified subscriptions are consumed
After a subscriber is notified, their subscription is consumed (one-shot). To receive future availability notifications for the same building, the student MUST re-subscribe.

#### Scenario: Student re-subscribes after notification
- **WHEN** a student whose subscription was consumed (notified) visits the building detail page
- **THEN** the button shows "Notify me when available" (not subscribed state) and the student can create a new subscription

### Requirement: Get subscription status endpoint
The system SHALL provide `GET /api/availability-subscriptions/{buildingId}` that returns whether the authenticated student has an active (un-notified) subscription for that building.

#### Scenario: Student has active subscription
- **WHEN** an authenticated student calls `GET /api/availability-subscriptions/42` and has an active subscription
- **THEN** the system returns `200 OK` with `{ subscribed: true }`

#### Scenario: Student has no subscription
- **WHEN** an authenticated student calls `GET /api/availability-subscriptions/42` and has no active subscription
- **THEN** the system returns `200 OK` with `{ subscribed: false }`
