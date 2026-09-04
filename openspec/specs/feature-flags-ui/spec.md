## ADDED Requirements

### Requirement: Feature Flags UI
The system SHALL provide an interface for users with the PLATFORM_ADMIN role to manage FF4J feature flags.

#### Scenario: Admin views feature flags
- **WHEN** an admin navigates to the feature flags management page
- **THEN** the system fetches and displays all available feature flags from `/api/admin/features`

#### Scenario: Admin toggles feature flag
- **WHEN** an admin toggles a feature flag
- **THEN** the system calls the `/api/admin/features/{featureId}/enable` or `disable` endpoint to update the status

#### Scenario: Unauthorized access prevented
- **WHEN** a non-admin user attempts to access the feature flags UI
- **THEN** the system denies access and redirects the user
