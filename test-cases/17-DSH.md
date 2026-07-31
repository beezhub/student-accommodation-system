# Admin Dashboard — UAT Test Cases

> **Module:** Admin Dashboard  
> **Routes:** `/admin` (Frontend), `/api/admin/dashboard/stats` & `/api/admin/dashboard/occupancy` (Backend)  
> **Guard:** AdminGuard  
> **Total Test Cases:** 12

---

### TC-DSH-001: Dashboard loads with summary statistics

| Field | Value |
|-------|-------|
| **ID** | TC-DSH-001 |
| **Module** | Admin Dashboard |
| **Priority** | Critical |
| **Preconditions** | Admin user logged in; data exists in the system (applications, buildings, rooms, students) |
| **Test Data** | Login: platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as platform admin | Login succeeds |
| 2 | Navigate to `/admin` | Admin Dashboard page loads |
| 3 | Observe the dashboard content | Summary statistics cards are displayed: total applications, pending/approved/rejected/in-review/allocated applications, total documents, total students, total admins, total rooms (available/occupied/full/maintenance), total buildings |
| 4 | Verify quick links section | Quick links are visible: Application Review, Document Verification, Room Allocations, Building Management, Waiting List, System Settings |
| 5 | Verify no loading errors | No error messages; data loads successfully (skeleton loaders disappear) |

| Field | Value |
|-------|-------|
| **Postconditions** | Dashboard displays all summary statistics and quick links |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DSH-002: Verify total applications count is correct

| Field | Value |
|-------|-------|
| **ID** | TC-DSH-002 |
| **Module** | Admin Dashboard |
| **Priority** | Critical |
| **Preconditions** | Admin logged in; applications exist in the system |
| **Test Data** | Login: platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin` | Dashboard loads |
| 2 | Note the "Total Applications" count displayed | Count recorded (e.g., 25) |
| 3 | Navigate to `/admin/applications` | Applications list page loads |
| 4 | Count the total number of applications (check pagination total if paginated) | Total count from the applications list |
| 5 | Compare the dashboard count with the applications list total | Both counts match |
| 6 | Verify via API: `GET /api/admin/dashboard/stats` | `totalApplications` field matches the displayed count |

| Field | Value |
|-------|-------|
| **Postconditions** | Total applications count on dashboard matches actual data |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DSH-003: Verify applications by status breakdown

| Field | Value |
|-------|-------|
| **ID** | TC-DSH-003 |
| **Module** | Admin Dashboard |
| **Priority** | Critical |
| **Preconditions** | Admin logged in; applications in various statuses exist |
| **Test Data** | Login: platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin` | Dashboard loads |
| 2 | Observe the application status breakdown | Counts are displayed for: Pending, Approved, Rejected, In Review (Awaiting), Allocated |
| 3 | Verify the donut chart segments match the counts | Chart segments correspond to the status counts; colors match the legend (Pending=orange, Approved=green, Awaiting=blue, Allocated=teal, Rejected=red) |
| 4 | Sum all status counts | Sum equals the total applications count |
| 5 | Cross-reference by navigating to `/admin/applications` and filtering by each status | Each filtered count matches the dashboard breakdown |

| Field | Value |
|-------|-------|
| **Postconditions** | Application status breakdown is accurate and sums to total |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DSH-004: Verify total buildings count

| Field | Value |
|-------|-------|
| **ID** | TC-DSH-004 |
| **Module** | Admin Dashboard |
| **Priority** | High |
| **Preconditions** | Admin logged in; buildings exist in the system |
| **Test Data** | Login: platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin` | Dashboard loads |
| 2 | Note the "Total Buildings" count | Count recorded |
| 3 | Navigate to `/admin/buildings` | Buildings list page loads |
| 4 | Count the total buildings in the list | Total from buildings page |
| 5 | Compare dashboard count with the buildings list | Both counts match |

| Field | Value |
|-------|-------|
| **Postconditions** | Total buildings count on dashboard is accurate |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DSH-005: Verify total rooms count

| Field | Value |
|-------|-------|
| **ID** | TC-DSH-005 |
| **Module** | Admin Dashboard |
| **Priority** | High |
| **Preconditions** | Admin logged in; rooms exist in the system |
| **Test Data** | Login: platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin` | Dashboard loads |
| 2 | Note the "Total Rooms" count and the room status breakdown (Available, Occupied, Full, Maintenance) | Counts recorded |
| 3 | Verify the room donut chart segments match the breakdown | Chart segments correspond to room status counts (Available=green, Occupied=blue, Full=red, Maintenance=grey) |
| 4 | Sum all room status counts | Sum equals total rooms count |
| 5 | Navigate to room management and cross-reference | Counts match |

| Field | Value |
|-------|-------|
| **Postconditions** | Total rooms count and status breakdown are accurate |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DSH-006: Verify occupancy rate percentage

| Field | Value |
|-------|-------|
| **ID** | TC-DSH-006 |
| **Module** | Admin Dashboard |
| **Priority** | High |
| **Preconditions** | Admin logged in; rooms and allocations exist |
| **Test Data** | Login: platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin` | Dashboard loads |
| 2 | Note the overall occupancy rate percentage | Percentage recorded (e.g., 65%) |
| 3 | Note totalCapacity and totalOccupancy values | Values recorded |
| 4 | Calculate: (totalOccupancy / totalCapacity) × 100 | Calculated percentage matches the displayed occupancy rate |
| 5 | Verify via API: `GET /api/admin/dashboard/stats` | `overallOccupancyRate` matches the displayed value |

| Field | Value |
|-------|-------|
| **Postconditions** | Occupancy rate is correctly calculated and displayed |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DSH-007: Verify occupancy statistics endpoint data

| Field | Value |
|-------|-------|
| **ID** | TC-DSH-007 |
| **Module** | Admin Dashboard |
| **Priority** | High |
| **Preconditions** | Admin logged in; buildings with rooms and allocations exist |
| **Test Data** | Login: platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin` | Dashboard loads |
| 2 | Scroll to the occupancy statistics section | Building occupancy table is displayed with columns: Building Name, Room Count, Capacity, Occupancy, Rate |
| 3 | Verify each building row shows: building name, number of rooms, total capacity, current occupancy, occupancy rate percentage | All fields are populated for each building |
| 4 | Verify per-building occupancy rate = (currentOccupancy / totalCapacity) × 100 | Calculation is correct for each building |
| 5 | Verify occupancy rate color coding | >= 90% shows warn/red, >= 70% shows accent/yellow, < 70% shows primary/green |
| 6 | Verify via API: `GET /api/admin/dashboard/occupancy` | Response `buildingOccupancies` array matches the displayed table data |

| Field | Value |
|-------|-------|
| **Postconditions** | Occupancy statistics are correct per building with proper color coding |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DSH-008: Stats update after creating a new application

| Field | Value |
|-------|-------|
| **ID** | TC-DSH-008 |
| **Module** | Admin Dashboard |
| **Priority** | High |
| **Preconditions** | Admin logged in; current dashboard stats noted; a student can submit applications |
| **Test Data** | Login: platformadmin@test.com / Admin@12345; Student: a registered student account |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin` and note the current total applications count and pending count | Counts recorded (e.g., total=25, pending=5) |
| 2 | In a separate session, log in as a student and submit a new application | Application submitted successfully |
| 3 | Return to the admin dashboard and refresh the page | Dashboard reloads |
| 4 | Verify the total applications count increased by 1 | New total = previous total + 1 (e.g., 26) |
| 5 | Verify the pending applications count increased by 1 | New pending = previous pending + 1 (e.g., 6) |

| Field | Value |
|-------|-------|
| **Postconditions** | Dashboard stats reflect the newly created application |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Stats update on page refresh, not real-time |

---

### TC-DSH-009: Stats update after allocating a room

| Field | Value |
|-------|-------|
| **ID** | TC-DSH-009 |
| **Module** | Admin Dashboard |
| **Priority** | High |
| **Preconditions** | Admin logged in; an approved application exists; an available room exists; current dashboard stats noted |
| **Test Data** | Login: platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin` and note: allocated applications count, available rooms count, occupied rooms count, occupancy rate | Counts recorded |
| 2 | Navigate to `/admin/room-allocations` and allocate a room to an approved application | Allocation succeeds |
| 3 | Return to `/admin` and refresh the page | Dashboard reloads |
| 4 | Verify allocated applications count increased by 1 | Count incremented |
| 5 | Verify available rooms count decreased (or occupied increased) | Room counts reflect the allocation |
| 6 | Verify occupancy rate updated | Rate recalculated with new occupancy |

| Field | Value |
|-------|-------|
| **Postconditions** | Dashboard stats reflect the new room allocation |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DSH-010: Tenant admin sees only their tenant's stats

| Field | Value |
|-------|-------|
| **ID** | TC-DSH-010 |
| **Module** | Admin Dashboard |
| **Priority** | Critical |
| **Preconditions** | Both platform admin and tenant admin logged in separately; data exists for multiple tenants |
| **Test Data** | Platform admin: platformadmin@test.com / Admin@12345; Tenant admin: tenantadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as platform admin and navigate to `/admin` | Dashboard loads with platform-wide stats |
| 2 | Note the total applications, buildings, rooms counts | Platform-wide counts recorded |
| 3 | Log out, log in as tenant admin | Login succeeds |
| 4 | Navigate to `/admin` | Dashboard loads |
| 5 | Note the total applications, buildings, rooms counts | Tenant-specific counts recorded |
| 6 | Compare counts | Tenant admin's counts are <= platform admin's counts (tenant sees only their own data due to Hibernate tenant filter) |
| 7 | Verify via API: `GET /api/admin/dashboard/stats` with each user's JWT | Responses differ; tenant admin's response is scoped to their tenant |

| Field | Value |
|-------|-------|
| **Postconditions** | Tenant admin dashboard shows tenant-scoped statistics only |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DSH-011: Dashboard loads within reasonable time (<3 seconds)

| Field | Value |
|-------|-------|
| **ID** | TC-DSH-011 |
| **Module** | Admin Dashboard |
| **Priority** | Medium |
| **Preconditions** | Admin logged in; system has a typical data volume |
| **Test Data** | Login: platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open browser developer tools → Network tab | Network tab ready |
| 2 | Navigate to `/admin` | Dashboard loads |
| 3 | Observe the network requests for dashboard stats and occupancy stats | Both API calls complete |
| 4 | Check the total load time from navigation to full render (skeleton loaders disappear) | Page fully renders within 3 seconds |
| 5 | Check individual API response times | `/api/admin/dashboard/stats` responds in < 2 seconds; `/api/admin/dashboard/occupancy` responds in < 2 seconds |

| Field | Value |
|-------|-------|
| **Postconditions** | Dashboard performance is within acceptable thresholds |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Record actual load times for baseline |

---

### TC-DSH-012: Student cannot access /admin

| Field | Value |
|-------|-------|
| **ID** | TC-DSH-012 |
| **Module** | Admin Dashboard |
| **Priority** | High |
| **Preconditions** | Student user logged in |
| **Test Data** | A registered student account with valid credentials |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as a student | Login succeeds; redirected to student dashboard |
| 2 | Manually navigate to `http://localhost:4200/admin` | User is redirected away from the admin dashboard (to student dashboard or login) |
| 3 | Verify the admin dashboard content is NOT displayed | No admin statistics or quick links are visible; AdminGuard blocks access |

| Field | Value |
|-------|-------|
| **Postconditions** | Student cannot access the admin dashboard |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |
