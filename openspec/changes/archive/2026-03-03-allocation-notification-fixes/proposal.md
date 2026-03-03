## Why

Four Phase-1 critical defects prevent the system from being production-ready: a spec-violating decline limit cancels student applications that should stay live, a full-table-scan + N+1 query pattern will degrade every allocation batch run at scale, the batch-confirm endpoint unconditionally returns `null` causing HTTP 500 errors, and the notification retry scheduler was never wired up so failed emails are silently lost forever.

## What Changes

- **Remove** `AllocationLifecycleService.MAX_DECLINE_COUNT` constant and the application-cancellation code path it gates, replacing it with unconditional re-queuing on every decline (aligning with OFFER-004)
- **Replace** `AllocationScoringOrchestrator.getAvailableRoomsForBatch()` full-table-scan + N+1 COUNT loop with a single JOIN query in `RoomRepository`
- **Implement** `AllocationBatchService.confirmBatchAllocationsWithResult()` — the method body is currently 40 lines of TODO comments ending in `return null`, causing NPE on every call to `POST /batch/{batchId}/confirm-all`
- **Implement** notification retry dispatch in `NotificationRetryService.retryNotification()` by storing template data as JSON on the `Notification` entity, then re-enable the `@Scheduled` hook via `ScheduledJobRunner`

## Capabilities

### New Capabilities

- `notification-retry`: Persistent retry of failed outbound emails using stored template data, with exponential backoff and a `MAX_RETRIES` ceiling

### Modified Capabilities

- `offer-lifecycle`: OFFER-004 compliance — remove the 3-strike application cancellation; students must be able to decline any number of offers without losing their application

## Impact

- **`AllocationLifecycleService`** — `MAX_DECLINE_COUNT` constant removed; `handleDeclineConsequences` simplified to always re-queue; `waitingListService.cancelApplicationAfterMaxDeclines` call deleted
- **`WaitingListService`** — `cancelApplicationAfterMaxDeclines` method becomes dead code; can be removed or deprecated
- **`AllocationScoringOrchestrator`** — `getAvailableRoomsForBatch` rewritten; N+1 eliminated
- **`RoomRepository`** — new JPQL query `findRoomsWithAvailableCapacity()` added
- **`AllocationBatchService`** — `confirmBatchAllocationsWithResult` implemented; NullPointerException in both REST controllers resolved
- **`Notification` entity** — new nullable `templateData` JSON column added (Flyway migration required)
- **`NotificationRetryService`** — `retryNotification` implemented; scheduler wired in `ScheduledJobRunner`
- **REST endpoints affected**: `POST /api/admin/staged-allocations/batch/{batchId}/confirm-all`, `PATCH /api/admin/allocations/staged/batch/{batchId}/confirm`
