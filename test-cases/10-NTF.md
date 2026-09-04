# Notifications — UAT Test Cases

> **Module:** Notifications (Email, In-App, Retry, Edge Cases)
> **Application URL:** http://localhost:4200
> **MailHog URL:** http://localhost:8025
> **SSE Endpoint:** `/api/notifications/stream`
> **Total Test Cases:** 35

## Test Data

| Role | Email | Password |
|------|-------|----------|
| Student A | john.doe@test.com | Student@123 |
| Platform Admin | platformadmin@test.com | Admin@12345 |
| Tenant Admin | tenantadmin@test.com | Admin@12345 |

**Email Templates (19):** welcome, password-reset, application-approved, application-rejected, document-verified, document-rejected, document-comment, room-allocated, allocation-proposal, allocation-confirmed, allocation-rejected, allocation-moved, offer-declined-requeue, offer-declined-max-reached, offer-expired, offer-expiry-reminder, check-in-reminder, check-in-expired

**In-App Notification Types:** ROOM_ALLOCATION, DOCUMENT_APPROVED, DOCUMENT_REJECTED, APPLICATION_APPROVED, APPLICATION_REJECTED, ALLOCATION_CONFIRMED, ALLOCATION_PROPOSED, ALLOCATION_REJECTED, CHECK_IN_CONFIRMED, CHECK_IN_EXPIRED, CHECK_IN_REMINDER, OFFER_EXPIRED, OFFER_EXPIRY_REMINDER, ROOM_MOVED, WAITING_LIST

**Retry Logic:** Max 3 retries, exponential backoff. Status flow: PENDING → SENT or FAILED → RETRYING → SENT or PERMANENTLY_FAILED

---

## Email Notifications

---

### TC-NTF-001: Welcome Email on Signup

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-001 |
| **Module** | Notifications |
| **Priority** | Critical |
| **Preconditions** | MailHog running at http://localhost:8025. No existing account for newstudent@test.com. |
| **Test Data** | Email: newstudent@test.com, Password: Student@123, Name: Jane Smith |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to http://localhost:4200/signup | Signup page loads |
| 2 | Fill in name "Jane Smith", email "newstudent@test.com", password "Student@123", confirm password "Student@123" | Fields populated |
| 3 | Click "Sign Up" button | Success message displayed; user redirected to email verification page |
| 4 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 5 | Locate email sent to newstudent@test.com | Email present in inbox |
| 6 | Verify email subject is "Welcome to Student Accommodation System" | Subject matches |
| 7 | Open email and verify recipient is newstudent@test.com | Recipient correct |
| 8 | Verify email body contains student name "Jane Smith" | Name present in greeting |
| 9 | Verify email contains a verification link | Link present and points to application URL |

| Field | Value |
|-------|-------|
| **Postconditions** | Account created; welcome email in MailHog |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-NTF-002: Password Reset Email

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-002 |
| **Module** | Notifications |
| **Priority** | Critical |
| **Preconditions** | Student A account exists and is verified. MailHog inbox cleared. |
| **Test Data** | Email: john.doe@test.com |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to http://localhost:4200/login | Login page loads |
| 2 | Click "Forgot Password?" link | Forgot password page loads |
| 3 | Enter email "john.doe@test.com" | Email field populated |
| 4 | Click "Send Reset Link" button | Success message: "If an account exists, a reset link has been sent" |
| 5 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 6 | Locate email sent to john.doe@test.com | Email present in inbox |
| 7 | Verify email subject contains "Password Reset Request" | Subject matches |
| 8 | Open email and verify it contains a password reset link | Reset link present |
| 9 | Click the reset link | Redirected to password reset form at http://localhost:4200 with valid token |

| Field | Value |
|-------|-------|
| **Postconditions** | Reset email delivered; link navigable |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-NTF-003: Application Approved Email

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-003 |
| **Module** | Notifications |
| **Priority** | Critical |
| **Preconditions** | Student A has a submitted application in SUBMITTED status. Tenant Admin logged in. MailHog inbox cleared. |
| **Test Data** | Student: john.doe@test.com, Application status: SUBMITTED |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin (tenantadmin@test.com / Admin@12345) | Admin dashboard loads |
| 2 | Navigate to Applications management page | Applications list displayed |
| 3 | Locate Student A's application | Application found in list |
| 4 | Open application details and click "Approve" | Confirmation dialog appears |
| 5 | Confirm approval | Application status changes to APPROVED; success message shown |
| 6 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 7 | Locate email sent to john.doe@test.com | Email present |
| 8 | Verify subject contains "Application Approved" | Subject matches |
| 9 | Verify email body contains student name and application reference | Details correct |
| 10 | Verify email contains a link to view the application | Link present and functional |

| Field | Value |
|-------|-------|
| **Postconditions** | Application in APPROVED status; approval email in MailHog |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-NTF-004: Application Rejected Email

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-004 |
| **Module** | Notifications |
| **Priority** | Critical |
| **Preconditions** | Student A has a submitted application in SUBMITTED status. Tenant Admin logged in. MailHog inbox cleared. |
| **Test Data** | Student: john.doe@test.com, Application status: SUBMITTED |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin (tenantadmin@test.com / Admin@12345) | Admin dashboard loads |
| 2 | Navigate to Applications management page | Applications list displayed |
| 3 | Locate Student A's application and open details | Application details shown |
| 4 | Click "Reject" and provide rejection reason "Incomplete documentation" | Confirmation dialog appears |
| 5 | Confirm rejection | Application status changes to REJECTED; success message shown |
| 6 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 7 | Locate email sent to john.doe@test.com | Email present |
| 8 | Verify subject contains "Application Status Update" | Subject matches |
| 9 | Verify email body contains rejection reason "Incomplete documentation" | Reason present in email |

| Field | Value |
|-------|-------|
| **Postconditions** | Application in REJECTED status; rejection email in MailHog |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-NTF-005: Document Verified Email

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-005 |
| **Module** | Notifications |
| **Priority** | High |
| **Preconditions** | Student A has uploaded a document with PENDING status. Tenant Admin logged in. MailHog inbox cleared. |
| **Test Data** | Student: john.doe@test.com, Document: ID document (PENDING) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin (tenantadmin@test.com / Admin@12345) | Admin dashboard loads |
| 2 | Navigate to Document management page | Documents list displayed |
| 3 | Locate Student A's pending document | Document found |
| 4 | Click "Approve" / "Verify" on the document | Confirmation dialog appears |
| 5 | Confirm document approval | Document status changes to VERIFIED; success message shown |
| 6 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 7 | Locate email sent to john.doe@test.com | Email present |
| 8 | Verify subject contains "Document Verified" | Subject matches |
| 9 | Verify email body references the document type | Document type mentioned |

| Field | Value |
|-------|-------|
| **Postconditions** | Document in VERIFIED status; verification email in MailHog |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-NTF-006: Document Rejected Email

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-006 |
| **Module** | Notifications |
| **Priority** | High |
| **Preconditions** | Student A has uploaded a document with PENDING status. Tenant Admin logged in. MailHog inbox cleared. |
| **Test Data** | Student: john.doe@test.com, Document: ID document (PENDING) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin (tenantadmin@test.com / Admin@12345) | Admin dashboard loads |
| 2 | Navigate to Document management page | Documents list displayed |
| 3 | Locate Student A's pending document | Document found |
| 4 | Click "Reject" on the document and enter reason "Image is blurry and unreadable" | Rejection form shown |
| 5 | Confirm rejection | Document status changes to REJECTED; success message shown |
| 6 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 7 | Locate email sent to john.doe@test.com | Email present |
| 8 | Verify subject contains "Document Review Required" | Subject matches |
| 9 | Verify email body contains rejection reason "Image is blurry and unreadable" | Reason present |

| Field | Value |
|-------|-------|
| **Postconditions** | Document in REJECTED status; rejection email in MailHog |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-NTF-007: Document Comment Email

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-007 |
| **Module** | Notifications |
| **Priority** | High |
| **Preconditions** | Student A has uploaded a document. Tenant Admin logged in. MailHog inbox cleared. |
| **Test Data** | Student: john.doe@test.com, Comment: "Please upload a higher resolution scan" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin (tenantadmin@test.com / Admin@12345) | Admin dashboard loads |
| 2 | Navigate to Document management page | Documents list displayed |
| 3 | Open Student A's document details | Document details shown |
| 4 | Add comment "Please upload a higher resolution scan" | Comment field populated |
| 5 | Submit comment | Comment saved; success message shown |
| 6 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 7 | Locate email sent to john.doe@test.com | Email present |
| 8 | Verify subject contains "Document Review: Action Required" | Subject matches |
| 9 | Verify email body contains the comment text | Comment text present |

| Field | Value |
|-------|-------|
| **Postconditions** | Comment added to document; comment email in MailHog |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-NTF-008: Room Allocated Directly Email

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-008 |
| **Module** | Notifications |
| **Priority** | Critical |
| **Preconditions** | Student A has an APPROVED application. Available room exists. Tenant Admin logged in. MailHog inbox cleared. |
| **Test Data** | Student: john.doe@test.com, Building: Test Building, Room: 101 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin (tenantadmin@test.com / Admin@12345) | Admin dashboard loads |
| 2 | Navigate to Room Allocation management | Allocation page loads |
| 3 | Select Student A's application for direct allocation | Application selected |
| 4 | Assign Room 101 in Test Building | Room selection confirmed |
| 5 | Confirm allocation | Allocation created; success message shown |
| 6 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 7 | Locate email sent to john.doe@test.com | Email present |
| 8 | Verify subject contains "Room Allocation Confirmation" | Subject matches |
| 9 | Verify email body contains building name "Test Building" and room "101" | Details correct |
| 10 | Verify email contains offer acceptance deadline | Deadline present |

| Field | Value |
|-------|-------|
| **Postconditions** | Room allocated to student; allocation email in MailHog |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-NTF-009: Staged Allocation Proposed Email

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-009 |
| **Module** | Notifications |
| **Priority** | High |
| **Preconditions** | Student A has an APPROVED application. Staged allocation created by admin/auto-allocation. MailHog inbox cleared. |
| **Test Data** | Student: john.doe@test.com |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin (tenantadmin@test.com / Admin@12345) | Admin dashboard loads |
| 2 | Navigate to Staged Allocations page | Staged allocations listed |
| 3 | Create a staged allocation for Student A to a specific room | Staged allocation created with PROPOSED status |
| 4 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 5 | Locate email sent to john.doe@test.com | Email present |
| 6 | Verify subject contains "Room Allocation Proposal" | Subject matches |
| 7 | Verify email body contains proposed room and building details | Details correct |

| Field | Value |
|-------|-------|
| **Postconditions** | Staged allocation in PROPOSED status; proposal email in MailHog |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-NTF-010: Staged Allocation Confirmed Email

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-010 |
| **Module** | Notifications |
| **Priority** | High |
| **Preconditions** | Staged allocation in PROPOSED status exists for Student A. Tenant Admin logged in. MailHog inbox cleared. |
| **Test Data** | Student: john.doe@test.com |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin (tenantadmin@test.com / Admin@12345) | Admin dashboard loads |
| 2 | Navigate to Staged Allocations page | Staged allocations listed |
| 3 | Locate Student A's proposed allocation | Allocation found |
| 4 | Click "Confirm" on the staged allocation | Confirmation dialog appears |
| 5 | Confirm the allocation | Status changes to CONFIRMED; success message shown |
| 6 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 7 | Locate email sent to john.doe@test.com | Email present |
| 8 | Verify subject contains "Room Allocation Confirmed" | Subject matches |
| 9 | Verify email body contains confirmed room and building details | Details correct |

| Field | Value |
|-------|-------|
| **Postconditions** | Allocation confirmed; confirmation email in MailHog |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-NTF-011: Staged Allocation Rejected Email

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-011 |
| **Module** | Notifications |
| **Priority** | High |
| **Preconditions** | Staged allocation in PROPOSED status exists for Student A. Tenant Admin logged in. MailHog inbox cleared. |
| **Test Data** | Student: john.doe@test.com |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin (tenantadmin@test.com / Admin@12345) | Admin dashboard loads |
| 2 | Navigate to Staged Allocations page | Staged allocations listed |
| 3 | Locate Student A's proposed allocation | Allocation found |
| 4 | Click "Reject" on the staged allocation | Rejection dialog appears |
| 5 | Enter reason and confirm rejection | Status changes to REJECTED; success message shown |
| 6 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 7 | Locate email sent to john.doe@test.com | Email present |
| 8 | Verify subject contains "Room Allocation Update" | Subject matches |
| 9 | Verify email body indicates allocation was not confirmed | Rejection details present |

| Field | Value |
|-------|-------|
| **Postconditions** | Staged allocation rejected; rejection email in MailHog |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-NTF-012: Student Moved to Different Room Email

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-012 |
| **Module** | Notifications |
| **Priority** | High |
| **Preconditions** | Student A has an active room allocation. A different room is available. Tenant Admin logged in. MailHog inbox cleared. |
| **Test Data** | Student: john.doe@test.com, Original Room: 101, New Room: 205 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin (tenantadmin@test.com / Admin@12345) | Admin dashboard loads |
| 2 | Navigate to Room Allocation management | Allocation page loads |
| 3 | Locate Student A's current allocation | Allocation found |
| 4 | Move student from Room 101 to Room 205 | Move dialog/form appears |
| 5 | Confirm the room change | Allocation updated; success message shown |
| 6 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 7 | Locate email sent to john.doe@test.com | Email present |
| 8 | Verify subject contains "Room Change Notification" | Subject matches |
| 9 | Verify email body contains old room (101) and new room (205) details | Both rooms referenced |

| Field | Value |
|-------|-------|
| **Postconditions** | Student moved to new room; move notification email in MailHog |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-NTF-013: Offer Declined — Requeue Email

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-013 |
| **Module** | Notifications |
| **Priority** | High |
| **Preconditions** | Student A has an active room offer (not at max decline limit). Student A logged in. MailHog inbox cleared. |
| **Test Data** | Student: john.doe@test.com, Max declines not reached |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A (john.doe@test.com / Student@123) | Student dashboard loads |
| 2 | Navigate to room offer / allocation section | Active offer displayed |
| 3 | Click "Decline Offer" | Decline confirmation dialog appears |
| 4 | Confirm decline | Offer declined; student returned to queue |
| 5 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 6 | Locate email sent to john.doe@test.com | Email present |
| 7 | Verify subject contains "Room Offer Declined - You Remain in the Queue" | Subject matches |
| 8 | Verify email body confirms student remains in allocation queue | Queue status confirmed |

| Field | Value |
|-------|-------|
| **Postconditions** | Offer declined; student re-queued; requeue email in MailHog |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-NTF-014: Offer Declined — Max Reached Email

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-014 |
| **Module** | Notifications |
| **Priority** | High |
| **Preconditions** | Student A has reached the maximum number of allowed offer declines minus one. Student A has an active room offer. MailHog inbox cleared. |
| **Test Data** | Student: john.doe@test.com, At max decline limit |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A (john.doe@test.com / Student@123) | Student dashboard loads |
| 2 | Navigate to room offer / allocation section | Active offer displayed |
| 3 | Click "Decline Offer" | Decline confirmation dialog appears (may warn this is final decline) |
| 4 | Confirm decline | Offer declined; application cancelled |
| 5 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 6 | Locate email sent to john.doe@test.com | Email present |
| 7 | Verify subject contains "Application Cancelled - Maximum Offer Declines Reached" | Subject matches |
| 8 | Verify email body explains application has been cancelled due to max declines | Cancellation reason stated |

| Field | Value |
|-------|-------|
| **Postconditions** | Application cancelled; max-declined email in MailHog |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-NTF-015: Offer Expired Email

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-015 |
| **Module** | Notifications |
| **Priority** | High |
| **Preconditions** | Student A has a room offer that has passed its expiry deadline without response. Scheduled job has run. MailHog inbox cleared. |
| **Test Data** | Student: john.doe@test.com, Offer expiry: past |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Ensure offer expiry deadline has passed (adjust test data or wait) | Offer deadline expired |
| 2 | Trigger or wait for the scheduled job that checks expired offers | Job processes expired offer |
| 3 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 4 | Locate email sent to john.doe@test.com | Email present |
| 5 | Verify subject contains "Room Offer Expired" | Subject matches |
| 6 | Verify email body explains the offer has expired due to no response | Expiry reason stated |

| Field | Value |
|-------|-------|
| **Postconditions** | Offer expired; expiry email in MailHog |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Requires scheduled job execution or manual trigger |

---

### TC-NTF-016: Offer Expiry Reminder Email (24h Before)

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-016 |
| **Module** | Notifications |
| **Priority** | High |
| **Preconditions** | Student A has a room offer expiring within 24 hours. Scheduled reminder job has run. MailHog inbox cleared. |
| **Test Data** | Student: john.doe@test.com, Offer expiry: within 24 hours |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Ensure offer expiry is within 24 hours from now | Offer within reminder window |
| 2 | Trigger or wait for the scheduled reminder job | Job processes upcoming expiry |
| 3 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 4 | Locate email sent to john.doe@test.com | Email present |
| 5 | Verify subject contains "Action Required: Room Offer Expiring Soon" | Subject matches |
| 6 | Verify email body contains the expiry deadline and urges action | Deadline and urgency present |

| Field | Value |
|-------|-------|
| **Postconditions** | Reminder email sent; offer still active |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Requires offer with expiry set to ~24 hours in future |

---

### TC-NTF-017: Check-In Reminder Email (24h Before)

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-017 |
| **Module** | Notifications |
| **Priority** | High |
| **Preconditions** | Student A has an accepted allocation with check-in deadline within 24 hours. Scheduled job has run. MailHog inbox cleared. |
| **Test Data** | Student: john.doe@test.com, Check-in deadline: within 24 hours |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Ensure check-in deadline is within 24 hours from now | Check-in within reminder window |
| 2 | Trigger or wait for the scheduled check-in reminder job | Job processes upcoming check-in |
| 3 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 4 | Locate email sent to john.doe@test.com | Email present |
| 5 | Verify subject contains "Reminder: Check-In Deadline Approaching" | Subject matches |
| 6 | Verify email body contains the check-in deadline date and location | Details present |

| Field | Value |
|-------|-------|
| **Postconditions** | Reminder email sent; allocation still active |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Requires allocation with check-in deadline ~24 hours in future |

---

### TC-NTF-018: Check-In Deadline Expired Email

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-018 |
| **Module** | Notifications |
| **Priority** | High |
| **Preconditions** | Student A has an accepted allocation whose check-in deadline has passed without check-in. Scheduled job has run. MailHog inbox cleared. |
| **Test Data** | Student: john.doe@test.com, Check-in deadline: past |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Ensure check-in deadline has passed | Deadline expired |
| 2 | Trigger or wait for the scheduled job that checks expired check-ins | Job processes expired check-in |
| 3 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 4 | Locate email sent to john.doe@test.com | Email present |
| 5 | Verify subject contains "Check-In Deadline Expired - Application Cancelled" | Subject matches |
| 6 | Verify email body explains the allocation and application have been cancelled | Cancellation details present |

| Field | Value |
|-------|-------|
| **Postconditions** | Application cancelled; check-in expired email in MailHog |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Requires scheduled job execution |

---

## In-App Notifications

---

### TC-NTF-019: In-App Notification on Application Approved

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-019 |
| **Module** | Notifications |
| **Priority** | Critical |
| **Preconditions** | Student A logged in and viewing dashboard. Student A has a SUBMITTED application. Tenant Admin logged in on a separate browser/tab. |
| **Test Data** | Student: john.doe@test.com, Admin: tenantadmin@test.com |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A (john.doe@test.com / Student@123) in Browser 1 | Student dashboard loads |
| 2 | Note current notification bell state (unread count) | Baseline count noted |
| 3 | In Browser 2, log in as Tenant Admin and approve Student A's application | Application approved |
| 4 | Return to Browser 1 (Student A's session) | Dashboard visible |
| 5 | Observe the notification bell icon | Unread count incremented by 1 |
| 6 | Click the notification bell | Notification panel opens |
| 7 | Verify notification with type APPLICATION_APPROVED is listed | Notification present with approval message |

| Field | Value |
|-------|-------|
| **Postconditions** | In-app notification visible; unread count updated |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Tests real-time SSE delivery |

---

### TC-NTF-020: In-App Notification on Room Allocated

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-020 |
| **Module** | Notifications |
| **Priority** | Critical |
| **Preconditions** | Student A logged in and viewing dashboard. Student A has an APPROVED application. Tenant Admin logged in on separate browser. |
| **Test Data** | Student: john.doe@test.com, Admin: tenantadmin@test.com |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A in Browser 1 | Student dashboard loads |
| 2 | Note current unread notification count | Baseline count noted |
| 3 | In Browser 2, log in as Tenant Admin and allocate a room to Student A | Room allocated |
| 4 | Return to Browser 1 | Dashboard visible |
| 5 | Observe the notification bell icon | Unread count incremented |
| 6 | Click the notification bell | Notification panel opens |
| 7 | Verify notification with type ROOM_ALLOCATION is listed | Notification present with allocation details |

| Field | Value |
|-------|-------|
| **Postconditions** | In-app ROOM_ALLOCATION notification visible |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-NTF-021: Unread Count Displayed on Notification Bell

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-021 |
| **Module** | Notifications |
| **Priority** | High |
| **Preconditions** | Student A has at least 3 unread notifications. Student A logged in. |
| **Test Data** | Student: john.doe@test.com |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A (john.doe@test.com / Student@123) | Student dashboard loads |
| 2 | Locate the notification bell icon in the navigation bar | Bell icon visible |
| 3 | Observe the badge/counter on the bell icon | Badge shows unread count (e.g., 3) |
| 4 | Verify the count matches the number of unread notifications | Count accurate |

| Field | Value |
|-------|-------|
| **Postconditions** | Unread count correctly displayed |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-NTF-022: Mark Single Notification as Read

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-022 |
| **Module** | Notifications |
| **Priority** | High |
| **Preconditions** | Student A has at least 2 unread notifications. Student A logged in. |
| **Test Data** | Student: john.doe@test.com |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A (john.doe@test.com / Student@123) | Student dashboard loads |
| 2 | Note the unread count on the notification bell (e.g., 3) | Baseline count noted |
| 3 | Click the notification bell to open the panel | Notification panel opens with unread notifications |
| 4 | Click on a single unread notification (or its "mark as read" action) | Notification marked as read; visual style changes (e.g., no longer bold) |
| 5 | Observe the unread count on the bell | Count decreased by 1 (e.g., 2) |

| Field | Value |
|-------|-------|
| **Postconditions** | One notification marked as read; unread count decremented |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-NTF-023: Mark All Notifications as Read

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-023 |
| **Module** | Notifications |
| **Priority** | High |
| **Preconditions** | Student A has multiple unread notifications. Student A logged in. |
| **Test Data** | Student: john.doe@test.com |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A (john.doe@test.com / Student@123) | Student dashboard loads |
| 2 | Verify unread count is > 0 on the notification bell | Count visible |
| 3 | Click the notification bell to open the panel | Notification panel opens |
| 4 | Click "Mark All as Read" button/link | All notifications marked as read |
| 5 | Observe the unread count on the bell | Count is 0; badge disappears or shows 0 |
| 6 | Verify all notifications in the panel appear as read (visual style) | All items show read state |

| Field | Value |
|-------|-------|
| **Postconditions** | All notifications marked as read; unread count = 0 |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-NTF-024: Delete Notification

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-024 |
| **Module** | Notifications |
| **Priority** | Medium |
| **Preconditions** | Student A has at least 1 notification. Student A logged in. |
| **Test Data** | Student: john.doe@test.com |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A (john.doe@test.com / Student@123) | Student dashboard loads |
| 2 | Click the notification bell to open the panel | Notification panel opens |
| 3 | Note the total number of notifications and identify one to delete | Notification identified |
| 4 | Click the delete/dismiss action on the chosen notification | Notification removed from the list |
| 5 | Verify the notification no longer appears in the panel | Notification absent |
| 6 | Verify notification count updated accordingly | Count correct |

| Field | Value |
|-------|-------|
| **Postconditions** | Notification permanently removed from list |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-NTF-025: Real-Time Notification via SSE

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-025 |
| **Module** | Notifications |
| **Priority** | Critical |
| **Preconditions** | Student A logged in with browser open (SSE connection active at /api/notifications/stream). Tenant Admin logged in on separate browser. |
| **Test Data** | Student: john.doe@test.com, Admin: tenantadmin@test.com |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A in Browser 1 | Student dashboard loads; SSE connection established |
| 2 | Open browser DevTools → Network tab and verify EventSource connection to /api/notifications/stream | SSE connection active (status: pending/streaming) |
| 3 | In Browser 2, log in as Tenant Admin | Admin dashboard loads |
| 4 | Perform an action that triggers a notification for Student A (e.g., approve a document) | Action completed |
| 5 | Return to Browser 1 without refreshing the page | Dashboard still open |
| 6 | Observe the notification bell | Unread count updates in real time without page refresh |
| 7 | Click the bell to verify the new notification appears | New notification listed at the top |

| Field | Value |
|-------|-------|
| **Postconditions** | Real-time notification delivered via SSE without page refresh |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Verify in DevTools that SSE events are received |

---

## Retry Logic

---

### TC-NTF-026: Notification with SENT Status

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-026 |
| **Module** | Notifications |
| **Priority** | High |
| **Preconditions** | An action that triggers an email notification has been performed (e.g., application approval). MailHog is running and accessible. |
| **Test Data** | Student: john.doe@test.com |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Trigger an email notification (e.g., approve Student A's application) | Notification triggered |
| 2 | Wait a few seconds for email processing | Processing completes |
| 3 | Verify the email arrives in MailHog | Email present in MailHog |
| 4 | Check notification record in the database or admin notification logs | Notification status is SENT |
| 5 | Verify retry count is 0 | No retries needed |

| Field | Value |
|-------|-------|
| **Postconditions** | Notification in SENT status with 0 retries |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | May require DB query: `SELECT status, retry_count FROM notification WHERE ...` |

---

### TC-NTF-027: Failed Notification Retries

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-027 |
| **Module** | Notifications |
| **Priority** | High |
| **Preconditions** | Mail server is temporarily unavailable (stop MailHog). An action triggering email notification is performed. |
| **Test Data** | Student: john.doe@test.com |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Stop MailHog service (docker-compose stop mailhog) | Mail server unavailable |
| 2 | Trigger an email notification (e.g., approve a document for Student A) | Notification triggered |
| 3 | Wait for initial send attempt to fail | Notification status changes to FAILED |
| 4 | Check notification record in database | Status: FAILED or RETRYING, retry_count: 1 |
| 5 | Wait for retry interval (exponential backoff) | System retries sending |
| 6 | Check notification record again | retry_count incremented (e.g., 2) |
| 7 | Restart MailHog (docker-compose start mailhog) | Mail server available again |
| 8 | Wait for next retry attempt | System retries sending |
| 9 | Check notification record | Status: SENT; email arrives in MailHog |

| Field | Value |
|-------|-------|
| **Postconditions** | Notification eventually SENT after retries; retry count > 0 |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Requires stopping/starting MailHog and monitoring DB records |

---

### TC-NTF-028: Permanently Failed After Max Retries

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-028 |
| **Module** | Notifications |
| **Priority** | High |
| **Preconditions** | Mail server is unavailable (MailHog stopped). Max retries = 3. |
| **Test Data** | Student: john.doe@test.com |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Stop MailHog service (docker-compose stop mailhog) | Mail server unavailable |
| 2 | Trigger an email notification | Notification triggered |
| 3 | Wait for initial send attempt to fail | Status: FAILED |
| 4 | Wait for all 3 retry attempts to complete (monitor retry intervals) | System retries 3 times with exponential backoff |
| 5 | Check notification record in database after all retries exhausted | Status: PERMANENTLY_FAILED, retry_count: 3 |
| 6 | Verify no further retry attempts are made | Status remains PERMANENTLY_FAILED |
| 7 | Restart MailHog (docker-compose start mailhog) | Mail server available |
| 8 | Verify the permanently failed notification is NOT retried | No new email appears for this notification |

| Field | Value |
|-------|-------|
| **Postconditions** | Notification in PERMANENTLY_FAILED status; no further retries |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Requires patience for retry intervals; check DB: `SELECT status, retry_count FROM notification` |

---

## Edge Cases

---

### TC-NTF-029: Empty Notification List

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-029 |
| **Module** | Notifications |
| **Priority** | Medium |
| **Preconditions** | Student account with no notifications (new account or all notifications deleted). |
| **Test Data** | New student account with no prior notifications |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as the student with no notifications | Student dashboard loads |
| 2 | Observe the notification bell | No badge/counter shown (or shows 0) |
| 3 | Click the notification bell | Notification panel opens |
| 4 | Verify the panel displays an empty state message | Message like "No notifications" or "You're all caught up" displayed |
| 5 | Verify no errors in the panel | No error messages or broken UI |

| Field | Value |
|-------|-------|
| **Postconditions** | Empty state displayed gracefully |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-NTF-030: Large Number of Notifications — Pagination/Scroll

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-030 |
| **Module** | Notifications |
| **Priority** | Medium |
| **Preconditions** | Student A has 50+ notifications (generated via multiple admin actions or DB seeding). |
| **Test Data** | Student: john.doe@test.com with 50+ notifications |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A (john.doe@test.com / Student@123) | Student dashboard loads |
| 2 | Click the notification bell | Notification panel opens |
| 3 | Verify initial set of notifications loads | First page/batch of notifications shown |
| 4 | Scroll down or click "Load More" / navigate to next page | Additional notifications load |
| 5 | Continue scrolling/paginating to the end | All notifications accessible |
| 6 | Verify no performance degradation or UI freezing | Panel remains responsive |

| Field | Value |
|-------|-------|
| **Postconditions** | All notifications accessible via pagination/scroll |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | May need to seed DB with notifications for testing |

---

### TC-NTF-031: Tenant Isolation — Notifications Not Visible Across Tenants

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-031 |
| **Module** | Notifications |
| **Priority** | Critical |
| **Preconditions** | Two tenants exist in the system. Student A belongs to Tenant 1. Student B belongs to Tenant 2. An action triggers a notification for Student A. |
| **Test Data** | Student A: john.doe@test.com (Tenant 1), Student B: different tenant |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Trigger a notification for Student A (Tenant 1) — e.g., approve their application | Notification created for Student A |
| 2 | Log in as Student A | Notification visible in bell panel |
| 3 | Log out and log in as Student B (Tenant 2) | Student B's dashboard loads |
| 4 | Click the notification bell | Student B's notification panel opens |
| 5 | Verify Student A's notification is NOT visible to Student B | Only Student B's own notifications shown |

| Field | Value |
|-------|-------|
| **Postconditions** | Tenant isolation confirmed; no cross-tenant notification leakage |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Requires multi-tenant test environment |

---

### TC-NTF-032: Email Contains Correct Student Name and Application Details

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-032 |
| **Module** | Notifications |
| **Priority** | High |
| **Preconditions** | Student A has a submitted application. Tenant Admin logged in. MailHog inbox cleared. |
| **Test Data** | Student: john.doe@test.com, Name: John Doe |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin and approve Student A's application | Approval email triggered |
| 2 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 3 | Open the approval email for john.doe@test.com | Email content displayed |
| 4 | Verify the email greeting includes "John Doe" | Correct student name present |
| 5 | Verify the email body references the application ID or reference number | Application identifier present |
| 6 | Verify the email contains correct accommodation preferences (if applicable) | Preferences accurate |

| Field | Value |
|-------|-------|
| **Postconditions** | Email content personalized with correct student and application data |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-NTF-033: Email Contains Correct Building and Room Details

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-033 |
| **Module** | Notifications |
| **Priority** | High |
| **Preconditions** | Student A allocated to a specific room. MailHog inbox cleared. |
| **Test Data** | Student: john.doe@test.com, Building: Test Building, Room: 101, Room Type: Single |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Allocate Student A to Room 101 in Test Building | Allocation email triggered |
| 2 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 3 | Open the allocation email for john.doe@test.com | Email content displayed |
| 4 | Verify email body contains building name "Test Building" | Building name correct |
| 5 | Verify email body contains room number "101" | Room number correct |
| 6 | Verify email body contains room type "Single" (if included in template) | Room type correct |
| 7 | Verify email body contains building address (if included in template) | Address correct |

| Field | Value |
|-------|-------|
| **Postconditions** | Email contains accurate building and room information |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-NTF-034: Email Links Navigate to Correct Pages

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-034 |
| **Module** | Notifications |
| **Priority** | High |
| **Preconditions** | An email notification with action links has been received (e.g., "View Application" link in approval email). Student A logged in. |
| **Test Data** | Student: john.doe@test.com |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open MailHog and locate an email for Student A that contains action links | Email found |
| 2 | Identify "View Application" or similar action link in the email body | Link present |
| 3 | Copy the link URL | URL captured |
| 4 | Open the link in a browser where Student A is logged in | Page loads |
| 5 | Verify the link navigates to the correct page (e.g., application details) | Correct page displayed |
| 6 | Verify the page shows the relevant data for Student A's application/allocation | Data matches |

| Field | Value |
|-------|-------|
| **Postconditions** | Email links functional and point to correct application pages |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-NTF-035: Multiple Rapid Actions — All Notifications Delivered

| Field | Value |
|-------|-------|
| **ID** | TC-NTF-035 |
| **Module** | Notifications |
| **Priority** | Medium |
| **Preconditions** | Student A has multiple pending documents and a submitted application. Tenant Admin logged in. MailHog inbox cleared. |
| **Test Data** | Student: john.doe@test.com, 3 pending documents |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin (tenantadmin@test.com / Admin@12345) | Admin dashboard loads |
| 2 | Rapidly approve 3 documents for Student A in quick succession | All 3 documents approved |
| 3 | Immediately approve Student A's application | Application approved |
| 4 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 5 | Count emails sent to john.doe@test.com | At least 4 emails present (3 document + 1 application) |
| 6 | Verify each email has the correct subject and content | All emails accurate and distinct |
| 7 | Log in as Student A and check notification bell | At least 4 in-app notifications present |

| Field | Value |
|-------|-------|
| **Postconditions** | All notifications from rapid actions delivered without loss |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Tests system under burst notification load |
