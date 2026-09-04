### Requirement: Grouped navigation structure
The admin sidebar SHALL organise navigation items into named groups with overline labels. The groups SHALL be: Overview, Applications, Buildings & Rooms, Configuration, Administration.

#### Scenario: Groups are rendered with labels
- **WHEN** the admin sidebar is displayed
- **THEN** each navigation group SHALL show an overline label above its items in `$slate-400` colour using `.type-overline` typography

#### Scenario: Items are placed in correct groups
- **WHEN** the sidebar renders
- **THEN** Dashboard appears in Overview; Applications, Allocations, Staged Allocations, Waiting List appear in Applications; Buildings, Rooms appear in Buildings & Rooms; Allocation Rules, System Settings appear in Configuration; Users, Documents, Activity Logs appear in Administration

### Requirement: Sidebar width
The admin sidebar SHALL be 280px wide on desktop.

#### Scenario: Sidebar renders at correct width
- **WHEN** the admin layout is displayed on a viewport wider than the tablet breakpoint
- **THEN** the sidebar SHALL measure 280px wide

### Requirement: Active navigation indicator
The active sidebar item SHALL be visually distinguished by a 3px left-border in `$accent-purple-dark` in addition to the `$slate-100` background.

#### Scenario: Active item has border indicator
- **WHEN** the user navigates to an admin route
- **THEN** the matching sidebar item SHALL show a 3px solid `$accent-purple-dark` left border with `$slate-100` background and `$accent-purple-dark` icon colour

#### Scenario: Inactive items have no left border
- **WHEN** a sidebar item is not the active route
- **THEN** it SHALL have no left border and a transparent background

### Requirement: Modern Angular control flow
The sidebar template SHALL use `@for`/`@if` Angular 17+ block syntax instead of `*ngFor`/`*ngIf` structural directives.

#### Scenario: Template uses block syntax
- **WHEN** the sidebar template is compiled
- **THEN** no `*ngFor` or `*ngIf` directives SHALL be present; `@for` blocks SHALL include a `track` expression

### Requirement: Branding header
The top of the sidebar SHALL display a branding area with the application name or logo.

#### Scenario: Branding is visible
- **WHEN** the admin sidebar is rendered
- **THEN** a branding header with the system name is displayed above the navigation groups
