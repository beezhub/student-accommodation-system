## MODIFIED Requirements

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
