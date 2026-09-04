# Allocation System — UAT Test Cases

> **Module:** Allocation System
> **Frontend Route (Admin):** `http://localhost:4200/admin/room-allocations`
> **Frontend Route (Student):** `http://localhost:4200/applications/:id`
> **Email Verification:** MailHog at `http://localhost:8025`
> **Total Test Cases:** 66

## Test Data Reference

| Item | Value |
|------|-------|
| **Student A** | john.doe@test.com / Student@123 (male, UCT, 1st year, no accessibility needs) |
| **Student B** | jane.smith@test.com / Student@123 (female, Stellenbosch, 2nd year) |
| **Platform Admin** | platformadmin@test.com / Admin@12345 |
| **Room A-101** | Building A, capacity 2, ANY gender, accessible |
| **Room A-102** | Building A, capacity 1, not accessible |
| **Room B-201** | Building B, capacity 2, FEMALE_ONLY |
| **Room C-301** | Building C, capacity 3, MALE_ONLY |
| **Room C-302** | Building C, maintenance status |

**Allocation Statuses:** OFFERED, ACCEPTED, DECLINED, EXPIRED, CHECK_IN_EXPIRED, ACTIVE, COMPLETED, TERMINATED, CANCELLED

**Valid Transitions:** OFFERED → ACCEPTED / DECLINED / EXPIRED / CANCELLED · ACCEPTED → ACTIVE / CHECK_IN_EXPIRED / CANCELLED · ACTIVE → COMPLETED / TERMINATED / CANCELLED

**Terminal Statuses:** DECLINED, EXPIRED, CHECK_IN_EXPIRED, COMPLETED, TERMINATED, CANCELLED

**Decline Reasons:** WRONG_LOCATION, TOO_EXPENSIVE, FOUND_ALTERNATIVE, ACCESSIBILITY_ISSUES, PERSONAL_REASONS, OTHER

---

## A. Admin — Manual Allocation

---

### TC-ALC-001: Allocate room to student with valid data

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-001 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Admin logged in. Student A exists with an approved application. Room A-101 has available capacity. |
| **Test Data** | Student A (john.doe@test.com), Room A-101, allocationDate = today |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/room-allocations` | Room allocations list page loads |
| 2 | Click "Allocate" button | AllocateDialog opens with studentId, roomId, applicationId, allocationDate fields |
| 3 | Enter Student A's ID in studentId field | Field accepts the value |
| 4 | Select Room A-101 from roomId dropdown | Room selected |
| 5 | Verify allocationDate defaults to today | Date field pre-populated with today's date |
| 6 | Click "Allocate" / submit | Success message displayed. Dialog closes. |
| 7 | Verify allocation appears in the list with status OFFERED | New row visible: Student A, Room A-101, status = OFFERED |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation record created with status OFFERED. Room occupancy incremented by 1. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-002: Allocate with empty studentId — validation error

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-002 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Admin logged in. AllocateDialog open. |
| **Test Data** | studentId = empty, Room A-101, allocationDate = today |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open AllocateDialog | Dialog displayed |
| 2 | Leave studentId field empty | Field empty |
| 3 | Select Room A-101 | Room selected |
| 4 | Click "Allocate" | Validation error on studentId: required field. Form does not submit. |

| Field | Value |
|-------|-------|
| **Postconditions** | No allocation created. Dialog remains open with error. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | studentId has min value of 1 |

---

### TC-ALC-003: Allocate without selecting room — validation error

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-003 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Admin logged in. AllocateDialog open. |
| **Test Data** | Student A, roomId = empty, allocationDate = today |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open AllocateDialog | Dialog displayed |
| 2 | Enter Student A's ID | Field populated |
| 3 | Leave roomId unselected | No room chosen |
| 4 | Click "Allocate" | Validation error on roomId: required field. Form does not submit. |

| Field | Value |
|-------|-------|
| **Postconditions** | No allocation created. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-004: Allocate without allocationDate — validation error

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-004 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Admin logged in. AllocateDialog open. |
| **Test Data** | Student A, Room A-101, allocationDate = cleared/empty |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open AllocateDialog | Dialog displayed with allocationDate defaulting to today |
| 2 | Clear the allocationDate field | Field is blank |
| 3 | Fill studentId and roomId | Both populated |
| 4 | Click "Allocate" | Validation error on allocationDate: required field. Form does not submit. |

| Field | Value |
|-------|-------|
| **Postconditions** | No allocation created. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-005: Allocate to room at full capacity — error

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-005 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Admin logged in. Room A-102 (capacity 1) already has 1 active allocation. |
| **Test Data** | Student A, Room A-102 (full) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open AllocateDialog | Dialog displayed |
| 2 | Enter Student A's ID and select Room A-102 | Fields populated |
| 3 | Click "Allocate" | Error message: room is at full capacity. Allocation not created. |

| Field | Value |
|-------|-------|
| **Postconditions** | No allocation created. Room occupancy unchanged. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-006: Allocate to room in MAINTENANCE status — error

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-006 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Admin logged in. Room C-302 is in MAINTENANCE status. |
| **Test Data** | Student A, Room C-302 (maintenance) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open AllocateDialog | Dialog displayed |
| 2 | Enter Student A's ID and select Room C-302 | Fields populated |
| 3 | Click "Allocate" | Error message: room is under maintenance and unavailable. Allocation not created. |

| Field | Value |
|-------|-------|
| **Postconditions** | No allocation created. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Room C-302 may not appear in room dropdown if filtered by status |

---

### TC-ALC-007: Verify allocation email sent to student via MailHog

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-007 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Allocation successfully created for Student A (TC-ALC-001 passed). MailHog running. |
| **Test Data** | Student A (john.doe@test.com), Room A-101 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to MailHog UI at `http://localhost:8025` | MailHog inbox loads |
| 2 | Search for emails to john.doe@test.com | Email(s) listed |
| 3 | Open the most recent email | Email subject contains "Room Allocation Confirmation" or similar |
| 4 | Verify email body contains room details (A-101), building name, allocation date | Room and building information present in email body |
| 5 | Verify email body contains instructions for accepting/declining the offer | Accept/decline instructions present |

| Field | Value |
|-------|-------|
| **Postconditions** | Email delivered and verified in MailHog. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-008: Allocate by application ID — allocation linked to application

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-008 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Admin logged in. Student A has an approved application (e.g. appId=100). Room A-101 available. |
| **Test Data** | Student A, Room A-101, applicationId = 100 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open AllocateDialog | Dialog displayed |
| 2 | Enter Student A's ID, select Room A-101 | Fields populated |
| 3 | Enter applicationId = 100 in the optional applicationId field | Field populated |
| 4 | Click "Allocate" | Allocation created successfully |
| 5 | View allocation details | Allocation shows linked applicationId = 100 |
| 6 | Navigate to the application detail page for appId 100 | Application shows linked allocation |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation linked to application record. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | applicationId is optional in AllocateDialog |

---

## B. Eligibility Checks

---

### TC-ALC-009: Male student to FEMALE_ONLY room — blocked

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-009 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Admin logged in. Student A is male. Room B-201 is FEMALE_ONLY. |
| **Test Data** | Student A (male), Room B-201 (FEMALE_ONLY) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open AllocateDialog | Dialog displayed |
| 2 | Enter Student A's ID and select Room B-201 | Fields populated |
| 3 | Click "Allocate" | Error: gender restriction violated — male student cannot be allocated to FEMALE_ONLY room |

| Field | Value |
|-------|-------|
| **Postconditions** | No allocation created. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-010: Female student to MALE_ONLY room — blocked

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-010 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Admin logged in. Student B is female. Room C-301 is MALE_ONLY. |
| **Test Data** | Student B (female), Room C-301 (MALE_ONLY) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open AllocateDialog | Dialog displayed |
| 2 | Enter Student B's ID and select Room C-301 | Fields populated |
| 3 | Click "Allocate" | Error: gender restriction violated — female student cannot be allocated to MALE_ONLY room |

| Field | Value |
|-------|-------|
| **Postconditions** | No allocation created. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-011: Male student to ANY gender room — success

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-011 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Admin logged in. Student A is male. Room A-101 is ANY gender with available capacity. |
| **Test Data** | Student A (male), Room A-101 (ANY gender) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open AllocateDialog | Dialog displayed |
| 2 | Enter Student A's ID and select Room A-101 | Fields populated |
| 3 | Click "Allocate" | Allocation created successfully with status OFFERED |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation created. No gender restriction error. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-012: Female student to ANY gender room — success

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-012 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Admin logged in. Student B is female. Room A-101 is ANY gender with available capacity. |
| **Test Data** | Student B (female), Room A-101 (ANY gender) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open AllocateDialog | Dialog displayed |
| 2 | Enter Student B's ID and select Room A-101 | Fields populated |
| 3 | Click "Allocate" | Allocation created successfully with status OFFERED |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation created. No gender restriction error. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-013: Student needing accessibility to non-accessible room — warning/blocked

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-013 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Admin logged in. A student with accessibility needs exists. Room A-102 is not accessible. |
| **Test Data** | Student with accessibility needs, Room A-102 (not accessible) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open AllocateDialog | Dialog displayed |
| 2 | Enter the accessibility-needing student's ID and select Room A-102 | Fields populated |
| 3 | Click "Allocate" | Warning or error: room does not meet student's accessibility requirements. Allocation blocked or warning shown requiring confirmation. |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation blocked or requires admin override. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Behavior may be a hard block or a soft warning depending on implementation |

---

### TC-ALC-014: Student needing accessibility to accessible room — success

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-014 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Admin logged in. A student with accessibility needs exists. Room A-101 is accessible. |
| **Test Data** | Student with accessibility needs, Room A-101 (accessible) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open AllocateDialog | Dialog displayed |
| 2 | Enter the student's ID and select Room A-101 | Fields populated |
| 3 | Click "Allocate" | Allocation created successfully. No accessibility warning. |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation created with status OFFERED. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-015: 1st year student to room with minYearOfStudy=2 — blocked

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-015 |
| **Module** | Allocation System |
| **Priority** | Medium |
| **Preconditions** | Admin logged in. Student A is 1st year. Target room has minYearOfStudy=2 configured. |
| **Test Data** | Student A (1st year), room with minYearOfStudy=2 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open AllocateDialog | Dialog displayed |
| 2 | Enter Student A's ID and select the restricted room | Fields populated |
| 3 | Click "Allocate" | Error: student's year of study (1) does not meet the minimum requirement (2) for this room |

| Field | Value |
|-------|-------|
| **Postconditions** | No allocation created. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Requires room with year-of-study restrictions configured |

---

### TC-ALC-016: 2nd year student to room with minYearOfStudy=1, maxYearOfStudy=4 — success

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-016 |
| **Module** | Allocation System |
| **Priority** | Medium |
| **Preconditions** | Admin logged in. Student B is 2nd year. Target room has minYearOfStudy=1, maxYearOfStudy=4. |
| **Test Data** | Student B (2nd year), room with year range 1–4 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open AllocateDialog | Dialog displayed |
| 2 | Enter Student B's ID and select the room | Fields populated |
| 3 | Click "Allocate" | Allocation created successfully. No year-of-study restriction error. |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation created with status OFFERED. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-017: Student from UCT to building linked to Stellenbosch only — blocked

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-017 |
| **Module** | Allocation System |
| **Priority** | Medium |
| **Preconditions** | Admin logged in. Student A is from UCT. Target building is linked to Stellenbosch institution only. |
| **Test Data** | Student A (UCT), room in Stellenbosch-only building |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open AllocateDialog | Dialog displayed |
| 2 | Enter Student A's ID and select a room in the Stellenbosch-only building | Fields populated |
| 3 | Click "Allocate" | Error: student's institution (UCT) is not eligible for this building |

| Field | Value |
|-------|-------|
| **Postconditions** | No allocation created. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Requires building with institution restriction configured |

---

### TC-ALC-018: Student from UCT to building linked to UCT — success

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-018 |
| **Module** | Allocation System |
| **Priority** | Medium |
| **Preconditions** | Admin logged in. Student A is from UCT. Target building is linked to UCT. Room has capacity. |
| **Test Data** | Student A (UCT), room in UCT-linked building |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open AllocateDialog | Dialog displayed |
| 2 | Enter Student A's ID and select a room in the UCT-linked building | Fields populated |
| 3 | Click "Allocate" | Allocation created successfully. No institution restriction error. |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation created with status OFFERED. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-019: Room availability check — shows current/max occupancy

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-019 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Admin logged in. Room A-101 has capacity 2 with 1 current allocation. |
| **Test Data** | Room A-101 (capacity 2, 1 occupied) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open AllocateDialog | Dialog displayed |
| 2 | Select Room A-101 from the roomId dropdown | Room selected |
| 3 | Observe room availability information | Current occupancy (1) and max capacity (2) displayed. Room shown as available. |
| 4 | Alternatively, check room list/details page | Room A-101 shows 1/2 occupancy |

| Field | Value |
|-------|-------|
| **Postconditions** | N/A — read-only verification. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## C. Student — Accept Offer

---

### TC-ALC-020: Student views allocation offer in application details

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-020 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Student A logged in. An allocation with status OFFERED exists for Student A linked to an application. |
| **Test Data** | Student A (john.doe@test.com / Student@123), allocation in OFFERED status |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A | Dashboard loads |
| 2 | Navigate to `/applications/:id` for the linked application | Application detail page loads |
| 3 | Locate the allocation/offer section | Offer details displayed: room number, building name, allocation date, offer expiry date |
| 4 | Verify Accept and Decline buttons are visible | Both action buttons present and enabled |

| Field | Value |
|-------|-------|
| **Postconditions** | Student can see offer and action buttons. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-021: Student accepts offer — OFFERED to ACCEPTED

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-021 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Student A logged in. Allocation in OFFERED status visible on application page. |
| **Test Data** | Student A, allocation in OFFERED status |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to the application details page with the offer | Offer section visible with Accept button |
| 2 | Click "Accept" button | Confirmation dialog appears |
| 3 | Confirm acceptance | Success message displayed |
| 4 | Verify allocation status changes to ACCEPTED | Status shown as ACCEPTED on the page |
| 5 | Check MailHog for acceptance confirmation email | Email received at john.doe@test.com confirming acceptance |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation status = ACCEPTED. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-022: Verify acceptance confirmation shown

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-022 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Student A just accepted offer (TC-ALC-021). |
| **Test Data** | Student A, allocation in ACCEPTED status |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Observe UI after accepting the offer | Success snackbar or confirmation banner displayed |
| 2 | Verify the confirmation includes room details | Room number and building shown in confirmation |
| 3 | Verify Accept/Decline buttons are no longer visible | Action buttons removed or disabled for ACCEPTED status |

| Field | Value |
|-------|-------|
| **Postconditions** | Confirmation visible. No further accept/decline actions available. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-023: After acceptance, offer expiry countdown no longer shown

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-023 |
| **Module** | Allocation System |
| **Priority** | Medium |
| **Preconditions** | Student A has accepted the offer. Allocation status = ACCEPTED. |
| **Test Data** | Student A, allocation in ACCEPTED status |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to the application details page | Page loads with allocation info |
| 2 | Look for offer expiry countdown (OfferExpiryComponent) | Countdown timer is NOT displayed for ACCEPTED allocations |
| 3 | Verify status shows ACCEPTED without urgency indicators | Status displayed cleanly without expiry warnings |

| Field | Value |
|-------|-------|
| **Postconditions** | No expiry countdown visible for accepted offers. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | OfferExpiryComponent should only render for OFFERED status |

---

## D. Student — Decline Offer

---

### TC-ALC-024: Decline with reason WRONG_LOCATION

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-024 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Student A logged in. Allocation in OFFERED status. |
| **Test Data** | Student A, decline reason = WRONG_LOCATION |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to application details with the offer | Offer visible with Decline button |
| 2 | Click "Decline" button | Decline dialog opens with reason selection |
| 3 | Select "WRONG_LOCATION" as decline reason | Reason selected |
| 4 | Submit decline | Success message. Allocation status changes to DECLINED. |
| 5 | Verify decline reason stored | Decline reason = WRONG_LOCATION visible in allocation details (admin view) |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation status = DECLINED. Decline reason = WRONG_LOCATION. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-025: Decline with reason TOO_EXPENSIVE

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-025 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Student logged in. Allocation in OFFERED status. |
| **Test Data** | Decline reason = TOO_EXPENSIVE |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to application details with the offer | Offer visible |
| 2 | Click "Decline" button | Decline dialog opens |
| 3 | Select "TOO_EXPENSIVE" as decline reason | Reason selected |
| 4 | Submit decline | Success. Status = DECLINED. Reason = TOO_EXPENSIVE. |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation status = DECLINED. Decline reason = TOO_EXPENSIVE. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-026: Decline with reason FOUND_ALTERNATIVE

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-026 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Student logged in. Allocation in OFFERED status. |
| **Test Data** | Decline reason = FOUND_ALTERNATIVE |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to application details with the offer | Offer visible |
| 2 | Click "Decline" → select "FOUND_ALTERNATIVE" → submit | Status = DECLINED. Reason = FOUND_ALTERNATIVE. |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation status = DECLINED. Decline reason = FOUND_ALTERNATIVE. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-027: Decline with reason ACCESSIBILITY_ISSUES

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-027 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Student logged in. Allocation in OFFERED status. |
| **Test Data** | Decline reason = ACCESSIBILITY_ISSUES |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to application details with the offer | Offer visible |
| 2 | Click "Decline" → select "ACCESSIBILITY_ISSUES" → submit | Status = DECLINED. Reason = ACCESSIBILITY_ISSUES. |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation status = DECLINED. Decline reason = ACCESSIBILITY_ISSUES. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-028: Decline with reason PERSONAL_REASONS

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-028 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Student logged in. Allocation in OFFERED status. |
| **Test Data** | Decline reason = PERSONAL_REASONS |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to application details with the offer | Offer visible |
| 2 | Click "Decline" → select "PERSONAL_REASONS" → submit | Status = DECLINED. Reason = PERSONAL_REASONS. |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation status = DECLINED. Decline reason = PERSONAL_REASONS. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-029: Decline with reason OTHER and free text

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-029 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Student logged in. Allocation in OFFERED status. |
| **Test Data** | Decline reason = OTHER, freeText = "I am deferring my studies to next semester." |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to application details with the offer | Offer visible |
| 2 | Click "Decline" button | Decline dialog opens |
| 3 | Select "OTHER" as decline reason | Free text field appears or becomes enabled |
| 4 | Enter "I am deferring my studies to next semester." in the free text field | Text entered |
| 5 | Submit decline | Status = DECLINED. Reason = OTHER with note stored. |
| 6 | Admin views allocation details | Decline reason = OTHER with free text note visible |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation status = DECLINED. Free text note persisted. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-030: After decline — student requeued on waiting list

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-030 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Student A has declined an offer. Student has not exceeded max declines. |
| **Test Data** | Student A, first decline |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Student A declines the offer (any reason) | Allocation status = DECLINED |
| 2 | Verify student is placed back on the waiting list | Student's application remains active and visible in the allocation queue |
| 3 | Check MailHog for requeue email | Email to john.doe@test.com with subject containing "Room Offer Declined - You Remain in the Queue" or similar |
| 4 | Admin views waiting list | Student A appears on the waiting list for reallocation |

| Field | Value |
|-------|-------|
| **Postconditions** | Student requeued. Email sent. Application still active. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-031: After max declines reached — application cancelled

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-031 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Student A has declined offers up to the maximum allowed number minus one. One more decline will trigger cancellation. |
| **Test Data** | Student A, at max decline threshold |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Student A declines the final offer | Allocation status = DECLINED |
| 2 | Verify application status changes to CANCELLED | Application no longer active |
| 3 | Check MailHog for cancellation email | Email to john.doe@test.com with subject containing "Application Cancelled - Maximum Offer Declines Reached" or similar |
| 4 | Student views their applications | Application shows as CANCELLED. No further offers will be made. |

| Field | Value |
|-------|-------|
| **Postconditions** | Application cancelled. Student removed from allocation queue. Email sent. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Max decline count is a system setting |

---

## E. Valid Status Transitions

---

### TC-ALC-032: OFFERED → ACCEPTED (student accepts)

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-032 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Allocation exists with status OFFERED for Student A. |
| **Test Data** | Student A, allocation in OFFERED status |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A | Dashboard loads |
| 2 | Navigate to application with offer | Offer visible |
| 3 | Accept the offer | Allocation status transitions to ACCEPTED |
| 4 | Admin verifies status in `/admin/room-allocations` | Status = ACCEPTED |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation status = ACCEPTED. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-033: OFFERED → DECLINED (student declines)

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-033 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Allocation exists with status OFFERED for Student A. |
| **Test Data** | Student A, allocation in OFFERED status |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A | Dashboard loads |
| 2 | Navigate to application with offer | Offer visible |
| 3 | Decline the offer with a reason | Allocation status transitions to DECLINED |
| 4 | Admin verifies status | Status = DECLINED with decline reason recorded |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation status = DECLINED (terminal). |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-034: OFFERED → EXPIRED (offer expiry)

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-034 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Allocation exists with status OFFERED. Offer expiry date is in the past or system clock is advanced past expiry. |
| **Test Data** | Allocation with expired offer date |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Wait for offer expiry date to pass (or verify an allocation with past expiry) | System detects expired offer |
| 2 | Verify allocation status changes to EXPIRED | Status = EXPIRED |
| 3 | Student views the application | Offer no longer actionable. Accept/Decline buttons not available. |
| 4 | Check MailHog for expiry notification | Email sent to student about expired offer |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation status = EXPIRED (terminal). Room capacity freed. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | May require scheduled job to run or manual DB setup |

---

### TC-ALC-035: OFFERED → CANCELLED (admin cancels)

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-035 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Admin logged in. Allocation exists with status OFFERED. |
| **Test Data** | Allocation in OFFERED status |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/room-allocations` | Allocations list loads |
| 2 | Locate the OFFERED allocation | Found in list |
| 3 | Click Cancel action for the allocation | Confirmation dialog appears |
| 4 | Confirm cancellation | Allocation status transitions to CANCELLED |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation status = CANCELLED (terminal). |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-036: ACCEPTED → ACTIVE (admin check-in)

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-036 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Admin logged in. Allocation exists with status ACCEPTED. |
| **Test Data** | Allocation in ACCEPTED status |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/room-allocations` | Allocations list loads |
| 2 | Locate the ACCEPTED allocation | Found in list |
| 3 | Click Check-in action | Confirmation dialog or action triggered |
| 4 | Confirm check-in | Allocation status transitions to ACTIVE. Check-in date recorded. |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation status = ACTIVE. Check-in timestamp stored. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-037: ACCEPTED → CHECK_IN_EXPIRED (check-in deadline passed)

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-037 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Allocation in ACCEPTED status. Check-in deadline has passed without student checking in. |
| **Test Data** | Allocation with past check-in deadline |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Verify allocation with past check-in deadline exists | Allocation found |
| 2 | Wait for system scheduled job to process expired check-ins (or verify after job runs) | System detects missed check-in |
| 3 | Verify allocation status changes to CHECK_IN_EXPIRED | Status = CHECK_IN_EXPIRED |
| 4 | Check MailHog for notification | Email sent to student about expired check-in |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation status = CHECK_IN_EXPIRED (terminal). Room freed. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | May require scheduled job execution |

---

### TC-ALC-038: ACCEPTED → CANCELLED (admin cancels)

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-038 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Admin logged in. Allocation exists with status ACCEPTED. |
| **Test Data** | Allocation in ACCEPTED status |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/room-allocations` | Allocations list loads |
| 2 | Locate the ACCEPTED allocation | Found in list |
| 3 | Click Cancel action and confirm | Allocation status transitions to CANCELLED |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation status = CANCELLED (terminal). |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-039: ACTIVE → COMPLETED (admin check-out / end allocation)

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-039 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Admin logged in. Allocation exists with status ACTIVE. |
| **Test Data** | Allocation in ACTIVE status |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/room-allocations` | Allocations list loads |
| 2 | Locate the ACTIVE allocation | Found in list |
| 3 | Click Check-out / Complete action | Confirmation dialog appears |
| 4 | Confirm | Allocation status transitions to COMPLETED. Check-out date recorded. |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation status = COMPLETED (terminal). Room capacity freed. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-040: ACTIVE → TERMINATED (admin terminates)

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-040 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Admin logged in. Allocation exists with status ACTIVE. |
| **Test Data** | Allocation in ACTIVE status |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/room-allocations` | Allocations list loads |
| 2 | Locate the ACTIVE allocation | Found in list |
| 3 | Click Terminate action | Confirmation dialog or reason input appears |
| 4 | Confirm termination | Allocation status transitions to TERMINATED |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation status = TERMINATED (terminal). Room capacity freed. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-041: ACTIVE → CANCELLED (admin cancels)

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-041 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Admin logged in. Allocation exists with status ACTIVE. |
| **Test Data** | Allocation in ACTIVE status |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/room-allocations` | Allocations list loads |
| 2 | Locate the ACTIVE allocation | Found in list |
| 3 | Click Cancel action and confirm | Allocation status transitions to CANCELLED |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation status = CANCELLED (terminal). Room capacity freed. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## F. Invalid Status Transitions

---

### TC-ALC-042: DECLINED → any transition — blocked (terminal)

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-042 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Allocation exists with status DECLINED. |
| **Test Data** | Allocation in DECLINED status |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/room-allocations` | Allocations list loads |
| 2 | Locate the DECLINED allocation | Found in list |
| 3 | Attempt to change status (check-in, activate, cancel, etc.) | No transition actions available. Status change buttons disabled or absent. |
| 4 | If any action is attempted via API | Error: invalid status transition from DECLINED |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation remains in DECLINED status. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | DECLINED is a terminal status |

---

### TC-ALC-043: EXPIRED → any transition — blocked (terminal)

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-043 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Allocation exists with status EXPIRED. |
| **Test Data** | Allocation in EXPIRED status |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate the EXPIRED allocation in admin view | Found |
| 2 | Attempt any status transition | No actions available. Blocked as terminal status. |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation remains in EXPIRED status. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-044: CHECK_IN_EXPIRED → any transition — blocked (terminal)

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-044 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Allocation exists with status CHECK_IN_EXPIRED. |
| **Test Data** | Allocation in CHECK_IN_EXPIRED status |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate the CHECK_IN_EXPIRED allocation in admin view | Found |
| 2 | Attempt any status transition | No actions available. Blocked as terminal status. |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation remains in CHECK_IN_EXPIRED status. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-045: COMPLETED → any transition — blocked (terminal)

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-045 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Allocation exists with status COMPLETED. |
| **Test Data** | Allocation in COMPLETED status |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate the COMPLETED allocation in admin view | Found |
| 2 | Attempt any status transition | No actions available. Blocked as terminal status. |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation remains in COMPLETED status. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-046: TERMINATED → any transition — blocked (terminal)

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-046 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Allocation exists with status TERMINATED. |
| **Test Data** | Allocation in TERMINATED status |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate the TERMINATED allocation in admin view | Found |
| 2 | Attempt any status transition | No actions available. Blocked as terminal status. |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation remains in TERMINATED status. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-047: CANCELLED → any transition — blocked (terminal)

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-047 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Allocation exists with status CANCELLED. |
| **Test Data** | Allocation in CANCELLED status |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate the CANCELLED allocation in admin view | Found |
| 2 | Attempt any status transition | No actions available. Blocked as terminal status. |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation remains in CANCELLED status. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-048: OFFERED → ACTIVE — blocked (must accept first)

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-048 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Admin logged in. Allocation exists with status OFFERED. |
| **Test Data** | Allocation in OFFERED status |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate the OFFERED allocation in admin view | Found |
| 2 | Attempt to check-in / activate the allocation directly | Action not available or error: cannot transition from OFFERED to ACTIVE — must be ACCEPTED first |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation remains in OFFERED status. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Valid path: OFFERED → ACCEPTED → ACTIVE |

---

### TC-ALC-049: ACCEPTED → COMPLETED — blocked (must check in first)

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-049 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Admin logged in. Allocation exists with status ACCEPTED. |
| **Test Data** | Allocation in ACCEPTED status |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate the ACCEPTED allocation in admin view | Found |
| 2 | Attempt to check-out / complete the allocation directly | Action not available or error: cannot transition from ACCEPTED to COMPLETED — must be ACTIVE first |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation remains in ACCEPTED status. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Valid path: ACCEPTED → ACTIVE → COMPLETED |

---

## G. Admin — Check-In / Check-Out

---

### TC-ALC-050: Check-in student (ACCEPTED → ACTIVE)

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-050 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Admin logged in. Allocation in ACCEPTED status for Student A. |
| **Test Data** | Student A, allocation in ACCEPTED status |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/room-allocations` | Allocations list loads |
| 2 | Locate Student A's ACCEPTED allocation | Found |
| 3 | Click Check-in action | Check-in dialog or confirmation appears |
| 4 | Confirm check-in | Status transitions to ACTIVE. Success message shown. |
| 5 | Verify the allocation row now shows ACTIVE status | Status = ACTIVE |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation status = ACTIVE. Check-in date = today. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-051: Check-out student (ACTIVE → COMPLETED)

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-051 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Admin logged in. Allocation in ACTIVE status for Student A. |
| **Test Data** | Student A, allocation in ACTIVE status |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/room-allocations` | Allocations list loads |
| 2 | Locate Student A's ACTIVE allocation | Found |
| 3 | Click Check-out / Complete action | Confirmation appears |
| 4 | Confirm check-out | Status transitions to COMPLETED. Success message shown. |
| 5 | Verify the allocation row now shows COMPLETED status | Status = COMPLETED |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation status = COMPLETED. Check-out date recorded. Room capacity freed. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-052: Verify check-in date recorded

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-052 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Student A was checked in (TC-ALC-050 passed). |
| **Test Data** | Student A, ACTIVE allocation |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to the allocation detail/history for Student A | Detail view loads |
| 2 | Verify check-in date field | Check-in date is populated with today's date and time |
| 3 | Verify the date is not null/empty | Date value present |

| Field | Value |
|-------|-------|
| **Postconditions** | Check-in date confirmed as persisted. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-053: Verify check-out date recorded

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-053 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Student A was checked out (TC-ALC-051 passed). |
| **Test Data** | Student A, COMPLETED allocation |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to the allocation detail/history for Student A | Detail view loads |
| 2 | Verify check-out date field | Check-out date is populated with today's date and time |
| 3 | Verify both check-in and check-out dates are present | Both dates visible and chronologically ordered |

| Field | Value |
|-------|-------|
| **Postconditions** | Check-out date confirmed as persisted. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## H. Admin — Move Student

---

### TC-ALC-054: Move student to different room — success

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-054 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | Admin logged in. Student A has an ACTIVE allocation in Room A-101. Room C-301 (MALE_ONLY) has available capacity. |
| **Test Data** | Student A (male), source = Room A-101, target = Room C-301 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/room-allocations` | Allocations list loads |
| 2 | Locate Student A's ACTIVE allocation | Found |
| 3 | Click Move action | AllocationMoveDialog opens |
| 4 | Select Room C-301 as the target room | Room selected |
| 5 | Confirm move | Success message. Allocation updated to Room C-301. |
| 6 | Verify Room A-101 occupancy decreased | Occupancy decremented |
| 7 | Verify Room C-301 occupancy increased | Occupancy incremented |
| 8 | Verify audit log entry created | Move action logged with source and target rooms |

| Field | Value |
|-------|-------|
| **Postconditions** | Student A now in Room C-301. Audit log records the move. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-055: Move to full room — blocked

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-055 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Admin logged in. Student A has ACTIVE allocation. Target room is at full capacity. |
| **Test Data** | Student A, target room at capacity |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open AllocationMoveDialog for Student A | Dialog opens |
| 2 | Select a room that is at full capacity | Room selected |
| 3 | Confirm move | Error: target room is at full capacity. Move not executed. |

| Field | Value |
|-------|-------|
| **Postconditions** | Student remains in original room. No change. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-056: Move to room violating gender restriction — blocked

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-056 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Admin logged in. Student A (male) has ACTIVE allocation. Room B-201 is FEMALE_ONLY. |
| **Test Data** | Student A (male), target = Room B-201 (FEMALE_ONLY) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open AllocationMoveDialog for Student A | Dialog opens |
| 2 | Select Room B-201 (FEMALE_ONLY) | Room selected |
| 3 | Confirm move | Error: gender restriction violated. Move not executed. |

| Field | Value |
|-------|-------|
| **Postconditions** | Student remains in original room. No change. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-057: Verify email sent to student after room move

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-057 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Student A was successfully moved to a new room (TC-ALC-054 passed). |
| **Test Data** | Student A (john.doe@test.com) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to MailHog at `http://localhost:8025` | Inbox loads |
| 2 | Search for emails to john.doe@test.com | Email(s) listed |
| 3 | Open the most recent email | Subject contains "Room Change Notification" or similar |
| 4 | Verify email body contains old room, new room, and building details | Move details present |

| Field | Value |
|-------|-------|
| **Postconditions** | Email delivered and verified. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## I. Admin — Override Allocation

---

### TC-ALC-058: Override constraints to force allocate

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-058 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Admin logged in. Student A (male) would normally be blocked from Room B-201 (FEMALE_ONLY). Override/force option available. |
| **Test Data** | Student A (male), Room B-201 (FEMALE_ONLY), constraint override enabled |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open AllocateDialog | Dialog displayed |
| 2 | Enter Student A's ID and select Room B-201 | Fields populated |
| 3 | Enable the override/force-allocate option (if available as checkbox or toggle) | Override enabled |
| 4 | Click "Allocate" | Allocation created despite gender restriction. Success message with override warning. |
| 5 | Verify audit log shows constraintBypassed=true | Audit entry records the override |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation created with override flag. Audit trail records constraint bypass. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Only PLATFORM_ADMIN may have override capability |

---

### TC-ALC-059: Verify override appears in allocation history

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-059 |
| **Module** | Allocation System |
| **Priority** | Medium |
| **Preconditions** | Override allocation created (TC-ALC-058 passed). |
| **Test Data** | Overridden allocation from TC-ALC-058 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to allocation detail or history view | Detail loads |
| 2 | Check for override indicator | Override/constraint-bypassed flag visible |
| 3 | View audit log / activity log | Entry shows allocation was created with constraint override, including admin user, timestamp, and bypassed constraint type |

| Field | Value |
|-------|-------|
| **Postconditions** | Override is traceable in audit trail. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## J. Admin — Search & Filter

---

### TC-ALC-060: Search allocations by student name

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-060 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Admin logged in. Multiple allocations exist. Student A has at least one allocation. |
| **Test Data** | Search term = "John" or "john.doe" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/room-allocations` | Allocations list loads |
| 2 | Enter "John" in the search field | Search input accepted |
| 3 | Submit search / observe auto-filter | Results filtered to show only allocations for students matching "John" |
| 4 | Verify Student A's allocation(s) appear | john.doe@test.com allocations visible |
| 5 | Verify non-matching allocations are hidden | Other students not shown |

| Field | Value |
|-------|-------|
| **Postconditions** | Filtered results displayed. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-061: Filter allocations by status

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-061 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Admin logged in. Allocations exist in multiple statuses (OFFERED, ACCEPTED, ACTIVE, etc.). |
| **Test Data** | Filter = ACTIVE |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/room-allocations` | Allocations list loads |
| 2 | Open status filter dropdown | Filter options displayed with all 9 statuses |
| 3 | Select "ACTIVE" | Filter applied |
| 4 | Verify only ACTIVE allocations are displayed | All visible rows have status = ACTIVE |
| 5 | Change filter to "OFFERED" | Results update to show only OFFERED allocations |

| Field | Value |
|-------|-------|
| **Postconditions** | Filtered results match selected status. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-062: View allocation history / audit trail

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-062 |
| **Module** | Allocation System |
| **Priority** | Medium |
| **Preconditions** | Admin logged in. An allocation exists that has undergone multiple status transitions. |
| **Test Data** | Allocation that went OFFERED → ACCEPTED → ACTIVE |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to the allocation detail view | Detail page loads |
| 2 | Locate the history / audit trail section | History visible |
| 3 | Verify each status transition is recorded with timestamp and user | Entries: Created (OFFERED), Accepted, Checked-in (ACTIVE) — each with date and actor |

| Field | Value |
|-------|-------|
| **Postconditions** | Audit trail verified. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ALC-063: Pagination through allocations

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-063 |
| **Module** | Allocation System |
| **Priority** | Medium |
| **Preconditions** | Admin logged in. More allocations exist than fit on one page (e.g. >10 if page size is 10). |
| **Test Data** | 15+ allocations in the system |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/room-allocations` | First page of allocations loads |
| 2 | Verify page size and pagination controls visible | Pagination at bottom of table (page numbers, next/prev, page size selector) |
| 3 | Click "Next" or page 2 | Second page loads with different allocations |
| 4 | Click "Previous" or page 1 | Returns to first page |
| 5 | Change page size (e.g. to 25) | Table updates to show 25 rows per page |

| Field | Value |
|-------|-------|
| **Postconditions** | Pagination works correctly. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## K. Admin — Delete

---

### TC-ALC-064: Delete allocation — audit logged

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-064 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Admin logged in. An allocation exists (preferably in a non-active terminal state like CANCELLED). |
| **Test Data** | Allocation in CANCELLED status |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/room-allocations` | Allocations list loads |
| 2 | Locate the target allocation | Found |
| 3 | Click Delete action | Confirmation dialog: "Are you sure you want to delete this allocation?" |
| 4 | Confirm deletion | Allocation removed from list. Success message displayed. |
| 5 | Verify allocation no longer appears in list | Gone from results |
| 6 | Check activity log / audit trail | Deletion event logged with admin user, timestamp, and allocation details |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation deleted. Audit log entry created. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Deletion may be restricted to certain statuses |

---

## L. Offer Expiry

---

### TC-ALC-065: Verify offer with past expiry date shows as expired

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-065 |
| **Module** | Allocation System |
| **Priority** | Critical |
| **Preconditions** | An allocation exists with status OFFERED and an offer expiry date in the past. Scheduled expiry job has run. |
| **Test Data** | Allocation with offerExpiryDate < now |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Admin navigates to `/admin/room-allocations` | Allocations list loads |
| 2 | Locate the allocation with past expiry date | Found |
| 3 | Verify status = EXPIRED | Status displayed as EXPIRED (either automatically transitioned or after scheduled job) |
| 4 | Student logs in and views the application | Offer is marked as expired. No accept/decline buttons. |

| Field | Value |
|-------|-------|
| **Postconditions** | Expired offer correctly reflected in both admin and student views. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | May require waiting for scheduled batch or manual trigger |

---

### TC-ALC-066: Verify offer expiry countdown displayed to student (OfferExpiryComponent)

| Field | Value |
|-------|-------|
| **ID** | TC-ALC-066 |
| **Module** | Allocation System |
| **Priority** | High |
| **Preconditions** | Student A logged in. Allocation in OFFERED status with offer expiry date in the future (e.g. 3 days from now). |
| **Test Data** | Student A, allocation in OFFERED status, expiry = 3 days out |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A | Dashboard loads |
| 2 | Navigate to `/applications/:id` with the active offer | Application detail page loads |
| 3 | Locate the OfferExpiryComponent / countdown section | Countdown timer visible |
| 4 | Verify countdown shows remaining time (e.g. "2 days, 23 hours remaining") | Time remaining displayed accurately relative to expiry date |
| 5 | Verify urgency styling for offers close to expiry | If <24 hours, warning color/icon may be shown |

| Field | Value |
|-------|-------|
| **Postconditions** | Countdown displayed correctly. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | OfferExpiryComponent should only render for OFFERED status allocations |

---

## Summary

| Section | Test Cases | Range |
|---------|-----------|-------|
| A. Admin — Manual Allocation | 8 | TC-ALC-001 – TC-ALC-008 |
| B. Eligibility Checks | 11 | TC-ALC-009 – TC-ALC-019 |
| C. Student — Accept Offer | 4 | TC-ALC-020 – TC-ALC-023 |
| D. Student — Decline Offer | 8 | TC-ALC-024 – TC-ALC-031 |
| E. Valid Status Transitions | 10 | TC-ALC-032 – TC-ALC-041 |
| F. Invalid Status Transitions | 8 | TC-ALC-042 – TC-ALC-049 |
| G. Admin — Check-In / Check-Out | 4 | TC-ALC-050 – TC-ALC-053 |
| H. Admin — Move Student | 4 | TC-ALC-054 – TC-ALC-057 |
| I. Admin — Override Allocation | 2 | TC-ALC-058 – TC-ALC-059 |
| J. Admin — Search & Filter | 4 | TC-ALC-060 – TC-ALC-063 |
| K. Admin — Delete | 1 | TC-ALC-064 |
| L. Offer Expiry | 2 | TC-ALC-065 – TC-ALC-066 |
| **Total** | **66** | |
