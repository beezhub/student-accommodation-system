## MODIFIED Requirements

### Requirement: Application submission form displays document pack readiness before submit
The Angular application submission form SHALL display a read-only document readiness summary listing all required document types, each marked as present (uploaded) or absent (not yet uploaded). The readiness summary SHALL be rendered before the submit button. The submit button SHALL be disabled when any required document type is absent. Additionally, the application form SHALL collect the deferred profile fields (student number, phone, date of birth, gender, special requirements) that are no longer collected during profile setup.

#### Scenario: All required documents are uploaded
- **WHEN** the student views the application submission form and all required document types have been uploaded
- **THEN** each required type shows a green "uploaded" indicator
- **AND** the submit button is enabled (assuming all required fields are also filled)

#### Scenario: One or more required documents are missing
- **WHEN** the student views the application submission form and at least one required document type has not been uploaded
- **THEN** the missing types are highlighted with a red indicator and a link to the document upload section
- **AND** the submit button is disabled

#### Scenario: Student navigates to document upload from the form
- **WHEN** the student clicks the document upload link in the readiness summary
- **THEN** they are navigated to the student dashboard documents section (`/dashboard/documents`)
- **AND** returning to the application form refreshes the readiness summary

#### Scenario: Application form collects deferred profile fields
- **WHEN** a student opens the application submission form
- **THEN** the form includes fields for: student number, phone number, date of birth, gender, and special requirements
- **AND** these fields are pre-populated if the student previously entered them on their profile

---

## ADDED Requirements

### Requirement: Profile setup requires only institution and year of study
The profile setup wizard step SHALL require only two fields for completion: institution and year of study. All other student profile fields (student number, phone, date of birth, gender, special requirements) SHALL be optional at the profile setup stage. The backend `PUT /api/students/profile` endpoint SHALL accept a partial profile containing only institution and year of study without returning a validation error.

#### Scenario: Student completes profile setup with minimal fields
- **WHEN** a student fills in only institution and year of study on the profile setup form and submits
- **THEN** the profile is saved successfully
- **AND** the student is navigated to the next wizard step

#### Scenario: Student provides additional fields at profile setup
- **WHEN** a student fills in institution, year of study, and optionally other fields (student number, phone, etc.)
- **THEN** all provided fields are saved successfully

#### Scenario: Profile guard checks only institution and year of study
- **WHEN** a student has a profile with institution and year of study filled in (but other fields blank)
- **THEN** the profile-required guard allows the student to proceed to browse and other authenticated pages

---

### Requirement: Application review step performs the actual application submission
The application review wizard step SHALL display a read-only summary of all application data (preferences, deferred profile fields, document readiness) and SHALL include a "Submit Application" button that triggers the `POST /api/application` API call. The previous step (application form) SHALL save form data locally or via a draft mechanism but SHALL NOT submit the application.

#### Scenario: Review step shows complete application summary
- **WHEN** a student navigates to the application review step
- **THEN** the page displays a read-only summary of: selected building preferences, accommodation type, move-in date, duration, room type, special requirements, and document readiness

#### Scenario: Submit button on review step creates the application
- **WHEN** a student clicks "Submit Application" on the review step
- **THEN** the `POST /api/application` endpoint is called with the application data
- **AND** on success, the student is navigated to the application confirmation page with a success snackbar

> Amendment (merge with develop wizard reorder): the wizard order is Account → Profile → Documents → Application → Review. Documents are uploaded before the application step, so review success navigates to `/application-confirmation/:id` instead of the document upload step.

#### Scenario: Review step submit button is disabled when data is incomplete
- **WHEN** the application data is missing required fields or required documents
- **THEN** the "Submit Application" button is disabled
- **AND** a message indicates what is missing
