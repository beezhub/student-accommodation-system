## Context

Four Phase-1 critical defects exist in the allocation and notification subsystems, all identified in the architectural review. They range from a spec violation (unlimited declines per OFFER-004) to a production crash bug (`confirmBatchAllocationsWithResult` returning `null`), a scalability bottleneck (N+1 queries on every batch), and a completely inactive retry mechanism for failed emails.

The codebase uses Spring Boot 3 with Spring Data JPA, MapStruct, and `@Async`/`@Scheduled` for background work. The notification model stores outbound email records in the `notification` table; however the table currently has no column for storing the original template context, which is the blocker for implementing retry.

## Goals / Non-Goals

**Goals:**
- Remove the 3-strike decline cancellation and comply with OFFER-004 (unlimited declines)
- Eliminate the `findAll()` + N+1 query pattern from `getAvailableRoomsForBatch()`
- Implement `confirmBatchAllocationsWithResult()` so the batch-confirm REST endpoints work correctly
- Implement real notification retry dispatch by persisting template data as JSON, then re-enable the scheduler

**Non-Goals:**
- Full rewrite of the notification system or switch to a message broker
- Implementing push/webhook notifications (out of scope)
- Changing the scoring algorithm logic
- Addressing Phase 2/3 open issues in this change

## Decisions

### Decision 1 — Remove MAX_DECLINE_COUNT entirely (not just raise the limit)

**Rationale:** OFFER-004 states there is no limit. Keeping the constant at any value (even a high one) means future engineers will have to reason about it again. The right fix is removal. The `cancelApplicationAfterMaxDeclines` method in `WaitingListService` is also deleted to avoid leaving a dead code path that could be accidentally re-introduced.

**Alternative considered:** Set `MAX_DECLINE_COUNT = Integer.MAX_VALUE`. Rejected — semantically wrong and misleading.

### Decision 2 — Single JPQL query with correlated subcount for available rooms

**Rationale:** The `Room` entity does not have a guaranteed up-to-date `currentOccupancy` field — occupancy truth lives in `room_allocation` rows. A correlated subquery (or JOIN GROUP BY) is the correct approach. We use JPQL rather than native SQL to stay consistent with the repository layer style in this codebase.

```jpql
SELECT r FROM Room r
WHERE (SELECT COUNT(ra) FROM RoomAllocation ra
       WHERE ra.room.id = r.id
         AND ra.status IN ('ACTIVE', 'ACCEPTED', 'OFFERED')) < r.capacity
```

**Alternative considered:** Maintain a `currentOccupancy` counter on `Room` and query `WHERE currentOccupancy < capacity`. Rejected — requires careful increment/decrement at every state transition, introducing a whole class of consistency bugs.

### Decision 3 — Implement confirmBatchAllocationsWithResult by delegating to StagedAllocationService

**Rationale:** `StagedAllocationService.confirmBatchAllocations()` already contains the correct iteration logic (fetch PROPOSED entries for the batch, call `confirmAllocation` per entry). Rather than duplicating that logic in `AllocationBatchService`, we will add a `confirmBatchAllocationsWithResult()` overload to `StagedAllocationService` that returns a populated `BatchConfirmationResult`, and have `AllocationBatchService` delegate to it.

**Alternative considered:** Re-implement the loop entirely in `AllocationBatchService`. Rejected — duplication of the staged allocation iteration logic.

### Decision 4 — Store template data as a JSON column on the Notification entity

**Rationale:** The retry blocker is that `retryNotification()` cannot reconstruct the email context from the `Notification` row alone. The cleanest solution is to store a `Map<String, Object>` (serialised to JSONB) on `Notification` at send time. This lets `retryNotification()` reload the stored context and pass it directly to the email template renderer without re-fetching related entities.

**Alternative considered:** Re-fetch source entities via `referenceId`/`referenceType` at retry time. Rejected — it requires a join across several domain tables per notification type, and those entities may have since been updated (e.g., offer declined after the email failed), making the re-fetched context stale or absent.

**Wire the scheduler via ScheduledJobRunner (not direct @Scheduled):** The existing codebase pattern (`ScheduledJobRunner`) centralises all scheduled jobs in one place. We add a `runNotificationRetry()` method there rather than uncommenting the `@Scheduled` directly on `NotificationRetryService`, keeping the pattern consistent.

## Risks / Trade-offs

- **[Risk] Notification `templateData` column adds payload size to the notification table** → Mitigation: column is JSONB and nullable; only populated if non-null context exists. A Flyway migration adds it cleanly.
- **[Risk] Existing `FAILED` notifications in the table have no `templateData`** → Mitigation: `retryNotification()` checks for null `templateData` and logs a warning + skips, rather than throwing. Old failures are left as permanently failed.
- **[Risk] JPQL correlated subquery performance on large datasets** → Mitigation: `room_allocation.room_id` and `room_allocation.status` must be indexed; indexes already exist per the schema migration history. If profiling shows further pressure, a native query with a window function is the next step.
- **[Risk] Removing `cancelApplicationAfterMaxDeclines` could leave callers** → Mitigation: Investigation confirmed it is only called from `handleDeclineConsequences`, which is itself only called from `processDecline`. Full callgraph is clean.

## Migration Plan

1. Add Flyway migration `V2026_03_03_0001__add_notification_template_data.sql` — adds nullable `template_data JSONB` column to `notification` table.
2. Deploy backend — Hibernate validates schema (new nullable column is non-breaking).
3. No data backfill needed — existing `FAILED` rows will be skipped by the retry logic (null check).
4. **Rollback:** Drop the `template_data` column (nullable, no existing data). Re-add `MAX_DECLINE_COUNT` constant and `handleDeclineConsequences` logic from git history if required.

## Open Questions

- Should `getAvailableRoomsForBatch()` also filter by `room.isActive = true` (if such a flag exists)? Investigation showed no `isActive` flag on `Room`; rooms are removed by deletion. No change needed.
- Should `BatchConfirmationResult.errors` include the exception message or just a generic failure reason? Decision: include exception message to aid admin debugging, but never expose student PII in error strings.
