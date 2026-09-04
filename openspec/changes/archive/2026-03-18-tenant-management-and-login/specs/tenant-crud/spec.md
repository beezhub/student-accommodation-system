## ADDED Requirements

### Requirement: Create Tenant
The system SHALL expose `POST /api/admin/tenants` allowing a `PLATFORM_ADMIN` to create a new tenant with a required `name` and optional `domain`.

#### Scenario: Platform admin creates a tenant successfully
- **WHEN** a `PLATFORM_ADMIN` sends `POST /api/admin/tenants` with a valid `name`
- **THEN** the system returns `201 Created` with the new tenant object (id, name, domain, isActive=true, createdAt, updatedAt)

#### Scenario: Duplicate tenant name is rejected
- **WHEN** a `PLATFORM_ADMIN` sends `POST /api/admin/tenants` with a `name` that already exists
- **THEN** the system returns `409 Conflict`

#### Scenario: Missing name is rejected
- **WHEN** a `PLATFORM_ADMIN` sends `POST /api/admin/tenants` with no `name` field
- **THEN** the system returns `400 Bad Request`

#### Scenario: Non-platform-admin is rejected
- **WHEN** a user without `PLATFORM_ADMIN` role sends `POST /api/admin/tenants`
- **THEN** the system returns `403 Forbidden`

### Requirement: Update Tenant
The system SHALL expose `PUT /api/admin/tenants/{id}` allowing a `PLATFORM_ADMIN` to update a tenant's `name` and `domain`. The platform tenant (id=0) SHALL NOT be editable.

#### Scenario: Platform admin updates a tenant successfully
- **WHEN** a `PLATFORM_ADMIN` sends `PUT /api/admin/tenants/{id}` with valid fields and `id > 0`
- **THEN** the system returns `200 OK` with the updated tenant object

#### Scenario: Platform tenant edit is rejected
- **WHEN** a `PLATFORM_ADMIN` sends `PUT /api/admin/tenants/0`
- **THEN** the system returns `400 Bad Request` with message "Platform tenant cannot be modified"

#### Scenario: Tenant not found
- **WHEN** a `PLATFORM_ADMIN` sends `PUT /api/admin/tenants/{id}` with a non-existent ID
- **THEN** the system returns `404 Not Found`

#### Scenario: Non-platform-admin is rejected
- **WHEN** a user without `PLATFORM_ADMIN` role sends `PUT /api/admin/tenants/{id}`
- **THEN** the system returns `403 Forbidden`

### Requirement: Toggle Tenant Active Status
The system SHALL expose `PATCH /api/admin/tenants/{id}/status` allowing a `PLATFORM_ADMIN` to toggle the `isActive` flag on a tenant. The platform tenant (id=0) SHALL NOT be deactivatable.

#### Scenario: Platform admin deactivates a tenant
- **WHEN** a `PLATFORM_ADMIN` sends `PATCH /api/admin/tenants/{id}/status` with `{ "isActive": false }` and `id > 0`
- **THEN** the system returns `200 OK` with the tenant object showing `isActive: false`

#### Scenario: Platform admin reactivates a tenant
- **WHEN** a `PLATFORM_ADMIN` sends `PATCH /api/admin/tenants/{id}/status` with `{ "isActive": true }` and `id > 0`
- **THEN** the system returns `200 OK` with the tenant object showing `isActive: true`

#### Scenario: Platform tenant deactivation is rejected
- **WHEN** a `PLATFORM_ADMIN` sends `PATCH /api/admin/tenants/0/status`
- **THEN** the system returns `400 Bad Request` with message "Platform tenant cannot be deactivated"

#### Scenario: Non-platform-admin is rejected
- **WHEN** a user without `PLATFORM_ADMIN` role sends `PATCH /api/admin/tenants/{id}/status`
- **THEN** the system returns `403 Forbidden`
