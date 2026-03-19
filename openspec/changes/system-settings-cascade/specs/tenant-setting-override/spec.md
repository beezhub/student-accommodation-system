## ADDED Requirements

### Requirement: Cascade setting lookup
The system SHALL resolve a setting value by first checking the current tenant's own row, then falling back to the PLATFORM tenant (`tenant_id = 0`) if no tenant-specific row exists. If neither row exists the system SHALL return `null`.

#### Scenario: Tenant has its own override
- **WHEN** `getSetting("auto_allocation_enabled")` is called with tenant ID 5 active
- **AND** a row exists in `system_settings` with `tenant_id = 5` and `setting_key = "auto_allocation_enabled"`
- **THEN** the tenant-specific value is returned

#### Scenario: Tenant has no override — falls back to platform
- **WHEN** `getSetting("auto_allocation_enabled")` is called with tenant ID 5 active
- **AND** no row exists for `tenant_id = 5` but a row exists for `tenant_id = 0`
- **THEN** the platform-level value is returned

#### Scenario: No row at either level
- **WHEN** `getSetting("unknown_key")` is called
- **AND** no row exists for the current tenant or for `tenant_id = 0`
- **THEN** `null` is returned

### Requirement: Merged effective settings view
The system SHALL provide `getAllSettings()` returning a merged `Map<String, String>` where platform defaults are the base and any tenant-specific rows override the corresponding keys.

#### Scenario: Tenant has partial overrides
- **WHEN** `getAllSettings()` is called for tenant ID 5
- **AND** the platform defines keys A, B, C and tenant 5 overrides only key B
- **THEN** the returned map contains A=platform-value, B=tenant-override-value, C=platform-value

#### Scenario: No tenant overrides
- **WHEN** `getAllSettings()` is called for a tenant with no overrides
- **THEN** the returned map equals the full set of platform-level settings

### Requirement: ADMIN can read effective settings
The system SHALL expose `GET /api/admin/system-settings` returning the cascade-resolved effective settings for the current tenant. This endpoint SHALL be accessible to both `ADMIN` and `PLATFORM_ADMIN` roles.

#### Scenario: Tenant admin reads effective settings
- **WHEN** an `ADMIN` user sends `GET /api/admin/system-settings`
- **THEN** the system returns `200 OK` with the merged map of effective key-value pairs for that admin's tenant

#### Scenario: Unauthenticated request is rejected
- **WHEN** an unauthenticated request is sent to `GET /api/admin/system-settings`
- **THEN** the system returns `401 Unauthorized`

### Requirement: ADMIN can write tenant-level overrides
The system SHALL expose `PUT /api/admin/system-settings/{key}` allowing an `ADMIN` user to create or update a setting override scoped to their own tenant. `PLATFORM_ADMIN` SHALL also be permitted to call this endpoint (it targets the current tenant context, not the platform row).

#### Scenario: Tenant admin creates a new override
- **WHEN** an `ADMIN` sends `PUT /api/admin/system-settings/offer_expiry_days` with body `{"value": "14"}`
- **AND** no row currently exists for that tenant and key
- **THEN** the system inserts a new row with the tenant's `tenant_id` and returns `200 OK`

#### Scenario: Tenant admin updates an existing override
- **WHEN** an `ADMIN` sends `PUT /api/admin/system-settings/offer_expiry_days` with body `{"value": "30"}`
- **AND** a row already exists for that tenant and key
- **THEN** the system updates the row's value and returns `200 OK`

#### Scenario: Unauthenticated request is rejected
- **WHEN** an unauthenticated request is sent to `PUT /api/admin/system-settings/{key}`
- **THEN** the system returns `401 Unauthorized`

### Requirement: ADMIN can delete a tenant-level override
The system SHALL expose `DELETE /api/admin/system-settings/{key}` allowing an `ADMIN` user to remove their tenant's override for a key, reverting to the platform default.

#### Scenario: Tenant admin deletes an override
- **WHEN** an `ADMIN` sends `DELETE /api/admin/system-settings/offer_expiry_days`
- **AND** a row exists for that tenant and key
- **THEN** the system deletes the row and returns `204 No Content`
- **AND** subsequent reads of that key return the platform-level value

#### Scenario: Delete on non-existent override
- **WHEN** an `ADMIN` sends `DELETE /api/admin/system-settings/offer_expiry_days`
- **AND** no override row exists for that tenant and key
- **THEN** the system returns `404 Not Found`

### Requirement: Platform settings seed uses PLATFORM tenant
The system SHALL seed all default settings (including `auto_allocation_enabled`) under `tenant_id = 0` (the PLATFORM tenant), not under any named tenant row.

#### Scenario: Platform setting is accessible to all tenants via cascade
- **WHEN** `auto_allocation_enabled` is seeded under `tenant_id = 0`
- **AND** any tenant without an override calls `getSetting("auto_allocation_enabled")`
- **THEN** the platform value `false` is returned
