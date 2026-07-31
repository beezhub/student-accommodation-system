## Requirements

### Requirement: Onboarding wizard order is Account, Profile, Documents, Application, Review
The student onboarding wizard SHALL proceed in the following order: account creation, profile setup, document upload, application form, and review. Profile setup SHALL navigate to the document upload step (`/document-upload`); the document upload step SHALL navigate to the application form (`/application-submission`); the application form SHALL navigate to the review step (`/application-review`). The `/document-upload` route SHALL remain accessible as a standalone dashboard section.

#### Scenario: Profile setup navigates to document upload
- **WHEN** a student completes profile setup
- **THEN** the application navigates to `/document-upload`

#### Scenario: Document upload navigates to the application form
- **WHEN** a student continues from the document upload step
- **THEN** the application navigates to `/application-submission`

#### Scenario: Wizard progress strips are consistent across steps
- **WHEN** a student views any wizard step
- **THEN** the progress strip stations are labelled consistently (Account, Profile, Documents, Application) with the current station marked active

---

### Requirement: Application submission form displays document pack readiness before continuing
The Angular application submission form SHALL display a read-only document readiness summary listing all required document types (tenant-aware, re-evaluated when building preferences change), each marked as present (uploaded) or absent (not yet uploaded). The readiness summary SHALL be rendered before the continue button. The continue button SHALL be disabled when any required document type is absent. Additionally, the application form SHALL collect the deferred profile fields (student number, phone, date of birth, gender, special requirements) that are not collected during profile setup.

#### Scenario: All required documents are uploaded
- **WHEN** the student views the application submission form and all required document types have been uploaded
- **THEN** each required type shows a green "uploaded" indicator
- **AND** the continue button is enabled (assuming all required fields are also filled)

#### Scenario: One or more required documents are missing
- **WHEN** the student views the application submission form and at least one required document type has not been uploaded
- **THEN** the missing types are highlighted with a red indicator and a link to the document upload section
- **AND** the continue button is disabled

#### Scenario: Student navigates to document upload from the form
- **WHEN** the student clicks the document upload link in the readiness summary
- **THEN** they are navigated to the documents section (`/documents`)
- **AND** returning to the application form refreshes the readiness summary

#### Scenario: Application form collects deferred profile fields
- **WHEN** a student opens the application submission form
- **THEN** the form includes fields for: student number, phone number, date of birth, gender, and special requirements
- **AND** these fields are pre-populated if the student previously entered them on their profile

---

### Requirement: Profile setup requires only institution and year of study
The profile setup wizard step SHALL require only two fields for completion: institution and year of study. All other student profile fields (student number, phone, date of birth, gender, special requirements) SHALL be optional at the profile setup stage. The backend `PUT /api/students/profile` endpoint SHALL accept a partial profile containing only institution and year of study without returning a validation error.

#### Scenario: Student completes profile setup with minimal fields
- **WHEN** a student fills in only institution and year of study on the profile setup form and submits
- **THEN** the profile is saved successfully
- **AND** the student is navigated to the next wizard step

#### Scenario: Student provides additional fields at profile setup
- **WHEN** a student fills in institution, year of study, and optionally other fields (student number, phone, etc.)
- **THEN** all provided fields are saved successfully

#### Scenario: Backend validates full profile at application submission
- **WHEN** a student submits an application while their profile is missing deferred fields (student number, phone, date of birth, gender)
- **THEN** the backend rejects the submission with a message listing the missing fields

---

### Requirement: Application review step performs the actual application submission
The application review wizard step SHALL display a read-only summary of all application data (preferences, deferred profile fields) and SHALL include a "Submit Application" button that triggers the `POST /api/application` API call. The previous step (application form) SHALL save form data locally via a draft mechanism but SHALL NOT submit the application. On success, the review step SHALL navigate to the application confirmation page.

#### Scenario: Review step shows complete application summary
- **WHEN** a student navigates to the application review step
- **THEN** the page displays a read-only summary of: selected building preferences, personal details, special requirements, and institution information

#### Scenario: Submit button on review step creates the application
- **WHEN** a student clicks "Submit Application" on the review step
- **THEN** the `POST /api/application` endpoint is called with the application data
- **AND** on success, the student is navigated to `/application-confirmation/:id` with a success snackbar

#### Scenario: Review step submit button is disabled when data is incomplete
- **WHEN** the application draft is missing or the terms checkbox is unchecked
- **THEN** the "Submit Application" button is disabled

---

### Requirement: Document upload step resolves required document types using the application's tenant context
The document upload wizard step SHALL pass the tenant ID derived from the student's application context (the tenant of the selected preferred buildings) when calling the API to fetch required document types. The component SHALL NOT rely on a hardcoded or default tenant ID.

#### Scenario: Required documents reflect the application's target tenant
- **WHEN** a student reaches the document upload step in the context of buildings belonging to tenant X
- **THEN** the component SHALL call the required document types API with tenant ID = X
- **AND** the displayed list of required documents SHALL match tenant X's effective document type configuration

#### Scenario: Fallback when tenant ID is unavailable
- **WHEN** a student navigates directly to `/document-upload` without an application context (e.g., from the dashboard)
- **THEN** the component SHALL use the student's default tenant context to fetch required document types

---

### Requirement: Document upload button is labelled "Upload Documents"
The primary action button on the document upload wizard step SHALL be labelled "Upload Documents". The label SHALL NOT read "Complete Registration" or any other label that misrepresents the action being performed.

#### Scenario: Button label on document upload step
- **WHEN** a student views the document upload wizard step
- **THEN** the primary action button SHALL display the text "Upload Documents"
