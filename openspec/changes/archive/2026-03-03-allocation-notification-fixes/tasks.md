## 1. Fix Decline Limit (OFFER-004 Compliance)

- [x] 1.1 Remove `MAX_DECLINE_COUNT` constant from `AllocationLifecycleService`
- [x] 1.2 Replace `handleDeclineConsequences` to unconditionally call `waitingListService.reQueueStudent` on every decline
- [x] 1.3 Delete `waitingListService.cancelApplicationAfterMaxDeclines` call site and the method itself from `WaitingListService`
- [x] 1.4 Remove `maxDeclineCount` field from `AllocationDeclinedEvent` if it is only used by the cancelled code path
- [x] 1.5 Add or update a unit test in `AllocationLifecycleServiceTest` verifying that repeated declines never cancel the application

## 2. Fix Available Rooms Query (N+1 Elimination)

- [x] 2.1 Add `findRoomsWithAvailableCapacity()` JPQL query to `RoomRepository` using a correlated subquery on `RoomAllocation` statuses `ACTIVE`, `ACCEPTED`, `OFFERED`
- [x] 2.2 Rewrite `AllocationScoringOrchestrator.getAvailableRoomsForBatch()` to call the new repository method and remove the `findAll()` call and the per-room COUNT loop
- [x] 2.3 Add a repository-level test or integration test verifying the query returns only rooms with available capacity

## 3. Implement confirmBatchAllocationsWithResult

- [x] 3.1 Add `confirmBatchAllocationsWithResult(String batchId, Long adminId)` method to `StagedAllocationService` — iterates `PROPOSED` staged allocations for the batch, calls `confirmAllocation` per entry, accumulates success/failure counts and error messages, returns a populated `BatchConfirmationResult`
- [x] 3.2 Update `AllocationBatchService.confirmBatchAllocationsWithResult` to delegate to the new `StagedAllocationService` method (replace the `return null` body)
- [x] 3.3 Verify `RoomAllocationService.confirmBatchAllocationsWithResult` facade passes through correctly (no change expected, but confirm)
- [x] 3.4 Add a unit test for `StagedAllocationService.confirmBatchAllocationsWithResult` covering the all-success path, the partial-failure path, and the empty-batch path

## 4. Implement Notification Retry

- [x] 4.1 Add nullable `template_data JSONB` column to the `notification` table via a new Flyway migration `V2026_03_03_0001__add_notification_template_data.sql`
- [x] 4.2 Add `templateData` field (`Map<String, Object>` serialised as JSON) to the `Notification` entity with `@Column(columnDefinition = "jsonb")` and appropriate Jackson/JPA converter
- [x] 4.3 Update `NotificationService` (or wherever `Notification` rows are created) to populate `templateData` with the email template variables at creation time
- [x] 4.4 Implement `NotificationRetryService.retryNotification()` — load `templateData`, check for null (skip with warning if null), reconstruct and dispatch the email via the existing mail sender, update status to `SENT` on success
- [x] 4.5 Use `getExponentialBackoff(retryCount)` (already implemented, currently unused) to enforce minimum delay between retries before picking up a notification
- [x] 4.6 Wire `NotificationRetryService.retryFailedNotifications()` into `ScheduledJobRunner` — add `notificationRetryService` field and a `runNotificationRetry()` `@Scheduled` method with `fixedDelayString = "${app.notification.retry-check-interval-ms:300000}"`
- [x] 4.7 Add unit tests for `NotificationRetryService` covering: successful retry, failure increments retry count, permanent failure after max retries, null templateData skipped

## 5. Verification

- [x] 5.1 Run `./gradlew test` in `backend/` — all tests must pass
- [x] 5.2 Run `./gradlew build` — clean build with no compilation errors
