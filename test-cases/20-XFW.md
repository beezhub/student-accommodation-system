# Cross-Feature Workflows — UAT Test Cases

> **Module:** Cross-Feature Workflows
> **System Under Test:** End-to-end scenarios spanning multiple modules (auth, applications, documents, allocations, notifications, jobs, admin)
> **Environment:** Frontend http://localhost:4200 | Backend http://localhost:8080 | Swagger http://localhost:8080/swagger-ui.html | MailHog http://localhost:8025 | MinIO http://localhost:9001

---

## Test Data Reference

| Role | Email | Password |
|------|-------|----------|
| Platform Admin | platformadmin@test.com | Admin@12345 |
| Tenant Admin | tenantadmin@test.com | Admin@12345 |
| Student A | john.doe@test.com | Student@123 |
| Student B | jane.smith@test.com | Student@123 |
| Student C | alex.jones@test.com | Student@123 |

| Building | Room | Capacity | Gender | Notes |
|----------|------|----------|--------|-------|
| Residence Hall Alpha | A-101 | 2 | ANY | Accessible |
| Residence Hall Beta | B-201 | 2 | FEMALE_ONLY | |
| Residence Hall Gamma | C-301 | 3 | MALE_ONLY | Accessible |

---

### TC-XFW-001: Full Happy Path — Signup to Checkout

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-001 |
| **Module** | Cross-Feature Workflows |
| **Priority** | Critical |
| **Preconditions** | System is running. No existing account for happypath@test.com. Room A-101 has available capacity. Required document types are configured. MailHog is cleared. |
| **Test Data** | New student: happypath@test.com / Student@123, First: Happy, Last: Path, Male, UCT, 1st Year. Room A-101. |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to http://localhost:4200/signup | Signup form displayed |
| 2 | Fill: Email=happypath@test.com, Password=Student@123, Confirm=Student@123 → Submit | Account created. Redirect to email verification page. |
| 3 | Open MailHog → find verification email to happypath@test.com → click verification link | Email verified. Redirected to profile setup. |
| 4 | Complete profile: First=Happy, Last=Path, Gender=Male, Institution=UCT, Year=1st Year → Submit | Profile saved. Redirected to student dashboard. |
| 5 | Navigate to Documents → upload required documents (ID document PDF, proof of enrollment PDF) | Documents uploaded with status PENDING |
| 6 | Navigate to Applications → Create New Application → fill preferences (Accommodation Type, Move-in Date, Duration, Room Type=Single, Special Requirements=None) → Submit | Application created with status PENDING |
| 7 | Check MailHog → find application submission confirmation email | Email received |
| 8 | Log in as Tenant Admin → navigate to Applications → find happypath@test.com's application | Application visible with status PENDING |
| 9 | Click on application → Change status to UNDER_REVIEW | Status changes to UNDER_REVIEW |
| 10 | Navigate to Documents → find happypath@test.com's documents → Approve each document | Each document status changes to APPROVED |
| 11 | Return to application → Approve application | Application status changes to APPROVED |
| 12 | Move application to AWAITING_ALLOCATION | Status changes to AWAITING_ALLOCATION |
| 13 | Navigate to Allocations → Allocate happypath@test.com to Room A-101 | Allocation created with status OFFERED |
| 14 | Check MailHog → find room offer email to happypath@test.com | Email received with room details and accept/decline instructions |
| 15 | Log in as happypath@test.com → navigate to Allocations → see OFFERED allocation for Room A-101 | Offer displayed with accept/decline buttons |
| 16 | Click Accept Offer | Allocation status changes to ACCEPTED |
| 17 | Check MailHog → find offer acceptance confirmation email | Email received |
| 18 | Log in as Tenant Admin → navigate to Allocations → find happypath@test.com → Check In | Allocation status changes to ACTIVE |
| 19 | Verify Room A-101 occupancy increased by 1 | Occupancy updated |
| 20 | Navigate to Allocations → Check Out happypath@test.com | Allocation status changes to COMPLETED |
| 21 | Verify Room A-101 occupancy decreased by 1 | Capacity freed |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation COMPLETED. Room capacity restored. Full email trail in MailHog. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-XFW-002: Application Rejection Flow

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-002 |
| **Module** | Cross-Feature Workflows |
| **Priority** | Critical |
| **Preconditions** | Student A has a PENDING application. Tenant Admin is available. |
| **Test Data** | john.doe@test.com (Student A), Tenant Admin |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin → navigate to Applications → find Student A's application (PENDING) | Application visible |
| 2 | Change status to UNDER_REVIEW | Status: UNDER_REVIEW |
| 3 | Click Reject → enter reason: "Insufficient documentation provided" → Confirm | Status: REJECTED |
| 4 | Check MailHog → find rejection email to john.doe@test.com | Email contains rejection reason |
| 5 | Log in as Student A → navigate to Applications | Application shows REJECTED status |
| 6 | Verify student cannot edit or resubmit the rejected application | No edit/resubmit buttons (REJECTED is terminal) |

| Field | Value |
|-------|-------|
| **Postconditions** | Application is REJECTED (terminal). Student notified. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-XFW-003: Changes Requested Flow

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-003 |
| **Module** | Cross-Feature Workflows |
| **Priority** | Critical |
| **Preconditions** | Student B has a PENDING application. Tenant Admin is available. |
| **Test Data** | jane.smith@test.com (Student B) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin → Applications → find Student B's application → UNDER_REVIEW | Status: UNDER_REVIEW |
| 2 | Click "Request Changes" → enter reason: "Please update your move-in date to fall within the academic year" → Submit | Status: CHANGES_REQUESTED |
| 3 | Check MailHog → find changes requested email to jane.smith@test.com | Email received with reason text |
| 4 | Log in as Student B → navigate to Applications | Application shows CHANGES_REQUESTED with the reason displayed |
| 5 | Edit the application → update move-in date → Resubmit | Application status changes back to PENDING |
| 6 | Log in as Tenant Admin → verify application is back in PENDING | Status: PENDING with updated data |
| 7 | Move to UNDER_REVIEW → Approve | Status: APPROVED |

| Field | Value |
|-------|-------|
| **Postconditions** | Application APPROVED after student addressed changes. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-XFW-004: Document Rejection and Resubmission

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-004 |
| **Module** | Cross-Feature Workflows |
| **Priority** | High |
| **Preconditions** | Student A has uploaded a document (e.g., ID document). Tenant Admin is available. |
| **Test Data** | john.doe@test.com (Student A), uploaded ID document |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin → navigate to Documents → find Student A's ID document | Document visible with PENDING status |
| 2 | Click Reject → enter comment: "Document is blurry and unreadable. Please upload a clear scan." → Submit | Document status: REJECTED |
| 3 | Check MailHog → find document rejection email to john.doe@test.com | Email received with rejection comment |
| 4 | Log in as Student A → navigate to Documents | Rejected document shown with rejection comment visible |
| 5 | Click on the rejected document → Upload new version (clear scan PDF) | New document version uploaded; status resets to PENDING |
| 6 | Log in as Tenant Admin → Documents → find the re-uploaded document | New version visible with PENDING status |
| 7 | Approve the document | Status: APPROVED |
| 8 | Check MailHog → find document approval email | Email received |

| Field | Value |
|-------|-------|
| **Postconditions** | Document approved after resubmission. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-XFW-005: Decline Offer and Requeue

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-005 |
| **Module** | Cross-Feature Workflows |
| **Priority** | Critical |
| **Preconditions** | Student A has an OFFERED allocation for Room A-101. Room C-301 has capacity. |
| **Test Data** | john.doe@test.com (Student A), Room A-101 → decline → reallocate to Room C-301 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A → Allocations → see offer for Room A-101 | OFFERED status displayed |
| 2 | Click Decline → select reason: WRONG_LOCATION → Confirm | Allocation status: DECLINED |
| 3 | Verify Room A-101 capacity is freed | Occupancy updated |
| 4 | Verify student's application returns to AWAITING_ALLOCATION (or waiting list) | Application requeued |
| 5 | Log in as Tenant Admin → Allocations → allocate Student A to Room C-301 | New allocation created: OFFERED for C-301 |
| 6 | Log in as Student A → Allocations → Accept offer for C-301 | Status: ACCEPTED |

| Field | Value |
|-------|-------|
| **Postconditions** | Student declined first offer, received and accepted second. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-XFW-006: Max Declines Reached — Application Cancelled

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-006 |
| **Module** | Cross-Feature Workflows |
| **Priority** | High |
| **Preconditions** | System has a max_declines setting (e.g., 2). Student C has AWAITING_ALLOCATION application. Multiple rooms available. |
| **Test Data** | alex.jones@test.com (Student C), max_declines = 2 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Verify system setting max_declines (via admin settings or Swagger) | Value noted (e.g., 2) |
| 2 | Tenant Admin allocates Student C to Room C-301 → OFFERED | Allocation created |
| 3 | Student C declines (reason: TOO_EXPENSIVE) | Decline #1. Student requeued. |
| 4 | Tenant Admin allocates Student C to Room A-101 → OFFERED | Second allocation created |
| 5 | Student C declines (reason: WRONG_LOCATION) | Decline #2. Max declines reached. |
| 6 | Verify Student C's application status | Status: CANCELLED (automatic due to max declines) |
| 7 | Check MailHog → find cancellation email to alex.jones@test.com | Email explains application cancelled due to maximum declined offers |
| 8 | Verify Student C is NOT requeued on waiting list | No waiting list entry exists |

| Field | Value |
|-------|-------|
| **Postconditions** | Application CANCELLED after max declines. No further allocations possible. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-XFW-007: Offer Expiry Flow — End to End

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-007 |
| **Module** | Cross-Feature Workflows |
| **Priority** | Critical |
| **Preconditions** | Student A has AWAITING_ALLOCATION application. Room A-101 available. Offer expiry job active. |
| **Test Data** | john.doe@test.com (Student A), offer_expiry_date set to past |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Tenant Admin allocates Student A to Room A-101 | Allocation: OFFERED |
| 2 | Via Swagger, set offerExpiryDate to 1 hour ago | Updated |
| 3 | Wait for offer expiry job to run | Job processes the allocation |
| 4 | GET allocation via Swagger | Status: EXPIRED |
| 5 | Check MailHog → find "Offer Expired" email to john.doe@test.com | Email received |
| 6 | Verify Student A's application status | Application back in allocation pool (AWAITING_ALLOCATION) or appropriate state |
| 7 | Verify Room A-101 capacity is freed | Capacity available |

| Field | Value |
|-------|-------|
| **Postconditions** | Offer expired. Room freed. Student notified. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-XFW-008: Check-In Expiry Flow — End to End

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-008 |
| **Module** | Cross-Feature Workflows |
| **Priority** | Critical |
| **Preconditions** | Student B has ACCEPTED allocation. Check-in expiry job active. |
| **Test Data** | jane.smith@test.com (Student B), check_in_expiry_date set to past |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Ensure Student B has ACCEPTED allocation for Room B-201 | Confirmed |
| 2 | Via Swagger, set checkInExpiryDate to 1 hour ago | Updated |
| 3 | Wait for check-in expiry job to run | Job processes the allocation |
| 4 | GET allocation via Swagger | Status: CHECK_IN_EXPIRED |
| 5 | Check MailHog → find "Check-In Expired" email to jane.smith@test.com | Email received |
| 6 | Verify Room B-201 capacity is freed | Capacity available |

| Field | Value |
|-------|-------|
| **Postconditions** | Check-in expired. Room freed. Student notified. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-XFW-009: Batch Allocation Flow — Multiple Students

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-009 |
| **Module** | Cross-Feature Workflows |
| **Priority** | Critical |
| **Preconditions** | Students A, B, C have AWAITING_ALLOCATION applications. Rooms available. auto_allocation_enabled = true. Allocation rules configured. |
| **Test Data** | All three students, Rooms A-101, B-201, C-301 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Verify all three students have AWAITING_ALLOCATION applications | Confirmed |
| 2 | Log in as Tenant Admin → navigate to Allocations → Propose Batch Allocation | Batch proposal initiated |
| 3 | Review staged allocations: verify students matched to rooms respecting gender rules (B→B-201 female, A/C→A-101/C-301 male-compatible) | Staged allocations displayed with proposed room assignments |
| 4 | Confirm all staged allocations | Staged allocations become OFFERED allocations |
| 5 | Check MailHog → all three students received offer emails | 3 offer emails present |
| 6 | Log in as Student A → Accept offer | ACCEPTED |
| 7 | Log in as Student B → Accept offer | ACCEPTED |
| 8 | Log in as Student C → Accept offer | ACCEPTED |
| 9 | Log in as Tenant Admin → Check in Student A | ACTIVE |
| 10 | Check in Student B | ACTIVE |
| 11 | Check in Student C | ACTIVE |

| Field | Value |
|-------|-------|
| **Postconditions** | All three students allocated, accepted, and checked in. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-XFW-010: Staged Allocation — Reject Some, Confirm Others

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-010 |
| **Module** | Cross-Feature Workflows |
| **Priority** | High |
| **Preconditions** | Batch allocation proposed with Students A, B, C in staged allocations. |
| **Test Data** | Students A, B, C with staged allocations |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin → Allocations → view staged allocations from batch proposal | Three staged allocations visible |
| 2 | Reject Student C's staged allocation (reason: room capacity concerns) | Staged allocation for C removed/rejected |
| 3 | Confirm Student A and Student B's staged allocations | Allocations created: A=OFFERED, B=OFFERED |
| 4 | Verify Student C remains on waiting list (AWAITING_ALLOCATION) | Application status unchanged |
| 5 | Check MailHog → Students A and B received offer emails; Student C did NOT | 2 offer emails only |

| Field | Value |
|-------|-------|
| **Postconditions** | A and B have offers. C remains in waiting pool. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-XFW-011: Move Student Between Rooms

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-011 |
| **Module** | Cross-Feature Workflows |
| **Priority** | High |
| **Preconditions** | Student A has ACTIVE allocation in Room A-101. Room C-301 has capacity. |
| **Test Data** | john.doe@test.com, Room A-101 → Room C-301 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin → Allocations → find Student A's ACTIVE allocation | Allocation visible for Room A-101 |
| 2 | Click Move → select Room C-301 as destination → Confirm | Move processed |
| 3 | Verify allocation now references Room C-301 | Room updated in allocation record |
| 4 | Verify Room A-101 occupancy decreased | Capacity freed |
| 5 | Verify Room C-301 occupancy increased | Capacity consumed |
| 6 | Check MailHog → find room move notification email to john.doe@test.com | Email received with old and new room details |

| Field | Value |
|-------|-------|
| **Postconditions** | Student moved. Both rooms' occupancy updated. Student notified. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-XFW-012: Override Allocation — Bypass Gender Restriction

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-012 |
| **Module** | Cross-Feature Workflows |
| **Priority** | High |
| **Preconditions** | Student A (male) exists. Room B-201 is FEMALE_ONLY. Admin has override capability. |
| **Test Data** | john.doe@test.com (male), Room B-201 (FEMALE_ONLY) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin → Allocations → attempt to allocate Student A to Room B-201 | System warns: gender restriction mismatch |
| 2 | Select "Override" or force allocation option → Confirm | Allocation created despite restriction: OFFERED |
| 3 | Navigate to Activity Logs (as Platform Admin if needed) | Override action logged with admin user, student, room, and override reason |
| 4 | Verify allocation record exists for Student A in Room B-201 | Allocation confirmed |

| Field | Value |
|-------|-------|
| **Postconditions** | Override allocation created. Audit log records the override. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-XFW-013: Multi-Tenant Isolation — End to End

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-013 |
| **Module** | Cross-Feature Workflows |
| **Priority** | Critical |
| **Preconditions** | Two tenants exist: Tenant A and Tenant B. Each has its own admin, buildings, rooms, and student applications. |
| **Test Data** | Tenant A admin: tenantadmin@test.com. Tenant B admin: tenantadminB@test.com. |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Platform Admin → create Tenant B (if not exists) with admin tenantadminB@test.com | Tenant B created |
| 2 | Log in as Tenant B admin → create Building "Beta Hall" with Room "BH-101" | Building and room created under Tenant B |
| 3 | Register a student under Tenant B → submit application | Application created under Tenant B |
| 4 | Log out → Log in as Tenant A admin (tenantadmin@test.com) | Tenant A dashboard loads |
| 5 | Navigate to Buildings | Only Tenant A's buildings shown (e.g., Residence Hall Alpha). "Beta Hall" NOT visible. |
| 6 | Navigate to Applications | Only Tenant A's applications shown. Tenant B's student NOT visible. |
| 7 | Via Swagger, GET /api/buildings/{tenantB_buildingId} with Tenant A token | 404 or 403 |
| 8 | Via Swagger, GET /api/admin/applications/{tenantB_appId} with Tenant A token | 404 or 403 |
| 9 | Log in as Platform Admin → verify both tenants' data is visible | All data accessible |

| Field | Value |
|-------|-------|
| **Postconditions** | Complete tenant isolation verified. Platform admin has cross-tenant access. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-XFW-014: Student Cancel and Reapply

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-014 |
| **Module** | Cross-Feature Workflows |
| **Priority** | High |
| **Preconditions** | Student A has a PENDING application. |
| **Test Data** | john.doe@test.com (Student A) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A → Applications → find PENDING application | Application visible |
| 2 | Cancel the application | Status: CANCELLED |
| 3 | Navigate to Applications → Create New Application | Application form displayed |
| 4 | Fill in new preferences (different from first application) → Submit | New application created with status PENDING |
| 5 | Verify the old CANCELLED application still exists in history | Both applications visible: one CANCELLED, one PENDING |
| 6 | Log in as Tenant Admin → verify new application appears in queue | New PENDING application visible |

| Field | Value |
|-------|-------|
| **Postconditions** | Old application CANCELLED. New application PENDING independently. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-XFW-015: Document Prerequisite Blocks Allocation Transition

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-015 |
| **Module** | Cross-Feature Workflows |
| **Priority** | Critical |
| **Preconditions** | Student A has APPROVED application. Student A has 2 documents: 1 APPROVED, 1 PENDING (not yet reviewed). |
| **Test Data** | john.doe@test.com, 1 approved doc, 1 pending doc |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin → Applications → find Student A's APPROVED application | Application visible |
| 2 | Attempt to move application to AWAITING_ALLOCATION | System blocks the transition: error message "Not all required documents are approved" (or similar) |
| 3 | Navigate to Documents → find Student A's PENDING document → Approve it | Document: APPROVED |
| 4 | Return to Applications → attempt to move to AWAITING_ALLOCATION again | Transition succeeds: status AWAITING_ALLOCATION |

| Field | Value |
|-------|-------|
| **Postconditions** | Application in AWAITING_ALLOCATION only after all docs approved. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-XFW-016: Notification Chain — Verify All Emails in Happy Path

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-016 |
| **Module** | Cross-Feature Workflows |
| **Priority** | High |
| **Preconditions** | MailHog cleared. A fresh student account will go through the full lifecycle. |
| **Test Data** | emailchain@test.com / Student@123 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Register emailchain@test.com → check MailHog | Email #1: Welcome / Verification email |
| 2 | Verify email → complete profile → upload docs → submit application → check MailHog | Email #2: Application submitted confirmation |
| 3 | Tenant Admin: UNDER_REVIEW → Approve documents → check MailHog | Email #3: Document(s) approved notification |
| 4 | Tenant Admin: Approve application → check MailHog | Email #4: Application approved notification |
| 5 | Tenant Admin: Move to AWAITING_ALLOCATION → Allocate to room → check MailHog | Email #5: Room offer notification |
| 6 | Student accepts offer → check MailHog | Email #6: Offer acceptance confirmation |
| 7 | Tenant Admin: Check in student → check MailHog | Email #7: Check-in confirmation (if sent) |
| 8 | Count total emails in MailHog for emailchain@test.com | At least 5-7 emails covering each major lifecycle event |
| 9 | Verify no duplicate emails for the same event | Each event has exactly one email |

| Field | Value |
|-------|-------|
| **Postconditions** | Complete email audit trail verified in MailHog. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Exact email count depends on which lifecycle events trigger notifications. |

---

### TC-XFW-017: Forced Password Change Flow

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-017 |
| **Module** | Cross-Feature Workflows |
| **Priority** | High |
| **Preconditions** | Platform Admin is available. |
| **Test Data** | New admin user: newadmin@test.com, temporary password: TempPass@123 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Platform Admin → Users → Create new admin user: newadmin@test.com with password TempPass@123 and mustChangePassword=true | User created |
| 2 | Log out → Log in as newadmin@test.com / TempPass@123 | Login succeeds |
| 3 | Observe redirect | Immediately redirected to /change-password |
| 4 | Try to navigate to /admin/applications by typing URL | Redirected back to /change-password |
| 5 | Enter new password: NewAdmin@12345 → Confirm → Submit | Password changed successfully. Redirect to /admin dashboard. |
| 6 | Navigate to /admin/applications | Admin applications page loads normally |
| 7 | Log out → Log in with NewAdmin@12345 | Login succeeds, no password change prompt |

| Field | Value |
|-------|-------|
| **Postconditions** | Password changed. mustChangePassword cleared. Normal admin access. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-XFW-018: Concurrent Allocation — Room Capacity Enforcement

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-018 |
| **Module** | Cross-Feature Workflows |
| **Priority** | Critical |
| **Preconditions** | Room with capacity=1 exists (or use a room with 1 remaining spot). Two students have AWAITING_ALLOCATION applications. |
| **Test Data** | Students A and B. Room with capacity 1 (or A-101 with 1 remaining spot). |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Ensure the target room has exactly 1 remaining spot | Confirmed |
| 2 | Tenant Admin allocates Student A to the room | Allocation: OFFERED. Room now at full capacity. |
| 3 | Tenant Admin attempts to allocate Student B to the SAME room | Allocation blocked: error "Room is full" or "No available capacity" |
| 4 | Verify Student B has no allocation | No allocation created for Student B |
| 5 | Verify room occupancy shows full | Capacity = occupied |

| Field | Value |
|-------|-------|
| **Postconditions** | Room capacity enforced. Second allocation prevented. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-XFW-019: Building Browse to Application (Unauthenticated → Authenticated)

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-019 |
| **Module** | Cross-Feature Workflows |
| **Priority** | Medium |
| **Preconditions** | Buildings are publicly visible. No account for browser@test.com. |
| **Test Data** | browser@test.com / Student@123 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open http://localhost:4200 without logging in | Public landing page or building browse loads |
| 2 | Browse buildings list → click on Residence Hall Alpha → view details | Building details page shows address, amenities, room types, images |
| 3 | Note the building name/details for application preference | Info noted |
| 4 | Click "Apply" or navigate to signup | Redirected to /signup or /login |
| 5 | Register as browser@test.com / Student@123 | Account created |
| 6 | Verify email (MailHog) → complete profile → upload docs | Profile complete |
| 7 | Submit application with Residence Hall Alpha as preferred building | Application submitted with building preference |
| 8 | Verify application includes the building preference | Preference recorded |

| Field | Value |
|-------|-------|
| **Postconditions** | Unauthenticated browsing → authenticated application with building preference. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-XFW-020: Academic Year Transition

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-020 |
| **Module** | Cross-Feature Workflows |
| **Priority** | High |
| **Preconditions** | Current academic year is set (e.g., "2025"). A new academic year "2026" can be created. |
| **Test Data** | Academic years: 2025 (current), 2026 (new) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin → Academic Years → verify current year is "2025" | Current year confirmed |
| 2 | Student A submits an application | Application associated with 2025 academic year |
| 3 | Tenant Admin creates new academic year "2026" → sets it as current | 2026 is now the current year |
| 4 | Student B submits a new application | Application associated with 2026 academic year |
| 5 | Verify Student A's existing application still shows 2025 | Unchanged |
| 6 | Verify the applications list can be filtered by academic year | Filter works: 2025 shows A's app, 2026 shows B's app |

| Field | Value |
|-------|-------|
| **Postconditions** | Academic year transition handled. Old applications retain original year. New applications use new year. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-XFW-021: GDPR Anonymization

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-021 |
| **Module** | Cross-Feature Workflows |
| **Priority** | High |
| **Preconditions** | Student A has gone through the full lifecycle (application, allocation, completed). Platform Admin is available. System supports anonymization. |
| **Test Data** | john.doe@test.com (Student A) — full data history |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Platform Admin → Users → find john.doe@test.com | User record displayed with full PII |
| 2 | Click "Anonymize" (or equivalent GDPR action) → Confirm | Anonymization process runs |
| 3 | Verify user record: first name, last name, email are scrubbed/hashed | PII replaced with anonymized values (e.g., "ANONYMIZED_12345") |
| 4 | Verify student profile data is scrubbed | Student number, phone, address anonymized |
| 5 | Verify application records reference anonymized data | No PII visible in application history |
| 6 | Verify documents are deleted or reference removed | Documents cleaned up |
| 7 | Attempt to log in as john.doe@test.com / Student@123 | Login fails — account no longer accessible |

| Field | Value |
|-------|-------|
| **Postconditions** | All PII scrubbed. Account inaccessible. Audit trail anonymized. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Skip if anonymization feature is not yet implemented. |

---

### TC-XFW-022: Account Deletion with Active Application

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-022 |
| **Module** | Cross-Feature Workflows |
| **Priority** | High |
| **Preconditions** | Student B has an active PENDING application and uploaded documents. |
| **Test Data** | jane.smith@test.com (Student B) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student B → verify application exists (PENDING) and documents exist | Confirmed |
| 2 | Navigate to Profile/Account → Delete Account → Confirm | Account deletion initiated |
| 3 | Verify the system handles cascading: application cancelled/deleted | Application status: CANCELLED or record deleted |
| 4 | Verify documents are deleted or disassociated | Documents cleaned up from DB and MinIO |
| 5 | Verify any allocation (if exists) is cancelled | Allocation: CANCELLED (if applicable) |
| 6 | Attempt to log in as jane.smith@test.com / Student@123 | Login fails — account deleted |
| 7 | As admin, search for jane.smith@test.com | No active user found (or shows deleted state) |

| Field | Value |
|-------|-------|
| **Postconditions** | Account deleted. All associated data handled. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Skip if self-service deletion is not implemented. Test via admin deletion if needed. |

---

### TC-XFW-023: Offer Reminder Then Accept Before Expiry

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-023 |
| **Module** | Cross-Feature Workflows |
| **Priority** | High |
| **Preconditions** | Student A has OFFERED allocation with expiry 23 hours out. Offer reminder job active. |
| **Test Data** | john.doe@test.com (Student A), offer_expiry_date = 23h from now |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Tenant Admin allocates Student A to Room A-101 → OFFERED | Allocation created |
| 2 | Via Swagger, set offerExpiryDate to 23 hours from now | Updated |
| 3 | Wait for offer reminder job to run | Reminder email sent |
| 4 | Check MailHog → find reminder email to john.doe@test.com | "Action Required: Offer Expiring Soon" email received |
| 5 | Log in as Student A → Allocations → Accept the offer | Status: ACCEPTED |
| 6 | Wait for offer expiry job to run (even though expiry time hasn't passed yet) | Job does NOT expire this allocation (status is ACCEPTED, not OFFERED) |
| 7 | Verify allocation remains ACCEPTED | Status unchanged |

| Field | Value |
|-------|-------|
| **Postconditions** | Student received reminder, accepted in time. Allocation proceeds normally. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-XFW-024: Waiting List Rescore After Rule Change

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-024 |
| **Module** | Cross-Feature Workflows |
| **Priority** | Medium |
| **Preconditions** | Students A, B, C are on the waiting list (AWAITING_ALLOCATION). Allocation rules exist with configurable weights. |
| **Test Data** | Students A, B, C. Allocation rules with adjustable weights (e.g., distance, year of study, disability). |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin → Allocation Rules → note current rule weights | Weights recorded (e.g., year_of_study=50, distance=30, disability=20) |
| 2 | View waiting list → note current student order/scores | Order: e.g., A(80), B(70), C(60) |
| 3 | Change rule weights: set year_of_study=20, distance=60, disability=20 → Save | Rules updated |
| 4 | Trigger rescore (if manual rescore button exists) or wait for batch job | Students rescored with new weights |
| 5 | View waiting list → verify order has changed | New order reflects distance-weighted scoring (e.g., C(85), A(65), B(55)) |
| 6 | Verify individual student scores match expected calculation | Scores consistent with new weights |

| Field | Value |
|-------|-------|
| **Postconditions** | Waiting list reordered based on new rule weights. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-XFW-025: Full Multi-Student Flow — 3 Students End to End

| Field | Value |
|-------|-------|
| **ID** | TC-XFW-025 |
| **Module** | Cross-Feature Workflows |
| **Priority** | Critical |
| **Preconditions** | Students A, B, C have verified accounts with complete profiles and approved documents. Rooms A-101 (ANY), B-201 (FEMALE_ONLY), C-301 (MALE_ONLY) have capacity. |
| **Test Data** | Student A (john.doe@test.com, male, UCT), Student B (jane.smith@test.com, female, Stellenbosch), Student C (alex.jones@test.com, male, Wits) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Student A logs in → submits application with preferences | Application A: PENDING |
| 2 | Student B logs in → submits application with preferences | Application B: PENDING |
| 3 | Student C logs in → submits application with preferences | Application C: PENDING |
| 4 | Tenant Admin → Applications → move all three to UNDER_REVIEW | A, B, C: UNDER_REVIEW |
| 5 | Approve all documents for all three students | All docs APPROVED |
| 6 | Approve all three applications | A, B, C: APPROVED |
| 7 | Move all three to AWAITING_ALLOCATION | A, B, C: AWAITING_ALLOCATION |
| 8 | Propose batch allocation | Staged allocations generated: B→B-201 (female), A→A-101 or C-301, C→C-301 or A-101 |
| 9 | Reject Student C's staged allocation | C's staged allocation removed |
| 10 | Confirm Student A and Student B's staged allocations | A: OFFERED (A-101), B: OFFERED (B-201) |
| 11 | Verify Student C remains AWAITING_ALLOCATION | C still in pool |
| 12 | Student A accepts offer | A: ACCEPTED |
| 13 | Student B accepts offer | B: ACCEPTED |
| 14 | Tenant Admin individually allocates Student C to Room C-301 | C: OFFERED (C-301) |
| 15 | Student C accepts offer | C: ACCEPTED |
| 16 | Tenant Admin checks in Student A | A: ACTIVE |
| 17 | Tenant Admin checks in Student B | B: ACTIVE |
| 18 | Tenant Admin checks in Student C | C: ACTIVE |
| 19 | Verify room occupancies: A-101=1, B-201=1, C-301=1 | Occupancies correct |
| 20 | Check MailHog → verify each student received all relevant emails | All notification emails present for all three students |

| Field | Value |
|-------|-------|
| **Postconditions** | All 3 students allocated and checked in. Rooms occupied. Full email trail. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |
