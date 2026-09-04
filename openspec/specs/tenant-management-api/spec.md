## ADDED Requirements

### Requirement: List All Tenants
The system SHALL expose `GET /api/admin/tenants` returning a list of all tenants, accessible only to `PLATFORM_ADMIN`.

#### Scenario: Platform admin retrieves tenant list
- **WHEN** a `PLATFORM_ADMIN` sends `GET /api/admin/tenants`
- **THEN** the system returns `200 OK` with a JSON array of tenant objects (id, name, and other tenant fields)

#### Scenario: Non-platform-admin is rejected
- **WHEN** a user without `PLATFORM_ADMIN` role sends `GET /api/admin/tenants`
- **THEN** the system returns `403 Forbidden`

### Requirement: Get Tenant by ID
The system SHALL expose `GET /api/admin/tenants/{id}` returning a single tenant by ID, accessible only to `PLATFORM_ADMIN`.

#### Scenario: Platform admin retrieves a specific tenant
- **WHEN** a `PLATFORM_ADMIN` sends `GET /api/admin/tenants/{id}` with a valid tenant ID
- **THEN** the system returns `200 OK` with the tenant object

#### Scenario: Tenant not found
- **WHEN** a `PLATFORM_ADMIN` sends `GET /api/admin/tenants/{id}` with an ID that does not exist
- **THEN** the system returns `404 Not Found`

#### Scenario: Non-platform-admin is rejected
- **WHEN** a user without `PLATFORM_ADMIN` role sends `GET /api/admin/tenants/{id}`
- **THEN** the system returns `403 Forbidden`

### Requirement: List tenant admin users for a tenant
The system SHALL expose `GET /api/admin/tenants/{tenantId}/admins` returning a list of all `TENANT_ADMIN` users belonging to the specified tenant, accessible only to `PLATFORM_ADMIN`.

#### Scenario: Platform admin lists admins for a tenant
- **WHEN** a `PLATFORM_ADMIN` sends `GET /api/admin/tenants/{tenantId}/admins` with a valid `tenantId`
- **THEN** the system returns `200 OK` with a JSON array of tenant admin objects (id, email, firstName, lastName, createdAt, mustChangePassword, accountLockedUntil)

#### Scenario: Non-platform-admin is rejected
- **WHEN** a user without `PLATFORM_ADMIN` role sends `GET /api/admin/tenants/{tenantId}/admins`
- **THEN** the system returns `403 Forbidden`

### Requirement: Create a tenant admin user for a tenant
The system SHALL expose `POST /api/admin/tenants/{tenantId}/admins` allowing a `PLATFORM_ADMIN` to provision a new `TENANT_ADMIN` user for the specified tenant.

#### Scenario: Platform admin creates a tenant admin
- **WHEN** a `PLATFORM_ADMIN` sends `POST /api/admin/tenants/{tenantId}/admins` with a valid request body
- **THEN** the system returns `201 Created` with the new user's details

#### Scenario: Non-platform-admin is rejected
- **WHEN** a user without `PLATFORM_ADMIN` role sends `POST /api/admin/tenants/{tenantId}/admins`
- **THEN** the system returns `403 Forbidden`

### Requirement: Deactivate a tenant admin user
The system SHALL expose `DELETE /api/admin/tenants/{tenantId}/admins/{userId}` allowing a `PLATFORM_ADMIN` to lock the account of a `TENANT_ADMIN` user.

#### Scenario: Platform admin deactivates a tenant admin
- **WHEN** a `PLATFORM_ADMIN` sends `DELETE /api/admin/tenants/{tenantId}/admins/{userId}` for a valid `TENANT_ADMIN` of that tenant
- **THEN** the system locks the user's account and returns `200 OK`

#### Scenario: Non-platform-admin is rejected
- **WHEN** a user without `PLATFORM_ADMIN` role sends `DELETE /api/admin/tenants/{tenantId}/admins/{userId}`
- **THEN** the system returns `403 Forbidden`
