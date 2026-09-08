# Scheduled Jobs — UAT Test Cases

> **Module:** Scheduled Jobs
> **System Under Test:** Background jobs verified through observable effects (allocation status changes, emails, file cleanup)
> **Environment:** Backend http://localhost:8080 | MailHog http://localhost:8025 | Swagger http://localhost:8080/swagger-ui.html
> **Note:** Jobs run on intervals (offer/check-in expiry: 1h, batch allocation: 6h, orphan cleanup: daily 3AM, notification retry: 5min). For UAT, the backend should be configured with shortened intervals or jobs triggered via Actuator/restart. All verification is done through API responses, UI state, and MailHog.

---

## Test Data Reference

| Role | Email | Password |
|------|-------|----------|
| Platform Admin | platformadmin@test.com | Admin@12345 |
| Tenant Admin | tenantadmin@test.com | Admin@12345 |
| Student A | john.doe@test.com | Student@123 |
| Student B | jane.smith@test.com | Student@123 |
| Student C | alex.jones@test.com | Student@123 |

| Building | Room | Capacity | Gender |
|----------|------|----------|--------|
| Residence Hall Alpha | A-101 | 2 | ANY |
| Residence Hall Beta | B-201 | 2 | FEMALE_ONLY |
| Residence Hall Gamma | C-301 | 3 | MALE_ONLY |

---

### TC-JOB-001: Offer Expiry Job — Expires Past-Due OFFERED Allocation

| Field | Value |
|-------|-------|
| **ID** | TC-JOB-001 |
| **Module** | Scheduled Jobs |
| **Priority** | Critical |
| **Preconditions** | Student A (john.doe@test.com) has an APPROVED application. Tenant admin is logged in. Room A-101 has capacity. Backend is running with offer expiry job active. |
| **Test Data** | Student A, Room A-101, offer_expiry_date set to a past timestamp (e.g., yesterday) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin at http://localhost:4200/login with tenantadmin@test.com / Admin@12345 | Admin dashboard loads |
| 2 | Navigate to Applications → find Student A's application → move to AWAITING_ALLOCATION status | Application status changes to AWAITING_ALLOCATION |
| 3 | Navigate to Allocations → Allocate Student A to Room A-101 | Allocation created with status OFFERED |
| 4 | Via Swagger (PUT /api/allocations/{id}), set `offerExpiryDate` to yesterday's date (e.g., `2025-01-01T00:00:00`) | API returns 200; offer_expiry_date updated |
| 5 | Wait for the offer expiry job to run (up to 1 hour, or restart backend to trigger immediately) | Job executes (check backend logs for `job=offerExpiry action=complete processed=1`) |
| 6 | Via Swagger, GET /api/allocations/{id} and check the allocation status | Status is `EXPIRED` |
| 7 | Navigate to Applications → find Student A's application | Application status reflects that allocation expired |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation status is EXPIRED. Room A-101 capacity is freed. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-JOB-002: Offer Expiry Job — Email Notification Sent to Student

| Field | Value |
|-------|-------|
| **ID** | TC-JOB-002 |
| **Module** | Scheduled Jobs |
| **Priority** | Critical |
| **Preconditions** | TC-JOB-001 has been executed. MailHog is running and accessible. |
| **Test Data** | Student A (john.doe@test.com), expired allocation from TC-JOB-001 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 2 | Search for emails to john.doe@test.com | List of emails displayed |
| 3 | Find an email with subject containing "Offer Expired" or "Room Offer Expired" | Email exists in the inbox |
| 4 | Open the email and inspect the body | Body contains: student name, room/building details, message explaining the offer has expired, and support contact information |
| 5 | Verify the "To" field is john.doe@test.com | Correct recipient |
| 6 | Verify the "From" field matches the configured support email | Correct sender |

| Field | Value |
|-------|-------|
| **Postconditions** | Offer expiry notification email confirmed delivered to MailHog. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-JOB-003: Check-In Expiry Job — Expires Past-Due ACCEPTED Allocation

| Field | Value |
|-------|-------|
| **ID** | TC-JOB-003 |
| **Module** | Scheduled Jobs |
| **Priority** | Critical |
| **Preconditions** | Student B (jane.smith@test.com) has an ACCEPTED allocation for Room B-201. Backend is running. |
| **Test Data** | Student B, Room B-201, check_in_expiry_date set to a past timestamp |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin. Ensure Student B has an application in AWAITING_ALLOCATION status. | Confirmed |
| 2 | Allocate Student B to Room B-201 → allocation status OFFERED | Allocation created |
| 3 | Log in as Student B at http://localhost:4200/login with jane.smith@test.com / Student@123 | Student dashboard loads |
| 4 | Navigate to Allocations → Accept the offer for Room B-201 | Allocation status changes to ACCEPTED |
| 5 | Via Swagger (PUT /api/allocations/{id}), set `checkInExpiryDate` to yesterday's date | API returns 200; check-in deadline updated to past |
| 6 | Wait for check-in expiry job to run (up to 1 hour, or restart backend) | Job executes (logs: `job=checkInExpiry action=complete processed=1`) |
| 7 | Via Swagger, GET /api/allocations/{id} | Status is `CHECK_IN_EXPIRED` |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation status is CHECK_IN_EXPIRED. Room B-201 capacity freed. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-JOB-004: Check-In Expiry Job — Email Notification Sent

| Field | Value |
|-------|-------|
| **ID** | TC-JOB-004 |
| **Module** | Scheduled Jobs |
| **Priority** | Critical |
| **Preconditions** | TC-JOB-003 has been executed. MailHog is running. |
| **Test Data** | Student B (jane.smith@test.com) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 2 | Search for emails to jane.smith@test.com | Emails listed |
| 3 | Find email with subject containing "Check-In Deadline Expired" or "Check-In Expired" | Email exists |
| 4 | Open the email and inspect the body | Body contains: student name, room/building reference, explanation that check-in deadline has passed, and next steps or support contact |
| 5 | Verify recipient is jane.smith@test.com | Correct |

| Field | Value |
|-------|-------|
| **Postconditions** | Check-in expiry email confirmed in MailHog. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-JOB-005: Offer Reminder Job — Sends Reminder 24h Before Expiry

| Field | Value |
|-------|-------|
| **ID** | TC-JOB-005 |
| **Module** | Scheduled Jobs |
| **Priority** | High |
| **Preconditions** | Student C (alex.jones@test.com) has an OFFERED allocation. Backend is running with offer reminder job active. MailHog cleared of previous emails for clarity. |
| **Test Data** | Student C, Room C-301, offer_expiry_date set to ~23 hours from now |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin. Ensure Student C has application in AWAITING_ALLOCATION. | Confirmed |
| 2 | Allocate Student C to Room C-301 → status OFFERED | Allocation created |
| 3 | Via Swagger, set `offerExpiryDate` to 23 hours from current time | API returns 200 |
| 4 | Wait for offer reminder job to run (up to 1 hour) | Job executes (logs: `job=offerReminder action=complete processed=1`) |
| 5 | Open MailHog → search for emails to alex.jones@test.com | Emails listed |
| 6 | Find email with subject containing "Action Required" or "Offer Expiring Soon" | Reminder email exists |
| 7 | Open email → verify body contains offer expiry date/time and instructions to accept/decline | Body is informative with deadline details |

| Field | Value |
|-------|-------|
| **Postconditions** | Offer reminder email sent. Allocation remains OFFERED. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-JOB-006: Check-In Reminder Job — Sends Reminder 24h Before Deadline

| Field | Value |
|-------|-------|
| **ID** | TC-JOB-006 |
| **Module** | Scheduled Jobs |
| **Priority** | High |
| **Preconditions** | Student A has an ACCEPTED allocation. Check-in deadline is ~23 hours from now. MailHog accessible. |
| **Test Data** | Student A (john.doe@test.com), Room A-101, check_in_expiry_date = 23 hours from now |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Ensure Student A has an ACCEPTED allocation for Room A-101 (accept offer via student UI or API) | Allocation status is ACCEPTED |
| 2 | Via Swagger, set `checkInExpiryDate` to 23 hours from now | API returns 200 |
| 3 | Wait for check-in reminder job to run (up to 1 hour) | Job executes (logs: `job=checkInReminder action=complete processed=1`) |
| 4 | Open MailHog → search for emails to john.doe@test.com | Emails listed |
| 5 | Find email with subject containing "Check-In Reminder" or "Check-In Deadline" | Reminder email found |
| 6 | Verify email body contains check-in deadline date and instructions | Body is correct |

| Field | Value |
|-------|-------|
| **Postconditions** | Check-in reminder email sent. Allocation remains ACCEPTED. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-JOB-007: Reminder Sent Only Once (reminder_24h_sent Flag)

| Field | Value |
|-------|-------|
| **ID** | TC-JOB-007 |
| **Module** | Scheduled Jobs |
| **Priority** | High |
| **Preconditions** | TC-JOB-005 completed — Student C has already received one offer reminder. Offer is still within 24h window. |
| **Test Data** | Student C (alex.jones@test.com), same allocation from TC-JOB-005 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Clear MailHog inbox (delete all emails) or note the current email count for alex.jones@test.com | Baseline noted |
| 2 | Wait for the offer reminder job to run again (next cycle, up to 1 hour) | Job executes (logs: `job=offerReminder action=complete processed=0`) |
| 3 | Check MailHog for new emails to alex.jones@test.com | No new reminder email sent |
| 4 | Via Swagger, GET the allocation and check `reminder24hSent` or equivalent flag | Flag is `true`, confirming reminder was already sent |

| Field | Value |
|-------|-------|
| **Postconditions** | No duplicate reminder. Flag prevents re-sending. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-JOB-008: Batch Allocation Disabled via Feature Flag

| Field | Value |
|-------|-------|
| **ID** | TC-JOB-008 |
| **Module** | Scheduled Jobs |
| **Priority** | Critical |
| **Preconditions** | Platform Admin is logged in. Students exist on the waiting list with AWAITING_ALLOCATION status. |
| **Test Data** | Platform Admin (platformadmin@test.com), auto_allocation_enabled = false |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Platform Admin at http://localhost:4200/login | Admin dashboard loads |
| 2 | Navigate to System Settings (or Feature Flags) → find `auto_allocation_enabled` | Setting is displayed |
| 3 | Set `auto_allocation_enabled` to `false` (disabled) → Save | Setting saved successfully |
| 4 | Ensure at least one student application is in AWAITING_ALLOCATION status | Confirmed via Applications list |
| 5 | Wait for batch allocation job to run (up to 6 hours; or restart backend) | Job runs but skips processing (logs: `job=batchAllocation action=complete processed=0` or skipped message) |
| 6 | Verify no new allocations were created — check Allocations list | No new OFFERED allocations |
| 7 | Verify applications remain in AWAITING_ALLOCATION status | Status unchanged |

| Field | Value |
|-------|-------|
| **Postconditions** | Batch allocation did not execute. Applications unchanged. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-JOB-009: Batch Allocation Enabled — Auto-Allocates from Waiting List

| Field | Value |
|-------|-------|
| **ID** | TC-JOB-009 |
| **Module** | Scheduled Jobs |
| **Priority** | Critical |
| **Preconditions** | auto_allocation_enabled = true. Multiple students have applications in AWAITING_ALLOCATION. Rooms have available capacity. Allocation rules are configured. |
| **Test Data** | Students A, B, C with AWAITING_ALLOCATION applications. Rooms A-101, B-201, C-301 with capacity. |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Platform Admin → System Settings → set `auto_allocation_enabled` to `true` → Save | Setting enabled |
| 2 | Verify Students A, B, C have applications in AWAITING_ALLOCATION | Confirmed |
| 3 | Verify Rooms A-101, B-201, C-301 have available capacity | Confirmed |
| 4 | Wait for the batch allocation job to run (up to 6 hours; or restart backend) | Job runs (logs: `job=batchAllocation action=complete processed=N` where N > 0) |
| 5 | Navigate to Allocations list as Tenant Admin | New allocations visible |
| 6 | Verify allocations were created with status OFFERED for eligible students | Allocations match students to appropriate rooms (respecting gender rules) |
| 7 | Verify offer expiry dates are set on each allocation | Dates are populated (future date based on OFFER_EXPIRY_DAYS config) |
| 8 | Check MailHog — students received offer notification emails | Emails received by allocated students |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocations created automatically. Students notified. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-JOB-010: Orphan File Cleanup — Removes Unreferenced MinIO Files

| Field | Value |
|-------|-------|
| **ID** | TC-JOB-010 |
| **Module** | Scheduled Jobs |
| **Priority** | Medium |
| **Preconditions** | MinIO is running (http://localhost:9001). A file exists in MinIO that has no corresponding DB record. Backend is running with cleanup cron active. |
| **Test Data** | An orphaned file uploaded directly to MinIO or whose DB record was deleted |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A → upload a document via the Documents page | Document uploaded successfully; file exists in MinIO bucket |
| 2 | Note the MinIO file key/path from the document record (via Swagger GET /api/documents/{id}) | File key recorded (e.g., `documents/abc-123.pdf`) |
| 3 | Delete the document DB record via Swagger (DELETE /api/documents/{id}) or via admin UI | DB record deleted; MinIO file still exists |
| 4 | Log in to MinIO Console (http://localhost:9001) → navigate to the bucket → verify the file still exists | File is present (orphaned) |
| 5 | Wait for the orphan cleanup job to run (daily at 3 AM; or adjust cron to run sooner, then restart) | Job runs (logs: `job=orphanCleanup action=complete processed=1`) |
| 6 | Check MinIO Console → navigate to the bucket → search for the file key | File no longer exists — cleaned up |

| Field | Value |
|-------|-------|
| **Postconditions** | Orphaned file removed from MinIO. No data loss for referenced files. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | This job runs daily at 3 AM by default. For UAT, configure `app.storage.cleanup.cron` to a sooner time. |

---

### TC-JOB-011: Notification Retry — Failed Email Retried

| Field | Value |
|-------|-------|
| **ID** | TC-JOB-011 |
| **Module** | Scheduled Jobs |
| **Priority** | High |
| **Preconditions** | A notification exists in the database with status FAILED and retry_count < max_retries (3). MailHog is running. |
| **Test Data** | A FAILED notification record (created by temporarily stopping MailHog during an email-triggering action, then restarting it) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Stop MailHog container (`docker-compose stop mailhog`) | MailHog stopped |
| 2 | Trigger an action that sends email — e.g., as Tenant Admin, approve Student A's application | Action succeeds but email delivery fails; notification record created with status FAILED |
| 3 | Restart MailHog (`docker-compose start mailhog`) | MailHog running again |
| 4 | Wait for notification retry job to run (every 5 minutes) | Job runs (logs: `job=notificationRetry action=complete`) |
| 5 | Check MailHog for the email that was previously failed | Email now delivered successfully |
| 6 | Via Swagger or DB, check the notification record | Status changed to SENT; retry_count incremented |

| Field | Value |
|-------|-------|
| **Postconditions** | Previously failed notification retried and delivered. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-JOB-012: Notification Permanently Failed After Max Retries

| Field | Value |
|-------|-------|
| **ID** | TC-JOB-012 |
| **Module** | Scheduled Jobs |
| **Priority** | High |
| **Preconditions** | A notification record exists with FAILED status and retry_count approaching max (3). MailHog remains stopped to force continued failure. |
| **Test Data** | FAILED notification with retry_count = 2 (one more retry allowed) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Ensure MailHog is stopped (`docker-compose stop mailhog`) | MailHog down |
| 2 | Trigger an email-sending action (e.g., approve a document) | Notification created with FAILED status, retry_count = 0 |
| 3 | Wait for notification retry job to run 3 times (3 × 5 min = ~15 min) | Each retry fails because MailHog is down |
| 4 | After the third failed retry, check the notification record via Swagger or DB | Status is `PERMANENTLY_FAILED`; retry_count = 3 |
| 5 | Wait for one more retry cycle | Job does NOT retry this notification (processed count does not include it) |
| 6 | Restart MailHog (`docker-compose start mailhog`) | MailHog restored for other tests |

| Field | Value |
|-------|-------|
| **Postconditions** | Notification marked PERMANENTLY_FAILED. No further retries attempted. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-JOB-013: Offer Expiry Does NOT Affect ACCEPTED/ACTIVE Allocations

| Field | Value |
|-------|-------|
| **ID** | TC-JOB-013 |
| **Module** | Scheduled Jobs |
| **Priority** | Critical |
| **Preconditions** | Student A has an ACCEPTED allocation. Student B has an ACTIVE allocation (checked in). Both have offer_expiry_date in the past. |
| **Test Data** | Student A — ACCEPTED allocation, offer_expiry_date = yesterday. Student B — ACTIVE allocation, offer_expiry_date = yesterday. |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Set up: Student A has ACCEPTED allocation with past offer_expiry_date (via Swagger) | Confirmed |
| 2 | Set up: Student B has ACTIVE allocation with past offer_expiry_date (via Swagger) | Confirmed |
| 3 | Wait for offer expiry job to run | Job runs (logs: `job=offerExpiry action=complete processed=0`) |
| 4 | GET Student A's allocation via Swagger | Status remains `ACCEPTED` (not expired) |
| 5 | GET Student B's allocation via Swagger | Status remains `ACTIVE` (not expired) |

| Field | Value |
|-------|-------|
| **Postconditions** | ACCEPTED and ACTIVE allocations are unaffected by offer expiry job. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Offer expiry should only target allocations with status = OFFERED. |

---

### TC-JOB-014: Check-In Expiry Does NOT Affect OFFERED Allocations

| Field | Value |
|-------|-------|
| **ID** | TC-JOB-014 |
| **Module** | Scheduled Jobs |
| **Priority** | Critical |
| **Preconditions** | Student C has an OFFERED allocation with check_in_expiry_date in the past (if such a date exists on an OFFERED record). |
| **Test Data** | Student C — OFFERED allocation, check_in_expiry_date = yesterday |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Ensure Student C has an OFFERED allocation | Confirmed |
| 2 | Via Swagger, set `checkInExpiryDate` to yesterday on this allocation | Updated |
| 3 | Wait for check-in expiry job to run | Job runs |
| 4 | GET Student C's allocation via Swagger | Status remains `OFFERED` — not changed to CHECK_IN_EXPIRED |

| Field | Value |
|-------|-------|
| **Postconditions** | OFFERED allocation unaffected by check-in expiry job. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Check-in expiry should only target ACCEPTED allocations. |

---

### TC-JOB-015: Multiple Offers — Only Past-Due Ones Expire

| Field | Value |
|-------|-------|
| **ID** | TC-JOB-015 |
| **Module** | Scheduled Jobs |
| **Priority** | High |
| **Preconditions** | Three OFFERED allocations exist: one with past expiry, one with expiry 48h from now, one with expiry 7 days from now. |
| **Test Data** | Allocation X — offer_expiry_date = yesterday. Allocation Y — offer_expiry_date = 48h from now. Allocation Z — offer_expiry_date = 7 days from now. |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Set up three OFFERED allocations for different students with varying expiry dates via admin + Swagger | Three allocations exist |
| 2 | Wait for offer expiry job to run | Job runs (logs: `processed=1`) |
| 3 | GET Allocation X via Swagger | Status is `EXPIRED` |
| 4 | GET Allocation Y via Swagger | Status remains `OFFERED` |
| 5 | GET Allocation Z via Swagger | Status remains `OFFERED` |
| 6 | Check MailHog — only the expired student received the expiry email | Only one expiry email sent |

| Field | Value |
|-------|-------|
| **Postconditions** | Only the past-due allocation expired. Future-dated allocations are untouched. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |
