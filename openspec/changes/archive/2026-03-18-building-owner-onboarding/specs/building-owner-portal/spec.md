## ADDED Requirements

### Requirement: Building owner can set a password and activate their account
After receiving an approval email, the owner SHALL follow the password-setup link to set a password and activate their account. The link SHALL expire after 48 hours.

#### Scenario: Successful password setup
- **WHEN** the owner visits the password-setup link within 48 hours and submits a valid password
- **THEN** the account is activated, the setup token is invalidated, and the owner is redirected to the owner portal login page

#### Scenario: Expired password-setup link
- **WHEN** the owner visits a password-setup link older than 48 hours
- **THEN** the system displays "This link has expired. Please contact support." and the account remains inactive

#### Scenario: Password validation enforced
- **WHEN** the owner submits a password shorter than 8 characters or missing complexity requirements
- **THEN** the system displays field-level validation errors and does not activate the account

### Requirement: Building owner can log in to the owner portal
An approved and activated building owner SHALL be able to authenticate using their email and password. Successful login SHALL issue a JWT with role `ROLE_BUILDING_OWNER`.

#### Scenario: Successful owner login
- **WHEN** an approved owner submits valid credentials at the login endpoint
- **THEN** the system issues a JWT token containing `ROLE_BUILDING_OWNER` and the owner's `owner_profile_id`

#### Scenario: Unapproved owner login attempt blocked
- **WHEN** a user whose `owner_registration` is `PENDING_REVIEW` or `REJECTED` attempts to log in
- **THEN** the system returns `401 Unauthorized`

### Requirement: Owner portal dashboard shows owner's buildings
The owner portal home page SHALL display a summary of all buildings assigned to the authenticated owner, including: building name, address, total rooms, and current occupancy count.

#### Scenario: Owner views their buildings
- **WHEN** an authenticated owner navigates to the portal dashboard
- **THEN** the system displays only buildings where `building.owner_id` matches the authenticated owner's profile ID

#### Scenario: Owner with no assigned buildings sees empty state
- **WHEN** an authenticated owner has no buildings assigned
- **THEN** the portal displays "No buildings assigned yet. Contact your administrator."

#### Scenario: Owner cannot access another owner's buildings
- **WHEN** an authenticated owner requests building data not assigned to them (e.g., via direct URL or API call)
- **THEN** the system returns `403 Forbidden`

### Requirement: Owner portal access is blocked for non-owner roles
All routes under `/owner/**` SHALL be protected by `OwnerGuard`. Users with `ROLE_STUDENT` or `ROLE_ADMIN` SHALL be redirected to their respective home pages.

#### Scenario: Admin attempts to access owner portal
- **WHEN** an authenticated admin navigates to `/owner/dashboard`
- **THEN** the guard redirects them to `/admin/home`

#### Scenario: Unauthenticated user attempts to access owner portal
- **WHEN** an unauthenticated user navigates to any `/owner/**` route
- **THEN** the guard redirects to `/login`
