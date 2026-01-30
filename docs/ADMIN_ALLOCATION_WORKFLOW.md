# Admin Allocation Workflow Guide

This guide provides step-by-step instructions for administrators on reviewing applications, handling documents, and performing room allocations.

## Table of Contents

1. [Application Review Process](#application-review-process)
2. [Moving Application to Allocation Phase](#moving-application-to-allocation-phase)
3. [Manual Allocation](#manual-allocation)
4. [Batch Allocations](#batch-allocations)
5. [Managing Waiting List](#managing-waiting-list)
6. [Moving Students Between Rooms](#moving-students-between-rooms)
7. [Compliance Considerations](#compliance-considerations)

---

## Application Review Process

### Accessing Application Review

1. Log in to the admin dashboard
2. Click **Application Review** in the Quick Access section
3. Or navigate directly to `/admin/applications`

### Filtering Applications

Use the filter panel to find specific applications:

| Filter | Description |
|--------|-------------|
| Application Code | Unique application identifier |
| Student Name | Search by student name |
| Application Status | PENDING, APPROVED, REJECTED, AWAITING_ALLOCATION, ALLOCATED |
| Allocation Status | PENDING, ALLOCATED, REJECTED |
| Preferred Building | Filter by student's building preference |
| Date Range | Filter by application submission date |

### Understanding the Application List

The application table displays:

| Column | Description |
|--------|-------------|
| Application Code | Unique identifier for the application |
| Student Name/Email | Student information with avatar |
| Preferred Building | Student's building preference (or "Any") |
| Document Status | Approved count / Total required documents |
| Allocation Status | Current allocation state |
| Application Status | Current application state |
| Date | Submission date |

### Opening Application Detail Dialog

1. Find the application in the list
2. Click the **View Details** button (rate_review icon)
3. The Application Detail Dialog opens with three tabs

### Reviewing Student Profile (Tab 1)

The Student Profile tab displays:

- Full name and contact information
- Email address
- Student number
- Institution and course
- Year of study
- Gender
- Date of birth
- Registration date
- Special requirements

### Reviewing Uploaded Documents (Tab 2)

The Documents & Checklist tab shows:

**Document Checklist:**
- List of required documents with status indicators
- Progress bar showing required documents approved
- Status badges: APPROVED (green), PENDING (yellow), REJECTED (red), NOT_UPLOADED (gray)

**Document Actions:**
- **Preview**: View document inline
- **Download**: Download for offline review
- **Update Status**: Change document status via dropdown

### Leaving Comments on Documents

To add a comment on a document (especially for rejections):

1. Click on the document in the checklist
2. Scroll to the **Comments** section
3. Enter your comment in the text area
4. Click **Add Comment**

Comments are visible to the student and provide transparency for document rejections.

### Approving/Rejecting Documents

To change a document's status:

1. Locate the document in the checklist
2. Click the status dropdown
3. Select new status: APPROVED, PENDING, or REJECTED
4. For rejections, add a comment explaining the reason

**Important**: Changes are not saved until you click **Save Progress**.

### Saving Progress Mid-Review

The **Save Progress** button:

- Saves all document status changes
- Does NOT change the application status
- Allows you to continue review later
- Shows confirmation on success

Use this when:
- You need to pause a review
- Waiting for additional information
- Want to save partial progress

### Reviewing Preferences (Tab 3)

The Preferences tab displays:

- Preferred accommodation type (Single, Double, Studio, En-suite)
- Preferred room type (Standard, Premium, Accessible, Quiet Floor)
- Preferred move-in date
- Duration in months
- Preferred building
- Special requirements/accessibility needs

---

## Moving Application to Allocation Phase

### Prerequisites

Before moving an application to allocation:

1. Application status must be APPROVED
2. **All required documents** must be APPROVED
3. No unsaved changes pending

### Checking Required Documents Checklist

In the Documents & Checklist tab:

1. Review the progress bar at the top
2. Ensure all required documents show green checkmarks
3. Optional documents can remain pending/not uploaded

### Understanding AWAITING_ALLOCATION Status

When an application moves to AWAITING_ALLOCATION:

- Student is eligible for room allocation
- Application appears in allocation queues
- Batch allocation can include this student
- Manual allocation becomes available

### Triggering Move to Allocation Phase

**Option 1: From Application Detail Dialog**

1. Open the application detail dialog
2. Ensure all required documents are approved
3. Click **Move to Allocation** button
4. Confirm the action

**Option 2: Approve Application Action**

1. In the application list, use the quick action
2. Click **Approve Application** (if not yet approved)
3. This approves AND moves to allocation in one step

---

## Manual Allocation

### When to Use Manual Allocation

Manual allocation is the **standard method** for all room assignments. The system shows only eligible rooms based on:

- Student's institution (building must serve it)
- Gender restrictions (room must match or be ANY)
- Accessibility requirements (if student needs accessible room)
- Year of study restrictions (student's year must be within room's range)

### Opening the Allocation Dialog

1. Find an application with status AWAITING_ALLOCATION
2. Click **Allocate Room** button (assignment_turned_in icon)
3. The Allocation Dialog opens showing eligible rooms

### Filtering Available Rooms

In the allocation dialog:

1. **Building Filter**: Select a specific building or "All"
2. **Room Number**: Search by room number
3. Click **Search** to filter results

The room list shows only **eligible rooms** for this student:

| Column | Description |
|--------|-------------|
| Room Number | Room identifier |
| Building | Building name |
| Occupancy | Current / Capacity |
| Available | Number of available spots |
| Gender | Gender restriction (if any) |
| Accessible | Accessibility status |
| Year Range | Min-Max year of study restriction |

**Note**: Rooms that don't match the student's profile (institution, gender, accessibility, year) are automatically filtered out.

### Selecting Appropriate Room

Consider these factors when selecting:

| Factor | What to Check |
|--------|---------------|
| Capacity | Room has available spots |
| Gender | Student's gender matches restriction |
| Accessibility | Meets student's special requirements |
| Preference | Matches student's preferred building |
| Room Type | Aligns with student's room type preference |

### Confirming Allocation

1. Select the desired room
2. Review the selection summary
3. Click **Confirm Allocation**
4. Student receives notification

### Compliance Documentation

For manual allocations that bypass constraints:

1. Check **Bypass Constraints** if needed
2. Enter mandatory **reason** (min 10 characters)
3. Specify which constraints are being bypassed
4. This creates an audit trail

---

---

## Batch Allocations

### Overview

Batch allocation allows processing multiple students simultaneously, useful for:
- Semester starts
- Large application volumes
- Systematic allocation runs

### Accessing Batch Allocation

1. Navigate to **Admin > Staged Allocations**
2. Or go to `/admin/staged-allocations`

### Selecting Multiple Students

**Step 1: Student Selection**

1. View list of students with AWAITING_ALLOCATION status
2. Use checkboxes to select students
3. Maximum 50 students per batch
4. Use master checkbox for select all

**Selection Guidance:**
- Select students with similar preferences together
- Consider building capacity
- Group by accessibility requirements if helpful

### Proposing Batch

**Step 2: Run Proposal**

1. Click **Run Batch Proposal**
2. System generates batch ID (format: BATCH-YYYYMMDDHHMMSS)
3. Wait for processing (time varies by batch size)
4. Proposals are created for each student

### Reviewing Proposals

**Step 3: Review & Confirm**

Each proposal shows:

| Field | Description |
|-------|-------------|
| Student | Name and application code |
| Proposed Room | Building - Room Number |
| Eligible | Whether room meets all restrictions |
| Warnings | Any constraint issues |
| Status | PROPOSED, CONFIRMED, or REJECTED |

### Confirming Batch or Individual Rejections

**Individual Actions:**

- **Confirm** (check icon): Accept this proposal
- **Skip** (cancel icon): Reject this proposal

**Batch Actions:**

- **Confirm All Pending**: Confirms all PROPOSED allocations at once

**After Completion:**

- Confirmed students are allocated
- Skipped students remain on waiting list
- Return to application review for skipped students

---

## Managing Waiting List

### Accessing Waiting List Dashboard

1. Navigate to **Admin > Waiting List**
2. Or go to `/admin/waiting-list`

### Understanding Priority

Students on the waiting list are ranked by:

| Factor | Impact |
|--------|--------|
| Application date | Earlier = Higher priority |
| Special requirements | May receive priority boost |

**Display:**
- Position in queue (top 3 highlighted)
- Application date
- Visual progress indicator

### When Restrictions Change

When room restrictions are updated:

1. Students on waiting list may have different eligible rooms
2. Review waiting list to identify newly eligible students
3. Allocate students as appropriate rooms become available

### Allocating from Waiting List

To allocate a waiting list student:

1. Click **Review Application** next to the student
2. Opens application review with filter applied
3. Process as normal manual allocation
4. Student is removed from waiting list upon allocation

### Monitoring Waiting List Size

Dashboard indicators:
- Total students on waiting list
- Average wait time
- Score distribution

**Action Triggers:**
- Large waiting list = Consider batch allocation
- Long wait times = Review capacity planning
- Score clustering = Check allocation rules

---

## Moving Students Between Rooms

### When to Move Students

Common reasons:
- Room reassignment requests
- Conflict resolution
- Maintenance requirements
- Accessibility accommodations
- Building closures

### Opening Move Dialog

1. Navigate to **Room Allocations** (`/admin/room-allocations`)
2. Find the student's current allocation
3. Click **Move Student** button (transfer_within_a_station icon)
4. Move Dialog opens

**Note**: Only available for ACTIVE allocations.

### Validating New Room

The move dialog shows:

**Current Room Info (Read-Only):**
- Current building and room number
- Current allocation date

**New Room Selection:**
1. Select building from dropdown
2. Enter or search room number
3. Only rooms with available capacity shown
4. System validates availability

### Capturing Reason (Compliance Requirement)

**Mandatory fields:**

| Field | Requirement |
|-------|-------------|
| Reason | Required, minimum 10 characters, maximum 500 |
| Target Room | Required, must have availability |

Example reasons:
- "Student requested move due to roommate conflict"
- "Room requires maintenance, relocating temporarily"
- "Accessibility accommodation upgrade"

### Confirming Move

1. Verify target room details
2. Enter comprehensive reason
3. Click **Confirm Move**
4. System updates allocation
5. Audit trail created

### Student Notifications

Upon move confirmation:
- Student receives email notification
- Notification includes:
  - New room assignment
  - Move date
  - Contact for questions

---

## Compliance Considerations

### Per-Case Review Requirement

Every allocation requires individual review:

- Automated suggestions must be confirmed by admin
- Batch allocations require review before confirmation
- No fully automated allocations without admin approval

### Audit Trail of All Decisions

The system records:

| Action | Captured Data |
|--------|---------------|
| Document Review | Status changes, timestamps, admin ID |
| Allocation | Method (manual/auto), admin ID, timestamp |
| Moves | Old room, new room, reason, admin ID |
| Overrides | Constraints bypassed, reason, admin ID |

Access audit logs via:
- Application detail history tab
- Allocation history in room management
- System logs (admin access required)

### Reason Capture for Overrides

When bypassing constraints, document:

1. **Which constraint**: Gender restriction, accessibility requirement
2. **Why bypassing**: Clear explanation
3. **Approval**: Your admin credentials serve as approval

**Example Override Reasons:**
- "Student provided medical documentation exempting gender restriction"
- "Temporary placement while accessible room becomes available"
- "Student waived accessibility requirement in writing"

### Gender and Accessibility Protections

**Gender Restrictions:**
- Enforced at room and building level
- Cannot be accidentally bypassed
- Requires explicit override with reason

**Accessibility Requirements:**
- Students with documented needs must receive compliant rooms
- System flags non-compliant assignments
- Override requires documented justification

### Document Preservation

All documents are preserved for:
- Regulatory compliance
- Audit requirements
- Dispute resolution

Document retention:
- Original uploads preserved
- Version history maintained
- Admin comments timestamped
- Status change history logged

---

## Quick Reference: Status Workflows

### Application Status Flow

```
PENDING
   ├── APPROVED ──► AWAITING_ALLOCATION ──► ALLOCATED
   │                       │
   └── REJECTED            └── REJECTED (if allocation fails)
```

### Allocation Status Flow

```
PENDING ──► ALLOCATED ──► ACTIVE ──► COMPLETED
                │                       │
                └── REJECTED            └── CANCELLED
```

### Document Status Flow

```
NOT_UPLOADED ──► PENDING ──► APPROVED
                    │
                    └── REJECTED ──► PENDING (resubmit)
```

---

## Keyboard Shortcuts

| Action | Shortcut |
|--------|----------|
| Save Progress | Ctrl + S (in dialog) |
| Next Application | Arrow Down |
| Previous Application | Arrow Up |
| Open Details | Enter |
| Close Dialog | Escape |

---

## Support

For additional assistance:
- Refer to the [Room Restrictions & Eligibility Guide](./ALLOCATION_RULES_GUIDE.md)
- Contact system administrator
- Check system logs for error details

---

*Document Version: 1.0 | Last Updated: January 2026*
