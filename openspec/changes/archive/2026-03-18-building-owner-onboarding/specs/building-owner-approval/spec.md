## ADDED Requirements

### Requirement: Admin can view pending owner registration requests
Admins SHALL be able to view a paginated list of all `owner_registration` records with status `PENDING_REVIEW`. Each entry SHALL display: applicant name, email, organisation, submission date, and verification status.

#### Scenario: Admin views registration queue
- **WHEN** an admin navigates to the owner registrations page
- **THEN** the system displays all `PENDING_REVIEW` registrations sorted by submission date (oldest first)

#### Scenario: Empty queue shown gracefully
- **WHEN** there are no `PENDING_REVIEW` registrations
- **THEN** the system displays an empty state message: "No pending registrations"

### Requirement: Admin can approve a building owner registration
When an admin approves a registration, the system SHALL create an `app_user` record with role `ROLE_BUILDING_OWNER`, create a linked `owner_profile` record, send an approval email with a password-setup link, and update the `owner_registration` status to `APPROVED`.

#### Scenario: Successful approval
- **WHEN** an admin clicks Approve on a `PENDING_REVIEW` registration
- **THEN** the system creates an `app_user` + `owner_profile`, marks the registration `APPROVED`, sends an approval email, and removes the entry from the pending queue

#### Scenario: Approval email contains password-setup link
- **WHEN** the approval email is sent
- **THEN** it contains a time-limited (48-hour) link to a password-setup page specific to the owner portal

#### Scenario: Approving already-approved registration fails gracefully
- **WHEN** an admin attempts to approve a registration already in `APPROVED` status
- **THEN** the system returns `409 Conflict` with message "This registration has already been processed"

### Requirement: Admin can reject a building owner registration
When an admin rejects a registration, the system SHALL update status to `REJECTED`, send a rejection notification email with the admin's optional reason, and remove the entry from the pending queue.

#### Scenario: Successful rejection with reason
- **WHEN** an admin clicks Reject, optionally enters a reason, and confirms
- **THEN** the system marks the registration `REJECTED`, sends a rejection email containing the reason (if provided), and removes it from the queue

#### Scenario: Rejection without reason
- **WHEN** an admin rejects without entering a reason
- **THEN** the system sends a generic rejection email and marks the registration `REJECTED`

### Requirement: Rejected owner can re-apply
A rejected owner SHALL be able to submit a new registration using the same email address. The previous `REJECTED` record SHALL be retained for audit purposes.

#### Scenario: Re-registration with previously rejected email
- **WHEN** an owner with a `REJECTED` registration submits a new registration with the same email
- **THEN** the system creates a new `owner_registration` record with status `PENDING`, leaving the old record intact
