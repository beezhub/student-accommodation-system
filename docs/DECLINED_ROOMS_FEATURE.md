# Declined Rooms Feature

## Overview

This feature prevents the system from offering the same room to a student who has previously declined it. This improves the user experience by respecting student preferences and avoiding repeated offers of unsuitable accommodations.

## Implementation Details

### Changes Made

#### 1. RoomAllocationRepository.java
**Added Method:**
```java
boolean hasStudentDeclinedRoom(@Param("studentId") Long studentId, @Param("roomId") Long roomId);
```

**Purpose:** Checks if a specific student has previously declined a specific room by querying room_allocation records with status = DECLINED.

**Query:**
```sql
SELECT COUNT(ra) > 0
FROM RoomAllocation ra
WHERE ra.student.id = :studentId
  AND ra.room.id = :roomId
  AND ra.status = DECLINED
```

#### 2. AllocationScoringService.java
**Modified Method:** `validateHardConstraints()`

**Added Check:**
```java
// Check if student has previously declined this room
if (roomAllocationRepository.hasStudentDeclinedRoom(student.getId(), room.getId())) {
    log.debug("Student {} has previously declined room {}, excluding from allocation",
              student.getId(), room.getRoomNumber());
    return false;
}
```

**Location:** Added as the final check before returning true, after all other hard constraints (gender, accessibility, year of study, institution).

**Impact:** Declined rooms are treated as a hard constraint - they are completely excluded from scoring and will never be offered to that student again.

### Test Coverage

#### AllocationScoringServiceTest.java
Added 3 new tests:

1. **validateHardConstraints_shouldFail_whenStudentHasDeclinedRoom()**
   - Verifies that validation fails when a student has previously declined a room
   - Expected: `validateHardConstraints()` returns false

2. **validateHardConstraints_shouldPass_whenStudentHasNotDeclinedRoom()**
   - Verifies that validation passes when a student has not declined a room
   - Expected: `validateHardConstraints()` returns true

3. **scoreAvailableRooms_shouldExcludeDeclinedRooms()**
   - Verifies that declined rooms are excluded from the scored rooms list
   - Expected: Scored rooms list is empty when the only available room was declined

#### RoomAllocationServiceTest.java
Added 1 integration test:

1. **allocateNextFromWaitingList_shouldSkipDeclinedRooms()**
   - Verifies that auto-allocation skips students when all suitable rooms have been declined
   - Expected: No allocation occurs, no waiting list update

### Test Results

✅ **All 17 allocation-related tests passing:**
- 6 AllocationScoringServiceTest tests
- 11 RoomAllocationServiceTest tests

## How It Works

### Allocation Flow with Declined Rooms Check

1. **Student on Waiting List**: System identifies a student needing allocation
2. **Find Available Rooms**: Query all rooms with available capacity
3. **Validate Hard Constraints**: For each room, check:
   - Gender compatibility ✓
   - Accessibility requirements ✓
   - Year of study constraints ✓
   - Institution compatibility ✓
   - **Student has not declined this room** ✓ (NEW)
4. **Score Remaining Rooms**: Only rooms passing all checks are scored
5. **Allocate Best Match**: Highest-scored room is offered to the student

### Example Scenario

**Initial State:**
- Student A on waiting list
- Room 101 available
- Room 102 available

**Step 1: First Allocation**
- System offers Room 101 to Student A
- Student A declines Room 101 (prefers different building)
- Record created: `room_allocation` with status = DECLINED

**Step 2: Second Allocation Attempt**
- Room 101 becomes available again (another student declined)
- System evaluates rooms for Student A
- Room 101: ❌ EXCLUDED (student previously declined)
- Room 102: ✓ PASSES all constraints
- System offers Room 102 to Student A

**Result:** Student A never sees Room 101 again in future allocations.

## Database Impact

- **No schema changes required** - Uses existing `room_allocation` table
- **Query Performance**: Indexed query on `student_id`, `room_id`, and `status`
- **Data Growth**: Naturally grows with allocation history (no additional tables)

## Benefits

1. **Improved User Experience**: Students don't see unsuitable rooms repeatedly
2. **Reduced Decision Fatigue**: Fewer irrelevant offers
3. **Better Allocation Efficiency**: Focus on genuinely suitable options
4. **Historical Tracking**: Decline history is preserved for analytics

## Configuration

No configuration needed - feature is automatically enabled for all allocations.

## Limitations and Considerations

1. **Permanent Exclusion**: Once declined, a room is permanently excluded for that student
   - **Consideration**: Students cannot "change their mind" about a previously declined room
   - **Mitigation**: Admin can manually allocate if student requests a previously declined room

2. **Scope**: Only applies to specific room declinations
   - Declining Room 101 in Building A does not exclude Room 102 in Building A
   - Each room-student pair is tracked independently

3. **Multiple Applications**: If a student submits multiple applications:
   - Decline history persists across applications
   - Student-room relationship is tracked, not application-room

## Future Enhancements

### Potential Improvements (Out of Scope)

1. **Time-Based Reset**: Allow declined rooms to become available after a time period (e.g., 6 months)
2. **Decline Reason Analysis**: Track why rooms are declined to improve matching algorithm
3. **Admin Override**: UI feature to reset decline history for specific student-room pairs
4. **Building-Level Exclusion**: Option to exclude entire buildings if student declines multiple rooms there
5. **Student Portal**: Show students their decline history and allow them to reset specific rooms

## Testing Checklist

- [x] Unit tests for repository method
- [x] Unit tests for hard constraint validation
- [x] Unit tests for scoring exclusion
- [x] Integration test for auto-allocation flow
- [ ] Manual testing with database (after DB repair)
- [ ] Load testing with large decline history

## Related Documentation

- Auto-Allocation Feature: See `backend/docs/AUTO_ALLOCATION_FEATURE.md`
- Allocation Scoring: See `AllocationScoringService.java` javadoc
- Room Allocation Lifecycle: See `AllocationStatus` enum

---

**Date Implemented**: 2026-02-09
**Author**: Claude (AI Assistant)
**Status**: Implemented and Tested ✅
