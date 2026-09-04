## ADDED Requirements

### Requirement: No student-facing flow uses raw browser alert() for feedback
All student-facing components SHALL use `MatSnackBar` with the appropriate semantic panel class (`success-snackbar`, `error-snackbar`, `warning-snackbar`, `info-snackbar`) for user notifications. No student-facing component SHALL call `window.alert()`, `alert()`, or `window.confirm()` for feedback messages. Specifically, the following existing `alert()` calls SHALL be replaced:
- Dashboard component: application cancellation confirmation and result messages
- Applications component: cancel application stub message
- Notifications component: "View all" placeholder message

#### Scenario: Application cancellation uses snackbar instead of alert
- **WHEN** a student cancels an application from the dashboard
- **THEN** the result (success or failure) SHALL be communicated via a MatSnackBar with the appropriate panel class
- **AND** no `alert()` dialog SHALL appear

#### Scenario: Cancel application stub uses snackbar
- **WHEN** a student triggers the cancel action in the applications list component
- **THEN** the feedback SHALL be delivered via a MatSnackBar (not an `alert()` dialog)

#### Scenario: Notifications "View all" action is functional or removed
- **WHEN** a student clicks the "View all notifications" button
- **THEN** the button SHALL either navigate to a notifications list page or be removed from the UI
- **AND** no `alert()` dialog SHALL appear

---

### Requirement: Token expiry message is user-friendly
The token expiry interceptor SHALL display a human-readable message when a JWT token expires, rather than a technical error string. The message SHALL inform the user their session has expired and prompt them to sign in again. The interceptor SHALL preserve the user's intended destination URL so they can be redirected back after re-authentication.

#### Scenario: Expired token shows friendly message
- **WHEN** a student's JWT token expires during a session
- **THEN** a MatSnackBar SHALL display the message "Your session has expired. Please sign in again." (or equivalent user-friendly wording)
- **AND** the snackbar SHALL use panelClass `warning-snackbar`

#### Scenario: Return URL is preserved on token expiry
- **WHEN** a student's token expires while they are on route `/dashboard/documents`
- **THEN** the interceptor SHALL redirect to the login page with a `returnUrl` query parameter set to `/dashboard/documents`
- **AND** after successful re-authentication, the student SHALL be navigated back to `/dashboard/documents`
