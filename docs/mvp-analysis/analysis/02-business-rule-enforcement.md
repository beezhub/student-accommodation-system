# Analysis 2: Business Rule Enforcement

## Scope

Verify whether each business rule (BR-001 through BR-012) defined in `docs/BUSINESS_REQUIREMENTS.md` is enforced in the actual codebase.

---

## Finding 7: BR-001 — Duplicate Application Check is Too Weak

**Category:** DEFECT
**Priority:** P0
**Affected Area:** Backend — `ApplicationService`

### Description
BR-001 states "Students can only have one active application per academic year." The implementation only checks for `PENDING` status. A student with an `APPROVED`, `AWAITING_ALLOCATION`, or `ALLOCATED` application can submit another one.

### Impact
- Students can game the system by submitting multiple applications once the first is approved.
- Potential double-allocation: two approved applications for the same student could both reach the allocation phase.

### Evidence
- `ApplicationService.submitApplication()`:
  ```java
  if (applicationRepository.existsByStudent_IdAndStatus(studentId, PENDING)) {
      throw new DuplicateApplicationException("Student already has a pending application");
  }
  ```
- Does not check `APPROVED`, `AWAITING_ALLOCATION`, or `ALLOCATED` statuses.

### Recommendation
```java
List<ApplicationStatus> activeStatuses = List.of(PENDING, APPROVED, AWAITING_ALLOCATION, ALLOCATED);
if (applicationRepository.existsByStudent_IdAndStatusIn(studentId, activeStatuses)) {
    throw new DuplicateApplicationException("Student already has an active application");
}
```
After [Finding #1](01-core-workflow-completeness.md#finding-1-no-academic-year--period-entity) is resolved, scope this check to the current `AcademicYear`.

---

## Finding 8: BR-004 — No Application Expiry Scheduled Task

**Category:** MISSING-CRITICAL
**Priority:** P1
**Affected Area:** Backend — Scheduled Tasks

### Description
BR-004 states "Applications expire after 30 days without required documents." There is no `@Scheduled` task or background job that identifies and expires stale applications. The only scheduled task in the codebase is `NotificationService.retryFailedNotifications()`.

### Impact
- Abandoned `PENDING` applications accumulate indefinitely in the database.
- Skews admin dashboard metrics and application queue.
- Stale applications may block legitimate re-applications (depending on duplicate check logic).

### Evidence
- Grep for `@Scheduled` in the backend yields only notification retry.
- No `ApplicationExpiryService` or similar class exists.

### Recommendation
Implement a nightly scheduled job:
```java
@Scheduled(cron = "0 0 2 * * *") // 2 AM daily
public void expireStaleApplications() {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
    List<Application> stale = applicationRepository
        .findByStatusAndUpdatedAtBefore(PENDING, cutoff);
    stale.forEach(app -> {
        app.setStatus(ApplicationStatus.EXPIRED);
        notificationService.sendApplicationExpiredNotification(app);
    });
}
```
Add `EXPIRED` to `ApplicationStatus` enum (see [proposals/corrected-application-lifecycle.md](../proposals/corrected-application-lifecycle.md)).

---

## Finding 9: BR-007 — 12-Month Document Validity Not Tracked

**Category:** MISSING-IMPORTANT
**Priority:** P2
**Affected Area:** Backend — `DocumentService`

### Description
BR-007 states "Documents remain valid for 12 months from upload." There is no expiry check on documents. A document approved 2 years ago is treated identically to one approved yesterday.

### Impact
- Students may have outdated documents (expired ID, old proof of enrollment) that are still marked `APPROVED`.
- At allocation time, there is no validation that approved documents are still within the 12-month window.

### Evidence
- `Document` entity has `createdAt` but no `expiresAt` or `validUntil` field.
- `ApplicationService.getApplicationFullDetailsEnhanced()` checks document status but not age.

### Recommendation
Add `validUntil` field to `Document` (computed as `createdAt + 12 months`). Add a validation step in the allocation workflow that checks document freshness.

---

## Finding 10: BR-010 — Gender Restrictions Bypassed in Manual Allocation

**Category:** DEFECT
**Priority:** P1
**Affected Area:** Backend — `RoomAllocationService`

### Description
The batch/auto allocation path (`AllocationScoringService`) correctly enforces gender restrictions as a hard constraint. However, the manual allocation path (`createRoomAllocation`) performs capacity checks but does **not** validate gender, accessibility, or year-of-study constraints.

### Impact
- An admin could accidentally allocate a male student to a female-only room via the manual endpoint.
- No warning is surfaced to the admin — the allocation silently succeeds.

### Evidence
- `RoomAllocationService.createRoomAllocation()` calls `validateRoomCapacity()` but not `validateHardConstraints()`.
- `AllocationScoringService.calculateScore()` checks `RoomAttributes.genderRestriction`.

### Recommendation
Add constraint validation to manual allocation. If constraints are violated, return a warning and require an explicit `force=true` parameter with a `reason` field (logged to `AllocationAuditLog`).

---

## Finding 11: BR-011 — Special Requirements is Unstructured Free Text

**Category:** IMPROVEMENT
**Priority:** P2
**Affected Area:** Data Model

### Description
BR-011 says "Special requirements must be considered in room allocation." Both `Student.specialRequirements` and `Application.specialRequirements` are free-text `String` fields. The allocation algorithm cannot match these programmatically.

### Impact
- The scoring algorithm ignores special requirements entirely (only `RoomAttributes.accessibilityCompliant` is matched).
- Admins must manually read text and make judgment calls, which doesn't scale.

### Evidence
- `Student.specialRequirements` — `String` field, no structured format.
- `Application.specialRequirements` — `String` field, max 500 chars.
- `AllocationScoringService` does not reference either field.

### Recommendation
For MVP, keep the free-text field but add common structured options as checkboxes (e.g., wheelchair access, ground floor, quiet area, dietary needs). The structured fields can feed into the scoring algorithm while the free-text field captures anything else.

---

## Finding 12: BR-012 — No First-Year Priority in Scoring

**Category:** MISSING-IMPORTANT
**Priority:** P1
**Affected Area:** Backend — `AllocationScoringService`

### Description
BR-012 states "First-year students receive priority for certain room types." The scoring algorithm calculates scores based on preference match, desirability, and accessibility weights. There is **no year-of-study factor** in the score calculation.

### Impact
- First-year students have no advantage over returning students in the automated allocation process.
- Violates a core business promise that many institutions make to incoming students.

### Evidence
- `AllocationScoringService.calculateScore()`:
  ```java
  BigDecimal totalScore = weightedPreference
      .add(weightedDesirability)
      .add(weightedAccessibility);
  // No year-of-study bonus
  ```
- `AllocationRule` entity has no `firstYearBonus` or `yearOfStudyWeight` field.

### Recommendation
Add `yearOfStudyWeight` to `AllocationRule`. In the scoring algorithm, add a bonus if `student.yearOfStudy == 1`:
```java
if (student.getYearOfStudy().getYearOfStudy().equals("1")) {
    totalScore = totalScore.add(rule.getYearOfStudyWeight());
}
```

---

## Finding 13: BR-003 — "Final Assignment" Contradicts Move/Override Endpoints

**Category:** CONTRADICTION
**Priority:** P2
**Affected Area:** Business Rules / Documentation

### Description
BR-003 states "Room assignments are final once confirmed." However, the system provides:
- `POST /admin/allocations/{allocationId}/move` — move a student to a different room.
- `POST /admin/allocations/override` — override constraints and force an allocation.

These endpoints are legitimate operational tools, but they directly contradict the documented business rule.

### Impact
- Documentation misleads stakeholders about system capabilities.
- Auditors may flag the MOVE capability as a policy violation.

### Evidence
- `RoomAllocationController`: move and override endpoints exist with audit logging.
- `BUSINESS_REQUIREMENTS.md` BR-003: "Room assignments are final once confirmed."

### Recommendation
Update BR-003 to: "Room assignments are final once confirmed by the student. Administrative reassignment requires documented justification and is tracked in the audit log." This aligns the rule with reality.

---

## Verification Summary

| Rule | Description | Enforced? | Finding |
|---|---|---|---|
| BR-001 | One active application per year | **PARTIAL** — checks PENDING only | [#7](#finding-7-br-001--duplicate-application-check-is-too-weak) |
| BR-002 | Mandatory documents before processing | **YES** — document checklist in review flow | — |
| BR-003 | Assignments are final | **CONTRADICTED** — move/override exist | [#13](#finding-13-br-003--final-assignment-contradicts-moveoverride-endpoints) |
| BR-004 | 30-day expiry | **NO** — no scheduled task | [#8](#finding-8-br-004--no-application-expiry-scheduled-task) |
| BR-005 | 10MB file limit | **YES** — `application.yml` + frontend validation | — |
| BR-006 | PDF/JPG/PNG only | **YES** — frontend `validateFile()` + backend config | — |
| BR-007 | 12-month document validity | **NO** — not tracked | [#9](#finding-9-br-007--12-month-document-validity-not-tracked) |
| BR-008 | Rejected docs must be re-uploaded | **PARTIAL** — frontend shows status, no backend block | — |
| BR-009 | Room capacity enforced | **YES** — optimistic locking on `Room.version` | — |
| BR-010 | Gender restrictions | **PARTIAL** — auto only, not manual | [#10](#finding-10-br-010--gender-restrictions-bypassed-in-manual-allocation) |
| BR-011 | Special requirements considered | **PARTIAL** — free text only | [#11](#finding-11-br-011--special-requirements-is-unstructured-free-text) |
| BR-012 | First-year priority | **NO** — not in scoring | [#12](#finding-12-br-012--no-first-year-priority-in-scoring) |

## Summary

| # | Title | Category | Priority |
|---|---|---|---|
| 7 | BR-001: Duplicate Check Too Weak | DEFECT | P0 |
| 8 | BR-004: No Application Expiry Task | MISSING-CRITICAL | P1 |
| 9 | BR-007: Document Validity Not Tracked | MISSING-IMPORTANT | P2 |
| 10 | BR-010: Gender Bypass in Manual Allocation | DEFECT | P1 |
| 11 | BR-011: Unstructured Special Requirements | IMPROVEMENT | P2 |
| 12 | BR-012: No First-Year Priority Scoring | MISSING-IMPORTANT | P1 |
| 13 | BR-003: "Final" Contradicts Move/Override | CONTRADICTION | P2 |
