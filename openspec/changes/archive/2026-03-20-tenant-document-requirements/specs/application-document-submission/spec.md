## MODIFIED Requirements

### Requirement: Application submission requires all required document types to be present in the student's pack
The `POST /api/application` endpoint SHALL, before saving the application, resolve the required document types from the **application's target tenant** — derived from the selected preferred buildings (`applicationTenantId = buildings[0].tenantId`), NOT from the student's `TenantContext` (which is always `0`). The required type set is the effective merged view: platform document types (`tenant_id = 0`) combined with the target tenant's own types, with tenant rows shadowing platform rows by `type_name`. Presence is determined by **name-based matching**: the system compares the set of `type_name` values already uploaded by the student against the set of required `type_name` values from the effective view. If any required name is absent, the system SHALL return `400 Bad Request` with a body listing the missing type names. A document in any status (PENDING, APPROVED, REJECTED) satisfies the presence check.

#### Scenario: Student has all required documents uploaded (tenant uses platform defaults only)
- **WHEN** a student submits an application and their document pack contains at least one document for each `isRequired = true` document type in the merged view (using only platform defaults)
- **THEN** the application submission proceeds normally and returns `201 Created`

#### Scenario: Student is missing one required document type from the tenant's effective list
- **WHEN** a student submits an application and their document pack is missing a document for at least one `isRequired = true` document type in the tenant's merged view
- **THEN** the system returns `400 Bad Request`
- **AND** the response body includes the names of the missing required document types

#### Scenario: Student's document (platform type ID) satisfies a tenant shadow of the same name
- **WHEN** a student has uploaded a document linked to platform type `"National ID"` (id=1)
- **AND** the target tenant has a shadow type also named `"National ID"` (id=5) with `isRequired = true`
- **THEN** the name-based check finds `"National ID"` in both the uploaded set and the required set
- **AND** the submission proceeds normally (`201 Created`) — the student is not required to re-upload

#### Scenario: Tenant has shadowed a required platform type to be non-required
- **WHEN** a tenant has a shadow document type with `isRequired = false` for a type that is `isRequired = true` at the platform level
- **AND** a student of that tenant submits an application without that document
- **THEN** the submission proceeds normally (`201 Created`) because the tenant's shadow takes precedence

#### Scenario: Tenant has added a custom required type not present at platform level
- **WHEN** a tenant has a tenant-specific document type with `isRequired = true` that does not exist at the platform level
- **AND** a student of that tenant submits an application without that document
- **THEN** the system returns `400 Bad Request` listing the missing tenant-specific required type

#### Scenario: No required document types exist in the tenant's effective view
- **WHEN** the tenant's merged document type list contains no types with `isRequired = true`
- **THEN** the document completeness check is a no-op and submission proceeds normally
