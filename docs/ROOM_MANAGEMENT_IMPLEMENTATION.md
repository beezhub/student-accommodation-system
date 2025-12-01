# Room Management System - Implementation Guide

## Overview
Complete room management system with CRUD operations for buildings, rooms, room browser for students, and allocation interface for admins.

## Backend Status
✅ **FULLY IMPLEMENTED** - All REST endpoints exist and are ready to use.

### Available Endpoints:

#### Buildings (`/api/building`)
- `GET /api/building` - Get all buildings (paginated)
- `GET /api/building/all` - Get all buildings (no pagination)
- `GET /api/building/search` - Search buildings
- `GET /api/building/{id}` - Get building by ID
- `POST /api/building` - Create building (ADMIN)
- `PUT /api/building/{id}` - Update building (ADMIN)
- `DELETE /api/building/{id}` - Delete building (ADMIN)

#### Rooms (`/api/room`)
- `GET /api/room` - Get all rooms (paginated)
- `GET /api/room/all` - Get all rooms (no pagination)
- `GET /api/room/search` - Search rooms (supports filters)
- `GET /api/room/{id}` - Get room by ID
- `POST /api/room` - Create room (ADMIN)
- `PUT /api/room/{id}` - Update room (ADMIN)
- `DELETE /api/room/{id}` - Delete room (ADMIN)

#### Room Allocations (`/api/room-allocation`)
- `GET /api/room-allocation` - Get all allocations (paginated)
- `GET /api/room-allocation/search` - Search allocations
- `GET /api/room-allocation/status/{status}` - Get by status
- `GET /api/room-allocation/student/{id}/active` - Get student's active allocation
- `GET /api/room-allocation/student/{id}/eligibility` - Check eligibility
- `GET /api/room-allocation/room/{id}/availability` - Check room availability
- `POST /api/room-allocation` - Create allocation (ADMIN)
- `POST /api/room-allocation/allocate-by-application` - Allocate by application (ADMIN)
- `PUT /api/room-allocation/{id}` - Update allocation (ADMIN)
- `PATCH /api/room-allocation/{id}/end` - End allocation (ADMIN)
- `DELETE /api/room-allocation/{id}` - Delete allocation (ADMIN)

---

## Frontend Implementation

### Services Created ✅

1. **BuildingService** (`frontend/src/app/core/services/building.service.ts`)
   - Full CRUD operations
   - Search and filter
   - Pagination support

2. **RoomService** (`frontend/src/app/core/services/room.service.ts`)
   - Full CRUD operations
   - Advanced search (roomNumber, buildingId, capacity range)
   - Pagination support

3. **RoomAllocationService** (`frontend/src/app/core/services/room-allocation.service.ts`)
   - Full CRUD operations
   - Allocation by application
   - Eligibility checking
   - Availability checking
   - Search and filtering

---

## Components to Implement

### 1. Building Management (ADMIN) - `building-management/`

#### Files:
```
building-management/
├── building-management.component.ts ✅ CREATED
├── building-management.component.html ✅ CREATED
├── building-management.component.css ✅ CREATED
└── building-form-dialog/
    ├── building-form-dialog.component.ts ⏳ NEEDED
    ├── building-form-dialog.component.html ⏳ NEEDED
    └── building-form-dialog.component.css ⏳ NEEDED
```

#### Features:
- ✅ List all buildings with pagination
- ✅ Search/filter buildings
- ⏳ Create building dialog
- ⏳ Edit building dialog
- ✅ Delete building with confirmation
- Display: Name, Address, Total Floors, Created Date

---

### 2. Room Management (ADMIN) - `room-management/`

#### Files Needed:
```
room-management/
├── room-management.component.ts
├── room-management.component.html
├── room-management.component.css
└── room-form-dialog/
    ├── room-form-dialog.component.ts
    ├── room-form-dialog.component.html
    └── room-form-dialog.component.css
```

#### Features:
- List all rooms with pagination
- Search/filter by: Room Number, Building, Capacity, Status
- Create room dialog with building selection
- Edit room dialog
- Delete room with confirmation
- Display: Room Number, Building, Capacity, Occupancy, Status

#### Room Statuses:
- AVAILABLE (green)
- OCCUPIED (yellow)
- FULL (red)
- MAINTENANCE (gray)

---

### 3. Room Browser (STUDENT) - `rooms/`

#### Files Needed:
```
rooms/
├── room-browser/
│   ├── room-browser.component.ts
│   ├── room-browser.component.html
│   └── room-browser.component.css
└── room-details-dialog/
    ├── room-details-dialog.component.ts
    ├── room-details-dialog.component.html
    └── room-details-dialog.component.css
```

#### Features:
- Card/Grid view of available rooms
- Filter by: Building, Capacity, Status
- Search by room number
- View room details in dialog
- Show: Room Number, Building, Capacity, Available Spots, Status
- Visual indicators for availability
- Responsive card layout

---

### 4. Room Allocation Interface (ADMIN) - `room-allocation/`

#### Files Needed:
```
room-allocation/
├── room-allocation.component.ts
├── room-allocation.component.html
├── room-allocation.component.css
├── allocate-dialog/
│   ├── allocate-dialog.component.ts
│   ├── allocate-dialog.component.html
│   └── allocate-dialog.component.css
└── allocation-details-dialog/
    ├── allocation-details-dialog.component.ts
    ├── allocation-details-dialog.component.html
    └── allocation-details-dialog.component.css
```

#### Features:
- List all allocations with pagination
- Search/filter by: Student, Room, Building, Status, Date Range
- Allocate room to student with validation
- Allocate by approved application
- Check student eligibility before allocation
- Check room availability
- End allocation (set to COMPLETED)
- View allocation details
- Status management (ACTIVE, COMPLETED, CANCELLED)
- Display: Student Name, Room, Building, Allocation Date, End Date, Status

#### Business Rules:
- Student must have approved application
- Student cannot have active allocation
- Room must have available capacity
- Auto-updates room occupancy

---

## Routing Structure

```typescript
// Admin routes
{
  path: 'admin',
  canActivate: [AuthGuard, AdminGuard],
  children: [
    { path: 'buildings', component: BuildingManagementComponent },
    { path: 'rooms', component: RoomManagementComponent },
    { path: 'room-allocations', component: RoomAllocationComponent }
  ]
}

// Student routes
{
  path: 'rooms',
  component: RoomBrowserComponent,
  canActivate: [AuthGuard, StudentGuard]
}
```

---

## Navigation Integration

### Admin Menu (app-nav-menu.component.ts):
Add to admin menu items:
```
- Buildings & Rooms (icon: apartment)
- Room Allocations (icon: meeting_room)
```

### Student Menu:
Add to student menu:
```
- Browse Rooms (icon: search)
```

---

## UI/UX Design Guidelines

### Colors:
- **Available**: Green (#388e3c)
- **Occupied**: Orange (#f57c00)
- **Full**: Red (#d32f2f)
- **Maintenance**: Gray (#757575)

### Icons:
- Buildings: `apartment`
- Rooms: `meeting_room`
- Allocations: `assignment_ind`
- Available: `check_circle`
- Occupied: `people`
- Full: `do_not_disturb`
- Maintenance: `build`

### Components:
- Tables: Material Table with sorting and pagination
- Dialogs: Material Dialog for forms
- Cards: Material Card for room browser
- Status: Material Chips with color coding
- Actions: Material Menu with icon buttons

---

## Data Flow

### Creating a Building:
1. Admin clicks "Add Building"
2. Dialog opens with form
3. Enter: Name (required), Address, Description, Total Floors
4. Submit → POST /api/building
5. Success → Close dialog, refresh list
6. Show success notification

### Creating a Room:
1. Admin clicks "Add Room"
2. Dialog opens with form
3. Select building from dropdown (loaded from API)
4. Enter: Room Number (required), Capacity (required), Status
5. Submit → POST /api/room
6. Success → Close dialog, refresh list
7. Auto-set currentOccupancy = 0

### Allocating a Room:
1. Admin clicks "Allocate Room"
2. Dialog shows:
   - Student selector (search/autocomplete)
   - Room selector (only available rooms)
   - Application selector (only approved)
3. Check eligibility: GET /api/room-allocation/student/{id}/eligibility
4. Check availability: GET /api/room-allocation/room/{id}/availability
5. If valid → POST /api/room-allocation/allocate-by-application
6. Success → Room occupancy updated, allocation created

### Student Room Browser:
1. Student navigates to /rooms
2. Load all available rooms: GET /api/room/search?status=AVAILABLE
3. Display in card grid
4. Click card → Open details dialog
5. Show: Room details, Building info, Capacity, Available spots
6. Can filter/search but cannot allocate (admin-only)

---

## Implementation Priority

### Phase 1: Admin CRUD (Highest Priority)
1. ✅ Building Management List
2. ⏳ Building Form Dialog
3. ⏳ Room Management List
4. ⏳ Room Form Dialog

### Phase 2: Allocation System
5. ⏳ Room Allocation List
6. ⏳ Allocate Dialog
7. ⏳ Eligibility & Availability Checks

### Phase 3: Student Features
8. ⏳ Room Browser
9. ⏳ Room Details Dialog

---

## Testing Checklist

### Building Management:
- [ ] Can create building
- [ ] Can edit building
- [ ] Can delete building (fails if has rooms)
- [ ] Search/filter works
- [ ] Pagination works
- [ ] Validation works (required fields)

### Room Management:
- [ ] Can create room
- [ ] Can edit room
- [ ] Can delete room (fails if has allocations)
- [ ] Building dropdown loads correctly
- [ ] Search/filter works
- [ ] Status updates correctly

### Room Allocation:
- [ ] Can allocate room to student
- [ ] Eligibility check prevents invalid allocations
- [ ] Availability check prevents overbooking
- [ ] Can allocate by application
- [ ] Can end allocation
- [ ] Room occupancy updates correctly
- [ ] Search/filter works

### Room Browser:
- [ ] Students can view rooms
- [ ] Filter by building works
- [ ] Filter by capacity works
- [ ] Room details dialog shows correctly
- [ ] Cannot allocate (admin-only)

---

## Quick Reference

### Status Values:

**Room Status:**
- AVAILABLE - Room has capacity
- OCCUPIED - Room partially filled
- FULL - Room at capacity
- MAINTENANCE - Room unavailable

**Allocation Status:**
- ACTIVE - Current allocation
- COMPLETED - Ended allocation
- CANCELLED - Cancelled allocation

### Capacity Logic:
```
availableSpots = capacity - currentOccupancy
isAvailable = currentOccupancy < capacity
status = currentOccupancy === 0 ? 'AVAILABLE' :
         currentOccupancy < capacity ? 'OCCUPIED' :
         'FULL'
```

---

## Next Steps

1. Complete building form dialog
2. Implement room management component
3. Implement room allocation component
4. Implement student room browser
5. Add to routing
6. Update navigation
7. Test all CRUD operations
8. Test business rules and validations

---

**Status:** Backend ✅ Complete | Frontend ⏳ In Progress
**Priority:** CRITICAL
**Estimated Time:** 2-3 days
