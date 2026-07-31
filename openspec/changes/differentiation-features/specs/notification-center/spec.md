## ADDED Requirements

### Requirement: Notification center page exists
The system SHALL provide a `/notifications` route accessible to authenticated students that displays a paginated list of all their notifications with type-specific rendering, read/unread status, and action buttons.

#### Scenario: Student navigates to notification center
- **WHEN** an authenticated student navigates to `/notifications`
- **THEN** the page displays their notifications in reverse chronological order with 20 items per page

#### Scenario: Unauthenticated user redirected
- **WHEN** an unauthenticated user navigates to `/notifications`
- **THEN** they are redirected to the login page with a returnUrl of `/notifications`

### Requirement: Notifications have read/unread status
Each notification in the notification center SHALL display its read/unread status visually. Unread notifications MUST have a distinct background color (light blue) and a dot indicator. Reading a notification (clicking or expanding it) MUST mark it as read.

#### Scenario: Unread notification displayed
- **WHEN** a notification has no `read_at` timestamp
- **THEN** it is displayed with a light blue background and an unread dot indicator

#### Scenario: Clicking notification marks it as read
- **WHEN** a student clicks an unread notification in the notification center
- **THEN** the system calls `PUT /api/notifications/{id}/read`, sets `read_at` to the current timestamp, and the visual indicator updates to read state

### Requirement: Mark all as read action
The notification center SHALL provide a "Mark all as read" button that marks all unread notifications as read in a single action.

#### Scenario: Student marks all as read
- **WHEN** a student clicks "Mark all as read"
- **THEN** the system calls `PUT /api/notifications/read-all` and all notifications update to read state visually

#### Scenario: No unread notifications
- **WHEN** all notifications are already read
- **THEN** the "Mark all as read" button is disabled

### Requirement: Type-specific notification rendering
The notification center SHALL render notifications with type-specific icons, colors, and action buttons based on the notification type. The system MUST support at least 5 type-specific templates for the most common types, with a generic fallback for remaining types.

#### Scenario: Offer notification rendered with specific template
- **WHEN** a notification of type `OFFER_RECEIVED` is displayed
- **THEN** it renders with a gift icon, green accent color, and a "View Offer" action button that navigates to the dashboard

#### Scenario: Application status notification rendered
- **WHEN** a notification of type `APPLICATION_STATUS_CHANGE` is displayed
- **THEN** it renders with a document icon, blue accent, and a "View Application" action button

#### Scenario: Unknown type uses generic fallback
- **WHEN** a notification of a type without a specific template is displayed
- **THEN** it renders with a bell icon, neutral accent, and the notification message text

### Requirement: Notification list API endpoint
The system SHALL provide a `GET /api/notifications` paginated endpoint that returns the authenticated student's notifications with read/unread status. Parameters: `page`, `size`, `unreadOnly` (boolean, default false).

#### Scenario: Student retrieves notifications
- **WHEN** an authenticated student calls `GET /api/notifications?page=0&size=20`
- **THEN** the system returns `200 OK` with a paginated list of notifications including `id`, `type`, `message`, `createdAt`, `readAt`

#### Scenario: Filter unread only
- **WHEN** an authenticated student calls `GET /api/notifications?unreadOnly=true`
- **THEN** only notifications where `read_at IS NULL` are returned

### Requirement: Read status API endpoints
The system SHALL provide `PUT /api/notifications/{id}/read` and `PUT /api/notifications/read-all` endpoints. The single-read endpoint MUST set `read_at` to the current timestamp. The read-all endpoint MUST set `read_at` on all of the student's unread notifications.

#### Scenario: Mark single notification as read
- **WHEN** `PUT /api/notifications/42/read` is called
- **THEN** `user_notification` row 42 has `read_at` set to the current timestamp and returns `200 OK`

#### Scenario: Mark single already-read notification
- **WHEN** `PUT /api/notifications/42/read` is called for a notification already marked as read
- **THEN** the system returns `200 OK` with no change (idempotent)

#### Scenario: Mark all as read
- **WHEN** `PUT /api/notifications/read-all` is called
- **THEN** all `user_notification` rows for the student where `read_at IS NULL` have `read_at` set to the current timestamp

### Requirement: Unread count in notification bell
The existing notification bell dropdown SHALL display an unread count badge. The count MUST update when notifications are marked as read.

#### Scenario: Unread badge shows count
- **WHEN** the student has 5 unread notifications
- **THEN** the notification bell icon displays a badge with "5"

#### Scenario: Badge hidden when no unread
- **WHEN** the student has 0 unread notifications
- **THEN** no badge is displayed on the notification bell icon

### Requirement: Notification bell links to notification center
The notification bell dropdown SHALL include a "View all notifications" link at the bottom that navigates to `/notifications`.

#### Scenario: User clicks view all
- **WHEN** a student clicks "View all notifications" in the bell dropdown
- **THEN** the application navigates to `/notifications`
