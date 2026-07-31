## ADDED Requirements

### Requirement: Student dashboard document section uses empty state on API failure
The student dashboard documents section SHALL display an `EmptyStateComponent` (or equivalent empty state pattern) when the document list API call fails or returns an empty result. The component SHALL NOT fall back to hardcoded mock document data. The `uploadedDocuments` array SHALL be initialised as an empty array (`[]`), not with placeholder objects.

#### Scenario: Document API failure shows empty state
- **WHEN** a student's dashboard loads and the document list API call fails
- **THEN** the documents section SHALL display an empty state with an appropriate icon and message (e.g., "Unable to load documents. Please try again later.")
- **AND** no mock or hardcoded document entries SHALL be displayed

#### Scenario: No documents uploaded shows empty state
- **WHEN** a student's dashboard loads and the document list API returns an empty array
- **THEN** the documents section SHALL display an empty state with a message (e.g., "No documents uploaded yet") and a call-to-action to upload documents

#### Scenario: Mock document data is removed from component initialisation
- **WHEN** the dashboard component initialises
- **THEN** the `uploadedDocuments` property SHALL be initialised as `[]`
- **AND** no hardcoded document objects with fabricated statuses (e.g., `{ name: "ID Document", status: "Verified" }`) SHALL exist in the component source code
