## ADDED Requirements

### Requirement: Wizard auto-saves form state to localStorage
Each wizard step component (profile-setup, application-submission, document-upload) SHALL automatically save its form state to localStorage on every field change, debounced by 500ms. The storage key MUST follow the pattern `beezhub_wizard_{stepName}_{userId|'anon'}`.

#### Scenario: Field change triggers auto-save
- **WHEN** a student changes a field value in the profile-setup step
- **THEN** after 500ms of no further changes, the complete form state is saved to localStorage under `beezhub_wizard_profile-setup_{userId}`

#### Scenario: Rapid changes are debounced
- **WHEN** a student types 5 characters in quick succession in a text field
- **THEN** only one save operation occurs, 500ms after the last keystroke

### Requirement: Wizard restores saved state on load
Each wizard step component SHALL check localStorage for saved state on initialization. If valid saved state exists, the form MUST be pre-populated with the restored values and the student MUST be shown a notification: "We restored your previous progress."

#### Scenario: Student returns to wizard with saved state
- **WHEN** a student navigates to the profile-setup step and valid saved state exists in localStorage
- **THEN** the form fields are populated with the saved values and a snackbar displays "We restored your previous progress"

#### Scenario: No saved state exists
- **WHEN** a student navigates to the profile-setup step with no saved state in localStorage
- **THEN** the form loads with default/empty values and no restoration notification is shown

### Requirement: Saved state includes schema version
Each saved wizard state object SHALL include a `schemaVersion` number. When the application's wizard schema version is newer than the saved version, the saved state MUST be discarded and the student MUST be notified: "Your saved progress was from an older version and could not be restored."

#### Scenario: Schema version matches
- **WHEN** saved state has `schemaVersion: 3` and the current wizard version is 3
- **THEN** the state is restored normally

#### Scenario: Schema version mismatch
- **WHEN** saved state has `schemaVersion: 2` and the current wizard version is 3
- **THEN** the saved state is discarded, the form loads empty, and a snackbar displays "Your saved progress was from an older version and could not be restored"

### Requirement: Wizard state cleared on successful completion
When the wizard reaches successful completion (final submission), ALL wizard step localStorage entries for the current user MUST be cleared.

#### Scenario: Successful submission clears all wizard state
- **WHEN** the student successfully completes the document upload step (final step)
- **THEN** all localStorage keys matching `beezhub_wizard_*_{userId}` are removed

### Requirement: Stale reference data is validated on restore
When restoring saved state, any values that reference server-side data (institution ID, year of study ID, academic year ID) MUST be validated against current reference data. Invalid references SHALL be cleared from the restored form with a field-level indicator.

#### Scenario: Saved institution ID no longer exists
- **WHEN** saved state contains `institutionId: 5` but institution 5 no longer exists in the current reference data
- **THEN** the institution field is cleared (not pre-populated) and a helper text displays "Previously selected option is no longer available"

#### Scenario: All references valid
- **WHEN** all reference IDs in saved state match current reference data
- **THEN** all fields are populated with no validation warnings

### Requirement: Manual discard option
Each wizard step with restored state SHALL provide a "Start fresh" action that clears the saved state for that step and resets the form to empty.

#### Scenario: Student discards saved state
- **WHEN** a student clicks "Start fresh" on a step with restored state
- **THEN** the localStorage entry for that step is deleted, the form resets to empty, and the restoration notification is dismissed
