## ADDED Requirements

### Requirement: All critical-path actions provide user-facing error feedback on failure
Every student-facing action that calls a backend API and whose failure blocks the student from progressing SHALL display a user-facing error message via MatSnackBar when the API call fails. No critical-path failure SHALL be silently logged to the console without notifying the student. Critical-path actions include: profile save, application submission, document upload, application data loading, and document list loading.

#### Scenario: Profile save failure shows error snackbar
- **WHEN** a student submits the profile setup form and the `PUT /api/students/profile` call fails
- **THEN** a MatSnackBar SHALL be displayed with panelClass `error-snackbar` and a message indicating the profile could not be saved
- **AND** the error SHALL also be logged to the console for debugging

#### Scenario: Application load failure shows error snackbar
- **WHEN** a student navigates to the dashboard and the application data API call fails
- **THEN** a MatSnackBar SHALL be displayed with panelClass `error-snackbar` and a message indicating application data could not be loaded

#### Scenario: Document list load failure shows error snackbar
- **WHEN** a student's dashboard loads and the document list API call fails
- **THEN** a MatSnackBar SHALL be displayed with panelClass `error-snackbar` and a message indicating documents could not be loaded
- **AND** the documents section SHALL display an empty state (not mock data)

#### Scenario: Document upload failure shows error snackbar
- **WHEN** a student uploads a document and the `POST /api/documents/upload-single` call fails
- **THEN** a MatSnackBar SHALL be displayed with panelClass `error-snackbar` and a message indicating the upload failed

---

### Requirement: OnPush components render asynchronous data reliably
All wizard step components using `ChangeDetectionStrategy.OnPush` that load data asynchronously (via HTTP or observable subscriptions) SHALL use Angular's `async` pipe in templates to ensure the view updates when data arrives. Direct property assignment from subscriptions without change detection notification SHALL NOT be used in OnPush components.

#### Scenario: Profile setup dropdowns render on data load
- **WHEN** a student navigates to the profile setup step and the component fetches dropdown options (institutions, years of study, academic years) from the API
- **THEN** the dropdown fields SHALL render the fetched options without requiring manual change detection triggers

#### Scenario: Document upload progress renders on state change
- **WHEN** a student is on the document upload step and upload progress or required document types arrive from the API
- **THEN** the progress indicators and document type list SHALL render immediately without visual delay or blank states

#### Scenario: Async pipe manages subscription lifecycle
- **WHEN** a student navigates away from a wizard step that uses async pipes
- **THEN** the subscriptions SHALL be automatically unsubscribed by the async pipe, preventing memory leaks
