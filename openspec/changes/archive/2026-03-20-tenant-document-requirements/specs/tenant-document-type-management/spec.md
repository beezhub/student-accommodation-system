## ADDED Requirements

### Requirement: Each document type is scoped to a tenant, with platform types as inheritable defaults
The `document_type` table SHALL carry a `tenant_id` column (FK to `tenant.id`). Platform-level document types SHALL be stored at `tenant_id = 0`. Tenant-specific document types SHALL be stored at the tenant's own `tenant_id`. The combination `(tenant_id, type_name)` SHALL be enforced as a unique constraint at the database level.

#### Scenario: Platform admin creates a platform-level document type
- **WHEN** a `PLATFORM_ADMIN` creates a document type via `POST /api/platform/document-types`
- **THEN** the row is saved with `tenant_id = 0`
- **AND** the type becomes visible to all tenants unless shadowed

#### Scenario: Tenant admin creates a tenant-specific document type
- **WHEN** a `TENANT_ADMIN` creates a document type via `POST /api/admin/document-types`
- **THEN** the row is saved with `tenant_id = TenantContext.getTenantId()`
- **AND** the type is visible only within that tenant

#### Scenario: Database rejects duplicate type name within same tenant
- **WHEN** a second row is inserted with the same `(tenant_id, type_name)` pair
- **THEN** the database raises a unique constraint violation

#### Scenario: Same type name is allowed across different tenants
- **WHEN** Tenant A and Tenant B each create a document type with the same name
- **THEN** both rows are stored successfully with their respective `tenant_id` values

---

### Requirement: Tenant admins see the merged view of platform types and their own types
The `GET /api/admin/document-types` and `GET /api/document-type/required` endpoints SHALL return a merged list combining platform document types (`tenant_id = 0`) and the current tenant's document types. When a tenant-scoped type has the same `type_name` as a platform type, the tenant's row SHALL take precedence (shadow semantics). The merged list SHALL be deduplicated by `type_name` before returning.

#### Scenario: Tenant with no custom types sees all platform types
- **WHEN** a tenant has no document types of their own and calls `GET /api/admin/document-types`
- **THEN** the response contains all platform-level document types
- **AND** no tenant-specific rows are included

#### Scenario: Tenant's custom type supplements platform types
- **WHEN** a tenant has created a document type that does not exist at the platform level
- **THEN** `GET /api/admin/document-types` returns both the platform types and the tenant-specific type

#### Scenario: Tenant type shadows a platform type with the same name
- **WHEN** a tenant has a document type with the same `type_name` as a platform type
- **THEN** the tenant's row is returned in place of the platform row
- **AND** the platform row is NOT included in the merged list

---

### Requirement: Tenant admins can shadow a platform document type's required flag
A `TENANT_ADMIN` SHALL be able to create a tenant-scoped document type with the same `type_name` as a platform type but with a different `isRequired` value. This shadow row overrides the platform type's required flag for that tenant's students without modifying the platform row.

#### Scenario: Tenant creates a shadow type to make a non-required platform type required
- **WHEN** a `TENANT_ADMIN` creates a document type with `typeName = "Proof of Income"` and `isRequired = true`, and the platform has `"Proof of Income"` with `isRequired = false`
- **THEN** students of that tenant are required to upload "Proof of Income" before submitting an application
- **AND** students of other tenants are NOT required to upload it

#### Scenario: Tenant creates a shadow type to make a required platform type optional
- **WHEN** a `TENANT_ADMIN` creates a document type with `typeName = "National ID"` and `isRequired = false`, and the platform has `"National ID"` with `isRequired = true`
- **THEN** students of that tenant are NOT required to upload "National ID" during application submission
- **AND** students of other tenants that have not overridden this type remain required to upload it

---

### Requirement: Tenant admins cannot mutate platform-level document types
A `TENANT_ADMIN` SHALL NOT be able to update or delete document types where `tenant_id = 0`. Attempts to do so SHALL return `403 Forbidden`.

#### Scenario: Tenant admin attempts to update a platform document type
- **WHEN** a `TENANT_ADMIN` calls `PUT /api/admin/document-types/{id}` for a document type with `tenant_id = 0`
- **THEN** the system returns `403 Forbidden`
- **AND** the platform row is unchanged

#### Scenario: Tenant admin attempts to delete a platform document type
- **WHEN** a `TENANT_ADMIN` calls `DELETE /api/admin/document-types/{id}` for a document type with `tenant_id = 0`
- **THEN** the system returns `403 Forbidden`
- **AND** the platform row is unchanged

---

### Requirement: Platform admins manage platform-level document types via a dedicated route
`PLATFORM_ADMIN` SHALL manage document types at `tenant_id = 0` via `POST/PUT/DELETE /api/platform/document-types`. These endpoints SHALL set `tenant_id = 0` regardless of the platform admin's current tenant context.

#### Scenario: Platform admin creates a platform default document type
- **WHEN** a `PLATFORM_ADMIN` calls `POST /api/platform/document-types` with valid body
- **THEN** the document type is created with `tenant_id = 0`
- **AND** `GET /api/admin/document-types` for any tenant without a shadow returns this type

#### Scenario: Platform admin deletes a platform document type
- **WHEN** a `PLATFORM_ADMIN` calls `DELETE /api/platform/document-types/{id}` for a platform type
- **THEN** the row is removed from `document_type`
- **AND** tenant shadow rows with the same `type_name` are NOT automatically deleted
