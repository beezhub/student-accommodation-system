## ADDED Requirements

### Requirement: Wizard steps route sequentially through the complete onboarding flow
The student onboarding wizard SHALL route through four steps in order: Account Creation → Profile Setup → Application Submission → Document Upload. On successful completion of each step, the frontend SHALL navigate to the next step in sequence rather than to an unrelated page. Specifically, successful application submission (step 3) SHALL navigate to `/document-upload` (step 4), not to `/dashboard`.

#### Scenario: Application submission navigates to document upload
- **WHEN** a student successfully submits an application in the wizard
- **THEN** the frontend SHALL navigate to `/document-upload`
- **AND** the progress strip SHALL show step 4 (Documents) as the active step

#### Scenario: Profile setup navigates to application submission
- **WHEN** a student successfully saves their profile in the wizard
- **THEN** the frontend SHALL navigate to `/application-submission`
- **AND** the progress strip SHALL show step 3 (Application) as the active step

#### Scenario: Document upload is the final wizard step
- **WHEN** a student completes document upload (step 4)
- **THEN** the frontend SHALL navigate to `/dashboard`
- **AND** a success snackbar SHALL confirm the onboarding is complete

---

### Requirement: Wizard progress strip accurately reflects the current step
The progress strip displayed across all wizard steps SHALL indicate the current step, completed steps, and remaining steps. The strip SHALL display exactly four steps: Account, Profile, Application, Documents. The active step indicator SHALL match the route the student is currently on.

#### Scenario: Progress strip on profile setup page
- **WHEN** a student is on the `/profile-setup` route
- **THEN** the progress strip SHALL show step 1 (Account) as completed and step 2 (Profile) as active

#### Scenario: Progress strip on application submission page
- **WHEN** a student is on the `/application-submission` route
- **THEN** the progress strip SHALL show steps 1-2 as completed and step 3 (Application) as active

#### Scenario: Progress strip on document upload page
- **WHEN** a student is on the `/document-upload` route
- **THEN** the progress strip SHALL show steps 1-3 as completed and step 4 (Documents) as active

---

### Requirement: Wizard steps support back navigation
Each wizard step after the first SHALL provide a "Back" button that navigates to the previous step. The back button SHALL preserve any data already entered on the current step (i.e., returning forward does not lose form state). Completed steps in the progress strip SHALL be clickable to navigate to that step.

#### Scenario: Back button on profile setup navigates to account info
- **WHEN** a student clicks "Back" on the profile setup step
- **THEN** the frontend SHALL navigate to the previous step (account/signup)

#### Scenario: Back button on application submission navigates to profile setup
- **WHEN** a student clicks "Back" on the application submission step
- **THEN** the frontend SHALL navigate to `/profile-setup`

#### Scenario: Back button on document upload navigates to application submission
- **WHEN** a student clicks "Back" on the document upload step
- **THEN** the frontend SHALL navigate to `/application-submission`

#### Scenario: Completed progress strip step is clickable
- **WHEN** a student clicks a completed step in the progress strip (e.g., step 2 from step 4)
- **THEN** the frontend SHALL navigate to that step's route

#### Scenario: Future progress strip steps are not clickable
- **WHEN** a student views the progress strip
- **THEN** steps after the current step SHALL NOT be clickable
