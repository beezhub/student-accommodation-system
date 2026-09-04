# Activity Logs — UAT Test Cases

> **Module:** Activity Logs  
> **Routes:** `/admin/activity-logs` (Frontend), `/api/admin/activity-logs` (Backend)  
> **Guard:** PlatformAdminGuard  
> **Actions:** CREATE, UPDATE, DELETE, LOGIN, LOGOUT, APPROVE, REJECT, UPLOAD, DOWNLOAD, VIEW, EXPORT  
> **Entity Types:** APPLICATION, DOCUMENT, USER, ROOM, PAYMENT, NOTIFICATION, SYSTEM  
> **Total Test Cases:** 19

---

### TC-LOG-001: View activity logs page

| Field | Value |
|-------|-------|
| **ID** | TC-LOG-001 |
| **Module** | Activity Logs |
| **Priority** | Critical |
| **Preconditions** | Platform admin logged in; activity logs exist in the system |
| **Test Data** | Login: platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as platform admin | Login succeeds |
| 2 | Navigate to `/admin/activity-logs` | Activity Logs page loads without errors |
| 3 | Observe the logs table | Logs are displayed in a table with columns: timestamp, user email/name, user role, action, entity type, entity ID, description, IP address |
| 4 | Verify logs are sorted by timestamp (most recent first) | Most recent log entry appears at the top |
| 5 | Verify filter controls are visible | Filter dropdowns/inputs for action, entity type, user, role, and date range are present |

| Field | Value |
|-------|-------|
| **Postconditions** | Activity logs page is displayed with data and filter controls |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-LOG-002: Filter by action (CREATE)

| Field | Value |
|-------|-------|
| **ID** | TC-LOG-002 |
| **Module** | Activity Logs |
| **Priority** | High |
| **Preconditions** | Platform admin logged in; logs with CREATE action exist |
| **Test Data** | Login: platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/activity-logs` | Page loads with all logs |
| 2 | Select "CREATE" from the action filter dropdown | Filter is applied |
| 3 | Observe the filtered results | Only log entries with action = CREATE are displayed |
| 4 | Verify no other action types appear in the results | All visible rows show "CREATE" in the action column |
| 5 | Clear the filter | All logs are displayed again |

| Field | Value |
|-------|-------|
| **Postconditions** | Logs filtered correctly by CREATE action |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-LOG-003: Filter by action (LOGIN)

| Field | Value |
|-------|-------|
| **ID** | TC-LOG-003 |
| **Module** | Activity Logs |
| **Priority** | High |
| **Preconditions** | Platform admin logged in; logs with LOGIN action exist |
| **Test Data** | Login: platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/activity-logs` | Page loads |
| 2 | Select "LOGIN" from the action filter dropdown | Filter is applied |
| 3 | Observe the filtered results | Only log entries with action = LOGIN are displayed |
| 4 | Verify each entry shows user information and timestamp | Login entries include the user who logged in and when |

| Field | Value |
|-------|-------|
| **Postconditions** | Logs filtered correctly by LOGIN action |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-LOG-004: Filter by entity type (APPLICATION)

| Field | Value |
|-------|-------|
| **ID** | TC-LOG-004 |
| **Module** | Activity Logs |
| **Priority** | High |
| **Preconditions** | Platform admin logged in; logs related to APPLICATION entity exist |
| **Test Data** | Login: platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/activity-logs` | Page loads |
| 2 | Select "APPLICATION" from the entity type filter dropdown | Filter is applied |
| 3 | Observe the filtered results | Only logs where entity type = APPLICATION are displayed |
| 4 | Verify entries include various actions on applications (CREATE, UPDATE, APPROVE, REJECT) | Multiple action types may appear, all related to APPLICATION entity |

| Field | Value |
|-------|-------|
| **Postconditions** | Logs filtered correctly by APPLICATION entity type |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-LOG-005: Filter by entity type (DOCUMENT)

| Field | Value |
|-------|-------|
| **ID** | TC-LOG-005 |
| **Module** | Activity Logs |
| **Priority** | High |
| **Preconditions** | Platform admin logged in; logs related to DOCUMENT entity exist |
| **Test Data** | Login: platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/activity-logs` | Page loads |
| 2 | Select "DOCUMENT" from the entity type filter dropdown | Filter is applied |
| 3 | Observe the filtered results | Only logs where entity type = DOCUMENT are displayed |
| 4 | Verify entries include UPLOAD, APPROVE, REJECT actions on documents | Document-related actions are shown |

| Field | Value |
|-------|-------|
| **Postconditions** | Logs filtered correctly by DOCUMENT entity type |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-LOG-006: Filter by user

| Field | Value |
|-------|-------|
| **ID** | TC-LOG-006 |
| **Module** | Activity Logs |
| **Priority** | High |
| **Preconditions** | Platform admin logged in; logs from multiple users exist |
| **Test Data** | Login: platformadmin@test.com / Admin@12345; Filter target: tenantadmin@test.com |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/activity-logs` | Page loads with logs from all users |
| 2 | Enter or select the target user (e.g., tenantadmin@test.com) in the user filter | Filter is applied |
| 3 | Observe the filtered results | Only log entries where userId matches the selected user are displayed |
| 4 | Verify all visible entries show the filtered user's email/name | No entries from other users appear |

| Field | Value |
|-------|-------|
| **Postconditions** | Logs filtered correctly by specific user |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-LOG-007: Filter by role

| Field | Value |
|-------|-------|
| **ID** | TC-LOG-007 |
| **Module** | Activity Logs |
| **Priority** | Medium |
| **Preconditions** | Platform admin logged in; logs from users with different roles exist |
| **Test Data** | Login: platformadmin@test.com / Admin@12345; Filter by role: STUDENT |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/activity-logs` | Page loads |
| 2 | Select "STUDENT" from the role filter dropdown | Filter is applied |
| 3 | Observe the filtered results | Only log entries where userRole = STUDENT are displayed |
| 4 | Verify all entries show the STUDENT role | No entries from ADMIN or PLATFORM_ADMIN roles appear |

| Field | Value |
|-------|-------|
| **Postconditions** | Logs filtered correctly by user role |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-LOG-008: Filter by date range

| Field | Value |
|-------|-------|
| **ID** | TC-LOG-008 |
| **Module** | Activity Logs |
| **Priority** | High |
| **Preconditions** | Platform admin logged in; logs exist across multiple dates |
| **Test Data** | Login: platformadmin@test.com / Admin@12345; Date range: today only |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/activity-logs` | Page loads |
| 2 | Set the start date to today's date | Start date filter is set |
| 3 | Set the end date to today's date | End date filter is set |
| 4 | Apply the filter | Results are filtered |
| 5 | Observe the results | Only log entries with timestamps within today's date range are displayed |
| 6 | Verify no entries from other dates appear | All timestamps fall within the selected range |

| Field | Value |
|-------|-------|
| **Postconditions** | Logs filtered correctly by date range |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-LOG-009: Combine multiple filters

| Field | Value |
|-------|-------|
| **ID** | TC-LOG-009 |
| **Module** | Activity Logs |
| **Priority** | High |
| **Preconditions** | Platform admin logged in; diverse log entries exist |
| **Test Data** | Login: platformadmin@test.com / Admin@12345; Filters: action=CREATE, entityType=APPLICATION, date range=today |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/activity-logs` | Page loads |
| 2 | Select action filter: "CREATE" | Filter applied |
| 3 | Select entity type filter: "APPLICATION" | Filter added |
| 4 | Set date range to today | Date filter added |
| 5 | Observe the results | Only entries matching ALL three criteria are displayed: CREATE action, APPLICATION entity, within today's date |
| 6 | Verify each entry satisfies all filters | No entries violate any of the applied filters |

| Field | Value |
|-------|-------|
| **Postconditions** | Multiple filters apply as AND conditions; results satisfy all criteria |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-LOG-010: Verify log created when user logs in

| Field | Value |
|-------|-------|
| **ID** | TC-LOG-010 |
| **Module** | Activity Logs |
| **Priority** | Critical |
| **Preconditions** | Platform admin can view activity logs; a known user will log in |
| **Test Data** | Login (to trigger log): tenantadmin@test.com / Admin@12345; Viewer: platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as tenant admin (tenantadmin@test.com) in a separate browser/incognito | Login succeeds |
| 2 | Log out of the tenant admin session | Logout succeeds |
| 3 | Log in as platform admin (platformadmin@test.com) | Login succeeds |
| 4 | Navigate to `/admin/activity-logs` | Page loads |
| 5 | Filter by action: LOGIN | Filter applied |
| 6 | Look for the most recent LOGIN entry | An entry exists for tenantadmin@test.com with action=LOGIN, timestamp matching the login time |
| 7 | Verify the log entry contains: userEmail, userRole, ipAddress, timestamp | All fields are populated |

| Field | Value |
|-------|-------|
| **Postconditions** | LOGIN activity is logged with correct user details |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-LOG-011: Verify log created when application created

| Field | Value |
|-------|-------|
| **ID** | TC-LOG-011 |
| **Module** | Activity Logs |
| **Priority** | Critical |
| **Preconditions** | A student can submit an application; platform admin can view logs |
| **Test Data** | Student: a registered student account; Viewer: platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as a student and submit a new accommodation application | Application submitted successfully |
| 2 | Note the application ID or code | Recorded for verification |
| 3 | Log in as platform admin | Login succeeds |
| 4 | Navigate to `/admin/activity-logs` | Page loads |
| 5 | Filter by action: CREATE, entity type: APPLICATION | Filter applied |
| 6 | Locate the log entry for the newly created application | Entry exists with action=CREATE, entityType=APPLICATION, entityId matching the application |
| 7 | Verify description contains relevant details | Description indicates application creation |

| Field | Value |
|-------|-------|
| **Postconditions** | Application creation is logged as CREATE/APPLICATION |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-LOG-012: Verify log created when document uploaded

| Field | Value |
|-------|-------|
| **ID** | TC-LOG-012 |
| **Module** | Activity Logs |
| **Priority** | Critical |
| **Preconditions** | A student can upload documents; platform admin can view logs |
| **Test Data** | Student: a registered student account; a valid document file (PDF/image); Viewer: platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as a student and upload a document | Document uploaded successfully |
| 2 | Note the document details | Recorded for verification |
| 3 | Log in as platform admin | Login succeeds |
| 4 | Navigate to `/admin/activity-logs` | Page loads |
| 5 | Filter by action: UPLOAD, entity type: DOCUMENT | Filter applied |
| 6 | Locate the log entry for the uploaded document | Entry exists with action=UPLOAD, entityType=DOCUMENT |
| 7 | Verify the log contains the user who uploaded and the entity ID | Fields are populated correctly |

| Field | Value |
|-------|-------|
| **Postconditions** | Document upload is logged as UPLOAD/DOCUMENT |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-LOG-013: Verify log created when room created/updated/deleted

| Field | Value |
|-------|-------|
| **ID** | TC-LOG-013 |
| **Module** | Activity Logs |
| **Priority** | High |
| **Preconditions** | Admin can manage rooms; platform admin can view logs |
| **Test Data** | Admin: platformadmin@test.com / Admin@12345; a building exists to add rooms to |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as platform admin | Login succeeds |
| 2 | Create a new room in an existing building | Room created successfully |
| 3 | Navigate to `/admin/activity-logs` and filter: action=CREATE, entityType=ROOM | A log entry exists for the room creation |
| 4 | Go back and update the room (e.g., change capacity) | Room updated successfully |
| 5 | Check activity logs with filter: action=UPDATE, entityType=ROOM | A log entry exists for the room update |
| 6 | Go back and delete the room | Room deleted successfully |
| 7 | Check activity logs with filter: action=DELETE, entityType=ROOM | A log entry exists for the room deletion |

| Field | Value |
|-------|-------|
| **Postconditions** | All room CRUD operations are logged with correct action and entity type |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-LOG-014: Verify log created when application approved/rejected

| Field | Value |
|-------|-------|
| **ID** | TC-LOG-014 |
| **Module** | Activity Logs |
| **Priority** | Critical |
| **Preconditions** | Admin can review applications; submitted applications exist |
| **Test Data** | Admin: platformadmin@test.com / Admin@12345; at least two submitted applications |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as platform admin | Login succeeds |
| 2 | Navigate to applications review and approve one application | Application approved |
| 3 | Navigate to `/admin/activity-logs` and filter: action=APPROVE, entityType=APPLICATION | A log entry exists for the approval with the correct application entityId |
| 4 | Go back and reject another application | Application rejected |
| 5 | Check activity logs with filter: action=REJECT, entityType=APPLICATION | A log entry exists for the rejection with the correct application entityId |

| Field | Value |
|-------|-------|
| **Postconditions** | Application approve/reject actions are logged correctly |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-LOG-015: Export logs to CSV

| Field | Value |
|-------|-------|
| **ID** | TC-LOG-015 |
| **Module** | Activity Logs |
| **Priority** | High |
| **Preconditions** | Platform admin logged in; activity logs exist |
| **Test Data** | Login: platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/activity-logs` | Page loads with logs |
| 2 | Optionally apply filters (e.g., action=LOGIN) | Filtered results displayed |
| 3 | Click the "Export" or "Export CSV" button | A file download is initiated |
| 4 | Verify the downloaded file | File name follows pattern: `activity-logs-YYYY-MM-DD.csv` |
| 5 | Open the CSV file | File contains headers and data rows matching the displayed/filtered logs |
| 6 | Verify CSV columns include: timestamp, user email, user role, action, entity type, entity ID, description | All expected columns are present |
| 7 | Verify the data matches the filtered view | Row count and content match the UI display |

| Field | Value |
|-------|-------|
| **Postconditions** | CSV file downloaded with correct activity log data |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-LOG-016: Pagination through logs

| Field | Value |
|-------|-------|
| **ID** | TC-LOG-016 |
| **Module** | Activity Logs |
| **Priority** | High |
| **Preconditions** | Platform admin logged in; more logs exist than fit on one page |
| **Test Data** | Login: platformadmin@test.com / Admin@12345; system has > 1 page of logs |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/activity-logs` | Page loads; first page of logs displayed |
| 2 | Observe the pagination controls | Page number, total pages/elements, and navigation buttons (next, previous) are visible |
| 3 | Note the last entry on page 1 | Entry recorded |
| 4 | Click "Next page" or page 2 | Second page of logs loads |
| 5 | Verify the entries on page 2 are different from page 1 | No duplicate entries between pages |
| 6 | Click "Previous page" or page 1 | First page of logs loads again with the same entries as before |
| 7 | Change the page size (if supported, e.g., 10 → 25) | More entries displayed per page |

| Field | Value |
|-------|-------|
| **Postconditions** | Pagination works correctly; all logs are accessible across pages |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-LOG-017: Empty filter results

| Field | Value |
|-------|-------|
| **ID** | TC-LOG-017 |
| **Module** | Activity Logs |
| **Priority** | Medium |
| **Preconditions** | Platform admin logged in |
| **Test Data** | Login: platformadmin@test.com / Admin@12345; use a filter combination that yields no results |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/activity-logs` | Page loads |
| 2 | Apply a filter combination that produces no results (e.g., entityType=PAYMENT with a date range where no payment logs exist) | Filter applied |
| 3 | Observe the results area | An empty state message is displayed (e.g., "No activity logs found" or an empty table) |
| 4 | Verify no error occurs | No error snackbar or console errors; empty state is handled gracefully |
| 5 | Clear filters | All logs are displayed again |

| Field | Value |
|-------|-------|
| **Postconditions** | Empty results are handled gracefully with appropriate messaging |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-LOG-018: Tenant admin cannot access /admin/activity-logs

| Field | Value |
|-------|-------|
| **ID** | TC-LOG-018 |
| **Module** | Activity Logs |
| **Priority** | High |
| **Preconditions** | Tenant admin logged in |
| **Test Data** | Login: tenantadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as tenant admin | Login succeeds |
| 2 | Check the admin sidebar | "Activity Logs" link is NOT visible (platformAdminOnly: true) |
| 3 | Manually navigate to `http://localhost:4200/admin/activity-logs` | User is redirected away from the page |
| 4 | Attempt API call: `GET /api/admin/activity-logs` with tenant admin JWT | Response: 403 Forbidden |

| Field | Value |
|-------|-------|
| **Postconditions** | Tenant admin cannot access activity logs |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-LOG-019: Student cannot access /admin/activity-logs

| Field | Value |
|-------|-------|
| **ID** | TC-LOG-019 |
| **Module** | Activity Logs |
| **Priority** | High |
| **Preconditions** | Student user logged in |
| **Test Data** | A registered student account with valid credentials |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as a student | Login succeeds; redirected to student dashboard |
| 2 | Manually navigate to `http://localhost:4200/admin/activity-logs` | User is redirected away (to student dashboard or login) |
| 3 | Verify the activity logs page content is NOT displayed | No log data is visible; route guard prevents access |

| Field | Value |
|-------|-------|
| **Postconditions** | Student cannot access activity logs |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |
