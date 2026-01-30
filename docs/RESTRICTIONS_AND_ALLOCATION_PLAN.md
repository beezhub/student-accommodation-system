# Restrictions & Allocation System

## Overview

The allocation system uses a two-level restriction model to ensure students are allocated to appropriate rooms:

- **Building-level**: Institution restrictions (buildings serve specific institutions)
- **Room-level**: Gender, Accessibility, and Year of Study restrictions

Admins manually allocate students to rooms, with the system filtering to show only eligible rooms based on the student's profile.

---

## Implementation Status

| Phase | Feature | Status |
|-------|---------|--------|
| 1 | Building-Institution Relationship | **COMPLETE** |
| 2 | Room Attributes (Year of Study) | **COMPLETE** |
| 3 | Bulk Room Attributes Endpoint | **COMPLETE** |
| 4 | Eligibility Filter Service | **COMPLETE** |
| 5 | Allocation Rules (Deprecated) | **COMPLETE** - Fields marked @Deprecated |
| 6 | Building-Level Gender (Deprecated) | **COMPLETE** - Fields marked @Deprecated |
| 7 | Building Management UI | **COMPLETE** |
| 8 | Room Attributes Dialog | **COMPLETE** |
| 9 | Bulk Edit UI | **COMPLETE** |
| 10 | Allocation Dialog | **COMPLETE** |
| 11 | Remove Allocation Rules UI | **COMPLETE** |
| 12 | Student Application Filter | **COMPLETE** |

---

## Architecture

### Restriction Hierarchy

```
Building
├── Institution Restriction (many-to-many)
│   └── Only students from linked institutions can be allocated
│
└── Rooms
    ├── Gender Restriction (ANY, MALE_ONLY, FEMALE_ONLY)
    ├── Accessibility Compliant (boolean)
    └── Year of Study Range (min, max - nullable)
```

### Eligibility Flow

```
Student applies for accommodation
        │
        ▼
Admin opens allocation dialog
        │
        ▼
System calls getEligibleRoomsForStudent()
        │
        ├── Filter by Institution (building must serve student's institution)
        ├── Filter by Gender (room restriction matches student)
        ├── Filter by Accessibility (if student requires it)
        └── Filter by Year of Study (student's year within range)
        │
        ▼
Admin sees only eligible rooms
        │
        ▼
Admin manually selects room
```

---

## API Endpoints

### Room Attributes

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/room-attributes/room/{roomId}` | Get room attributes |
| POST | `/api/admin/room-attributes` | Save room attributes |
| POST | `/api/admin/room-attributes/bulk` | Bulk save room attributes |
| DELETE | `/api/admin/room-attributes/room/{roomId}` | Delete room attributes |

### Building Attributes

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/buildings/{buildingId}/attributes` | Get building attributes |
| PUT | `/api/admin/buildings/{buildingId}/attributes` | Update building attributes |
| GET | `/api/admin/buildings/{buildingId}/rooms/attributes` | Get all room attributes for building |

### Eligible Rooms

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/room-allocation/admin/student/{studentId}/eligible-rooms` | Get eligible rooms for student |
| GET | `/api/room-allocation/admin/student/{studentId}/eligible-rooms?buildingId={id}` | Filter by building |

### Buildings by Institution

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/building/by-institution/{institutionId}` | Get buildings serving an institution |

---

## Data Models

### RoomAttributes Entity

```java
@Entity
public class RoomAttributes {
    private Long id;
    private Room room;
    private Boolean accessibilityCompliant;      // Default: false
    private GenderRestriction genderRestriction; // ANY, MALE_ONLY, FEMALE_ONLY
    private Integer minYearOfStudy;              // Nullable (null = no minimum)
    private Integer maxYearOfStudy;              // Nullable (null = no maximum)
}
```

### Building Entity (Institution Relationship)

```java
@Entity
public class Building {
    // ... other fields

    @ManyToMany
    @JoinTable(
        name = "building_institution",
        joinColumns = @JoinColumn(name = "building_id"),
        inverseJoinColumns = @JoinColumn(name = "institution_id")
    )
    private Set<Institution> institutions;
}
```

### BuildingAttributes Entity (Deprecated Fields)

```java
@Entity
public class BuildingAttributes {
    private Long id;
    private Building building;

    @Deprecated  // Gender restrictions now at room level only
    private GenderRestriction genderRestriction;

    @Deprecated  // No longer used
    private Boolean allowMixedGender;

    @Deprecated  // Scoring removed
    private Integer desirabilityScore;

    private String accessibilityFeatures;  // Informational only (JSON)
}
```

---

## Frontend Components

### Room Management

**Location**: `frontend/src/app/features/admin/room-management/`

Features:
- Room table with checkbox selection for bulk operations
- Single room attributes editing via dialog
- Bulk attributes editing for multiple rooms
- Displays gender and accessibility badges

**Key Files**:
- `room-management.component.ts` - Main component with selection logic
- `room-attributes-dialog/` - Single room attribute editor
- `bulk-attributes-dialog/` - Bulk attribute editor

### Building Management

**Location**: `frontend/src/app/features/admin/building-management/`

Features:
- Institution multi-select when creating/editing buildings
- Displays which institutions each building serves

**Key Files**:
- `building-form-dialog.component.ts` - Form with institution selection

### Allocation Dialog

**Location**: `frontend/src/app/features/admin/application-review/allocation-choice-dialog/`

Features:
- Shows only eligible rooms based on student restrictions
- Manual room selection (no auto-allocation)
- Filters by building

### Student Application

**Location**: `frontend/src/app/features/auth/application-submission/`

Features:
- Preferred building dropdown filtered by student's institution
- Only shows buildings that serve the student's institution

---

## Services

### AllocationScoringService

**Location**: `backend/.../service/AllocationScoringService.java`

Key Methods:
- `getEligibleRoomsForStudent(Student, Building)` - Returns rooms meeting all constraints
- `getEligibleRoomsForStudentByInstitution(Student)` - Filters by student's institution
- `validateHardConstraints(Student, Room, boolean)` - Validates all restrictions

Constraint Checks:
1. `isGenderCompatible()` - Room gender restriction matches student
2. `isAccessibilityCompliant()` - Room is accessible if student requires it
3. `isYearOfStudyCompatible()` - Student's year within room's range
4. `isBuildingInstitutionCompatible()` - Building serves student's institution

### RoomAttributesService

**Location**: `backend/.../service/RoomAttributesService.java`

Key Methods:
- `getAttributesByRoomId(Long)` - Get or create default attributes
- `saveAttributes(RoomAttributesRequest, String)` - Save with year validation
- `bulkSaveAttributes(List<RoomAttributesRequest>, String)` - Bulk update

### BuildingService

**Location**: `backend/.../service/BuildingService.java`

Key Methods:
- `getBuildingsByInstitution(Long)` - Get buildings for an institution
- `createBuilding(BuildingRequest)` - Handles institution IDs
- `updateBuilding(Long, BuildingRequest)` - Updates institution relationships

---

## Database Schema

### building_institution (Junction Table)

```sql
CREATE TABLE building_institution (
    building_id BIGINT NOT NULL,
    institution_id BIGINT NOT NULL,
    PRIMARY KEY (building_id, institution_id),
    FOREIGN KEY (building_id) REFERENCES building(id) ON DELETE CASCADE,
    FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE CASCADE
);
```

### room_attributes

```sql
CREATE TABLE room_attributes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id BIGINT NOT NULL UNIQUE,
    accessibility_compliant BOOLEAN NOT NULL DEFAULT FALSE,
    gender_restriction VARCHAR(20),  -- 'ANY', 'MALE_ONLY', 'FEMALE_ONLY'
    min_year_of_study INTEGER,
    max_year_of_study INTEGER,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (room_id) REFERENCES room(id)
);
```

---

## Usage Guide

### Setting Room Restrictions

1. Navigate to **Admin > Room Management**
2. Click the tune icon on a room row to edit single room
3. Or select multiple rooms with checkboxes and click "Edit X Selected"
4. Set:
   - **Gender**: No Restriction, Female Only, or Male Only
   - **Accessibility**: Check if room is wheelchair accessible
   - **Year Range**: Set min/max year of study (leave empty for any)

### Assigning Institutions to Buildings

1. Navigate to **Admin > Building Management**
2. Click Edit on a building or create new
3. In the "Institutions Served" field, select one or more institutions
4. Buildings with no institutions selected accept students from all institutions

### Allocating Students

1. Navigate to **Admin > Applications**
2. Find an approved application and click "Allocate"
3. The system shows only rooms the student is eligible for based on:
   - Their institution
   - Their gender
   - Their year of study
   - Their accessibility needs
4. Select a room and confirm

---

## Verification Checklist

- [ ] Building with Institution X only shows for students from Institution X
- [ ] Room with Gender=Female only appears for female students
- [ ] Room with Year 1-2 only appears for Year 1 and Year 2 students
- [ ] Accessibility-required students only see accessible rooms
- [ ] Bulk edit updates multiple rooms at once
- [ ] Student application form shows only buildings for their institution
