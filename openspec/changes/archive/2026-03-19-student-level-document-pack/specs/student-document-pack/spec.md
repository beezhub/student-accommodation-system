## ADDED Requirements

### Requirement: Each student has at most one document per document type
The system SHALL enforce a uniqueness constraint of one document record per `(student_id, document_type_id)` pair at the database level. Attempting to insert a second document of the same type for the same student via any code path other than the designated upsert path SHALL raise a database constraint violation.

#### Scenario: First upload of a document type succeeds
- **WHEN** a student uploads a document and no document of that type exists in their pack
- **THEN** the system saves the document record and returns a success response

#### Scenario: Database rejects a duplicate insert outside the upsert path
- **WHEN** a raw INSERT is attempted for a `(student_id, document_type_id)` pair that already exists
- **THEN** the database raises a unique constraint violation

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

### Requirement: Students can view their full document pack
The `GET /api/documents/my` endpoint SHALL return all documents in the student's pack, regardless of document status, ordered by `createdAt` descending by default.

#### Scenario: Student with documents in multiple states
- **WHEN** a student calls `GET /api/documents/my`
- **THEN** the response includes all documents owned by that student (PENDING, APPROVED, REJECTED)
- **AND** the response includes the document type name and description for each entry

#### Scenario: Student with no documents uploaded
- **WHEN** a student calls `GET /api/documents/my` and has no documents
- **THEN** the response returns an empty paginated result with `totalElements = 0`

### Requirement: Students can delete a document from their pack
The `DELETE /api/documents/delete/{documentId}` endpoint SHALL allow a student to delete any of their own documents at any time. After deletion, the document type slot is vacant and a new document may be uploaded for that type.

#### Scenario: Student deletes their own document
- **WHEN** a student calls `DELETE /api/documents/delete/{id}` for a document they own
- **THEN** the document record is deleted from the database and the MinIO object is deleted (best-effort)
- **AND** `GET /api/documents/my` no longer returns that document type entry

#### Scenario: Student attempts to delete another student's document
- **WHEN** a student calls `DELETE /api/documents/delete/{id}` for a document they do not own
- **THEN** the system returns `403 Forbidden`

### Requirement: The application_document junction table is removed
The `application_document` table and all associated code (`ApplicationDocument` entity, `ApplicationDocumentRepository`, `linkDocumentToActiveApplication` in `DocumentLifecycleService`) SHALL be removed. Admins accessing a student's documents during application review SHALL query documents directly via `document.student_id`.

#### Scenario: Document uploaded after junction table removal is still visible to admins
- **WHEN** a student uploads a document
- **THEN** an admin calling `GET /api/documents/info/all/{userId}` can still retrieve that document
- **AND** no junction table row is created

#### Scenario: No auto-link side-effect on upload
- **WHEN** a student uploads a document via `upload-single` or `upload-multiple`
- **THEN** no row is inserted into any junction or pivot table as a side effect of the upload
