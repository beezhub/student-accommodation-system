# Room Restrictions & Eligibility Guide

This guide explains how room restrictions and eligibility work in the Student Accommodation System, including building-institution relationships, room-level restrictions, and manual allocation workflow.

## Table of Contents

1. [Overview](#overview)
2. [Building-Level Restrictions](#building-level-restrictions)
3. [Room-Level Restrictions](#room-level-restrictions)
4. [Eligibility Filtering](#eligibility-filtering)
5. [Manual Allocation Workflow](#manual-allocation-workflow)
6. [Configuration Guide](#configuration-guide)
7. [Troubleshooting](#troubleshooting)

---

## Overview

### How Room Eligibility Works

The system uses a **simplified restriction-based approach** to determine which rooms are eligible for each student. Instead of weighted scoring, rooms are filtered based on:

- **Institution** (buildings serve specific institutions)
- **Gender** (room-level restriction)
- **Accessibility** (room-level compliance)
- **Year of Study** (room-level min/max range)

### How Restrictions Affect Room Assignments

When allocating a room to a student, the system:

1. Filters buildings by **student's institution**
2. Filters rooms by **gender restriction** (must match or be ANY)
3. Filters by **accessibility compliance** (if student requires it)
4. Filters by **year of study range** (student's year must be within min-max)
5. Shows only **eligible rooms** to the administrator
6. **Administrator manually selects** the appropriate room

### Key Principles

| Principle | Description |
|-----------|-------------|
| Institution-Based | Buildings serve specific institutions |
| Room-Level Restrictions | Gender, accessibility, year of study at room level |
| Manual Decision | Admin reviews eligible rooms and decides |
| No Scoring | Removed weighted scoring system |

---

## Building-Level Restrictions

### Institution Relationship

Buildings can be configured to serve **specific institutions** through a many-to-many relationship:

- A building can serve one or multiple institutions
- A building with no institutions serves ALL institutions (no restriction)
- Students only see buildings that serve their institution

### Configuration

**Admin Panel: Building Management**

1. Navigate to **Admin > Building Management**
2. Edit or create a building
3. Select institutions from the multi-select dropdown
4. Save the building

### Example Scenarios

| Building | Institutions | Result |
|----------|--------------|--------|
| Residence A | University X | Only University X students see this building |
| Residence B | University X, College Y | Students from both institutions see this building |
| Residence C | (none selected) | All students see this building |

### Impact on Student Applications

When students submit applications:

- Preferred Building dropdown shows only buildings serving their institution
- Students cannot select buildings restricted to other institutions
- Admin allocation also respects these restrictions

---

## Room-Level Restrictions

### Available Restrictions

Each room can have the following restrictions configured:

| Restriction | Type | Description |
|-------------|------|-------------|
| Gender | Enum | MALE_ONLY, FEMALE_ONLY, or ANY |
| Accessibility | Boolean | Whether room is accessibility compliant |
| Min Year of Study | Integer | Minimum year (e.g., 1 for first-year only) |
| Max Year of Study | Integer | Maximum year (e.g., 2 for years 1-2 only) |

### Gender Restriction

| Value | Description |
|-------|-------------|
| `ANY` | No gender restriction (default) |
| `MALE_ONLY` | Only male students allowed |
| `FEMALE_ONLY` | Only female students allowed |

### Year of Study Range

Configure which year students can be allocated to a room:

| Configuration | Eligible Students |
|---------------|-------------------|
| Min: null, Max: null | All years |
| Min: 1, Max: 1 | First-year only |
| Min: 1, Max: 2 | First and second year |
| Min: 3, Max: null | Third year and above |
| Min: null, Max: 2 | Up to second year |

### Accessibility Compliance

| Room Setting | Student Requirement | Result |
|--------------|---------------------|--------|
| Compliant | Requires accessibility | ELIGIBLE |
| Compliant | No requirement | ELIGIBLE |
| Not compliant | Requires accessibility | NOT ELIGIBLE |
| Not compliant | No requirement | ELIGIBLE |

### Bulk Configuration

Configure multiple rooms at once:

1. Navigate to **Admin > Room Management**
2. Select multiple rooms using checkboxes
3. Click **Edit Selected**
4. Set restrictions for all selected rooms
5. Save changes

---

## Eligibility Filtering

### Filter Order

When determining eligible rooms for a student, the system applies filters in this order:

1. **Institution Filter**: Building must serve student's institution
2. **Gender Filter**: Room gender restriction must match student or be ANY
3. **Accessibility Filter**: If student requires accessibility, room must be compliant
4. **Year of Study Filter**: Student's year must be within room's min-max range
5. **Availability Filter**: Room must have available capacity

### Eligibility Examples

**Example 1: First-Year Female Student from University X**

```
Student: Female, Year 1, University X, No accessibility needs

Room A: Building serves University X, Female Only, Year 1-2 → ELIGIBLE
Room B: Building serves University X, Male Only, Year 1-2 → NOT ELIGIBLE (gender)
Room C: Building serves College Y, Female Only, Year 1-2 → NOT ELIGIBLE (institution)
Room D: Building serves University X, Any Gender, Year 3-4 → NOT ELIGIBLE (year)
```

**Example 2: Third-Year Male Student Requiring Accessibility**

```
Student: Male, Year 3, University X, Requires accessibility

Room A: University X, Male Only, Accessible, Year 1-4 → ELIGIBLE
Room B: University X, Male Only, Not Accessible, Year 1-4 → NOT ELIGIBLE (accessibility)
Room C: University X, Any Gender, Accessible, Year 1-2 → NOT ELIGIBLE (year)
```

### No Scoring System

The system does **not** use weighted scoring. All eligible rooms are shown equally to the administrator, who makes the final decision based on:

- Student preferences (preferred building)
- Room availability
- Any special circumstances
- Institutional policies

---

## Manual Allocation Workflow

### Overview

All allocations are performed **manually by administrators**. The system shows only eligible rooms, and the admin makes the final decision.

### Allocation Steps

1. **Open Allocation Dialog**: Click "Allocate Room" on an approved application
2. **View Eligible Rooms**: System shows rooms filtered by all restrictions
3. **Filter by Building**: Optionally filter to student's preferred building
4. **Select Room**: Choose from available eligible rooms
5. **Confirm Allocation**: Finalize the allocation

### What Admins See

The allocation dialog displays:

| Column | Description |
|--------|-------------|
| Room Number | Room identifier |
| Building | Building name |
| Occupancy | Current / Capacity |
| Available | Number of available spots |
| Gender | Gender restriction (if any) |
| Accessible | Accessibility status |
| Year Range | Min-Max year restriction |

### Filtering Options

- **Building Filter**: Show rooms from specific building
- **Room Number Search**: Find specific room

### Admin Override

In exceptional cases, administrators can bypass constraints:

1. Enable **Bypass Constraints** option
2. Provide **mandatory reason** (minimum 10 characters)
3. Confirm the override

This creates an audit trail documenting:
- Which constraints were bypassed
- The reason for the override
- The administrator who performed the action
- Timestamp of the decision

---

## Configuration Guide

### Configuring Building Institutions

1. Navigate to **Admin > Building Management**
2. Click **Edit** on a building
3. In the **Institutions** section, select institutions
4. Click **Save**

### Configuring Room Restrictions

**Individual Room:**

1. Navigate to **Admin > Room Management**
2. Click **Room Attributes** on a room
3. Set:
   - Gender Restriction (ANY, MALE_ONLY, FEMALE_ONLY)
   - Accessibility Compliant (Yes/No)
   - Min Year of Study (optional)
   - Max Year of Study (optional)
4. Click **Save**

**Bulk Configuration:**

1. Navigate to **Admin > Room Management**
2. Select multiple rooms using checkboxes
3. Click **Edit Selected**
4. Set restrictions for all selected rooms
5. Click **Apply to All**

### Validating Configuration

Before deployment:

1. Test allocation with a sample student
2. Verify eligible rooms list shows correct rooms
3. Check that restricted rooms are properly filtered
4. Confirm building-institution relationships work

### Monitoring

After configuring restrictions:

1. Review allocation reports in Dashboard
2. Check waiting list size (high = restrictions may be too tight)
3. Verify students are being allocated to appropriate rooms
4. Adjust restrictions as needed

---

## Troubleshooting

### "Why doesn't this student see any eligible rooms?"

**Check these factors:**

1. **Institution Mismatch**
   - Does the student's institution match any building's institutions?
   - Solution: Add student's institution to appropriate buildings

2. **Gender Restriction**
   - Do available rooms match the student's gender?
   - Solution: Check room gender restrictions, add rooms with ANY restriction

3. **Year of Study**
   - Is the student's year within available rooms' year ranges?
   - Solution: Adjust room year restrictions or add rooms for that year

4. **Accessibility**
   - Does student require accessibility but no compliant rooms available?
   - Solution: Mark more rooms as accessibility compliant

**Debugging Steps:**

```
1. Open Application Detail Dialog
2. Note student's institution, gender, year, accessibility needs
3. Check Building Management for institution assignments
4. Check Room Management for room restrictions
5. Verify at least one room matches all criteria
```

### "Why can't student see their preferred building?"

**Possible Causes:**

1. Building doesn't serve student's institution
2. Building has no rooms matching student's restrictions

**Solutions:**

1. Add student's institution to the building
2. Configure rooms in that building to accept the student's profile

### "How do I reserve rooms for specific year groups?"

Use the year of study restrictions:

1. Navigate to **Room Management**
2. Select rooms to reserve for specific years
3. Set Min Year and Max Year appropriately
4. Example: Min=1, Max=1 for first-year only

### "How do I handle multi-institution buildings?"

Buildings can serve multiple institutions:

1. Edit the building
2. Select multiple institutions in the dropdown
3. Students from ANY selected institution can see the building

### "Can I change restrictions mid-allocation season?"

**Yes, with considerations:**

- Changes apply immediately to **new allocations**
- Already confirmed allocations are **not affected**
- Students on waiting list will see updated eligible rooms

**Best Practices:**

1. Document the reason for changes
2. Review impact on waiting list students
3. Communicate changes to the admin team

---

## API Reference

### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/building/by-institution/{institutionId}` | Get buildings by institution |
| GET | `/api/admin/eligible-rooms?studentId={id}&buildingId={id}` | Get eligible rooms for student |
| POST | `/api/admin/room-attributes/bulk` | Bulk update room attributes |
| PUT | `/api/admin/room-attributes/{roomId}` | Update room attributes |

### Room Attributes Schema

```json
{
  "id": 1,
  "roomId": 101,
  "roomNumber": "A101",
  "buildingId": 5,
  "buildingName": "Residence Hall A",
  "genderRestriction": "FEMALE_ONLY",
  "accessibilityCompliant": true,
  "minYearOfStudy": 1,
  "maxYearOfStudy": 2,
  "createdAt": "2026-01-15T10:30:00",
  "updatedAt": "2026-01-20T14:00:00"
}
```

### Building Schema (with Institutions)

```json
{
  "id": 5,
  "name": "Residence Hall A",
  "address": "123 Campus Drive",
  "institutions": [
    { "id": 1, "name": "University X" },
    { "id": 2, "name": "College Y" }
  ]
}
```

---

*For additional support, contact the system administrator or refer to the Admin Allocation Workflow guide.*
