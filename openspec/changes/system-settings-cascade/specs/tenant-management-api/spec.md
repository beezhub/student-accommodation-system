## ADDED Requirements

### Requirement: Platform admin manages platform-level settings
The system SHALL expose `GET /api/platform/system-settings` and `PUT /api/platform/system-settings/{key}` for managing settings stored under `tenant_id = 0` (the PLATFORM tenant). Both endpoints SHALL be restricted to `PLATFORM_ADMIN` only.

#### Scenario: Platform admin reads platform settings
- **WHEN** a `PLATFORM_ADMIN` sends `GET /api/platform/system-settings`
- **THEN** the system returns `200 OK` with the map of all platform-level (`tenant_id = 0`) key-value pairs

#### Scenario: Non-platform-admin is rejected from platform settings
- **WHEN** a user with only `ADMIN` role sends `GET /api/platform/system-settings`
- **THEN** the system returns `403 Forbidden`

#### Scenario: Platform admin updates a platform setting
- **WHEN** a `PLATFORM_ADMIN` sends `PUT /api/platform/system-settings/auto_allocation_enabled` with body `{"value": "true"}`
- **THEN** the system updates (or creates) the row with `tenant_id = 0` and returns `200 OK`

#### Scenario: Non-platform-admin cannot update platform settings
- **WHEN** a user with only `ADMIN` role sends `PUT /api/platform/system-settings/{key}`
- **THEN** the system returns `403 Forbidden`
