## MODIFIED Requirements

### Requirement: Students may decline offers without limit
A student SHALL be able to decline any number of room allocation offers without their application being cancelled or penalised. On every decline the student's application MUST be returned to the `AWAITING_ALLOCATION` status and re-added to the waiting list. There MUST be no constant, configuration value, or code path that cancels an application due to exceeding a decline count threshold.

#### Scenario: First decline re-queues student
- **WHEN** a student declines a room offer for the first time
- **THEN** the allocation is set to `DECLINED`, the application status returns to `AWAITING_ALLOCATION`, and the student is placed back on the waiting list

#### Scenario: Repeated declines continue to re-queue student
- **WHEN** a student has already declined two or more previous offers and declines another
- **THEN** the allocation is set to `DECLINED`, the application status returns to `AWAITING_ALLOCATION`, and the student is placed back on the waiting list — no cancellation occurs

#### Scenario: No application cancellation on decline
- **WHEN** any number of offers have been declined by a student
- **THEN** the student's application status is NEVER set to `CANCELLED` as a result of the decline action alone
