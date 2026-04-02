## 1. Database Migrations

- [x] 1.1 Write Flyway migration to deduplicate `document` table: for each `(student_id, document_type_id)` group, delete all rows except the one with the latest `updated_at` (tie-break on `id DESC`)
- [x] 1.2 Write Flyway migration to add `UNIQUE(student_id, document_type_id)` constraint to `document` table after deduplication (can be in the same migration as 1.1)
- [x] 1.3 Write Flyway migration to drop the `application_document` table and its indexes (`idx_application_document_application`, `idx_application_document_document`)

## 2. Backend — Remove Junction Table

- [x] 2.1 Delete `ApplicationDocument` entity class
- [x] 2.2 Delete `ApplicationDocumentRepository` interface
- [x] 2.3 Remove `linkDocumentToActiveApplication` method from `DocumentLifecycleService`
- [x] 2.4 Remove all usages and imports of `ApplicationDocument`, `ApplicationDocumentRepository`, and `ApplicationRepository` from `DocumentLifecycleService`
- [x] 2.5 Remove `applicationRepository` dependency injection from `DocumentLifecycleService`

## 3. Backend — Upsert on Single Upload

- [x] 3.1 Add `DocumentRepository.findByStudentAndDocumentType(Student student, DocumentType type)` query method (or equivalent by student_id and type_id)
- [x] 3.2 In `DocumentLifecycleService.uploadSingleDocument`: before calling `validateAndUploadFile`, check if a document already exists for `(student, documentType)`; if so, delete its MinIO object (best-effort, log error on failure) and delete the DB record
- [x] 3.3 Verify that the existing compensating transaction logic in `validateAndUploadFile` still works correctly after the pre-delete step

## 4. Backend — Pre-Submission Document Check

- [x] 4.1 Add `DocumentRepository.findDocumentTypeIdsByStudentId(Long studentId)` query method returning the set of `document_type_id` values for a student
- [x] 4.2 Add `DocumentTypeRepository.findAllByIsRequiredTrue()` query method (or use existing query if present)
- [x] 4.3 In `ApplicationCoreService.submitApplication`: after building `resolvedPreferenceBuildings` and before checking the duplicate-application constraint, fetch required document type IDs and the student's uploaded type IDs; throw `400` with missing type names if any required type is absent
- [x] 4.4 Add a new checked exception or use `IllegalArgumentException` with a structured message listing missing document type names

## 5. Frontend — Decouple Document Upload from Application Flow

- [x] 5.1 In `ApplicationSubmissionComponent.onSubmit` success handler: change `this.router.navigate(['/document-upload'])` to `this.router.navigate(['/dashboard'])`
- [x] 5.2 Remove the `skipApplication()` method and its template reference from `ApplicationSubmissionComponent`
- [x] 5.3 Remove any guard or route logic that blocks access to document upload without an active application

## 6. Frontend — Document Pack Dashboard Section

- [x] 6.1 Create `DocumentPackComponent` at `features/dashboard/documents/document-pack/document-pack.component.ts` — displays all document types (required and optional), each row showing: type name, description, status badge (Not Uploaded / Pending / Approved / Rejected), and Upload/Replace/Delete actions
- [x] 6.2 Create `DocumentsModule` or standalone component routing under `/dashboard/documents`
- [x] 6.3 Add "Documents" link to the student dashboard sidebar/nav (alongside "My Applications")
- [x] 6.4 Wire `DocumentPackComponent` to call `GET /api/documents/my` on load and render the full document type list (merge with required types from `GET /api/document-types` to show types with no uploads)
- [x] 6.5 Implement upload/replace action in `DocumentPackComponent`: call `POST /api/documents/upload-single` with file and type ID; refresh list on success
- [x] 6.6 Implement delete action in `DocumentPackComponent`: confirm dialog → call `DELETE /api/documents/delete/{id}` → refresh list on success

## 7. Frontend — Document Readiness Checklist on Application Form

- [x] 7.1 In `ApplicationSubmissionComponent.ngOnInit`, load required document types via `GET /api/document-types?required=true` and current student documents via `GET /api/documents/my`
- [x] 7.2 Compute `missingRequiredTypes` array from the above two lists
- [x] 7.3 Render a document readiness checklist above the submit button: green check for uploaded types, red alert icon with type name for missing types; include a link to `/dashboard/documents` for missing types
- [x] 7.4 Disable the submit button when `missingRequiredTypes.length > 0`

## 8. Verification

- [x] 8.1 Write integration test for `uploadSingleDocument` upsert: upload type A, upload type A again, verify only one document of type A exists for the student and the old MinIO path is replaced
- [x] 8.2 Write integration test for `submitApplication` pre-submission check: submit with missing required doc → expect `400`; upload required doc → submit → expect `201`
- [x] 8.3 Verify `GET /api/documents/info/all/{userId}` still returns student documents after `application_document` table is removed (admin access path) — confirmed: uses `documentRepository.findAllByStudent_AppUser_Id(userId)`, no junction table dependency
- [x] 8.4 Manual smoke test: student uploads a document, navigates to application form, sees green checkmark, submits application, lands on `/dashboard`
  - Note: `DocumentUploadComponent` application gate removed (W1 fix) — `/document-upload` is now freely accessible without an active application
