## ADDED Requirements

### Requirement: All student-facing error handlers include user-facing feedback
Every `console.error` or error handler in a student-facing Angular component that handles an API failure or critical operation failure SHALL include a user-facing notification via MatSnackBar, in addition to the existing console logging. The console logging SHALL be retained for developer debugging. No student-facing error handler SHALL silently log without notifying the user.

#### Scenario: Error handler with actionable retry shows snackbar with action
- **WHEN** a student-facing API call fails and the operation can be retried by the user (e.g., saving, loading data, uploading)
- **THEN** a MatSnackBar SHALL be displayed with `panelClass: 'error-snackbar'` and a descriptive message (e.g., "Failed to load your applications. Please try again.")
- **AND** the snackbar MAY include a "Retry" action button where applicable

#### Scenario: Error handler for non-retryable failure shows informational snackbar
- **WHEN** a student-facing operation fails and the user cannot directly retry (e.g., background data load, initialisation failure)
- **THEN** a MatSnackBar SHALL be displayed with `panelClass: 'error-snackbar'` and an informational message (e.g., "Something went wrong. Please refresh the page.")

#### Scenario: Console logging is preserved alongside user feedback
- **WHEN** any student-facing error handler fires
- **THEN** the existing `console.error` (or equivalent) logging SHALL remain in place
- **AND** the user-facing MatSnackBar notification SHALL be an addition, not a replacement

---

### Requirement: Student-facing components contain no silent error handlers
After the audit is complete, no student-facing Angular component SHALL contain an error handler (in `subscribe`, `catchError`, `.catch`, or try/catch blocks) that logs to the console without also displaying a user-facing notification. This applies to all components under: `features/auth/`, `features/dashboard/`, `features/student/`, and `shared/` (when used in student contexts).

#### Scenario: Audit confirms zero silent handlers
- **WHEN** the student-facing codebase is searched for `console.error` patterns
- **THEN** every match SHALL have a corresponding `MatSnackBar.open()` or `ErrorHandlingService` call within the same error handling block
