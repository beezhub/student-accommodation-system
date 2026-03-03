## ADDED Requirements

### Requirement: Failed notifications are retried automatically
The system SHALL automatically retry outbound email notifications that failed to send. Retries MUST occur on a scheduled interval (default: every 5 minutes). A notification MUST be retried up to a maximum of 3 times before being marked as permanently failed. Each retry attempt MUST use exponential backoff between attempts.

#### Scenario: Failed notification is retried
- **WHEN** a `Notification` row has `status = FAILED` and `retryCount < 3`
- **THEN** the scheduler picks it up within the next retry interval, attempts to resend the email, and updates `status` to `SENT` on success or increments `retryCount` on failure

#### Scenario: Notification permanently failed after max retries
- **WHEN** a `Notification` row has `status = FAILED` and `retryCount >= 3`
- **THEN** the scheduler marks it as `PERMANENTLY_FAILED` and does not attempt further retries

#### Scenario: Retry skipped if no template data
- **WHEN** a `Notification` row has `status = FAILED` but `templateData` is null (legacy row from before this feature)
- **THEN** the retry is skipped with a warning log and the row is left as-is

### Requirement: Notification template data is persisted at send time
When an outbound email notification is created, the system MUST persist the template rendering context (key-value pairs used to populate the email template) as a JSON payload in the `templateData` column on the `Notification` entity. This data MUST be sufficient to reconstruct and resend the email without re-fetching the original domain entities.

#### Scenario: Template data stored on notification creation
- **WHEN** a new `Notification` is created and saved
- **THEN** the `templateData` column contains a non-null JSON object with all variables required to render the notification's email template

#### Scenario: Retry uses stored template data
- **WHEN** `retryNotification()` is called for a `FAILED` notification with non-null `templateData`
- **THEN** the email is reconstructed from `templateData` alone, without loading any `RoomAllocation`, `Application`, or `Student` entities

### Requirement: Allocation batch confirmation returns complete result
The `confirmBatchAllocationsWithResult` operation MUST return a `BatchConfirmationResult` containing the total number of allocations attempted, the count of successes, the count of failures, the list of confirmed allocation IDs, and any per-allocation error messages. It MUST NOT return null.

#### Scenario: Successful batch confirmation
- **WHEN** `POST /api/admin/staged-allocations/batch/{batchId}/confirm-all` is called with a valid batch containing proposed staged allocations
- **THEN** the response is HTTP 200 with a `BatchConfirmationResult` body where `successCount > 0` and `failureCount == 0`

#### Scenario: Partial failure batch confirmation
- **WHEN** some staged allocations in the batch fail to confirm (e.g., room no longer available)
- **THEN** the response is HTTP 200 with a `BatchConfirmationResult` body listing both successful and failed allocations, and `errors` contains per-allocation failure reasons

#### Scenario: Null never returned
- **WHEN** `confirmBatchAllocationsWithResult` is called under any condition
- **THEN** the return value is a non-null `BatchConfirmationResult` object

### Requirement: Available rooms for allocation batch use a single database query
The system MUST determine available rooms for an allocation batch using a single database query that filters at the database level. The query MUST NOT load all rooms into memory and MUST NOT issue additional per-room queries to check occupancy.

#### Scenario: Single query for available rooms
- **WHEN** `getAvailableRoomsForBatch()` is called
- **THEN** exactly one database query is issued, and it returns only rooms where current active allocations are fewer than room capacity
