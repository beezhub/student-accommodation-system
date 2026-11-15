# Document Management — UAT Test Cases

| Field            | Value                                      |
|------------------|--------------------------------------------|
| **Module**       | Document Management                        |
| **Version**      | 1.0                                        |
| **Date Created** | 2026-05-17                                 |
| **Application**  | Student Accommodation System               |
| **Frontend URL** | http://localhost:4200                       |
| **Backend URL**  | http://localhost:8080                       |
| **File Storage** | MinIO (console: http://localhost:9001)      |
| **Email Testing**| MailHog (http://localhost:8025)             |

## Test Data

| Item              | Value                                                           |
|-------------------|-----------------------------------------------------------------|
| **Student A**     | john.doe@test.com / Student@123                                 |
| **Platform Admin**| platformadmin@test.com / Admin@12345                            |
| **Doc Types**     | National ID (required), Proof of Enrollment (required), Proof of Funding (required), Medical Certificate (optional) |
| **Test Files**    | PDF < 5 MB, JPG image, large file > 10 MB, `.exe` file         |
| **Max Upload**    | 10 MB per file (configured via `spring.servlet.multipart.max-file-size`) |

## Document Statuses

`PENDING` → `APPROVED` | `REJECTED`

## Key Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/documents/upload-single` | POST | Upload single document |
| `/api/documents/upload-multiple` | POST | Upload multiple documents |
| `/api/documents/my` | GET | Student's own documents (paginated, filterable) |
| `/api/documents/download/{id}` | GET | Download document file |
| `/api/documents/info/{id}` | GET | Document info with presigned URL |
| `/api/documents/update-status/{id}` | PUT | Admin: approve/reject |
| `/api/documents/search` | GET | Admin: search all documents |
| `/api/documents/{id}/comments` | GET/POST | Get/add comments |
| `/api/documents/comments/{id}` | DELETE | Delete comment |
| `/api/document-type` | CRUD | Document type management |
| `/api/document-type/my` | GET | Student: types to upload, with the required types of the companies applied to or picked |

---

## A. Student — Upload Documents

---

### TC-DOC-001: Upload single PDF document with document type

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-001 |
| **Module** | Document Management |
| **Priority** | Critical |
| **Preconditions** | Student A is logged in; at least one document type (National ID) exists; student has a complete profile |
| **Test Data** | File: `test-id.pdf` (< 5 MB); Document type: National ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/documents` | Documents page loads with upload section and document list |
| 2 | Click the upload button / drag-and-drop area | File picker dialog opens |
| 3 | Select `test-id.pdf` from local filesystem | File name appears in the upload form |
| 4 | Select "National ID" from document type dropdown | Document type is selected |
| 5 | Click "Upload" / confirm button | Upload progress indicator shows; success snackbar appears |
| 6 | Verify the document appears in the document list | Document row shows: name `test-id.pdf`, type "National ID", status `PENDING`, upload date = today |
| 7 | Open MinIO console → navigate to the documents bucket | Uploaded file exists in MinIO storage under the student's path |

| Field | Value |
|-------|-------|
| **Postconditions** | Document record created with status PENDING; file stored in MinIO |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DOC-002: Upload multiple documents at once

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-002 |
| **Module** | Document Management |
| **Priority** | Critical |
| **Preconditions** | Student A is logged in; document types National ID, Proof of Enrollment, Proof of Funding exist |
| **Test Data** | Files: `id-doc.pdf`, `enrollment.pdf`, `funding.pdf` (each < 5 MB); corresponding document type IDs |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/documents` | Documents page loads |
| 2 | Use the multi-file upload feature (if available via `/upload-multiple` endpoint) | Upload form allows selecting multiple files |
| 3 | Select all three files and assign each a document type | Files and types are listed in the upload form |
| 4 | Submit the upload | Success message displayed; all three documents uploaded |
| 5 | Verify document list shows all three new entries | Each document shows correct name, type, status `PENDING`, and today's date |

| Field | Value |
|-------|-------|
| **Postconditions** | Three document records created, each with status PENDING; three files in MinIO |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DOC-003: Upload document without selecting document type

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-003 |
| **Module** | Document Management |
| **Priority** | High |
| **Preconditions** | Student A is logged in |
| **Test Data** | File: `test-doc.pdf` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/documents` | Documents page loads |
| 2 | Select a file for upload | File appears in upload form |
| 3 | Leave the document type dropdown unselected / empty | No document type chosen |
| 4 | Attempt to submit the upload | Validation error shown: document type is required; upload button is disabled or submission is blocked |
| 5 | Verify no document was created in the list | Document list is unchanged |

| Field | Value |
|-------|-------|
| **Postconditions** | No document record created; no file in MinIO |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Backend requires `documentType` param on `/upload-single` |

---

### TC-DOC-004: Upload file exceeding maximum size limit (> 10 MB)

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-004 |
| **Module** | Document Management |
| **Priority** | High |
| **Preconditions** | Student A is logged in; max upload size is 10 MB (configured in `application.yml`) |
| **Test Data** | File: `large-file.pdf` (> 10 MB); Document type: National ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/documents` | Documents page loads |
| 2 | Select `large-file.pdf` for upload | File appears in upload form |
| 3 | Select "National ID" as document type | Type is selected |
| 4 | Submit the upload | Error message displayed indicating file exceeds maximum allowed size |
| 5 | Verify no document was created | Document list is unchanged; no file in MinIO |

| Field | Value |
|-------|-------|
| **Postconditions** | No document record created; Spring returns `MaxUploadSizeExceededException` |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | `spring.servlet.multipart.max-file-size=10MB` |

---

### TC-DOC-005: Upload file with invalid/restricted file type (.exe)

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-005 |
| **Module** | Document Management |
| **Priority** | High |
| **Preconditions** | Student A is logged in |
| **Test Data** | File: `malware.exe`; Document type: National ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/documents` | Documents page loads |
| 2 | Select `malware.exe` for upload | File appears in form (or is rejected by the file picker `accept` attribute) |
| 3 | Select "National ID" as document type | Type is selected |
| 4 | Submit the upload | Error message: unsupported file type, or the frontend file picker blocks .exe entirely |
| 5 | Verify no document was created | Document list unchanged; no file in MinIO |

| Field | Value |
|-------|-------|
| **Postconditions** | Upload rejected; no record or file created |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | If backend does not restrict content types, document may upload — note the actual behavior |

---

### TC-DOC-006: Upload JPG image as document

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-006 |
| **Module** | Document Management |
| **Priority** | Medium |
| **Preconditions** | Student A is logged in |
| **Test Data** | File: `id-photo.jpg` (< 5 MB); Document type: National ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/documents` | Documents page loads |
| 2 | Select `id-photo.jpg` for upload | File appears in upload form |
| 3 | Select "National ID" as document type | Type is selected |
| 4 | Submit the upload | Upload succeeds; success snackbar shown |
| 5 | Verify document appears in list with status PENDING | Row shows: `id-photo.jpg`, "National ID", `PENDING` |

| Field | Value |
|-------|-------|
| **Postconditions** | JPG document stored in MinIO; record created with PENDING status |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DOC-007: Verify uploaded document appears in student's document list

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-007 |
| **Module** | Document Management |
| **Priority** | Critical |
| **Preconditions** | Student A is logged in and has uploaded at least one document (TC-DOC-001 completed) |
| **Test Data** | Previously uploaded `test-id.pdf` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/documents` | Documents page loads |
| 2 | Observe the document list | Previously uploaded document is visible in the list |
| 3 | Verify displayed fields | Document name, document type name, status (`PENDING`), upload date, and file size are all shown correctly |
| 4 | Refresh the page (F5) | Document persists in the list after reload |

| Field | Value |
|-------|-------|
| **Postconditions** | Document list displays uploaded documents consistently |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Endpoint: `GET /api/documents/my` |

---

## B. Student — View Documents

---

### TC-DOC-008: View list of own documents at /documents

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-008 |
| **Module** | Document Management |
| **Priority** | Critical |
| **Preconditions** | Student A is logged in; has 2+ uploaded documents in varying statuses |
| **Test Data** | Existing documents: 1 PENDING, 1 APPROVED, 1 REJECTED |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/documents` | Documents page loads with paginated table/list |
| 2 | Verify all own documents are listed | All uploaded documents visible; correct count |
| 3 | Verify pagination controls are present (if > 10 docs) | Page size selector and page navigation work |
| 4 | Verify sort defaults to newest first (`createdAt DESC`) | Most recently uploaded document appears first |

| Field | Value |
|-------|-------|
| **Postconditions** | Student sees complete list of own documents |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Uses `GET /api/documents/my` with pagination |

---

### TC-DOC-009: View document details (name, type, status, upload date)

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-009 |
| **Module** | Document Management |
| **Priority** | High |
| **Preconditions** | Student A is logged in; at least one document exists |
| **Test Data** | Existing document: `test-id.pdf`, type National ID, status PENDING |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/documents` | Documents page loads |
| 2 | Click on a document row or its detail/view action | Document viewer opens (DocumentViewerComponent) |
| 3 | Verify document name is displayed | Shows `test-id.pdf` |
| 4 | Verify document type is displayed | Shows "National ID" |
| 5 | Verify status is displayed | Shows `PENDING` with appropriate styling |
| 6 | Verify upload date is displayed | Shows correct date/time of upload |
| 7 | Verify file preview or download option is available | PDF preview renders or download link is present |

| Field | Value |
|-------|-------|
| **Postconditions** | Document details are accurately displayed |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Uses `GET /api/documents/info/{documentId}` with presigned URL |

---

### TC-DOC-010: Download own document

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-010 |
| **Module** | Document Management |
| **Priority** | High |
| **Preconditions** | Student A is logged in; has at least one uploaded document |
| **Test Data** | Existing document: `test-id.pdf` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/documents` | Documents page loads |
| 2 | Click the download action for `test-id.pdf` | Browser starts file download |
| 3 | Verify downloaded file name matches original | File saved as `test-id.pdf` |
| 4 | Open the downloaded file | File content is intact and readable; matches uploaded original |

| Field | Value |
|-------|-------|
| **Postconditions** | File downloaded successfully from MinIO via presigned URL or `/api/documents/download/{id}` |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Response header: `Content-Disposition: attachment; filename="test-id.pdf"` |

---

### TC-DOC-011: View document with PENDING status badge

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-011 |
| **Module** | Document Management |
| **Priority** | Medium |
| **Preconditions** | Student A is logged in; has a document with status PENDING |
| **Test Data** | Newly uploaded document |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/documents` | Documents page loads |
| 2 | Locate the PENDING document in the list | Status badge/chip displays "Pending" |
| 3 | Verify badge styling | Badge uses warning/amber color scheme indicating review pending |

| Field | Value |
|-------|-------|
| **Postconditions** | PENDING status is visually distinguishable |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DOC-012: View document with APPROVED status badge

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-012 |
| **Module** | Document Management |
| **Priority** | Medium |
| **Preconditions** | Student A is logged in; has a document with status APPROVED (admin approved it) |
| **Test Data** | Previously approved document |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/documents` | Documents page loads |
| 2 | Locate the APPROVED document in the list | Status badge/chip displays "Approved" |
| 3 | Verify badge styling | Badge uses success/green color scheme |

| Field | Value |
|-------|-------|
| **Postconditions** | APPROVED status is visually distinguishable |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DOC-013: View document with REJECTED status badge

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-013 |
| **Module** | Document Management |
| **Priority** | Medium |
| **Preconditions** | Student A is logged in; has a document with status REJECTED (admin rejected it) |
| **Test Data** | Previously rejected document |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/documents` | Documents page loads |
| 2 | Locate the REJECTED document in the list | Status badge/chip displays "Rejected" |
| 3 | Verify badge styling | Badge uses error/red color scheme |
| 4 | Verify rejection reason or comment is visible (if displayed inline) | Rejection comment from admin is shown or accessible via detail view |

| Field | Value |
|-------|-------|
| **Postconditions** | REJECTED status is visually distinguishable; student aware of rejection |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DOC-014: Student cannot view another student's documents

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-014 |
| **Module** | Document Management |
| **Priority** | Critical |
| **Preconditions** | Student A is logged in; Student B (different user) has uploaded documents |
| **Test Data** | Student B's document ID (e.g., ID = 999) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/documents` | Only Student A's own documents are listed |
| 2 | Attempt to access `GET /api/documents/info/999` directly (Student B's doc) via browser URL or API | Returns 403 Forbidden or 404 Not Found |
| 3 | Attempt to access `GET /api/documents/download/999` (Student B's doc) | Returns 403 Forbidden or 404 Not Found |
| 4 | Verify no data leakage in the document list | No documents from Student B appear |

| Field | Value |
|-------|-------|
| **Postconditions** | Student A cannot access another student's documents; tenant filter enforced |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Ownership check in DocumentQueryService + Hibernate tenant filter |

---

## C. Student — Resubmit Rejected Document

---

### TC-DOC-015: Upload new version of a rejected document

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-015 |
| **Module** | Document Management |
| **Priority** | Critical |
| **Preconditions** | Student A is logged in; has a REJECTED document of type "National ID" |
| **Test Data** | New file: `id-corrected.pdf`; Document type: National ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/documents` | Documents page loads; REJECTED document visible |
| 2 | Click resubmit / re-upload action on the rejected document (or upload a new document with same type) | Upload form opens with document type pre-selected or selectable |
| 3 | Select `id-corrected.pdf` | File appears in upload form |
| 4 | Submit the upload | Upload succeeds; success message shown |
| 5 | Verify new document appears with status PENDING | New row: `id-corrected.pdf`, "National ID", `PENDING` |

| Field | Value |
|-------|-------|
| **Postconditions** | New document record created with PENDING status; file in MinIO |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Student re-uploads via the standard upload-single endpoint |

---

### TC-DOC-016: Verify old rejected document status after resubmission

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-016 |
| **Module** | Document Management |
| **Priority** | High |
| **Preconditions** | TC-DOC-015 completed; both old (rejected) and new (pending) documents exist |
| **Test Data** | Old document: REJECTED; New document: PENDING; both type "National ID" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/documents` | Documents page loads |
| 2 | Check if the old REJECTED document is still visible | Old document may still appear with REJECTED status, or may be archived/hidden |
| 3 | Verify the new PENDING document is listed | New document `id-corrected.pdf` shows as PENDING |
| 4 | Confirm document count reflects both entries (or only the latest if replaced) | Behavior is consistent with system design |

| Field | Value |
|-------|-------|
| **Postconditions** | Document history is preserved or replaced per system design |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Document entity uses `@OneToOne` with document type — observe whether old record is deleted or preserved |

---

## D. Admin — Document Verification

---

### TC-DOC-017: Admin views all documents at /admin/documents

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-017 |
| **Module** | Document Management |
| **Priority** | Critical |
| **Preconditions** | Platform Admin is logged in; multiple students have uploaded documents |
| **Test Data** | Documents from multiple students in various statuses |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/documents` | Document verification page loads (DocumentVerificationComponent) |
| 2 | Verify document table/list displays all documents | Documents from all students within the tenant are listed |
| 3 | Verify columns: student name, document name, document type, status, upload date | All columns populated with correct data |
| 4 | Verify pagination works | Page controls present and functional |
| 5 | Verify default sort is by upload date (newest first) | Most recent uploads appear first |

| Field | Value |
|-------|-------|
| **Postconditions** | Admin has full view of tenant documents via `GET /api/documents/search` |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DOC-018: Admin searches documents by student name

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-018 |
| **Module** | Document Management |
| **Priority** | High |
| **Preconditions** | Platform Admin is logged in; documents exist for Student A (John Doe) |
| **Test Data** | Search term: "John" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/documents` | Document verification page loads |
| 2 | Enter "John" in the student name search field | Search/filter input accepts text |
| 3 | Trigger search (press Enter or click Search) | Results filtered to show only documents belonging to students matching "John" |
| 4 | Verify results show documents for John Doe only | All listed documents belong to John Doe |
| 5 | Clear the search field and re-search | Full document list restored |

| Field | Value |
|-------|-------|
| **Postconditions** | Search filter works correctly via `studentName` param on `/api/documents/search` |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DOC-019: Admin filters documents by status — PENDING

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-019 |
| **Module** | Document Management |
| **Priority** | High |
| **Preconditions** | Platform Admin is logged in; documents exist in PENDING, APPROVED, and REJECTED statuses |
| **Test Data** | Filter: status = PENDING |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/documents` | Document verification page loads |
| 2 | Select "Pending" from the status filter dropdown | Filter is applied |
| 3 | Verify results | Only documents with status PENDING are displayed |
| 4 | Verify count matches expected number of pending documents | Row count is correct |

| Field | Value |
|-------|-------|
| **Postconditions** | Filter correctly scopes results to PENDING status |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DOC-020: Admin filters documents by status — APPROVED

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-020 |
| **Module** | Document Management |
| **Priority** | Medium |
| **Preconditions** | Platform Admin is logged in; at least one APPROVED document exists |
| **Test Data** | Filter: status = APPROVED |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/documents` | Document verification page loads |
| 2 | Select "Approved" from the status filter dropdown | Filter is applied |
| 3 | Verify results | Only documents with status APPROVED are displayed |

| Field | Value |
|-------|-------|
| **Postconditions** | Filter correctly scopes results to APPROVED status |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DOC-021: Admin filters documents by status — REJECTED

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-021 |
| **Module** | Document Management |
| **Priority** | Medium |
| **Preconditions** | Platform Admin is logged in; at least one REJECTED document exists |
| **Test Data** | Filter: status = REJECTED |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/documents` | Document verification page loads |
| 2 | Select "Rejected" from the status filter dropdown | Filter is applied |
| 3 | Verify results | Only documents with status REJECTED are displayed |

| Field | Value |
|-------|-------|
| **Postconditions** | Filter correctly scopes results to REJECTED status |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DOC-022: Admin approves a PENDING document

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-022 |
| **Module** | Document Management |
| **Priority** | Critical |
| **Preconditions** | Platform Admin is logged in; at least one PENDING document exists |
| **Test Data** | Document: Student A's National ID, status PENDING |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/documents` | Document verification page loads |
| 2 | Filter to PENDING documents | Only pending documents shown |
| 3 | Select Student A's National ID document | Document detail/action opens |
| 4 | Click "Approve" action | Confirmation dialog may appear |
| 5 | Confirm approval | Success snackbar: document approved |
| 6 | Verify document status changed to APPROVED | Status badge updates to "Approved" (green) |
| 7 | Log in as Student A → navigate to `/documents` | Student sees document status as APPROVED |

| Field | Value |
|-------|-------|
| **Postconditions** | Document status = APPROVED in database; `PUT /api/documents/update-status/{id}?status=APPROVED` |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DOC-023: Admin rejects a PENDING document with comment

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-023 |
| **Module** | Document Management |
| **Priority** | Critical |
| **Preconditions** | Platform Admin is logged in; at least one PENDING document exists |
| **Test Data** | Document: Student A's Proof of Enrollment, status PENDING; Rejection comment: "Document is expired. Please upload a current version." |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/documents` | Document verification page loads |
| 2 | Locate Student A's Proof of Enrollment (PENDING) | Document found |
| 3 | Click "Reject" action | Rejection dialog/form opens with optional comment field |
| 4 | Enter rejection comment: "Document is expired. Please upload a current version." | Comment entered |
| 5 | Confirm rejection | Success snackbar: document rejected |
| 6 | Verify document status changed to REJECTED | Status badge updates to "Rejected" (red) |
| 7 | Verify rejection comment is saved | Comment visible in document detail |

| Field | Value |
|-------|-------|
| **Postconditions** | Document status = REJECTED; rejection comment stored; `PUT /api/documents/update-status/{id}?status=REJECTED&rejectionComment=...` |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DOC-024: Email notification sent on document approval

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-024 |
| **Module** | Document Management |
| **Priority** | High |
| **Preconditions** | TC-DOC-022 completed (document just approved) |
| **Test Data** | Student A email: john.doe@test.com |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 2 | Search for emails to john.doe@test.com | Email(s) found |
| 3 | Locate email with subject "Document Verified - Student Accommodation" | Email present in inbox |
| 4 | Open the email | Email body contains: document name, document type, confirmation that document was verified/approved |
| 5 | Verify sender and formatting | Email is properly formatted with correct branding |

| Field | Value |
|-------|-------|
| **Postconditions** | Approval notification email delivered to student |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Subject constant: `EmailSubjects.DOCUMENT_VERIFIED` = "Document Verified - Student Accommodation" |

---

### TC-DOC-025: Email notification sent on document rejection

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-025 |
| **Module** | Document Management |
| **Priority** | High |
| **Preconditions** | TC-DOC-023 completed (document just rejected) |
| **Test Data** | Student A email: john.doe@test.com |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 2 | Search for emails to john.doe@test.com | Email(s) found |
| 3 | Locate email with subject "Document Review Required - Student Accommodation" | Email present in inbox |
| 4 | Open the email | Email body contains: document name, document type, rejection reason/comment |
| 5 | Verify the rejection comment from TC-DOC-023 appears in the email | "Document is expired. Please upload a current version." visible in email body |

| Field | Value |
|-------|-------|
| **Postconditions** | Rejection notification email delivered to student with reason |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Subject constant: `EmailSubjects.DOCUMENT_REJECTED` = "Document Review Required - Student Accommodation" |

---

### TC-DOC-026: Admin downloads a document for review

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-026 |
| **Module** | Document Management |
| **Priority** | High |
| **Preconditions** | Platform Admin is logged in; documents exist in the system |
| **Test Data** | Any student document |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/documents` | Document verification page loads |
| 2 | Locate a document and click the download/view action | File download starts or file opens in viewer |
| 3 | Verify file downloads correctly | File is intact, matches what student uploaded |
| 4 | Verify Content-Disposition header contains correct filename | Filename in download matches original document name |

| Field | Value |
|-------|-------|
| **Postconditions** | Admin can download any tenant document via `GET /api/documents/download/{id}` |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DOC-027: Admin attempts to approve an already-approved document

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-027 |
| **Module** | Document Management |
| **Priority** | Medium |
| **Preconditions** | Platform Admin is logged in; a document with status APPROVED exists |
| **Test Data** | Already approved document |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/documents` | Document verification page loads |
| 2 | Locate the APPROVED document | Document found with APPROVED badge |
| 3 | Attempt to click "Approve" again (if button is still available) | Either: (a) Approve button is disabled/hidden for already-approved docs, or (b) action succeeds idempotently with no status change, or (c) error message indicates document is already approved |
| 4 | Verify status remains APPROVED | No unintended state change |

| Field | Value |
|-------|-------|
| **Postconditions** | Document status unchanged at APPROVED |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Document actual behavior on re-approval; no duplicate emails should be sent |

---

## E. Admin — Document Comments

---

### TC-DOC-028: Admin adds a comment to a document

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-028 |
| **Module** | Document Management |
| **Priority** | High |
| **Preconditions** | Platform Admin is logged in; a document exists |
| **Test Data** | Document: Student A's National ID; Comment: "Please ensure the document is not cropped." |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/documents` | Document verification page loads |
| 2 | Select Student A's National ID document to view details | Document detail/comment section opens |
| 3 | Locate the comment input area (DocumentCommentSectionComponent) | Comment text area is visible |
| 4 | Enter: "Please ensure the document is not cropped." | Text entered in comment field |
| 5 | Click "Add Comment" / submit button | Comment is saved; appears in the comment thread |
| 6 | Verify comment shows: admin name, comment text, timestamp | All fields correctly displayed |

| Field | Value |
|-------|-------|
| **Postconditions** | Comment record created via `POST /api/documents/{id}/comments`; linked to document and admin user |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DOC-029: Admin adds multiple comments — all visible in order

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-029 |
| **Module** | Document Management |
| **Priority** | Medium |
| **Preconditions** | Platform Admin is logged in; TC-DOC-028 completed (one comment exists) |
| **Test Data** | Additional comments: "Quality is acceptable.", "Awaiting student response." |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open the same document's comment section | Existing comment from TC-DOC-028 is visible |
| 2 | Add comment: "Quality is acceptable." | Comment appears in thread |
| 3 | Add comment: "Awaiting student response." | Comment appears in thread |
| 4 | Verify all 3 comments are visible in chronological order | Comments ordered by `createdAt` ascending; all 3 displayed with correct text, author, and timestamps |

| Field | Value |
|-------|-------|
| **Postconditions** | Three comments exist on the document in correct order |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DOC-030: Email notification sent when admin adds a comment

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-030 |
| **Module** | Document Management |
| **Priority** | High |
| **Preconditions** | TC-DOC-028 completed (comment just added to Student A's document) |
| **Test Data** | Student A email: john.doe@test.com |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open MailHog at http://localhost:8025 | MailHog inbox loads |
| 2 | Search for emails to john.doe@test.com | Email(s) found |
| 3 | Locate email with subject "Document Review: Action Required - Student Accommodation" | Email present in inbox |
| 4 | Open the email | Email body contains: document name, comment text, link/instruction to view the document |
| 5 | Verify the comment text appears in the email body | "Please ensure the document is not cropped." visible |

| Field | Value |
|-------|-------|
| **Postconditions** | Comment notification email delivered to student |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Subject constant: `EmailSubjects.DOCUMENT_COMMENT` = "Document Review: Action Required - Student Accommodation" |

---

### TC-DOC-031: Student views comments on their document

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-031 |
| **Module** | Document Management |
| **Priority** | High |
| **Preconditions** | Student A is logged in; admin has added comments to Student A's document (TC-DOC-028/029 completed) |
| **Test Data** | Document with admin comments |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/documents` | Documents page loads |
| 2 | Select the document that has comments | Document detail/viewer opens |
| 3 | Locate the comments section | Comments section visible (DocumentCommentSectionComponent) |
| 4 | Verify all admin comments are displayed | Comments show: admin name, comment text, timestamps; in chronological order |
| 5 | Verify student cannot add comments (if restricted to admin only) | No "Add Comment" input visible for student, or it is disabled |

| Field | Value |
|-------|-------|
| **Postconditions** | Student can read but not create comments (comments endpoint `POST` is `@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")`) |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | `GET /api/documents/{id}/comments` is `isAuthenticated()` — accessible to students |

---

### TC-DOC-032: Admin deletes a comment

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-032 |
| **Module** | Document Management |
| **Priority** | Medium |
| **Preconditions** | Platform Admin is logged in; document has at least one comment |
| **Test Data** | Existing comment to delete |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to the document's comment section in `/admin/documents` | Comments are listed |
| 2 | Click the delete (trash/X) icon on a comment | Confirmation dialog appears |
| 3 | Confirm deletion | Comment is removed from the thread |
| 4 | Verify comment no longer appears in the list | Comment count decremented; thread updated |
| 5 | Refresh the page | Deleted comment remains absent |

| Field | Value |
|-------|-------|
| **Postconditions** | Comment record deleted via `DELETE /api/documents/comments/{commentId}` |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## F. Admin — Document Type Management

---

### TC-DOC-033: Create new document type with name and description

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-033 |
| **Module** | Document Management |
| **Priority** | Critical |
| **Preconditions** | Platform Admin is logged in |
| **Test Data** | Name: "Lease Agreement"; Description: "Signed lease agreement for accommodation"; isRequired: false |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/document-types` | Document type management page loads (DocumentTypeManagementComponent) |
| 2 | Click "Add" / "Create" button | Document type form dialog opens (DocumentTypeFormDialogComponent) |
| 3 | Enter name: "Lease Agreement" | Name field populated |
| 4 | Enter description: "Signed lease agreement for accommodation" | Description field populated |
| 5 | Set isRequired to false (unchecked) | Required toggle/checkbox unchecked |
| 6 | Click "Save" / "Create" | Success snackbar; dialog closes |
| 7 | Verify "Lease Agreement" appears in the document type list | New type listed with correct name, description, and required = No |

| Field | Value |
|-------|-------|
| **Postconditions** | Document type created via `POST /api/document-type`; scoped to admin's tenant |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DOC-034: Create document type with empty name — validation error

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-034 |
| **Module** | Document Management |
| **Priority** | High |
| **Preconditions** | Platform Admin is logged in |
| **Test Data** | Name: "" (empty); isRequired: true |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/document-types` | Page loads |
| 2 | Click "Add" / "Create" button | Form dialog opens |
| 3 | Leave name field empty | Name field blank |
| 4 | Set isRequired to true | Checkbox checked |
| 5 | Click "Save" / "Create" | Validation error: "Document type name is required"; form does not submit |

| Field | Value |
|-------|-------|
| **Postconditions** | No document type created; `@NotBlank` validation on `DocumentTypeRequest.name` |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DOC-035: Create document type with excessively long name

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-035 |
| **Module** | Document Management |
| **Priority** | Medium |
| **Preconditions** | Platform Admin is logged in |
| **Test Data** | Name: string of 256 characters; isRequired: false |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/document-types` | Page loads |
| 2 | Click "Add" / "Create" button | Form dialog opens |
| 3 | Enter a 256-character string in the name field | Text entered (may be truncated by frontend maxlength) |
| 4 | Click "Save" | Either: (a) frontend blocks submission with length error, or (b) backend returns validation/database error for exceeding column length |

| Field | Value |
|-------|-------|
| **Postconditions** | No document type created with excessively long name |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Check `type_name` column length constraint in DB schema |

---

### TC-DOC-036: Create document type with duplicate name — error

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-036 |
| **Module** | Document Management |
| **Priority** | High |
| **Preconditions** | Platform Admin is logged in; "National ID" document type already exists |
| **Test Data** | Name: "National ID"; isRequired: true |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/document-types` | Page loads; "National ID" visible in list |
| 2 | Click "Add" / "Create" button | Form dialog opens |
| 3 | Enter name: "National ID" | Name field populated |
| 4 | Set isRequired to true | Checked |
| 5 | Click "Save" | Error message: document type with this name already exists (or unique constraint violation) |

| Field | Value |
|-------|-------|
| **Postconditions** | No duplicate document type created |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | May be unique per tenant (tenant_id + type_name composite) |

---

### TC-DOC-037: Update existing document type

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-037 |
| **Module** | Document Management |
| **Priority** | High |
| **Preconditions** | Platform Admin is logged in; "Lease Agreement" document type exists (from TC-DOC-033) |
| **Test Data** | Updated name: "Lease Agreement (Current Year)"; Updated description: "Current academic year lease agreement" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/document-types` | Page loads |
| 2 | Click Edit on "Lease Agreement" | Edit form/dialog opens pre-populated with current values |
| 3 | Change name to "Lease Agreement (Current Year)" | Name field updated |
| 4 | Change description to "Current academic year lease agreement" | Description updated |
| 5 | Click "Save" | Success snackbar; dialog closes |
| 6 | Verify updated name and description in the list | Row reflects new values |

| Field | Value |
|-------|-------|
| **Postconditions** | Document type updated via `PUT /api/document-type/{id}` |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DOC-038: Toggle isRequired flag on document type

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-038 |
| **Module** | Document Management |
| **Priority** | High |
| **Preconditions** | Platform Admin is logged in; "Medical Certificate" exists with isRequired = false |
| **Test Data** | Document type: Medical Certificate; toggle to isRequired = true |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/document-types` | Page loads |
| 2 | Locate "Medical Certificate" with Required = No | Type found |
| 3 | Click the required toggle or use the Edit form to change isRequired to true | Toggle/checkbox updated |
| 4 | Save changes | Success confirmation |
| 5 | Verify "Medical Certificate" now shows Required = Yes | Updated in the list |
| 6 | Toggle back to isRequired = false | Successfully reverted |

| Field | Value |
|-------|-------|
| **Postconditions** | Required flag updated via `PATCH /api/document-type/{id}/required?required=true` |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-DOC-039: Delete document type

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-039 |
| **Module** | Document Management |
| **Priority** | High |
| **Preconditions** | Platform Admin is logged in; "Lease Agreement (Current Year)" exists and has no documents linked to it |
| **Test Data** | Document type: "Lease Agreement (Current Year)" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/document-types` | Page loads |
| 2 | Click Delete on "Lease Agreement (Current Year)" | Confirmation dialog appears |
| 3 | Confirm deletion | Success snackbar; type removed from list |
| 4 | Verify type no longer appears in list | List updated; type absent |
| 5 | Verify type no longer appears in student upload dropdown | Type not available for selection |

| Field | Value |
|-------|-------|
| **Postconditions** | Document type deleted via `DELETE /api/document-type/{id}` |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Deletion may fail if documents reference this type (FK constraint) |

---

### TC-DOC-040: Required document types appear in student's upload checklist

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-040 |
| **Module** | Document Management |
| **Priority** | High |
| **Preconditions** | Student A is logged in; required document types exist: National ID, Proof of Enrollment, Proof of Funding |
| **Test Data** | Required document types configured by admin |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/documents` | Documents page loads |
| 2 | Observe the upload section or document checklist | Required document types are listed with indicators (e.g., asterisk, "Required" label) |
| 3 | Verify National ID is marked as required | Shows as required |
| 4 | Verify Proof of Enrollment is marked as required | Shows as required |
| 5 | Verify Proof of Funding is marked as required | Shows as required |
| 6 | Verify Medical Certificate is shown as optional (if visible) | Not marked as required or listed separately |

| Field | Value |
|-------|-------|
| **Postconditions** | Student is aware of which documents are mandatory for their application |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Uses `GET /api/document-type/required` endpoint |

---

### TC-DOC-041: Tenant-scoped document types — tenant admin sees own + platform defaults

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-041 |
| **Module** | Document Management |
| **Priority** | High |
| **Preconditions** | Platform Admin has created platform-default document types (tenant_id=0); Tenant Admin is logged in for a specific tenant |
| **Test Data** | Platform defaults: National ID, Proof of Enrollment; Tenant-specific: "Campus Card" (created by tenant admin) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin | Dashboard loads |
| 2 | Navigate to `/admin/document-types` | Document type management page loads |
| 3 | Verify platform default types are visible | "National ID" and "Proof of Enrollment" shown with `isPlatformDefault = true` indicator |
| 4 | Verify tenant-specific type "Campus Card" is visible | Listed alongside platform defaults |
| 5 | Attempt to edit a platform default type | Edit action is disabled/hidden or returns 403 for tenant admin |
| 6 | Attempt to delete a platform default type | Delete action is disabled/hidden or returns 403 for tenant admin |
| 7 | Edit the tenant-specific "Campus Card" type | Edit succeeds — tenant admin can manage own types |

| Field | Value |
|-------|-------|
| **Postconditions** | Tenant admin sees merged view; can only modify own tenant's types, not platform defaults |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Shadow semantics: tenant row wins if same type_name as platform row |

---

### TC-DOC-044: Student uploads a company's own required document type

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-044 |
| **Module** | Document Management |
| **Priority** | High |
| **Preconditions** | Tenant Admin X has created a required document type "Bursary Letter", a name no platform type uses; Company X has a building with an intake open to Student A's institution; Student A has uploaded every platform required type and has no application with Company X; Student B has not picked a Company X building or applied there |
| **Test Data** | Company type: Bursary Letter (required); File: `bursary.pdf` (< 5 MB) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | As Student A, go to `/applications/new` and pick Company X's building | Company X's panel says "Company X needs bursary Letter." and shows "Upload documents" |
| 2 | Click "Upload documents" | `/documents?buildings={buildingId}` opens; the pack has a "Bursary Letter" slot marked Required |
| 3 | Upload `bursary.pdf` to the Bursary Letter slot | Upload succeeds; the slot shows `PENDING` |
| 4 | Go back to `/applications/new` and pick the same building | Company X's panel no longer lists missing documents |
| 5 | Submit the application | The application to Company X is created |
| 6 | Open `/documents` from the menu | "Bursary Letter" is still listed, marked Required, with the uploaded file |
| 7 | As Tenant Admin X, open the application's required documents | Bursary Letter shows the uploaded file as `PENDING`, not as not uploaded |
| 8 | As Student B, open `/documents` | No "Bursary Letter" slot is listed |

| Field | Value |
|-------|-------|
| **Postconditions** | A company's own required type can be uploaded, and the upload counts for submit and for the admin checklist |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | #443. The page calls `GET /api/document-type/my?buildingIds=` (students only). Step 4 of onboarding (`/application-submission`) links to the page with the picked buildings the same way. |

---

## G. Role-Based Access

---

### TC-DOC-042: Student cannot access /admin/documents

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-042 |
| **Module** | Document Management |
| **Priority** | Critical |
| **Preconditions** | Student A is logged in |
| **Test Data** | N/A |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Manually navigate to `/admin/documents` in the browser URL bar | Access denied: redirected to student dashboard or shown a "Not Authorized" page |
| 2 | Attempt direct API call: `GET /api/documents/search` with student JWT | Returns 403 Forbidden |
| 3 | Attempt direct API call: `PUT /api/documents/update-status/1?status=APPROVED` with student JWT | Returns 403 Forbidden |
| 4 | Attempt direct API call: `POST /api/documents/1/comments` with student JWT | Returns 403 Forbidden |

| Field | Value |
|-------|-------|
| **Postconditions** | Student role cannot access admin document endpoints; Angular guard blocks frontend route |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Frontend: `admin.guard.ts`; Backend: `@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")` |

---

### TC-DOC-043: Tenant admin sees only their tenant's documents

| Field | Value |
|-------|-------|
| **ID** | TC-DOC-043 |
| **Module** | Document Management |
| **Priority** | Critical |
| **Preconditions** | Two tenants exist (Tenant X and Tenant Y); each has students with uploaded documents; Tenant Admin X is logged in |
| **Test Data** | Tenant X documents: 3 documents; Tenant Y documents: 2 documents |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin X | Dashboard loads |
| 2 | Navigate to `/admin/documents` | Document verification page loads |
| 3 | Verify only Tenant X's documents are displayed | Only 3 documents from Tenant X students visible |
| 4 | Verify no documents from Tenant Y appear | Tenant Y's 2 documents are absent |
| 5 | Search for a Tenant Y student by name | No results returned |
| 6 | Attempt direct API call with Tenant Y document ID: `GET /api/documents/info/{tenantYDocId}` | Returns 404 or 403 (Hibernate tenant filter excludes the record) |

| Field | Value |
|-------|-------|
| **Postconditions** | Tenant isolation enforced via Hibernate `tenantFilter` |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Document entity has `@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")` |

---

## Summary

| Section | Test Cases | IDs |
|---------|-----------|-----|
| A. Student — Upload Documents | 7 | TC-DOC-001 to TC-DOC-007 |
| B. Student — View Documents | 7 | TC-DOC-008 to TC-DOC-014 |
| C. Student — Resubmit Rejected | 2 | TC-DOC-015 to TC-DOC-016 |
| D. Admin — Document Verification | 11 | TC-DOC-017 to TC-DOC-027 |
| E. Admin — Document Comments | 5 | TC-DOC-028 to TC-DOC-032 |
| F. Admin — Document Type Management | 10 | TC-DOC-033 to TC-DOC-041, TC-DOC-044 |
| G. Role-Based Access | 2 | TC-DOC-042 to TC-DOC-043 |
| **Total** | **44** | |

---

| Field | Value |
|-------|-------|
| **Reviewed By** | _________________ |
| **Review Date** | _________________ |
| **Approved By** | _________________ |
| **Approval Date** | _________________ |
