## ADDED Requirements

### Requirement: Frontend Tenant Context Awareness
The frontend SHALL extract and maintain awareness of the current user's `tenantId` from the JWT token and provide it to the application state.

#### Scenario: User logs in and tenant is extracted
- **WHEN** a user logs in successfully
- **THEN** the auth service decodes the JWT, extracts the `tenantId`, and stores it in the application state

### Requirement: Super Admin Multi-Tenant Support
The frontend SHALL provide mechanisms for Platform Admins (sentinel tenant ID `0L`) to interact with data across different tenants.

#### Scenario: Platform Admin views tenant-specific data
- **WHEN** a Platform Admin is viewing global dashboards
- **THEN** the system provides a way to explicitly select and filter by a specific tenant

### Requirement: Tenant-Aware User Creation
The user management UI SHALL allow specifying the tenant when a Platform Admin creates a new user.

#### Scenario: Platform Admin creates a new building owner
- **WHEN** a Platform Admin creates a new user with the `BUILDING_OWNER` role
- **THEN** the UI presents a required field to select which tenant the user belongs to
