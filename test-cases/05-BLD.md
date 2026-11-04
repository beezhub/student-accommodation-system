# Building Management — UAT Test Cases

> **Module:** Building Management
> **Application URL:** http://localhost:4200
> **Admin Route:** /admin/buildings
> **Public Routes:** /buildings (browse), /buildings/:id (detail)
> **API Base:** /api/building, /api/admin/buildings/{id}/attributes
> **Last Updated:** 2026-05-17

---

## Test Data Reference

| Item | Value |
|------|-------|
| **Platform Admin** | platformadmin@test.com / Admin@12345 |
| **Tenant Admin** | tenantadmin@test.com / Admin@12345 |
| **Institutions** | University of Cape Town, Stellenbosch University, Wits University |
| **Building Alpha** | Residence Hall Alpha, 123 Main Street, 5 floors, UCT, ANY gender, accessible |
| **Building Beta** | Residence Hall Beta, 456 Oak Avenue, 3 floors, Stellenbosch, FEMALE_ONLY |
| **Building Gamma** | Residence Hall Gamma, 789 Pine Road, 8 floors, Wits, MALE_ONLY, accessible |

---

## A. Admin — Create Building

### TC-BLD-001: Create building with all valid fields

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-001 |
| **Module** | Building Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com; navigated to /admin/buildings |
| **Test Data** | Name: "Residence Hall Delta", Address: "100 University Ave, Cape Town", totalFloors: 4, Description: "New residence for 2026 intake", Institutions: [University of Cape Town] |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click the "Add Building" button | Create building dialog/form opens |
| 2 | Enter name: "Residence Hall Delta" | Name field populated |
| 3 | Type "100 University Ave" in address field and select Nominatim autocomplete suggestion | Address, placeId, latitude, and longitude fields auto-populated |
| 4 | Enter totalFloors: 4 | Floors field shows 4 |
| 5 | Enter description: "New residence for 2026 intake" | Description textarea populated |
| 6 | Select "University of Cape Town" from institution multi-select | Institution chip/tag appears |
| 7 | Click "Save" / "Create" button | Success toast shown; building "Residence Hall Delta" appears in the buildings list |

| Field | Value |
|-------|-------|
| **Postconditions** | Building "Residence Hall Delta" exists in the system with all entered data |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-BLD-002: Create building with empty name — validation error

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-002 |
| **Module** | Building Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com; on create building form |
| **Test Data** | Name: (empty), Address: "50 Test Road", totalFloors: 2 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open the create building form | Form opens |
| 2 | Leave the name field empty | Name field is blank |
| 3 | Fill in address: "50 Test Road", totalFloors: 2 | Other fields populated |
| 4 | Click "Save" / "Create" button | Validation error displayed: "Building name is required"; form is not submitted |

| Field | Value |
|-------|-------|
| **Postconditions** | No building created; form remains open with error message |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Backend returns @NotBlank validation |

---

### TC-BLD-003: Create building with duplicate name — unique constraint error

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-003 |
| **Module** | Building Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; "Residence Hall Alpha" already exists |
| **Test Data** | Name: "Residence Hall Alpha", Address: "999 New Road", totalFloors: 3 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open the create building form | Form opens |
| 2 | Enter name: "Residence Hall Alpha" | Name field populated with existing building name |
| 3 | Enter address: "999 New Road", totalFloors: 3 | Fields populated |
| 4 | Click "Save" / "Create" | Error displayed indicating building name already exists (unique constraint violation) |

| Field | Value |
|-------|-------|
| **Postconditions** | No duplicate building created |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-BLD-004: Create building — Nominatim autocomplete populates lat/long

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-004 |
| **Module** | Building Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; on create building form; internet connection available |
| **Test Data** | Address search: "123 Main Street, Cape Town" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open the create building form | Form opens |
| 2 | Begin typing "123 Main Street, Cape Town" in the address field | Nominatim autocomplete dropdown appears with suggestions |
| 3 | Select the matching suggestion from dropdown | Address field filled with full address; placeId auto-populated; latitude and longitude fields auto-populated with numeric coordinates |
| 4 | Verify latitude and longitude fields are readonly | Fields display coordinates but cannot be manually edited |

| Field | Value |
|-------|-------|
| **Postconditions** | Address, placeId, latitude, longitude fields are all populated from Nominatim |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Requires external Nominatim service availability |

---

### TC-BLD-005: Create building without address — allowed (optional field)

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-005 |
| **Module** | Building Management |
| **Priority** | Medium |
| **Preconditions** | Logged in as platformadmin@test.com; on create building form |
| **Test Data** | Name: "Temporary Building X", totalFloors: 1, Address: (empty) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open the create building form | Form opens |
| 2 | Enter name: "Temporary Building X" | Name populated |
| 3 | Leave address field empty | Address, placeId, lat/long all empty |
| 4 | Enter totalFloors: 1 | Floors set |
| 5 | Click "Save" / "Create" | Building created successfully; appears in list without address |

| Field | Value |
|-------|-------|
| **Postconditions** | Building "Temporary Building X" exists with null address/coordinates |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Address is not annotated with @NotBlank in BuildingRequest |

---

### TC-BLD-006: Create building linked to multiple institutions

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-006 |
| **Module** | Building Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; institutions UCT, Stellenbosch, Wits exist |
| **Test Data** | Name: "Multi-Uni Residence", totalFloors: 6, Institutions: [University of Cape Town, Stellenbosch University] |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open the create building form | Form opens |
| 2 | Enter name: "Multi-Uni Residence", totalFloors: 6 | Fields populated |
| 3 | Select "University of Cape Town" from institution multi-select | First institution chip appears |
| 4 | Select "Stellenbosch University" from institution multi-select | Second institution chip appears; both visible |
| 5 | Click "Save" / "Create" | Building created; detail view shows both institutions linked |

| Field | Value |
|-------|-------|
| **Postconditions** | Building linked to 2 institutions; retrievable via /api/building/by-institution for both |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-BLD-007: Create building with no institutions linked

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-007 |
| **Module** | Building Management |
| **Priority** | Medium |
| **Preconditions** | Logged in as platformadmin@test.com; on create building form |
| **Test Data** | Name: "Standalone Residence", totalFloors: 2, Institutions: (none selected) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open the create building form | Form opens |
| 2 | Enter name: "Standalone Residence", totalFloors: 2 | Fields populated |
| 3 | Do not select any institution | Institution field remains empty |
| 4 | Click "Save" / "Create" | Building created successfully with no linked institutions |

| Field | Value |
|-------|-------|
| **Postconditions** | Building exists with empty institution list |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | institutionIds is not @NotNull in BuildingRequest |

---

### TC-BLD-008: Create building with description text

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-008 |
| **Module** | Building Management |
| **Priority** | Low |
| **Preconditions** | Logged in as platformadmin@test.com; on create building form |
| **Test Data** | Name: "Described Residence", totalFloors: 3, Description: "A modern 3-floor residence with Wi-Fi, laundry facilities, and 24-hour security. Located near the main campus." |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open the create building form | Form opens |
| 2 | Enter name: "Described Residence", totalFloors: 3 | Fields populated |
| 3 | Enter long description in textarea | Description field accepts multi-line text |
| 4 | Click "Save" / "Create" | Building created; description preserved and visible in detail view |

| Field | Value |
|-------|-------|
| **Postconditions** | Building exists with full description text |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-BLD-009: Create building with totalFloors = 1 (minimum boundary)

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-009 |
| **Module** | Building Management |
| **Priority** | Medium |
| **Preconditions** | Logged in as platformadmin@test.com; on create building form |
| **Test Data** | Name: "Single Floor Building", totalFloors: 1 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open the create building form | Form opens |
| 2 | Enter name: "Single Floor Building" | Name populated |
| 3 | Enter totalFloors: 1 | Floors field accepts value 1 |
| 4 | Click "Save" / "Create" | Building created successfully with totalFloors = 1 |

| Field | Value |
|-------|-------|
| **Postconditions** | Building exists with 1 floor |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | @Min(1) boundary test |

---

### TC-BLD-010: Create building with totalFloors = 0 — validation error

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-010 |
| **Module** | Building Management |
| **Priority** | Medium |
| **Preconditions** | Logged in as platformadmin@test.com; on create building form |
| **Test Data** | Name: "Zero Floor Building", totalFloors: 0 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open the create building form | Form opens |
| 2 | Enter name: "Zero Floor Building" | Name populated |
| 3 | Enter totalFloors: 0 | Value entered |
| 4 | Click "Save" / "Create" | Validation error: "Total floors must be at least 1"; building not created |

| Field | Value |
|-------|-------|
| **Postconditions** | No building created; form shows validation error |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | @Min(1) on totalFloors |

---

## B. Admin — Update Building

### TC-BLD-011: Update building name

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-011 |
| **Module** | Building Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com; "Residence Hall Alpha" exists |
| **Test Data** | Existing: "Residence Hall Alpha", New name: "Residence Hall Alpha — Renovated" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/buildings | Buildings list displayed |
| 2 | Click edit on "Residence Hall Alpha" | Edit form opens pre-populated with current data |
| 3 | Change name to "Residence Hall Alpha — Renovated" | Name field updated |
| 4 | Click "Save" / "Update" | Success toast; building list shows updated name |
| 5 | Click on the building to view detail | Detail shows the new name |

| Field | Value |
|-------|-------|
| **Postconditions** | Building name is "Residence Hall Alpha — Renovated" |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-BLD-012: Update building address

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-012 |
| **Module** | Building Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; "Residence Hall Alpha" exists with address "123 Main Street" |
| **Test Data** | New address: "200 Campus Drive, Cape Town" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click edit on "Residence Hall Alpha" | Edit form opens |
| 2 | Clear address field and type "200 Campus Drive, Cape Town" | Nominatim autocomplete suggestions appear |
| 3 | Select a suggestion | Address, placeId, latitude, longitude updated |
| 4 | Click "Save" / "Update" | Success; building now shows new address |

| Field | Value |
|-------|-------|
| **Postconditions** | Building address updated to "200 Campus Drive, Cape Town" with new coordinates |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-BLD-013: Update linked institutions

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-013 |
| **Module** | Building Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; "Residence Hall Alpha" linked to UCT only |
| **Test Data** | Add: Stellenbosch University; Remove: University of Cape Town |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click edit on "Residence Hall Alpha" | Edit form shows UCT as linked institution |
| 2 | Remove "University of Cape Town" from multi-select | UCT chip removed |
| 3 | Add "Stellenbosch University" to multi-select | Stellenbosch chip appears |
| 4 | Click "Save" / "Update" | Success; building now linked to Stellenbosch only |
| 5 | Verify via GET /api/building/by-institution/{stellenboschId} | Building appears in Stellenbosch list, not in UCT list |

| Field | Value |
|-------|-------|
| **Postconditions** | Building linked to Stellenbosch only |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-BLD-014: Update building to duplicate name — error

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-014 |
| **Module** | Building Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; "Residence Hall Alpha" and "Residence Hall Beta" both exist |
| **Test Data** | Edit "Residence Hall Beta", change name to "Residence Hall Alpha" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click edit on "Residence Hall Beta" | Edit form opens |
| 2 | Change name to "Residence Hall Alpha" | Name field updated |
| 3 | Click "Save" / "Update" | Error: building name already exists; update rejected |

| Field | Value |
|-------|-------|
| **Postconditions** | "Residence Hall Beta" name unchanged |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## C. Admin — Delete Building

### TC-BLD-015: Delete building with no rooms — success

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-015 |
| **Module** | Building Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com; building "Temporary Building X" exists with zero rooms |
| **Test Data** | Building: "Temporary Building X" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/buildings | Buildings list shown |
| 2 | Click delete on "Temporary Building X" | Confirmation dialog appears |
| 3 | Confirm deletion | Success toast; building removed from list |
| 4 | Refresh the page | Building no longer appears |

| Field | Value |
|-------|-------|
| **Postconditions** | Building deleted from system |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-BLD-016: Delete building with rooms — verify cascade/restriction behavior

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-016 |
| **Module** | Building Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com; "Residence Hall Alpha" has rooms A-101 and A-102 |
| **Test Data** | Building: "Residence Hall Alpha" (has 2 rooms) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click delete on "Residence Hall Alpha" | Confirmation dialog appears |
| 2 | Confirm deletion | Either: (a) error message stating building has rooms and cannot be deleted, OR (b) cascade delete removes building and all its rooms |
| 3 | Verify outcome | If blocked: building and rooms still exist. If cascaded: building and rooms all removed |

| Field | Value |
|-------|-------|
| **Postconditions** | Document observed behavior (blocked vs cascaded) |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Record which behavior the system exhibits |

---

## D. Admin — Building Images

### TC-BLD-017: Upload single image to building

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-017 |
| **Module** | Building Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; "Residence Hall Alpha" exists; MinIO running |
| **Test Data** | File: building-front.jpg (JPEG, < 5 MB) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to building detail/edit for "Residence Hall Alpha" | Building detail opens |
| 2 | Click "Upload Image" button | File picker opens |
| 3 | Select building-front.jpg | File selected |
| 4 | Confirm upload | Image uploaded; thumbnail displayed in the building images section |
| 5 | Verify image loads via presigned URL | Image renders correctly in browser |

| Field | Value |
|-------|-------|
| **Postconditions** | One image associated with building; stored in MinIO |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | API: POST /api/building/{id}/images |

---

### TC-BLD-018: Upload multiple images

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-018 |
| **Module** | Building Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; "Residence Hall Alpha" exists |
| **Test Data** | Files: img1.jpg, img2.jpg, img3.png (all < 5 MB) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to building images section for "Residence Hall Alpha" | Images section displayed |
| 2 | Select multiple files (img1.jpg, img2.jpg, img3.png) in file picker | 3 files selected |
| 3 | Confirm upload | All 3 images uploaded; 3 thumbnails displayed |
| 4 | Verify GET /api/building/{id}/images returns 3 entries | Response contains 3 image objects with presigned URLs |

| Field | Value |
|-------|-------|
| **Postconditions** | 3 images associated with building |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-BLD-019: Set image as primary

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-019 |
| **Module** | Building Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; "Residence Hall Alpha" has 3 uploaded images; first image is primary by default |
| **Test Data** | Set second image as primary |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to building images section for "Residence Hall Alpha" | Images displayed with first marked as primary |
| 2 | Click "Set as Primary" on the second image | API call: PUT /api/building/{id}/images/{imageId}/primary |
| 3 | Verify the second image now shows the primary badge/indicator | Primary indicator moved to second image |
| 4 | Verify the first image no longer shows primary indicator | First image is no longer primary |

| Field | Value |
|-------|-------|
| **Postconditions** | Second image is the primary image for the building |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-BLD-020: Delete an image

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-020 |
| **Module** | Building Management |
| **Priority** | Medium |
| **Preconditions** | Logged in as platformadmin@test.com; "Residence Hall Alpha" has at least 2 images |
| **Test Data** | Delete the non-primary image |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to building images for "Residence Hall Alpha" | Images displayed |
| 2 | Click "Delete" on a non-primary image | Confirmation prompt appears |
| 3 | Confirm deletion | Image removed from display; image count decremented |
| 4 | Verify image no longer returned by GET /api/building/{id}/images | Deleted image not in response |

| Field | Value |
|-------|-------|
| **Postconditions** | Image deleted from MinIO and database |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | API: DELETE /api/building/{buildingId}/images/{imageId} |

---

### TC-BLD-021: Primary image shows first in display

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-021 |
| **Module** | Building Management |
| **Priority** | Medium |
| **Preconditions** | "Residence Hall Alpha" has 3 images; second image is set as primary |
| **Test Data** | N/A |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to the public building detail page /buildings/{alphaId} | Building detail page loads |
| 2 | Observe the image gallery/carousel | The primary image is displayed first / as the hero image |
| 3 | Navigate to /admin/buildings and view Alpha's detail | Primary image appears first in admin view as well |

| Field | Value |
|-------|-------|
| **Postconditions** | Primary image ordering confirmed on both public and admin views |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## E. Admin — Building Attributes

> **Screen (#305):** the app edits only accessibility features, from a building's menu in Building
> Management: Accessibility Features. Gender is set per room in Room Management. The building-level
> gender cases below are API checks of deprecated fields: run them with `PUT`, not a screen.

### TC-BLD-022: Set gender restriction to ANY

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-022 |
| **Module** | Building Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; "Residence Hall Alpha" exists |
| **Test Data** | genderRestriction: ANY, buildingId: {alphaId} |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to building attributes for "Residence Hall Alpha" | Attributes form/section displayed |
| 2 | Set genderRestriction to "ANY" | Dropdown/select updated |
| 3 | Click "Save" | API: PUT /api/admin/buildings/{id}/attributes; success response |
| 4 | Reload the page and verify attribute persisted | genderRestriction shows "ANY" |

| Field | Value |
|-------|-------|
| **Postconditions** | Building attributes saved with genderRestriction = ANY |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-BLD-023: Set gender restriction to MALE_ONLY

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-023 |
| **Module** | Building Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; "Residence Hall Gamma" exists |
| **Test Data** | genderRestriction: MALE_ONLY |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to building attributes for "Residence Hall Gamma" | Attributes form displayed |
| 2 | Set genderRestriction to "MALE_ONLY" | Value selected |
| 3 | Click "Save" | Success response; attribute persisted |
| 4 | Verify via GET /api/admin/buildings/{gammaId}/attributes | Response shows genderRestriction: "MALE_ONLY" |

| Field | Value |
|-------|-------|
| **Postconditions** | Building gender restriction is MALE_ONLY |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-BLD-024: Set gender restriction to FEMALE_ONLY

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-024 |
| **Module** | Building Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; "Residence Hall Beta" exists |
| **Test Data** | genderRestriction: FEMALE_ONLY |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to building attributes for "Residence Hall Beta" | Attributes form displayed |
| 2 | Set genderRestriction to "FEMALE_ONLY" | Value selected |
| 3 | Click "Save" | Success; attribute saved |
| 4 | Verify via GET /api/admin/buildings/{betaId}/attributes | Response shows genderRestriction: "FEMALE_ONLY" |

| Field | Value |
|-------|-------|
| **Postconditions** | Building gender restriction is FEMALE_ONLY |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-BLD-025: Set accessibility features

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-025 |
| **Module** | Building Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; "Residence Hall Alpha" exists |
| **Test Data** | accessibilityFeatures: "Wheelchair ramps, elevators on all floors, braille signage, accessible bathrooms" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to building attributes for "Residence Hall Alpha" | Attributes form displayed |
| 2 | Enter accessibilityFeatures text | Text entered in field |
| 3 | Click "Save" | Success; attributes updated |
| 4 | Reload page; verify accessibility features text persisted | Text matches what was entered |

| Field | Value |
|-------|-------|
| **Postconditions** | Accessibility features text saved |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | accessibilityFeatures is a free-text String field |

---

### TC-BLD-026: Set desirability score (1–10)

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-026 |
| **Module** | Building Management |
| **Priority** | Medium |
| **Preconditions** | Logged in as platformadmin@test.com; "Residence Hall Alpha" exists |
| **Test Data** | desirabilityScore: 8 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to building attributes for "Residence Hall Alpha" | Attributes form displayed |
| 2 | Set desirabilityScore to 8 | Value entered |
| 3 | Click "Save" | Success; score saved |
| 4 | Try setting score to 0 | Validation error: "Desirability score must be at least 1" |
| 5 | Try setting score to 11 | Validation error: "Desirability score must not exceed 10" |
| 6 | Set score to 10 and save | Success; boundary value accepted |

| Field | Value |
|-------|-------|
| **Postconditions** | Desirability score is 10 (final saved value) |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | @Min(1) @Max(10) validation |

---

## F. Public — Building Browsing (No Auth Required)

### TC-BLD-027: Browse buildings at /buildings — list loads without login

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-027 |
| **Module** | Building Management |
| **Priority** | Critical |
| **Preconditions** | User is NOT logged in; buildings exist in the system |
| **Test Data** | N/A |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open browser and navigate to http://localhost:4200/buildings | Page loads without redirect to login |
| 2 | Verify building cards/list displayed | Buildings shown with names, images, and basic info |
| 3 | Verify pagination controls visible | Page size default 12; pagination works |

| Field | Value |
|-------|-------|
| **Postconditions** | Public browse page accessible without authentication |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Uses /api/building/browse endpoint (no @PreAuthorize) |

---

### TC-BLD-028: View building detail at /buildings/:id without login

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-028 |
| **Module** | Building Management |
| **Priority** | Critical |
| **Preconditions** | User is NOT logged in; "Residence Hall Alpha" exists with id = {alphaId} |
| **Test Data** | URL: /buildings/{alphaId} |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to http://localhost:4200/buildings/{alphaId} | Building detail page loads |
| 2 | Verify building name, address, description, floors displayed | All data matches "Residence Hall Alpha" |
| 3 | Verify images load | Building images displayed via presigned MinIO URLs |

| Field | Value |
|-------|-------|
| **Postconditions** | Public detail page accessible without auth |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Uses /api/building/{id} (no @PreAuthorize) |

---

### TC-BLD-029: Search buildings by name (public)

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-029 |
| **Module** | Building Management |
| **Priority** | High |
| **Preconditions** | On /buildings page; buildings Alpha, Beta, Gamma exist |
| **Test Data** | Search term: "Alpha" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /buildings | Browse page loads |
| 2 | Enter "Alpha" in the search field | Search applied |
| 3 | Verify results | Only "Residence Hall Alpha" displayed; Beta and Gamma filtered out |

| Field | Value |
|-------|-------|
| **Postconditions** | Search results filtered by name |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Browse API uses `search` param which searches name and address |

---

### TC-BLD-030: Search buildings by address (public)

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-030 |
| **Module** | Building Management |
| **Priority** | High |
| **Preconditions** | On /buildings page; buildings with different addresses exist |
| **Test Data** | Search term: "Oak Avenue" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /buildings | Browse page loads |
| 2 | Enter "Oak Avenue" in the search field | Search applied |
| 3 | Verify results | Only "Residence Hall Beta" (456 Oak Avenue) displayed |

| Field | Value |
|-------|-------|
| **Postconditions** | Results filtered by address content |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-BLD-031: Filter by price range and availability (public browse)

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-031 |
| **Module** | Building Management |
| **Priority** | Medium |
| **Preconditions** | On /buildings page; buildings have rooms with varying monthly rates |
| **Test Data** | minPrice: 3000, maxPrice: 4000, available: true |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /buildings | Browse page loads |
| 2 | Set price filter: min R3 000, max R4 000 | Price filter applied |
| 3 | Toggle "Available" filter on | Availability filter applied |
| 4 | Verify results | Only buildings with rooms in R3 000–R4 000 range that have available rooms are shown |

| Field | Value |
|-------|-------|
| **Postconditions** | Results filtered by price and availability |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Browse API supports minPrice, maxPrice, available params |

---

### TC-BLD-032: View building images in detail page

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-032 |
| **Module** | Building Management |
| **Priority** | Medium |
| **Preconditions** | "Residence Hall Alpha" has 3 images uploaded; user not logged in |
| **Test Data** | Building: Residence Hall Alpha |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /buildings/{alphaId} | Detail page loads |
| 2 | Observe image gallery/carousel area | Images are displayed; primary image appears first |
| 3 | Navigate through images (next/prev if carousel) | All 3 images viewable |

| Field | Value |
|-------|-------|
| **Postconditions** | All building images visible on public detail page |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Uses GET /api/building/{id}/images (no auth) |

---

## G. Admin — Search & Filter

### TC-BLD-033: Search buildings by name in admin panel

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-033 |
| **Module** | Building Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; on /admin/buildings |
| **Test Data** | Search: "Gamma" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/buildings | Admin buildings list shown |
| 2 | Enter "Gamma" in the name search field | Search triggered |
| 3 | Verify results | Only "Residence Hall Gamma" displayed |

| Field | Value |
|-------|-------|
| **Postconditions** | Admin search correctly filters by name |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | API: GET /api/building/search?name=Gamma |

---

### TC-BLD-034: Filter by institution in admin

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-034 |
| **Module** | Building Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com; buildings linked to different institutions |
| **Test Data** | Filter: institution = "Wits University" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/buildings | Admin list shown |
| 2 | Select "Wits University" from institution filter dropdown | Filter applied |
| 3 | Verify results | Only "Residence Hall Gamma" (linked to Wits) displayed |

| Field | Value |
|-------|-------|
| **Postconditions** | List filtered to Wits buildings only |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | API: GET /api/building/by-institution/{witsId} |

---

### TC-BLD-035: Empty search results — empty state shown

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-035 |
| **Module** | Building Management |
| **Priority** | Medium |
| **Preconditions** | Logged in as platformadmin@test.com; on /admin/buildings |
| **Test Data** | Search: "NonExistentBuildingXYZ123" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Enter "NonExistentBuildingXYZ123" in search field | Search applied |
| 2 | Verify results area | Empty state message displayed (e.g., "No buildings found") |
| 3 | Verify no error/crash | Page remains functional |

| Field | Value |
|-------|-------|
| **Postconditions** | Empty state UI is user-friendly; no errors |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## H. Access Control

### TC-BLD-036: Student cannot access /admin/buildings

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-036 |
| **Module** | Building Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as a student user (not admin) |
| **Test Data** | Student account credentials |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as student user | Dashboard loads |
| 2 | Manually navigate to http://localhost:4200/admin/buildings | Access denied: redirected to student dashboard or 403 page displayed |
| 3 | Attempt direct API call: POST /api/building with valid body and student JWT | 403 Forbidden response |

| Field | Value |
|-------|-------|
| **Postconditions** | Student has no access to admin building management |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')") enforced |

---

### TC-BLD-037: Tenant admin sees only their tenant's buildings

| Field | Value |
|-------|-------|
| **ID** | TC-BLD-037 |
| **Module** | Building Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as tenantadmin@test.com; multiple tenants exist with different buildings |
| **Test Data** | Tenant admin's tenant has "Residence Hall Alpha"; another tenant has "Residence Hall Gamma" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as tenantadmin@test.com | Admin dashboard loads |
| 2 | Navigate to /admin/buildings | Buildings list shown |
| 3 | Verify only buildings belonging to the tenant admin's tenant are displayed | Only tenant-scoped buildings shown (e.g., "Residence Hall Alpha") |
| 4 | Verify "Residence Hall Gamma" (other tenant's building) is NOT visible | Building not in list |
| 5 | Attempt direct API: GET /api/building/{gammaId} | Either 403 or 404 (tenant filter excludes it) |

| Field | Value |
|-------|-------|
| **Postconditions** | Multi-tenancy isolation confirmed for building management |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Hibernate tenantFilter scopes queries by tenant_id |
