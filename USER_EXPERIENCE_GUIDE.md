# Auto-Allocation Feature - User Experience Guide

## Overview

This document describes what administrators and students will experience when the auto-allocation feature is enabled.

---

## Admin Experience

### Scenario 1: Approving Application (Auto-Allocation Enabled, Rooms Available)

**Admin Action:**
1. Reviews student application in admin portal
2. Clicks "Approve" button
3. Optionally adds remarks
4. Confirms approval

**System Response:**
```
✅ Application approved successfully
ℹ️ Room automatically allocated: Room 101, Building A
✅ Student notified of allocation offer
```

**What Happens Behind the Scenes:**
- Application status → `APPROVED`
- System automatically scores available rooms
- Best matching room selected (e.g., Room 101)
- Room allocation created with status `OFFERED`
- Allocation method marked as `AUTO`
- Student receives two notifications:
  1. "Your application has been approved"
  2. "You have been offered Room 101 in Building A"

**Admin Portal Shows:**
- Application Status: `APPROVED`
- Allocation Status: `OFFERED`
- Room: `101 - Building A`
- Allocation Method: `AUTO` (badge or indicator)
- Score: `95.5` (matching score)

**Time Saved:**
- Previously: ~5-10 minutes to manually review rooms and create allocation
- Now: **Instant** (automatic allocation in < 1 second)

---

### Scenario 2: Approving Application (No Rooms Available)

**Admin Action:**
1. Reviews and approves application
2. Clicks "Approve" button

**System Response:**
```
✅ Application approved successfully
⚠️ No rooms currently available - Student added to waiting list
ℹ️ Student will be notified when a room becomes available
```

**What Happens Behind the Scenes:**
- Application status → `AWAITING_ALLOCATION`
- System attempts to find rooms → None available
- Student automatically added to priority waiting list
- Student receives two notifications:
  1. "Your application has been approved"
  2. "You've been added to the waiting list"

**Admin Portal Shows:**
- Application Status: `AWAITING_ALLOCATION`
- Waiting List: `Yes (Priority Queue)`
- Position: `#5 in queue`
- Next Action: `Will auto-allocate when room available`

---

### Scenario 3: Approving Application (Documents Not Yet Approved)

**Admin Action:**
1. Approves application before all documents are approved

**System Response:**
```
✅ Application approved successfully
ℹ️ Waiting for document approval before allocation
📋 Approved documents: 3/5
```

**What Happens Behind the Scenes:**
- Application status → `AWAITING_ALLOCATION`
- System detects not all documents approved
- Allocation deferred until documents approved
- Student notified that documents are pending

**Admin Portal Shows:**
- Application Status: `AWAITING_ALLOCATION`
- Documents: `3/5 approved`
- Next Action: `Approve remaining documents`
- Note: "Allocation will proceed after document approval"

---

### Scenario 4: Feature Flag Disabled (Manual Allocation)

**Admin Action:**
1. Approves application

**System Response:**
```
✅ Application approved successfully
📝 Ready for manual allocation
```

**What Happens Behind the Scenes:**
- Application status → `APPROVED`
- No automatic allocation attempted
- Admin must manually create allocation (existing workflow)

**Admin Portal Shows:**
- Application Status: `APPROVED`
- Allocation Status: `None`
- Action Button: `Create Allocation` (manual)

---

## Student Experience

### Scenario 1: Application Approved with Room Allocated

**Student Portal - Dashboard:**

```
🎉 Great News!

Your accommodation application has been approved!

📍 Room Offer Details:
   Room Number: 101
   Building: Building A
   Floor: 1st Floor
   Type: Single Room
   Monthly Rate: R2,500

📅 Offer Expires: February 17, 2026 (7 days)

Actions:
[Accept Offer]  [Decline Offer]  [Request Change]
```

**Notifications Received:**
1. **In-App Notification:**
   - Title: "Application Approved"
   - Message: "Your accommodation application has been approved!"
   - Time: Just now

2. **In-App Notification:**
   - Title: "Room Allocated"
   - Message: "Your room 101 in Building A has been allocated!"
   - Time: Just now

3. **Email Notification:** (Optional)
   - Subject: "Room Allocation Offer - Action Required"
   - Body: Details of room offer with acceptance deadline

**Student Actions:**
- **Accept Offer:** Confirm room allocation → Status changes to `ALLOCATED`
- **Decline Offer:** Reject room → Status changes to `AWAITING_ALLOCATION`, added to waiting list
- **Request Change:** Contact admin for different room (manual process)

---

### Scenario 2: Application Approved but on Waiting List

**Student Portal - Dashboard:**

```
✅ Application Approved

Your accommodation application has been approved!

⏳ Waiting List Status:
   You've been added to our priority waiting list.

   Current Position: #5
   Estimated Wait: 1-2 weeks

   What happens next:
   ✓ We're actively monitoring room availability
   ✓ You'll be notified immediately when a room becomes available
   ✓ Your position is secure based on your application date

📧 We'll keep you updated via email and in-app notifications.
```

**Notifications Received:**
1. **In-App Notification:**
   - Title: "Application Approved"
   - Message: "Your accommodation application has been approved!"

2. **In-App Notification:**
   - Title: "Added to Waiting List"
   - Message: "You've been added to the waiting list. We'll notify you as soon as a room becomes available."

3. **Email Notification:** (Optional)
   - Subject: "Application Approved - Waiting List"
   - Body: Explanation of waiting list process and next steps

**Student Experience:**
- Clear communication about status
- Transparency on waiting list position
- Estimated timeline provided
- No action required (wait for room availability)

---

### Scenario 3: Application Approved but Documents Pending

**Student Portal - Dashboard:**

```
✅ Application Approved

Your accommodation application has been approved!

📋 Documents Required:
   ✅ Proof of Registration (Approved)
   ✅ ID Document (Approved)
   ✅ Proof of Address (Approved)
   ⏳ Financial Guarantee (Pending Review)
   ❌ Medical Certificate (Not Uploaded)

⚠️ Action Required:
   Please upload your Medical Certificate to proceed with room allocation.

📝 Upload Documents: [Go to Documents →]

Once all documents are approved, we'll allocate a room for you.
```

**Notifications Received:**
1. **In-App Notification:**
   - Title: "Application Approved"
   - Message: "Your application has been approved! Please upload remaining documents."

**Student Experience:**
- Clear indication of what's needed
- Progress tracking (3/5 approved)
- Direct link to upload documents
- Understanding that allocation is pending document approval

---

## Comparison: Before vs. After Auto-Allocation

### Admin Workflow

| Step | Before (Manual) | After (Auto) | Time Saved |
|------|----------------|-------------|------------|
| Approve application | Click "Approve" | Click "Approve" | 0s |
| Search for rooms | Manual search, filters | Automatic scoring | ~2-3 min |
| Compare rooms | Review multiple rooms | Best match selected | ~2-3 min |
| Create allocation | Fill allocation form | Automatic creation | ~1-2 min |
| Notify student | Automatic | Automatic | 0s |
| **Total Time** | **~6-8 minutes** | **< 5 seconds** | **6-8 min per application** |

**Annual Impact:**
- Assuming 500 applications/year
- Time saved: 500 × 7 min = **3,500 minutes** (58 hours)
- **Over 1 week of work saved per year**

### Student Experience

| Aspect | Before (Manual) | After (Auto) |
|--------|----------------|-------------|
| Wait time after approval | 1-3 days | Immediate (seconds) |
| Transparency | Limited visibility | Clear status updates |
| Notifications | Email only | Email + In-app + Dashboard |
| Waiting list | Manual tracking | Automated with position |
| Overall satisfaction | Good | **Excellent** |

---

## Feature Flag Control

### Admin Settings Panel

```
Feature Flags

┌─────────────────────────────────────────────────┐
│ Auto-Allocation                                 │
│                                                 │
│ Status: ● ENABLED                               │
│                                                 │
│ Description:                                    │
│ Automatically allocate rooms to students when   │
│ their applications are approved.                │
│                                                 │
│ Statistics (Last 30 Days):                      │
│ • Allocations: 87 successful, 3 waiting list    │
│ • Success Rate: 96.7%                           │
│ • Average Score: 89.2                           │
│ • Time Saved: ~10 hours                         │
│                                                 │
│ [Disable Feature]  [View Analytics]             │
└─────────────────────────────────────────────────┘
```

**Toggle Effect:**
- **Enabled:** Automatic allocation on approval
- **Disabled:** Manual allocation required (existing workflow)
- **Runtime Toggle:** No system restart needed
- **Gradual Rollout:** Can enable for specific buildings or student types

---

## Notifications Summary

### Student Receives

**Scenario 1: Successful Allocation**
- ✉️ Email: "Room Allocation Offer - Action Required"
- 🔔 In-App: "Application Approved"
- 🔔 In-App: "Room Allocated"

**Scenario 2: Waiting List**
- ✉️ Email: "Application Approved - Waiting List"
- 🔔 In-App: "Application Approved"
- 🔔 In-App: "Added to Waiting List"

**Scenario 3: Documents Pending**
- ✉️ Email: "Application Approved - Action Required"
- 🔔 In-App: "Application Approved"
- 🔔 In-App: "Documents Required"

### Admin Receives

- 📊 Daily Summary: "Auto-Allocation Report"
  - Successful allocations
  - Waiting list additions
  - Document pending cases
  - Average matching scores

---

## Support & FAQs

### For Students

**Q: How long does auto-allocation take?**
A: Immediate! Within seconds of application approval, you'll receive a room offer if available.

**Q: Can I decline the auto-allocated room?**
A: Yes, you can decline and request a different room. You'll be added to the waiting list.

**Q: What if I'm on the waiting list?**
A: You'll be notified immediately when a room becomes available. Your position is based on application date.

**Q: How is the room selected for me?**
A: Our smart algorithm matches you with the best room based on your preferences, accessibility needs, and room availability.

### For Admins

**Q: Can I override auto-allocation?**
A: Yes, you can manually change the allocation after approval.

**Q: What happens if allocation fails?**
A: The application approval still succeeds. Student is added to waiting list or status set to AWAITING_ALLOCATION for manual handling.

**Q: Can I disable auto-allocation?**
A: Yes, use the feature flag toggle. System falls back to manual allocation immediately.

**Q: How do I monitor auto-allocation performance?**
A: Check the analytics dashboard for success rates, average scores, and time saved.

---

## Future Enhancements (Roadmap)

### Phase 2 (Planned)
- ✨ Auto-allocation when documents are approved (not just application approval)
- ✨ Smart waiting list processing (auto-allocate when rooms become available)
- ✨ Student preference weighting (allow students to set preferences)
- ✨ Bulk allocation for multiple students at once

### Phase 3 (Proposed)
- 🎯 Machine learning for better room matching
- 📊 Advanced analytics and reporting
- 🤝 Roommate matching algorithm
- 📱 Mobile push notifications

---

**Document Version:** 1.0
**Last Updated:** February 10, 2026
**Status:** Ready for User Acceptance Testing
