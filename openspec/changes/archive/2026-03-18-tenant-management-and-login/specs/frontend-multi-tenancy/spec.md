## MODIFIED Requirements

### Requirement: Tenant-Aware User Creation
The user management UI SHALL allow specifying the tenant when a Platform Admin creates a new user. When creating a user with the `BUILDING_OWNER` **or `ADMIN`** role, the UI SHALL enforce that the user is scoped to a single tenant — neither a building owner nor a tenant admin SHALL be assigned to `tenantId = 0` (the platform tenant). The tenant options SHALL be loaded from `GET /api/admin/tenants`.

#### Scenario: Platform Admin creates a new building owner
- **WHEN** a Platform Admin creates a new user with the `BUILDING_OWNER` role
- **THEN** the UI presents a required field to select which tenant the user belongs to, the platform tenant (id=0) is excluded from the selectable options, and the options are loaded from `GET /api/admin/tenants`

#### Scenario: Platform Admin creates a new tenant admin
- **WHEN** a Platform Admin creates a new user with the `ADMIN` role
- **THEN** the UI presents a required field to select which tenant the user belongs to, the platform tenant (id=0) is excluded from the selectable options, and the options are loaded from `GET /api/admin/tenants`

#### Scenario: Building owner assigned to platform tenant is rejected
- **WHEN** a Platform Admin attempts to create a `BUILDING_OWNER` user with tenantId = 0
- **THEN** the system returns `400 Bad Request` with message "Building owners must be scoped to a specific institution tenant"

#### Scenario: Tenant admin assigned to platform tenant is rejected
- **WHEN** a Platform Admin attempts to create an `ADMIN` user with tenantId = 0
- **THEN** the system returns `400 Bad Request` with message "Tenant admins must be scoped to a specific institution tenant"
