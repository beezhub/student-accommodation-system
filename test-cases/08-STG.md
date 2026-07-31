# Staged/Batch Allocation — UAT Test Cases

> **Module:** Staged/Batch Allocation
> **Route:** `/admin/allocations/staged` (`StagedAllocationComponent`)
> **Statuses:** PROPOSED → CONFIRMED / REJECTED
> **Last Updated:** 2026-05-17

---

## Test Data Reference

| Item | Details |
|------|---------|
| **Platform Admin** | platformadmin@test.com / Admin@12345 |
| **Student A** | john.doe@test.com, male, UCT |
| **Student B** | jane.smith@test.com, female, Stellenbosch |
| **Student C** | alex.jones@test.com, male, Wits |
| **Room A-101** | Capacity 2, ANY gender |
| **Room A-102** | Capacity 1, ANY gender |
| **Room B-201** | Capacity 2, FEMALE_ONLY |
| **Room C-301** | Capacity 3, MALE_ONLY |
| **MailHog** | http://localhost:8025 |

---

### TC-STG-001: Propose batch allocation with 1 student

| Field | Value |
|-------|-------|
| **ID** | TC-STG-001 |
| **Module** | Staged/Batch Allocation |
| **Priority** | Critical |
| **Preconditions** | Platform admin logged in. Student A has application in AWAITING_ALLOCATION status. At least one room with available capacity exists. |
| **Test Data** | Student A (john.doe@test.com), Room A-101 (cap 2, ANY) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/allocations/staged` | Staged allocation page loads |
| 2 | Click "Propose Batch Allocation" button | Batch proposal dialog/form opens |
| 3 | Select 1 student (Student A) or set batch size to 1 | Student A selected for proposal |
| 4 | Click "Propose" / "Submit" | Success toast: batch proposed |
| 5 | Verify staged allocation list updates | One new entry appears with status PROPOSED, showing Student A, assigned room, score, and a batchId |
| 6 | Verify the staged allocation record has all fields | Student, room, application, score, warnings (if any), and batchId are populated |

| Field | Value |
|-------|-------|
| **Postconditions** | One staged allocation exists with status PROPOSED. No actual RoomAllocation created yet. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-STG-002: Propose batch with 5 students

| Field | Value |
|-------|-------|
| **ID** | TC-STG-002 |
| **Module** | Staged/Batch Allocation |
| **Priority** | Critical |
| **Preconditions** | Platform admin logged in. 5 students have applications in AWAITING_ALLOCATION status. Sufficient rooms with available capacity. |
| **Test Data** | 5 students including Student A, Student B, Student C plus 2 additional students with AWAITING_ALLOCATION applications |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/allocations/staged` | Staged allocation page loads |
| 2 | Click "Propose Batch Allocation" | Batch proposal dialog opens |
| 3 | Set batch size to 5 or select 5 students | 5 students selected |
| 4 | Click "Propose" | Success toast: batch proposed with 5 allocations |
| 5 | Verify staged allocation list | 5 new entries appear, all with status PROPOSED |
| 6 | Verify all entries share the same batchId | All 5 entries have identical batchId value |
| 7 | Verify each entry has a score, assigned room, and student | All fields populated for each entry |

| Field | Value |
|-------|-------|
| **Postconditions** | 5 staged allocations exist with PROPOSED status under one batchId. No actual RoomAllocations created. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-STG-003: Propose batch with 50 students (max boundary)

| Field | Value |
|-------|-------|
| **ID** | TC-STG-003 |
| **Module** | Staged/Batch Allocation |
| **Priority** | High |
| **Preconditions** | Platform admin logged in. 50 students have AWAITING_ALLOCATION applications. Sufficient room capacity across all buildings. |
| **Test Data** | 50 students with AWAITING_ALLOCATION applications |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/allocations/staged` | Page loads |
| 2 | Click "Propose Batch Allocation" | Dialog opens |
| 3 | Set batch size to 50 | 50 students selected |
| 4 | Click "Propose" | Success toast: batch proposed with 50 allocations |
| 5 | Verify staged allocation list | 50 new entries with PROPOSED status |
| 6 | Verify all share one batchId | All 50 entries have the same batchId |
| 7 | Verify page performance | List loads within acceptable time (<5s), pagination works if applicable |

| Field | Value |
|-------|-------|
| **Postconditions** | 50 staged allocations created with PROPOSED status. System remains responsive. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-STG-004: Propose batch with >50 students (exceeds max)

| Field | Value |
|-------|-------|
| **ID** | TC-STG-004 |
| **Module** | Staged/Batch Allocation |
| **Priority** | High |
| **Preconditions** | Platform admin logged in. 51+ students with AWAITING_ALLOCATION applications. |
| **Test Data** | 51 students with AWAITING_ALLOCATION applications |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/allocations/staged` | Page loads |
| 2 | Click "Propose Batch Allocation" | Dialog opens |
| 3 | Attempt to set batch size to 51 | Input validation prevents entry >50, or value is accepted for server-side validation |
| 4 | Click "Propose" (if input accepted) | Error message: "Batch size cannot exceed 50" or equivalent |
| 5 | Verify no staged allocations created | Staged allocation list unchanged |

| Field | Value |
|-------|-------|
| **Postconditions** | No new staged allocations created. Error message displayed to admin. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-STG-005: Propose batch with 0 students

| Field | Value |
|-------|-------|
| **ID** | TC-STG-005 |
| **Module** | Staged/Batch Allocation |
| **Priority** | Medium |
| **Preconditions** | Platform admin logged in. No students with AWAITING_ALLOCATION applications, or batch size set to 0. |
| **Test Data** | Batch size = 0 or no eligible students |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/allocations/staged` | Page loads |
| 2 | Click "Propose Batch Allocation" | Dialog opens |
| 3 | Set batch size to 0 or attempt to propose with no eligible students | Input validation prevents 0, or server rejects |
| 4 | Click "Propose" (if allowed) | Error message: "No students available for allocation" or "Batch size must be at least 1" |
| 5 | Verify no staged allocations created | List unchanged |

| Field | Value |
|-------|-------|
| **Postconditions** | No staged allocations created. Appropriate error displayed. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-STG-006: View staged allocations list with filters

| Field | Value |
|-------|-------|
| **ID** | TC-STG-006 |
| **Module** | Staged/Batch Allocation |
| **Priority** | Critical |
| **Preconditions** | Platform admin logged in. Multiple staged allocations exist across different batches with mixed statuses (PROPOSED, CONFIRMED, REJECTED). |
| **Test Data** | At least 10 staged allocations across 2+ batches |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/allocations/staged` | Staged allocation list loads showing all entries |
| 2 | Verify table columns displayed | Columns include: Student Name, Room, Building, Score, Status, Batch ID, Warnings, Actions |
| 3 | Verify pagination works (if >10 entries) | Page navigation controls function correctly |
| 4 | Verify sorting by clicking column headers | Table sorts by selected column (score, status, student name) |
| 5 | Verify filter controls are visible | Filter dropdowns/inputs for status, batch ID, and other criteria are present |

| Field | Value |
|-------|-------|
| **Postconditions** | Admin can view full staged allocation list with working filters and sorting. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-STG-007: Filter staged allocations by status (PROPOSED)

| Field | Value |
|-------|-------|
| **ID** | TC-STG-007 |
| **Module** | Staged/Batch Allocation |
| **Priority** | High |
| **Preconditions** | Platform admin logged in. Staged allocations exist with mixed statuses: some PROPOSED, some CONFIRMED, some REJECTED. |
| **Test Data** | At least 3 PROPOSED, 2 CONFIRMED, 1 REJECTED staged allocations |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/allocations/staged` | Full list loads |
| 2 | Select "PROPOSED" from status filter dropdown | List filters to show only PROPOSED entries |
| 3 | Verify all displayed entries have PROPOSED status | No CONFIRMED or REJECTED entries visible |
| 4 | Verify result count matches expected number of PROPOSED entries | Count is accurate |
| 5 | Change filter to "CONFIRMED" | List updates to show only CONFIRMED entries |
| 6 | Clear filter / select "All" | Full list reappears with all statuses |

| Field | Value |
|-------|-------|
| **Postconditions** | Filters correctly isolate staged allocations by status. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-STG-008: Filter staged allocations by batch ID

| Field | Value |
|-------|-------|
| **ID** | TC-STG-008 |
| **Module** | Staged/Batch Allocation |
| **Priority** | High |
| **Preconditions** | Platform admin logged in. Staged allocations exist across at least 2 different batches. |
| **Test Data** | Batch 1 with 3 entries, Batch 2 with 5 entries |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/allocations/staged` | Full list loads with entries from multiple batches |
| 2 | Note the batchId of the first entry | BatchId identified (e.g., "batch-abc-123") |
| 3 | Enter or select that batchId in the batch filter | List filters to show only entries from that batch |
| 4 | Verify all displayed entries have the selected batchId | All entries match the selected batch |
| 5 | Verify count matches expected entries for that batch | Count is accurate |
| 6 | Clear the batch filter | Full list reappears |

| Field | Value |
|-------|-------|
| **Postconditions** | Batch filter correctly isolates entries by batch ID. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-STG-009: Confirm individual staged allocation

| Field | Value |
|-------|-------|
| **ID** | TC-STG-009 |
| **Module** | Staged/Batch Allocation |
| **Priority** | Critical |
| **Preconditions** | Platform admin logged in. At least one staged allocation with PROPOSED status exists. The assigned room still has available capacity. |
| **Test Data** | Student A (john.doe@test.com) with PROPOSED staged allocation to Room A-101 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/allocations/staged` | List loads with PROPOSED entries |
| 2 | Locate Student A's PROPOSED staged allocation | Entry visible with PROPOSED status |
| 3 | Click "Confirm" action button on Student A's entry | Confirmation dialog appears (if applicable) |
| 4 | Confirm the action | Success toast: "Allocation confirmed" |
| 5 | Verify staged allocation status changes to CONFIRMED | Entry now shows CONFIRMED status |
| 6 | Navigate to room allocations list (`/admin/allocations`) | Allocations page loads |
| 7 | Verify a new RoomAllocation exists for Student A in Room A-101 | RoomAllocation record present with status OFFERED |
| 8 | Verify the RoomAllocation status is OFFERED | Status is OFFERED (not ACCEPTED or CONFIRMED) |

| Field | Value |
|-------|-------|
| **Postconditions** | Staged allocation is CONFIRMED. Actual RoomAllocation created with OFFERED status. Room occupancy count updated. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-STG-010: Verify email sent to student when staged allocation confirmed

| Field | Value |
|-------|-------|
| **ID** | TC-STG-010 |
| **Module** | Staged/Batch Allocation |
| **Priority** | Critical |
| **Preconditions** | TC-STG-009 completed. Student A's staged allocation just confirmed. MailHog running. |
| **Test Data** | Student A (john.doe@test.com), Room A-101 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open MailHog UI at http://localhost:8025 | MailHog inbox loads |
| 2 | Search for emails to john.doe@test.com | Email list appears |
| 3 | Find the most recent email | Email with subject containing "Room Allocation Confirmed" or similar |
| 4 | Open the email | Email body loads |
| 5 | Verify email contains room details | Room number (A-101), building name, and allocation details present |
| 6 | Verify email contains next steps / offer acceptance instructions | Instructions for accepting/declining the offer included |
| 7 | Verify email sender and formatting | Sent from configured support email, properly formatted HTML |

| Field | Value |
|-------|-------|
| **Postconditions** | Student received confirmation email with correct room details and instructions. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-STG-011: Reject individual staged allocation

| Field | Value |
|-------|-------|
| **ID** | TC-STG-011 |
| **Module** | Staged/Batch Allocation |
| **Priority** | Critical |
| **Preconditions** | Platform admin logged in. At least one staged allocation with PROPOSED status exists. |
| **Test Data** | Student B (jane.smith@test.com) with PROPOSED staged allocation to Room B-201 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/allocations/staged` | List loads |
| 2 | Locate Student B's PROPOSED staged allocation | Entry visible |
| 3 | Click "Reject" action button on Student B's entry | Rejection confirmation dialog appears |
| 4 | Optionally enter rejection reason | Reason field accepts input |
| 5 | Confirm the rejection | Success toast: "Allocation rejected" |
| 6 | Verify staged allocation status changes to REJECTED | Entry now shows REJECTED status |
| 7 | Navigate to room allocations list | Allocations page loads |
| 8 | Verify NO RoomAllocation was created for Student B | No allocation record exists for Student B |
| 9 | Verify Room B-201 occupancy is unchanged | Room capacity not affected |

| Field | Value |
|-------|-------|
| **Postconditions** | Staged allocation is REJECTED. No RoomAllocation created. Room availability unchanged. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-STG-012: Verify email sent when staged allocation rejected

| Field | Value |
|-------|-------|
| **ID** | TC-STG-012 |
| **Module** | Staged/Batch Allocation |
| **Priority** | High |
| **Preconditions** | TC-STG-011 completed. Student B's staged allocation just rejected. MailHog running. |
| **Test Data** | Student B (jane.smith@test.com) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open MailHog UI at http://localhost:8025 | MailHog inbox loads |
| 2 | Search for emails to jane.smith@test.com | Email list appears |
| 3 | Find the most recent email | Email with subject containing "Room Allocation Update" or similar |
| 4 | Open the email | Email body loads |
| 5 | Verify email indicates the allocation was not successful | Message states allocation was not confirmed |
| 6 | Verify email contains next steps (e.g., waitlist info, reapply) | Guidance for student's next options included |

| Field | Value |
|-------|-------|
| **Postconditions** | Student received rejection notification email. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-STG-013: Confirm entire batch

| Field | Value |
|-------|-------|
| **ID** | TC-STG-013 |
| **Module** | Staged/Batch Allocation |
| **Priority** | Critical |
| **Preconditions** | Platform admin logged in. A batch exists with 5 staged allocations, all in PROPOSED status. All assigned rooms have available capacity. |
| **Test Data** | Batch with 5 PROPOSED entries (Students A, B, C + 2 others) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/allocations/staged` | List loads |
| 2 | Filter by the target batch ID | 5 PROPOSED entries displayed |
| 3 | Click "Confirm Entire Batch" or "Confirm All" button | Confirmation dialog appears: "Confirm all 5 proposed allocations?" |
| 4 | Confirm the action | Progress indicator shown; success toast: "Batch confirmed — 5 allocations created" |
| 5 | Verify all 5 entries now show CONFIRMED status | All entries updated to CONFIRMED |
| 6 | Navigate to room allocations list | 5 new RoomAllocation records exist |
| 7 | Verify all 5 RoomAllocations have OFFERED status | Each allocation shows OFFERED status |
| 8 | Check MailHog for 5 confirmation emails | 5 emails sent to respective students |

| Field | Value |
|-------|-------|
| **Postconditions** | All batch entries CONFIRMED. 5 RoomAllocations created with OFFERED status. 5 notification emails sent. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-STG-014: Confirm batch where some already confirmed

| Field | Value |
|-------|-------|
| **ID** | TC-STG-014 |
| **Module** | Staged/Batch Allocation |
| **Priority** | High |
| **Preconditions** | Platform admin logged in. A batch exists with 5 entries: 2 CONFIRMED, 1 REJECTED, 2 PROPOSED. |
| **Test Data** | Batch with mixed statuses: 2 CONFIRMED, 1 REJECTED, 2 PROPOSED |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/allocations/staged` | List loads |
| 2 | Filter by the target batch ID | 5 entries displayed with mixed statuses |
| 3 | Click "Confirm Entire Batch" | Confirmation dialog appears: "Confirm 2 remaining proposed allocations?" |
| 4 | Confirm the action | Success toast: "2 allocations confirmed" |
| 5 | Verify the 2 previously PROPOSED entries are now CONFIRMED | Status changed to CONFIRMED |
| 6 | Verify the 2 previously CONFIRMED entries remain CONFIRMED | No change to already-confirmed entries |
| 7 | Verify the 1 REJECTED entry remains REJECTED | No change to rejected entry |
| 8 | Verify only 2 new RoomAllocations created | 2 new allocations (not duplicates of previously confirmed) |

| Field | Value |
|-------|-------|
| **Postconditions** | Only PROPOSED entries were confirmed. Previously CONFIRMED and REJECTED entries unaffected. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-STG-015: Verify allocation scores displayed for each staged allocation

| Field | Value |
|-------|-------|
| **ID** | TC-STG-015 |
| **Module** | Staged/Batch Allocation |
| **Priority** | High |
| **Preconditions** | Platform admin logged in. A batch has been proposed with at least 3 students. |
| **Test Data** | 3 staged allocations from a single batch |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/allocations/staged` | List loads |
| 2 | Locate the batch entries | Entries visible in the list |
| 3 | Verify each entry displays a numeric score | Score column shows a number for each entry |
| 4 | Verify scores differ between students (assuming different profiles) | Scores are not all identical; reflect different student attributes |
| 5 | Click on a staged allocation to view details (if detail view exists) | Detail view shows score breakdown: preference match, desirability, accessibility, FCFS, NSFAS priority, university preference |
| 6 | Verify entries are ordered by score (highest first) | List is sorted descending by score |

| Field | Value |
|-------|-------|
| **Postconditions** | Scores are visible and reflect the allocation scoring algorithm. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-STG-016: Verify warnings displayed (e.g., gender mismatch, capacity issues)

| Field | Value |
|-------|-------|
| **ID** | TC-STG-016 |
| **Module** | Staged/Batch Allocation |
| **Priority** | High |
| **Preconditions** | Platform admin logged in. A batch proposed where at least one allocation has warnings (e.g., partial capacity, preference mismatch). |
| **Test Data** | Staged allocation with known warning conditions |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/allocations/staged` | List loads |
| 2 | Locate an entry with warnings | Warning icon or indicator visible on the entry |
| 3 | Hover over or click the warning indicator | Warning details displayed (e.g., "Room near capacity", "Preference not fully matched") |
| 4 | Verify warning text is descriptive and actionable | Admin can understand the issue from the warning message |
| 5 | Verify entries without warnings show no warning indicator | Clean entries have no warning icons |

| Field | Value |
|-------|-------|
| **Postconditions** | Warnings are clearly displayed for relevant entries. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-STG-017: Staged allocation with room at capacity — warning shown

| Field | Value |
|-------|-------|
| **ID** | TC-STG-017 |
| **Module** | Staged/Batch Allocation |
| **Priority** | High |
| **Preconditions** | Platform admin logged in. Room A-102 (cap 1) already has 1 occupant (full). A batch proposal attempts to allocate another student to A-102. |
| **Test Data** | Room A-102 (capacity 1, currently full), Student C proposed for A-102 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Propose a batch that includes allocating Student C to Room A-102 | Batch proposed |
| 2 | Navigate to `/admin/allocations/staged` | List loads |
| 3 | Locate Student C's entry | Entry visible |
| 4 | Verify a capacity warning is displayed | Warning: "Room A-102 is at full capacity" or similar |
| 5 | Attempt to confirm this allocation | System either blocks confirmation with error or shows a strong warning requiring override |

| Field | Value |
|-------|-------|
| **Postconditions** | Admin is warned about capacity issue before confirming. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-STG-018: Staged allocation violating gender restriction — warning shown

| Field | Value |
|-------|-------|
| **ID** | TC-STG-018 |
| **Module** | Staged/Batch Allocation |
| **Priority** | High |
| **Preconditions** | Platform admin logged in. Room B-201 is FEMALE_ONLY. A batch proposal attempts to allocate Student A (male) to Room B-201. |
| **Test Data** | Student A (john.doe@test.com, male), Room B-201 (FEMALE_ONLY) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Propose a batch that includes allocating Student A to Room B-201 | Batch proposed |
| 2 | Navigate to `/admin/allocations/staged` | List loads |
| 3 | Locate Student A's entry for Room B-201 | Entry visible |
| 4 | Verify a gender mismatch warning is displayed | Warning: "Gender mismatch — Student is male, room is FEMALE_ONLY" or similar |
| 5 | Attempt to confirm this allocation | System either blocks confirmation or requires explicit override acknowledgment |

| Field | Value |
|-------|-------|
| **Postconditions** | Admin is warned about gender restriction violation. Allocation blocked or requires override. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-STG-019: Propose allocation for student already allocated

| Field | Value |
|-------|-------|
| **ID** | TC-STG-019 |
| **Module** | Staged/Batch Allocation |
| **Priority** | High |
| **Preconditions** | Platform admin logged in. Student A already has an active RoomAllocation (OFFERED or ACCEPTED). |
| **Test Data** | Student A (john.doe@test.com) with existing allocation to Room A-101 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/allocations/staged` | Page loads |
| 2 | Attempt to propose a batch that includes Student A | Proposal process initiated |
| 3 | Verify system handles the duplicate | Error message: "Student already has an active allocation" or warning indicating duplicate |
| 4 | If warning, verify Student A is excluded from the batch or flagged | Student A either not included in proposal or flagged with warning |
| 5 | Verify other students in the batch are processed normally | Remaining students have valid PROPOSED entries |

| Field | Value |
|-------|-------|
| **Postconditions** | Student with existing allocation is not double-allocated. Appropriate error or warning shown. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-STG-020: Confirm staged allocation where room became full since proposal

| Field | Value |
|-------|-------|
| **ID** | TC-STG-020 |
| **Module** | Staged/Batch Allocation |
| **Priority** | Critical |
| **Preconditions** | Platform admin logged in. A PROPOSED staged allocation exists for Student C to Room A-102 (cap 1). Between proposal and confirmation, another student was manually allocated to A-102, filling it. |
| **Test Data** | Student C with PROPOSED allocation to Room A-102 (now full) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/allocations/staged` | List loads |
| 2 | Locate Student C's PROPOSED entry for Room A-102 | Entry visible |
| 3 | Click "Confirm" on Student C's entry | System checks current room availability |
| 4 | Verify error is displayed | Error: "Room A-102 no longer has available capacity" or similar |
| 5 | Verify staged allocation remains PROPOSED (not erroneously confirmed) | Status unchanged or set to an error state |
| 6 | Verify no RoomAllocation was created | No allocation record for Student C |

| Field | Value |
|-------|-------|
| **Postconditions** | Confirmation blocked due to capacity change. No over-allocation occurred. Admin informed of the issue. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-STG-021: Platform admin can see all staged allocations

| Field | Value |
|-------|-------|
| **ID** | TC-STG-021 |
| **Module** | Staged/Batch Allocation |
| **Priority** | Critical |
| **Preconditions** | Platform admin logged in. Staged allocations exist across multiple tenants. |
| **Test Data** | Platform admin (platformadmin@test.com), staged allocations from Tenant 1 and Tenant 2 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as platform admin (platformadmin@test.com / Admin@12345) | Dashboard loads |
| 2 | Navigate to `/admin/allocations/staged` | Staged allocation list loads |
| 3 | Verify entries from Tenant 1 are visible | Tenant 1 allocations present |
| 4 | Verify entries from Tenant 2 are visible | Tenant 2 allocations present |
| 5 | Verify total count includes all tenants | All staged allocations across tenants displayed |

| Field | Value |
|-------|-------|
| **Postconditions** | Platform admin has cross-tenant visibility of all staged allocations. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-STG-022: Tenant admin sees only their tenant's staged allocations

| Field | Value |
|-------|-------|
| **ID** | TC-STG-022 |
| **Module** | Staged/Batch Allocation |
| **Priority** | Critical |
| **Preconditions** | Tenant admin for Tenant 1 logged in. Staged allocations exist for both Tenant 1 and Tenant 2. |
| **Test Data** | Tenant 1 admin credentials, staged allocations from Tenant 1 (3 entries) and Tenant 2 (2 entries) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant 1 admin | Dashboard loads |
| 2 | Navigate to `/admin/allocations/staged` | Staged allocation list loads |
| 3 | Verify only Tenant 1's staged allocations are visible | Only 3 Tenant 1 entries displayed |
| 4 | Verify no Tenant 2 entries are shown | Tenant 2 allocations NOT visible |
| 5 | Verify count matches Tenant 1's entries only | Count = 3 |

| Field | Value |
|-------|-------|
| **Postconditions** | Tenant admin sees only their own tenant's data. Multi-tenancy isolation confirmed. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-STG-023: Student cannot access /admin/allocations/staged

| Field | Value |
|-------|-------|
| **ID** | TC-STG-023 |
| **Module** | Staged/Batch Allocation |
| **Priority** | Critical |
| **Preconditions** | Student A logged in with student credentials. |
| **Test Data** | Student A (john.doe@test.com) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A (john.doe@test.com) | Student dashboard loads |
| 2 | Manually navigate to `/admin/allocations/staged` in the browser address bar | Access denied |
| 3 | Verify redirect or error | User is redirected to student dashboard or shown "Access Denied" / 403 Forbidden page |
| 4 | Verify no staged allocation data is visible | No admin data exposed |

| Field | Value |
|-------|-------|
| **Postconditions** | Student role cannot access admin staged allocation functionality. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-STG-024: Verify allocation proposal email sent

| Field | Value |
|-------|-------|
| **ID** | TC-STG-024 |
| **Module** | Staged/Batch Allocation |
| **Priority** | Medium |
| **Preconditions** | Platform admin logged in. MailHog running. A batch proposal is about to be created. |
| **Test Data** | Student A (john.doe@test.com), MailHog at http://localhost:8025 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Clear MailHog inbox or note current email count | Baseline established |
| 2 | Navigate to `/admin/allocations/staged` | Page loads |
| 3 | Propose a batch allocation including Student A | Batch proposed successfully |
| 4 | Open MailHog UI at http://localhost:8025 | Inbox loads |
| 5 | Search for email to john.doe@test.com | New email found |
| 6 | Verify subject contains "Room Allocation Proposal" or similar | Subject line matches expected pattern |
| 7 | Verify email body contains proposal details | Room details, score information, and any instructions present |

| Field | Value |
|-------|-------|
| **Postconditions** | Proposal notification email sent to student upon batch creation. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Note: This email may or may not be sent depending on system configuration. Verify expected behavior with the team. |

---

### TC-STG-025: Propose batch — verify FCFS scoring orders earlier applications higher

| Field | Value |
|-------|-------|
| **ID** | TC-STG-025 |
| **Module** | Staged/Batch Allocation |
| **Priority** | High |
| **Preconditions** | Platform admin logged in. 3 students with AWAITING_ALLOCATION applications submitted at different times. Allocation rules include FCFS (first come first served) as a scoring factor. All other scoring factors are equal. |
| **Test Data** | Student A (submitted Jan 10), Student B (submitted Jan 15), Student C (submitted Jan 20) — all with identical profiles except submission date |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/allocations/staged` | Page loads |
| 2 | Propose a batch allocation with all 3 students | Batch proposed |
| 3 | View the staged allocations for this batch | 3 entries displayed |
| 4 | Verify Student A has the highest score | Student A's score > Student B's score > Student C's score |
| 5 | Verify the order reflects submission time | Earlier applications ranked higher |
| 6 | Verify Student A is assigned the most desirable room (if applicable) | Best room goes to highest-scoring student |

| Field | Value |
|-------|-------|
| **Postconditions** | FCFS scoring correctly prioritizes earlier applications. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## Summary

| ID | Title | Priority | Status |
|----|-------|----------|--------|
| TC-STG-001 | Propose batch with 1 student | Critical | ☐ |
| TC-STG-002 | Propose batch with 5 students | Critical | ☐ |
| TC-STG-003 | Propose batch with 50 students (max) | High | ☐ |
| TC-STG-004 | Propose batch >50 students (error) | High | ☐ |
| TC-STG-005 | Propose batch with 0 students (error) | Medium | ☐ |
| TC-STG-006 | View staged allocations with filters | Critical | ☐ |
| TC-STG-007 | Filter by status (PROPOSED) | High | ☐ |
| TC-STG-008 | Filter by batch ID | High | ☐ |
| TC-STG-009 | Confirm individual allocation | Critical | ☐ |
| TC-STG-010 | Email on confirm | Critical | ☐ |
| TC-STG-011 | Reject individual allocation | Critical | ☐ |
| TC-STG-012 | Email on reject | High | ☐ |
| TC-STG-013 | Confirm entire batch | Critical | ☐ |
| TC-STG-014 | Confirm batch with mixed statuses | High | ☐ |
| TC-STG-015 | Allocation scores displayed | High | ☐ |
| TC-STG-016 | Warnings displayed | High | ☐ |
| TC-STG-017 | Room at capacity warning | High | ☐ |
| TC-STG-018 | Gender restriction warning | High | ☐ |
| TC-STG-019 | Already allocated student | High | ☐ |
| TC-STG-020 | Room full since proposal | Critical | ☐ |
| TC-STG-021 | Platform admin cross-tenant view | Critical | ☐ |
| TC-STG-022 | Tenant admin scoped view | Critical | ☐ |
| TC-STG-023 | Student access denied | Critical | ☐ |
| TC-STG-024 | Proposal email sent | Medium | ☐ |
| TC-STG-025 | FCFS scoring order | High | ☐ |
