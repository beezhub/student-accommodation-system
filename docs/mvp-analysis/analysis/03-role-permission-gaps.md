# Analysis 3: Role & Permission Gaps

## Scope

Examine each user role for missing functionality, incorrect access, and SaaS-readiness gaps.

---

## Finding 14: LANDLORD Role Has Zero Dedicated Functionality

**Category:** MISSING-IMPORTANT
**Priority:** P1
**Affected Area:** Backend / Frontend / SaaS Strategy

### Description
The `LANDLORD` role exists in `UserRole` enum and the role hierarchy (`ADMIN > LANDLORD > STUDENT`), but has no dedicated controllers, service methods, or frontend pages. Admin can create landlord accounts via `UserManagementController`, but those users have no actions to perform.

### Impact
- Dead code increases the attack surface (see [Finding #39](07-security-concerns.md#finding-39-role-hierarchy-landlord-inherits-student)).
- For SaaS: the Landlord role is critical. External property managers need to manage their own buildings, view occupancy, and handle maintenance — without full admin access.
- Without Landlord functionality, the platform cannot onboard external accommodation providers.

### Evidence
- `UserRole.java`: `STUDENT, ADMIN, LANDLORD`.
- No controller has `@PreAuthorize("hasRole('LANDLORD')")`.
- No frontend route has a `LandlordGuard`.

### Recommendation
For MVP, either:
- **Option A (Recommended for SaaS):** Define the Landlord scope: view own buildings, view occupancy, update building info, receive allocation notifications. Implement a basic `LandlordController` and dashboard.
- **Option B (Minimal):** Remove `LANDLORD` from the role hierarchy and document it as Phase 2. Keep the enum value but don't expose it in user creation.

---

## Finding 15: Students Cannot See Waiting List Position

**Category:** MISSING-IMPORTANT
**Priority:** P1
**Affected Area:** Frontend / Backend

### Description
The `WaitingListEntry` entity tracks students awaiting allocation. The admin API returns waiting list data with priority scores. However, there is no student-facing endpoint or UI to show a student their position on the waiting list.

### Impact
- Students in the waiting list have no visibility into their status or expected timeline.
- Leads to support inquiries: "Where am I in the queue?"
- For SaaS: institutions will demand self-service transparency for students.

### Evidence
- `GET /api/admin/waiting-list` — admin-only endpoint.
- No `GET /api/waiting-list/my-position` or equivalent student endpoint.
- Student dashboard has no waiting list section.

### Recommendation
Add `GET /api/waiting-list/me` (STUDENT role) that returns the student's position, queue size, and status. Display this on the student dashboard when their application is in `AWAITING_ALLOCATION`.

---

## Finding 16: No Admin Bulk Application Processing

**Category:** MISSING-IMPORTANT
**Priority:** P1
**Affected Area:** Backend / Admin UX

### Description
Admins can only update application status one at a time via `PATCH /api/applications/{applicationId}/status`. During intake periods, an admin managing thousands of applications has no way to bulk-approve or bulk-reject a filtered set.

### Impact
- Processing 500 applications individually is operationally impractical.
- For SaaS: high-volume institutions will not adopt a system that requires per-application clicks.

### Evidence
- `ApplicationController`: only single-application status update endpoint exists.
- No `PATCH /api/applications/bulk-status` or similar.

### Recommendation
Add `PATCH /api/admin/applications/bulk-status` accepting a list of application IDs and a target status. Enforce the same `canTransitionTo()` validation per application and return a result summary (succeeded, failed, reasons).

---

## Finding 17: Students Cannot View Roommate Information

**Category:** MISSING-IMPORTANT
**Priority:** P2
**Affected Area:** Frontend / Backend

### Description
When a student is allocated to a shared room (capacity > 1), they cannot see who their roommates are. The `RoomAllocation` endpoint returns room details but not other occupants' information.

### Impact
- Students cannot prepare for shared living (e.g., coordinating move-in, knowing who they'll share with).
- For SaaS: this is a basic expectation of any accommodation platform.

### Evidence
- `RoomAllocationResponse` includes room info (number, building, capacity) but no occupant list.
- No endpoint returns "other students in my room."

### Recommendation
Add an optional occupant list (first name, year of study — no sensitive data) to the student's allocation view. Respect privacy by only showing co-occupants of the same room.

---

## Finding 18: Confusing API Identifier Usage

**Category:** IMPROVEMENT
**Priority:** P2
**Affected Area:** Backend — `StudentController`

### Description
`GET /api/student/{id}` accepts a path parameter called `id`, but the underlying service (`getStudentByUserId`) treats it as the `AppUser.id`, not the `Student.id`. Since these are different entities with different primary keys, passing a `Student.id` returns the wrong record or a 404.

### Impact
- Frontend developers or API consumers may pass `Student.id` (returned in list responses) and get unexpected results.
- The admin list endpoint returns `StudentResponse.id` (Student entity ID), but the detail endpoint expects `AppUser.id`.

### Evidence
- `StudentController.getStudent(@PathVariable Long id)` calls `studentService.getStudentByUserId(id)`.
- `StudentService.getStudentByUserId()` uses `studentRepository.findByAppUser_Id()`.

### Recommendation
Either rename the endpoint to `GET /api/student/by-user/{userId}` for clarity, or change the implementation to accept `Student.id` directly and look up the user for authorization.

---

## Verification Summary

| Role | Question | Status | Finding |
|---|---|---|---|
| STUDENT | Can see rejection reasons? | **YES** — `remarks` field in `ApplicationResponse` | — |
| STUDENT | Can see rejected document comments? | **YES** — `DocumentComment` + comments endpoint | — |
| STUDENT | Can re-upload rejected document? | **YES** — upload endpoint, same document type | — |
| STUDENT | Can cancel pending application? | **YES** — `DELETE /api/applications/{id}` | — |
| STUDENT | Can see waiting list position? | **NO** | [#15](#finding-15-students-cannot-see-waiting-list-position) |
| STUDENT | Can view roommate details? | **NO** | [#17](#finding-17-students-cannot-view-roommate-information) |
| STUDENT | Can report maintenance issues? | **NO** — Phase 2 | — |
| ADMIN | Consolidated student view? | **YES** — `getApplicationFullDetailsEnhanced` | — |
| ADMIN | Bulk approve/reject? | **NO** | [#16](#finding-16-no-admin-bulk-application-processing) |
| ADMIN | Room utilization trends? | **PARTIAL** — current stats only | — |
| ADMIN | Export allocation data? | **PARTIAL** — activity logs CSV only | — |
| ADMIN | Configure application periods? | **NO** | See [Finding #4](01-core-workflow-completeness.md#finding-4-no-application-period-management) |
| ADMIN | Send announcements? | **NO** | See [Finding #26](05-missing-mvp-features.md#finding-26-no-broadcast--announcement-system) |
| LANDLORD | Any dedicated functionality? | **NO** | [#14](#finding-14-landlord-role-has-zero-dedicated-functionality) |

## Summary

| # | Title | Category | Priority |
|---|---|---|---|
| 14 | LANDLORD Role Has Zero Functionality | MISSING-IMPORTANT | P1 |
| 15 | Students Cannot See Waiting List Position | MISSING-IMPORTANT | P1 |
| 16 | No Admin Bulk Application Processing | MISSING-IMPORTANT | P1 |
| 17 | Students Cannot View Roommate Info | MISSING-IMPORTANT | P2 |
| 18 | Confusing API Identifier Usage | IMPROVEMENT | P2 |
