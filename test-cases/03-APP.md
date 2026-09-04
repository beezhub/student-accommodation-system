# Application Management — UAT Test Cases

| Field | Value |
|-------|-------|
| **Module** | Application Management |
| **Version** | 1.0 |
| **Total Test Cases** | 63 |
| **Author** | UAT Team |
| **Date** | _________________ |

**Test Data:**

| Role | Email | Password |
|------|-------|----------|
| Student A | john.doe@test.com | Student@123 |
| Student B | jane.smith@test.com | Student@123 |
| Platform Admin | platformadmin@test.com | Admin@12345 |
| Tenant Admin | tenantadmin@test.com | Admin@12345 |

**Application Statuses:** PENDING, UNDER_REVIEW, CHANGES_REQUESTED, APPROVED, AWAITING_ALLOCATION, ALLOCATED, CHECKED_IN, REJECTED, EXPIRED, CANCELLED

**Terminal States:** CHECKED_IN, REJECTED, EXPIRED, CANCELLED

---

## A. STUDENT — SUBMIT APPLICATION

---

### TC-APP-001: Submit new application with valid data

| Field | Value |
|-------|-------|
| **ID** | TC-APP-001 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Student A is logged in; profile is complete; no existing PENDING application |
| **Test Data** | studentId: Student A's ID, courseName: "Computer Science", institutionName: "University of Cape Town", preferredBuildingIds: [1] |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/applications/new` | Application form loads with all fields visible |
| 2 | Fill in courseName: "Computer Science" | Field accepts input |
| 3 | Fill in institutionName: "University of Cape Town" | Field accepts input |
| 4 | Select a preferred building from the dropdown | Building is selected and displayed |
| 5 | Leave requiresAccessibility unchecked | Checkbox remains unchecked |
| 6 | Leave specialRequirements empty | Field remains empty |
| 7 | Click "Submit" button | Application is submitted successfully; HTTP 201 returned; redirect to `/applications` list |
| 8 | Verify success notification appears | Snackbar/toast shows "Application submitted successfully" or similar |

| Field | Value |
|-------|-------|
| **Postconditions** | New application exists with status PENDING; application code generated |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-APP-002: Submit with empty courseName — validation error

| Field | Value |
|-------|-------|
| **ID** | TC-APP-002 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Student A is logged in; no existing PENDING application |
| **Test Data** | courseName: (empty), institutionName: "University of Cape Town", preferredBuildingIds: [1] |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/applications/new` | Application form loads |
| 2 | Leave courseName field empty | Field is blank |
| 3 | Fill in all other required fields with valid data | Other fields populated |
| 4 | Click "Submit" button | Form validation error displayed; courseName field highlighted in red with error message |
| 5 | Verify no API call is made | Network tab shows no POST request to `/api/applications/submit` |

| Field | Value |
|-------|-------|
| **Postconditions** | No application created; user remains on form |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-APP-003: Submit with accessibility requirement checked

| Field | Value |
|-------|-------|
| **ID** | TC-APP-003 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Student A is logged in; no existing PENDING application |
| **Test Data** | courseName: "Engineering", requiresAccessibility: true |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/applications/new` | Application form loads |
| 2 | Fill in all required fields with valid data | Fields populated |
| 3 | Check the "Requires Accessibility" checkbox | Checkbox becomes checked |
| 4 | Click "Submit" button | Application submitted successfully; HTTP 201 |
| 5 | Navigate to `/applications` and click on the new application | Application detail shows requiresAccessibility = true |

| Field | Value |
|-------|-------|
| **Postconditions** | Application created with requiresAccessibility flag set to true |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-APP-004: Submit selecting multiple preferred buildings

| Field | Value |
|-------|-------|
| **ID** | TC-APP-004 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Student A is logged in; at least 3 buildings exist in the system |
| **Test Data** | preferredBuildingIds: [1, 2, 3], courseName: "Mathematics" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/applications/new` | Application form loads |
| 2 | Fill in all required fields | Fields populated |
| 3 | Select 3 preferred buildings from the building selection | All 3 buildings appear as selected preferences |
| 4 | Click "Submit" button | Application submitted successfully; HTTP 201 |
| 5 | View the created application details | All 3 preferred buildings are listed in order |

| Field | Value |
|-------|-------|
| **Postconditions** | Application created with 3 preferred buildings in the specified order |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Maximum of 3 preferred buildings enforced by @Size(max=3) validation |

---

### TC-APP-005: Submit with special requirements text

| Field | Value |
|-------|-------|
| **ID** | TC-APP-005 |
| **Module** | Application Management |
| **Priority** | Medium |
| **Preconditions** | Student A is logged in; no existing PENDING application |
| **Test Data** | specialRequirements: "I require a ground floor room due to mobility issues. Prefer a quiet area away from common rooms." |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/applications/new` | Application form loads |
| 2 | Fill in all required fields | Fields populated |
| 3 | Enter special requirements text in the specialRequirements field | Text accepted in the textarea |
| 4 | Click "Submit" button | Application submitted successfully; HTTP 201 |
| 5 | View the created application details | Special requirements text is displayed correctly |

| Field | Value |
|-------|-------|
| **Postconditions** | Application created with special requirements saved |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-APP-006: Verify application appears in list with PENDING status

| Field | Value |
|-------|-------|
| **ID** | TC-APP-006 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Student A has just submitted a new application (TC-APP-001 passed) |
| **Test Data** | Student A credentials |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/applications` | Applications list page loads |
| 2 | Locate the newly submitted application in the list | Application is visible in the list |
| 3 | Verify the status column | Status displays "Pending" or "PENDING" |
| 4 | Verify the application date | Date matches today's date |

| Field | Value |
|-------|-------|
| **Postconditions** | Application confirmed visible with PENDING status |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-APP-007: Verify application code is generated (unique)

| Field | Value |
|-------|-------|
| **ID** | TC-APP-007 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Student A has submitted an application |
| **Test Data** | Student A credentials |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/applications` | Applications list loads |
| 2 | Locate the newly submitted application | Application visible |
| 3 | Verify the applicationCode column is populated | A unique code is displayed (non-empty, non-null) |
| 4 | Submit a second application (if allowed) or check Student B's application code | Different application code generated |

| Field | Value |
|-------|-------|
| **Postconditions** | Each application has a unique, system-generated application code |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | If duplicate applications are blocked (HTTP 409), verify error message instead of step 4 |

---

## B. STUDENT — VIEW APPLICATIONS

---

### TC-APP-008: View list of own applications

| Field | Value |
|-------|-------|
| **ID** | TC-APP-008 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Student A is logged in; has at least 1 submitted application |
| **Test Data** | Student A credentials |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/applications` | Applications list page loads |
| 2 | Verify the list is populated | Student A's applications are displayed |
| 3 | Verify each row shows: application code, status, date | All columns render correctly |
| 4 | Verify only Student A's own applications are shown | No applications from other students visible |

| Field | Value |
|-------|-------|
| **Postconditions** | Student sees only their own applications |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | API call goes to GET `/api/applications/my-applications` |

---

### TC-APP-009: View single application details

| Field | Value |
|-------|-------|
| **ID** | TC-APP-009 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Student A is logged in; has at least 1 application |
| **Test Data** | Student A credentials; known applicationId |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/applications/:id` (using a valid application ID) | Application detail page loads |
| 2 | Verify application code is displayed | Code matches the one from the list |
| 3 | Verify status is displayed | Correct current status shown |
| 4 | Verify student info section | Student name, email, course displayed |
| 5 | Verify preferences section | Preferred buildings, accessibility, special requirements shown |

| Field | Value |
|-------|-------|
| **Postconditions** | Application details displayed correctly |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-APP-010: Verify application details show correct student info, preferences, status

| Field | Value |
|-------|-------|
| **ID** | TC-APP-010 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Student A is logged in; application submitted with known data (accessibility=true, specialRequirements="Ground floor", 2 preferred buildings) |
| **Test Data** | Application from TC-APP-003 or TC-APP-005 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/applications/:id` | Detail page loads |
| 2 | Verify student first name and last name | Matches Student A's profile (John Doe) |
| 3 | Verify courseName field | Shows "Engineering" or the submitted course |
| 4 | Verify institutionName | Matches submitted institution |
| 5 | Verify requiresAccessibility | Shows true/checked |
| 6 | Verify specialRequirements | Shows the submitted text |
| 7 | Verify preferred buildings | Listed in correct order |
| 8 | Verify status | Shows "Pending" |

| Field | Value |
|-------|-------|
| **Postconditions** | All application data verified as accurate |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-APP-011: Student cannot view another student's application

| Field | Value |
|-------|-------|
| **ID** | TC-APP-011 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Student A is logged in; Student B has an application with known ID |
| **Test Data** | Student A credentials; Student B's applicationId |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/applications/{Student B's applicationId}` | HTTP 403 Forbidden returned |
| 2 | Verify error message or redirect | Access denied message displayed or user redirected away |
| 3 | Verify no data from Student B's application is leaked | Response body does not contain Student B's data |

| Field | Value |
|-------|-------|
| **Postconditions** | Student A cannot access Student B's application |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Security test — verify both UI and API level |

---

## C. STUDENT — CANCEL APPLICATION

---

### TC-APP-012: Cancel own PENDING application

| Field | Value |
|-------|-------|
| **ID** | TC-APP-012 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Student A is logged in; has an application in PENDING status |
| **Test Data** | Student A credentials; PENDING application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/applications` | List loads with PENDING application visible |
| 2 | Click "Cancel" button on the PENDING application | Confirmation dialog appears |
| 3 | Confirm the cancellation | DELETE request sent to `/api/applications/{id}`; HTTP 204 returned |
| 4 | Verify the application status updates | Status changes to CANCELLED |
| 5 | Verify the cancel button is no longer available | No cancel action visible for this application |

| Field | Value |
|-------|-------|
| **Postconditions** | Application status is CANCELLED; cannot be modified further |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-APP-013: Cancel own CHANGES_REQUESTED application

| Field | Value |
|-------|-------|
| **ID** | TC-APP-013 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Student A is logged in; has an application in CHANGES_REQUESTED status (admin previously requested changes) |
| **Test Data** | Student A credentials; CHANGES_REQUESTED application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/applications` | List loads with CHANGES_REQUESTED application |
| 2 | Click "Cancel" button on the application | Confirmation dialog appears |
| 3 | Confirm the cancellation | DELETE request sent; HTTP 204 returned |
| 4 | Verify status changes to CANCELLED | Application shows CANCELLED status |

| Field | Value |
|-------|-------|
| **Postconditions** | Application is CANCELLED |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | CHANGES_REQUESTED → CANCELLED is a valid transition per canTransitionTo() |

---

### TC-APP-014: Cannot cancel application in UNDER_REVIEW status

| Field | Value |
|-------|-------|
| **ID** | TC-APP-014 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Student A is logged in; has an application in UNDER_REVIEW status |
| **Test Data** | Student A credentials; UNDER_REVIEW application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/applications` | List loads with UNDER_REVIEW application |
| 2 | Verify the "Cancel" button state | Cancel button is either hidden or disabled for UNDER_REVIEW applications |
| 3 | (If button visible) Click cancel and confirm | HTTP 400 error returned; error message indicates application cannot be cancelled in current status |

| Field | Value |
|-------|-------|
| **Postconditions** | Application remains in UNDER_REVIEW status |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | UNDER_REVIEW does not include CANCELLED in its canTransitionTo() targets (only PENDING, CHANGES_REQUESTED, APPROVED, REJECTED) |

---

### TC-APP-015: Cannot cancel application in terminal state

| Field | Value |
|-------|-------|
| **ID** | TC-APP-015 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Student A is logged in; has applications in REJECTED, CANCELLED, EXPIRED, or CHECKED_IN status |
| **Test Data** | Student A credentials; terminal-state application IDs |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/applications` | List loads |
| 2 | Locate a REJECTED application | Cancel button is hidden/disabled |
| 3 | Locate a CANCELLED application | Cancel button is hidden/disabled |
| 4 | Locate an EXPIRED application (if available) | Cancel button is hidden/disabled |
| 5 | Attempt API call: DELETE `/api/applications/{rejectedAppId}` | HTTP 400 returned; transition not allowed |

| Field | Value |
|-------|-------|
| **Postconditions** | Terminal-state applications remain unchanged |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Terminal states: CHECKED_IN, REJECTED, EXPIRED, CANCELLED |

---

### TC-APP-016: Student cannot cancel another student's application

| Field | Value |
|-------|-------|
| **ID** | TC-APP-016 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Student A is logged in; Student B has a PENDING application |
| **Test Data** | Student A credentials; Student B's PENDING application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Attempt API call: DELETE `/api/applications/{studentB_appId}` using Student A's auth token | HTTP 403 Forbidden returned |
| 2 | Verify Student B's application is unchanged | Status remains PENDING |

| Field | Value |
|-------|-------|
| **Postconditions** | Student B's application is unmodified |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Security test — ownership validation |

---

## D. ADMIN — VIEW & SEARCH APPLICATIONS

---

### TC-APP-017: Admin views all applications

| Field | Value |
|-------|-------|
| **ID** | TC-APP-017 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Tenant Admin is logged in; multiple applications exist in the system |
| **Test Data** | tenantadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/applications` | ApplicationReviewComponent loads |
| 2 | Verify the applications table is populated | Table shows applications with columns: studentName, applicationCode, preferredBuilding, allocationScore, documentStatus, allocationStatus, status, applicationDate, actions |
| 3 | Verify metric cards at top | Total, Pending, Awaiting Allocation, and Allocated counts displayed |
| 4 | Verify default filter is set to PENDING status | Status filter shows "PENDING" selected |

| Field | Value |
|-------|-------|
| **Postconditions** | Admin can see application list |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Default page size is 20; default sort by applicationDate |

---

### TC-APP-018: Search by student name

| Field | Value |
|-------|-------|
| **ID** | TC-APP-018 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Admin is logged in; Student A (John Doe) has at least 1 application |
| **Test Data** | studentName search: "John" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/applications` | Page loads |
| 2 | Toggle filters panel (if collapsed) | Filter form becomes visible |
| 3 | Clear the default status filter | Status filter cleared |
| 4 | Enter "John" in the studentName filter field | Text entered |
| 5 | Click "Search" / "Apply Filters" button | Table refreshes with filtered results |
| 6 | Verify all results contain "John" in student name | Only John Doe's applications appear |

| Field | Value |
|-------|-------|
| **Postconditions** | Filtered results shown |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Search hits `/api/applications/search?studentName=John` |

---

### TC-APP-019: Filter by status (PENDING)

| Field | Value |
|-------|-------|
| **ID** | TC-APP-019 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Admin is logged in; applications exist in various statuses |
| **Test Data** | status filter: "PENDING" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/applications` | Page loads with PENDING filter already active (default) |
| 2 | Verify all displayed applications have PENDING status | Every row shows "Pending" in the status column |
| 3 | Verify the count matches the Pending metric card | Numbers are consistent |

| Field | Value |
|-------|-------|
| **Postconditions** | Only PENDING applications shown |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-APP-020: Filter by status (UNDER_REVIEW)

| Field | Value |
|-------|-------|
| **ID** | TC-APP-020 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Admin is logged in; at least 1 application in UNDER_REVIEW status |
| **Test Data** | status filter: "UNDER_REVIEW" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/applications` | Page loads |
| 2 | Change the status filter dropdown to select a value that represents UNDER_REVIEW | Filter applied (note: UNDER_REVIEW may not be in the default statuses dropdown — verify if it's available) |
| 3 | Click "Apply Filters" | Table refreshes |
| 4 | Verify all results show UNDER_REVIEW status | Only Under Review applications displayed |

| Field | Value |
|-------|-------|
| **Postconditions** | Filtered to UNDER_REVIEW only |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | The component statuses array may not include UNDER_REVIEW — verify dropdown options |

---

### TC-APP-021: Filter by status (APPROVED)

| Field | Value |
|-------|-------|
| **ID** | TC-APP-021 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Admin is logged in; at least 1 APPROVED application exists |
| **Test Data** | status filter: "APPROVED" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/applications` | Page loads |
| 2 | Change status filter to "Approved" | Filter selected |
| 3 | Click "Apply Filters" | Table refreshes with only APPROVED applications |
| 4 | Verify all rows show APPROVED status | Confirmed |

| Field | Value |
|-------|-------|
| **Postconditions** | Only APPROVED applications displayed |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-APP-022: Filter by date range

| Field | Value |
|-------|-------|
| **ID** | TC-APP-022 |
| **Module** | Application Management |
| **Priority** | Medium |
| **Preconditions** | Admin is logged in; applications exist with various dates |
| **Test Data** | startDate: "2025-01-01", endDate: "2025-12-31" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/applications` | Page loads |
| 2 | Toggle filters panel | Filters visible |
| 3 | Clear the status filter | Status cleared |
| 4 | Set start date to 2025-01-01 using the date picker | Date selected |
| 5 | Set end date to 2025-12-31 using the date picker | Date selected |
| 6 | Click "Apply Filters" | Table refreshes |
| 7 | Verify all displayed applications have applicationDate within the range | All dates fall between 2025-01-01 and 2025-12-31 |

| Field | Value |
|-------|-------|
| **Postconditions** | Results filtered by date range |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-APP-023: Search with no results — empty state

| Field | Value |
|-------|-------|
| **ID** | TC-APP-023 |
| **Module** | Application Management |
| **Priority** | Medium |
| **Preconditions** | Admin is logged in |
| **Test Data** | studentName: "ZZZNONEXISTENT999" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/applications` | Page loads |
| 2 | Toggle filters and clear status filter | Filters visible, status cleared |
| 3 | Enter "ZZZNONEXISTENT999" in student name field | Text entered |
| 4 | Click "Apply Filters" | Table refreshes |
| 5 | Verify empty state component is displayed | EmptyStateComponent renders with message like "No applications found" |
| 6 | Verify total count shows 0 | Metric card shows 0 |

| Field | Value |
|-------|-------|
| **Postconditions** | Empty state displayed gracefully |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-APP-024: Pagination — navigate pages

| Field | Value |
|-------|-------|
| **ID** | TC-APP-024 |
| **Module** | Application Management |
| **Priority** | Medium |
| **Preconditions** | Admin is logged in; more than 20 applications exist (to exceed default page size) |
| **Test Data** | Clear all filters to show all applications |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/applications` | Page loads |
| 2 | Clear status filter and apply | All applications shown |
| 3 | Verify paginator shows total count > 20 | Paginator displays e.g., "1 – 20 of 35" |
| 4 | Click "Next page" arrow on paginator | Page 2 loads; different set of applications displayed |
| 5 | Click "Previous page" arrow | Returns to page 1; original applications visible |
| 6 | Change page size dropdown (e.g., to 10) | Table reloads with 10 rows per page; paginator updates |

| Field | Value |
|-------|-------|
| **Postconditions** | Pagination works correctly |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Default page size is 20 |

---

### TC-APP-025: View application full details (detail dialog)

| Field | Value |
|-------|-------|
| **ID** | TC-APP-025 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Admin is logged in; at least 1 application exists |
| **Test Data** | Any application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/applications` | Page loads |
| 2 | Click "View Details" (or the row action) on an application | ApplicationDetailDialogComponent opens (850px wide dialog) |
| 3 | Verify student profile information is displayed | Student name, email, course, institution visible |
| 4 | Verify documents section | List of uploaded documents with statuses shown |
| 5 | Verify application status and history | Current status displayed |
| 6 | Close the dialog | Dialog closes; table remains unchanged |

| Field | Value |
|-------|-------|
| **Postconditions** | Detail dialog displayed and dismissed correctly |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Dialog fetches from GET `/api/applications/{id}/full-details` |

---

### TC-APP-026: View application preferences (preferences dialog)

| Field | Value |
|-------|-------|
| **ID** | TC-APP-026 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Admin is logged in; application exists with preferences |
| **Test Data** | Application with multiple preferred buildings |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/applications` | Page loads |
| 2 | Click "View Preferences" action on an application | PreferencesDialogComponent opens (500px wide) |
| 3 | Verify preferred buildings are listed | Building names displayed in preference order |
| 4 | Verify accessibility requirement status | Shows yes/no for requiresAccessibility |
| 5 | Verify special requirements text | Text displayed if present |
| 6 | Close the dialog | Dialog closes |

| Field | Value |
|-------|-------|
| **Postconditions** | Preferences displayed correctly |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## E. ADMIN — STATUS TRANSITIONS (Valid)

---

### TC-APP-027: PENDING → UNDER_REVIEW

| Field | Value |
|-------|-------|
| **ID** | TC-APP-027 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Admin is logged in; a PENDING application exists |
| **Test Data** | PENDING application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/applications` | PENDING applications listed |
| 2 | Click "Mark Under Review" action on a PENDING application | POST `/api/applications/{id}/mark-under-review` is called |
| 3 | Verify success notification | "Application marked as Under Review" snackbar appears |
| 4 | Verify application status updates to UNDER_REVIEW | Table refreshes; application now shows "Under Review" |

| Field | Value |
|-------|-------|
| **Postconditions** | Application is in UNDER_REVIEW status |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Uses dedicated `markUnderReview()` endpoint |

---

### TC-APP-028: PENDING → APPROVED

| Field | Value |
|-------|-------|
| **ID** | TC-APP-028 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Admin is logged in; a PENDING application exists |
| **Test Data** | PENDING application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/applications` | PENDING applications listed |
| 2 | Click "Approve" action or update status to APPROVED via actions menu | PATCH `/api/applications/{id}/status` with status=APPROVED |
| 3 | Verify success notification | "Application approved successfully" snackbar |
| 4 | Verify application status is now APPROVED | Table refreshes; status shows "Approved" |

| Field | Value |
|-------|-------|
| **Postconditions** | Application is in APPROVED status |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-APP-029: PENDING → REJECTED

| Field | Value |
|-------|-------|
| **ID** | TC-APP-029 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Admin is logged in; a PENDING application exists |
| **Test Data** | PENDING application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/applications` | PENDING applications listed |
| 2 | Click "Reject" action on the application | PATCH `/api/applications/{id}/status` with status=REJECTED |
| 3 | Verify success notification | "Application rejected successfully" snackbar |
| 4 | Verify application status is now REJECTED | Status shows "Rejected" |

| Field | Value |
|-------|-------|
| **Postconditions** | Application is in REJECTED (terminal) status |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Terminal state — no further transitions allowed |

---

### TC-APP-030: PENDING → CANCELLED

| Field | Value |
|-------|-------|
| **ID** | TC-APP-030 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Admin is logged in; a PENDING application exists |
| **Test Data** | PENDING application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/applications` | PENDING applications listed |
| 2 | Click "Cancel" action on the application | PATCH `/api/applications/{id}/status` with status=CANCELLED |
| 3 | Verify success notification | "Application cancelled successfully" snackbar |
| 4 | Verify status is CANCELLED | Status shows "Cancelled" |

| Field | Value |
|-------|-------|
| **Postconditions** | Application is in CANCELLED (terminal) status |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-APP-031: UNDER_REVIEW → PENDING

| Field | Value |
|-------|-------|
| **ID** | TC-APP-031 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Admin is logged in; an UNDER_REVIEW application exists |
| **Test Data** | UNDER_REVIEW application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/applications` and filter by UNDER_REVIEW (if available) or locate UNDER_REVIEW app | Application visible |
| 2 | Update status to PENDING via actions menu | PATCH `/api/applications/{id}/status` with status=PENDING |
| 3 | Verify success notification | Status update confirmed |
| 4 | Verify application status is now PENDING | Status shows "Pending" |

| Field | Value |
|-------|-------|
| **Postconditions** | Application returned to PENDING status |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | This allows sending an application back for requeue |

---

### TC-APP-032: UNDER_REVIEW → CHANGES_REQUESTED (with reason)

| Field | Value |
|-------|-------|
| **ID** | TC-APP-032 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Admin is logged in; an UNDER_REVIEW application exists |
| **Test Data** | UNDER_REVIEW application ID; reason: "Please upload a clearer copy of your student ID" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/applications` and locate UNDER_REVIEW app | Application visible |
| 2 | Click "Request Changes" action on the application | Browser prompt() dialog appears asking for reason |
| 3 | Enter reason: "Please upload a clearer copy of your student ID" | Text entered in prompt |
| 4 | Click OK | POST `/api/applications/{id}/request-changes` with body `{"reason": "..."}` |
| 5 | Verify success notification | "Changes requested successfully" snackbar |
| 6 | Verify application status is now CHANGES_REQUESTED | Status shows "Changes Requested" |

| Field | Value |
|-------|-------|
| **Postconditions** | Application is in CHANGES_REQUESTED status; reason stored |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Reason is required — empty reason should be rejected (IllegalArgumentException) |

---

### TC-APP-033: UNDER_REVIEW → APPROVED

| Field | Value |
|-------|-------|
| **ID** | TC-APP-033 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Admin is logged in; an UNDER_REVIEW application exists |
| **Test Data** | UNDER_REVIEW application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate UNDER_REVIEW application | Application visible |
| 2 | Click "Approve" action | PATCH with status=APPROVED |
| 3 | Verify success notification | Confirmation snackbar |
| 4 | Verify status is APPROVED | Status updated |

| Field | Value |
|-------|-------|
| **Postconditions** | Application is APPROVED |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-APP-034: UNDER_REVIEW → REJECTED

| Field | Value |
|-------|-------|
| **ID** | TC-APP-034 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Admin is logged in; an UNDER_REVIEW application exists |
| **Test Data** | UNDER_REVIEW application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate UNDER_REVIEW application | Application visible |
| 2 | Click "Reject" action | PATCH with status=REJECTED |
| 3 | Verify success notification | Confirmation snackbar |
| 4 | Verify status is REJECTED | Terminal state reached |

| Field | Value |
|-------|-------|
| **Postconditions** | Application is REJECTED (terminal) |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-APP-035: CHANGES_REQUESTED → PENDING (student resubmits)

| Field | Value |
|-------|-------|
| **ID** | TC-APP-035 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Student A is logged in; has an application in CHANGES_REQUESTED status |
| **Test Data** | Student A credentials; CHANGES_REQUESTED application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/applications` | Application with CHANGES_REQUESTED status visible |
| 2 | Click on the application to view details | Detail page shows requested changes reason |
| 3 | Make the requested changes (e.g., re-upload document) and resubmit | Application status transitions back to PENDING |
| 4 | Verify status is now PENDING | Status shows "Pending" |

| Field | Value |
|-------|-------|
| **Postconditions** | Application is back in PENDING status for admin review |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | This may be triggered by student action or admin action |

---

### TC-APP-036: CHANGES_REQUESTED → CANCELLED

| Field | Value |
|-------|-------|
| **ID** | TC-APP-036 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Student A or Admin is logged in; application in CHANGES_REQUESTED status |
| **Test Data** | CHANGES_REQUESTED application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate CHANGES_REQUESTED application | Application visible |
| 2 | Click "Cancel" action | Status update request sent with status=CANCELLED |
| 3 | Verify success notification | Confirmation shown |
| 4 | Verify status is CANCELLED | Terminal state reached |

| Field | Value |
|-------|-------|
| **Postconditions** | Application is CANCELLED (terminal) |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-APP-037: APPROVED → AWAITING_ALLOCATION

| Field | Value |
|-------|-------|
| **ID** | TC-APP-037 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Admin is logged in; an APPROVED application exists with all required documents approved |
| **Test Data** | APPROVED application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate APPROVED application | Application visible |
| 2 | Click "Move to Allocation" action | POST `/api/applications/{id}/move-to-allocation` called |
| 3 | Verify success notification | "Application moved to allocation phase" snackbar |
| 4 | Verify status is AWAITING_ALLOCATION | Status shows "Awaiting Allocation" |

| Field | Value |
|-------|-------|
| **Postconditions** | Application is in AWAITING_ALLOCATION status |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Requires all documents to be approved — see TC-APP-055 |

---

### TC-APP-038: APPROVED → REJECTED

| Field | Value |
|-------|-------|
| **ID** | TC-APP-038 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Admin is logged in; an APPROVED application exists |
| **Test Data** | APPROVED application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate APPROVED application | Application visible |
| 2 | Click "Reject" action | PATCH with status=REJECTED |
| 3 | Verify success notification | Confirmation snackbar |
| 4 | Verify status is REJECTED | Terminal state |

| Field | Value |
|-------|-------|
| **Postconditions** | Application is REJECTED (terminal) |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-APP-039: APPROVED → CANCELLED

| Field | Value |
|-------|-------|
| **ID** | TC-APP-039 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Admin is logged in; an APPROVED application exists |
| **Test Data** | APPROVED application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate APPROVED application | Application visible |
| 2 | Click "Cancel" action | PATCH with status=CANCELLED |
| 3 | Verify success notification | Confirmation snackbar |
| 4 | Verify status is CANCELLED | Terminal state |

| Field | Value |
|-------|-------|
| **Postconditions** | Application is CANCELLED (terminal) |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-APP-040: AWAITING_ALLOCATION → ALLOCATED

| Field | Value |
|-------|-------|
| **ID** | TC-APP-040 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Admin is logged in; an AWAITING_ALLOCATION application exists; rooms are available |
| **Test Data** | AWAITING_ALLOCATION application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/applications` and filter by AWAITING_ALLOCATION | Application visible |
| 2 | Click "Allocate" action or open AllocationChoiceDialogComponent | Allocation dialog opens (650px wide) |
| 3 | Select allocation method (auto-allocation or manual) and confirm | Allocation process runs; staged allocation proposed and confirmed |
| 4 | Verify success notification | "Room allocated successfully!" snackbar |
| 5 | Verify application status is ALLOCATED | Status shows "Allocated" |

| Field | Value |
|-------|-------|
| **Postconditions** | Application is ALLOCATED; room assigned to student |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Uses StagedAllocationService for propose → confirm flow |

---

### TC-APP-041: AWAITING_ALLOCATION → REJECTED

| Field | Value |
|-------|-------|
| **ID** | TC-APP-041 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Admin is logged in; an AWAITING_ALLOCATION application exists |
| **Test Data** | AWAITING_ALLOCATION application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate AWAITING_ALLOCATION application | Application visible |
| 2 | Click "Reject" action | PATCH with status=REJECTED |
| 3 | Verify success notification | Confirmation snackbar |
| 4 | Verify status is REJECTED | Terminal state |

| Field | Value |
|-------|-------|
| **Postconditions** | Application is REJECTED (terminal) |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-APP-042: AWAITING_ALLOCATION → EXPIRED

| Field | Value |
|-------|-------|
| **ID** | TC-APP-042 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Admin is logged in; an AWAITING_ALLOCATION application exists |
| **Test Data** | AWAITING_ALLOCATION application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate AWAITING_ALLOCATION application | Application visible |
| 2 | Update status to EXPIRED via API or admin action | PATCH with status=EXPIRED |
| 3 | Verify status is EXPIRED | Terminal state |

| Field | Value |
|-------|-------|
| **Postconditions** | Application is EXPIRED (terminal) |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Expiry may also be triggered automatically by scheduled job |

---

### TC-APP-043: AWAITING_ALLOCATION → CANCELLED

| Field | Value |
|-------|-------|
| **ID** | TC-APP-043 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Admin is logged in; an AWAITING_ALLOCATION application exists |
| **Test Data** | AWAITING_ALLOCATION application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate AWAITING_ALLOCATION application | Application visible |
| 2 | Click "Cancel" action | PATCH with status=CANCELLED |
| 3 | Verify success notification | Confirmation snackbar |
| 4 | Verify status is CANCELLED | Terminal state |

| Field | Value |
|-------|-------|
| **Postconditions** | Application is CANCELLED (terminal) |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-APP-044: ALLOCATED → CHECKED_IN

| Field | Value |
|-------|-------|
| **ID** | TC-APP-044 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Admin is logged in; an ALLOCATED application exists |
| **Test Data** | ALLOCATED application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate ALLOCATED application | Application visible |
| 2 | Update status to CHECKED_IN via admin action | PATCH with status=CHECKED_IN |
| 3 | Verify success notification | Confirmation snackbar |
| 4 | Verify status is CHECKED_IN | Terminal state — student has moved in |

| Field | Value |
|-------|-------|
| **Postconditions** | Application is CHECKED_IN (terminal); student occupancy confirmed |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Final positive outcome of the application lifecycle |

---

### TC-APP-045: ALLOCATED → EXPIRED

| Field | Value |
|-------|-------|
| **ID** | TC-APP-045 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Admin is logged in; an ALLOCATED application exists |
| **Test Data** | ALLOCATED application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate ALLOCATED application | Application visible |
| 2 | Update status to EXPIRED | PATCH with status=EXPIRED |
| 3 | Verify status is EXPIRED | Terminal state — offer expired |

| Field | Value |
|-------|-------|
| **Postconditions** | Application is EXPIRED (terminal); room released |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | May be triggered by offer expiry scheduled job |

---

### TC-APP-046: ALLOCATED → CANCELLED

| Field | Value |
|-------|-------|
| **ID** | TC-APP-046 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Admin is logged in; an ALLOCATED application exists |
| **Test Data** | ALLOCATED application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate ALLOCATED application | Application visible |
| 2 | Click "Cancel" action | PATCH with status=CANCELLED |
| 3 | Verify success notification | Confirmation snackbar |
| 4 | Verify status is CANCELLED | Terminal state |

| Field | Value |
|-------|-------|
| **Postconditions** | Application is CANCELLED (terminal); room released |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## F. ADMIN — INVALID STATUS TRANSITIONS (Verify Blocked)

---

### TC-APP-047: PENDING → ALLOCATED — blocked

| Field | Value |
|-------|-------|
| **ID** | TC-APP-047 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Admin is logged in; a PENDING application exists |
| **Test Data** | PENDING application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate PENDING application | Application visible |
| 2 | Attempt to update status to ALLOCATED via API: PATCH `/api/applications/{id}/status` with `{"status": "ALLOCATED"}` | HTTP 400 Bad Request returned |
| 3 | Verify error message | Response indicates invalid transition: PENDING cannot transition to ALLOCATED |
| 4 | Verify application status remains PENDING | Status unchanged |

| Field | Value |
|-------|-------|
| **Postconditions** | Application remains PENDING; invalid transition blocked |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Skip-state transition — must go through APPROVED → AWAITING_ALLOCATION first |

---

### TC-APP-048: PENDING → AWAITING_ALLOCATION — blocked

| Field | Value |
|-------|-------|
| **ID** | TC-APP-048 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Admin is logged in; a PENDING application exists |
| **Test Data** | PENDING application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate PENDING application | Application visible |
| 2 | Attempt to update status to AWAITING_ALLOCATION via API | HTTP 400 Bad Request returned |
| 3 | Verify error message | Invalid transition: PENDING → AWAITING_ALLOCATION not allowed |
| 4 | Verify status remains PENDING | Unchanged |

| Field | Value |
|-------|-------|
| **Postconditions** | Application remains PENDING |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Must go through APPROVED first |

---

### TC-APP-049: UNDER_REVIEW → ALLOCATED — blocked

| Field | Value |
|-------|-------|
| **ID** | TC-APP-049 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Admin is logged in; an UNDER_REVIEW application exists |
| **Test Data** | UNDER_REVIEW application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate UNDER_REVIEW application | Application visible |
| 2 | Attempt to update status to ALLOCATED via API | HTTP 400 Bad Request returned |
| 3 | Verify error message | Invalid transition: UNDER_REVIEW → ALLOCATED not allowed |
| 4 | Verify status remains UNDER_REVIEW | Unchanged |

| Field | Value |
|-------|-------|
| **Postconditions** | Application remains UNDER_REVIEW |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-APP-050: CHANGES_REQUESTED → APPROVED — blocked

| Field | Value |
|-------|-------|
| **ID** | TC-APP-050 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Admin is logged in; a CHANGES_REQUESTED application exists |
| **Test Data** | CHANGES_REQUESTED application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate CHANGES_REQUESTED application | Application visible |
| 2 | Attempt to update status to APPROVED via API | HTTP 400 Bad Request returned |
| 3 | Verify error message | Invalid transition: CHANGES_REQUESTED → APPROVED not allowed (must go through PENDING first) |
| 4 | Verify status remains CHANGES_REQUESTED | Unchanged |

| Field | Value |
|-------|-------|
| **Postconditions** | Application remains CHANGES_REQUESTED |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Student must resubmit (→ PENDING) before approval |

---

### TC-APP-051: REJECTED → any status — blocked (terminal)

| Field | Value |
|-------|-------|
| **ID** | TC-APP-051 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Admin is logged in; a REJECTED application exists |
| **Test Data** | REJECTED application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate REJECTED application | Application visible |
| 2 | Attempt to update status to PENDING via API | HTTP 400 Bad Request returned |
| 3 | Attempt to update status to APPROVED via API | HTTP 400 Bad Request returned |
| 4 | Attempt to update status to UNDER_REVIEW via API | HTTP 400 Bad Request returned |
| 5 | Verify error message indicates terminal state | "Cannot transition from REJECTED" or similar |
| 6 | Verify status remains REJECTED | Unchanged |

| Field | Value |
|-------|-------|
| **Postconditions** | Application remains REJECTED; no transitions possible |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | REJECTED is a terminal state — `canTransitionTo()` returns false for all targets |

---

### TC-APP-052: CANCELLED → any status — blocked (terminal)

| Field | Value |
|-------|-------|
| **ID** | TC-APP-052 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Admin is logged in; a CANCELLED application exists |
| **Test Data** | CANCELLED application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate CANCELLED application | Application visible |
| 2 | Attempt to update status to PENDING via API | HTTP 400 Bad Request returned |
| 3 | Attempt to update status to APPROVED via API | HTTP 400 Bad Request returned |
| 4 | Verify error message indicates terminal state | "Cannot transition from CANCELLED" or similar |
| 5 | Verify status remains CANCELLED | Unchanged |

| Field | Value |
|-------|-------|
| **Postconditions** | Application remains CANCELLED; no transitions possible |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-APP-053: CHECKED_IN → any status — blocked (terminal)

| Field | Value |
|-------|-------|
| **ID** | TC-APP-053 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Admin is logged in; a CHECKED_IN application exists |
| **Test Data** | CHECKED_IN application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate CHECKED_IN application | Application visible |
| 2 | Attempt to update status to ALLOCATED via API | HTTP 400 Bad Request returned |
| 3 | Attempt to update status to CANCELLED via API | HTTP 400 Bad Request returned |
| 4 | Verify error message indicates terminal state | "Cannot transition from CHECKED_IN" or similar |
| 5 | Verify status remains CHECKED_IN | Unchanged |

| Field | Value |
|-------|-------|
| **Postconditions** | Application remains CHECKED_IN; no transitions possible |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-APP-054: EXPIRED → any status — blocked (terminal)

| Field | Value |
|-------|-------|
| **ID** | TC-APP-054 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Admin is logged in; an EXPIRED application exists |
| **Test Data** | EXPIRED application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate EXPIRED application | Application visible |
| 2 | Attempt to update status to PENDING via API | HTTP 400 Bad Request returned |
| 3 | Attempt to update status to AWAITING_ALLOCATION via API | HTTP 400 Bad Request returned |
| 4 | Verify error message indicates terminal state | "Cannot transition from EXPIRED" or similar |
| 5 | Verify status remains EXPIRED | Unchanged |

| Field | Value |
|-------|-------|
| **Postconditions** | Application remains EXPIRED; no transitions possible |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## G. ADMIN — MOVE TO ALLOCATION PREREQUISITES

---

### TC-APP-055: Move APPROVED app to AWAITING_ALLOCATION with all docs approved — success

| Field | Value |
|-------|-------|
| **ID** | TC-APP-055 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Admin is logged in; an APPROVED application exists; all required documents for this application have status APPROVED |
| **Test Data** | APPROVED application ID with all documents approved |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open application detail dialog to verify all documents are APPROVED | All documents show green/approved status |
| 2 | Click "Move to Allocation" action | POST `/api/applications/{id}/move-to-allocation` called |
| 3 | Verify HTTP 200 response | Success response returned |
| 4 | Verify success notification | "Application moved to allocation phase" snackbar |
| 5 | Verify application status is now AWAITING_ALLOCATION | Status updated |

| Field | Value |
|-------|-------|
| **Postconditions** | Application is in AWAITING_ALLOCATION status |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | This is the happy path for move-to-allocation |

---

### TC-APP-056: Move APPROVED app to AWAITING_ALLOCATION with docs PENDING — blocked

| Field | Value |
|-------|-------|
| **ID** | TC-APP-056 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Admin is logged in; an APPROVED application exists; at least one required document has status PENDING |
| **Test Data** | APPROVED application ID with at least 1 PENDING document |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open application detail dialog to verify document statuses | At least one document shows PENDING status |
| 2 | Click "Move to Allocation" action | POST `/api/applications/{id}/move-to-allocation` called |
| 3 | Verify HTTP 400 Bad Request returned | Error response received |
| 4 | Verify error message | Message indicates "Cannot move to allocation — requirements not met" or lists unapproved documents |
| 5 | Verify application status remains APPROVED | Status unchanged |

| Field | Value |
|-------|-------|
| **Postconditions** | Application remains APPROVED; allocation blocked until docs are approved |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Backend validates all required documents before allowing transition |

---

### TC-APP-057: Move APPROVED app to AWAITING_ALLOCATION with docs REJECTED — blocked

| Field | Value |
|-------|-------|
| **ID** | TC-APP-057 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Admin is logged in; an APPROVED application exists; at least one required document has status REJECTED |
| **Test Data** | APPROVED application ID with at least 1 REJECTED document |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open application detail dialog to verify document statuses | At least one document shows REJECTED status |
| 2 | Click "Move to Allocation" action | POST `/api/applications/{id}/move-to-allocation` called |
| 3 | Verify HTTP 400 Bad Request returned | Error response received |
| 4 | Verify error message | Message indicates documents must be approved |
| 5 | Verify application status remains APPROVED | Status unchanged |

| Field | Value |
|-------|-------|
| **Postconditions** | Application remains APPROVED; student must re-upload rejected documents |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## H. ADMIN — OTHER ACTIONS

---

### TC-APP-058: Request changes with reason text — student notified

| Field | Value |
|-------|-------|
| **ID** | TC-APP-058 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Admin is logged in; an UNDER_REVIEW application exists; email service (MailHog) is running |
| **Test Data** | UNDER_REVIEW application ID; reason: "Your proof of enrollment document is expired. Please upload a current version." |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate UNDER_REVIEW application | Application visible |
| 2 | Click "Request Changes" action | Prompt dialog appears |
| 3 | Enter reason: "Your proof of enrollment document is expired. Please upload a current version." | Text entered |
| 4 | Click OK to confirm | POST `/api/applications/{id}/request-changes` sent |
| 5 | Verify success notification | "Changes requested successfully" snackbar |
| 6 | Verify status changes to CHANGES_REQUESTED | Status updated |
| 7 | Check MailHog UI (http://localhost:8025) for notification email | Email sent to student with the reason text |
| 8 | Verify email content includes the reason | Email body contains "Your proof of enrollment document is expired..." |

| Field | Value |
|-------|-------|
| **Postconditions** | Application is CHANGES_REQUESTED; notification email sent to student |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Email sending is async — may have slight delay |

---

### TC-APP-059: Save review progress (partial review)

| Field | Value |
|-------|-------|
| **ID** | TC-APP-059 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Admin is logged in; an application exists with uploaded documents |
| **Test Data** | Application ID; partial document review data (approve some docs, leave others) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open application detail dialog | Dialog loads with document list |
| 2 | Review 2 of 4 documents (approve 1, add comment to another) | Document statuses updated in the dialog |
| 3 | Click "Save Progress" button | POST `/api/applications/{id}/save-review-progress` called |
| 4 | Verify HTTP 200 response | ReviewProgressResponse returned |
| 5 | Verify success notification | "Review progress saved successfully" snackbar |
| 6 | Close and reopen the dialog | Previously saved progress is retained — document statuses and comments persist |

| Field | Value |
|-------|-------|
| **Postconditions** | Partial review saved; admin can resume later |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Atomic save — all updates applied together or rolled back |

---

### TC-APP-060: View application full details including student info, documents, eligibility

| Field | Value |
|-------|-------|
| **ID** | TC-APP-060 |
| **Module** | Application Management |
| **Priority** | High |
| **Preconditions** | Admin is logged in; application exists with student profile, documents, and preferences |
| **Test Data** | Any application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/applications` | Page loads |
| 2 | Click "View Details" on an application | Detail dialog opens |
| 3 | Verify student profile section | Student first name, last name, email, student number displayed |
| 4 | Verify course and institution | CourseName and institutionName shown |
| 5 | Verify documents section | List of documents with type, upload date, status, and comments |
| 6 | Verify required documents checklist | Shows which required document types have been uploaded |
| 7 | Verify allocation eligibility section | Shows whether application meets requirements for allocation |
| 8 | Verify preferences section | Preferred buildings, accessibility, special requirements |

| Field | Value |
|-------|-------|
| **Postconditions** | Full application details viewed by admin |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Uses ApplicationFullDetailsResponse DTO |

---

## I. ROLE-BASED ACCESS

---

### TC-APP-061: Tenant admin can only see applications in their tenant

| Field | Value |
|-------|-------|
| **ID** | TC-APP-061 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Tenant Admin is logged in; applications exist in multiple tenants |
| **Test Data** | tenantadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin | Login successful |
| 2 | Navigate to `/admin/applications` | Applications list loads |
| 3 | Clear all filters and load all applications | Full list displayed |
| 4 | Verify all displayed applications belong to the tenant admin's tenant | No cross-tenant applications visible |
| 5 | Attempt to access an application from another tenant by ID via API: GET `/api/applications/{otherTenantAppId}` | HTTP 403 or 404 returned (Hibernate tenant filter excludes it) |

| Field | Value |
|-------|-------|
| **Postconditions** | Tenant isolation confirmed |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Multi-tenancy enforced by Hibernate `tenantFilter` aspect |

---

### TC-APP-062: Platform admin can see all applications

| Field | Value |
|-------|-------|
| **ID** | TC-APP-062 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Platform Admin is logged in; applications exist in multiple tenants |
| **Test Data** | platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Platform Admin | Login successful |
| 2 | Navigate to `/admin/applications` | Applications list loads |
| 3 | Clear all filters | Full list displayed |
| 4 | Verify applications from multiple tenants are visible | Cross-tenant applications present in the list |
| 5 | Verify total count includes all tenants' applications | Count matches sum of all tenant applications |

| Field | Value |
|-------|-------|
| **Postconditions** | Platform admin has full visibility across tenants |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Platform admin role bypasses tenant filter or sees all tenants |

---

### TC-APP-063: Student cannot access /admin/applications

| Field | Value |
|-------|-------|
| **ID** | TC-APP-063 |
| **Module** | Application Management |
| **Priority** | Critical |
| **Preconditions** | Student A is logged in |
| **Test Data** | john.doe@test.com / Student@123 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A | Login successful |
| 2 | Navigate to `/admin/applications` in the browser | Angular guard blocks access; user is redirected to student dashboard or shown access denied |
| 3 | Attempt API call: GET `/api/applications` (admin endpoint) using student's auth token | HTTP 403 Forbidden returned |
| 4 | Attempt API call: PATCH `/api/applications/{id}/status` using student's auth token | HTTP 403 Forbidden returned |
| 5 | Attempt API call: POST `/api/applications/{id}/mark-under-review` using student's auth token | HTTP 403 Forbidden returned |

| Field | Value |
|-------|-------|
| **Postconditions** | Student has no access to admin application management features |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Both frontend guards (admin.guard.ts) and backend @PreAuthorize enforce role restrictions |

---

## Summary

| Section | Test Cases | IDs |
|---------|-----------|-----|
| A. Student — Submit Application | 7 | TC-APP-001 to TC-APP-007 |
| B. Student — View Applications | 4 | TC-APP-008 to TC-APP-011 |
| C. Student — Cancel Application | 5 | TC-APP-012 to TC-APP-016 |
| D. Admin — View & Search Applications | 10 | TC-APP-017 to TC-APP-026 |
| E. Admin — Valid Status Transitions | 20 | TC-APP-027 to TC-APP-046 |
| F. Admin — Invalid Status Transitions | 8 | TC-APP-047 to TC-APP-054 |
| G. Admin — Move to Allocation Prerequisites | 3 | TC-APP-055 to TC-APP-057 |
| H. Admin — Other Actions | 3 | TC-APP-058 to TC-APP-060 |
| I. Role-Based Access | 3 | TC-APP-061 to TC-APP-063 |
| **Total** | **63** | **TC-APP-001 to TC-APP-063** |
