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
