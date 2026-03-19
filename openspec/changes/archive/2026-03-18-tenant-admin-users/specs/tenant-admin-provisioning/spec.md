## ADDED Requirements

### Requirement: Platform admin creates a tenant admin user
The system SHALL allow a `PLATFORM_ADMIN` to create a `TENANT_ADMIN` user scoped to a specific tenant by calling `POST /api/admin/tenants/{tenantId}/admins`. The created user SHALL have `must_change_password = true` and `email_verified = true` (provisioned accounts skip email verification).

#### Scenario: Successful creation
- **WHEN** a `PLATFORM_ADMIN` sends `POST /api/admin/tenants/{tenantId}/admins` with valid `firstName`, `lastName`, `email`, and `password` fields
- **THEN** the system creates an `AppUser` with `user_role = TENANT_ADMIN`, `tenant_id = {tenantId}`, `must_change_password = true`, `email_verified = true`, and returns `201 Created` with the new user's id, email, firstName, lastName, and createdAt

#### Scenario: Tenant does not exist
- **WHEN** a `PLATFORM_ADMIN` sends `POST /api/admin/tenants/{tenantId}/admins` with a `tenantId` that does not exist
- **THEN** the system returns `404 Not Found`

#### Scenario: Tenant is inactive
- **WHEN** a `PLATFORM_ADMIN` sends `POST /api/admin/tenants/{tenantId}/admins` for a tenant with `is_active = false`
- **THEN** the system returns `409 Conflict` with an error message indicating the tenant is inactive

#### Scenario: Email already in use
- **WHEN** a `PLATFORM_ADMIN` sends `POST /api/admin/tenants/{tenantId}/admins` with an email that already exists in `app_user`
- **THEN** the system returns `409 Conflict` with an error message indicating the email is taken

#### Scenario: Non-platform-admin is rejected
- **WHEN** a user without `PLATFORM_ADMIN` role calls `POST /api/admin/tenants/{tenantId}/admins`
- **THEN** the system returns `403 Forbidden`

### Requirement: Platform admin lists tenant admin users
The system SHALL allow a `PLATFORM_ADMIN` to retrieve all `TENANT_ADMIN` users for a given tenant via `GET /api/admin/tenants/{tenantId}/admins`.

#### Scenario: Successful list
- **WHEN** a `PLATFORM_ADMIN` sends `GET /api/admin/tenants/{tenantId}/admins` for an existing tenant
- **THEN** the system returns `200 OK` with a JSON array of tenant admin user objects (id, email, firstName, lastName, createdAt, mustChangePassword, accountLockedUntil)

#### Scenario: Empty list
- **WHEN** a `PLATFORM_ADMIN` sends `GET /api/admin/tenants/{tenantId}/admins` and no `TENANT_ADMIN` users exist for that tenant
- **THEN** the system returns `200 OK` with an empty array

#### Scenario: Non-platform-admin is rejected
- **WHEN** a user without `PLATFORM_ADMIN` role calls `GET /api/admin/tenants/{tenantId}/admins`
- **THEN** the system returns `403 Forbidden`

### Requirement: Platform admin deactivates a tenant admin user
The system SHALL allow a `PLATFORM_ADMIN` to deactivate a `TENANT_ADMIN` user by locking their account via `DELETE /api/admin/tenants/{tenantId}/admins/{userId}`. The user record is NOT deleted; `account_locked_until` SHALL be set to a far-future date (effectively permanent lock).

#### Scenario: Successful deactivation
- **WHEN** a `PLATFORM_ADMIN` sends `DELETE /api/admin/tenants/{tenantId}/admins/{userId}` for an existing active `TENANT_ADMIN`
- **THEN** the system sets `account_locked_until` to a far-future timestamp and returns `200 OK`

#### Scenario: User not found or not a TENANT_ADMIN of that tenant
- **WHEN** a `PLATFORM_ADMIN` sends `DELETE /api/admin/tenants/{tenantId}/admins/{userId}` where `userId` does not exist or does not belong to `tenantId` as a `TENANT_ADMIN`
- **THEN** the system returns `404 Not Found`

#### Scenario: Non-platform-admin is rejected
- **WHEN** a user without `PLATFORM_ADMIN` role calls `DELETE /api/admin/tenants/{tenantId}/admins/{userId}`
- **THEN** the system returns `403 Forbidden`

### Requirement: Provisioned tenant admin receives welcome email
The system SHALL send a welcome email to the newly created `TENANT_ADMIN` user containing their login email and a prompt to change their password on first login.

#### Scenario: Email sent on creation
- **WHEN** a `PLATFORM_ADMIN` successfully creates a `TENANT_ADMIN` user
- **THEN** the system asynchronously sends a welcome email to the new user's email address containing the login URL and a note that a password change is required on first login

#### Scenario: Email failure does not rollback user creation
- **WHEN** the email service fails to deliver the welcome email
- **THEN** the user account is still created and the system logs the email failure for retry via the existing notification retry mechanism
