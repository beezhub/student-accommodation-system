# Reference Data Management — UAT Test Cases

> **Module:** Reference Data Management (Institutions, Years of Study, Academic Years, Accommodation Types)
> **Application URL:** http://localhost:4200
> **Total Test Cases:** 49

## Test Data

| Role | Email | Password |
|------|-------|----------|
| Student A | john.doe@test.com | Student@123 |
| Platform Admin | platformadmin@test.com | Admin@12345 |
| Tenant Admin | tenantadmin@test.com | Admin@12345 |

**Institutions:** University of Cape Town, Stellenbosch University, Wits University
**Years of Study:** 1st Year, 2nd Year, 3rd Year, 4th Year, Postgraduate
**Academic Year:** 2026 (2026-02-01 to 2026-11-30, app open 2026-01-01, close 2026-01-31)

**Validation Rules:**
- Institution: name (required, unique, max 100), address (required, max 255), phoneNumber (required, max 20), emailAddress (required, valid email, max 100)
- Year of Study: yearOfStudy (required, max 50, numericYearValidator — accepts "1", "1st Year", "First Year", "Year 2"; rejects "undergrad", "senior", "abc")
- Academic Year: name (required, max 20), startDate (required), endDate (required), applicationOpenDate (required), applicationCloseDate (required), isCurrent (only one true)
- Accommodation Type: name (required, unique, max 100), description (max 500), isActive (toggle, default true)

---

## Institutions (`/admin/institutions`)

---

### TC-REF-001: Create Institution with All Valid Fields

| Field | Value |
|-------|-------|
| **ID** | TC-REF-001 |
| **Module** | Reference Data Management |
| **Priority** | Critical |
| **Preconditions** | Tenant Admin logged in. No institution with name "Rhodes University" exists. |
| **Test Data** | Name: Rhodes University, Address: Drostdy Rd, Makhanda, 6139, Phone: +27466038111, Email: info@ru.ac.za |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin (tenantadmin@test.com / Admin@12345) | Admin dashboard loads |
| 2 | Navigate to /admin/institutions | Institutions management page loads |
| 3 | Click "Add Institution" button | Create institution form/dialog appears |
| 4 | Enter Name: "Rhodes University" | Field populated |
| 5 | Enter Address: "Drostdy Rd, Makhanda, 6139" | Field populated |
| 6 | Enter Phone: "+27466038111" | Field populated |
| 7 | Enter Email: "info@ru.ac.za" | Field populated |
| 8 | Click "Save" / "Create" | Success message displayed; institution appears in list |
| 9 | Verify institution appears in the institutions table | Rhodes University listed with correct details |

| Field | Value |
|-------|-------|
| **Postconditions** | Institution "Rhodes University" created and visible in list |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-002: Create Institution with Empty Name

| Field | Value |
|-------|-------|
| **ID** | TC-REF-002 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Tenant Admin logged in. On institutions management page. |
| **Test Data** | Name: (empty), Address: "123 Test St", Phone: "0123456789", Email: "test@test.com" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Institution" button | Create form appears |
| 2 | Leave Name field empty | Field empty |
| 3 | Fill in Address: "123 Test St", Phone: "0123456789", Email: "test@test.com" | Other fields populated |
| 4 | Click "Save" | Validation error displayed: name is required |
| 5 | Verify institution is NOT created | No new institution in list |

| Field | Value |
|-------|-------|
| **Postconditions** | No institution created; validation error shown |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-003: Create Institution with Name > 100 Characters

| Field | Value |
|-------|-------|
| **ID** | TC-REF-003 |
| **Module** | Reference Data Management |
| **Priority** | Medium |
| **Preconditions** | Tenant Admin logged in. On institutions management page. |
| **Test Data** | Name: 101-character string (e.g., "A" repeated 101 times), Address: "123 Test St", Phone: "0123456789", Email: "test@test.com" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Institution" button | Create form appears |
| 2 | Enter a name with 101 characters | Field populated with long string |
| 3 | Fill in remaining required fields with valid data | Fields populated |
| 4 | Click "Save" | Validation error: name exceeds maximum length of 100 characters |
| 5 | Verify institution is NOT created | No new institution in list |

| Field | Value |
|-------|-------|
| **Postconditions** | No institution created; max length validation enforced |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-004: Create Institution with Duplicate Name

| Field | Value |
|-------|-------|
| **ID** | TC-REF-004 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Tenant Admin logged in. Institution "University of Cape Town" already exists. |
| **Test Data** | Name: University of Cape Town, Address: "New Address", Phone: "0000000000", Email: "dup@test.com" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Institution" button | Create form appears |
| 2 | Enter Name: "University of Cape Town" | Field populated |
| 3 | Fill in remaining fields with valid data | Fields populated |
| 4 | Click "Save" | Error: institution name must be unique / already exists |
| 5 | Verify duplicate institution is NOT created | Only one "University of Cape Town" in list |

| Field | Value |
|-------|-------|
| **Postconditions** | Duplicate prevented; uniqueness constraint enforced |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-005: Create Institution with Empty Address

| Field | Value |
|-------|-------|
| **ID** | TC-REF-005 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Tenant Admin logged in. On institutions management page. |
| **Test Data** | Name: "Test University", Address: (empty), Phone: "0123456789", Email: "test@test.com" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Institution" button | Create form appears |
| 2 | Enter Name: "Test University" | Field populated |
| 3 | Leave Address field empty | Field empty |
| 4 | Fill Phone: "0123456789", Email: "test@test.com" | Fields populated |
| 5 | Click "Save" | Validation error: address is required |

| Field | Value |
|-------|-------|
| **Postconditions** | No institution created; address required validation shown |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-006: Create Institution with Address > 255 Characters

| Field | Value |
|-------|-------|
| **ID** | TC-REF-006 |
| **Module** | Reference Data Management |
| **Priority** | Medium |
| **Preconditions** | Tenant Admin logged in. On institutions management page. |
| **Test Data** | Name: "Test University", Address: 256-character string, Phone: "0123456789", Email: "test@test.com" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Institution" button | Create form appears |
| 2 | Enter Name: "Test University" | Field populated |
| 3 | Enter an address with 256 characters | Field populated with long string |
| 4 | Fill remaining fields with valid data | Fields populated |
| 5 | Click "Save" | Validation error: address exceeds maximum length of 255 characters |

| Field | Value |
|-------|-------|
| **Postconditions** | No institution created; max length validation enforced |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-007: Create Institution with Empty Phone

| Field | Value |
|-------|-------|
| **ID** | TC-REF-007 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Tenant Admin logged in. On institutions management page. |
| **Test Data** | Name: "Test University", Address: "123 Test St", Phone: (empty), Email: "test@test.com" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Institution" button | Create form appears |
| 2 | Enter Name: "Test University", Address: "123 Test St" | Fields populated |
| 3 | Leave Phone field empty | Field empty |
| 4 | Enter Email: "test@test.com" | Field populated |
| 5 | Click "Save" | Validation error: phone number is required |

| Field | Value |
|-------|-------|
| **Postconditions** | No institution created; phone required validation shown |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-008: Create Institution with Phone > 20 Characters

| Field | Value |
|-------|-------|
| **ID** | TC-REF-008 |
| **Module** | Reference Data Management |
| **Priority** | Medium |
| **Preconditions** | Tenant Admin logged in. On institutions management page. |
| **Test Data** | Name: "Test University", Address: "123 Test St", Phone: "123456789012345678901" (21 chars), Email: "test@test.com" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Institution" button | Create form appears |
| 2 | Fill in Name and Address with valid data | Fields populated |
| 3 | Enter Phone: "123456789012345678901" (21 characters) | Field populated |
| 4 | Enter Email: "test@test.com" | Field populated |
| 5 | Click "Save" | Validation error: phone number exceeds maximum length of 20 characters |

| Field | Value |
|-------|-------|
| **Postconditions** | No institution created; phone max length validation enforced |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-009: Create Institution with Empty Email

| Field | Value |
|-------|-------|
| **ID** | TC-REF-009 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Tenant Admin logged in. On institutions management page. |
| **Test Data** | Name: "Test University", Address: "123 Test St", Phone: "0123456789", Email: (empty) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Institution" button | Create form appears |
| 2 | Fill in Name, Address, Phone with valid data | Fields populated |
| 3 | Leave Email field empty | Field empty |
| 4 | Click "Save" | Validation error: email is required |

| Field | Value |
|-------|-------|
| **Postconditions** | No institution created; email required validation shown |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-010: Create Institution with Invalid Email Format

| Field | Value |
|-------|-------|
| **ID** | TC-REF-010 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Tenant Admin logged in. On institutions management page. |
| **Test Data** | Name: "Test University", Address: "123 Test St", Phone: "0123456789", Email: "not-an-email" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Institution" button | Create form appears |
| 2 | Fill in Name, Address, Phone with valid data | Fields populated |
| 3 | Enter Email: "not-an-email" | Field populated |
| 4 | Click "Save" | Validation error: invalid email format |
| 5 | Try Email: "test@" and click "Save" | Validation error: invalid email format |
| 6 | Try Email: "@test.com" and click "Save" | Validation error: invalid email format |

| Field | Value |
|-------|-------|
| **Postconditions** | No institution created; email format validation enforced |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-011: Create Institution with Email > 100 Characters

| Field | Value |
|-------|-------|
| **ID** | TC-REF-011 |
| **Module** | Reference Data Management |
| **Priority** | Medium |
| **Preconditions** | Tenant Admin logged in. On institutions management page. |
| **Test Data** | Name: "Test University", Address: "123 Test St", Phone: "0123456789", Email: 101-character valid email (e.g., "aaa...aaa@test.com") |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Institution" button | Create form appears |
| 2 | Fill in Name, Address, Phone with valid data | Fields populated |
| 3 | Enter an email address with 101 characters total (valid format) | Field populated |
| 4 | Click "Save" | Validation error: email exceeds maximum length of 100 characters |

| Field | Value |
|-------|-------|
| **Postconditions** | No institution created; email max length validation enforced |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-012: Update Institution

| Field | Value |
|-------|-------|
| **ID** | TC-REF-012 |
| **Module** | Reference Data Management |
| **Priority** | Critical |
| **Preconditions** | Tenant Admin logged in. Institution "Rhodes University" exists. |
| **Test Data** | Updated Address: "New Campus, Makhanda, 6140" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/institutions | Institutions list displayed |
| 2 | Locate "Rhodes University" in the list | Institution found |
| 3 | Click "Edit" on Rhodes University | Edit form loads with current values |
| 4 | Verify current values are pre-populated | All fields show existing data |
| 5 | Change Address to "New Campus, Makhanda, 6140" | Address field updated |
| 6 | Click "Save" / "Update" | Success message displayed |
| 7 | Verify the updated address is shown in the institutions list | New address displayed |

| Field | Value |
|-------|-------|
| **Postconditions** | Institution updated with new address |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-013: Delete Institution

| Field | Value |
|-------|-------|
| **ID** | TC-REF-013 |
| **Module** | Reference Data Management |
| **Priority** | Critical |
| **Preconditions** | Tenant Admin logged in. Institution "Rhodes University" exists and is not referenced by any student. |
| **Test Data** | Institution: Rhodes University |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/institutions | Institutions list displayed |
| 2 | Locate "Rhodes University" in the list | Institution found |
| 3 | Click "Delete" on Rhodes University | Confirmation dialog appears |
| 4 | Confirm deletion | Institution removed; success message displayed |
| 5 | Verify "Rhodes University" no longer appears in the list | Institution absent from list |

| Field | Value |
|-------|-------|
| **Postconditions** | Institution deleted and removed from list |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | If institution is referenced by students, deletion may be blocked — test that scenario separately if applicable |

---

### TC-REF-014: Search Institutions by Name

| Field | Value |
|-------|-------|
| **ID** | TC-REF-014 |
| **Module** | Reference Data Management |
| **Priority** | Medium |
| **Preconditions** | Tenant Admin logged in. Multiple institutions exist (University of Cape Town, Stellenbosch University, Wits University). |
| **Test Data** | Search term: "Cape" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/institutions | Institutions list displayed with all institutions |
| 2 | Locate the search/filter input | Search field visible |
| 3 | Enter "Cape" in the search field | Search initiated |
| 4 | Verify results are filtered | Only "University of Cape Town" shown (or institutions containing "Cape") |
| 5 | Clear the search field | All institutions shown again |
| 6 | Enter "xyz" (no match) | No results or empty state displayed |

| Field | Value |
|-------|-------|
| **Postconditions** | Search filters institutions correctly |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## Years of Study (`/admin/year-of-study`)

---

### TC-REF-015: Create Year of Study with Value "1"

| Field | Value |
|-------|-------|
| **ID** | TC-REF-015 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Tenant Admin logged in. No year of study with value "1" exists. |
| **Test Data** | Year of Study: "1" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/year-of-study | Years of study management page loads |
| 2 | Click "Add Year of Study" button | Create form appears |
| 3 | Enter value: "1" | Field populated |
| 4 | Click "Save" | Success message; year of study "1" appears in list |

| Field | Value |
|-------|-------|
| **Postconditions** | Year of study "1" created |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | numericYearValidator accepts plain numeric values |

---

### TC-REF-016: Create Year of Study with Value "1st Year"

| Field | Value |
|-------|-------|
| **ID** | TC-REF-016 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Tenant Admin logged in. |
| **Test Data** | Year of Study: "1st Year" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/year-of-study | Years of study page loads |
| 2 | Click "Add Year of Study" | Create form appears |
| 3 | Enter value: "1st Year" | Field populated |
| 4 | Click "Save" | Success message; "1st Year" appears in list |

| Field | Value |
|-------|-------|
| **Postconditions** | Year of study "1st Year" created |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | numericYearValidator accepts ordinal format |

---

### TC-REF-017: Create Year of Study with Value "First Year"

| Field | Value |
|-------|-------|
| **ID** | TC-REF-017 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Tenant Admin logged in. |
| **Test Data** | Year of Study: "First Year" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/year-of-study | Years of study page loads |
| 2 | Click "Add Year of Study" | Create form appears |
| 3 | Enter value: "First Year" | Field populated |
| 4 | Click "Save" | Success message; "First Year" appears in list |

| Field | Value |
|-------|-------|
| **Postconditions** | Year of study "First Year" created |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | numericYearValidator accepts word format |

---

### TC-REF-018: Create Year of Study with Value "Year 2"

| Field | Value |
|-------|-------|
| **ID** | TC-REF-018 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Tenant Admin logged in. |
| **Test Data** | Year of Study: "Year 2" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/year-of-study | Years of study page loads |
| 2 | Click "Add Year of Study" | Create form appears |
| 3 | Enter value: "Year 2" | Field populated |
| 4 | Click "Save" | Success message; "Year 2" appears in list |

| Field | Value |
|-------|-------|
| **Postconditions** | Year of study "Year 2" created |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | numericYearValidator accepts "Year N" format |

---

### TC-REF-019: Create Year of Study with Value "2nd Year"

| Field | Value |
|-------|-------|
| **ID** | TC-REF-019 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Tenant Admin logged in. |
| **Test Data** | Year of Study: "2nd Year" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/year-of-study | Years of study page loads |
| 2 | Click "Add Year of Study" | Create form appears |
| 3 | Enter value: "2nd Year" | Field populated |
| 4 | Click "Save" | Success message; "2nd Year" appears in list |

| Field | Value |
|-------|-------|
| **Postconditions** | Year of study "2nd Year" created |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-020: Create Year of Study with Value "undergrad" — Error

| Field | Value |
|-------|-------|
| **ID** | TC-REF-020 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Tenant Admin logged in. |
| **Test Data** | Year of Study: "undergrad" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/year-of-study | Years of study page loads |
| 2 | Click "Add Year of Study" | Create form appears |
| 3 | Enter value: "undergrad" | Field populated |
| 4 | Click "Save" | Validation error: invalid year of study format |
| 5 | Verify "undergrad" is NOT created | Not in list |

| Field | Value |
|-------|-------|
| **Postconditions** | No year of study created; validator rejects non-numeric format |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | numericYearValidator rejects arbitrary text |

---

### TC-REF-021: Create Year of Study with Value "senior" — Error

| Field | Value |
|-------|-------|
| **ID** | TC-REF-021 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Tenant Admin logged in. |
| **Test Data** | Year of Study: "senior" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/year-of-study | Years of study page loads |
| 2 | Click "Add Year of Study" | Create form appears |
| 3 | Enter value: "senior" | Field populated |
| 4 | Click "Save" | Validation error: invalid year of study format |

| Field | Value |
|-------|-------|
| **Postconditions** | No year of study created |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-022: Create Year of Study with Value "abc" — Error

| Field | Value |
|-------|-------|
| **ID** | TC-REF-022 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Tenant Admin logged in. |
| **Test Data** | Year of Study: "abc" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/year-of-study | Years of study page loads |
| 2 | Click "Add Year of Study" | Create form appears |
| 3 | Enter value: "abc" | Field populated |
| 4 | Click "Save" | Validation error: invalid year of study format |

| Field | Value |
|-------|-------|
| **Postconditions** | No year of study created |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-023: Create Year of Study with Empty Value

| Field | Value |
|-------|-------|
| **ID** | TC-REF-023 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Tenant Admin logged in. |
| **Test Data** | Year of Study: (empty) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/year-of-study | Years of study page loads |
| 2 | Click "Add Year of Study" | Create form appears |
| 3 | Leave value field empty | Field empty |
| 4 | Click "Save" | Validation error: year of study is required |

| Field | Value |
|-------|-------|
| **Postconditions** | No year of study created; required validation shown |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-024: Create Year of Study with Value > 50 Characters

| Field | Value |
|-------|-------|
| **ID** | TC-REF-024 |
| **Module** | Reference Data Management |
| **Priority** | Medium |
| **Preconditions** | Tenant Admin logged in. |
| **Test Data** | Year of Study: 51-character string (e.g., "1st Year" padded to 51 chars) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/year-of-study | Years of study page loads |
| 2 | Click "Add Year of Study" | Create form appears |
| 3 | Enter a value with 51 characters | Field populated |
| 4 | Click "Save" | Validation error: exceeds maximum length of 50 characters |

| Field | Value |
|-------|-------|
| **Postconditions** | No year of study created; max length validation enforced |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-025: Update Year of Study

| Field | Value |
|-------|-------|
| **ID** | TC-REF-025 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Tenant Admin logged in. Year of study "1st Year" exists. |
| **Test Data** | Updated value: "First Year" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/year-of-study | Years of study list displayed |
| 2 | Locate "1st Year" in the list | Entry found |
| 3 | Click "Edit" | Edit form loads with current value |
| 4 | Change value to "First Year" | Field updated |
| 5 | Click "Save" | Success message; updated value shown in list |

| Field | Value |
|-------|-------|
| **Postconditions** | Year of study updated from "1st Year" to "First Year" |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-026: Delete Year of Study

| Field | Value |
|-------|-------|
| **ID** | TC-REF-026 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Tenant Admin logged in. Year of study "Year 2" exists and is not referenced by any student. |
| **Test Data** | Year of Study: "Year 2" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/year-of-study | Years of study list displayed |
| 2 | Locate "Year 2" in the list | Entry found |
| 3 | Click "Delete" | Confirmation dialog appears |
| 4 | Confirm deletion | Entry removed; success message displayed |
| 5 | Verify "Year 2" no longer in list | Entry absent |

| Field | Value |
|-------|-------|
| **Postconditions** | Year of study deleted |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## Academic Years (`/admin/academic-years`)

---

### TC-REF-027: Create Academic Year with All Valid Fields

| Field | Value |
|-------|-------|
| **ID** | TC-REF-027 |
| **Module** | Reference Data Management |
| **Priority** | Critical |
| **Preconditions** | Platform Admin logged in. No academic year named "2027" exists. |
| **Test Data** | Name: "2027", Start: 2027-02-01, End: 2027-11-30, App Open: 2027-01-01, App Close: 2027-01-31, isCurrent: false |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/academic-years | Academic years page loads |
| 2 | Click "Add Academic Year" | Create form appears |
| 3 | Enter Name: "2027" | Field populated |
| 4 | Enter Start Date: 2027-02-01 | Date selected |
| 5 | Enter End Date: 2027-11-30 | Date selected |
| 6 | Enter Application Open Date: 2027-01-01 | Date selected |
| 7 | Enter Application Close Date: 2027-01-31 | Date selected |
| 8 | Leave isCurrent unchecked | Checkbox unchecked |
| 9 | Click "Save" | Success message; "2027" appears in academic years list |

| Field | Value |
|-------|-------|
| **Postconditions** | Academic year "2027" created with all dates |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-028: Create Academic Year with Empty Name

| Field | Value |
|-------|-------|
| **ID** | TC-REF-028 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Platform Admin logged in. |
| **Test Data** | Name: (empty), all dates valid |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Academic Year" | Create form appears |
| 2 | Leave Name field empty | Field empty |
| 3 | Fill in all date fields with valid values | Dates populated |
| 4 | Click "Save" | Validation error: name is required |

| Field | Value |
|-------|-------|
| **Postconditions** | No academic year created; name required validation shown |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-029: Create Academic Year with Name > 20 Characters

| Field | Value |
|-------|-------|
| **ID** | TC-REF-029 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Platform Admin logged in. |
| **Test Data** | Name: "Academic Year 2026-2027" (23 characters), all dates valid |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Academic Year" | Create form appears |
| 2 | Enter Name: "Academic Year 2026-2027" (23 chars) | Field populated |
| 3 | Fill in all date fields with valid values | Dates populated |
| 4 | Click "Save" | Validation error: name exceeds maximum length of 20 characters |

| Field | Value |
|-------|-------|
| **Postconditions** | No academic year created; VARCHAR(20) constraint enforced |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | DB column is VARCHAR(20) — this is a critical boundary test |

---

### TC-REF-030: Create Academic Year with Name Exactly 20 Characters

| Field | Value |
|-------|-------|
| **ID** | TC-REF-030 |
| **Module** | Reference Data Management |
| **Priority** | Medium |
| **Preconditions** | Platform Admin logged in. |
| **Test Data** | Name: "AcademicYear2026-027" (exactly 20 characters), all dates valid |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Academic Year" | Create form appears |
| 2 | Enter Name: "AcademicYear2026-027" (20 chars exactly) | Field populated |
| 3 | Fill in all date fields with valid values | Dates populated |
| 4 | Click "Save" | Success message; academic year created |
| 5 | Verify name is stored correctly with all 20 characters | Full name displayed in list |

| Field | Value |
|-------|-------|
| **Postconditions** | Academic year created at exact boundary length |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Boundary test for VARCHAR(20) |

---

### TC-REF-031: Create Academic Year without Start Date

| Field | Value |
|-------|-------|
| **ID** | TC-REF-031 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Platform Admin logged in. |
| **Test Data** | Name: "2028", Start Date: (empty), other dates valid |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Academic Year" | Create form appears |
| 2 | Enter Name: "2028" | Field populated |
| 3 | Leave Start Date empty | Field empty |
| 4 | Fill in End Date, App Open Date, App Close Date with valid values | Dates populated |
| 5 | Click "Save" | Validation error: start date is required |

| Field | Value |
|-------|-------|
| **Postconditions** | No academic year created; start date required |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-032: Create Academic Year without End Date

| Field | Value |
|-------|-------|
| **ID** | TC-REF-032 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Platform Admin logged in. |
| **Test Data** | Name: "2028", End Date: (empty), other dates valid |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Academic Year" | Create form appears |
| 2 | Enter Name: "2028" | Field populated |
| 3 | Fill in Start Date, App Open Date, App Close Date | Dates populated |
| 4 | Leave End Date empty | Field empty |
| 5 | Click "Save" | Validation error: end date is required |

| Field | Value |
|-------|-------|
| **Postconditions** | No academic year created; end date required |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-033: Create Academic Year without Application Open Date

| Field | Value |
|-------|-------|
| **ID** | TC-REF-033 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Platform Admin logged in. |
| **Test Data** | Name: "2028", Application Open Date: (empty), other dates valid |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Academic Year" | Create form appears |
| 2 | Enter Name: "2028" | Field populated |
| 3 | Fill in Start Date, End Date, App Close Date | Dates populated |
| 4 | Leave Application Open Date empty | Field empty |
| 5 | Click "Save" | Validation error: application open date is required |

| Field | Value |
|-------|-------|
| **Postconditions** | No academic year created; application open date required |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-034: Create Academic Year without Application Close Date

| Field | Value |
|-------|-------|
| **ID** | TC-REF-034 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Platform Admin logged in. |
| **Test Data** | Name: "2028", Application Close Date: (empty), other dates valid |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Academic Year" | Create form appears |
| 2 | Enter Name: "2028" | Field populated |
| 3 | Fill in Start Date, End Date, App Open Date | Dates populated |
| 4 | Leave Application Close Date empty | Field empty |
| 5 | Click "Save" | Validation error: application close date is required |

| Field | Value |
|-------|-------|
| **Postconditions** | No academic year created; application close date required |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-035: Set Academic Year as Current

| Field | Value |
|-------|-------|
| **ID** | TC-REF-035 |
| **Module** | Reference Data Management |
| **Priority** | Critical |
| **Preconditions** | Platform Admin logged in. Academic year "2026" is currently marked as isCurrent=true. Academic year "2027" exists with isCurrent=false. |
| **Test Data** | Current: "2026", Target: "2027" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/academic-years | Academic years list displayed |
| 2 | Verify "2026" is marked as current | Current indicator visible on "2026" |
| 3 | Edit "2027" and set isCurrent to true (check the checkbox) | Checkbox checked |
| 4 | Click "Save" | Success message displayed |
| 5 | Verify "2027" is now marked as current | Current indicator on "2027" |
| 6 | Verify "2026" is no longer marked as current | Current indicator removed from "2026" |

| Field | Value |
|-------|-------|
| **Postconditions** | "2027" is current; "2026" is no longer current |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | System should automatically unset previous current year |

---

### TC-REF-036: Only One Academic Year Can Be Current

| Field | Value |
|-------|-------|
| **ID** | TC-REF-036 |
| **Module** | Reference Data Management |
| **Priority** | Critical |
| **Preconditions** | Platform Admin logged in. Multiple academic years exist. One is marked as current. |
| **Test Data** | Academic years: "2026" (current), "2027", "2028" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/academic-years | Academic years list displayed |
| 2 | Count the number of academic years marked as current | Exactly 1 academic year is current |
| 3 | Set "2028" as current | "2028" marked as current |
| 4 | Verify only "2028" is current; "2026" and "2027" are not | Exactly 1 current year |
| 5 | Set "2026" as current again | "2026" marked as current |
| 6 | Verify only "2026" is current | Exactly 1 current year |

| Field | Value |
|-------|-------|
| **Postconditions** | Only one academic year is current at any time |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Verify at DB level if needed: `SELECT COUNT(*) FROM academic_year WHERE is_current = true` should be 1 |

---

### TC-REF-037: Update Academic Year Dates

| Field | Value |
|-------|-------|
| **ID** | TC-REF-037 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Platform Admin logged in. Academic year "2027" exists. |
| **Test Data** | Updated Start Date: 2027-03-01, Updated End Date: 2027-12-15 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/academic-years | Academic years list displayed |
| 2 | Click "Edit" on "2027" | Edit form loads with current values |
| 3 | Verify all date fields are pre-populated with current values | Dates shown correctly |
| 4 | Change Start Date to 2027-03-01 | Date updated |
| 5 | Change End Date to 2027-12-15 | Date updated |
| 6 | Click "Save" | Success message displayed |
| 7 | Verify updated dates are shown in the list | New dates displayed |

| Field | Value |
|-------|-------|
| **Postconditions** | Academic year dates updated |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-038: Delete Academic Year

| Field | Value |
|-------|-------|
| **ID** | TC-REF-038 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Platform Admin logged in. Academic year "2028" exists and has no associated applications. |
| **Test Data** | Academic Year: "2028" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/academic-years | Academic years list displayed |
| 2 | Locate "2028" in the list | Entry found |
| 3 | Click "Delete" | Confirmation dialog appears |
| 4 | Confirm deletion | Academic year removed; success message |
| 5 | Verify "2028" no longer appears in list | Entry absent |

| Field | Value |
|-------|-------|
| **Postconditions** | Academic year deleted |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## Accommodation Types (`/admin/accommodation-types`)

---

### TC-REF-039: Create Accommodation Type with Valid Data

| Field | Value |
|-------|-------|
| **ID** | TC-REF-039 |
| **Module** | Reference Data Management |
| **Priority** | Critical |
| **Preconditions** | Tenant Admin logged in. No accommodation type named "Studio Apartment" exists. |
| **Test Data** | Name: "Studio Apartment", Description: "Self-contained studio unit with kitchenette" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/accommodation-types | Accommodation types page loads |
| 2 | Click "Add Accommodation Type" | Create form appears |
| 3 | Enter Name: "Studio Apartment" | Field populated |
| 4 | Enter Description: "Self-contained studio unit with kitchenette" | Field populated |
| 5 | Verify isActive is checked by default | Toggle/checkbox is on |
| 6 | Click "Save" | Success message; "Studio Apartment" appears in list |

| Field | Value |
|-------|-------|
| **Postconditions** | Accommodation type "Studio Apartment" created, active by default |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-040: Create Accommodation Type with Empty Name

| Field | Value |
|-------|-------|
| **ID** | TC-REF-040 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Tenant Admin logged in. |
| **Test Data** | Name: (empty), Description: "Some description" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Accommodation Type" | Create form appears |
| 2 | Leave Name field empty | Field empty |
| 3 | Enter Description: "Some description" | Field populated |
| 4 | Click "Save" | Validation error: name is required |

| Field | Value |
|-------|-------|
| **Postconditions** | No accommodation type created; name required validation shown |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-041: Create Accommodation Type with Name > 100 Characters

| Field | Value |
|-------|-------|
| **ID** | TC-REF-041 |
| **Module** | Reference Data Management |
| **Priority** | Medium |
| **Preconditions** | Tenant Admin logged in. |
| **Test Data** | Name: 101-character string |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Accommodation Type" | Create form appears |
| 2 | Enter a name with 101 characters | Field populated |
| 3 | Click "Save" | Validation error: name exceeds maximum length of 100 characters |

| Field | Value |
|-------|-------|
| **Postconditions** | No accommodation type created; max length validation enforced |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-042: Create Accommodation Type with Duplicate Name

| Field | Value |
|-------|-------|
| **ID** | TC-REF-042 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Tenant Admin logged in. Accommodation type "Studio Apartment" already exists. |
| **Test Data** | Name: "Studio Apartment", Description: "Duplicate attempt" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Accommodation Type" | Create form appears |
| 2 | Enter Name: "Studio Apartment" | Field populated |
| 3 | Enter Description: "Duplicate attempt" | Field populated |
| 4 | Click "Save" | Error: accommodation type name must be unique / already exists |

| Field | Value |
|-------|-------|
| **Postconditions** | Duplicate prevented; uniqueness constraint enforced |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-043: Create Accommodation Type with Description > 500 Characters

| Field | Value |
|-------|-------|
| **ID** | TC-REF-043 |
| **Module** | Reference Data Management |
| **Priority** | Medium |
| **Preconditions** | Tenant Admin logged in. |
| **Test Data** | Name: "Test Type", Description: 501-character string |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Accommodation Type" | Create form appears |
| 2 | Enter Name: "Test Type" | Field populated |
| 3 | Enter a description with 501 characters | Field populated |
| 4 | Click "Save" | Validation error: description exceeds maximum length of 500 characters |

| Field | Value |
|-------|-------|
| **Postconditions** | No accommodation type created; description max length enforced |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-044: Toggle Accommodation Type isActive Off

| Field | Value |
|-------|-------|
| **ID** | TC-REF-044 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Tenant Admin logged in. Accommodation type "Studio Apartment" exists with isActive=true. |
| **Test Data** | Accommodation Type: "Studio Apartment" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/accommodation-types | Accommodation types list displayed |
| 2 | Locate "Studio Apartment" — verify it is active | Active indicator shown |
| 3 | Toggle isActive off (uncheck or click toggle) | Toggle set to inactive |
| 4 | Save the change (if form-based) | Success message; type now shows as inactive |
| 5 | Navigate to a view that shows only active accommodation types (e.g., student application form) | Active types list loaded |
| 6 | Verify "Studio Apartment" does NOT appear in active types | Type absent from active list |

| Field | Value |
|-------|-------|
| **Postconditions** | Accommodation type inactive; hidden from active type lists |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-045: Toggle Accommodation Type isActive On

| Field | Value |
|-------|-------|
| **ID** | TC-REF-045 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Tenant Admin logged in. Accommodation type "Studio Apartment" exists with isActive=false (from TC-REF-044). |
| **Test Data** | Accommodation Type: "Studio Apartment" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/accommodation-types | Accommodation types list displayed |
| 2 | Locate "Studio Apartment" — verify it is inactive | Inactive indicator shown |
| 3 | Toggle isActive on (check or click toggle) | Toggle set to active |
| 4 | Save the change | Success message; type now shows as active |
| 5 | Navigate to a view that shows active accommodation types | Active types list loaded |
| 6 | Verify "Studio Apartment" now appears in active types | Type present in active list |

| Field | Value |
|-------|-------|
| **Postconditions** | Accommodation type active again; visible in active type lists |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-046: Update Accommodation Type

| Field | Value |
|-------|-------|
| **ID** | TC-REF-046 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Tenant Admin logged in. Accommodation type "Studio Apartment" exists. |
| **Test Data** | Updated Description: "Fully furnished studio with private bathroom and kitchenette" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/accommodation-types | Accommodation types list displayed |
| 2 | Click "Edit" on "Studio Apartment" | Edit form loads with current values |
| 3 | Verify fields are pre-populated | Name and description shown |
| 4 | Change Description to "Fully furnished studio with private bathroom and kitchenette" | Description updated |
| 5 | Click "Save" | Success message displayed |
| 6 | Verify updated description is shown | New description displayed |

| Field | Value |
|-------|-------|
| **Postconditions** | Accommodation type description updated |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-047: Delete Accommodation Type

| Field | Value |
|-------|-------|
| **ID** | TC-REF-047 |
| **Module** | Reference Data Management |
| **Priority** | High |
| **Preconditions** | Tenant Admin logged in. Accommodation type "Studio Apartment" exists and is not referenced by any application. |
| **Test Data** | Accommodation Type: "Studio Apartment" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to /admin/accommodation-types | Accommodation types list displayed |
| 2 | Locate "Studio Apartment" | Entry found |
| 3 | Click "Delete" | Confirmation dialog appears |
| 4 | Confirm deletion | Entry removed; success message displayed |
| 5 | Verify "Studio Apartment" no longer in list | Entry absent |

| Field | Value |
|-------|-------|
| **Postconditions** | Accommodation type deleted |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## Access Control

---

### TC-REF-048: Tenant Admin Can Manage Reference Data

| Field | Value |
|-------|-------|
| **ID** | TC-REF-048 |
| **Module** | Reference Data Management |
| **Priority** | Critical |
| **Preconditions** | Tenant Admin account exists with ADMIN role. |
| **Test Data** | Tenant Admin: tenantadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin (tenantadmin@test.com / Admin@12345) | Admin dashboard loads |
| 2 | Navigate to /admin/institutions | Institutions page loads; CRUD operations available |
| 3 | Navigate to /admin/year-of-study | Year of study page loads; CRUD operations available |
| 4 | Navigate to /admin/academic-years | Academic years page loads; years listed read-only, with no add, edit, set-current or delete actions |
| 5 | Navigate to /admin/accommodation-types | Accommodation types page loads; CRUD operations available |
| 6 | Verify all create, edit, delete actions are accessible | Buttons/actions enabled and functional |

| Field | Value |
|-------|-------|
| **Postconditions** | Tenant Admin has full CRUD access to all reference data |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-REF-049: Student Cannot Access Admin Reference Data Pages

| Field | Value |
|-------|-------|
| **ID** | TC-REF-049 |
| **Module** | Reference Data Management |
| **Priority** | Critical |
| **Preconditions** | Student A account exists with STUDENT role. |
| **Test Data** | Student: john.doe@test.com / Student@123 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A (john.doe@test.com / Student@123) | Student dashboard loads |
| 2 | Manually navigate to /admin/institutions in the URL bar | Access denied / redirected to student dashboard / 403 error |
| 3 | Manually navigate to /admin/year-of-study | Access denied / redirected |
| 4 | Manually navigate to /admin/academic-years | Access denied / redirected |
| 5 | Manually navigate to /admin/accommodation-types | Access denied / redirected |
| 6 | Verify no admin menu items are visible in the student navigation | Admin links absent from nav |

| Field | Value |
|-------|-------|
| **Postconditions** | Student cannot access any admin reference data pages |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Verify both frontend route guard and backend API authorization |
