## ADDED Requirements

### Requirement: Unknown routes display a 404 Not Found page
The Angular application SHALL route all unmatched paths (wildcard route `**`) to a dedicated `NotFoundComponent` instead of silently redirecting to the home page. The component SHALL be a simple standalone component with no data fetching, no route guards, and no service dependencies.

#### Scenario: Unmatched route shows 404 page
- **WHEN** a user navigates to a URL path that does not match any defined route (e.g., `/nonexistent-page`)
- **THEN** the `NotFoundComponent` SHALL be rendered
- **AND** the browser URL SHALL remain at the requested path (no redirect)

#### Scenario: 404 page displays navigation options
- **WHEN** the `NotFoundComponent` is rendered
- **THEN** the page SHALL display a clear "Page Not Found" heading
- **AND** the page SHALL provide navigation links to: Home (`/`), Dashboard (`/dashboard`), and Contact Support (link to support email or page)

#### Scenario: 404 page is accessible without authentication
- **WHEN** an unauthenticated user navigates to an unmatched route
- **THEN** the `NotFoundComponent` SHALL render without requiring login
- **AND** the Dashboard link SHALL navigate to `/auth/login` if the user is not authenticated
