## Requirements

### Requirement: Feature Flags UI
The system SHALL provide an interface for users with the ADMIN role to manage FF4J feature flags.

#### Scenario: Admin views feature flags
- **WHEN** an admin navigates to `/admin/feature-flags`
- **THEN** the system fetches and displays all available feature flags from `/api/admin/features`

#### Scenario: Admin toggles feature flag
- **WHEN** an admin toggles a feature flag
- **THEN** the system calls `/api/admin/features/{featureId}/enable` or `/api/admin/features/{featureId}/disable`
- **AND** the UI reflects the new state immediately (optimistic update)
- **AND** reverts to the previous state if the API call fails

#### Scenario: Unauthorized access prevented
- **WHEN** a non-admin user attempts to access `/admin/feature-flags`
- **THEN** the AdminGuard denies access

#### Scenario: Empty state shown when no flags
- **WHEN** the API returns an empty feature flag list
- **THEN** the page displays an empty state message

#### Scenario: Feature flags accessible from System Settings
- **WHEN** an admin views the System Settings page
- **THEN** a card linking to `/admin/feature-flags` is visible

#### Scenario: Feature flags in sidebar navigation
- **WHEN** an admin is logged in
- **THEN** "Feature Flags" appears in the sidebar Configuration group
