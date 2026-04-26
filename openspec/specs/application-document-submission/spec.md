## MODIFIED Requirements

### Requirement: Application submission no longer redirects to a separate document upload page
After a successful application submission, the Angular frontend SHALL navigate the student to the document upload wizard step (`/document-upload`) to continue the onboarding flow rather than to `/dashboard`. The `/document-upload` route SHALL remain accessible as a standalone dashboard section.

#### Scenario: Successful application submission redirects to document upload
- **WHEN** a student successfully submits an application
- **THEN** the application navigates to `/document-upload`
- **AND** a success snackbar message is shown ("Application submitted successfully!")

#### Scenario: skipApplication guard is removed
- **WHEN** a student navigates directly to the document upload section without having submitted an application
- **THEN** the document upload section is accessible without any warning or redirect

---

## ADDED Requirements

### Requirement: Document upload step resolves required document types using the application's tenant context
The document upload wizard step SHALL pass the tenant ID derived from the student's application context (the tenant of the selected preferred buildings) when calling the API to fetch required document types. The component SHALL NOT rely on a hardcoded or default tenant ID.

#### Scenario: Required documents reflect the application's target tenant
- **WHEN** a student reaches the document upload step after submitting an application for buildings belonging to tenant X
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
