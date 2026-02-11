# Critical Findings — P0 and P1

All findings that must be resolved before MVP launch.

---

## P0 — Showstoppers (MVP Cannot Ship)

| # | Title | Category | Area | Action | Acceptance Criteria |
|---|---|---|---|---|---|
| [1](../analysis/01-core-workflow-completeness.md#finding-1-no-academic-year--period-entity) | No Academic Year Entity | MISSING-CRITICAL | Data Model | Create `AcademicYear` entity, link to `Application` and `RoomAllocation` | Admin can create academic years. Applications are scoped to a year. Duplicate check uses academic year. |
| [2](../analysis/01-core-workflow-completeness.md#finding-2-no-student-offer-acceptance-step) | No Student Offer Acceptance | MISSING-CRITICAL | Workflow | Add `OFFERED`/`ACCEPTED` allocation statuses. Add student accept/decline endpoints. | Student receives offer notification. Can accept or decline. Declined rooms return to pool. |
| [7](../analysis/02-business-rule-enforcement.md#finding-7-br-001--duplicate-application-check-is-too-weak) | Duplicate Check Too Weak | DEFECT | Backend | Update to check all active statuses, not just PENDING | Student with APPROVED app cannot submit another for same academic year. |
| [19](../analysis/04-data-model-gaps.md#finding-19-no-pricing--fee-model-on-rooms) | No Pricing Model | MISSING-CRITICAL | Data Model | Create `RoomPricing` entity | Admin can set rates per building/type/year. Allocation locks price. |
| [20](../analysis/04-data-model-gaps.md#finding-20-no-student-funding--sponsorship-tracking) | No Funding Tracking | MISSING-CRITICAL | Data Model | Create `StudentFunding` entity | Student funding type (NSFAS/bursary/self) is captured and verifiable. |
| [21](../analysis/04-data-model-gaps.md#finding-21-no-lease--contract-agreement-entity) | No Lease/Contract | MISSING-CRITICAL | Data Model | Create `LeaseAgreement` entity | Lease generated on offer acceptance. Terms version and price locked. |
| [26](../analysis/05-missing-mvp-features.md#finding-26-no-bulk-data-import-for-students-or-rooms) | No Bulk Import | MISSING-CRITICAL | Backend | Create CSV import endpoints for rooms and students | Admin can upload CSV of 500+ rooms. Validation errors reported per row. |
| [32](../analysis/06-nfr-vs-reality.md#finding-32-no-rate-limiting-anywhere-nfr-008) | No Rate Limiting | MISSING-CRITICAL | Security | Implement Bucket4j rate limiting on auth endpoints | Login limited to 5/min/IP. Password reset limited to 3/hour/email. |

---

## P1 — Critical (Must Fix Before UAT)

| # | Title | Category | Area | Action | Acceptance Criteria |
|---|---|---|---|---|---|
| [3](../analysis/01-core-workflow-completeness.md#finding-3-no-check-in--check-out-process) | No Check-in/Check-out | MISSING-IMPORTANT | Workflow | Add check-in/check-out fields and endpoints | Admin can record check-in date. Room status reflects actual occupancy. |
| [4](../analysis/01-core-workflow-completeness.md#finding-4-no-application-period-management) | No Application Periods | MISSING-IMPORTANT | Backend | Use `AcademicYear` open/close dates to gate submissions | Application rejected with clear message if submitted outside window. |
| [5](../analysis/01-core-workflow-completeness.md#finding-5-no-re-application--renewal-flow) | No Renewal Flow | MISSING-IMPORTANT | Workflow | Scope duplicate check to academic year | Current resident can apply for next year. |
| [6](../analysis/01-core-workflow-completeness.md#finding-6-incomplete-post-allocation-experience) | No Post-Allocation Guidance | MISSING-IMPORTANT | Frontend | Create "My Accommodation" dashboard section | Student sees room details, building info, move-in instructions after allocation. |
| [8](../analysis/02-business-rule-enforcement.md#finding-8-br-004--no-application-expiry-scheduled-task) | No Expiry Task | MISSING-CRITICAL | Backend | Create nightly scheduled job | PENDING apps > 30 days old are set to EXPIRED. Student notified. |
| [10](../analysis/02-business-rule-enforcement.md#finding-10-br-010--gender-restrictions-bypassed-in-manual-allocation) | Gender Bypass in Manual | DEFECT | Backend | Add constraint validation to manual allocation | Manual allocation fails if gender mismatch. Override requires `force=true` + reason. |
| [12](../analysis/02-business-rule-enforcement.md#finding-12-br-012--no-first-year-priority-in-scoring) | No First-Year Priority | MISSING-IMPORTANT | Backend | Add year-of-study weight to scoring | Year 1 students score higher than Year 3+ for equivalent rooms. |
| [14](../analysis/03-role-permission-gaps.md#finding-14-landlord-role-has-zero-dedicated-functionality) | Landlord Role Empty | MISSING-IMPORTANT | SaaS | Define and implement basic Landlord scope | Landlord can view own buildings and occupancy. |
| [15](../analysis/03-role-permission-gaps.md#finding-15-students-cannot-see-waiting-list-position) | No Waiting List Visibility | MISSING-IMPORTANT | Backend/FE | Add student-facing waiting list endpoint | Student sees position and queue size on dashboard. |
| [16](../analysis/03-role-permission-gaps.md#finding-16-no-admin-bulk-application-processing) | No Bulk Status Update | MISSING-IMPORTANT | Backend | Add bulk status update endpoint | Admin can select 50 applications and approve/reject in one action. |
| [22](../analysis/04-data-model-gaps.md#finding-22-no-room-amenities--features-data) | No Room Amenities | MISSING-IMPORTANT | Data Model | Create `Amenity` entity + join tables | Rooms and buildings have tagged amenities. Students can filter. |
| [23](../analysis/04-data-model-gaps.md#finding-23-accommodationtype-not-linked-to-room) | AccommodationType Not Linked | DEFECT | Data Model | Add FK from `Room` to `AccommodationType` | Rooms are typed. Student preference matches actual rooms. |
| [27](../analysis/05-missing-mvp-features.md#finding-27-no-broadcast--announcement-system) | No Broadcast System | MISSING-IMPORTANT | Backend | Add announcement endpoint with targeting | Admin can email all students in Building A. |
| [28](../analysis/05-missing-mvp-features.md#finding-28-no-data-export-for-funding-claims) | No Funding Export | MISSING-IMPORTANT | Backend | Add CSV export for allocations | Admin exports allocation+funding data for NSFAS claims. |
| [31](../analysis/05-missing-mvp-features.md#finding-31-no-saas-tenant-onboarding-flow) | No Tenant Onboarding | MISSING-IMPORTANT | SaaS | Document onboarding process | Runbook exists for onboarding a new institution. |
| [36](../analysis/06-nfr-vs-reality.md#finding-36-no-api-versioning-nfr-023) | No API Versioning | MISSING-IMPORTANT | Backend | Add `/api/v1/` prefix to all endpoints | All endpoints use versioned paths. |
| [38](../analysis/07-security-concerns.md#finding-38-runtime-exception-leaks-internal-messages) | Error Message Leakage | DEFECT | Security | Mask generic exception messages | RuntimeException returns generic message + trace ID. |
| [39](../analysis/07-security-concerns.md#finding-39-role-hierarchy--landlord-inherits-student-permissions) | Role Hierarchy Risk | DEFECT | Security | Remove LANDLORD > STUDENT inheritance | Landlord cannot access student endpoints. |
| [40](../analysis/07-security-concerns.md#finding-40-no-jwt-revocation-on-password-change) | No Token Revocation | MISSING-IMPORTANT | Security | Implement token version or blacklist | Password change invalidates existing tokens. |
| [41](../analysis/07-security-concerns.md#finding-41-file-upload-content-type-not-validated-server-side) | No Content-Type Validation | MISSING-IMPORTANT | Security | Add server-side MIME validation | Renamed .exe → .pdf is rejected on upload. |
| [42](../analysis/07-security-concerns.md#finding-42-delete-account-may-not-remove-minio-files) | Delete Doesn't Clean MinIO | DEFECT | Security | Delete MinIO files before DB records | Account deletion removes all stored documents. |
| [43](../analysis/08-error-handling-edge-cases.md#finding-43-institution-deletion-with-active-students) | Institution Delete Cascade | DEFECT | Backend | Add pre-deletion check | Cannot delete institution with linked students. 409 returned. |
| [44](../analysis/08-error-handling-edge-cases.md#finding-44-buildingroom-deletion-with-active-allocations) | Room Delete Cascade | DEFECT | Backend | Add pre-deletion check | Cannot delete room with active allocation. 409 returned. |
| [45](../analysis/08-error-handling-edge-cases.md#finding-45-zombie-user--registered-but-no-student-profile) | Zombie User State | DEFECT | Backend/FE | Add profile-complete guard + backend check | Incomplete profile users cannot submit applications. Redirect to setup. |
| [48](../analysis/09-ux-workflow-friction.md#finding-48-application-status-labels-are-technical-jargon) | Jargon Status Labels | IMPROVEMENT | Frontend | Add display label mapping | Students see "Approved — Finding Your Room" not "AWAITING_ALLOCATION". |
| [49](../analysis/09-ux-workflow-friction.md#finding-49-no-post-allocation-guidance-for-students) | No Post-Allocation UX | MISSING-IMPORTANT | Frontend | Build "My Accommodation" section | Post-allocation dashboard shows room, building, instructions. |
| [53](../analysis/10-contradictions.md#finding-53-fr-015--under-review-status-does-not-exist) | "Under Review" Missing | CONTRADICTION | Backend | Add UNDER_REVIEW to ApplicationStatus | Admin can mark application as under review. Student sees status change. |
| [54](../analysis/10-contradictions.md#finding-54-fr-031--amenities-and-features-missing-from-room) | Room Amenities Missing | CONTRADICTION | Data Model | Implement Amenity entity | Rooms have amenities. Search supports filtering by amenity. |
| [58](../analysis/10-contradictions.md#finding-58-nfr-023--apis-are-unversioned) | APIs Unversioned | CONTRADICTION | Backend | Add /api/v1/ prefix | All endpoints versioned. |
