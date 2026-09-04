## ADDED Requirements

### Requirement: Application tenant is derived from building preference at submission
When a student submits an application, the system SHALL assign a non-zero `tenant_id` to the application, derived from the `tenant_id` of the student's first (highest-priority) preferred building. The `tenant_id` SHALL be explicitly set before the entity is persisted, ensuring the Hibernate tenant filter exposes the application to the correct TENANT_ADMIN.

#### Scenario: Single building preference — tenant_id is set
- **WHEN** a student submits an application with one or more preferred buildings that all belong to the same tenant (e.g., `tenant_id = 5`)
- **THEN** the saved `application.tenant_id` equals `5`
- **AND** a TENANT_ADMIN with `tenant_id = 5` can retrieve the application via `GET /api/admin/applications`

#### Scenario: Multiple building preferences from the same tenant
- **WHEN** a student lists multiple preferred buildings that all share the same `tenant_id`
- **THEN** the application's `tenant_id` is set to that shared `tenant_id`
- **AND** the application is saved successfully

### Requirement: Cross-tenant building preferences are rejected at submission
The system SHALL reject an application submission where the preferred buildings span more than one distinct `tenant_id`, returning `400 Bad Request`.

#### Scenario: Building preferences span two tenants
- **WHEN** a student submits an application whose preferred building list contains buildings belonging to two different tenants (e.g., `tenant_id = 5` and `tenant_id = 7`)
- **THEN** the system returns `400 Bad Request` with an error message indicating that all preferred buildings must belong to the same institution

#### Scenario: Valid single-tenant preferences pass validation
- **WHEN** all preferred buildings in the request belong to the same tenant
- **THEN** the validation passes and the application proceeds to be saved

### Requirement: Application submission requires at least one building preference
The system SHALL reject an application that provides no building preferences (neither `preferredBuildingId` nor any `preferredBuildingIds`), returning `400 Bad Request`. A building preference is required to identify which property company (tenant) the student is applying to.

#### Scenario: No building preference provided
- **WHEN** a student submits an application with `preferredBuildingId = null` and an empty or absent `preferredBuildingIds` list
- **THEN** the system returns `400 Bad Request` with an error message requiring at least one building preference

#### Scenario: At least one preference provided — submission proceeds
- **WHEN** a student submits an application with at least one valid building preference
- **THEN** the submission proceeds to the tenant assignment and application window validation steps

### Requirement: Duplicate application constraint is per-student per-tenant
The system SHALL allow a student to have at most one `PENDING` application per tenant at a time. A student MAY have simultaneous `PENDING` applications to different tenants (property companies).

#### Scenario: Student submits a second application to the same tenant
- **WHEN** a student already has a `PENDING` application for tenant X and submits another application whose building preferences also resolve to tenant X
- **THEN** the system returns `409 Conflict` with an error message indicating a pending application for this property company already exists

#### Scenario: Student submits applications to two different tenants
- **WHEN** a student has a `PENDING` application for tenant X and submits a new application whose building preferences resolve to tenant Y (a different property company)
- **THEN** the system accepts the second application and returns `201 Created`

### Requirement: Existing applications with tenant_id = 0 are backfilled by migration
The system SHALL include a Flyway migration that updates the `tenant_id` of existing application records where `tenant_id = 0` and a building preference record exists, setting it to the `tenant_id` of the first building preference.

#### Scenario: Application has a resolvable building preference
- **WHEN** the backfill migration runs and an application row has `tenant_id = 0` and at least one row in `application_building_preference` joined to a building with a non-zero `tenant_id`
- **THEN** the migration sets the application's `tenant_id` to the `tenant_id` of the building from the lowest `preference_order` row

#### Scenario: Application has no building preference — remains platform-level
- **WHEN** the backfill migration runs and an application row has `tenant_id = 0` and no associated building preference
- **THEN** the migration leaves `tenant_id = 0` unchanged (the application remains visible only to PLATFORM_ADMIN)
