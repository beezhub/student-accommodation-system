# Configurable Room Allocation System - Implementation Plan

**Created:** January 20, 2026  
**Status:** Ready for Implementation  
**Execution:** AI Agent-driven

## Overview

Transform the current FIFO auto-allocation system into a configurable, rule-driven allocation engine with per-building weighted scoring, student building preferences, staged admin workflows, move capabilities, override audit logging, and comprehensive notifications.

## Current State

### Auto Allocation (Backend)
- **Trigger:** `RoomAllocationDocumentListener` fires on document APPROVED status
- **Logic:** `RoomAllocationService.allocate()` validates rules (approved application, no active allocation, room capacity) and assigns first AVAILABLE room ordered by building ID then room number
- **Waiting List:** FIFO ordering by `created_at` in `WaitingListRepository`
- **Reallocation:** After deletion/end, `allocateFromWaitingList()` picks first waiting student

### Student Preferences (Current)
- **Captured:** accommodation type, move-in date, duration, room type, special requirements, course
- **Missing:** building choice
- **Location:** `PreferenceDto`, `ApplicationPreference` entity, Angular forms in `frontend/src/app/features`

### Admin Review Flow
- **UI:** `ApplicationReviewComponent` shows paginated applications with filters
- **Actions:** Bulk/individual status updates (APPROVED/REJECTED/PENDING)
- **Gaps:** No allocation controls, no waiting list visibility, no student preferences display

### Database Schema Gaps
- Buildings/rooms lack attributes (gender restrictions, accessibility, desirability)
- No allocation rules table
- No staged allocations or audit logging

## Requirements

### Allocation Criteria
1. **Configurable per-building rules** with weighted scoring
2. **Hard constraints:** gender restrictions, accessibility compliance
3. **Soft factors:** building preference match, desirability score
4. **Tie-breaker:** Application created date (earliest first)
5. **Latest rules** always applied to pending allocations

### Student Preferences
1. **Building selection** dropdown in application form
2. **All preference fields** exposed (special requirements, room type)
3. Preferences validated against building/room attributes during scoring

### Admin Capabilities
1. **Staged allocation workflow:** propose → review scores/warnings → confirm/reject batch
2. **Move students:** between rooms with validation, mandatory reason, audit logging
3. **Override constraints:** with reason capture and audit trail (log-only, no approval gate)
4. **Manage rules:** per-building weight configuration and constraint toggles
5. **Waiting list visibility:** with real-time priority scores
6. **Batch limits:** Max 50 students per proposal for review feasibility

### Notifications
- Allocation proposal sent to student
- Allocation confirmation
- Allocation rejection with reason
- Room move with reason and new details

## Implementation Steps

### Step 1: Database Migrations
**Location:** `backend/src/main/resources/db/migration`

Create new migration files:

#### `allocation_rules` table
```sql
- building_id (FK to buildings, nullable for global defaults)
- rule_name VARCHAR(100)
- preference_match_weight DECIMAL(5,2)
- desirability_weight DECIMAL(5,2)
- accessibility_weight DECIMAL(5,2)
- active BOOLEAN DEFAULT true
- priority INT (for multiple rules per building)
- created_at, updated_at TIMESTAMP
- UNIQUE(building_id, rule_name)
```

#### `building_attributes` table
```sql
- building_id (FK to buildings, unique)
- gender_restriction VARCHAR(20) (NONE, MALE_ONLY, FEMALE_ONLY)
- accessibility_features TEXT (JSON or comma-separated)
- desirability_score INT (1-10 scale)
- allow_mixed_gender BOOLEAN DEFAULT true
- created_at, updated_at TIMESTAMP
```

#### `room_attributes` table
```sql
- room_id (FK to rooms, unique)
- accessibility_compliant BOOLEAN DEFAULT false
- gender_restriction VARCHAR(20) (NONE, MALE_ONLY, FEMALE_ONLY)
- desirability_override INT (null or 1-10, overrides building score)
- created_at, updated_at TIMESTAMP
```

#### `staged_allocations` table
```sql
- id BIGINT PRIMARY KEY
- student_id (FK to students)
- room_id (FK to rooms)
- application_id (FK to applications, nullable)
- status VARCHAR(20) (PROPOSED, CONFIRMED, REJECTED)
- score DECIMAL(10,2)
- warnings TEXT (JSON array: ["PARTIAL_MATCH", "SUBOPTIMAL_DESIRABILITY"])
- proposed_by BIGINT (FK to users/admins)
- batch_id VARCHAR(50) (group related proposals)
- created_at, updated_at, confirmed_at TIMESTAMP
```

#### `allocation_audit_log` table
```sql
- id BIGINT PRIMARY KEY
- allocation_id (FK to room_allocations)
- action_type VARCHAR(50) (MOVE, OVERRIDE, DELETE, FORCE_ALLOCATE)
- admin_user_id BIGINT (FK to users)
- reason TEXT NOT NULL
- old_room_id BIGINT (FK to rooms, nullable)
- new_room_id BIGINT (FK to rooms, nullable)
- constraint_bypassed VARCHAR(100) (nullable, e.g., "GENDER_MISMATCH")
- created_at TIMESTAMP
```

#### Alter `applications` table
```sql
- Add preferred_building_id BIGINT (FK to buildings, nullable)
```

### Step 2: Backend Entities & Scoring Engine
**Location:** `backend/src/main/java/com/beezhub`

#### New Entities
- `AllocationRule` (entity, repository, DTO)
- `BuildingAttributes` (entity, repository, DTO)
- `RoomAttributes` (entity, repository, DTO)
- `StagedAllocation` (entity, repository, DTO)
- `AllocationAuditLog` (entity, repository, DTO)

#### New Service: `AllocationScoringService`
**Responsibilities:**
1. Load latest active rule set for target building
2. Enforce hard constraints (return empty if violated):
   - Gender match (student gender vs room/building restriction)
   - Accessibility required vs room accessibility_compliant
3. Calculate weighted score:
   ```
   score = (building_match_bonus × preference_match_weight) +
           (desirability_score × desirability_weight) +
           (accessibility_match_bonus × accessibility_weight)
   ```
4. Apply tie-breaker: `application.created_at ASC`
5. Generate warnings:
   - `BUILDING_MISMATCH` if preferred building unavailable
   - `SUBOPTIMAL_DESIRABILITY` if score < threshold
6. Return `List<ScoredRoom>` with score, warnings, room details

**Methods:**
- `List<ScoredRoom> scoreAvailableRooms(Student, Application, Building preferredBuilding)`
- `ScoredRoom scoreSingleRoom(Student, Application, Room)`
- `AllocationRule getLatestRuleForBuilding(Long buildingId)`
- `boolean validateHardConstraints(Student, Room)`

#### Refactor: `RoomAllocationService`
- Replace `findFirstAvailableRoom()` with `AllocationScoringService.scoreAvailableRooms()`
- Replace FIFO waiting list with priority queue:
  - On `allocateFromWaitingList()`, score all waiting students against available rooms
  - Pick highest-scoring student-room pair
- Add methods:
  - `proposeAllocation(List<Long> studentIds, Long adminId)` → creates `StagedAllocation` records
  - `confirmAllocation(Long stagedAllocationId, Long adminId)` → converts to active allocation
  - `moveStudent(Long allocationId, Long newRoomId, String reason, Long adminId)` → validates, updates, logs audit
  - `overrideConstraints(Long studentId, Long roomId, String reason, Long adminId)` → bypasses hard constraints, logs audit

#### Refactor: `PreferenceDto` & `ApplicationService`
- Add `preferredBuildingId` field to DTO
- Update mapper to persist to `applications.preferred_building_id`
- Validate building exists on save

### Step 3: Frontend - Student Application Forms
**Location:** `frontend/src/app/features`

#### Update Application Create/Update Forms
- **Component:** `ApplicationCreateComponent`, `ApplicationUpdateComponent`
- **Template:** Add building selector:
  ```html
  <mat-form-field>
    <mat-label>Preferred Building</mat-label>
    <mat-select formControlName="preferredBuilding">
      <mat-option *ngFor="let building of buildings$ | async" [value]="building.id">
        {{building.name}}
      </mat-option>
    </mat-select>
  </mat-form-field>
  ```
- **Service:** Inject `BuildingService` to fetch buildings
- **Form:** Add `preferredBuilding` control to reactive form
- Expose special requirements fields if hidden

### Step 4: Admin Backend APIs
**Location:** `backend/src/main/java/com/beezhub/controller`

#### New Controller: `AllocationRuleController`
**Endpoints:**
- `GET /api/admin/allocation-rules?buildingId={id}` - list rules for building
- `POST /api/admin/allocation-rules` - create rule (validate weights sum to 100)
- `PUT /api/admin/allocation-rules/{id}` - update rule
- `DELETE /api/admin/allocation-rules/{id}` - deactivate rule
- `GET /api/admin/allocation-rules/{id}` - get single rule

#### New Controller: `BuildingAttributesController`
**Endpoints:**
- `GET /api/admin/buildings/{id}/attributes` - get attributes
- `PUT /api/admin/buildings/{id}/attributes` - update attributes
- `GET /api/admin/rooms/{id}/attributes` - get room attributes
- `PUT /api/admin/rooms/{id}/attributes` - update room attributes

#### Extend: `RoomAllocationController`
**New Endpoints:**
- `POST /api/admin/allocations/propose` - body: `{studentIds: [], batchId: ""}` (max 50)
  - Returns `List<StagedAllocationDto>` with scores/warnings
- `GET /api/admin/allocations/staged?batchId={id}&status={status}` - paginated staged allocations
- `PATCH /api/admin/allocations/staged/{id}/confirm` - convert to active allocation
- `PATCH /api/admin/allocations/staged/{id}/reject` - mark rejected
- `PATCH /api/admin/allocations/staged/batch/{batchId}/confirm` - batch confirm
- `POST /api/admin/allocations/{id}/move` - body: `{newRoomId, reason}`
  - Validates constraints, logs audit, triggers notification
- `POST /api/admin/allocations/override` - body: `{studentId, roomId, reason, bypassConstraints: []}`
  - Logs to audit with constraint list
- `GET /api/admin/waiting-list?buildingId={id}` - returns waiting list with scores

### Step 5: Admin Frontend UI
**Location:** `frontend/src/app/features/admin`

#### New Component: `AllocationRuleManagementComponent`
**Route:** `/admin/allocation-rules`
**Features:**
- Building selector dropdown
- Rule list table with edit/delete actions
- Create/edit dialog:
  - Weight sliders (preference match, desirability, accessibility)
  - Live validation: weights sum to 100%
  - Constraint toggles (gender restrictions, accessibility required)
  - Priority input
- Save calls `AllocationRuleService`

#### New Component: `StagedAllocationComponent`
**Route:** `/admin/allocations/staged`
**Features:**
- Step 1: Student selection (multi-select with filters, max 50, generate batch ID)
- Step 2: Propose allocation button → calls API, shows loading
- Step 3: Review table:
  - Columns: Student, Application, Proposed Room, Score, Warnings
  - Color-code warnings: yellow (partial match), red (hard constraint bypass if override used)
  - Actions: Confirm All, Reject All, individual confirm/reject
- Confirm triggers notification and converts to active allocation

#### New Component: `AllocationMoveDialogComponent`
**Triggered from:** `AllocationManagementComponent` (existing)
**Features:**
- Current allocation details (student, room, dates)
- New room selector:
  - Search/filter by building, availability, attributes
  - Show room attributes and compatibility warnings
- Reason textarea (required)
- Preview validation results before submit
- Submit calls `moveStudent()` API, logs audit, sends notification

#### Extend: `ApplicationReviewComponent`
**Add columns/details:**
- Preferred building name
- Current allocation status (badge: ALLOCATED, PENDING, WAITING)
- Allocation score (if pending/waiting)
- Button: "View Preferences" → dialog showing full `PreferenceDto`
- Action: "Allocate Now" → opens staged allocation flow for single student

#### New Component: `WaitingListDashboardComponent`
**Route:** `/admin/waiting-list`
**Features:**
- Table with columns: Student, Application, Preferred Building, Priority Score, Created Date
- Sortable by score (desc), created date
- Filter by building, status
- Shows warning badge if rules changed since last score calculation
- Action: "Re-score All" button → recalculates scores using latest rules

#### New Component: `BuildingAttributesManagementComponent`
**Route:** `/admin/buildings/{id}/attributes`
**Features:**
- Edit form for building attributes:
  - Gender restriction dropdown
  - Accessibility features multi-select/textarea
  - Desirability score slider (1-10)
  - Allow mixed gender toggle
- Room attributes sub-table (editable inline):
  - Accessibility compliant, gender restriction, desirability override

### Step 6: Notifications & Email Templates
**Location:** `backend/src/main/resources/templates/email`

#### New Email Templates (Thymeleaf)
1. **allocation-proposal.html**
   - Subject: "Room Allocation Proposal"
   - Content: Proposed room details, building, score, warnings, "This is pending admin confirmation"

2. **allocation-confirmed.html**
   - Subject: "Room Allocation Confirmed"
   - Content: Final room assignment, building, move-in date, next steps

3. **allocation-rejected.html**
   - Subject: "Room Allocation Update"
   - Content: "Your proposed allocation was not confirmed. You remain on the waiting list."

4. **allocation-moved.html**
   - Subject: "Room Change Notification"
   - Content: Old room, new room, reason (from admin), effective date, contact info

#### Service Updates
**Location:** `backend/src/main/java/com/beezhub/service`
- `NotificationService.sendAllocationProposal(StagedAllocation)`
- `NotificationService.sendAllocationConfirmed(RoomAllocation)`
- `NotificationService.sendAllocationRejected(Student, StagedAllocation)`
- `NotificationService.sendAllocationMoved(RoomAllocation, Room oldRoom, String reason)`

Trigger in service layer:
- `proposeAllocation()` → send proposal email
- `confirmAllocation()` → send confirmed email
- `rejectStagedAllocation()` → send rejected email
- `moveStudent()` → send move email

### Step 7: Testing & Documentation

#### Unit Tests
**Location:** `backend/src/test/java/com/beezhub`
- `AllocationScoringServiceTest`:
  - Test hard constraint enforcement (gender, accessibility)
  - Test weighted score calculation with various rule weights
  - Test tie-breaker logic
  - Test warning generation
- `RoomAllocationServiceTest`:
  - Test staged allocation creation with batch limits
  - Test confirm/reject flows
  - Test move validation and audit logging
  - Test override with constraint bypass
  - Test waiting list priority scoring

#### Integration Tests
- End-to-end staged allocation flow (propose → confirm → active allocation)
- Move student with notification trigger
- Rule change impact on waiting list re-scoring
- Batch allocation with partial failures

#### Documentation
**Location:** `docs/`
- Update existing docs or create new:
  - **ALLOCATION_RULES_GUIDE.md**: Explain weighted scoring, per-building rules, how to configure
  - **ADMIN_ALLOCATION_WORKFLOW.md**: Step-by-step for staged allocation, moves, overrides
  - **STUDENT_PREFERENCES_GUIDE.md**: How building selection affects allocation
  - Add section to `ADMIN_DASHBOARD_QUICK_START.md` for new allocation features

## Technical Considerations

### Performance
- **Batch scoring:** Limit to 50 students per proposal to keep response times under 5 seconds
- **Waiting list re-scoring:** Run asynchronously when rules change (consider job/scheduler)
- **Index** on `staged_allocations.batch_id`, `allocation_audit_log.allocation_id`, `applications.preferred_building_id`

### Validation
- Weights must sum to 100% (enforce in controller/service)
- Room capacity checks before confirming staged allocation
- Prevent double-allocation during confirmation (transactional lock)
- Move validation: new room must have capacity, meet hard constraints (unless override)

### Audit Trail
- All admin actions logged to `allocation_audit_log`
- Include admin user ID, timestamp, reason (mandatory for moves/overrides)
- Expose audit log in admin UI for transparency

### Security
- All admin endpoints require `ROLE_ADMIN`
- Validate ownership: students can only view their own allocations/preferences
- Rate-limit allocation proposal endpoint (prevent abuse)

### Data Migration
- Seed default allocation rules for existing buildings (equal weights: 33.33% each for 3 criteria)
- Populate `building_attributes` with sensible defaults (no restrictions, desirability=5)
- Backfill `room_attributes` based on existing room data (if available)

## Success Criteria

1. ✅ Students can select preferred building during application
2. ✅ Allocation respects hard constraints (gender, accessibility)
3. ✅ Scoring uses configurable per-building weighted rules
4. ✅ Waiting list ordered by score, not FIFO
5. ✅ Admins can propose allocations in batches (max 50) with score preview
6. ✅ Admins can confirm/reject staged allocations with warnings displayed
7. ✅ Admins can move students with reason capture and audit logging
8. ✅ Admins can override constraints with reason (logged, no approval gate)
9. ✅ Notifications sent at all stages (propose, confirm, reject, move)
10. ✅ Latest rules always applied to pending allocations
11. ✅ Rule changes trigger waiting list re-scoring

## Execution Notes for AI Agents

- **Approach:** Implement steps sequentially (DB → backend → frontend)
- **Testing:** Write tests alongside each component (TDD encouraged)
- **Commits:** Atomic commits per feature (e.g., "Add allocation_rules table migration")
- **Dependencies:** Ensure Spring Boot, JPA, Angular Material versions support new features
- **Code Style:** Follow existing patterns in `RoomAllocationService`, `ApplicationService`
- **Error Handling:** Return meaningful HTTP status codes, wrap validation errors in DTO
- **Logging:** Add INFO logs for allocation actions, WARN for constraint bypasses
