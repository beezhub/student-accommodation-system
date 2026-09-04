# Capability: Student Document Pack

## Purpose

Defines the rules governing a student's personal document pack — a per-student collection of uploaded files keyed by document type. Each student holds at most one document per type; uploading to an occupied slot replaces the previous file.

---

## Requirements

### Requirement: Each student has at most one document per document type
The system SHALL enforce a uniqueness constraint of one document record per `(student_id, document_type_id)` pair at the database level. Attempting to insert a second document of the same type for the same student via any code path other than the designated upsert path SHALL raise a database constraint violation.

#### Scenario: First upload of a document type succeeds
- **WHEN** a student uploads a document and no document of that type exists in their pack
- **THEN** the system saves the document record and returns a success response

#### Scenario: Database rejects a duplicate insert outside the upsert path
- **WHEN** a raw INSERT is attempted for a `(student_id, document_type_id)` pair that already exists
- **THEN** the database raises a unique constraint violation

---

### Requirement: Uploading a document type that already exists replaces the previous file
The `POST /api/documents/upload-single` endpoint SHALL implement upsert semantics: if the student already has a document of the requested type, the existing MinIO object is deleted, the existing document record is deleted, and the new file is stored in its place. The replacement SHALL reset the document status to `PENDING`.

#### Scenario: Student replaces an existing document
- **WHEN** a student calls `POST /api/documents/upload-single` with a `documentType` for which they already have a document in their pack
- **THEN** the old document record and its associated MinIO object are deleted
- **AND** a new document record is created with status `PENDING`
- **AND** the response reflects the new document's metadata

#### Scenario: Student uploads to a type they do not yet have
- **WHEN** a student calls `POST /api/documents/upload-single` with a `documentType` for which they have no existing document
- **THEN** a new document record is created (no deletion occurs)
- **AND** the response reflects the new document's metadata

#### Scenario: MinIO deletion fails during replacement
- **WHEN** the old MinIO object cannot be deleted during a replace operation
- **THEN** the system logs the failure at ERROR level and continues — the new document record is saved and the old orphan is cleaned up by the scheduled storage cleanup job

---

### Requirement: Students can view their full document pack
The `GET /api/documents/my` endpoint SHALL return all documents in the student's pack, regardless of document status, ordered by `createdAt` descending by default. The response includes the document type name and description for each entry. No tenant-specific enrichment is applied at this endpoint — the per-tenant required-type view is served by `GET /api/document-type/required?tenantId={id}` and consumed by the application form and applications list, not by the document pack endpoint.

#### Scenario: Student with documents in multiple states
- **WHEN** a student calls `GET /api/documents/my`
- **THEN** the response includes all documents owned by that student (PENDING, APPROVED, REJECTED)
- **AND** the response includes the document type name and description for each entry

#### Scenario: Student with no documents uploaded
- **WHEN** a student calls `GET /api/documents/my` and has no documents
- **THEN** the response returns an empty paginated result with `totalElements = 0`

---

### Requirement: Students can delete a document from their pack
The `DELETE /api/documents/delete/{documentId}` endpoint SHALL allow a student to delete any of their own documents at any time. After deletion, the document type slot is vacant and a new document may be uploaded for that type.

#### Scenario: Student deletes their own document
- **WHEN** a student calls `DELETE /api/documents/delete/{id}` for a document they own
- **THEN** the document record is deleted from the database and the MinIO object is deleted (best-effort)
- **AND** `GET /api/documents/my` no longer returns that document type entry

#### Scenario: Student attempts to delete another student's document
- **WHEN** a student calls `DELETE /api/documents/delete/{id}` for a document they do not own
- **THEN** the system returns `403 Forbidden`

---

### Requirement: The application_document junction table is removed
The `application_document` table and all associated code (`ApplicationDocument` entity, `ApplicationDocumentRepository`, `linkDocumentToActiveApplication` in `DocumentLifecycleService`) SHALL be removed. Admins accessing a student's documents during application review SHALL query documents directly via `document.student_id`.

#### Scenario: Document uploaded after junction table removal is still visible to admins
- **WHEN** a student uploads a document
- **THEN** an admin calling `GET /api/documents/info/all/{userId}` can still retrieve that document
- **AND** no junction table row is created

#### Scenario: No auto-link side-effect on upload
- **WHEN** a student uploads a document via `upload-single` or `upload-multiple`
- **THEN** no row is inserted into any junction or pivot table as a side effect of the upload

---

### Requirement: Student dashboard documents page is a tenant-agnostic portfolio manager
The Angular student dashboard documents page (`/dashboard/documents`) SHALL display only platform-level document types (`tenant_id = 0`) as the reference list. It SHALL NOT show tenant-specific required types — those are surfaced on the application submission form and the applications list page, where the target tenant is known. The documents page is a general-purpose portfolio: the student uploads and manages documents here without needing to know which tenant they are applying to.

#### Scenario: Student views document pack on the documents page
- **WHEN** a student navigates to `/dashboard/documents`
- **THEN** the page shows all platform-level document types as the reference list
- **AND** each platform type shows its upload status (not uploaded / pending / approved / rejected)
- **AND** no tenant-specific document types appear in this view

#### Scenario: Student uploads a tenant-specific document type from the documents page
- **WHEN** a student follows an upload link from the application submission form that pre-selects a tenant-specific document type
- **AND** they are redirected to the document upload UI
- **THEN** the upload UI accepts the pre-selected `documentTypeId` (including tenant-specific IDs) regardless of whether that type appears in the platform-types reference list

---

### Requirement: Student applications list shows per-application document status against the target tenant's required types
The Angular student applications list page SHALL display a document status summary beneath each active application. The summary SHALL call `GET /api/document-type/required?tenantId={applicationTenantId}` to resolve the target tenant's effective required types and compare against the student's uploaded document names. Each required type SHALL be shown as present or absent. Absent types SHALL include an upload action that pre-selects the correct `documentTypeId`.

#### Scenario: Student views an application with all tenant-required documents uploaded
- **WHEN** a student views their applications list and all required document types for the target tenant are present in their pack (by name)
- **THEN** the document summary shows all requirements as satisfied with no upload prompts

#### Scenario: Student views an application with a missing tenant-specific required document
- **WHEN** a student views their applications list and a tenant-specific required document type is absent from their pack
- **THEN** the document summary shows that type as missing
- **AND** an upload action is shown that routes to the document upload UI with the tenant-specific `documentTypeId` pre-selected

#### Scenario: Student has two active applications to different tenants with different requirements
- **WHEN** a student has active applications to Tenant A and Tenant B, each with different required document types
- **THEN** each application's document summary shows its own tenant's required types independently
- **AND** a document present for Tenant A's requirement does not affect Tenant B's summary unless the same type name is also required by Tenant B
