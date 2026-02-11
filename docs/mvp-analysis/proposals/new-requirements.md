# Proposal: New Requirements for Business Requirements Document

## Purpose

These requirements should be added to `docs/BUSINESS_REQUIREMENTS.md` to fill gaps identified during the MVP analysis. They are written in the same format as the existing document.

---

## New Functional Requirements

### 3.8 Academic Calendar Management

- **FR-044**: System shall support configurable academic years with start date, end date, application open date, and application close date.
  - *Justification:* [Finding #1](../analysis/01-core-workflow-completeness.md#finding-1-no-academic-year--period-entity)
- **FR-045**: Applications shall be scoped to an academic year. Students shall only submit applications during the configured open period.
  - *Justification:* [Finding #4](../analysis/01-core-workflow-completeness.md#finding-4-no-application-period-management)
- **FR-046**: Students shall be able to apply for the next academic year while a current-year allocation is active.
  - *Justification:* [Finding #5](../analysis/01-core-workflow-completeness.md#finding-5-no-re-application--renewal-flow)

### 3.9 Offer Acceptance & Occupancy

- **FR-047**: When a room is allocated to a student, the system shall create an offer with a configurable acceptance deadline (default: 7 days).
  - *Justification:* [Finding #2](../analysis/01-core-workflow-completeness.md#finding-2-no-student-offer-acceptance-step)
- **FR-048**: Students shall accept or decline room offers via the student portal.
  - *Justification:* [Finding #2](../analysis/01-core-workflow-completeness.md#finding-2-no-student-offer-acceptance-step)
- **FR-049**: System shall support check-in confirmation when a student physically moves in, and check-out when they depart.
  - *Justification:* [Finding #3](../analysis/01-core-workflow-completeness.md#finding-3-no-check-in--check-out-process)
- **FR-050**: The application review workflow shall include an "Under Review" status visible to the student.
  - *Justification:* [Finding #53](../analysis/10-contradictions.md#finding-53-fr-015--under-review-status-does-not-exist)

### 3.10 Pricing & Funding

- **FR-051**: System shall maintain room pricing per building, accommodation type, and academic year.
  - *Justification:* [Finding #19](../analysis/04-data-model-gaps.md#finding-19-no-pricing--fee-model-on-rooms)
- **FR-052**: System shall track student funding sources (NSFAS, bursary, self-funded) including reference numbers and verification status.
  - *Justification:* [Finding #20](../analysis/04-data-model-gaps.md#finding-20-no-student-funding--sponsorship-tracking)
- **FR-053**: Room allocations shall lock the applicable price at the time of offer acceptance.
  - *Justification:* [Finding #21](../analysis/04-data-model-gaps.md#finding-21-no-lease--contract-agreement-entity)

### 3.11 Lease / Agreement Management

- **FR-054**: System shall generate a lease agreement when a room offer is accepted, capturing terms version, locked price, and lease period.
  - *Justification:* [Finding #21](../analysis/04-data-model-gaps.md#finding-21-no-lease--contract-agreement-entity)
- **FR-055**: Buildings shall have configurable lease agreement templates.
  - *Justification:* SaaS requirement — different buildings may have different landlords with different terms.

### 3.12 Bulk Operations

- **FR-056**: Administrative staff shall import students in bulk via CSV upload.
  - *Justification:* [Finding #26](../analysis/05-missing-mvp-features.md#finding-26-no-bulk-data-import-for-students-or-rooms)
- **FR-057**: Administrative staff shall import rooms in bulk via CSV upload.
  - *Justification:* [Finding #26](../analysis/05-missing-mvp-features.md#finding-26-no-bulk-data-import-for-students-or-rooms)
- **FR-058**: Administrative staff shall update application statuses in bulk for a filtered set of applications.
  - *Justification:* [Finding #16](../analysis/03-role-permission-gaps.md#finding-16-no-admin-bulk-application-processing)

### 3.13 Communication

- **FR-059**: Administrative staff shall send broadcast announcements to students, filtered by building, application status, or institution.
  - *Justification:* [Finding #27](../analysis/05-missing-mvp-features.md#finding-27-no-broadcast--announcement-system)

### 3.14 Reporting & Export

- **FR-060**: System shall export allocation and funding data as CSV for external reporting and NSFAS claims.
  - *Justification:* [Finding #28](../analysis/05-missing-mvp-features.md#finding-28-no-data-export-for-funding-claims)

---

## New Business Rules

- **BR-013**: Unaccepted room offers shall expire after 7 days. The room shall be released to the next student on the waiting list.
  - *Justification:* [Finding #2](../analysis/01-core-workflow-completeness.md#finding-2-no-student-offer-acceptance-step)
- **BR-014**: Allocation scoring shall apply a configurable priority bonus for first-year students.
  - *Justification:* [Finding #12](../analysis/02-business-rule-enforcement.md#finding-12-br-012--no-first-year-priority-in-scoring)
- **BR-015**: Manual room allocation shall validate gender, accessibility, and year-of-study constraints. Override requires an explicit reason logged in the audit trail.
  - *Justification:* [Finding #10](../analysis/02-business-rule-enforcement.md#finding-10-br-010--gender-restrictions-bypassed-in-manual-allocation)
- **BR-016**: Deletion of institutions, buildings, or rooms shall be blocked if active allocations or applications reference them.
  - *Justification:* [Finding #43](../analysis/08-error-handling-edge-cases.md#finding-43-institution-deletion-with-active-students), [Finding #44](../analysis/08-error-handling-edge-cases.md#finding-44-buildingroom-deletion-with-active-allocations)

---

## New Non-Functional Requirements

- **NFR-028**: System shall implement rate limiting on authentication endpoints (login: 5/min/IP, signup: 10/hour/IP, password reset: 3/hour/email).
  - *Justification:* [Finding #32](../analysis/06-nfr-vs-reality.md#finding-32-no-rate-limiting-anywhere-nfr-008)
- **NFR-029**: All API endpoints shall be versioned with a `/api/v1/` prefix.
  - *Justification:* [Finding #36](../analysis/06-nfr-vs-reality.md#finding-36-no-api-versioning-nfr-023), [Finding #58](../analysis/10-contradictions.md#finding-58-nfr-023--apis-are-unversioned)
- **NFR-030**: The system shall validate uploaded file content types server-side (not just extension).
  - *Justification:* [Finding #41](../analysis/07-security-concerns.md#finding-41-file-upload-content-type-not-validated-server-side)
- **NFR-031**: Generic runtime errors shall not expose internal messages to API consumers. A trace reference ID shall be returned instead.
  - *Justification:* [Finding #38](../analysis/07-security-concerns.md#finding-38-runtime-exception-leaks-internal-messages)
- **NFR-032**: Account deletion shall remove all associated personal data including files stored in object storage (MinIO).
  - *Justification:* [Finding #42](../analysis/07-security-concerns.md#finding-42-delete-account-may-not-remove-minio-files)

---

## Updated Business Rules

These existing rules should be reworded:

- **BR-001 (updated)**: "Students can only have one active application per academic year. An application is considered active if its status is PENDING, UNDER_REVIEW, CHANGES_REQUESTED, APPROVED, AWAITING_ALLOCATION, or ALLOCATED."
  - *Justification:* [Finding #7](../analysis/02-business-rule-enforcement.md#finding-7-br-001--duplicate-application-check-is-too-weak)
- **BR-003 (updated)**: "Room assignments are final once accepted by the student. Administrative reassignment requires documented justification and is recorded in the audit log."
  - *Justification:* [Finding #13](../analysis/02-business-rule-enforcement.md#finding-13-br-003--final-assignment-contradicts-moveoverride-endpoints)
