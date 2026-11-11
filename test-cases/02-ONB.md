# Student Onboarding — UAT Test Cases

> **Module:** Student Onboarding
> **Application:** Student Accommodation System
> **Frontend:** Angular 21 — http://localhost:4200
> **Backend:** Spring Boot — http://localhost:8080
> **Email Testing:** MailHog — http://localhost:8025
> **Version:** 1.0
> **Last Updated:** 2026-05-17

---

## Test Data Reference

| Item | Value |
|------|-------|
| New student email | `newstudent@test.com` |
| Password | `Student@123` |
| Student number | `STU004` |
| Valid phone | `0712345678` (10 digits) |
| Invalid phone (short) | `071234567` (9 digits) |
| Invalid phone (long) | `07123456789` (11 digits) |
| Date of birth | `2000-01-15` |
| Institutions | University of Cape Town, Stellenbosch University, Wits University |
| Years of study | 1st Year, 2nd Year, 3rd Year, 4th Year, Postgraduate |
| Gender options | Male, Female, Other, Prefer not to say |
| Buildings | Residence Hall Alpha, Residence Hall Beta, Residence Hall Gamma |
| Required documents | National ID, Proof of Enrollment, Proof of Funding |
| Optional documents | Medical Certificate |

---

## A. Profile Setup (`/profile-setup`)

### TC-ONB-001: Complete profile setup with all valid data

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-001 |
| **Module** | Student Onboarding |
| **Priority** | Critical |
| **Preconditions** | User `newstudent@test.com` is registered, email verified, logged in. No student profile exists yet. |
| **Test Data** | Student number: `STU004`, Phone: `0712345678`, DOB: `2000-01-15`, Institution: `University of Cape Town`, Year: `1st Year`, Gender: `Male`, Special requirements: (empty) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/profile-setup` | Profile setup form loads with all fields empty |
| 2 | Enter `STU004` in Student Number field | Field accepts input, no error shown |
| 3 | Enter `0712345678` in Phone Number field | Field accepts input, no error shown |
| 4 | Select `2000-01-15` in Date of Birth picker | Date is selected and displayed |
| 5 | Select `University of Cape Town` from Institution dropdown | Option is selected |
| 6 | Select `1st Year` from Year of Study dropdown | Option is selected |
| 7 | Select `Male` from Gender options | Option is selected |
| 8 | Leave Special Requirements empty | No validation error |
| 9 | Click **Submit** / **Save Profile** button | Success message displayed; user redirected to `/document-upload` |

| Field | Value |
|-------|-------|
| **Postconditions** | Student profile created in database with all entered values. User is on document upload page. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ONB-002: Submit profile with empty student number

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-002 |
| **Module** | Student Onboarding |
| **Priority** | High |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified. On `/profile-setup`. |
| **Test Data** | Student number: (empty), Phone: `0712345678`, DOB: `2000-01-15`, Institution: `University of Cape Town`, Year: `1st Year`, Gender: `Male` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/profile-setup` | Profile setup form loads |
| 2 | Leave Student Number field empty | — |
| 3 | Fill all other required fields with valid test data | All other fields populated |
| 4 | Click **Submit** | Form does NOT submit. Validation error displayed on Student Number field: "Student number is required" or equivalent |

| Field | Value |
|-------|-------|
| **Postconditions** | No profile created. User remains on `/profile-setup`. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ONB-003: Submit profile with empty phone number

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-003 |
| **Module** | Student Onboarding |
| **Priority** | High |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified. On `/profile-setup`. |
| **Test Data** | Student number: `STU004`, Phone: (empty), DOB: `2000-01-15`, Institution: `University of Cape Town`, Year: `1st Year`, Gender: `Male` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/profile-setup` | Profile setup form loads |
| 2 | Enter `STU004` in Student Number | Field accepts input |
| 3 | Leave Phone Number field empty | — |
| 4 | Fill all other required fields with valid test data | All other fields populated |
| 5 | Click **Submit** | Form does NOT submit. Validation error on Phone Number: "Phone number is required" or equivalent |

| Field | Value |
|-------|-------|
| **Postconditions** | No profile created. User remains on `/profile-setup`. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ONB-004: Submit profile with 9-digit phone number

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-004 |
| **Module** | Student Onboarding |
| **Priority** | High |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified. On `/profile-setup`. |
| **Test Data** | Student number: `STU004`, Phone: `071234567` (9 digits), DOB: `2000-01-15`, Institution: `University of Cape Town`, Year: `1st Year`, Gender: `Male` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/profile-setup` | Profile setup form loads |
| 2 | Enter `071234567` in Phone Number field | Field accepts input |
| 3 | Fill all other required fields with valid test data | All other fields populated |
| 4 | Click **Submit** | Form does NOT submit. Validation error on Phone Number: "Phone number must be exactly 10 digits" or equivalent pattern error |

| Field | Value |
|-------|-------|
| **Postconditions** | No profile created. User remains on `/profile-setup`. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ONB-005: Submit profile with 11-digit phone number

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-005 |
| **Module** | Student Onboarding |
| **Priority** | High |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified. On `/profile-setup`. |
| **Test Data** | Student number: `STU004`, Phone: `07123456789` (11 digits), DOB: `2000-01-15`, Institution: `University of Cape Town`, Year: `1st Year`, Gender: `Male` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/profile-setup` | Profile setup form loads |
| 2 | Enter `07123456789` in Phone Number field | Field accepts input |
| 3 | Fill all other required fields with valid test data | All other fields populated |
| 4 | Click **Submit** | Form does NOT submit. Validation error on Phone Number: "Phone number must be exactly 10 digits" or equivalent pattern error |

| Field | Value |
|-------|-------|
| **Postconditions** | No profile created. User remains on `/profile-setup`. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ONB-006: Submit profile with letters in phone number

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-006 |
| **Module** | Student Onboarding |
| **Priority** | Medium |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified. On `/profile-setup`. |
| **Test Data** | Student number: `STU004`, Phone: `071234abcd`, DOB: `2000-01-15`, Institution: `University of Cape Town`, Year: `1st Year`, Gender: `Male` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/profile-setup` | Profile setup form loads |
| 2 | Enter `071234abcd` in Phone Number field | Input is either rejected (field allows digits only) or accepted for validation |
| 3 | Fill all other required fields with valid test data | All other fields populated |
| 4 | Click **Submit** | Form does NOT submit. Validation error on Phone Number indicating invalid format (digits only) |

| Field | Value |
|-------|-------|
| **Postconditions** | No profile created. User remains on `/profile-setup`. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Verify whether the field masks non-digit input or validates on submit. |

---

### TC-ONB-007: Submit profile with special characters in phone number

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-007 |
| **Module** | Student Onboarding |
| **Priority** | Medium |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified. On `/profile-setup`. |
| **Test Data** | Student number: `STU004`, Phone: `+27-71-234`, DOB: `2000-01-15`, Institution: `University of Cape Town`, Year: `1st Year`, Gender: `Male` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/profile-setup` | Profile setup form loads |
| 2 | Enter `+27-71-234` in Phone Number field | Input is either rejected or accepted for validation |
| 3 | Fill all other required fields with valid test data | All other fields populated |
| 4 | Click **Submit** | Form does NOT submit. Validation error on Phone Number indicating invalid format |

| Field | Value |
|-------|-------|
| **Postconditions** | No profile created. User remains on `/profile-setup`. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ONB-008: Submit profile without date of birth

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-008 |
| **Module** | Student Onboarding |
| **Priority** | High |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified. On `/profile-setup`. |
| **Test Data** | Student number: `STU004`, Phone: `0712345678`, DOB: (empty), Institution: `University of Cape Town`, Year: `1st Year`, Gender: `Male` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/profile-setup` | Profile setup form loads |
| 2 | Fill Student Number, Phone, Institution, Year of Study, Gender with valid data | Fields populated |
| 3 | Leave Date of Birth empty / unselected | — |
| 4 | Click **Submit** | Form does NOT submit. Validation error on Date of Birth: "Date of birth is required" or equivalent |

| Field | Value |
|-------|-------|
| **Postconditions** | No profile created. User remains on `/profile-setup`. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ONB-009: Submit profile without selecting institution

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-009 |
| **Module** | Student Onboarding |
| **Priority** | High |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified. On `/profile-setup`. |
| **Test Data** | Student number: `STU004`, Phone: `0712345678`, DOB: `2000-01-15`, Institution: (none selected), Year: `1st Year`, Gender: `Male` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/profile-setup` | Profile setup form loads |
| 2 | Fill all required fields except Institution | Fields populated |
| 3 | Leave Institution dropdown at default (no selection) | — |
| 4 | Click **Submit** | Form does NOT submit. Validation error on Institution: "Institution is required" or equivalent |

| Field | Value |
|-------|-------|
| **Postconditions** | No profile created. User remains on `/profile-setup`. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ONB-010: Submit profile without selecting year of study

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-010 |
| **Module** | Student Onboarding |
| **Priority** | High |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified. On `/profile-setup`. |
| **Test Data** | Student number: `STU004`, Phone: `0712345678`, DOB: `2000-01-15`, Institution: `University of Cape Town`, Year: (none selected), Gender: `Male` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/profile-setup` | Profile setup form loads |
| 2 | Fill all required fields except Year of Study | Fields populated |
| 3 | Leave Year of Study dropdown at default (no selection) | — |
| 4 | Click **Submit** | Form does NOT submit. Validation error on Year of Study: "Year of study is required" or equivalent |

| Field | Value |
|-------|-------|
| **Postconditions** | No profile created. User remains on `/profile-setup`. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ONB-011: Submit profile without selecting gender

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-011 |
| **Module** | Student Onboarding |
| **Priority** | High |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified. On `/profile-setup`. |
| **Test Data** | Student number: `STU004`, Phone: `0712345678`, DOB: `2000-01-15`, Institution: `University of Cape Town`, Year: `1st Year`, Gender: (none selected) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/profile-setup` | Profile setup form loads |
| 2 | Fill all required fields except Gender | Fields populated |
| 3 | Leave Gender at default (no selection) | — |
| 4 | Click **Submit** | Form does NOT submit. Validation error on Gender: "Gender is required" or equivalent |

| Field | Value |
|-------|-------|
| **Postconditions** | No profile created. User remains on `/profile-setup`. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ONB-012: Verify institutions dropdown is populated correctly

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-012 |
| **Module** | Student Onboarding |
| **Priority** | Medium |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified. On `/profile-setup`. Institutions seeded in database. |
| **Test Data** | Expected institutions: `University of Cape Town`, `Stellenbosch University`, `Wits University` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/profile-setup` | Profile setup form loads |
| 2 | Click on the Institution dropdown to expand it | Dropdown opens showing list of institutions |
| 3 | Verify listed institutions | Dropdown contains at minimum: `University of Cape Town`, `Stellenbosch University`, `Wits University` |
| 4 | Select `Stellenbosch University` | Option is selected and displayed in the field |

| Field | Value |
|-------|-------|
| **Postconditions** | Institution dropdown verified as populated. No form submission. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Verify values are fetched from backend API, not hardcoded. |

---

### TC-ONB-013: Verify years of study dropdown is populated correctly

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-013 |
| **Module** | Student Onboarding |
| **Priority** | Medium |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified. On `/profile-setup`. Years of study seeded in database. |
| **Test Data** | Expected values: `1st Year`, `2nd Year`, `3rd Year`, `4th Year`, `Postgraduate` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/profile-setup` | Profile setup form loads |
| 2 | Click on the Year of Study dropdown to expand it | Dropdown opens showing list of years |
| 3 | Verify listed years | Dropdown contains: `1st Year`, `2nd Year`, `3rd Year`, `4th Year`, `Postgraduate` |
| 4 | Select `3rd Year` | Option is selected and displayed in the field |

| Field | Value |
|-------|-------|
| **Postconditions** | Year of Study dropdown verified as populated. No form submission. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ONB-014: Submit profile with special requirements filled

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-014 |
| **Module** | Student Onboarding |
| **Priority** | Medium |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified. On `/profile-setup`. |
| **Test Data** | Student number: `STU004`, Phone: `0712345678`, DOB: `2000-01-15`, Institution: `University of Cape Town`, Year: `1st Year`, Gender: `Female`, Special requirements: `I require a ground-floor room due to mobility impairment. Wheelchair accessible bathroom needed.` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/profile-setup` | Profile setup form loads |
| 2 | Fill all required fields with valid test data | Fields populated |
| 3 | Enter `I require a ground-floor room due to mobility impairment. Wheelchair accessible bathroom needed.` in Special Requirements textarea | Text accepted |
| 4 | Click **Submit** | Success message displayed; user redirected to `/document-upload`. Special requirements saved in database. |

| Field | Value |
|-------|-------|
| **Postconditions** | Student profile created with special requirements text stored. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Verify the special requirements text is persisted by checking via API or admin panel. |

---

### TC-ONB-015: Submit profile with special requirements empty (should succeed)

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-015 |
| **Module** | Student Onboarding |
| **Priority** | Medium |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified. On `/profile-setup`. |
| **Test Data** | Student number: `STU004`, Phone: `0712345678`, DOB: `2000-01-15`, Institution: `Wits University`, Year: `2nd Year`, Gender: `Prefer not to say`, Special requirements: (empty) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/profile-setup` | Profile setup form loads |
| 2 | Fill all required fields with valid test data | Fields populated |
| 3 | Leave Special Requirements textarea empty | No validation error shown |
| 4 | Click **Submit** | Success message displayed; user redirected to `/document-upload` |

| Field | Value |
|-------|-------|
| **Postconditions** | Student profile created. Special requirements field is null/empty in database. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## B. Document Upload (`/document-upload`)

### TC-ONB-016: Upload all required documents successfully

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-016 |
| **Module** | Student Onboarding |
| **Priority** | Critical |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified, profile setup completed. On `/document-upload`. |
| **Test Data** | Files: `national_id.pdf` (< 5 MB), `proof_of_enrollment.pdf` (< 5 MB), `proof_of_funding.pdf` (< 5 MB) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/document-upload` | Document upload page loads showing required document types: National ID, Proof of Enrollment, Proof of Funding |
| 2 | Click upload area/button for **National ID** | File picker opens |
| 3 | Select `national_id.pdf` and confirm | File uploads; progress indicator shown; upload completes with success indicator |
| 4 | Click upload area/button for **Proof of Enrollment** | File picker opens |
| 5 | Select `proof_of_enrollment.pdf` and confirm | File uploads successfully |
| 6 | Click upload area/button for **Proof of Funding** | File picker opens |
| 7 | Select `proof_of_funding.pdf` and confirm | File uploads successfully |
| 8 | Verify all three documents show as uploaded | Each document displays filename, upload date, and status `PENDING` |
| 9 | Click **Continue** / **Next** | User is redirected to `/application-submission` |

| Field | Value |
|-------|-------|
| **Postconditions** | Three documents stored in MinIO. Document records in database with status `PENDING`. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Verify files exist in MinIO via console at http://localhost:9001. |

---

### TC-ONB-017: Upload a single required document

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-017 |
| **Module** | Student Onboarding |
| **Priority** | High |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified, profile completed. On `/document-upload`. |
| **Test Data** | File: `national_id.pdf` (< 5 MB) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/document-upload` | Document upload page loads |
| 2 | Click upload area/button for **National ID** | File picker opens |
| 3 | Select `national_id.pdf` and confirm | File uploads successfully; document appears in uploaded list with status `PENDING` |
| 4 | Verify other required documents still show as not uploaded | Proof of Enrollment and Proof of Funding show as pending upload |

| Field | Value |
|-------|-------|
| **Postconditions** | One document stored. Two required documents still outstanding. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ONB-018: Attempt to proceed without uploading required documents

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-018 |
| **Module** | Student Onboarding |
| **Priority** | Critical |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified, profile completed. On `/document-upload`. No documents uploaded. |
| **Test Data** | N/A |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/document-upload` | Document upload page loads showing required documents |
| 2 | Do NOT upload any documents | All required documents show as not uploaded |
| 3 | Click **Continue** / **Next** button | Button is disabled OR error message displayed: "Please upload all required documents" or equivalent. User is NOT redirected. |

| Field | Value |
|-------|-------|
| **Postconditions** | User remains on `/document-upload`. No navigation to application submission. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Verify the Continue button is disabled when required documents are missing. |

---

### TC-ONB-019: Upload optional document (Medical Certificate)

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-019 |
| **Module** | Student Onboarding |
| **Priority** | Medium |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified, profile completed. On `/document-upload`. All required documents already uploaded. |
| **Test Data** | File: `medical_certificate.pdf` (< 5 MB) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/document-upload` | Document upload page loads; required documents show as uploaded |
| 2 | Locate Medical Certificate upload area (marked as optional) | Optional document section is visible |
| 3 | Click upload area/button for **Medical Certificate** | File picker opens |
| 4 | Select `medical_certificate.pdf` and confirm | File uploads successfully; document appears with status `PENDING` |
| 5 | Verify document list shows 4 documents (3 required + 1 optional) | All four documents listed with correct types and `PENDING` status |

| Field | Value |
|-------|-------|
| **Postconditions** | Four documents stored in MinIO. Medical Certificate record in database with status `PENDING`. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ONB-020: Upload file with invalid type

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-020 |
| **Module** | Student Onboarding |
| **Priority** | High |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified, profile completed. On `/document-upload`. |
| **Test Data** | File: `malicious_script.exe` (invalid file type) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/document-upload` | Document upload page loads |
| 2 | Click upload area/button for **National ID** | File picker opens |
| 3 | Select `malicious_script.exe` and confirm | Upload is rejected. Error message displayed: "Invalid file type" or equivalent. Only allowed types (PDF, JPG, PNG, etc.) accepted. |
| 4 | Verify no document record was created | Document list does not show the rejected file |

| Field | Value |
|-------|-------|
| **Postconditions** | No file stored in MinIO. No document record in database. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Verify both frontend file picker filter and backend validation reject the file. |

---

### TC-ONB-021: Verify uploaded documents appear in list with PENDING status

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-021 |
| **Module** | Student Onboarding |
| **Priority** | High |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified, profile completed. All three required documents uploaded. |
| **Test Data** | Previously uploaded: `national_id.pdf`, `proof_of_enrollment.pdf`, `proof_of_funding.pdf` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/document-upload` | Document upload page loads |
| 2 | Review the uploaded documents list | All three documents are displayed |
| 3 | Verify each document shows: filename, document type, upload date, and status | Each document shows its original filename, correct type (National ID / Proof of Enrollment / Proof of Funding), upload timestamp, and status badge showing `PENDING` |
| 4 | Verify via backend API: `GET http://localhost:8080/api/documents` (with auth token) | API returns 3 document records, each with `status: "PENDING"` |

| Field | Value |
|-------|-------|
| **Postconditions** | Documents confirmed visible and in PENDING status. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## C. Application Submission (`/application-submission`)

### TC-ONB-022: Submit application with all fields filled

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-022 |
| **Module** | Student Onboarding |
| **Priority** | Critical |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified, profile completed, all required documents uploaded. On `/application-submission`. |
| **Test Data** | Course name: `BSc Computer Science`, Accessibility: unchecked, Preferred buildings: `Residence Hall Alpha`, `Residence Hall Beta`, Special requirements: `Quiet floor preferred` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/application-submission` | Application form loads with empty fields |
| 2 | Enter `BSc Computer Science` in Course Name field | Field accepts input |
| 3 | Leave **Requires Accessibility** checkbox unchecked | Checkbox remains unchecked (default) |
| 4 | Select `Residence Hall Alpha` and `Residence Hall Beta` from Preferred Buildings | Both buildings selected and shown as selected |
| 5 | Enter `Quiet floor preferred` in Special Requirements textarea | Text accepted |
| 6 | Tick **I confirm that all the information provided is accurate and complete**, then click **Continue to Review** | Profile and application are saved on the server; success message displayed; user redirected to `/application-review` showing the saved application |

| Field | Value |
|-------|-------|
| **Postconditions** | Application record created in database with status `SUBMITTED` or `PENDING`. Course name, building preferences, and special requirements persisted. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ONB-023: Submit application with empty course name

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-023 |
| **Module** | Student Onboarding |
| **Priority** | High |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified, profile completed, required documents uploaded. On `/application-submission`. |
| **Test Data** | Course name: (empty), Accessibility: unchecked, Preferred buildings: `Residence Hall Alpha` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/application-submission` | Application form loads |
| 2 | Leave Course Name field empty | — |
| 3 | Select `Residence Hall Alpha` from Preferred Buildings | Building selected |
| 4 | Click **Submit Application** | Form does NOT submit. Validation error on Course Name: "Course name is required" or equivalent |

| Field | Value |
|-------|-------|
| **Postconditions** | No application created. User remains on `/application-submission`. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ONB-024: Submit application with accessibility checkbox checked

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-024 |
| **Module** | Student Onboarding |
| **Priority** | High |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified, profile completed, required documents uploaded. On `/application-submission`. |
| **Test Data** | Course name: `BA Law`, Accessibility: checked (true), Preferred buildings: `Residence Hall Gamma`, Special requirements: `Wheelchair ramp access required` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/application-submission` | Application form loads |
| 2 | Enter `BA Law` in Course Name | Field accepts input |
| 3 | Check the **Requires Accessibility** checkbox | Checkbox becomes checked |
| 4 | Select `Residence Hall Gamma` from Preferred Buildings | Building selected |
| 5 | Enter `Wheelchair ramp access required` in Special Requirements | Text accepted |
| 6 | Click **Submit Application** | Success message; user redirected. Application saved with `requiresAccessibility = true` |

| Field | Value |
|-------|-------|
| **Postconditions** | Application created with `requires_accessibility = true` in database. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Verify via API that the accessibility flag is persisted correctly. |

---

### TC-ONB-025: Submit application selecting multiple preferred buildings

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-025 |
| **Module** | Student Onboarding |
| **Priority** | High |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified, profile completed, required documents uploaded. On `/application-submission`. |
| **Test Data** | Course name: `BSc Engineering`, Preferred buildings: `Residence Hall Alpha`, `Residence Hall Beta`, `Residence Hall Gamma` (all three) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/application-submission` | Application form loads |
| 2 | Enter `BSc Engineering` in Course Name | Field accepts input |
| 3 | Select all three buildings: `Residence Hall Alpha`, `Residence Hall Beta`, `Residence Hall Gamma` | All three shown as selected |
| 4 | Click **Submit Application** | Success message; application submitted with all three building IDs stored as preferences |

| Field | Value |
|-------|-------|
| **Postconditions** | Application created with three preferred building IDs linked. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ONB-026: Submit application with no building preferences (should succeed)

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-026 |
| **Module** | Student Onboarding |
| **Priority** | Medium |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified, profile completed, required documents uploaded. On `/application-submission`. |
| **Test Data** | Course name: `BCom Accounting`, Preferred buildings: (none selected), Accessibility: unchecked |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/application-submission` | Application form loads |
| 2 | Enter `BCom Accounting` in Course Name | Field accepts input |
| 3 | Do NOT select any preferred buildings | No buildings selected |
| 4 | Click **Submit Application** | Success message; application submitted. Building preferences stored as empty list. |

| Field | Value |
|-------|-------|
| **Postconditions** | Application created with no building preferences. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ONB-027: Submit application with special requirements text

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-027 |
| **Module** | Student Onboarding |
| **Priority** | Medium |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified, profile completed, required documents uploaded. On `/application-submission`. |
| **Test Data** | Course name: `BSc Mathematics`, Special requirements: `I have a severe nut allergy. Please ensure the accommodation kitchen area is nut-free. I also require proximity to the campus medical centre.` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/application-submission` | Application form loads |
| 2 | Enter `BSc Mathematics` in Course Name | Field accepts input |
| 3 | Enter the special requirements text (see test data) in Special Requirements textarea | Text accepted; textarea expands or scrolls to accommodate content |
| 4 | Click **Submit Application** | Success message; application submitted with full special requirements text preserved |

| Field | Value |
|-------|-------|
| **Postconditions** | Application created. Special requirements text stored in full in database. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Verify full text is preserved (no truncation) via API. |

---

### TC-ONB-028: Verify building gallery/selection shows available buildings

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-028 |
| **Module** | Student Onboarding |
| **Priority** | Medium |
| **Preconditions** | User `newstudent@test.com` is logged in, email verified, profile completed, required documents uploaded. On `/application-submission`. Buildings seeded in database. |
| **Test Data** | Expected buildings: `Residence Hall Alpha`, `Residence Hall Beta`, `Residence Hall Gamma` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/application-submission` | Application form loads |
| 2 | Locate the Preferred Buildings selection area | Building selection UI is visible (multi-select, card gallery, or checkbox list) |
| 3 | Verify listed buildings | At minimum: `Residence Hall Alpha`, `Residence Hall Beta`, `Residence Hall Gamma` are displayed |
| 4 | Verify each building shows relevant details | Building name is displayed; optionally images, address, or room count |

| Field | Value |
|-------|-------|
| **Postconditions** | Building list verified. No form submission. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Verify buildings are fetched from backend API. |

---

## D. Post-Submission

### TC-ONB-029: After submission — redirected to application review page

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-029 |
| **Module** | Student Onboarding |
| **Priority** | Critical |
| **Preconditions** | User `newstudent@test.com` has just successfully submitted an application via TC-ONB-022 or equivalent. |
| **Test Data** | N/A |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Complete application submission (per TC-ONB-022) | Success message appears |
| 2 | Observe browser URL after redirect | URL changes to `/application-review?ids={id}` |
| 3 | Verify the page content | Page shows the saved application read-only (course name, building preferences, accessibility, special requirements); no submit button or terms checkbox |
| 4 | Refresh the page | The same saved details are shown again |
| 5 | Click **Finish** | Redirected to `/application-confirmation/{id}` |

| Field | Value |
|-------|-------|
| **Postconditions** | User is on the application review/confirmation page. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ONB-030: Application confirmation page shows correct details

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-030 |
| **Module** | Student Onboarding |
| **Priority** | Critical |
| **Preconditions** | User `newstudent@test.com` has submitted application with: Course: `BSc Computer Science`, Buildings: `Residence Hall Alpha` & `Residence Hall Beta`, Accessibility: false, Special requirements: `Quiet floor preferred`. |
| **Test Data** | Application submitted per TC-ONB-022 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to the application confirmation page (redirect from submission or via `/application-confirmation/{id}`) | Confirmation page loads |
| 2 | Verify course name | Displays `BSc Computer Science` |
| 3 | Verify preferred buildings | Displays `Residence Hall Alpha` and `Residence Hall Beta` |
| 4 | Verify accessibility | Shows as `No` / unchecked / not required |
| 5 | Verify special requirements | Displays `Quiet floor preferred` |
| 6 | Verify application status | Status shown as `PENDING` or `SUBMITTED` |
| 7 | Verify student details section (if shown) | Student number `STU004`, institution, year of study match profile data |

| Field | Value |
|-------|-------|
| **Postconditions** | All displayed data matches submitted values. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-ONB-031: Application status is PENDING after submission

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-031 |
| **Module** | Student Onboarding |
| **Priority** | Critical |
| **Preconditions** | User `newstudent@test.com` has submitted an application. |
| **Test Data** | N/A |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | After application submission, note the application ID from the confirmation page or URL | Application ID captured |
| 2 | Verify status on the confirmation/review page | Status badge/label shows `PENDING` or `SUBMITTED` |
| 3 | Verify via backend API: `GET http://localhost:8080/api/applications/{id}` (with auth token) | Response JSON contains `"status": "SUBMITTED"` or `"status": "PENDING"` |
| 4 | Check the student dashboard at `http://localhost:4200/dashboard` | Application appears in the "My Applications" section with status `PENDING` / `SUBMITTED` |

| Field | Value |
|-------|-------|
| **Postconditions** | Application confirmed as PENDING/SUBMITTED in both UI and API. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## E. Full End-to-End Onboarding

### TC-ONB-032: Complete onboarding flow — signup through confirmation

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-032 |
| **Module** | Student Onboarding |
| **Priority** | Critical |
| **Preconditions** | No account exists for `newstudent@test.com`. MailHog running at http://localhost:8025. All reference data (institutions, years of study, buildings) seeded. |
| **Test Data** | Email: `newstudent@test.com`, Password: `Student@123`, Student number: `STU004`, Phone: `0712345678`, DOB: `2000-01-15`, Institution: `University of Cape Town`, Year: `1st Year`, Gender: `Male`, Course: `BSc Computer Science`, Buildings: `Residence Hall Alpha`, Documents: `national_id.pdf`, `proof_of_enrollment.pdf`, `proof_of_funding.pdf` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://localhost:4200/signup` | Signup page loads (ref: TC-AUTH-001) |
| 2 | Register with `newstudent@test.com` / `Student@123` | Account created; verification email sent |
| 3 | Open MailHog at `http://localhost:8025` | Verification email received for `newstudent@test.com` |
| 4 | Click the verification link in the email | Email verified; redirected to login or profile setup |
| 5 | Log in with `newstudent@test.com` / `Student@123` | Login successful; redirected to `/profile-setup` |
| 6 | Fill profile: `STU004`, `0712345678`, `2000-01-15`, `University of Cape Town`, `1st Year`, `Male` | All fields populated |
| 7 | Click **Submit** on profile form | Profile saved; redirected to `/document-upload` |
| 8 | Upload `national_id.pdf` for National ID | Upload successful |
| 9 | Upload `proof_of_enrollment.pdf` for Proof of Enrollment | Upload successful |
| 10 | Upload `proof_of_funding.pdf` for Proof of Funding | Upload successful |
| 11 | Click **Continue** to proceed | Redirected to `/application-submission` |
| 12 | Enter `BSc Computer Science` as course name | Field populated |
| 13 | Select `Residence Hall Alpha` as preferred building | Building selected |
| 14 | Tick the confirmation checkbox and click **Continue to Review** | Application submitted; redirected to `/application-review`, then **Finish** goes to the confirmation page |
| 15 | Verify confirmation page shows all correct details | Course, building, status (PENDING) all displayed correctly |
| 16 | Navigate to `http://localhost:4200/dashboard` | Student dashboard loads showing the submitted application |

| Field | Value |
|-------|-------|
| **Postconditions** | Full student record exists: user account (verified), student profile, 3 documents (PENDING), 1 application (SUBMITTED/PENDING). |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | This is the critical happy-path E2E test. If this fails, the entire onboarding flow is broken. |

---

## F. Navigation Guards

### TC-ONB-033: Access /profile-setup without login — redirected to /login

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-033 |
| **Module** | Student Onboarding |
| **Priority** | Critical |
| **Preconditions** | User is NOT logged in (no JWT token in localStorage). Browser session is clean / incognito. |
| **Test Data** | N/A |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open a new browser window / incognito tab | Clean session with no auth state |
| 2 | Navigate directly to `http://localhost:4200/profile-setup` | AuthGuard intercepts the route |
| 3 | Observe the redirect | User is redirected to `http://localhost:4200/login` |
| 4 | Verify the login page loads | Login form is displayed |
| 5 | Verify URL | URL is `/login` (possibly with a `returnUrl` query parameter) |

| Field | Value |
|-------|-------|
| **Postconditions** | User is on login page. `/profile-setup` was not accessible. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Also verify for `/document-upload`, `/application-submission`, and `/application-confirmation/1`. |

---

### TC-ONB-034: Access /profile-setup as admin — redirected appropriately

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-034 |
| **Module** | Student Onboarding |
| **Priority** | High |
| **Preconditions** | An admin user is logged in (role: ADMIN). JWT token present in localStorage. |
| **Test Data** | Admin credentials (use existing admin account) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as an admin user | Admin dashboard or admin home page loads |
| 2 | Navigate directly to `http://localhost:4200/profile-setup` | StudentGuard intercepts the route |
| 3 | Observe the redirect | User is redirected away from `/profile-setup` (to admin dashboard, `/unauthorized`, or `/login`) |
| 4 | Verify the page content | Admin is NOT shown the student profile setup form |

| Field | Value |
|-------|-------|
| **Postconditions** | Admin user cannot access student onboarding routes. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | StudentGuard should reject non-STUDENT roles. |

---

### TC-ONB-035: Access /dashboard without verified email — redirected to email verification

| Field | Value |
|-------|-------|
| **ID** | TC-ONB-035 |
| **Module** | Student Onboarding |
| **Priority** | Critical |
| **Preconditions** | User `newstudent@test.com` is registered and logged in but has NOT verified their email address. JWT token present. |
| **Test Data** | Email: `newstudent@test.com` (unverified) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in with `newstudent@test.com` / `Student@123` (email not yet verified) | Login successful; JWT issued |
| 2 | Navigate directly to `http://localhost:4200/dashboard` | EmailVerifiedGuard intercepts the route |
| 3 | Observe the redirect | User is redirected to `/email-verification-required` or equivalent verification prompt page |
| 4 | Verify the page content | Page instructs user to verify their email, possibly with a "Resend verification email" button |

| Field | Value |
|-------|-------|
| **Postconditions** | Unverified user cannot access dashboard. Directed to verify email first. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## Summary

| Section | Test Cases | IDs |
|---------|-----------|-----|
| A. Profile Setup | 15 | TC-ONB-001 to TC-ONB-015 |
| B. Document Upload | 6 | TC-ONB-016 to TC-ONB-021 |
| C. Application Submission | 7 | TC-ONB-022 to TC-ONB-028 |
| D. Post-Submission | 3 | TC-ONB-029 to TC-ONB-031 |
| E. Full End-to-End | 1 | TC-ONB-032 |
| F. Navigation Guards | 3 | TC-ONB-033 to TC-ONB-035 |
| **Total** | **35** | |

---

> **Sign-off:**
>
> | Role | Name | Signature | Date |
> |------|------|-----------|------|
> | QA Lead | _________________ | _________________ | _________________ |
> | Dev Lead | _________________ | _________________ | _________________ |
> | Product Owner | _________________ | _________________ | _________________ |
