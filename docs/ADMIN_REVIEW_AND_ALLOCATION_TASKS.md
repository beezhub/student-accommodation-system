# Admin Application Review & Allocation System - Implementation Tasks

**Created:** January 20, 2026  
**Status:** Ready for Implementation  
**Total Tasks:** 40  
**Phases:** 10 (Database → Testing → Documentation → Deployment)

---

## Phase 1: Database Foundation

### Task 1: Database Migrations - Core Tables
**Status:** Not Started  
**Priority:** 🔴 Critical (Blocking)  
**Dependencies:** None  
**Estimated Time:** 2-3 hours

**Description:**
Create migration for document comments, system settings, and building preference support.

**Migration Files Required:**
- `document_comments` table: Store admin comments on rejected documents visible to students
  - Columns: `id`, `document_id` (FK), `admin_id` (FK), `comment_text`, `created_at`
  - Index: `document_id`, `admin_id`

- `system_settings` table: Feature flags and configuration (including auto-allocation toggle)
  - Columns: `id`, `setting_key` (UNIQUE), `setting_value`, `description`, `created_at`, `updated_at`
  - Default records: `auto_allocation_enabled = false`

- Alter `applications` table: Add `preferred_building_id` column
  - Columns: Add `preferred_building_id` (FK to buildings, nullable), `allocation_method` (ENUM: AUTO/MANUAL)

**Location:** `backend/src/main/resources/db/migration/`

**Files to Create:**
- `V2026_01_20_1000__add_document_comments_system_settings.sql`
- `V2026_01_20_1100__alter_applications_add_building_preference.sql`

**Verification:**
- Run migrations successfully
- Test rollback functionality
- Verify schema in database client

---

### Task 2: Database Migrations - Allocation System
**Status:** Not Started  
**Priority:** 🔴 Critical (Blocking)  
**Dependencies:** Task 1  
**Estimated Time:** 3-4 hours

**Description:**
Create comprehensive allocation system schema supporting configurable rules, building/room attributes, staged allocations, and audit logging.

**Migration Files Required:**

1. **allocation_rules** table: Per-building weighted scoring configuration
   ```sql
   Columns: id, building_id (FK, nullable), rule_name, 
            preference_match_weight, desirability_weight, 
            accessibility_weight, active, priority, 
            created_at, updated_at, created_by (FK to users)
   Unique Constraint: (building_id, rule_name)
   ```

2. **building_attributes** table: Building-level restrictions and metadata
   ```sql
   Columns: id, building_id (FK, UNIQUE), gender_restriction (ENUM), 
            accessibility_features (TEXT/JSON), desirability_score (INT 1-10), 
            allow_mixed_gender (BOOLEAN), created_at, updated_at
   ```

3. **room_attributes** table: Room-level overrides and restrictions
   ```sql
   Columns: id, room_id (FK, UNIQUE), accessibility_compliant (BOOLEAN), 
            gender_restriction (ENUM), desirability_override (INT 1-10, nullable), 
            created_at, updated_at
   ```

4. **staged_allocations** table: Batch allocation proposals awaiting admin review
   ```sql
   Columns: id, student_id (FK), room_id (FK), application_id (FK, nullable), 
            status (ENUM: PROPOSED, CONFIRMED, REJECTED), score (DECIMAL), 
            warnings (JSON array), proposed_by (FK to users), batch_id (VARCHAR), 
            created_at, updated_at, confirmed_at
   Indexes: batch_id, status, student_id
   ```

5. **allocation_audit_log** table: Compliance audit trail for all allocation changes
   ```sql
   Columns: id, allocation_id (FK), action_type (ENUM: MOVE, OVERRIDE, DELETE, FORCE_ALLOCATE), 
            admin_user_id (FK), reason (TEXT), old_room_id (FK), new_room_id (FK), 
            constraint_bypassed (VARCHAR), created_at
   Indexes: allocation_id, admin_user_id, created_at
   ```

**Location:** `backend/src/main/resources/db/migration/`

**Files to Create:**
- `V2026_01_20_1200__create_allocation_rules_table.sql`
- `V2026_01_20_1300__create_building_room_attributes_tables.sql`
- `V2026_01_20_1400__create_staged_allocations_table.sql`
- `V2026_01_20_1500__create_allocation_audit_log_table.sql`

**Verification:**
- All tables created with correct constraints
- Foreign keys properly configured
- Indexes created for performance
- Test insertion of sample data

---

## Phase 2: Backend Entities & Services

### Task 3: Backend Entities - Document Comments
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** Task 1  
**Estimated Time:** 1-2 hours

**Description:**
Create JPA entities, repositories, and DTOs for document comment system.

**Files to Create:**
- `backend/src/main/java/com/beezhub/student_accommodation/model/entity/DocumentComment.java`
  - Entity with JPA annotations
  - Relationships: ManyToOne with Document and AppUser
  - Fields: id, document, admin, commentText, createdAt
  
- `backend/src/main/java/com/beezhub/student_accommodation/repository/DocumentCommentRepository.java`
  - JpaRepository interface
  - Methods: `findByDocument_Id(Long)`, `findByAdmin_Id(Long)`, `findByDocument_IdOrderByCreatedAtDesc(Long)`

- `backend/src/main/java/com/beezhub/student_accommodation/model/dto/DocumentCommentRequest.java`
  - Fields: commentText (required, min 10 chars)

- `backend/src/main/java/com/beezhub/student_accommodation/model/dto/DocumentCommentResponse.java`
  - Fields: id, documentId, adminName, commentText, createdAt

**Location:** `backend/src/main/java/com/beezhub/student_accommodation/`

**Verification:**
- Entities compile without errors
- Repositories auto-generated correctly
- Test CRUD operations via repository

---

### Task 4: Backend Entities - Allocation System
**Status:** Not Started  
**Priority:** 🔴 Critical  
**Dependencies:** Task 2  
**Estimated Time:** 3-4 hours

**Description:**
Create JPA entities, repositories, and DTOs for allocation rules, building/room attributes, staged allocations, and audit logging.

**Files to Create:**

1. **AllocationRule** Entity + Repository + DTOs
   - Fields: id, building, ruleName, preferenceMatchWeight, desirabilityWeight, accessibilityWeight, active, priority, createdAt, updatedAt, createdBy

2. **BuildingAttributes** Entity + Repository + DTOs
   - Fields: id, building, genderRestriction (ENUM), accessibilityFeatures (String/JSON), desirabilityScore, allowMixedGender, createdAt, updatedAt

3. **RoomAttributes** Entity + Repository + DTOs
   - Fields: id, room, accessibilityCompliant, genderRestriction (ENUM), desirabilityOverride, createdAt, updatedAt

4. **StagedAllocation** Entity + Repository + DTOs
   - Fields: id, student, room, application, status (ENUM), score, warnings (JSON), proposedBy, batchId, createdAt, updatedAt, confirmedAt
   - DTOs: StagedAllocationRequest, StagedAllocationResponse (with room details, student info, calculated scores)

5. **AllocationAuditLog** Entity + Repository + DTOs
   - Fields: id, allocation, actionType (ENUM), adminUser, reason, oldRoom, newRoom, constraintBypassed, createdAt
   - DTOs: AllocationAuditLogResponse (read-only, for compliance reporting)

**Enums to Create:**
- `GenderRestriction` (NONE, MALE_ONLY, FEMALE_ONLY)
- `AllocationStatus` (PROPOSED, CONFIRMED, REJECTED)
- `AllocationAuditActionType` (MOVE, OVERRIDE, DELETE, FORCE_ALLOCATE)
- `AllocationMethod` (AUTO, MANUAL)

**Location:** `backend/src/main/java/com/beezhub/student_accommodation/model/`

**Verification:**
- All entities compile and serialize to JSON
- Repositories functional
- Test relationships between entities

---

### Task 5: Backend Service - AllocationScoringService
**Status:** Not Started  
**Priority:** 🔴 Critical  
**Dependencies:** Task 4  
**Estimated Time:** 4-5 hours

**Description:**
Implement scoring engine that enforces hard constraints, calculates weighted scores, generates warnings, and applies tie-breaker logic.

**File to Create:**
`backend/src/main/java/com/beezhub/student_accommodation/service/AllocationScoringService.java`

**Key Methods:**
1. `List<ScoredRoom> scoreAvailableRooms(Student, Application, Building preferredBuilding)`
   - Loads latest active AllocationRule for building
   - Scores all available rooms against student/application preferences
   - Returns list sorted by score DESC, application created_at ASC
   - Filters out rooms that fail hard constraints

2. `ScoredRoom scoreSingleRoom(Student, Application, Room)`
   - Calculates score for one student-room pair
   - Returns ScoredRoom DTO with score, warnings, room details

3. `AllocationRule getLatestRuleForBuilding(Long buildingId)`
   - Queries database for active rule with highest priority
   - Falls back to global rule (building_id = null) if none found

4. `boolean validateHardConstraints(Student, Room, boolean allowOverride)`
   - Checks gender compatibility (student.gender vs room.genderRestriction)
   - Checks accessibility compliance (student.specialRequirements vs room.accessibilityCompliant)
   - Returns false if any constraint fails and allowOverride = false

5. `List<String> generateWarnings(Student, Application, Room, AllocationRule)`
   - Returns list of warning strings:
     - "BUILDING_MISMATCH" if application.preferredBuilding != room.building
     - "SUBOPTIMAL_DESIRABILITY" if score < threshold (e.g., 50)
     - "PARTIAL_PREFERENCE_MATCH" if room type doesn't match preference

**Scoring Formula:**
```
score = (building_match_bonus × preference_match_weight) +
        (desirability_score × desirability_weight) +
        (accessibility_match_bonus × accessibility_weight)

Where:
- building_match_bonus = 100 if preferred, 50 if alternative, 0 if not preferred
- accessibility_match_bonus = 100 if accessible and required, 0 if required but not accessible
- desirability_score = room or building desirability_score (1-10)
```

**Location:** `backend/src/main/java/com/beezhub/student_accommodation/service/`

**Verification:**
- Unit tests for scoring logic (see Task 29)
- Test hard constraint enforcement
- Test warning generation
- Test tie-breaker (application date order)

---

### Task 6: Backend Service - DocumentService Enhancement
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** Task 3, existing DocumentService  
**Estimated Time:** 2-3 hours

**Description:**
Add document comment functionality to existing DocumentService.

**Enhancements to `backend/src/main/java/com/beezhub/student_accommodation/service/DocumentService.java`:**

**New Methods:**
1. `DocumentCommentResponse addComment(Long documentId, Long adminId, String commentText)`
   - Validates document exists
   - Creates and persists DocumentComment
   - Triggers notification to student
   - Returns comment DTO

2. `List<DocumentCommentResponse> getCommentsByDocument(Long documentId)`
   - Retrieves all comments for document ordered by createdAt DESC
   - Returns list of comment DTOs

3. `void deleteComment(Long commentId, Long adminId)`
   - Validates admin owns comment or is super-admin
   - Deletes comment
   - Logs to audit trail (if needed for compliance)

4. Update `updateDocumentStatus(Long documentId, DocumentStatus newStatus, Long adminId, Optional<String> comments)` 
   - When status changes to REJECTED, auto-create comment if provided
   - Send notification with comment text to student

**Notification Triggers:**
- When admin rejects document with comment → Send email with comment visible to student
- Template: "Document {documentName} requires attention. Admin comment: {comment}"

**Location:** `backend/src/main/java/com/beezhub/student_accommodation/service/`

**Verification:**
- Test comment creation with validation
- Test retrieval with correct ordering
- Test deletion with permission checks
- Test notifications sent on comment creation

---

### Task 7: Backend Service - ApplicationService Enhancement
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** Task 2, existing ApplicationService  
**Estimated Time:** 3-4 hours

**Description:**
Enhance ApplicationService to support building preferences, application status transitions, required document validation, and completion checklist.

**Enhancements:**

1. **Update DTOs:**
   - Add `preferredBuildingId` to `ApplicationRequest` and `ApplicationResponse`
   - Add `allocationMethod` (AUTO/MANUAL) to response

2. **Update Enums:**
   - Add `AWAITING_ALLOCATION` status to `ApplicationStatus` enum
   - Order: PENDING → APPROVED → AWAITING_ALLOCATION → ALLOCATED / REJECTED

3. **New Methods:**

   a. `ApplicationResponse createApplication(ApplicationRequest request, Long studentId)`
      - Existing logic + validate and store preferredBuildingId
      - Ensure building exists if provided

   b. `RequiredDocumentsChecklistResponse getRequiredDocumentsChecklist(Long applicationId)`
      - Query `document_type` table for all `is_required = true` records
      - Cross-check against student's documents with status
      - Return DTO:
        ```java
        {
          applicationId: Long,
          totalRequired: Int,
          approved: Int,
          pending: Int,
          rejected: Int,
          documents: [
            { type: String, status: String, uploadedAt: Timestamp }
          ],
          allApproved: Boolean
        }
        ```

   c. `void moveToAllocationPhase(Long applicationId, Long adminId)`
      - Validate application status = APPROVED
      - Validate all required documents approved
      - Update status to AWAITING_ALLOCATION
      - Log action

   d. `ApplicationResponse getApplicationFullDetails(Long applicationId)`
      - Return application with nested student profile, documents with comments, checklist, preferred building details
      - For admin use only (@PreAuthorize("hasRole('ADMIN')"))

4. **Update Existing Methods:**
   - `updateApplicationStatus()` → Add validation: can only move to AWAITING_ALLOCATION if documents complete

**Location:** `backend/src/main/java/com/beezhub/student_accommodation/service/`

**Verification:**
- Test application creation with building preference
- Test required document checklist calculation
- Test phase transition validation
- Test full details retrieval with nested data

---

### Task 8: Backend Service - RoomAllocationService Refactor
**Status:** Not Started  
**Priority:** 🔴 Critical  
**Dependencies:** Task 5, existing RoomAllocationService  
**Estimated Time:** 5-6 hours

**Description:**
Refactor RoomAllocationService to replace FIFO logic with scoring-based allocation, implement staged workflow, move functionality, and override capabilities. Disable auto-allocation trigger.

**Major Changes:**

1. **Disable Auto-Allocation Listener:**
   - Remove or comment out `RoomAllocationDocumentListener` event handler
   - Ensure document status changes no longer trigger automatic allocation

2. **Replace FIFO with Scoring-Based Allocation:**
   - Replace `findFirstAvailableRoom()` method call with `AllocationScoringService.scoreAvailableRooms()`
   - Update `allocateFromWaitingList()` to:
     - Query all waiting students
     - Score each against available rooms
     - Select highest-scoring pair
     - Allocate and remove from waiting list

3. **New Methods:**

   a. `BatchAllocationProposalResponse proposeAllocation(BatchAllocationRequest request, Long adminId)`
      - Params: `{ studentIds: List<Long>, batchId: String }`
      - Validates: Max 50 students, all AWAITING_ALLOCATION status
      - For each student:
        - Get latest approved application
        - Score against available rooms
        - Create StagedAllocation record with status PROPOSED
        - Add to response list
      - Returns: `{ batchId, proposals: List<StagedAllocationResponse>, warnings: List<String> }`
      - Sends student notifications: "Your allocation has been proposed. Review in your portal."

   b. `void confirmAllocation(Long stagedAllocationId, Long adminId)`
      - Validates: StagedAllocation.status = PROPOSED
      - Validates: Room still has capacity
      - Creates RoomAllocation with status ACTIVE, allocation_method = AUTO
      - Updates StagedAllocation.status = CONFIRMED, confirmed_at = now
      - Updates Application.status = ALLOCATED
      - Logs to AllocationAuditLog with action FORCE_ALLOCATE
      - Sends notification: "Your room allocation has been confirmed."

   c. `void rejectAllocation(Long stagedAllocationId, String reason, Long adminId)`
      - Updates StagedAllocation.status = REJECTED
      - Keeps student in waiting list (or returns to AWAITING_ALLOCATION)
      - Sends notification: "Your proposed allocation was not confirmed. Reason: {reason}"

   d. `void confirmBatchAllocations(String batchId, Long adminId)`
      - Finds all StagedAllocation records with this batchId and status PROPOSED
      - Confirms each (calls confirmAllocation for each)
      - Logs bulk action

   e. `void moveStudent(Long allocationId, Long newRoomId, String reason, Long adminId)`
      - Validates: Allocation.status = ACTIVE
      - Validates: newRoom has capacity and meets hard constraints
      - Gets old room for audit log
      - Updates allocation: room_id = newRoomId, updated_at = now
      - Logs to AllocationAuditLog with action MOVE, reason captured
      - Sends notification: "You have been moved from {oldRoom} to {newRoom}. Reason: {reason}"

   f. `void overrideConstraints(Long studentId, Long roomId, String reason, Long adminId, List<String> constraintsBypassed)`
      - Validates: Student AWAITING_ALLOCATION, room has capacity
      - Creates StagedAllocation (bypassing validateHardConstraints check)
      - Logs to AllocationAuditLog with action OVERRIDE, constraintBypassed = "{list}"
      - Sends proposal notification

   g. `List<WaitingListEntryResponse> getWaitingListWithScores(Long buildingId)`
      - Queries waiting_list_entry records
      - For each waiting student, scores against available rooms in buildingId
      - Returns list sorted by score DESC:
        ```java
        {
          studentId, applicationId, studentName, 
          priorityScore, buildingPreference, 
          createdAt, position
        }
        ```

4. **Update Existing Methods:**
   - `allocate()` → No longer called automatically; now manual via proposeAllocation
   - Keep `checkAvailability()`, room capacity checks, etc.

**Location:** `backend/src/main/java/com/beezhub/student_accommodation/service/`

**Verification:**
- Unit tests for all new methods (see Task 30)
- Integration tests with scoring service
- Test batch limits (max 50)
- Test constraint override audit logging
- Test waiting list re-scoring

---

### Task 9: Backend Controller - DocumentController Enhancement
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** Task 6  
**Estimated Time:** 2-3 hours

**Description:**
Add document comment endpoints to DocumentController.

**Enhancements to `backend/src/main/java/com/beezhub/student_accommodation/controller/DocumentController.java`:**

**New Endpoints:**

1. `POST /api/documents/{documentId}/comments`
   - Request: `DocumentCommentRequest { commentText: String }`
   - Response: `DocumentCommentResponse`
   - Security: ROLE_ADMIN required
   - Calls: `DocumentService.addComment()`

2. `GET /api/documents/{documentId}/comments`
   - Response: `List<DocumentCommentResponse>`
   - Security: ROLE_ADMIN or document owner (student)
   - Calls: `DocumentService.getCommentsByDocument()`

3. `DELETE /api/documents/comments/{commentId}`
   - Security: ROLE_ADMIN or comment creator required
   - Calls: `DocumentService.deleteComment()`

**Update Existing Endpoints:**

1. Extend `PUT /api/documents/update-status/{documentId}`
   - Add optional field: `rejectionComment?: String`
   - If status = REJECTED and rejectionComment provided, auto-create comment

**Error Handling:**
- 404 if document not found
- 403 if user lacks permission
- 400 if validation fails

**Location:** `backend/src/main/java/com/beezhub/student_accommodation/controller/`

**Verification:**
- Test all endpoints with valid/invalid inputs
- Test permission-based access control
- Test integration with DocumentService

---

### Task 10: Backend Controller - ApplicationController Enhancement
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** Task 7, existing ApplicationController  
**Estimated Time:** 3-4 hours

**Description:**
Add endpoints for application full details, review progress saving, and allocation phase transition.

**Enhancements to `backend/src/main/java/com/beezhub/student_accommodation/controller/ApplicationController.java`:**

**New Endpoints:**

1. `GET /api/applications/{applicationId}/full-details`
   - Response: `ApplicationFullDetailsResponse` including:
     - Application data (id, code, status, dates, preferences, preferredBuildingId)
     - Student profile (name, email, gender, special requirements)
     - Documents array with comments nested:
       ```json
       {
         id, fileName, type, status, uploadedAt,
         comments: [
           { id, adminName, text, createdAt }
         ]
       }
       ```
     - Required documents checklist
     - Allocation eligibility status
   - Security: ROLE_ADMIN or application owner (student)
   - Returns: `ApplicationFullDetailsResponse` DTO

2. `POST /api/applications/{applicationId}/save-review-progress`
   - Request: `ReviewProgressRequest` with partial updates:
     ```json
     {
       documentUpdates: [
         { documentId, newStatus, comment }
       ]
     }
     ```
   - Updates document statuses and comments atomically
   - Does NOT complete the review or move to next phase
   - Response: `{ success: Boolean, message: String }`
   - Security: ROLE_ADMIN required
   - Calls: `ApplicationService.saveReviewProgress()` (new method in ApplicationService)

3. `POST /api/applications/{applicationId}/move-to-allocation`
   - Validates: All required documents approved
   - Updates Application.status = AWAITING_ALLOCATION
   - Response: `ApplicationResponse` with new status
   - Security: ROLE_ADMIN required
   - Calls: `ApplicationService.moveToAllocationPhase()`

**Update Existing Endpoints:**

1. Extend `GET /api/applications?page=0&size=20`
   - Add optional fields to response: `allocationStatus`, `preferredBuildingId`, `documentCompletionPercent`

**Location:** `backend/src/main/java/com/beezhub/student_accommodation/controller/`

**Verification:**
- Test full details retrieval with nested relationships
- Test save progress without completing review
- Test phase transition with validation
- Test permission-based access control

---

### Task 11: Backend Controller - AllocationRuleController
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** Task 4, new AllocationRuleService  
**Estimated Time:** 2-3 hours

**Description:**
Create new controller for allocation rule management (admin only).

**File to Create:**
`backend/src/main/java/com/beezhub/student_accommodation/controller/AllocationRuleController.java`

**Endpoints:**

1. `GET /api/admin/allocation-rules?buildingId={id}`
   - Lists all rules for building or global rules (buildingId = null)
   - Paged response
   - Returns: `List<AllocationRuleResponse>`

2. `GET /api/admin/allocation-rules/{ruleId}`
   - Gets single rule
   - Returns: `AllocationRuleResponse`

3. `POST /api/admin/allocation-rules`
   - Request: `AllocationRuleRequest { buildingId, ruleName, weights, priority }`
   - Validates: weights sum to 100%
   - Creates rule with status active
   - Returns: `AllocationRuleResponse`

4. `PUT /api/admin/allocation-rules/{ruleId}`
   - Request: `AllocationRuleRequest` (same as POST)
   - Updates rule fields
   - Returns: `AllocationRuleResponse`

5. `DELETE /api/admin/allocation-rules/{ruleId}`
   - Soft delete: Sets active = false
   - Returns: `{ success: Boolean }`

6. `PATCH /api/admin/allocation-rules/{ruleId}/activate`
   - Sets active = true
   - Returns: `AllocationRuleResponse`

**Security:** All endpoints require `@PreAuthorize("hasRole('ADMIN')")`

**Error Handling:**
- 400 if weights don't sum to 100%
- 404 if rule/building not found
- 409 if rule name duplicate for building

**Location:** `backend/src/main/java/com/beezhub/student_accommodation/controller/`

**Service Required:**
Create `backend/src/main/java/com/beezhub/student_accommodation/service/AllocationRuleService.java` with CRUD methods.

**Verification:**
- Test CRUD operations
- Test weight validation (sum to 100%)
- Test uniqueness constraint (buildingId + ruleName)
- Test activation/deactivation

---

### Task 12: Backend Controller - BuildingAttributesController
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** Task 4, new BuildingAttributesService  
**Estimated Time:** 2-3 hours

**Description:**
Create new controller for building and room attributes management (admin only).

**File to Create:**
`backend/src/main/java/com/beezhub/student_accommodation/controller/BuildingAttributesController.java`

**Endpoints:**

1. `GET /api/admin/buildings/{buildingId}/attributes`
   - Gets building attributes
   - Returns: `BuildingAttributesResponse`

2. `PUT /api/admin/buildings/{buildingId}/attributes`
   - Request: `BuildingAttributesRequest { genderRestriction, accessibilityFeatures, desirabilityScore, allowMixedGender }`
   - Updates or creates attributes
   - Returns: `BuildingAttributesResponse`

3. `GET /api/admin/buildings/{buildingId}/rooms/attributes`
   - Lists all room attributes for building
   - Returns: `List<RoomAttributesResponse>`

4. `PUT /api/admin/rooms/{roomId}/attributes`
   - Request: `RoomAttributesRequest { accessibilityCompliant, genderRestriction, desirabilityOverride }`
   - Updates or creates room attributes
   - Returns: `RoomAttributesResponse`

5. `GET /api/admin/rooms/{roomId}/attributes`
   - Gets room attributes
   - Returns: `RoomAttributesResponse`

**Security:** All endpoints require `@PreAuthorize("hasRole('ADMIN')")`

**Error Handling:**
- 404 if building/room not found
- 400 if invalid enum values

**Location:** `backend/src/main/java/com/beezhub/student_accommodation/controller/`

**Service Required:**
Create `backend/src/main/java/com/beezhub/student_accommodation/service/BuildingAttributesService.java` with CRUD methods.

**Verification:**
- Test update operations for buildings and rooms
- Test GET retrieval
- Test enum validation (gender restrictions)
- Test creation of new attributes if not exists

---

### Task 13: Backend Controller - RoomAllocationController Enhancement
**Status:** Not Started  
**Priority:** 🔴 Critical  
**Dependencies:** Task 8  
**Estimated Time:** 4-5 hours

**Description:**
Extend existing RoomAllocationController with staged allocation, move, override, and waiting list endpoints.

**Enhancements to `backend/src/main/java/com/beezhub/student_accommodation/controller/RoomAllocationController.java`:**

**New Endpoints:**

1. `POST /api/admin/allocations/propose`
   - Request: `{ studentIds: List<Long>, batchId: String }`
   - Response: `BatchAllocationProposalResponse` with list of scored proposals
   - Security: ROLE_ADMIN required
   - Rate limit: Max 1 request per 10 seconds (prevent abuse)
   - Calls: `RoomAllocationService.proposeAllocation()`

2. `GET /api/admin/allocations/staged?batchId={id}&status={status}&page=0&size=50`
   - Lists staged allocations for batch and status
   - Response: `Page<StagedAllocationResponse>`
   - Shows scores, warnings, proposed room details

3. `PATCH /api/admin/allocations/staged/{stagedAllocationId}/confirm`
   - Confirms single staged allocation
   - Calls: `RoomAllocationService.confirmAllocation()`
   - Returns: `RoomAllocationResponse`

4. `PATCH /api/admin/allocations/staged/{stagedAllocationId}/reject`
   - Request: `{ reason: String }`
   - Calls: `RoomAllocationService.rejectAllocation()`
   - Returns: `{ success: Boolean }`

5. `PATCH /api/admin/allocations/staged/batch/{batchId}/confirm`
   - Bulk confirm all proposals in batch
   - Returns: `{ success: Boolean, confirmed: Int, failed: Int }`
   - Calls: `RoomAllocationService.confirmBatchAllocations()`

6. `POST /api/admin/allocations/{allocationId}/move`
   - Request: `{ newRoomId: Long, reason: String }`
   - Calls: `RoomAllocationService.moveStudent()`
   - Returns: `RoomAllocationResponse`

7. `POST /api/admin/allocations/override`
   - Request: `{ studentId: Long, roomId: Long, reason: String, bypassConstraints: List<String> }`
   - Bypasses hard constraints with reason
   - Returns: `StagedAllocationResponse` with bypass warning
   - Calls: `RoomAllocationService.overrideConstraints()`

8. `GET /api/admin/waiting-list?buildingId={id}&sortBy=score`
   - Lists waiting list with priority scores
   - Response: `List<WaitingListEntryResponse>` sorted by score DESC
   - Calls: `RoomAllocationService.getWaitingListWithScores()`

9. `POST /api/admin/waiting-list/rescore`
   - Rescores all waiting list entries using latest rules
   - Returns: `{ success: Boolean, rescored: Int }`
   - Async operation (consider Job/Scheduler)

**Update Existing Endpoints:**

1. `GET /api/room-allocation`
   - Add optional filters: `allocationMethod=AUTO|MANUAL`, `status=ACTIVE|COMPLETED`
   - Include `allocationMethod` in response

**Error Handling:**
- 400 if batch exceeds 50 students
- 409 if room no longer available
- 422 if hard constraints violated (unless override)
- Rate limit error (429) if proposal endpoint abused

**Location:** `backend/src/main/java/com/beezhub/student_accommodation/controller/`

**Verification:**
- Test all endpoints with batch size validation
- Test constraint bypass with audit logging
- Test rate limiting on propose endpoint
- Integration test: propose → confirm → active allocation flow

---

### Task 14: Email Templates & Notifications
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** Task 8, existing NotificationService  
**Estimated Time:** 3-4 hours

**Description:**
Create email templates and update NotificationService with allocation-related notifications.

**Email Templates to Create:**
Location: `backend/src/main/resources/templates/email/`

1. **allocation-proposal.html** (Thymeleaf)
   ```
   Subject: "Room Allocation Proposal"
   Body:
   - "Your accommodation has been proposed"
   - Room details: building, room number, floor
   - Scores and warnings displayed
   - "This proposal is pending confirmation. You will be notified once confirmed."
   - Link to student portal to view details
   ```

2. **allocation-confirmed.html**
   ```
   Subject: "Room Allocation Confirmed"
   Body:
   - "Your room allocation has been confirmed"
   - Final room assignment details
   - Move-in date from application
   - Next steps and contact information
   ```

3. **allocation-rejected.html**
   ```
   Subject: "Room Allocation Update"
   Body:
   - "Your proposed allocation was not confirmed"
   - Optional: "Reason: {reason}"
   - "You remain on the waiting list. We will notify you of next steps."
   ```

4. **allocation-moved.html**
   ```
   Subject: "Room Change Notification"
   Body:
   - "Your room has been changed"
   - Old room and new room details
   - Effective date
   - "Reason for change: {reason}"
   - Contact admin if questions
   ```

5. **document-comment.html** (Update if creating new)
   ```
   Subject: "Document Review: Action Required"
   Body:
   - "A comment was left on your document: {documentName}"
   - Comment text displayed
   - "Please review and resubmit if necessary"
   - Link to document in student portal
   ```

**NotificationService Enhancements:**
Location: `backend/src/main/java/com/beezhub/student_accommodation/service/NotificationService.java`

**New Methods:**

1. `void sendAllocationProposal(StagedAllocation proposal)`
   - Sends email using allocation-proposal.html template
   - Sends in-app notification
   - Template variables: buildingName, roomNumber, score, warnings, moveInDate

2. `void sendAllocationConfirmed(RoomAllocation allocation)`
   - Sends email using allocation-confirmed.html template
   - Template variables: buildingName, roomNumber, floor, moveInDate, contactInfo

3. `void sendAllocationRejected(Student student, StagedAllocation proposal, String reason)`
   - Sends email using allocation-rejected.html template
   - Template variables: reason (optional), nextSteps

4. `void sendAllocationMoved(RoomAllocation allocation, Room oldRoom, String reason)`
   - Sends email using allocation-moved.html template
   - Template variables: oldBuildingRoom, newBuildingRoom, reason, effectiveDate

5. `void sendDocumentComment(Document document, DocumentComment comment)`
   - Sends email using document-comment.html template
   - Template variables: documentName, commentText, documentLink

**In-App Notification System:**
If using existing in-app notification system:
- Create records in notification/message table
- Link to User/Student
- Mark as unread
- Fetch via notification endpoint

**Verification:**
- Test template rendering with sample data
- Test email delivery to student email
- Test in-app notification creation
- Verify email contains required information

---

## Phase 3: Frontend Models & Services

### Task 15: Frontend Model & Services - Allocation DTOs
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** None (parallel to backend)  
**Estimated Time:** 2-3 hours

**Description:**
Create TypeScript interfaces and Angular services for allocation system front-end data handling.

**Files to Create:**

1. **Models (DTOs):** `frontend/src/app/model/`

   - `document-comment.model.ts`
     ```typescript
     export interface DocumentComment {
       id: number;
       documentId: number;
       adminName: string;
       commentText: string;
       createdAt: Date;
     }
     ```

   - `allocation-rule.model.ts`
     ```typescript
     export interface AllocationRule {
       id: number;
       buildingId?: number;
       ruleName: string;
       preferenceMatchWeight: number;
       desirabilityWeight: number;
       accessibilityWeight: number;
       active: boolean;
       priority: number;
       createdAt: Date;
     }
     ```

   - `building-attributes.model.ts`
     ```typescript
     export interface BuildingAttributes {
       id: number;
       buildingId: number;
       genderRestriction: 'NONE' | 'MALE_ONLY' | 'FEMALE_ONLY';
       accessibilityFeatures: string;
       desirabilityScore: number; // 1-10
       allowMixedGender: boolean;
     }
     ```

   - `room-attributes.model.ts`
     ```typescript
     export interface RoomAttributes {
       id: number;
       roomId: number;
       accessibilityCompliant: boolean;
       genderRestriction: 'NONE' | 'MALE_ONLY' | 'FEMALE_ONLY';
       desirabilityOverride?: number;
     }
     ```

   - `staged-allocation.model.ts`
     ```typescript
     export interface StagedAllocation {
       id: number;
       studentId: number;
       roomId: number;
       applicationId?: number;
       status: 'PROPOSED' | 'CONFIRMED' | 'REJECTED';
       score: number;
       warnings: string[];
       proposedBy: number;
       batchId: string;
       roomDetails: RoomDetail;
       studentName: string;
       createdAt: Date;
     }
     ```

   - `allocation-audit-log.model.ts`
     ```typescript
     export interface AllocationAuditLog {
       id: number;
       allocationId: number;
       actionType: 'MOVE' | 'OVERRIDE' | 'DELETE' | 'FORCE_ALLOCATE';
       adminUserName: string;
       reason: string;
       oldRoomId?: number;
       newRoomId?: number;
       constraintBypassed?: string;
       createdAt: Date;
     }
     ```

   - `waiting-list-entry.model.ts`
     ```typescript
     export interface WaitingListEntry {
       studentId: number;
       applicationId: number;
       studentName: string;
       priorityScore: number;
       buildingPreference: string;
       createdAt: Date;
       position: number;
     }
     ```

2. **Services:** `frontend/src/app/core/services/`

   - `allocation-rule.service.ts`
     ```typescript
     @Injectable({ providedIn: 'root' })
     export class AllocationRuleService {
       constructor(private http: HttpClient) {}
       
       getRulesForBuilding(buildingId: number): Observable<AllocationRule[]> {}
       getRule(ruleId: number): Observable<AllocationRule> {}
       createRule(rule: Partial<AllocationRule>): Observable<AllocationRule> {}
       updateRule(ruleId: number, rule: Partial<AllocationRule>): Observable<AllocationRule> {}
       deleteRule(ruleId: number): Observable<{ success: boolean }> {}
       activateRule(ruleId: number): Observable<AllocationRule> {}
     }
     ```

   - `building-attributes.service.ts`
     ```typescript
     @Injectable({ providedIn: 'root' })
     export class BuildingAttributesService {
       constructor(private http: HttpClient) {}
       
       getAttributes(buildingId: number): Observable<BuildingAttributes> {}
       updateAttributes(buildingId: number, attrs: Partial<BuildingAttributes>): Observable<BuildingAttributes> {}
       getRoomAttributes(buildingId: number): Observable<RoomAttributes[]> {}
       getRoomAttribute(roomId: number): Observable<RoomAttributes> {}
       updateRoomAttribute(roomId: number, attrs: Partial<RoomAttributes>): Observable<RoomAttributes> {}
     }
     ```

   - `staged-allocation.service.ts`
     ```typescript
     @Injectable({ providedIn: 'root' })
     export class StagedAllocationService {
       constructor(private http: HttpClient) {}
       
       proposeAllocation(studentIds: number[], batchId: string): Observable<BatchAllocationProposal> {}
       getProposals(batchId: string, status?: string, page?: number): Observable<Page<StagedAllocation>> {}
       confirmAllocation(stagedId: number): Observable<RoomAllocation> {}
       rejectAllocation(stagedId: number, reason: string): Observable<{ success: boolean }> {}
       confirmBatch(batchId: string): Observable<{ success: boolean; confirmed: number; failed: number }> {}
       moveStudent(allocationId: number, newRoomId: number, reason: string): Observable<RoomAllocation> {}
       overrideConstraints(studentId: number, roomId: number, reason: string, bypass: string[]): Observable<StagedAllocation> {}
       getWaitingList(buildingId?: number): Observable<WaitingListEntry[]> {}
       rescoreWaitingList(): Observable<{ success: boolean; rescored: number }> {}
     }
     ```

   - `document-comment.service.ts`
     ```typescript
     @Injectable({ providedIn: 'root' })
     export class DocumentCommentService {
       constructor(private http: HttpClient) {}
       
       addComment(documentId: number, text: string): Observable<DocumentComment> {}
       getComments(documentId: number): Observable<DocumentComment[]> {}
       deleteComment(commentId: number): Observable<{ success: boolean }> {}
     }
     ```

**Location:** `frontend/src/app/model/` and `frontend/src/app/core/services/`

**Verification:**
- Models match backend DTOs
- Services compile and have correct method signatures
- HTTP methods (GET/POST/PUT/DELETE) correctly configured

---

## Phase 4: Student-Facing Features

### Task 16: Frontend - Student Application Form Enhancement
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** Task 15, existing ApplicationCreateComponent  
**Estimated Time:** 2-3 hours

**Description:**
Add building preference selector to student application form and ensure all preferences visible.

**Enhancements to Application Create/Update Components:**
Location: `frontend/src/app/features/student/application-create/`

**Changes:**

1. **Add Building Selector:**
   - Inject `BuildingService` to fetch all buildings
   - Add FormControl: `preferredBuilding` (optional, nullable)
   - Add Material dropdown field in template:
     ```html
     <mat-form-field class="full-width">
       <mat-label>Preferred Building (Optional)</mat-label>
       <mat-select formControlName="preferredBuilding">
         <mat-option>None</mat-option>
         <mat-optgroup *ngFor="let group of buildingGroups" [label]="group.label">
           <mat-option *ngFor="let building of group.buildings" [value]="building.id">
             {{building.name}}
           </mat-option>
         </mat-optgroup>
       </mat-select>
     </mat-form-field>
     ```

2. **Ensure All Preferences Visible:**
   - Verify `specialRequirements` textarea is visible and clearly labeled
   - Verify `preferredRoomType` dropdown visible with options
   - Verify `preferredAccommodationType` dropdown visible
   - Verify `preferredMoveInDate` date picker visible
   - Verify `preferredDurationMonths` number input visible

3. **Form Submission:**
   - Collect `preferredBuildingId` when form submitted
   - Include in `ApplicationRequest` payload
   - Send to backend

4. **Validation:**
   - All existing validations remain
   - Building preference is optional (allow null)

**Location:** `frontend/src/app/features/student/application-create/application-create.component.ts` and `.html`

**Verification:**
- Building dropdown loads correctly
- Form submits with preferredBuildingId
- Form still works if building not selected (null handling)
- All other preference fields visible and functional

---

## Phase 5: Admin Application Review

### Task 17: Frontend - Document Comment Component
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** Task 15, DocumentCommentService  
**Estimated Time:** 2-3 hours

**Description:**
Create document comment component for admin rejection comments and student visibility.

**File to Create:**
`frontend/src/app/features/shared/components/document-comment-section/document-comment-section.component.ts`

**Component Features:**

1. **Display Comments:**
   - List of existing comments from DocumentCommentService
   - Show: admin name, comment text, created date
   - Timestamp formatted as relative time (e.g., "2 hours ago")

2. **Add Comment (Admin Only):**
   - Textarea for comment input (min 10 chars, max 500)
   - Submit button (disabled until text entered)
   - Shows: "Leave a comment" placeholder
   - If admin rejects document, pre-fill with generic rejection message (customizable)

3. **Visual Design:**
   - Comments displayed in chronological order (newest at bottom)
   - Admin comments highlighted differently (e.g., light red background for rejections)
   - Responsive layout (fits in modal/dialog)

4. **Usage:**
   - Use in [application-detail-dialog.component.ts] (Task 18)
   - Use in student document view (student portal)

**Template Structure:**
```html
<div class="comment-section">
  <h3>Comments</h3>
  
  <div *ngIf="comments.length === 0" class="no-comments">
    No comments yet
  </div>
  
  <div *ngFor="let comment of comments" class="comment">
    <div class="comment-header">
      <strong>{{comment.adminName}}</strong>
      <span class="timestamp">{{comment.createdAt | timeago}}</span>
    </div>
    <p class="comment-text">{{comment.commentText}}</p>
  </div>
  
  <div *ngIf="isAdmin" class="add-comment">
    <textarea 
      placeholder="Leave a comment..."
      [formControl]="commentControl"
      rows="4"></textarea>
    <button mat-raised-button (click)="submitComment()">Submit</button>
  </div>
</div>
```

**Location:** `frontend/src/app/features/shared/components/document-comment-section/`

**Verification:**
- Comments load and display correctly
- Admin can add comments
- Students can view but not add comments
- Form validation works (min length)
- Timestamps display in human-readable format

---

### Task 18: Frontend - Application Detail Dialog
**Status:** Not Started  
**Priority:** 🔴 Critical  
**Dependencies:** Task 17, ApplicationService  
**Estimated Time:** 5-6 hours

**Description:**
Create comprehensive application detail dialog showing student profile, all preferences, documents with comments, required document checklist, save progress, and move-to-allocation button.

**File to Create:**
`frontend/src/app/features/admin/application-review/application-detail-dialog/application-detail-dialog.component.ts`

**Component Structure:**

1. **Header Section:**
   - Application code, student name, email
   - Application date, current status badge

2. **Student Profile Section:**
   - Name, email, phone, date of birth, gender
   - Special requirements (if any)
   - Institution and year of study
   - Link to full student profile (if available)

3. **Application Preferences Section:**
   - Accommodation type, room type, building preference (if selected)
   - Move-in date, duration (months)
   - Special requirements (again for emphasis)

4. **Documents Review Section:**
   - Table/card view of all documents:
     - Document name, type, status badge
     - Upload date
     - "Download" link
     - Inline comment section (from Task 17 component)
     - Reject/Approve buttons (if PENDING)
   - "Save Progress" button at bottom (saves all pending changes without completing review)

5. **Required Document Checklist:**
   - Visual indicator (pie chart or progress bar)
   - List of all required document types
   - Checkmark if approved, X if pending/rejected
   - Count: "5/7 required documents approved"

6. **Action Buttons:**
   - "Save Progress" (saves document status/comments without moving to allocation)
   - "Move to Allocation" (enabled only if ALL required documents approved)
   - "Reject Application" (sets status to REJECTED)
   - "Close" (cancel without saving)

**Key Features:**

1. **Load Data:**
   - Call `ApplicationService.getApplicationFullDetails(applicationId)`
   - Display nested student, application, documents, checklist

2. **Document Review:**
   - Iterate through documents array
   - For each document, embed DocumentCommentSection component
   - Allow inline status changes (dropdown: PENDING/APPROVED/REJECTED)
   - Update UI immediately on status change

3. **Save Progress:**
   - On "Save Progress" click:
     - Collect all document status changes and comments
     - Call `ApplicationService.saveReviewProgress()` endpoint
     - Show toast: "Progress saved successfully"
     - Don't close dialog, allow continuing review

4. **Move to Allocation:**
   - Validate all required documents approved
   - On click:
     - Call `ApplicationService.moveToAllocationPhase(applicationId)`
     - Update local Application.status to AWAITING_ALLOCATION
     - Show toast: "Application moved to allocation phase"
     - Trigger Task 19 (allocation choice dialog)

5. **Reject Application:**
   - Open small dialog to enter rejection reason
   - Call `ApplicationController.updateApplicationStatus()` with REJECTED status
   - Close detail dialog, return to review table

**Dialog Configuration:**
- Width: 90vw (responsive)
- Max-width: 1000px
- Use Material mat-dialog for modal behavior
- Scrollable content area

**Location:** `frontend/src/app/features/admin/application-review/application-detail-dialog/`

**Services Used:**
- ApplicationService
- DocumentService
- DocumentCommentService

**Verification:**
- Dialog opens with correct application data
- Document comments load and display
- Save progress persists data without closing
- Move to allocation validates requirements
- All buttons functional
- Responsive on different screen sizes

---

### Task 19: Frontend - Allocation Choice Dialog
**Status:** Not Started  
**Priority:** 🔴 Critical  
**Dependencies:** Task 15, StagedAllocationService, system settings check  
**Estimated Time:** 2-3 hours

**Description:**
Create modal dialog allowing admin to choose between auto and manual allocation after application approval. Auto-allocation option conditionally hidden if disabled in system settings.

**File to Create:**
`frontend/src/app/features/admin/application-review/allocation-choice-dialog/allocation-choice-dialog.component.ts`

**Component Features:**

1. **Dialog Layout:**
   - Title: "Choose Allocation Method"
   - Description: "Select how to allocate a room for this student"

2. **Options:**

   a. **Auto Allocate (conditional):**
      - Radio button + description
      - Description: "System will automatically assign an available room based on preferences and configured rules"
      - Hidden if `systemSettings.autoAllocationEnabled === false`

   b. **Manual Allocate:**
      - Radio button + description
      - Description: "You will manually select a room for this student"
      - Always visible

3. **Selection Logic:**
   - If Auto Allocate visible and selected:
     - Call `StagedAllocationService.proposeAllocation([studentId], uniqueBatchId)`
     - Show loading spinner
     - On response: Display proposed room with score/warnings
     - Button: "Confirm" or "Back to Choose Method"
   
   - If Manual Allocate selected:
     - Open room selector component
     - Allow filtering by building, room number, capacity
     - Show room attributes (accessibility, gender restriction)
     - Select room, click "Allocate"

4. **Room Selector (for Manual):**
   - Filter form:
     - Building selector (dropdown)
     - Room number input (optional, search)
     - Filters for accessibility, capacity
   - Results table:
     - Room number, building, capacity, occupancy
     - Attributes (accessibility badge, etc.)
     - "Select" button per row
   - Selected room highlighted/checked

5. **Confirmation:**
   - Show selected allocation summary
   - Button: "Confirm Allocation" (creates RoomAllocation with allocation_method = MANUAL)
   - On success:
     - Close dialog
     - Return to application review table
     - Show toast: "Room allocation completed"

**Check System Settings:**
- Inject SystemSettingsService (or embed in auth service)
- Call endpoint to check `auto_allocation_enabled` setting
- Conditionally render auto allocate option

**Location:** `frontend/src/app/features/admin/application-review/allocation-choice-dialog/`

**Services Used:**
- StagedAllocationService
- RoomService
- SystemSettingsService

**Verification:**
- Auto allocate option hidden when disabled
- Manual allocate always visible
- Room selector filters work correctly
- Allocation confirmed successfully
- allocation_method stored correctly

---

### Task 20: Frontend - Application Review Table Refactor
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** Task 18, existing ApplicationReviewComponent  
**Estimated Time:** 3-4 hours

**Description:**
Update application review table to add Review button triggering detail dialog, display new status badges including AWAITING_ALLOCATION, and show allocation status.

**Enhancements to ApplicationReviewComponent:**
Location: `frontend/src/app/features/admin/application-review/`

**Changes:**

1. **Update Table Columns:**
   - Keep existing: Application Code, Student Name, Email, Application Date
   - Add: Preferred Building (nullable, show "-" if none)
   - Add: Document Status (progress bar: X/Y approved)
   - Add: Allocation Status (badge: PENDING_ALLOCATION / ALLOCATED / REJECTED)
   - Keep: Current Status (PENDING / APPROVED / AWAITING_ALLOCATION / REJECTED)
   - Add: Actions

2. **New Status Badges:**
   - PENDING (gray)
   - APPROVED (blue)
   - AWAITING_ALLOCATION (orange)
   - ALLOCATED (green)
   - REJECTED (red)

3. **Row Actions:**
   - "Review" button → Opens application-detail-dialog (Task 18)
   - "Allocate Now" button (if status = AWAITING_ALLOCATION) → Opens allocation-choice-dialog (Task 19)

4. **Remove Bulk Operations:**
   - Remove bulk select checkboxes for review/allocation (per compliance requirement for case-by-case handling)
   - Keep individual row actions

5. **Update Component Logic:**
   - Modify applicationList to fetch with new columns
   - Map application statuses to badges with correct colors
   - Calculate document completion % and display as progress bar
   - Add methods:
     - `openDetailDialog(applicationId)` → MatDialog.open(ApplicationDetailDialogComponent)
     - `openAllocationDialog(applicationId)` → MatDialog.open(AllocationChoiceDialogComponent)

6. **Filters:**
   - Add filter by: Preferred Building
   - Existing filters remain: status, application code, student name, date range

**Template Updates:**
```html
<!-- Table header update -->
<th>Preferred Building</th>
<th>Document Status</th>
<th>Allocation Status</th>
<th>Actions</th>

<!-- Rows -->
<td>{{app.preferredBuildingName || '-'}}</td>
<td>
  <mat-progress-bar mode="determinate" 
    [value]="app.documentCompletionPercent">
  </mat-progress-bar>
  <small>{{app.approvedDocuments}}/{{app.totalRequiredDocuments}}</small>
</td>
<td>
  <mat-chip [ngClass]="'status-' + app.allocationStatus">
    {{app.allocationStatus}}
  </mat-chip>
</td>
<td>
  <button mat-icon-button (click)="openDetailDialog(app.id)">
    <mat-icon>open_in_new</mat-icon>
  </button>
  <button mat-icon-button 
    [disabled]="app.status !== 'AWAITING_ALLOCATION'"
    (click)="openAllocationDialog(app.id)">
    <mat-icon>assignment</mat-icon>
  </button>
</td>
```

**Location:** `frontend/src/app/features/admin/application-review/application-review.component.ts` and `.html`

**Verification:**
- New columns display correctly
- Review button opens detail dialog
- Allocate Now button opens choice dialog (conditionally)
- Status badges color-coded
- Preferred building displays correctly
- Document progress bar calculates percentage

---

## Phase 6: Allocation Management UI

### Task 21: Frontend - Allocation Rule Management Component
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** Task 15, AllocationRuleService  
**Estimated Time:** 3-4 hours

**Description:**
Create comprehensive allocation rule management interface for admins to configure per-building weighted scoring rules.

**File to Create:**
`frontend/src/app/features/admin/allocation-rule-management/allocation-rule-management.component.ts`

**Component Features:**

1. **Header:**
   - Title: "Allocation Rules Configuration"
   - Building selector dropdown (populated from BuildingService)
   - "Create New Rule" button

2. **Rules List Table:**
   - Columns: Rule Name, Preference Match %, Desirability %, Accessibility %, Priority, Status (Active/Inactive), Actions
   - Fetch from `AllocationRuleService.getRulesForBuilding(buildingId)`
   - Paginated (20 per page)
   - Sortable by priority

3. **Create/Edit Rule Dialog:**
   - Form fields:
     - Rule Name (required, text input)
     - Preference Match Weight slider (0-100)
     - Desirability Weight slider (0-100)
     - Accessibility Weight slider (0-100)
     - Live validation: Weights must sum to 100% (show error if not)
     - Priority input (number, default = 1)
     - Constraint toggles (if applicable):
       - "Gender Restriction Required"
       - "Accessibility Compliance Required"
   - Buttons: "Save", "Cancel"

4. **Rule Actions:**
   - "Edit" button → Opens dialog with rule pre-filled
   - "Delete" button → Soft delete (sets active = false)
   - "Activate" button (if inactive) → Sets active = true

5. **Validation:**
   - Rule name not empty
   - Weights sum to exactly 100%
   - Priority is positive integer
   - Unique rule name per building (validate on server, handle 409 error)

6. **Feedback:**
   - Toast on successful save: "Rule saved successfully"
   - Toast on error: Show backend error message
   - Loader while saving

**Form Data Structure:**
```typescript
interface RuleFormData {
  ruleName: string;
  preferenceMatchWeight: number;
  desirabilityWeight: number;
  accessibilityWeight: number;
  priority: number;
  genderRestrictionRequired?: boolean;
  accessibilityRequired?: boolean;
}
```

**Location:** `frontend/src/app/features/admin/allocation-rule-management/`

**Route:** `/admin/allocation-rules`

**Services Used:**
- AllocationRuleService
- BuildingService

**Verification:**
- Rules load for selected building
- Weight slider validation works (sum to 100%)
- Create/edit/delete operations successful
- Form validation prevents invalid submissions

---

### Task 22: Frontend - Staged Allocation Component
**Status:** Not Started  
**Priority:** 🔴 Critical  
**Dependencies:** Task 15, StagedAllocationService  
**Estimated Time:** 5-6 hours

**Description:**
Create comprehensive staged allocation workflow with 3 steps: student selection (max 50), propose batch, and review table with scoring/warnings.

**File to Create:**
`frontend/src/app/features/admin/staged-allocation/staged-allocation.component.ts`

**Component Structure - 3 Steps:**

**Step 1: Student Selection**
- Multi-select from AWAITING_ALLOCATION applications
- Search by student name or application code
- Selected count display: "X students selected (Max 50)"
- Error if trying to select >50
- Show building preference and special requirements for each student
- Generate unique batch ID (client-side or get from server)
- "Next" button → Move to Step 2

**Step 2: Propose Batch**
- Summary of selected students
- "Propose Allocation" button
- Show loading spinner
- Call `StagedAllocationService.proposeAllocation(studentIds, batchId)`
- On success: Show "Proposals created" and move to Step 3
- On error: Show error message, allow retry or back to Step 1

**Step 3: Review & Confirm**
- Table of proposed allocations:
  - Columns: Student, Application, Proposed Room, Score, Warnings, Actions
  - Room details: Building, Room #, Floor, Capacity
  - Score: Numeric score (e.g., 87.5)
  - Warnings: List of warning badges (BUILDING_MISMATCH, SUBOPTIMAL_DESIRABILITY, etc.)
  - Actions: "Confirm" (individual), "Reject" (individual), delete row

- Bulk actions at bottom:
  - "Confirm All" button → Confirms all remaining proposals
  - "Reject All" button → Opens dialog to reject all with reason
  - "Back to Student Selection" button

- Status indicators:
  - Row color: Gray = PROPOSED, Green = CONFIRMED, Red = REJECTED
  - Confirmed/rejected rows can't be edited

**Features:**

1. **State Management:**
   - Step 1: Store selected student IDs
   - Step 2: Generate batch ID and call API
   - Step 3: Store proposals, allow individual or bulk confirm/reject

2. **Validation:**
   - Max 50 students per proposal
   - All students must be AWAITING_ALLOCATION
   - No duplicate students in selection

3. **Rate Limiting:**
   - Disable "Propose" button for 10 seconds after clicking (or based on backend 429 response)

4. **Undo:**
   - Each step has "Back" button to return to previous step
   - If back from Step 3, proposals remain in memory (don't lose them)

5. **Feedback:**
   - Toast on successful confirmation: "1 allocation confirmed"
   - Toast on error: Show reason

**Location:** `frontend/src/app/features/admin/staged-allocation/`

**Route:** `/admin/allocations/staged`

**Services Used:**
- StagedAllocationService
- ApplicationService
- BuildingService

**Verification:**
- Multi-select works with 50 limit
- Propose batch API called with correct payload
- Proposals display with scores and warnings
- Confirm/reject individual or bulk works
- Back button preserves state

---

### Task 23: Frontend - Allocation Move Dialog
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** Task 15, RoomService, StagedAllocationService  
**Estimated Time:** 3-4 hours

**Description:**
Create modal dialog for moving an active allocation to a different room with validation, reason capture, and audit logging.

**File to Create:**
`frontend/src/app/features/admin/allocation-move-dialog/allocation-move-dialog.component.ts`

**Dialog Features:**

1. **Current Allocation Display:**
   - Student name, application code
   - Current room: Building, Room #, Floor
   - Occupancy dates (move-in, end date if set)

2. **New Room Selection:**
   - Filter form:
     - Building selector (dropdown)
     - Room number search (optional)
     - Capacity filter (show available rooms only)
     - Accessibility filter (checkbox)
   - Results table:
     - Room number, building, floor, capacity, current occupancy
     - Attributes: accessibility badge, gender restriction
     - "Select" button per row
   - Selected room highlighted

3. **Room Compatibility Check:**
   - After room selected, validate:
     - Room has capacity (current_occupancy < capacity)
     - Hard constraints met (gender, accessibility)
   - Show warnings if soft constraints violated (e.g., not preferred building)
   - Display validation results before submit

4. **Reason Textarea:**
   - Required field (min 10 chars, max 500)
   - Placeholder: "Why is this student being moved? (for compliance record)"
   - Show character count

5. **Buttons:**
   - "Confirm Move" (enabled only if room selected + reason entered)
   - "Cancel"

6. **Submission:**
   - Call `StagedAllocationService.moveStudent(allocationId, newRoomId, reason)`
   - Show loading spinner
   - On success: "Student moved successfully" toast, close dialog
   - On error: Show error message

**Location:** `frontend/src/app/features/admin/allocation-move-dialog/`

**Services Used:**
- RoomService
- StagedAllocationService

**Verification:**
- Room selector filters correctly
- Validation prevents invalid moves
- Reason required and validated
- Move API called successfully
- Audit log created on backend

---

### Task 24: Frontend - Building Attributes Management Component
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** Task 15, BuildingAttributesService  
**Estimated Time:** 3-4 hours

**Description:**
Create interface for managing building and room attributes (gender restrictions, accessibility, desirability scores).

**File to Create:**
`frontend/src/app/features/admin/building-attributes-management/building-attributes-management.component.ts`

**Component Features:**

1. **Building Selection:**
   - Dropdown selector
   - List all buildings

2. **Building Attributes Section:**
   - Form fields:
     - Gender Restriction dropdown (NONE, MALE_ONLY, FEMALE_ONLY)
     - Accessibility Features textarea (comma-separated: "wheelchair access, elevator, grab bars")
     - Desirability Score slider (1-10)
     - Allow Mixed Gender toggle (BOOLEAN)
   - "Save" button

3. **Room Attributes Sub-Section:**
   - Inline editable table for building's rooms
   - Columns:
     - Room Number
     - Accessibility Compliant (toggle)
     - Gender Restriction (dropdown)
     - Desirability Override (number input, optional)
     - Actions (Save per row)

4. **Validation:**
   - Desirability score: 1-10 only
   - Gender restriction: Valid enum values only

5. **Feedback:**
   - Toast on save: "Building attributes updated"
   - Toast on error

**Location:** `frontend/src/app/features/admin/building-attributes-management/`

**Route:** `/admin/buildings/:buildingId/attributes`

**Services Used:**
- BuildingAttributesService
- BuildingService

**Verification:**
- Building attributes load and display
- Room attributes load in table
- Updates save correctly
- Validation prevents invalid values

---

### Task 25: Frontend - Waiting List Dashboard Component
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** Task 15, StagedAllocationService  
**Estimated Time:** 3-4 hours

**Description:**
Create dashboard displaying waiting list with priority scores, sortable by score and creation date, with re-score functionality.

**File to Create:**
`frontend/src/app/features/admin/waiting-list-dashboard/waiting-list-dashboard.component.ts`

**Component Features:**

1. **Filters:**
   - Building selector (dropdown, optional)
   - Status filter (PENDING, etc.)
   - Date range picker (optional)
   - Refresh button

2. **Waiting List Table:**
   - Columns:
     - Position (1, 2, 3...)
     - Student Name
     - Application Code
     - Preferred Building
     - Priority Score (numeric, sortable DESC)
     - Created Date (sortable ASC/DESC)
     - Time in Queue (e.g., "5 days")
     - Actions
   - Sortable by: Score (default DESC), Created Date, Time in Queue
   - Paged results (20 per page)

3. **Row Actions:**
   - "View Application" → Opens application-detail-dialog (if permission)
   - "Allocate Now" → Opens allocation-choice-dialog

4. **Bulk Actions:**
   - "Re-score All Waiting List" button
   - Shows: "Rescoring based on latest allocation rules..."
   - On success: "Re-scored X students. Waiting list updated."
   - Refreshes table automatically

5. **Alerts:**
   - Warning banner if rules have changed since last re-score: "Rules have been updated. Re-score waiting list to apply changes."
   - Highlighted rows if their score likely changed (show alert icon)

6. **Data Fetch:**
   - Call `StagedAllocationService.getWaitingList(buildingId)` on load and filter change
   - Display loading spinner while fetching

**Location:** `frontend/src/app/features/admin/waiting-list-dashboard/`

**Route:** `/admin/waiting-list`

**Services Used:**
- StagedAllocationService
- ApplicationService

**Verification:**
- Waiting list loads with scores
- Sorting by score works
- Re-score functionality works
- Alerts display appropriately
- Actions functional

---

### Task 26: Frontend - Extend Application Review UI
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** Tasks 18, 20  
**Estimated Time:** 2-3 hours

**Description:**
Add new columns and features to application review component for enhanced visibility: preferred building, allocation status, allocation score, and quick preview.

**Enhancements:**

1. **Additional Table Columns** (if not already done in Task 20):
   - Preferred Building (display name, "-" if none)
   - Allocation Status (badge: PENDING_ALLOCATION / ALLOCATED)
   - Allocation Score (if waiting: show score, if allocated: show "Allocated")

2. **View Preferences Button:**
   - Mini button per row → Opens small dialog/drawer showing:
     - Room type preference
     - Accommodation type preference
     - Special requirements
     - Building preference
     - Duration, move-in date
     - Course, institution
   - Dismissible

3. **Allocate Now Button:**
   - Shows when application status = AWAITING_ALLOCATION
   - Clicking opens allocation-choice-dialog (Task 19)
   - Disabled if application not in correct status

4. **Column Sorting:**
   - Ensure sortable by: Application Date, Preferred Building, Status

**Location:** `frontend/src/app/features/admin/application-review/application-review.component.ts` and `.html`

**Verification:**
- New columns display with correct data
- View Preferences dialog shows all preference fields
- Allocate Now button functional for correct status

---

### Task 27: Frontend - Admin Routing
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** All admin components created (Tasks 18-26)  
**Estimated Time:** 1-2 hours

**Description:**
Add routes for new admin features in admin routing module.

**Routes to Add:**

```typescript
// frontend/src/app/features/admin/admin-routing.module.ts

const routes: Routes = [
  {
    path: 'allocation-rules',
    component: AllocationRuleManagementComponent,
    data: { title: 'Allocation Rules' }
  },
  {
    path: 'allocations/staged',
    component: StagedAllocationComponent,
    data: { title: 'Staged Allocations' }
  },
  {
    path: 'waiting-list',
    component: WaitingListDashboardComponent,
    data: { title: 'Waiting List' }
  },
  {
    path: 'buildings/:buildingId/attributes',
    component: BuildingAttributesManagementComponent,
    data: { title: 'Building Attributes' }
  },
  // Existing routes remain unchanged
];
```

**Location:** `frontend/src/app/features/admin/admin-routing.module.ts`

**Verification:**
- Routes defined correctly
- Lazy loading works if applicable
- Components load on navigation

---

## Phase 7: System Configuration

### Task 28: Backend - System Settings for Auto-Allocation Toggle
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** Task 1  
**Estimated Time:** 2-3 hours

**Description:**
Create system settings infrastructure with auto-allocation feature toggle (disabled by default).

**Files to Create:**

1. **SystemSettings Entity + Repository:**
   `backend/src/main/java/com/beezhub/student_accommodation/model/entity/SystemSettings.java`
   - Fields: id, settingKey (UNIQUE), settingValue, description, createdAt, updatedAt
   - Type: JPA Entity

2. **DTOs:**
   - `SystemSettingsRequest { settingKey, settingValue, description }`
   - `SystemSettingsResponse { id, settingKey, settingValue, description, updatedAt }`

3. **Service:**
   `backend/src/main/java/com/beezhub/student_accommodation/service/SystemSettingsService.java`
   - Method: `getSetting(String settingKey): SystemSettingsResponse`
   - Method: `updateSetting(String settingKey, String value): SystemSettingsResponse`
   - Method: `isAutoAllocationEnabled(): Boolean`
   - Cache settings with reasonable TTL (e.g., 5 minutes)

4. **Controller:**
   `backend/src/main/java/com/beezhub/student_accommodation/controller/SystemSettingsController.java`
   - `GET /api/admin/settings/{settingKey}` - Get setting
   - `GET /api/admin/settings/auto-allocation/enabled` - Get auto-allocation status
   - `PUT /api/admin/settings/{settingKey}` - Update setting
   - Security: ROLE_ADMIN required

5. **Data Initialization:**
   - Migration or Spring Boot startup to seed `auto_allocation_enabled = false`

**Location:** `backend/src/main/java/com/beezhub/student_accommodation/`

**Verification:**
- SystemSettings table created
- Default value inserted (auto_allocation_enabled = false)
- Service retrieves and caches correctly
- Controller endpoint returns correct value
- Admin can update toggle

---

## Phase 8: Testing

### Task 29: Backend Unit Tests - AllocationScoringService
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** Task 5  
**Estimated Time:** 3-4 hours

**Description:**
Write comprehensive unit tests for scoring engine logic, constraint validation, and warning generation.

**Test File:**
`backend/src/test/java/com/beezhub/student_accommodation/service/AllocationScoringServiceTest.java`

**Test Cases:**

1. **Hard Constraint Validation:**
   - `testGenderMismatchBlocked()` - Blocks allocation if gender doesn't match
   - `testAccessibilityRequiredButRoomNotCompliant()` - Blocks if accessibility needed but room doesn't comply
   - `testAccessibilityMet()` - Allows if accessibility requirements met

2. **Scoring Calculation:**
   - `testBuildingPreferenceMatchBonus()` - Score higher if preferred building matches
   - `testDesirabilityScored()` - Desirability weight applied correctly
   - `testAccessibilityWeighted()` - Accessibility weight applied
   - `testWeightedScoringFormula()` - Overall calculation correct

3. **Tie-Breaker Logic:**
   - `testTieBreakerByApplicationDate()` - Earlier application dates ranked higher
   - `testTieBreakerMultipleStudents()` - Correct ordering with multiple students

4. **Warning Generation:**
   - `testBuildingMismatchWarning()` - Generates warning if preferred building unavailable
   - `testSuboptimalDesirabilityWarning()` - Warns if score below threshold
   - `testMultipleWarnings()` - Returns multiple warnings correctly

5. **Rule Loading:**
   - `testGetLatestRuleForBuilding()` - Loads active rule with highest priority
   - `testFallbackToGlobalRule()` - Falls back to global rule if no building-specific rule

**Location:** `backend/src/test/java/com/beezhub/student_accommodation/service/`

**Verification:**
- All tests pass
- Code coverage > 80%
- Edge cases covered

---

### Task 30: Backend Unit Tests - RoomAllocationService
**Status:** Not Started  
**Priority:** 🔴 Critical  
**Dependencies:** Task 8  
**Estimated Time:** 4-5 hours

**Description:**
Write comprehensive unit tests for allocation service including batch operations, moves, overrides, and audit logging.

**Test File:**
`backend/src/test/java/com/beezhub/student_accommodation/service/RoomAllocationServiceTest.java`

**Test Cases:**

1. **Staged Allocation Proposal:**
   - `testProposeAllocation()` - Creates staged allocations with scores
   - `testProposeBatchLimit()` - Rejects if >50 students
   - `testProposalValidatesStudentStatus()` - Only AWAITING_ALLOCATION students accepted
   - `testProposalGeneratesUniqueId()` - Each proposal has unique ID

2. **Confirmation Flow:**
   - `testConfirmAllocation()` - Creates active RoomAllocation
   - `testConfirmUpdateApplicationStatus()` - Sets application to ALLOCATED
   - `testConfirmRoomCapacity()` - Checks room has capacity before confirming
   - `testConfirmAuditLog()` - Logs to AllocationAuditLog

3. **Batch Confirmation:**
   - `testConfirmBatchAllocations()` - Confirms all in batch
   - `testConfirmBatchPartialFailure()` - Returns counts on success/failure

4. **Rejection:**
   - `testRejectAllocation()` - Sets staged allocation to REJECTED
   - `testRejectStudentRemainsWaiting()` - Student stays on waiting list

5. **Move Student:**
   - `testMoveStudent()` - Updates room_id and timestamp
   - `testMoveValidatesCapacity()` - Rejects if new room full
   - `testMoveValidatesConstraints()` - Rejects if constraints violated
   - `testMoveLogsToAuditTrail()` - Creates audit log with reason
   - `testMoveSendsNotification()` - Triggers notification email

6. **Override Constraints:**
   - `testOverrideConstraints()` - Creates allocation bypassing hard constraints
   - `testOverrideLogsConstraintBypassed()` - Records which constraint bypassed
   - `testOverrideWithReason()` - Captures admin reason

7. **Waiting List Re-scoring:**
   - `testAllocateFromWaitingList()` - Picks highest-scoring student
   - `testWaitingListReScoringOnRuleChange()` - Recalculates all scores

8. **Waiting List Query:**
   - `testGetWaitingListWithScores()` - Returns students ordered by score DESC

**Location:** `backend/src/test/java/com/beezhub/student_accommodation/service/`

**Verification:**
- All tests pass
- Code coverage > 80%
- Edge cases and error conditions covered

---

### Task 31: Backend Integration Tests - Allocation Workflow
**Status:** Not Started  
**Priority:** 🔴 Critical  
**Dependencies:** Tasks 29, 30  
**Estimated Time:** 4-5 hours

**Description:**
Write end-to-end integration tests for full allocation workflows including API calls, database state, and notifications.

**Test File:**
`backend/src/test/java/com/beezhub/student_accommodation/integration/AllocationWorkflowIntegrationTest.java`

**Test Scenarios:**

1. **Full Proposal → Confirm → Active Flow:**
   - Create application, approve documents, move to AWAITING_ALLOCATION
   - Call propose endpoint with student ID
   - Verify StagedAllocation created with score
   - Call confirm endpoint
   - Verify RoomAllocation created, application moved to ALLOCATED
   - Verify notification email sent

2. **Manual Allocation Flow:**
   - Create application, move to AWAITING_ALLOCATION
   - Call manual allocate endpoint with room selection
   - Verify RoomAllocation created with allocation_method=MANUAL
   - Verify notification sent

3. **Move Student Flow:**
   - Have active allocation
   - Call move endpoint with new room
   - Verify allocation.room_id updated
   - Verify audit log created with reason
   - Verify move notification sent to student

4. **Rule Change Impact:**
   - Create waiting list entries
   - Change allocation rules
   - Verify waiting list re-scores
   - Verify order changes based on new scores

5. **Batch Operations:**
   - Create 10 AWAITING_ALLOCATION applications
   - Call propose batch endpoint
   - Verify all 10 have staged allocations
   - Call confirm batch
   - Verify all confirmed in single transaction (all or nothing)

6. **Override Constraint:**
   - Create allocation violating gender restriction
   - Call override endpoint with constraint list
   - Verify allocation created with bypass reason logged
   - Verify audit trail recorded

7. **Error Handling:**
   - Test >50 student batch (400 error)
   - Test confirm non-existent staged allocation (404 error)
   - Test move to full room (409 error)
   - Test invalid building preference (400 error)

**Location:** `backend/src/test/java/com/beezhub/student_accommodation/integration/`

**Verification:**
- All workflows complete successfully
- Database state correct after each operation
- Notifications triggered appropriately
- Error handling correct
- Transaction integrity maintained

---

### Task 32: Frontend Unit Tests - Allocation Components
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** Tasks 18-26  
**Estimated Time:** 4-5 hours

**Description:**
Write unit tests for allocation UI components using Angular testing utilities.

**Test Files to Create:**

1. `application-detail-dialog.component.spec.ts`
   - Test component initialization with application data
   - Test document comment display
   - Test save progress functionality
   - Test move-to-allocation button validation
   - Test reject application flow

2. `allocation-choice-dialog.component.spec.ts`
   - Test auto-allocate option visibility (based on settings)
   - Test manual room selector functionality
   - Test allocation confirmation

3. `staged-allocation.component.spec.ts`
   - Test student multi-select with 50 limit
   - Test batch proposal API call
   - Test staged allocation review table
   - Test confirm/reject operations

4. `allocation-move-dialog.component.spec.ts`
   - Test room selector filters
   - Test validation (reason required, room has capacity)
   - Test move API call

5. `allocation-rule-management.component.spec.ts`
   - Test rule list loading
   - Test weight slider validation (sum to 100%)
   - Test create/edit/delete operations

6. `waiting-list-dashboard.component.spec.ts`
   - Test waiting list table loading
   - Test sorting by score
   - Test re-score functionality

**Location:** `frontend/src/app/features/admin/`

**Test Pattern:**
```typescript
describe('ApplicationDetailDialogComponent', () => {
  let component: ApplicationDetailDialogComponent;
  let fixture: ComponentFixture<ApplicationDetailDialogComponent>;
  let mockApplicationService: jasmine.SpyObj<ApplicationService>;
  
  beforeEach(async () => {
    // Setup
  });
  
  it('should load application details on init', () => {
    // Test
  });
  
  // More tests...
});
```

**Verification:**
- All tests pass
- Component initialization and interaction working
- Service calls mocked and verified
- User actions trigger expected behaviors

---

## Phase 9: Documentation

### Task 33: Documentation - Allocation Rules Guide
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** Tasks 5, 21  
**Estimated Time:** 2-3 hours

**Description:**
Create comprehensive guide explaining allocation rules, weighted scoring, per-building configuration, and how to optimize scores for institutional goals.

**File to Create:**
`docs/ALLOCATION_RULES_GUIDE.md`

**Sections:**

1. **Overview**
   - What allocation rules are
   - How they affect room assignments
   - When to use different scoring weights

2. **Scoring Formula**
   - Mathematical formula with explanation
   - Each component (building preference, desirability, accessibility)
   - Score range (0-100+)

3. **Per-Building Configuration**
   - How to set rules per building
   - Rule priority (which rule applies)
   - Global vs building-specific rules

4. **Weight Tuning**
   - Preference match weight: When to increase (encourage preferred buildings)
   - Desirability weight: When to increase (prioritize better facilities)
   - Accessibility weight: When to increase (prioritize disabled students)
   - Examples of weight combinations for different goals

5. **Hard Constraints**
   - Gender restrictions (NONE, MALE_ONLY, FEMALE_ONLY)
   - Accessibility compliance (must-haves)
   - How to bypass with admin override

6. **Step-by-Step Configuration**
   - Accessing allocation rules admin panel
   - Creating a rule
   - Testing scores before deployment
   - Monitoring allocation outcomes

7. **Troubleshooting**
   - "Why is this student getting a low score?"
   - "How do I ensure accessibility students are prioritized?"
   - "Can I change rules mid-allocation season?"

**Location:** `docs/ALLOCATION_RULES_GUIDE.md`

---

### Task 34: Documentation - Admin Allocation Workflow
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** Tasks 18-23  
**Estimated Time:** 2-3 hours

**Description:**
Create step-by-step guide for admins on how to review applications, handle documents, and perform allocations (manual and auto).

**File to Create:**
`docs/ADMIN_ALLOCATION_WORKFLOW.md`

**Sections:**

1. **Application Review Process**
   - Opening application detail dialog
   - Reviewing student profile and preferences
   - Reviewing uploaded documents
   - Leaving comments on rejected documents
   - Approving/rejecting documents
   - Saving progress mid-review

2. **Moving Application to Allocation Phase**
   - Checking required documents checklist
   - Understanding AWAITING_ALLOCATION status
   - Triggering move to allocation phase

3. **Manual Allocation**
   - Choosing "Manual Allocate" option
   - Filtering available rooms
   - Selecting appropriate room
   - Confirming allocation
   - Compliance documentation

4. **Auto Allocation (When Enabled)**
   - Understanding scoring and warnings
   - Reviewing proposed room
   - Confirming auto-suggested allocation
   - When to override auto-suggestion

5. **Batch Allocations**
   - Selecting multiple students (max 50)
   - Proposing batch
   - Reviewing proposals with scores
   - Confirming batch or individual rejections

6. **Managing Waiting List**
   - Understanding priority scoring
   - Re-scoring when rules change
   - Allocating from waiting list
   - Monitoring waiting list size

7. **Moving Students Between Rooms**
   - Opening move dialog
   - Validating new room
   - Capturing reason (compliance requirement)
   - Confirming move
   - Student notifications

8. **Compliance Considerations**
   - Per-case review requirement
   - Audit trail of all decisions
   - Reason capture for overrides
   - Gender and accessibility protections
   - Document preservation

**Location:** `docs/ADMIN_ALLOCATION_WORKFLOW.md`

---

### Task 35: Documentation - Student Preferences Guide
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** Task 16  
**Estimated Time:** 1-2 hours

**Description:**
Create guide explaining student preferences, how building selection works, and impact on allocation.

**File to Create:**
`docs/STUDENT_PREFERENCES_GUIDE.md`

**Sections:**

1. **Application Preferences Overview**
   - What preferences students can specify
   - How preferences affect allocation

2. **Preferred Building Selection**
   - Why to select a building
   - How selection influences scoring
   - Availability of buildings
   - Changing preferences before approval

3. **Room Type Preferences**
   - Available room types
   - How room type preference is matched

4. **Special Requirements**
   - Accessibility needs (list of common requirements)
   - How special requirements are honored
   - Accessibility compliance guarantee
   - Gender restrictions (if applicable)

5. **Move-In Date & Duration**
   - How move-in date affects allocation
   - Duration of stay
   - Fixed vs flexible dates

6. **How Allocation Works**
   - Application review process
   - Document verification
   - Allocation phase (auto vs manual)
   - Receiving allocation notification
   - What to do if not allocated (waiting list)

7. **Frequently Asked Questions**
   - "What if my preferred building is full?"
   - "Are my special requirements guaranteed to be met?"
   - "Can I change my preferences after applying?"
   - "How long does allocation take?"

**Location:** `docs/STUDENT_PREFERENCES_GUIDE.md`

---

## Phase 10: Data Migration & Performance

### Task 36: Data Migration - Seed Allocation Rules
**Status:** Not Started  
**Priority:** 🔴 Critical  
**Dependencies:** Task 2  
**Estimated Time:** 1-2 hours

**Description:**
Create database migration to seed default allocation rules for existing buildings with equal weights.

**Migration File:**
`backend/src/main/resources/db/migration/V2026_01_20_1600__seed_default_allocation_rules.sql`

**Content:**
```sql
-- Seed default allocation rules for all existing buildings
INSERT INTO allocation_rules (building_id, rule_name, preference_match_weight, 
  desirability_weight, accessibility_weight, active, priority, created_at, updated_at, created_by)
SELECT 
  id,
  'Default Allocation Rule',
  33.33,
  33.33,
  33.34,
  true,
  1,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP,
  1 -- Assuming admin user ID = 1
FROM buildings;

-- Global default rule (for buildings without specific rules)
INSERT INTO allocation_rules (building_id, rule_name, preference_match_weight, 
  desirability_weight, accessibility_weight, active, priority, created_at, updated_at, created_by)
VALUES (
  NULL,
  'Global Default Allocation Rule',
  33.33,
  33.33,
  33.34,
  true,
  1,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP,
  1
);

-- Verify insertion
SELECT COUNT(*) as rules_created FROM allocation_rules;
```

**Verification:**
- Migration executes without errors
- Default rule created for each building
- Global rule created
- Weights sum to 100%

---

### Task 37: Data Migration - Populate Building/Room Attributes
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** Task 2  
**Estimated Time:** 1-2 hours

**Description:**
Create migration to backfill building_attributes and room_attributes tables with sensible defaults.

**Migration File:**
`backend/src/main/resources/db/migration/V2026_01_20_1700__seed_building_room_attributes.sql`

**Content:**
```sql
-- Seed building attributes with defaults
INSERT INTO building_attributes (building_id, gender_restriction, accessibility_features, 
  desirability_score, allow_mixed_gender, created_at, updated_at)
SELECT 
  id,
  'NONE', -- No gender restriction by default
  'Wheelchair accessible, Elevator, Ground floor rooms', -- Default accessibility features
  5, -- Medium desirability score (1-10 scale)
  true, -- Allow mixed gender by default
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM buildings;

-- Seed room attributes
INSERT INTO room_attributes (room_id, accessibility_compliant, gender_restriction, 
  desirability_override, created_at, updated_at)
SELECT 
  id,
  CASE WHEN room_number <= 5 THEN true ELSE false END, -- Example: Rooms 1-5 are accessible
  'NONE',
  NULL, -- No override, use building's desirability score
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM rooms;

-- Verify insertion
SELECT COUNT(*) as building_attrs FROM building_attributes;
SELECT COUNT(*) as room_attrs FROM room_attributes;
```

**Verification:**
- Building attributes created for all buildings
- Room attributes created for all rooms
- Defaults are reasonable (no restrictions, medium desirability)
- Can be updated later by admins

---

### Task 38: System Integration Testing
**Status:** Not Started  
**Priority:** 🔴 Critical  
**Dependencies:** All implementation complete  
**Estimated Time:** 6-8 hours

**Description:**
Comprehensive end-to-end testing of entire system: admin review application → approve documents → choose allocation mode → manual/auto allocation → receive notifications.

**Test Scenarios:**

1. **Student Application Submission**
   - Student submits application with all preferences (including building)
   - System creates application with PENDING status
   - Student uploads required documents

2. **Admin Document Verification**
   - Admin opens application detail dialog
   - Reviews all documents
   - Leaves comments on rejected documents
   - Saves progress mid-review
   - Approves all required documents
   - Application moves to AWAITING_ALLOCATION

3. **Manual Allocation Path**
   - Admin opens allocation choice dialog
   - Selects "Manual Allocate"
   - Filters available rooms (by building, capacity)
   - Selects appropriate room
   - Confirms allocation
   - Verify RoomAllocation created with allocation_method=MANUAL

4. **Auto Allocation Path** (if enabled)
   - Admin opens allocation choice dialog
   - Selects "Auto Allocate"
   - System proposes room based on scoring rules
   - Admin reviews score and warnings
   - Admin confirms proposal
   - Verify RoomAllocation created with allocation_method=AUTO

5. **Notifications**
   - Verify student receives email when documents rejected with comment visible
   - Verify student receives email when allocation proposed
   - Verify student receives email when allocation confirmed
   - Verify student receives in-app notifications

6. **Waiting List Flow**
   - When room unavailable, student added to waiting list
   - Admin can view waiting list with priority scores
   - When room becomes available, top waiting student allocated
   - Verify waiting list re-scoring when rules change

7. **Move Student**
   - Student currently allocated to Room A
   - Admin moves student to Room B
   - Verify audit log created with reason
   - Verify student receives move notification with reason
   - Verify room capacities updated

8. **Compliance Audit Trail**
   - Verify all allocation decisions logged with admin name
   - Verify reasons captured for overrides/moves
   - Verify audit log accessible to compliance officer
   - Verify timestamp precision (audit trail accuracy)

9. **Performance Testing**
   - Batch propose 50 students → Response time < 5 seconds
   - Load waiting list with 100+ students → Display < 2 seconds
   - Re-score waiting list → Complete < 10 seconds

10. **Error Handling**
    - Attempt batch allocation with >50 students → Rejected with 400 error
    - Attempt move to full room → Rejected with 409 error
    - Attempt allocation violating accessibility → Shows warning/error
    - Verify meaningful error messages shown to admin

**Testing Tools:**
- Postman or REST client for API testing
- Chrome DevTools for frontend performance
- Database query tool for audit trail verification
- Email client or mock for notification verification

**Cross-Browser & Mobile:**
- Test in Chrome, Firefox, Safari
- Test responsive design on tablet (iPad)
- Test modal/dialog functionality on small screens

**Verification Checklist:**
- ✅ All workflows complete end-to-end
- ✅ Database state correct after each operation
- ✅ Notifications sent and deliverable
- ✅ Audit trail complete and accurate
- ✅ Performance acceptable for production
- ✅ No data loss or inconsistencies
- ✅ Compliance requirements met
- ✅ Error handling graceful

---

### Task 39: Performance Optimization & Indexing
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** All implementation complete  
**Estimated Time:** 2-3 hours

**Description:**
Create database indexes for performance optimization and execute performance tuning.

**Migration File:**
`backend/src/main/resources/db/migration/V2026_01_20_1800__add_performance_indexes.sql`

**Indexes to Create:**
```sql
-- Staged allocations queries
CREATE INDEX idx_staged_allocations_batch_id ON staged_allocations(batch_id);
CREATE INDEX idx_staged_allocations_status ON staged_allocations(status);
CREATE INDEX idx_staged_allocations_student_id ON staged_allocations(student_id);

-- Allocation audit log queries
CREATE INDEX idx_allocation_audit_log_allocation_id ON allocation_audit_log(allocation_id);
CREATE INDEX idx_allocation_audit_log_admin_user_id ON allocation_audit_log(admin_user_id);
CREATE INDEX idx_allocation_audit_log_created_at ON allocation_audit_log(created_at DESC);

-- Application queries
CREATE INDEX idx_applications_preferred_building_id ON applications(preferred_building_id);
CREATE INDEX idx_applications_status ON applications(status);
CREATE INDEX idx_applications_student_id ON applications(student_id);

-- Document comment queries
CREATE INDEX idx_document_comments_document_id ON document_comments(document_id);
CREATE INDEX idx_document_comments_admin_id ON document_comments(admin_id);

-- Waiting list queries
CREATE INDEX idx_waiting_list_entry_student_id ON waiting_list_entry(student_id);
CREATE INDEX idx_waiting_list_entry_created_at ON waiting_list_entry(created_at);

-- Building/Room attribute queries
CREATE INDEX idx_building_attributes_building_id ON building_attributes(building_id);
CREATE INDEX idx_room_attributes_room_id ON room_attributes(room_id);

-- Allocation rule queries
CREATE INDEX idx_allocation_rules_building_id ON allocation_rules(building_id);
CREATE INDEX idx_allocation_rules_active ON allocation_rules(active);
```

**Query Optimization:**
1. Batch proposal query: Ensure scoring query uses indexes
2. Waiting list query: Index on created_at for FIFO tie-breaker
3. Application list query: Index on status for filtering

**Caching Strategy:**
- Cache allocation rules (TTL: 1 hour, invalidate on update)
- Cache building/room attributes (TTL: 30 minutes)
- Cache system settings (TTL: 5 minutes)

**Verification:**
- Run EXPLAIN PLAN on slow queries
- Verify index usage (check MySQL EXPLAIN output)
- Load test: 100 concurrent batch proposals
- Monitor query times (target: < 500ms for most queries)

---

### Task 40: Security Audit & Rate Limiting
**Status:** Not Started  
**Priority:** 🟠 High  
**Dependencies:** All implementation complete  
**Estimated Time:** 2-3 hours

**Description:**
Verify security controls, implement rate limiting, and validate data isolation.

**Security Checks:**

1. **Role-Based Access Control:**
   - ✅ All admin endpoints require `@PreAuthorize("hasRole('ADMIN')")`
   - ✅ Students can only view their own applications/documents
   - ✅ Students cannot access allocation endpoints
   - Test: Attempt unauthorized access, verify 403 error

2. **Data Isolation:**
   - ✅ StudentId filters in all queries (no data bleeding)
   - ✅ Application detail dialog only accessible to admin or application owner
   - Test: Try to access another student's application, verify denied

3. **Rate Limiting:**
   - ✅ Batch proposal endpoint limited (max 1 per 10 seconds per admin)
   - ✅ Rule update endpoint limited (max 10 per minute per admin)
   - Implement using `@RateLimiter` annotation or Spring Cloud Rate Limiter
   - Test: Exceed limit, verify 429 (Too Many Requests) error

4. **Input Validation:**
   - ✅ All DTOs have @Valid annotations
   - ✅ Comments min 10 chars, max 500 chars
   - ✅ Weights sum to 100% (validated server-side)
   - ✅ Batch size max 50 (validated server-side)
   - Test: Submit invalid input, verify validation errors

5. **SQL Injection Prevention:**
   - ✅ All queries use parameterized statements (JPA/Hibernate)
   - ✅ No string concatenation in SQL
   - Verify using code review

6. **Audit Logging:**
   - ✅ All admin actions logged with timestamp, admin ID, action type
   - ✅ Sensitive operations (overrides, moves) logged with reason
   - ✅ Audit logs immutable (INSERT only, no UPDATE/DELETE)
   - Test: Perform admin action, verify audit log created

7. **Password & Authentication:**
   - ✅ BCrypt password hashing maintained
   - ✅ JWT tokens expire correctly (10 hours)
   - ✅ CORS properly configured
   - Existing security verified

**Implementation:**
```java
// Rate limiting annotation example
@PostMapping("/allocations/propose")
@RateLimiter(limit = 1, timeWindow = 10, timeUnit = TimeUnit.SECONDS)
public ResponseEntity<BatchAllocationProposalResponse> proposeAllocation(...) {
  // Implementation
}
```

**Verification Checklist:**
- ✅ Attempt unauthorized access → 403 Forbidden
- ✅ Attempt to access another user's data → 403 Forbidden
- ✅ Exceed rate limit → 429 Too Many Requests
- ✅ Submit invalid data → 400 Bad Request with validation errors
- ✅ All admin actions logged in audit trail
- ✅ No SQL injection vulnerabilities
- ✅ Passwords hashed with BCrypt
- ✅ Audit logs immutable

---

## Summary

**Total Tasks:** 40  
**Phases:** 10  
**Estimated Total Time:** 80-100 hours

**Execution Priority:**
1. **Phase 1-2:** Database & Backend (Foundation - must complete first)
2. **Phase 3-4:** Frontend Models & Student Features (Parallel to Phase 1-2)
3. **Phase 5:** Admin Application Review (Most critical user-facing feature)
4. **Phase 6:** Allocation Management UI (Completes allocation workflow)
5. **Phase 7:** System Configuration (Enables auto-allocation feature flag)
6. **Phase 8:** Testing (Verify all functionality)
7. **Phase 9:** Documentation (Support end users)
8. **Phase 10:** Final optimization and security

**Go-Live Readiness:**
- ✅ Manual allocation fully functional and tested
- ✅ Auto-allocation code ready but disabled (system_settings toggle = false)
- ✅ Per-case compliance workflow enforced
- ✅ Document comments visible to students for transparency
- ✅ Save progress functionality prevents incomplete reviews
- ✅ Audit trail complete for compliance
- ✅ All notifications working (document rejection, allocation updates)
- ✅ Security controls implemented and verified

---

*End of Implementation Plan*
