## Why

Documents are currently uploaded as part of the post-application flow — the frontend forces a redirect to document upload only after an application is submitted, and the comment in the codebase even blocks skipping the application ("an application is required before you can upload documents"). This coupling means students must re-upload the same documents for each new application and cannot manage their document portfolio independently. A student-level document pack decouples document ownership from applications, allowing students to upload documents once and have those documents referenced across all their applications.

## What Changes

- Documents are owned by the student permanently — uploaded once, valid for all future applications
- Each document type has a single slot per student: uploading a new file for an existing type **replaces** the previous file (upsert semantics)
- The application submission flow no longer redirects to a document upload page; document upload becomes a standalone section of the student dashboard
- Application submission validates that required documents are present in the student's pack before allowing submission (rather than after)
- Admin application review surfaces the student's document pack inline, with per-document approval/rejection status
- The `skipApplication()` warning ("an application is required before you can upload documents") is removed entirely

## Capabilities

### New Capabilities
- `student-document-pack`: A persistent, student-owned document portfolio. Each document type occupies one slot per student. Upload replaces the existing document of the same type. Students can upload, view, replace, and delete documents at any time, independently of any application. Document verification status (PENDING, APPROVED, REJECTED) is tracked per document.

### Modified Capabilities
- `application-building-preference`: No requirement changes — building preference logic is unchanged. This change does not modify the tenant derivation or multi-preference rules.

## Impact

**Backend:**
- `document` table: add `UNIQUE(student_id, document_type_id)` constraint via Flyway migration; upsert behaviour in `DocumentService.uploadSingleDocument`
- `DocumentService`: replace-on-upload logic when same document type already exists for the student
- `ApplicationCoreService.submitApplication`: add pre-submission guard — reject if student is missing any required document types in their pack
- No schema additions required for application → document linkage (documents already carry `student_id`)

**Frontend:**
- Remove post-application-submission redirect to `/document-upload`
- Add document pack management page to the student dashboard (upload/replace/view per type)
- Application submission form: show document readiness summary (which required types are present/missing) so students know their pack is ready before submitting
- Remove the `skipApplication()` guard that blocked document upload without an application

**APIs:**
- `POST /api/documents/upload-single` — gains upsert semantics (existing document for that type is deleted and replaced)
- `POST /api/application` — gains pre-submission document completeness validation
- No new endpoints required
