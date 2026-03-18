## ADDED Requirements

### Requirement: Create Tenant API
The system SHALL expose `POST /api/admin/tenants` to create a new tenant. See `tenant-crud` spec for full requirements.

#### Scenario: Endpoint is available
- **WHEN** a `PLATFORM_ADMIN` sends `POST /api/admin/tenants` with `{ "name": "University of Cape Town" }`
- **THEN** the system returns `201 Created` with the created tenant object

### Requirement: Update Tenant API
The system SHALL expose `PUT /api/admin/tenants/{id}` to update a tenant's name and domain. See `tenant-crud` spec for full requirements.

#### Scenario: Endpoint is available
- **WHEN** a `PLATFORM_ADMIN` sends `PUT /api/admin/tenants/1` with updated fields
- **THEN** the system returns `200 OK` with the updated tenant object

### Requirement: Toggle Tenant Status API
The system SHALL expose `PATCH /api/admin/tenants/{id}/status` to toggle the `isActive` flag. See `tenant-crud` spec for full requirements.

#### Scenario: Endpoint is available
- **WHEN** a `PLATFORM_ADMIN` sends `PATCH /api/admin/tenants/1/status` with `{ "isActive": false }`
- **THEN** the system returns `200 OK` with the tenant showing `isActive: false`
