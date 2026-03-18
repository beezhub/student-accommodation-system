## ADDED Requirements

### Requirement: Building owner can submit a registration request
A prospective building owner SHALL be able to submit a registration request from a public-facing page without requiring an existing account. The request captures: full name, email address, organisation name, contact phone number, and an optional message. Submission creates an `owner_registration` record with status `PENDING` and triggers a verification email.

#### Scenario: Successful registration submission
- **WHEN** a visitor submits the registration form with all required fields valid
- **THEN** the system creates an `owner_registration` record with status `PENDING`, sends a verification email to the provided address, and displays a confirmation message instructing the user to verify their email

#### Scenario: Duplicate email rejected
- **WHEN** a visitor submits a registration form with an email already associated with any `app_user` or existing `owner_registration`
- **THEN** the system returns a `409 Conflict` error and displays "An account with this email already exists"

#### Scenario: Missing required fields rejected
- **WHEN** a visitor submits the registration form with any required field empty
- **THEN** the system returns `400 Bad Request` with field-level validation errors and does not create any record

### Requirement: Owner must verify email before registration is reviewable
After submitting a registration, the owner SHALL receive a time-limited email verification link. Until verified, the registration SHALL NOT appear in the admin review queue.

#### Scenario: Email verified successfully
- **WHEN** the owner clicks the verification link within 24 hours of submission
- **THEN** the `owner_registration` status changes to `PENDING_REVIEW` and the registration appears in the admin queue

#### Scenario: Verification link expired
- **WHEN** the owner clicks a verification link older than 24 hours
- **THEN** the system displays "This link has expired" and offers a resend option; the registration status remains `PENDING`

#### Scenario: Verification link resent
- **WHEN** the owner requests a new verification link from the expired-link page
- **THEN** the system generates a new token, invalidates the previous one, and sends a fresh verification email

### Requirement: Registration request is persisted independently of app_user
The `owner_registration` table SHALL store all submission data separately from `app_user`. No `app_user` record SHALL be created until admin approval.

#### Scenario: No login possible before approval
- **WHEN** a submitted owner attempts to log in using their registration email before approval
- **THEN** the authentication endpoint returns `401 Unauthorized` with no token issued
