## ADDED Requirements

### Requirement: TENANT_ADMIN can access scoped admin dashboard
The system SHALL grant a `TENANT_ADMIN` user access to the admin dashboard, with all data scoped exclusively to their own `tenant_id`. The `TENANT_ADMIN` SHALL NOT be able to view or modify data belonging to another tenant.

#### Scenario: Successful dashboard load
- **WHEN** a `TENANT_ADMIN` user successfully authenticates and navigates to `/admin`
- **THEN** the system displays the admin dashboard showing summary statistics (applications, buildings, rooms, allocations) scoped to their tenant only

#### Scenario: Cross-tenant data is inaccessible
- **WHEN** a `TENANT_ADMIN` calls any admin API endpoint (e.g., `GET /api/admin/applications`)
- **THEN** the system returns only records whose `tenant_id` matches the caller's `tenant_id` and never returns records from another tenant

### Requirement: TENANT_ADMIN can manage buildings and rooms for their tenant
The system SHALL allow a `TENANT_ADMIN` to create, read, update, and delete buildings and rooms that belong to their tenant.

#### Scenario: List buildings
- **WHEN** a `TENANT_ADMIN` sends `GET /api/admin/buildings`
- **THEN** the system returns only buildings belonging to their `tenant_id`

#### Scenario: Create building scoped to tenant
- **WHEN** a `TENANT_ADMIN` sends `POST /api/admin/buildings` with valid building data
- **THEN** the system creates the building with `tenant_id` set to the caller's tenant and returns `201 Created`

#### Scenario: Cannot access another tenant's building
- **WHEN** a `TENANT_ADMIN` sends `GET /api/admin/buildings/{id}` where `{id}` belongs to a different tenant
- **THEN** the system returns `404 Not Found` (not 403, to avoid leaking existence of the resource)

### Requirement: TENANT_ADMIN can manage applications for their tenant
The system SHALL allow a `TENANT_ADMIN` to review, approve, and manage student applications scoped to their tenant.

#### Scenario: List applications
- **WHEN** a `TENANT_ADMIN` sends `GET /api/admin/applications`
- **THEN** the system returns only applications where `tenant_id` matches the caller's tenant

#### Scenario: Update application status
- **WHEN** a `TENANT_ADMIN` sends a status update request for an application belonging to their tenant
- **THEN** the system processes the update and returns `200 OK`

#### Scenario: Cannot modify another tenant's application
- **WHEN** a `TENANT_ADMIN` sends a status update request for an application belonging to a different tenant
- **THEN** the system returns `404 Not Found`

### Requirement: TENANT_ADMIN can manage allocations for their tenant
The system SHALL allow a `TENANT_ADMIN` to view, approve staged allocations, and manage room allocations scoped to their tenant.

#### Scenario: List allocations
- **WHEN** a `TENANT_ADMIN` sends `GET /api/admin/room-allocations`
- **THEN** the system returns only allocations whose `tenant_id` matches the caller's tenant

#### Scenario: Approve staged allocation
- **WHEN** a `TENANT_ADMIN` approves a staged allocation for their tenant
- **THEN** the system finalises the allocation and returns `200 OK`

### Requirement: TENANT_ADMIN can manage documents for their tenant
The system SHALL allow a `TENANT_ADMIN` to view, verify, and request resubmission of documents scoped to their tenant.

#### Scenario: List documents
- **WHEN** a `TENANT_ADMIN` sends `GET /api/admin/documents`
- **THEN** the system returns only documents belonging to students of their tenant

### Requirement: PLATFORM_ADMIN-only sections are inaccessible to TENANT_ADMIN
The system SHALL prevent a `TENANT_ADMIN` from accessing platform-level administrative features.

#### Scenario: Tenant management endpoint forbidden
- **WHEN** a `TENANT_ADMIN` sends `GET /api/admin/tenants`
- **THEN** the system returns `403 Forbidden`

#### Scenario: System settings endpoint forbidden
- **WHEN** a `TENANT_ADMIN` sends `GET /api/admin/settings`
- **THEN** the system returns `403 Forbidden`

#### Scenario: Feature flags endpoint forbidden
- **WHEN** a `TENANT_ADMIN` sends any request to `/api/admin/features`
- **THEN** the system returns `403 Forbidden`

#### Scenario: Activity logs endpoint forbidden
- **WHEN** a `TENANT_ADMIN` sends `GET /api/admin/activity-logs`
- **THEN** the system returns `403 Forbidden`

#### Scenario: User management endpoint forbidden
- **WHEN** a `TENANT_ADMIN` sends any request to `/api/admin/tenants/{id}/admins`
- **THEN** the system returns `403 Forbidden`

### Requirement: Frontend hides PLATFORM-only sections for TENANT_ADMIN
The Angular admin dashboard SHALL hide sidebar navigation items for platform-only sections when the authenticated user has the `TENANT_ADMIN` role, so the interface is uncluttered and does not expose inaccessible sections.

#### Scenario: Sidebar rendered for TENANT_ADMIN
- **WHEN** a `TENANT_ADMIN` user loads the admin dashboard
- **THEN** the sidebar SHALL display: Dashboard, Buildings, Rooms, Applications, Allocations, Documents, Notifications, Students, Allocation Rules
- **AND** the sidebar SHALL NOT display: Tenant Management, System Settings, Feature Flags, Activity Logs, User Management

#### Scenario: Direct URL navigation blocked
- **WHEN** a `TENANT_ADMIN` user navigates directly to a platform-only route (e.g., `/admin/tenants`)
- **THEN** the Angular route guard redirects them to `/admin` (dashboard)

### Requirement: TENANT_ADMIN forced to change password on first login
The system SHALL redirect a `TENANT_ADMIN` user to the change-password page on their first login if `must_change_password` is `true`.

#### Scenario: First login redirect
- **WHEN** a `TENANT_ADMIN` authenticates and the JWT payload contains `mustChangePassword: true`
- **THEN** the frontend redirects the user to the change-password page before allowing any other navigation

#### Scenario: Subsequent logins after password change
- **WHEN** a `TENANT_ADMIN` has changed their password and `must_change_password` is `false`
- **THEN** the frontend allows normal navigation to the admin dashboard
