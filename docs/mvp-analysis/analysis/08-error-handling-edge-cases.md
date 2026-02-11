# Analysis 8: Error Handling & Edge Cases

## Scope

Identify scenarios where cascading operations, concurrent access, external service failures, or incomplete user flows may cause data corruption, broken state, or poor user experience.

---

## Finding 43: Institution Deletion with Active Students

**Category:** DEFECT
**Priority:** P1
**Affected Area:** Backend — Data Integrity

### Description
`InstitutionController.deleteInstitution()` allows an admin to delete an institution. If students are currently linked to that institution (via `Student.institution`), the deletion will either:
- Fail with a foreign key constraint error (if no cascade), exposing an ugly database error.
- Cascade and orphan student records (if cascade is misconfigured).

### Impact
- Admin sees an unhandled database error instead of a helpful message.
- If cascade is enabled, student profiles lose their institution reference silently.

### Evidence
- `Student` entity: `@OneToOne Institution institution` — fetch EAGER, no explicit cascade or orphan removal.
- `InstitutionController.deleteInstitution()` calls `institutionService.deleteInstitution()`.
- No pre-deletion check for linked students.

### Recommendation
Add a pre-deletion check: if any `Student` or `Building` references this institution, return `409 CONFLICT` with message "Cannot delete institution with active students or buildings."

---

## Finding 44: Building/Room Deletion with Active Allocations

**Category:** DEFECT
**Priority:** P1
**Affected Area:** Backend — Data Integrity

### Description
`BuildingController.deleteBuilding()` and `RoomController.deleteRoom()` allow deletion without checking for active `RoomAllocation` records. Deleting a room with an `ACTIVE` allocation would break the allocation record and leave the student in limbo.

### Impact
- Allocated students lose their room reference.
- Occupancy counts become inaccurate.
- Audit trail breaks (allocation references a deleted room).

### Evidence
- `Room` entity: `@OneToMany RoomAllocation` — no cascade on delete.
- `RoomService.deleteRoom()` — no check for active allocations before deletion.

### Recommendation
Add pre-deletion validation:
```java
if (roomAllocationRepository.existsActiveAllocationByRoomId(roomId)) {
    throw new ConflictException("Cannot delete room with active allocations");
}
```
Same pattern for buildings: check all rooms in the building for active allocations.

---

## Finding 45: Zombie User — Registered but No Student Profile

**Category:** DEFECT
**Priority:** P1
**Affected Area:** Backend / Frontend — User Flow

### Description
Registration is a two-step process: `POST /api/auth/signup` creates an `AppUser`, then the user must separately create a `Student` profile via `POST /api/student`. Between these steps, the user has a valid JWT token and is authenticated, but cannot submit applications or upload documents because those operations require a linked `Student` entity.

### Impact
- Users who stop after step 1 are "stuck" — logged in but unable to do anything useful.
- No background process or reminder prompts them to complete profile setup.
- The `StudentGuard` + `EmailVerifiedGuard` on the frontend route to profile setup, but if the user bookmarks the dashboard URL directly, they may bypass the flow.

### Evidence
- `AuthService.signup()`: creates `AppUser`, returns JWT.
- `StudentController.createStudent()`: separate endpoint, requires existing `AppUser`.
- Frontend `ProfileSetupComponent`: exists but user could navigate away.

### Recommendation
- Backend: Add a check in `ApplicationService.submitApplication()` that throws a clear error if `Student` profile is incomplete.
- Frontend: Add a `ProfileCompleteGuard` that redirects to profile setup if `Student` record doesn't exist (this may already exist via the existing routing, but should be verified end-to-end).
- Consider: add a reminder notification for users who registered > 24 hours ago but haven't completed their profile.

---

## Finding 46: Email Service Failure During Critical Actions

**Category:** IMPROVEMENT
**Priority:** P2
**Affected Area:** Backend — Resilience

### Description
`NotificationService` sends emails asynchronously with a retry mechanism (max 3 retries). This is well-designed. However, if the email service is completely unavailable, the system still proceeds with the business operation (application approval, document rejection, etc.). The student may not receive critical notifications.

### Impact
- A student's application could be approved but they never find out (email failed, in-app notification also depends on polling).
- The retry mechanism (5-minute intervals, max 3) covers transient failures but not prolonged outages.

### Evidence
- `NotificationService` uses `@Async` — email failure does not roll back the business transaction.
- `Notification` entity tracks `status` (PENDING/SENT/FAILED) and `retryCount`.
- Max retries: 3 (configurable).

### Recommendation
This is acceptable for MVP. For post-MVP: add a dead-letter notification dashboard for admins to see failed notifications and manually resend or follow up.

---

## Finding 47: MinIO Unavailability During Document Upload

**Category:** IMPROVEMENT
**Priority:** P2
**Affected Area:** Backend — Resilience

### Description
If MinIO is down when a student uploads a document, the upload will fail. The error handling returns an appropriate error to the user. However, the student may have filled out a long form and lose context.

### Impact
- Poor user experience during MinIO outages.
- No queuing or retry mechanism for uploads.

### Evidence
- `DocumentService.uploadDocuments()`: uploads directly to MinIO. If MinIO is unavailable, an exception is thrown.

### Recommendation
Acceptable for MVP. Ensure the frontend preserves form state on upload failure so the student can retry without re-filling everything. For post-MVP: consider a temporary local storage fallback with background sync to MinIO.

---

## Edge Case Verification Summary

| Scenario | Handled? | Finding |
|---|---|---|
| Institution deleted with linked students | **NO** — FK violation or silent orphan | [#43](#finding-43-institution-deletion-with-active-students) |
| Building deleted with occupied rooms | **NO** — same pattern | [#44](#finding-44-buildingroom-deletion-with-active-allocations) |
| Room deleted with active allocation | **NO** — same pattern | [#44](#finding-44-buildingroom-deletion-with-active-allocations) |
| Document type deleted with existing docs | **LIKELY SAFE** — FK prevents deletion | — |
| Concurrent room allocation (same room) | **OK** — `Room.version` optimistic locking | — |
| Email server down during approval | **OK** — async with retry, business logic proceeds | [#46](#finding-46-email-service-failure-during-critical-actions) |
| MinIO down during upload | **OK** — error returned to user | [#47](#finding-47-minio-unavailability-during-document-upload) |
| User registered but no Student profile | **PARTIAL** — frontend guard exists, backend gap | [#45](#finding-45-zombie-user--registered-but-no-student-profile) |
| Student with multiple institutions | **PREVENTED** — `Student` has single `Institution` FK | — |

## Summary

| # | Title | Category | Priority |
|---|---|---|---|
| 43 | Institution Deletion with Active Students | DEFECT | P1 |
| 44 | Building/Room Deletion with Active Allocations | DEFECT | P1 |
| 45 | Zombie User: No Student Profile | DEFECT | P1 |
| 46 | Email Service Failure During Critical Actions | IMPROVEMENT | P2 |
| 47 | MinIO Unavailability During Upload | IMPROVEMENT | P2 |
