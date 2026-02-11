# Analysis 10: Contradictions Between Requirements and Implementation

## Scope

Identify direct conflicts between `docs/BUSINESS_REQUIREMENTS.md` and the actual codebase.

---

## Finding 53: FR-015 — "Under Review" Status Does Not Exist

**Category:** CONTRADICTION
**Priority:** P1
**Affected Area:** Backend / Business Rules

### Description
FR-015 defines the application workflow as: `PENDING → Under Review → APPROVED/REJECTED`. The implementation uses: `PENDING → APPROVED → AWAITING_ALLOCATION → ALLOCATED`. There is no `UNDER_REVIEW` status.

### Impact
- Students see `PENDING` from submission until decision — no indication their application is being actively reviewed.
- Admins cannot distinguish between untouched applications and ones they've started reviewing.
- FR-015 is directly contradicted.

### Evidence
- `BUSINESS_REQUIREMENTS.md` FR-015: "PENDING → Under Review → APPROVED/REJECTED."
- `ApplicationStatus.java`: `PENDING, APPROVED, AWAITING_ALLOCATION, ALLOCATED, REJECTED`.

### Recommendation
Add `UNDER_REVIEW` to `ApplicationStatus`. Transition `PENDING → UNDER_REVIEW` when an admin first opens the application detail view or explicitly clicks "Start Review." See [proposals/corrected-application-lifecycle.md](../proposals/corrected-application-lifecycle.md).

---

## Finding 54: FR-031 — "Amenities and Features" Missing from Room

**Category:** CONTRADICTION
**Priority:** P1
**Affected Area:** Data Model

### Description
FR-031 requires: "System shall maintain room inventory with: Room numbers, Capacity, Building/location, **Amenities and features**." The `Room` entity has number, capacity, and building — but no amenities field or relationship.

### Impact
- The requirement is documented but not implemented.
- Students and admins cannot filter or view room features.

### Evidence
- `BUSINESS_REQUIREMENTS.md` FR-031.
- `Room` entity: `roomNumber`, `capacity`, `currentOccupancy`, `status`, `building`.
- No `amenities` field or `Amenity` entity.

### Recommendation
See [Finding #22](04-data-model-gaps.md#finding-22-no-room-amenities--features-data) for the proposed `Amenity` entity.

---

## Finding 55: FR-012 — "Budget Constraints" Field Not Present

**Category:** CONTRADICTION
**Priority:** P2
**Affected Area:** Data Model

### Description
FR-012 requires the application to include "Budget constraints." The `Application` entity has `preferredAccommodationType`, `preferredMoveInDate`, `preferredDurationMonths`, `requiresAccessibility`, `specialRequirements`, and `preferredBuilding` — but no budget or price range field.

### Impact
- Students cannot express price preferences.
- The allocation algorithm cannot factor in budget.
- Minor impact for NSFAS-funded students (fixed rate), but relevant for self-funded students.

### Evidence
- `BUSINESS_REQUIREMENTS.md` FR-012: "Budget constraints."
- `Application` entity: no `budgetMin`, `budgetMax`, or `pricePreference` field.

### Recommendation
After implementing [Finding #19](04-data-model-gaps.md#finding-19-no-pricing--fee-model-on-rooms), add `BigDecimal maxBudget` to `Application`. The scoring algorithm can then penalize rooms that exceed the student's budget.

---

## Finding 56: FR-042/043 — Support Ticket System Not Implemented

**Category:** CONTRADICTION
**Priority:** P2
**Affected Area:** Frontend / Backend

### Description
FR-042 requires "Students shall be able to submit support tickets." FR-043 requires "Support staff shall respond to and track tickets." The frontend has a `ContactSupportComponent` but it is a static information page — no ticket entity, no ticket API, no tracking.

### Evidence
- `BUSINESS_REQUIREMENTS.md` FR-042, FR-043.
- `ContactSupportComponent`: exists in frontend routing.
- No `SupportTicket` entity or `/api/support-tickets` endpoints.

### Recommendation
Defer to Phase 2. For MVP, update the Contact Support page to display the institution's support email and office hours (configurable via `SystemSetting`). Document this as a known gap in the release notes.

---

## Finding 57: Section 5.2 — Redis Mentioned But Not Used

**Category:** CONTRADICTION
**Priority:** P2
**Affected Area:** Documentation / Infrastructure

### Description
Section 5.2 of the business requirements lists "Redis for session and performance caching" as part of the data storage architecture. Redis is not used anywhere in the application. Authentication is JWT-based (no session store needed), and there is no caching layer.

### Impact
- The documented architecture doesn't match reality.
- Stakeholders may expect Redis-based performance optimizations that don't exist.

### Evidence
- `BUSINESS_REQUIREMENTS.md` Section 5.2: "Caching: Redis for session and performance caching."
- No Redis dependency in `build.gradle`.
- No `@Cacheable` annotations or Redis configuration.

### Recommendation
Update `BUSINESS_REQUIREMENTS.md` to remove Redis from the current architecture. Add it to Section 11 (Future Enhancements) if caching is planned for later. If rate limiting is implemented via `Bucket4j` with Redis backend ([Finding #32](06-nfr-vs-reality.md#finding-32-no-rate-limiting-anywhere-nfr-008)), Redis can be reintroduced then.

---

## Finding 58: NFR-023 — APIs Are Unversioned

**Category:** CONTRADICTION
**Priority:** P1
**Affected Area:** Backend / SaaS Architecture

### Description
NFR-023 requires "Support for API version management." All endpoints use `/api/...` with no version prefix. For a SaaS platform, this is a significant gap — once external clients or mobile apps consume these APIs, any breaking change requires coordinated deployment across all consumers.

### Evidence
- `BUSINESS_REQUIREMENTS.md` NFR-023: "System shall support API versioning."
- All controllers: `@RequestMapping("/api/...")` — no `/api/v1/` prefix.

### Recommendation
Add `/api/v1/` prefix to all endpoints now, before any external consumers exist. This is a low-effort, high-value change.

---

## Contradiction Matrix

| Requirement | What It Says | What's Implemented | Finding |
|---|---|---|---|
| FR-012 | Budget constraints in application | No budget field | [#55](#finding-55-fr-012--budget-constraints-field-not-present) |
| FR-015 | Under Review status | No such status exists | [#53](#finding-53-fr-015--under-review-status-does-not-exist) |
| FR-031 | Room amenities and features | No amenities data | [#54](#finding-54-fr-031--amenities-and-features-missing-from-room) |
| FR-042/043 | Support ticket system | Static contact page only | [#56](#finding-56-fr-042043--support-ticket-system-not-implemented) |
| Section 5.2 | Redis caching | Not used | [#57](#finding-57-section-52--redis-mentioned-but-not-used) |
| NFR-023 | API versioning | Unversioned `/api/` | [#58](#finding-58-nfr-023--apis-are-unversioned) |
| BR-001 | Per academic year constraint | Checks PENDING only | [Finding #7](02-business-rule-enforcement.md#finding-7-br-001--duplicate-application-check-is-too-weak) |
| BR-003 | Final assignment | Move/Override exist | [Finding #13](02-business-rule-enforcement.md#finding-13-br-003--final-assignment-contradicts-moveoverride-endpoints) |
| BR-004 | 30-day expiry | No scheduled task | [Finding #8](02-business-rule-enforcement.md#finding-8-br-004--no-application-expiry-scheduled-task) |
| BR-007 | 12-month doc validity | Not tracked | [Finding #9](02-business-rule-enforcement.md#finding-9-br-007--12-month-document-validity-not-tracked) |
| BR-012 | First-year priority | Not in scoring | [Finding #12](02-business-rule-enforcement.md#finding-12-br-012--no-first-year-priority-in-scoring) |
| Section 6.1 | Payment gateway | No payment model | Deferred — see Phase 2 |

## Summary

| # | Title | Category | Priority |
|---|---|---|---|
| 53 | FR-015: "Under Review" Missing | CONTRADICTION | P1 |
| 54 | FR-031: Amenities Missing from Room | CONTRADICTION | P1 |
| 55 | FR-012: Budget Constraints Missing | CONTRADICTION | P2 |
| 56 | FR-042/043: Support Tickets Missing | CONTRADICTION | P2 |
| 57 | Section 5.2: Redis Not Used | CONTRADICTION | P2 |
| 58 | NFR-023: APIs Unversioned | CONTRADICTION | P1 |
