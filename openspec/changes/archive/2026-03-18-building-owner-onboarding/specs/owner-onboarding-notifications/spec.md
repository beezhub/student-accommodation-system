## ADDED Requirements

### Requirement: Registration confirmation email sent on submission
The system SHALL send a confirmation email to the owner immediately after a successful registration submission, before email verification.

#### Scenario: Confirmation email delivered after submission
- **WHEN** a building owner submits a valid registration form
- **THEN** the system sends an email to the provided address with subject "Registration received — please verify your email" containing the email verification link

### Requirement: Email verification reminder sent after 24 hours of inactivity
If an `owner_registration` remains in `PENDING` (unverified) status for 24 hours, the system SHALL send a single reminder email with a refreshed verification link.

#### Scenario: Reminder sent after inactivity
- **WHEN** 24 hours pass and an `owner_registration` remains in `PENDING` status
- **THEN** the system sends one reminder email with a new verification link and updates the token expiry

#### Scenario: No duplicate reminders
- **WHEN** a reminder has already been sent for a `PENDING` registration
- **THEN** the system does not send additional reminders regardless of further inactivity

### Requirement: Approval notification email sent when registration is approved
The system SHALL send an approval email to the owner when an admin approves their registration.

#### Scenario: Approval email contains activation link
- **WHEN** an admin approves a registration
- **THEN** the system sends an email with subject "Your building owner account has been approved" containing a 48-hour password-setup link and the owner portal URL

### Requirement: Rejection notification email sent when registration is rejected
The system SHALL send a rejection email to the owner when an admin rejects their registration.

#### Scenario: Rejection email with reason
- **WHEN** an admin rejects a registration with a reason
- **THEN** the system sends an email with subject "Update on your building owner registration" including the admin's reason and instructions to reapply

#### Scenario: Rejection email without reason
- **WHEN** an admin rejects a registration without providing a reason
- **THEN** the system sends an email with subject "Update on your building owner registration" with a generic message and support contact

### Requirement: All onboarding emails are sent asynchronously
All owner onboarding notification emails SHALL be sent via the existing `@Async` email service. Failures SHALL be logged and retried using the existing notification retry mechanism.

#### Scenario: Email send failure triggers retry
- **WHEN** an onboarding email fails to send
- **THEN** the system logs the failure and schedules a retry according to the existing `NotificationService` retry policy
