## ADDED Requirements

### Requirement: Build output contains no Sass deprecation warnings
The Angular build SHALL produce zero `darken() is deprecated` warnings. All `darken($color, $amount)` calls must be replaced with the `sass:color` module equivalent.

#### Scenario: Development build runs clean
- **WHEN** `ng build --configuration development` is executed
- **THEN** no lines matching `darken() is deprecated` appear in the build output

### Requirement: No redundant mat-icon font-family overrides in component SCSS
Individual admin component SCSS files SHALL NOT contain a `mat-icon { font-family: 'Material Icons' !important }` block. The global stylesheet is the single source for this fix.

#### Scenario: Admin SCSS files are free of duplicate icon font fix
- **WHEN** the admin component SCSS files are inspected
- **THEN** none of the following files contain a `mat-icon { font-family` block: `activity-logs`, `admin-dashboard`, `allocation-move-dialog`, `allocation-rule-management`, `building-management`, `room-allocation`, `room-management`, `user-management`, `waiting-list-dashboard`
