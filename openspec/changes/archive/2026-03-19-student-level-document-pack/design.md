## Context

Documents were historically stored against an `application_id` foreign key. A migration in February 2026 (`V2026_02_25_0010`) decoupled documents from applications by making `application_id` nullable and adding an `application_document` junction table. A follow-up migration dropped the `application_id` column entirely (`V2026_02_25_0011`). The junction table is still in use today: `DocumentLifecycleService.linkDocumentToActiveApplication` auto-links every newly uploaded document to the student's first active application.

Despite the schema changes, the frontend UX has not caught up: the application submission component still redirects to `/document-upload` after a successful application submit, and `skipApplication()` explicitly blocks document upload without a prior application. The result is an inconsistent half-decoupled state.

The goal of this change is to complete the decoupling: documents are a student's permanent property, uploaded and managed via the student dashboard independently of any application lifecycle.

**Constraints:**
- PostgreSQL 16.2 with Flyway migrations (no manual DB edits)
- Existing documents in `document` table must not be lost during migration
- `application_document` junction table has existing rows that need handling
- Backend is multi-tenant; document `tenant_id` is currently stamped from `TenantContext` — students have `tenant_id = 0` (PLATFORM)
- Admin (TENANT_ADMIN) access to student documents must be preserved via the `getAllDocumentsByUserId` / `searchDocuments` endpoints which bypass the tenant filter via explicit joins

## Goals / Non-Goals

**Goals:**
- One document slot per document type per student (`UNIQUE(student_id, document_type_id)`) with upsert semantics on upload
- Remove the `application_document` junction table and all auto-linking logic
- Application submission validates that all required document types are present in the student's pack before accepting the request
- Frontend: student manages documents in the dashboard before and after applying; application submission form shows document readiness; the post-submit redirect to `/document-upload` is removed
- Admin application review shows the student's full current document pack (no snapshot needed; documents are replaced-in-place, so latest is always correct)

**Non-Goals:**
- Point-in-time snapshots of which documents existed at submission time
- Versioning or history of replaced documents
- Document sharing between students
- Changing the `DocumentType` global catalog or adding tenant-scoped document types (future concern)
- Altering the tenant filter on the `document` table

## Decisions

### Decision 1: Remove `application_document` junction table entirely

**Chosen:** Drop the table and all related code (`ApplicationDocument` entity, `ApplicationDocumentRepository`, auto-link logic in `DocumentLifecycleService`).

**Why:** The junction table was an intermediate migration aid. With documents fully living at the student level and one-doc-per-type semantics, there is no need to track which documents were attached to which application. Admins viewing an application simply access the student's document pack. This reduces accidental complexity — the auto-link logic in `linkDocumentToActiveApplication` is fragile (it links to the "first active application", which breaks for students with multiple tenant applications).

**Alternative considered:** Keep the junction table as an audit snapshot populated at submission time. Rejected because: (a) it requires snapshot logic on submission and re-snapshot on document replace, (b) adds complexity without clear benefit since documents are replaced-in-place and always represent the student's latest state.

### Decision 2: Add `UNIQUE(student_id, document_type_id)` with migration-safe deduplication

**Chosen:** Before adding the constraint, a Flyway migration deduplicates by keeping only the newest document per `(student_id, document_type_id)` pair (by `updated_at` desc, then `id` desc). The older duplicates are deleted from both DB and MinIO (best-effort for MinIO — same compensation pattern already used in `DocumentLifecycleService`).

**Why:** This is the minimal safe path to enforce one-slot-per-type. The deduplication is run in the same Flyway transaction so the constraint add cannot fail due to pre-existing duplicates.

**Alternative considered:** Application-layer enforcement only (no DB constraint). Rejected — application-layer uniqueness is race-condition prone under concurrent uploads.

### Decision 3: Upsert semantics in `DocumentLifecycleService.uploadSingleDocument`

**Chosen:** Before persisting a new document record, check if a document already exists for `(student_id, document_type_id)`. If found: delete the MinIO object and the existing DB record, then save the new one. The upload-multiple endpoint retains its current additive behaviour (uploading multiple files of different types in one call remains valid).

**Why:** Replace-on-upload matches the "one slot per type" mental model. Students clicking "Update ID document" should not have to manually delete the old one first.

**Alternative considered:** Separate `PATCH /api/documents/{id}/replace` endpoint. Rejected — this adds API surface complexity for a common workflow; upsert at the service layer is transparent to the caller.

### Decision 4: Pre-submission document completeness check in `ApplicationCoreService`

**Chosen:** `submitApplication` fetches all `DocumentType` records where `isRequired = true` and verifies each one has a corresponding approved-or-pending `document` row for the student. If any required type is missing, `400 Bad Request` is returned with a list of missing types.

**Why:** Moves validation to the point of commitment (submission), not at upload time. Students can upload documents at any point before or during the application process; the system only gates at submit.

**Alternative considered:** Reject the application only if required docs are missing AND none are PENDING/APPROVED. Adopted this variant — PENDING is sufficient (admin will review). Only MISSING (no upload at all) blocks submission.

### Decision 5: Frontend — dashboard-first document management

**Chosen:** Add a "Documents" section to the student dashboard (alongside "My Applications"). Remove the post-submission redirect to `/document-upload`. The application submission form will show a read-only document readiness checklist (green/red per required type) so students can see their pack status before submitting.

**Why:** Matches the "document pack" mental model — documents are persistent student data, not application attachments. The readiness checklist gives students immediate feedback without blocking them from viewing the form.

## Risks / Trade-offs

- **Risk: MinIO orphan on deduplication migration** → Same compensation pattern already in codebase. The Flyway migration deletes DB records first; MinIO cleanup is best-effort. An existing orphan-cleanup job handles any stragglers.
- **Risk: Admin queries for documents break after removing junction table** → `DocumentQueryService.searchDocuments` and `getAllDocumentsByUserId` join directly on `document.student_id`; they do not use `application_document`. No breakage expected.
- **Risk: Students with applications in UNDER_REVIEW may have no documents linked after migration** → After removing the junction, admins simply read the student's document pack. No data is lost — the documents still exist, just not via the junction.
- **Trade-off: No point-in-time snapshot** → If a student replaces a document after an admin has started reviewing, the admin sees the new version. Accepted: the new version is always more current and relevant to the review.

## Migration Plan

1. **Flyway: deduplication + constraint** — new migration `V{date}__document_pack_unique_constraint.sql`:
   - Delete duplicate documents keeping newest per `(student_id, document_type_id)`
   - Add `UNIQUE(student_id, document_type_id)` constraint
2. **Flyway: drop junction table** — new migration `V{date}__drop_application_document.sql`:
   - `DROP TABLE application_document CASCADE`
3. **Backend: remove dead code** — `ApplicationDocument` entity, `ApplicationDocumentRepository`, `linkDocumentToActiveApplication` method, related imports
4. **Backend: upsert in `DocumentLifecycleService.uploadSingleDocument`**
5. **Backend: pre-submission check in `ApplicationCoreService.submitApplication`**
6. **Frontend: remove post-submit redirect; add document pack dashboard section; add readiness checklist to application form**

**Rollback:** The deduplication migration is destructive (deletes duplicate documents). A database backup must be taken before running. If rollback is needed, the junction table can be recreated from the backup — the `UNIQUE` constraint can be dropped without data loss.

## Open Questions

- Should the application submission check be a hard block (400 error) or a soft warning that lets the student submit anyway? → **Decision: hard block** — submission should only proceed when the student's pack is complete, as incomplete docs will block admin review anyway.
- Should `upload-multiple` also gain upsert semantics? → **Decision: No** — bulk upload is used for initial pack setup; upsert semantics are only on the single-upload path which is the primary replace-a-document flow.
