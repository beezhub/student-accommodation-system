## MODIFIED Requirements

### Requirement: Students may decline offers without limit
A student SHALL be able to decline any number of room allocation offers without their application being cancelled or penalised. On every decline the student's application MUST be returned to the `AWAITING_ALLOCATION` status and re-added to the waiting list. There MUST be no constant, configuration value, or code path that cancels an application due to exceeding a decline count threshold. When re-added to the waiting list, the student's waitlist entry MUST be created with a `created_at` timestamp of the decline action, placing them at the end of the queue. The student MUST be informed of their new waitlist position via a snackbar message and via the waitlist status card on their dashboard.

#### Scenario: First decline re-queues student
- **WHEN** a student declines a room offer for the first time
- **THEN** the allocation is set to `DECLINED`, the application status returns to `AWAITING_ALLOCATION`, a new waitlist entry is created, and the student is placed back on the waiting list

#### Scenario: Repeated declines continue to re-queue student
- **WHEN** a student has already declined two or more previous offers and declines another
- **THEN** the allocation is set to `DECLINED`, the application status returns to `AWAITING_ALLOCATION`, and the student is placed back on the waiting list — no cancellation occurs

#### Scenario: No application cancellation on decline
- **WHEN** any number of offers have been declined by a student
- **THEN** the student's application status is NEVER set to `CANCELLED` as a result of the decline action alone

#### Scenario: Student sees waitlist position after decline
- **WHEN** a student declines an offer and is re-queued to the waiting list
- **THEN** a snackbar message displays "You've been placed back on the waiting list at position #N" and the dashboard waitlist card reflects the new position
