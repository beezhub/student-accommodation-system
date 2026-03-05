## ADDED Requirements

### Requirement: Admin pages use EmptyStateComponent
All admin page components with empty-data states SHALL use `<app-empty-state>` instead of inline `.no-data` div blocks.

#### Scenario: No-data state renders consistently
- **WHEN** an admin page has zero items to display
- **THEN** the `EmptyStateComponent` SHALL be rendered with the appropriate icon, title, and description inputs

#### Scenario: EmptyStateComponent replaces all inline no-data blocks
- **WHEN** the admin section is audited
- **THEN** no standalone `.no-data` div blocks SHALL remain in page-level components (application-review, building-management, room-management, room-allocation, document-verification, activity-logs, waiting-list-dashboard, academic-year-management, accommodation-type-management, document-type-management, institution-management)
