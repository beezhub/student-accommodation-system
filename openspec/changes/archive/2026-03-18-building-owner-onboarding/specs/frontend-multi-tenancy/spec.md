## MODIFIED Requirements

### Requirement: Tenant-Aware User Creation
The user management UI SHALL allow specifying the tenant when a Platform Admin creates a new user. When creating a user with the `BUILDING_OWNER` role, the UI SHALL also enforce that the owner is scoped to a single tenant — a building owner SHALL NOT be assigned to `tenantId = 0` (the platform tenant).

#### Scenario: Platform Admin creates a new building owner
- **WHEN** a Platform Admin creates a new user with the `BUILDING_OWNER` role
- **THEN** the UI presents a required field to select which tenant the user belongs to, and the platform tenant (id=0) is excluded from the selectable options

#### Scenario: Building owner assigned to platform tenant is rejected
- **WHEN** a Platform Admin attempts to create a `BUILDING_OWNER` user with tenantId = 0
- **THEN** the system returns `400 Bad Request` with message "Building owners must be scoped to a specific institution tenant"
