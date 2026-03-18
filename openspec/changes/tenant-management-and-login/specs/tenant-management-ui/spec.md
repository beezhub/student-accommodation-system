## ADDED Requirements

### Requirement: Tenant Management Page
The system SHALL provide an Angular page at `/admin/tenants` that lists all tenants in a Material table with columns: Name, Domain, Status (Active/Inactive), Created At, and Actions (Edit, Toggle Status).

#### Scenario: Platform admin views tenant list
- **WHEN** a `PLATFORM_ADMIN` navigates to `/admin/tenants`
- **THEN** the system displays a table of all tenants loaded from `GET /api/admin/tenants`

#### Scenario: Non-platform-admin cannot access tenant management
- **WHEN** a user without `PLATFORM_ADMIN` role navigates to `/admin/tenants`
- **THEN** the system redirects to the admin dashboard or shows a 403 page

#### Scenario: Empty state displayed when no tenants
- **WHEN** there are no tenants in the system
- **THEN** the system displays the `EmptyStateComponent` with an appropriate message

### Requirement: Create Tenant Dialog
The system SHALL provide a Material dialog for creating a new tenant, triggered by a "New Tenant" button on the tenant management page. The dialog SHALL have fields for `name` (required) and `domain` (optional).

#### Scenario: Platform admin creates a tenant via dialog
- **WHEN** a `PLATFORM_ADMIN` fills in the dialog with a valid `name` and clicks Save
- **THEN** the system calls `POST /api/admin/tenants`, closes the dialog, refreshes the table, and shows a success snackbar

#### Scenario: Duplicate name shows error
- **WHEN** the backend returns `409 Conflict`
- **THEN** the dialog remains open and displays an inline error message

#### Scenario: Empty name prevents submission
- **WHEN** the `name` field is empty and the user clicks Save
- **THEN** the form shows a validation error and the Save button is disabled

### Requirement: Edit Tenant Dialog
The system SHALL allow a `PLATFORM_ADMIN` to edit a tenant's `name` and `domain` via the same dialog form (pre-populated). The Edit action SHALL be disabled for the platform tenant (id=0).

#### Scenario: Platform admin edits a tenant
- **WHEN** a `PLATFORM_ADMIN` clicks Edit on a non-platform tenant and submits valid changes
- **THEN** the system calls `PUT /api/admin/tenants/{id}`, refreshes the table, and shows a success snackbar

#### Scenario: Edit is disabled for platform tenant
- **WHEN** a `PLATFORM_ADMIN` views the tenant list
- **THEN** the Edit button for the platform tenant (id=0) is disabled

### Requirement: Toggle Tenant Status
The system SHALL allow a `PLATFORM_ADMIN` to activate or deactivate a tenant from the tenant management table. The toggle action SHALL be disabled for the platform tenant (id=0).

#### Scenario: Platform admin deactivates a tenant
- **WHEN** a `PLATFORM_ADMIN` clicks the deactivate action on an active tenant (id > 0)
- **THEN** the system calls `PATCH /api/admin/tenants/{id}/status`, updates the row in the table, and shows a success snackbar

#### Scenario: Toggle is disabled for platform tenant
- **WHEN** a `PLATFORM_ADMIN` views the tenant list
- **THEN** the toggle action for the platform tenant (id=0) is disabled

### Requirement: Tenant Sidebar Navigation Entry
The admin sidebar SHALL include a "Tenants" navigation item pointing to `/admin/tenants`, visible only to users with the `PLATFORM_ADMIN` role.

#### Scenario: Platform admin sees Tenants in sidebar
- **WHEN** a `PLATFORM_ADMIN` is logged in and views the admin sidebar
- **THEN** a "Tenants" navigation item is present and links to `/admin/tenants`

#### Scenario: Non-platform-admin does not see Tenants in sidebar
- **WHEN** a user without `PLATFORM_ADMIN` role views the admin sidebar
- **THEN** the "Tenants" navigation item is not displayed
