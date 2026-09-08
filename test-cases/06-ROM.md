# Room Management — UAT Test Cases

> **Module:** Room Management
> **Application URL:** http://localhost:4200
> **Admin Route:** /admin/rooms
> **API Base:** /api/room, /api/admin/rooms/{id}/attributes, /api/admin/room-attributes
> **Last Updated:** 2026-05-17

---

## Test Data Reference

| Item | Value |
|------|-------|
| **Platform Admin** | platformadmin@test.com / Admin@12345 |
| **Tenant Admin** | tenantadmin@test.com / Admin@12345 |
| **Building Alpha** | Residence Hall Alpha (id: {alphaId}) |
| **Building Beta** | Residence Hall Beta (id: {betaId}) |
| **Building Gamma** | Residence Hall Gamma (id: {gammaId}) |
| **Room A-101** | Building Alpha, capacity 2, R3 500, AVAILABLE |
| **Room A-102** | Building Alpha, capacity 1, R4 500, AVAILABLE |
| **Room B-201** | Building Beta, capacity 2, R3 000, AVAILABLE |
| **Room C-301** | Building Gamma, capacity 3, R2 500, AVAILABLE |
| **Room C-302** | Building Gamma, capacity 1, R4 000, MAINTENANCE |
| **Room Statuses** | AVAILABLE, OCCUPIED, FULL, MAINTENANCE |

---

## A. Admin — Create Single Room

### TC-ROM-001: Create room with all valid fields

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-001 |
| **Module** | Room Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com; "Residence Hall Alpha" exists; navigated to /admin/rooms |
| **Test Data** | roomNumber: "A-201", buildingId: {alphaId}, capacity: 2, status: "AVAILABLE", monthlyRate: 3500.00 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Room" button | Create room form/dialog opens |
| 2 | Enter roomNumber: "A-201" | Field populated |
| 3 | Select building: "Residence Hall Alpha" | Building dropdown shows Alpha |
| 4 | Enter capacity: 2 | Capacity field set |
| 5 | Select status: "AVAILABLE" | Status set |
| 6 | Enter monthlyRate: 3500.00 | Rate entered |
| 7 | Click "Save" / "Create" | Success toast; room "A-201" appears in room list under Residence Hall Alpha |

| Field | Value |
|-------|-------|
| **Postconditions** | Room A-201 exists with capacity 2, AVAILABLE status, R3 500 rate |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ROM-002: Create room with empty roomNumber — validation error

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-002 |
| **Module** | Room Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com; on create room form |
| **Test Data** | roomNumber: (empty), buildingId: {alphaId}, capacity: 1, status: "AVAILABLE" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open create room form | Form opens |
| 2 | Leave roomNumber empty | Field blank |
| 3 | Fill remaining fields: building Alpha, capacity 1, status AVAILABLE | Fields populated |
| 4 | Click "Save" / "Create" | Validation error: "Room number is required"; form not submitted |

| Field | Value |
|-------|-------|
| **Postconditions** | No room created |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | @NotBlank on roomNumber |

---

### TC-ROM-003: Create room without selecting building — error

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-003 |
| **Module** | Room Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com; on create room form |
| **Test Data** | roomNumber: "X-100", buildingId: (not selected), capacity: 1, status: "AVAILABLE" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open create room form | Form opens |
| 2 | Enter roomNumber: "X-100" | Field populated |
| 3 | Do NOT select any building | Building dropdown empty/unselected |
| 4 | Enter capacity: 1, status: AVAILABLE | Other fields set |
| 5 | Click "Save" / "Create" | Validation error requiring building selection; room not created |

| Field | Value |
|-------|-------|
| **Postconditions** | No room created |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | buildingId is required for room creation |

---

### TC-ROM-004: Create room with capacity = 0 — validation error

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-004 |
| **Module** | Room Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; on create room form |
| **Test Data** | roomNumber: "A-300", buildingId: {alphaId}, capacity: 0, status: "AVAILABLE" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open create room form | Form opens |
| 2 | Enter roomNumber: "A-300", select building Alpha | Fields populated |
| 3 | Enter capacity: 0 | Value entered |
| 4 | Click "Save" / "Create" | Validation error: "Capacity must be at least 1"; room not created |

| Field | Value |
|-------|-------|
| **Postconditions** | No room created |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | @Min(1) on capacity |

---

### TC-ROM-005: Create room with capacity = 1 — boundary success

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-005 |
| **Module** | Room Management |
| **Priority** | Medium |
| **Preconditions** | Logged in as platformadmin@test.com; on create room form |
| **Test Data** | roomNumber: "A-301", buildingId: {alphaId}, capacity: 1, status: "AVAILABLE", monthlyRate: 4500 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open create room form | Form opens |
| 2 | Enter roomNumber: "A-301", building: Alpha, capacity: 1, status: AVAILABLE, rate: 4500 | All fields populated |
| 3 | Click "Save" / "Create" | Room created successfully with capacity = 1 |

| Field | Value |
|-------|-------|
| **Postconditions** | Room A-301 exists with capacity 1 |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | @Min(1) boundary test |

---

### TC-ROM-006: Create room with negative monthlyRate — validation error

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-006 |
| **Module** | Room Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; on create room form |
| **Test Data** | roomNumber: "A-302", buildingId: {alphaId}, capacity: 1, monthlyRate: -500.00 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open create room form | Form opens |
| 2 | Enter roomNumber: "A-302", building: Alpha, capacity: 1 | Fields set |
| 3 | Enter monthlyRate: -500 | Negative value entered |
| 4 | Click "Save" / "Create" | Validation error: "Monthly rate must be zero or positive"; room not created |

| Field | Value |
|-------|-------|
| **Postconditions** | No room created |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | @Min(0) on monthlyRate |

---

### TC-ROM-007: Create room with monthlyRate = 0 — success

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-007 |
| **Module** | Room Management |
| **Priority** | Medium |
| **Preconditions** | Logged in as platformadmin@test.com; on create room form |
| **Test Data** | roomNumber: "A-303", buildingId: {alphaId}, capacity: 2, status: "AVAILABLE", monthlyRate: 0 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Enter all fields with monthlyRate: 0 | Fields populated |
| 2 | Click "Save" / "Create" | Room created successfully with R0.00 rate |

| Field | Value |
|-------|-------|
| **Postconditions** | Room A-303 exists with monthlyRate = 0 (free accommodation) |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | @Min(0) boundary — zero is valid |

---

### TC-ROM-008: Create room with monthlyRate = 3500 — typical value

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-008 |
| **Module** | Room Management |
| **Priority** | Low |
| **Preconditions** | Logged in as platformadmin@test.com; on create room form |
| **Test Data** | roomNumber: "A-304", buildingId: {alphaId}, capacity: 2, status: "AVAILABLE", monthlyRate: 3500.00 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Enter all fields with monthlyRate: 3500 | Fields populated |
| 2 | Click "Save" / "Create" | Room created; rate displays as R3 500.00 |

| Field | Value |
|-------|-------|
| **Postconditions** | Room exists with typical rate value |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ROM-009: Create room without status — error

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-009 |
| **Module** | Room Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; on create room form |
| **Test Data** | roomNumber: "A-305", buildingId: {alphaId}, capacity: 1, status: (not selected), monthlyRate: 2000 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Enter roomNumber, building, capacity, monthlyRate | Fields set |
| 2 | Do NOT select a status | Status field empty/unselected |
| 3 | Click "Save" / "Create" | Either: validation error requiring status, OR system defaults to AVAILABLE |
| 4 | Document observed behavior | Record whether status is required or defaulted |

| Field | Value |
|-------|-------|
| **Postconditions** | Document whether status is required or has a default |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | status field in RoomRequest has no @NotBlank; may default server-side |

---

### TC-ROM-010: Create room with each status value

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-010 |
| **Module** | Room Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; on create room form |
| **Test Data** | Four rooms: A-310 (AVAILABLE), A-311 (OCCUPIED), A-312 (FULL), A-313 (MAINTENANCE) in building Alpha |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Create room A-310 with status AVAILABLE | Room created; status badge shows "AVAILABLE" |
| 2 | Create room A-311 with status OCCUPIED | Room created; status badge shows "OCCUPIED" |
| 3 | Create room A-312 with status FULL | Room created; status badge shows "FULL" |
| 4 | Create room A-313 with status MAINTENANCE | Room created; status badge shows "MAINTENANCE" |
| 5 | Verify all 4 rooms visible in room list | All rooms displayed with correct status indicators |

| Field | Value |
|-------|-------|
| **Postconditions** | Four rooms exist with each possible status |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ROM-011: Create room with duplicate room number in SAME building — error

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-011 |
| **Module** | Room Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com; room "A-101" already exists in Residence Hall Alpha |
| **Test Data** | roomNumber: "A-101", buildingId: {alphaId}, capacity: 1, status: "AVAILABLE" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open create room form | Form opens |
| 2 | Enter roomNumber: "A-101", select building: Residence Hall Alpha | Fields set |
| 3 | Enter capacity: 1, status: AVAILABLE | Fields set |
| 4 | Click "Save" / "Create" | Error: duplicate room number in building; room not created |

| Field | Value |
|-------|-------|
| **Postconditions** | No duplicate room created |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Unique constraint on (buildingId, roomNumber) |

---

### TC-ROM-012: Create room with same room number in DIFFERENT building — success

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-012 |
| **Module** | Room Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; "A-101" exists in Alpha; Beta has no "A-101" |
| **Test Data** | roomNumber: "A-101", buildingId: {betaId}, capacity: 2, status: "AVAILABLE", monthlyRate: 3000 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open create room form | Form opens |
| 2 | Enter roomNumber: "A-101", select building: Residence Hall Beta | Fields set |
| 3 | Enter capacity: 2, status: AVAILABLE, rate: 3000 | Fields set |
| 4 | Click "Save" / "Create" | Room created successfully — same number allowed in different building |

| Field | Value |
|-------|-------|
| **Postconditions** | "A-101" exists in both Alpha and Beta |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Room number uniqueness is per-building, not global |

---

## B. Admin — Bulk Room Creation

### TC-ROM-013: Bulk create 5 rooms with prefix "R", start 101, capacity 2

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-013 |
| **Module** | Room Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com; Residence Hall Alpha exists; no rooms R101–R105 in Alpha |
| **Test Data** | buildingId: {alphaId}, prefix: "R", startNumber: 101, count: 5, capacity: 2, status: AVAILABLE, monthlyRate: 3000 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Bulk Create Rooms" button | Bulk creation form opens |
| 2 | Select building: Residence Hall Alpha | Building selected |
| 3 | Enter prefix: "R", startNumber: 101, count: 5 | Fields set |
| 4 | Enter capacity: 2, status: AVAILABLE, monthlyRate: 3000 | Fields set |
| 5 | Click "Create" | Success: 5 rooms created (R101, R102, R103, R104, R105) |
| 6 | Navigate to room list filtered by Alpha | All 5 rooms visible with correct data |

| Field | Value |
|-------|-------|
| **Postconditions** | 5 rooms R101–R105 exist in Alpha with capacity 2, R3 000 rate |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | API: POST /api/room/bulk with list of 5 RoomRequest objects |

---

### TC-ROM-014: Bulk create with count = 1

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-014 |
| **Module** | Room Management |
| **Priority** | Medium |
| **Preconditions** | Logged in as platformadmin@test.com; on bulk create form |
| **Test Data** | buildingId: {betaId}, prefix: "S", startNumber: 1, count: 1, capacity: 1 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open bulk create form | Form opens |
| 2 | Enter: building Beta, prefix "S", start 1, count 1, capacity 1 | Fields set |
| 3 | Click "Create" | 1 room created: "S1" in Beta |

| Field | Value |
|-------|-------|
| **Postconditions** | Room S1 exists in Beta |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Minimum count boundary |

---

### TC-ROM-015: Bulk create with count = 100 (maximum boundary)

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-015 |
| **Module** | Room Management |
| **Priority** | Medium |
| **Preconditions** | Logged in as platformadmin@test.com; Gamma has no rooms numbered T1–T100 |
| **Test Data** | buildingId: {gammaId}, prefix: "T", startNumber: 1, count: 100, capacity: 2 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open bulk create form | Form opens |
| 2 | Enter: building Gamma, prefix "T", start 1, count 100, capacity 2 | Fields set |
| 3 | Click "Create" | 100 rooms created (T1–T100); loading indicator shown during processing |
| 4 | Filter rooms by building Gamma | 100+ rooms displayed across paginated results |

| Field | Value |
|-------|-------|
| **Postconditions** | 100 rooms T1–T100 exist in Gamma |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Test system performance with large bulk creation |

---

### TC-ROM-016: Bulk create with count = 0 — validation error

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-016 |
| **Module** | Room Management |
| **Priority** | Medium |
| **Preconditions** | Logged in as platformadmin@test.com; on bulk create form |
| **Test Data** | count: 0 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open bulk create form | Form opens |
| 2 | Enter count: 0 (other fields valid) | Value entered |
| 3 | Click "Create" | Validation error: count must be at least 1; no rooms created |

| Field | Value |
|-------|-------|
| **Postconditions** | No rooms created |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ROM-017: Bulk create with count = 101 — validation error (exceeds max)

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-017 |
| **Module** | Room Management |
| **Priority** | Medium |
| **Preconditions** | Logged in as platformadmin@test.com; on bulk create form |
| **Test Data** | count: 101 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open bulk create form | Form opens |
| 2 | Enter count: 101 (other fields valid) | Value entered |
| 3 | Click "Create" | Validation error: count must not exceed 100; no rooms created |

| Field | Value |
|-------|-------|
| **Postconditions** | No rooms created |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Max 100 rooms per bulk operation |

---

### TC-ROM-018: Bulk create without selecting building — error

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-018 |
| **Module** | Room Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; on bulk create form |
| **Test Data** | buildingId: (not selected), prefix: "U", startNumber: 1, count: 5, capacity: 2 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open bulk create form | Form opens |
| 2 | Do NOT select a building | Building field empty |
| 3 | Fill other fields: prefix "U", start 1, count 5, capacity 2 | Fields set |
| 4 | Click "Create" | Validation error: building is required; no rooms created |

| Field | Value |
|-------|-------|
| **Postconditions** | No rooms created |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ROM-019: Bulk create with startNumber = 0 — error

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-019 |
| **Module** | Room Management |
| **Priority** | Medium |
| **Preconditions** | Logged in as platformadmin@test.com; on bulk create form |
| **Test Data** | startNumber: 0 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open bulk create form | Form opens |
| 2 | Enter startNumber: 0 with other valid fields | Value entered |
| 3 | Click "Create" | Validation error: start number must be at least 1; no rooms created |

| Field | Value |
|-------|-------|
| **Postconditions** | No rooms created |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ROM-020: Bulk create with capacity = 0 — error

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-020 |
| **Module** | Room Management |
| **Priority** | Medium |
| **Preconditions** | Logged in as platformadmin@test.com; on bulk create form |
| **Test Data** | capacity: 0 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open bulk create form | Form opens |
| 2 | Enter capacity: 0 with other valid fields | Value entered |
| 3 | Click "Create" | Validation error: "Capacity must be at least 1"; no rooms created |

| Field | Value |
|-------|-------|
| **Postconditions** | No rooms created |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | @Min(1) on capacity in each RoomRequest |

---

### TC-ROM-021: Bulk create with no prefix — rooms numbered without prefix

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-021 |
| **Module** | Room Management |
| **Priority** | Medium |
| **Preconditions** | Logged in as platformadmin@test.com; Beta has no rooms "501"–"503" |
| **Test Data** | buildingId: {betaId}, prefix: (empty), startNumber: 501, count: 3, capacity: 2 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open bulk create form | Form opens |
| 2 | Select building Beta, leave prefix empty, start: 501, count: 3, capacity: 2 | Fields set |
| 3 | Click "Create" | 3 rooms created: "501", "502", "503" (no prefix) |
| 4 | Verify room numbers in list | Rooms show as "501", "502", "503" |

| Field | Value |
|-------|-------|
| **Postconditions** | 3 rooms with numeric-only names in Beta |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Prefix is optional |

---

### TC-ROM-022: Bulk create where some numbers already exist — conflict handling

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-022 |
| **Module** | Room Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; room "A-101" already exists in Alpha |
| **Test Data** | buildingId: {alphaId}, prefix: "A-", startNumber: 100, count: 5, capacity: 2 (would create A-100, A-101, A-102, A-103, A-104) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open bulk create form | Form opens |
| 2 | Enter: building Alpha, prefix "A-", start 100, count 5, capacity 2 | Fields set |
| 3 | Click "Create" | Either: (a) entire batch rejected with duplicate error, OR (b) partial success with error for A-101 only |
| 4 | Document observed behavior | Record whether it's all-or-nothing or partial |

| Field | Value |
|-------|-------|
| **Postconditions** | Document behavior for duplicate conflicts in bulk operations |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Important: record whether bulk is transactional (all-or-nothing) or partial |

---

## C. Admin — Update Room

### TC-ROM-023: Update room number

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-023 |
| **Module** | Room Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; room "A-101" exists in Alpha |
| **Test Data** | Existing: "A-101", New roomNumber: "A-101A" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/rooms | Room list displayed |
| 2 | Click edit on room "A-101" (Alpha) | Edit form opens pre-populated |
| 3 | Change roomNumber to "A-101A" | Field updated |
| 4 | Click "Save" / "Update" | Success; room list shows "A-101A" |

| Field | Value |
|-------|-------|
| **Postconditions** | Room number changed to "A-101A" |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ROM-024: Update capacity

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-024 |
| **Module** | Room Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; room "A-102" exists in Alpha with capacity 1 |
| **Test Data** | Existing capacity: 1, New capacity: 3 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click edit on room "A-102" | Edit form opens showing capacity: 1 |
| 2 | Change capacity to 3 | Capacity updated |
| 3 | Click "Save" / "Update" | Success; room shows capacity 3 |

| Field | Value |
|-------|-------|
| **Postconditions** | Room A-102 capacity is 3 |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ROM-025: Update monthly rate

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-025 |
| **Module** | Room Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; room "B-201" exists in Beta with rate R3 000 |
| **Test Data** | Existing rate: 3000, New rate: 3500 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click edit on room "B-201" | Edit form opens showing rate: 3000 |
| 2 | Change monthlyRate to 3500 | Rate updated |
| 3 | Click "Save" / "Update" | Success; room shows R3 500.00 |

| Field | Value |
|-------|-------|
| **Postconditions** | Room B-201 rate is R3 500 |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ROM-026: Update status to MAINTENANCE

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-026 |
| **Module** | Room Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; room "B-201" exists with status AVAILABLE |
| **Test Data** | Existing status: AVAILABLE, New status: MAINTENANCE |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click edit on room "B-201" | Edit form opens showing status: AVAILABLE |
| 2 | Change status to "MAINTENANCE" | Status dropdown changed |
| 3 | Click "Save" / "Update" | Success; room status badge shows "MAINTENANCE" |
| 4 | Verify room no longer appears when filtering by "onlyAvailable" | Room excluded from available-only search |

| Field | Value |
|-------|-------|
| **Postconditions** | Room B-201 is in MAINTENANCE status |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ROM-027: Update to duplicate room number in same building — error

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-027 |
| **Module** | Room Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; rooms "A-101" and "A-102" both exist in Alpha |
| **Test Data** | Edit "A-102", change roomNumber to "A-101" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click edit on room "A-102" (Alpha) | Edit form opens |
| 2 | Change roomNumber to "A-101" | Field updated |
| 3 | Click "Save" / "Update" | Error: room number already exists in this building; update rejected |

| Field | Value |
|-------|-------|
| **Postconditions** | Room "A-102" unchanged |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## D. Admin — Delete Room

### TC-ROM-028: Delete room with no active allocations — success

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-028 |
| **Module** | Room Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com; room "A-301" exists in Alpha with no allocations |
| **Test Data** | Room: A-301 (Alpha) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/rooms | Room list displayed |
| 2 | Click delete on room "A-301" | Confirmation dialog appears |
| 3 | Confirm deletion | Success; room removed from list |
| 4 | Refresh page | Room no longer visible |

| Field | Value |
|-------|-------|
| **Postconditions** | Room deleted from system |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ROM-029: Delete room with active allocation — verify behavior

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-029 |
| **Module** | Room Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com; room "A-101" has an active room allocation with a student |
| **Test Data** | Room: A-101 (Alpha) with active allocation |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click delete on room "A-101" | Confirmation dialog appears |
| 2 | Confirm deletion | Either: (a) error stating room has active allocations and cannot be deleted, OR (b) cascade: allocation also removed |
| 3 | Document observed behavior | Record whether blocked or cascaded |

| Field | Value |
|-------|-------|
| **Postconditions** | Document system behavior for rooms with active allocations |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Record which behavior: FK restriction or cascade delete |

---

## E. Admin — Room Attributes

### TC-ROM-030: Set accessibilityCompliant = true

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-030 |
| **Module** | Room Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; room "A-101" exists in Alpha |
| **Test Data** | roomId: {a101Id}, accessibilityCompliant: true |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to room attributes for "A-101" | Room attributes form/section displayed |
| 2 | Toggle accessibilityCompliant to true/checked | Checkbox enabled |
| 3 | Click "Save" | API: POST /api/admin/room-attributes; success response |
| 4 | Reload and verify | accessibilityCompliant shows true |

| Field | Value |
|-------|-------|
| **Postconditions** | Room A-101 marked as accessibility compliant |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ROM-031: Set genderRestriction = FEMALE_ONLY

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-031 |
| **Module** | Room Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; room "B-201" exists in Beta |
| **Test Data** | roomId: {b201Id}, genderRestriction: FEMALE_ONLY |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to room attributes for "B-201" | Attributes form displayed |
| 2 | Set genderRestriction to "FEMALE_ONLY" | Dropdown changed |
| 3 | Click "Save" | Success; attribute saved |
| 4 | Verify via GET /api/admin/room-attributes/room/{b201Id} | genderRestriction: "FEMALE_ONLY" |

| Field | Value |
|-------|-------|
| **Postconditions** | Room B-201 restricted to FEMALE_ONLY |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Room-level override of building-level restriction |

---

### TC-ROM-032: Set minYearOfStudy = 1, maxYearOfStudy = 4

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-032 |
| **Module** | Room Management |
| **Priority** | Medium |
| **Preconditions** | Logged in as platformadmin@test.com; room "C-301" exists in Gamma |
| **Test Data** | roomId: {c301Id}, minYearOfStudy: 1, maxYearOfStudy: 4 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to room attributes for "C-301" | Attributes form displayed |
| 2 | Enter minYearOfStudy: 1 | Field set |
| 3 | Enter maxYearOfStudy: 4 | Field set |
| 4 | Click "Save" | Success; attributes saved |
| 5 | Verify persisted values | min: 1, max: 4 confirmed |

| Field | Value |
|-------|-------|
| **Postconditions** | Room C-301 restricted to students in years 1–4 |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | @Min(1) @Max(10) on both fields |

---

### TC-ROM-033: Set minYearOfStudy > maxYearOfStudy — validation behavior

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-033 |
| **Module** | Room Management |
| **Priority** | Medium |
| **Preconditions** | Logged in as platformadmin@test.com; room "C-301" exists |
| **Test Data** | roomId: {c301Id}, minYearOfStudy: 5, maxYearOfStudy: 2 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to room attributes for "C-301" | Attributes form displayed |
| 2 | Enter minYearOfStudy: 5 | Field set |
| 3 | Enter maxYearOfStudy: 2 | Field set |
| 4 | Click "Save" | Either: (a) validation error "min must be <= max", OR (b) saved without cross-field validation |
| 5 | Document observed behavior | Record if cross-field validation exists |

| Field | Value |
|-------|-------|
| **Postconditions** | Document whether cross-field min/max validation is enforced |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | No explicit cross-field validator in RoomAttributesRequest — may be a gap |

---

### TC-ROM-034: Bulk set attributes for multiple rooms in same building

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-034 |
| **Module** | Room Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; rooms C-301 and C-302 exist in Gamma |
| **Test Data** | Two RoomAttributesRequests: [{roomId: c301Id, accessibilityCompliant: true, genderRestriction: MALE_ONLY}, {roomId: c302Id, accessibilityCompliant: false, genderRestriction: ANY}] |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to room attributes bulk management for Gamma | Bulk form or select-multiple UI displayed |
| 2 | Select rooms C-301 and C-302 | Both rooms selected |
| 3 | Set attributes for each room as per test data | Attributes configured |
| 4 | Click "Save All" / submit bulk | API: POST /api/admin/room-attributes/bulk; success for both |
| 5 | Verify via GET /api/admin/room-attributes/building/{gammaId} | Both rooms show correct individual attributes |

| Field | Value |
|-------|-------|
| **Postconditions** | C-301: accessible, MALE_ONLY; C-302: not accessible, ANY |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Uses POST /api/admin/room-attributes/bulk endpoint |

---

## F. Admin — Search & Filter

### TC-ROM-035: Search by room number

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-035 |
| **Module** | Room Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; on /admin/rooms; multiple rooms exist |
| **Test Data** | Search: "A-101" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/rooms | Room list displayed |
| 2 | Enter "A-101" in the room number search field | Filter applied |
| 3 | Verify results | Only rooms with "A-101" in room number displayed (may appear in multiple buildings) |

| Field | Value |
|-------|-------|
| **Postconditions** | Search correctly filters by room number |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | API: GET /api/room/search?roomNumber=A-101 |

---

### TC-ROM-036: Filter by building

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-036 |
| **Module** | Room Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; on /admin/rooms |
| **Test Data** | Filter: building = Residence Hall Beta |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/rooms | All rooms listed |
| 2 | Select "Residence Hall Beta" from building filter dropdown | Filter applied |
| 3 | Verify results | Only rooms belonging to Beta displayed (B-201, etc.) |

| Field | Value |
|-------|-------|
| **Postconditions** | Rooms filtered to single building |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | API: GET /api/room/search?buildingId={betaId} |

---

### TC-ROM-037: Filter by capacity

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-037 |
| **Module** | Room Management |
| **Priority** | Medium |
| **Preconditions** | Logged in as platformadmin@test.com; rooms with varying capacities exist |
| **Test Data** | minCapacity: 2, maxCapacity: 3 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/rooms | Room list displayed |
| 2 | Set min capacity filter to 2 | Filter applied |
| 3 | Set max capacity filter to 3 | Filter applied |
| 4 | Verify results | Only rooms with capacity 2 or 3 displayed; single-occupancy rooms excluded |

| Field | Value |
|-------|-------|
| **Postconditions** | Rooms filtered by capacity range |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | API: GET /api/room/search?minCapacity=2&maxCapacity=3 |

---

### TC-ROM-038: Filter by availability status (onlyAvailable)

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-038 |
| **Module** | Room Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; rooms exist with AVAILABLE, MAINTENANCE, OCCUPIED statuses |
| **Test Data** | onlyAvailable: true |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/rooms | All rooms listed |
| 2 | Toggle "Available only" filter on | Filter applied |
| 3 | Verify results | Only rooms with AVAILABLE status shown; MAINTENANCE (C-302), OCCUPIED, and FULL rooms excluded |

| Field | Value |
|-------|-------|
| **Postconditions** | Only available rooms displayed |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | API: GET /api/room/search?onlyAvailable=true |

---

### TC-ROM-039: Empty search results — empty state

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-039 |
| **Module** | Room Management |
| **Priority** | Medium |
| **Preconditions** | Logged in as platformadmin@test.com; on /admin/rooms |
| **Test Data** | Search: "ZZZ-NONEXISTENT-999" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Enter "ZZZ-NONEXISTENT-999" in search field | Filter applied |
| 2 | Verify results area | Empty state message displayed (e.g., "No rooms found") |
| 3 | Verify no crash or error | Page remains functional; filters can be cleared |

| Field | Value |
|-------|-------|
| **Postconditions** | Empty state shown gracefully |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## G. Pagination

### TC-ROM-040: Navigate through paginated room list

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-040 |
| **Module** | Room Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; more than 20 rooms exist in the system |
| **Test Data** | Default page size: 20 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/rooms | First page of rooms displayed (up to 20 rooms) |
| 2 | Verify pagination controls visible | Page number, next/prev buttons shown |
| 3 | Click "Next" page | Second page loads with different rooms |
| 4 | Click "Previous" page | First page loads again with original rooms |
| 5 | Click on a specific page number (e.g., page 3) | Correct page loads |

| Field | Value |
|-------|-------|
| **Postconditions** | Pagination works correctly across all pages |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Default page size is 20, sorted by roomNumber |

---

### TC-ROM-041: Change page size

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-041 |
| **Module** | Room Management |
| **Priority** | Medium |
| **Preconditions** | Logged in as platformadmin@test.com; more than 50 rooms exist |
| **Test Data** | Change page size from 20 to 50 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/rooms | Default 20 rooms per page |
| 2 | Change page size selector to 50 | Page reloads with up to 50 rooms displayed |
| 3 | Verify row count | Up to 50 rooms shown on single page |
| 4 | Verify pagination updates | Total pages reduced accordingly |

| Field | Value |
|-------|-------|
| **Postconditions** | Page size change reflected in display and pagination |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## H. Access Control

### TC-ROM-042: Student cannot access /admin/rooms

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-042 |
| **Module** | Room Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as a student user (not admin) |
| **Test Data** | Student account credentials |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as student user | Student dashboard loads |
| 2 | Manually navigate to http://localhost:4200/admin/rooms | Redirected to student dashboard or 403 page |
| 3 | Attempt direct API: POST /api/room with valid body and student JWT | 403 Forbidden response |
| 4 | Attempt direct API: DELETE /api/room/{id} with student JWT | 403 Forbidden response |

| Field | Value |
|-------|-------|
| **Postconditions** | Student has no access to admin room management |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')") on all mutating endpoints |

---

### TC-ROM-043: Tenant admin sees only their tenant's rooms

| Field | Value |
|-------|-------|
| **ID** | TC-ROM-043 |
| **Module** | Room Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as tenantadmin@test.com; tenant has buildings Alpha (with rooms A-101, A-102); other tenant has Gamma (with rooms C-301, C-302) |
| **Test Data** | Tenant admin's tenant owns Alpha; another tenant owns Gamma |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as tenantadmin@test.com | Admin dashboard loads |
| 2 | Navigate to /admin/rooms | Room list displayed |
| 3 | Verify only rooms from tenant's buildings are shown | A-101, A-102 visible; C-301, C-302 NOT visible |
| 4 | Attempt direct API: GET /api/room/{c301Id} with tenant admin JWT | Either 403 or 404 (tenant filter excludes it) |
| 5 | Attempt direct API: PUT /api/room/{c301Id} with tenant admin JWT | Either 403 or 404 |

| Field | Value |
|-------|-------|
| **Postconditions** | Multi-tenancy isolation confirmed for room management |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Hibernate tenantFilter scopes all queries by tenant_id |
