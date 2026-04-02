# Capability: Application Document Submission

## Purpose

Defines the rules governing document completeness checks during application submission — both the backend enforcement and the Angular frontend form readiness display — and the post-submission navigation flow.

---

## Requirements

### Requirement: Application submission requires all required document types to be present in the student's pack
The `POST /api/application` endpoint SHALL, before saving the application, resolve the required document types from the **application's target tenant** — derived from the selected preferred buildings (`applicationTenantId = buildings[0].tenantId`), NOT from the student's `TenantContext` (which is always `0`). The required type set is the effective merged view: platform document types (`tenant_id = 0`) combined with the target tenant's own types, with tenant rows shadowing platform rows by `type_name`. Presence is determined by **name-based matching**: the system compares the set of `type_name` values already uploaded by the student against the set of required `type_name` values from the effective view. If any required name is absent, the system SHALL return `400 Bad Request` with a body listing the missing type names. A document in any status (PENDING, APPROVED, REJECTED) satisfies the presence check.

#### Scenario: Student has all required documents uploaded (tenant uses platform defaults only)
- **WHEN** a student submits an application and their document pack contains at least one document for each `isRequired = true` document type in the merged view (using only platform defaults)
- **THEN** the application submission proceeds normally and returns `201 Created`

#### Scenario: Student is missing one required document type from the tenant's effective list
- **WHEN** a student submits an application and their document pack is missing a document for at least one `isRequired = true` document type in the tenant's merged view
- **THEN** the system returns `400 Bad Request`
- **AND** the response body includes the names of the missing required document types

#### Scenario: Student's document (platform type ID) satisfies a tenant shadow of the same name
- **WHEN** a student has uploaded a document linked to platform type `"National ID"` (id=1)
- **AND** the target tenant has a shadow type also named `"National ID"` (id=5) with `isRequired = true`
- **THEN** the name-based check finds `"National ID"` in both the uploaded set and the required set
- **AND** the submission proceeds normally (`201 Created`) — the student is not required to re-upload

#### Scenario: Tenant has shadowed a required platform type to be non-required
- **WHEN** a tenant has a shadow document type with `isRequired = false` for a type that is `isRequired = true` at the platform level
- **AND** a student of that tenant submits an application without that document
- **THEN** the submission proceeds normally (`201 Created`) because the tenant's shadow takes precedence

#### Scenario: Tenant has added a custom required type not present at platform level
- **WHEN** a tenant has a tenant-specific document type with `isRequired = true` that does not exist at the platform level
- **AND** a student of that tenant submits an application without that document
- **THEN** the system returns `400 Bad Request` listing the missing tenant-specific required type

#### Scenario: No required document types exist in the tenant's effective view
- **WHEN** the tenant's merged document type list contains no types with `isRequired = true`
- **THEN** the document completeness check is a no-op and submission proceeds normally

---

### Requirement: Application submission form displays document pack readiness before submit
The Angular application submission form SHALL display a read-only document readiness summary listing all required document types, each marked as present (uploaded) or absent (not yet uploaded). The readiness summary SHALL be rendered before the submit button. The submit button SHALL be disabled when any required document type is absent.

#### Scenario: All required documents are uploaded
- **WHEN** the student views the application submission form and all required document types have been uploaded
- **THEN** each required type shows a green "uploaded" indicator
- **AND** the submit button is enabled

#### Scenario: One or more required documents are missing
- **WHEN** the student views the application submission form and at least one required document type has not been uploaded
- **THEN** the missing types are highlighted with a red indicator and a link to the document upload section
- **AND** the submit button is disabled

#### Scenario: Student navigates to document upload from the form
- **WHEN** the student clicks the document upload link in the readiness summary
- **THEN** they are navigated to the student dashboard documents section (`/dashboard/documents`)
- **AND** returning to the application form refreshes the readiness summary

---

### Requirement: Application submission no longer redirects to a separate document upload page
After a successful application submission, the Angular frontend SHALL navigate the student to their application dashboard (`/dashboard`) rather than to `/document-upload`. The `/document-upload` route SHALL remain accessible as a standalone dashboard section.

#### Scenario: Successful application submission redirects to dashboard
- **WHEN** a student successfully submits an application
- **THEN** the application navigates to `/dashboard`
- **AND** a success snackbar message is shown ("Application submitted successfully!")

#### Scenario: skipApplication guard is removed
- **WHEN** a student navigates directly to the document upload section without having submitted an application
- **THEN** the document upload section is accessible without any warning or redirect

---

### Requirement: Student dashboard includes a standalone document management section
The student dashboard SHALL include a dedicated "Documents" navigation item that routes to a document management page (`/dashboard/documents`). This page SHALL display the student's full document pack (all uploaded types with status) and allow uploading, replacing, or deleting documents for each type.

#### Scenario: Student views their document pack
- **WHEN** a student navigates to `/dashboard/documents`
- **THEN** they see a list of all document types (required and optional), each showing: type name, upload status (not uploaded / pending / approved / rejected), and actions (upload / replace / delete)

#### Scenario: Student uploads a document from the document pack page
- **WHEN** a student clicks "Upload" or "Replace" for a document type on the documents page and selects a file
- **THEN** `POST /api/documents/upload-single` is called with the selected file and document type
- **AND** the document pack list refreshes to show the new document with status PENDING

#### Scenario: Student deletes a document from the document pack page
- **WHEN** a student clicks "Delete" for an existing document on the documents page and confirms
- **THEN** `DELETE /api/documents/delete/{id}` is called
- **AND** the document pack list refreshes showing the type as "not uploaded"
