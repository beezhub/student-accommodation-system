## ADDED Requirements

### Requirement: Application submission requires all required document types to be present in the student's pack
The `POST /api/application` endpoint SHALL, before saving the application, verify that the authenticated student has at least one document of every `DocumentType` where `isRequired = true`. If any required document type is absent from the student's pack, the system SHALL return `400 Bad Request` with a response body listing the names of the missing document types. A document in any status (PENDING, APPROVED, REJECTED) satisfies the presence check — the absence check is only about whether the student has uploaded the document at all.

#### Scenario: Student has all required documents uploaded
- **WHEN** a student submits an application and their document pack contains at least one document for each `isRequired = true` document type (regardless of each document's status)
- **THEN** the application submission proceeds normally and returns `201 Created`

#### Scenario: Student is missing one required document type
- **WHEN** a student submits an application and their document pack is missing a document for at least one `isRequired = true` document type
- **THEN** the system returns `400 Bad Request`
- **AND** the response body includes the names of the missing required document types

#### Scenario: No required document types exist in the system
- **WHEN** there are no `DocumentType` records with `isRequired = true`
- **THEN** the document completeness check is a no-op and submission proceeds normally

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

### Requirement: Application submission no longer redirects to a separate document upload page
After a successful application submission, the Angular frontend SHALL navigate the student to their application dashboard (`/dashboard`) rather than to `/document-upload`. The `/document-upload` route SHALL remain accessible as a standalone dashboard section.

#### Scenario: Successful application submission redirects to dashboard
- **WHEN** a student successfully submits an application
- **THEN** the application navigates to `/dashboard`
- **AND** a success snackbar message is shown ("Application submitted successfully!")

#### Scenario: skipApplication guard is removed
- **WHEN** a student navigates directly to the document upload section without having submitted an application
- **THEN** the document upload section is accessible without any warning or redirect

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
