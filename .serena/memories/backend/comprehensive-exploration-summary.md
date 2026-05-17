# Backend Exploration Summary

## Architecture Overview
- **Package**: `com.beezhub.student_accommodation`
- **Multi-tenancy**: Hibernate `@Filter(name="tenantFilter")` on `TenantAware` entities; `TenantContext` (ThreadLocal); `TenantListener` stamps `tenant_id` on `@PrePersist`; `TenantFilterAspect` enables filter on `@TenantRequired` methods
- **Security**: JWT-based (JwtUtil, JwtAuthenticationFilter); roles: PLATFORM_ADMIN, TENANT_ADMIN, STUDENT
- **Feature flags**: FF4j (`FF4jConfig`)
- **File storage**: MinIO
- **Metrics**: Micrometer (counters/timers for allocations, batch runs)

## Allocation System (Core Business Logic)

### ScoringStrategy interface
- `scoreAvailableRooms(Student, Application, Building)` → sorted `List<ScoredRoom>`
- `scoreSingleRoom(Student, Application, Room, AllocationRule)` → `ScoredRoom`
- `satisfiesHardConstraints(Student, Room)` → boolean
- Implementations: `LegacyScoringStrategy` (v1.1), `OwnerConfigurableScoringStrategy` (v2.0, `@Primary`)

### OwnerConfigurableScoringStrategy (v2.0)
**Score components** (all configurable via AllocationRule weights):
1. **FCFS** — normalized by createdAt (100 pts max), weighted by `fcfsWeight`
2. **University preference** — student institution in rule's `priorityInstitutionIds` → 100 pts, weighted by `universityPrefWeight`
3. **NSFAS priority** — keyword detection ("nsfas", "financial aid", "bursary") in specialRequirements → 100 pts, weighted by `nsfasPriorityWeight`
4. **Accessibility match** — required+compliant=100, not required+compliant=25, neither=50, required+non-compliant=0; weighted by `accessibilityWeight`

**Hard constraints** (all must pass):
- Gender compatibility (room→building cascade)
- Accessibility (if required, room must be compliant)
- Year of study (min/max on RoomAttributes)
- Institution (building.institutions must include student's)
- Prior decline (student hasn't declined this room before)

**Rule resolution**: building-specific active rule (highest priority) → global active rule → default (all weights=1)

### AllocationLifecycleService
- `createRoomAllocation` — validates prerequisites, builds OFFERED allocation, audit logs, updates waiting list, room occupancy, sends notification event
- `acceptOffer` — validates check-in date within `checkInExpiryDays`, sets ACCEPTED, sets `checkInExpiryDate`, publishes `AllocationAcceptedEvent`
- `declineOffer` — sets DECLINED, releases room occupancy, re-queues student via WaitingListService, triggers `allocateNextFromWaitingList`
- `deleteRoomAllocation` — CANCELLED status, releases occupancy, triggers next allocation
- `allocateRoomByApplication` — manual allocation by applicationId+roomId
- `moveStudent` → delegates to `AllocationMoveService`
- `updateRoomAllocation` — room swap with occupancy updates
- Offer expiry: `offerExpiryDays` (default 7), check-in expiry: `checkInExpiryDays` (default 7)

### AllocationStatusValidator
- Static utility: validates `AllocationStatus.canTransitionTo(newStatus)`

### AutoAllocationService
- Triggered when application transitions to APPROVED and auto-allocation enabled
- Flow: check feature flag → validate documents approved → score rooms → allocate best → fallback to AWAITING_ALLOCATION + waiting list

### AllocationBatchService
- `scheduledBatchAllocation` — called by ScheduledJobRunner; checks `batch-enabled` property; processes waiting list in batches (default 50)
- `batchAllocateFromWaitingList(maxAllocations)` — greedy matching: build scoring matrix → track room capacity → assign best available room per student → create allocations in separate transactions
- Staged allocation support: `proposeAllocation`, `confirmAllocation`, `rejectAllocation`, `confirmBatchAllocations`
- `overrideConstraints` — admin can bypass hard constraints for specific student-room pair

### AllocationScoringOrchestrator
- `buildScoringMatrix` — scores all waiting list students against all rooms
- `buildRoomCapacityMap` — available spots per room
- `getWaitingListWithScores` — enriches waiting list entries with best room scores
- `rescoreWaitingList` — validates scoring succeeds (scores are transient, not persisted)

### WaitingListService
- `enqueueWaitingList` — prevents duplicates, checks no active allocation exists
- `updateStatusToAllocated` — marks entry as ALLOCATED
- `reQueueStudent` — after decline: sets application to AWAITING_ALLOCATION, adds remarks, re-enqueues, publishes `AllocationDeclinedEvent`

### AllocationCheckInService
- `confirmCheckIn` → ACTIVE status, sets checkInDate, updates application to CHECKED_IN
- `confirmCheckOut` → COMPLETED, releases occupancy, publishes `AllocationFreedEvent`
- `endAllocation` → COMPLETED, releases occupancy

## SystemSettingService (Cascade Model)
- Platform settings at `tenant_id=0` (native queries bypass Hibernate filter)
- Tenant overrides at their own `tenant_id`
- `getSetting(key)`: tenant row → platform fallback
- `getAllSettings()`: merged map (platform base + tenant overrides)
- `isAutoAllocationEnabled()`: cascade lookup of `auto_allocation_enabled`

## AllocationStatus State Machine
OFFERED → ACCEPTED/DECLINED/CANCELLED/EXPIRED
ACCEPTED → ACTIVE/CANCELLED
ACTIVE → COMPLETED/SUSPENDED
(validated by `AllocationStatus.canTransitionTo()`)

## ApplicationStatus Values
PENDING, UNDER_REVIEW, APPROVED, REJECTED, CANCELLED, WAITLISTED, ALLOCATED, AWAITING_ALLOCATION, CHECKED_IN

## Notification Contexts (email templates)
Welcome, PasswordReset, RoomAllocated, ApplicationApproved, ApplicationRejected, DocumentVerified, DocumentRejected, DocumentComment, AllocationProposal, AllocationConfirmed, AllocationRejected, AllocationMoved, OfferExpiryReminder, OfferExpired, OfferDeclinedRequeue, OfferDeclinedMaxReached, CheckInReminder, CheckInExpired

## Key Service Files
- `service/allocation/OwnerConfigurableScoringStrategy.java` — primary scoring (526 lines)
- `service/allocation/AllocationLifecycleService.java` — allocation CRUD + state transitions (381 lines)
- `service/allocation/AllocationBatchService.java` — batch processing + staged allocations (313 lines)
- `service/allocation/AutoAllocationService.java` — auto-allocation on approval (139 lines)
- `service/allocation/WaitingListService.java` — waiting list management (113 lines)
- `service/allocation/AllocationCheckInService.java` — check-in/out (112 lines)
- `service/SystemSettingService.java` — cascade settings (140 lines)
