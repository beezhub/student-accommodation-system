## MODIFIED Requirements

### Requirement: Super Admin Multi-Tenant Support
The frontend SHALL provide mechanisms for Platform Admins (sentinel tenant ID `0L`) to interact with data across different tenants. Tenant data SHALL be loaded from `GET /api/admin/tenants` via `TenantService`.

#### Scenario: Platform Admin views tenant-specific data
- **WHEN** a Platform Admin is viewing global dashboards
- **THEN** the system provides a way to explicitly select and filter by a specific tenant

#### Scenario: Tenant list is loaded from backend
- **WHEN** the Platform Admin accesses a UI that requires a tenant selector
- **THEN** `TenantService` calls `GET /api/admin/tenants` and the response populates the dropdown

### Requirement: Tenant-Aware User Creation
The user management UI SHALL allow specifying the tenant when a Platform Admin creates a new user. When creating a user with the `BUILDING_OWNER` role, the UI SHALL also enforce that the owner is scoped to a single tenant — a building owner SHALL NOT be assigned to `tenantId = 0` (the platform tenant). The tenant options SHALL be loaded from `GET /api/admin/tenants`.

#### Scenario: Platform Admin creates a new building owner
- **WHEN** a Platform Admin creates a new user with the `BUILDING_OWNER` role
- **THEN** the UI presents a required field to select which tenant the user belongs to, the platform tenant (id=0) is excluded from the selectable options, and the options are loaded from `GET /api/admin/tenants`

#### Scenario: Building owner assigned to platform tenant is rejected
- **WHEN** a Platform Admin attempts to create a `BUILDING_OWNER` user with tenantId = 0
- **THEN** the system returns `400 Bad Request` with message "Building owners must be scoped to a specific institution tenant"
