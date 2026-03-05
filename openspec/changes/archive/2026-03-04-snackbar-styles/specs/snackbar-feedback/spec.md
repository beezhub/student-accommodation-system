## ADDED Requirements

### Requirement: Snackbars use semantic colour coding
The system SHALL apply distinct background and foreground colours to snackbar notifications based on their type (success, error, warning, info) via global CSS panel classes.

#### Scenario: Success snackbar is green
- **WHEN** a snackbar is opened with panelClass `snack-success` or `success-snackbar`
- **THEN** the snackbar surface SHALL display a dark green background (`#065f46`) with light green text (`#d1fae5`)

#### Scenario: Error snackbar is red
- **WHEN** a snackbar is opened with panelClass `snack-error` or `error-snackbar`
- **THEN** the snackbar surface SHALL display a dark red background (`#7f1d1d`) with light red text (`#fee2e2`)

#### Scenario: Warning snackbar is amber
- **WHEN** a snackbar is opened with panelClass `snack-warning` or `warning-snackbar`
- **THEN** the snackbar surface SHALL display a dark amber background (`#78350f`) with light amber text (`#fef3c7`)

#### Scenario: Info snackbar is blue
- **WHEN** a snackbar is opened with panelClass `snack-info` or `info-snackbar`
- **THEN** the snackbar surface SHALL display a dark blue background (`#0c4a6e`) with light blue text (`#e0f2fe`)

### Requirement: Snackbars use correct ARIA politeness
The centralised notification service SHALL set the `politeness` property on all snackbar configurations to ensure correct screen reader announcement timing.

#### Scenario: Error snackbars are assertive
- **WHEN** `ErrorHandlingService.showErrorSnackbar()` or `ErrorInterceptor` triggers a snackbar
- **THEN** the snackbar SHALL be configured with `politeness: 'assertive'` so screen readers interrupt immediately

#### Scenario: Non-error snackbars are polite
- **WHEN** `ErrorHandlingService.showSuccess()`, `showWarning()`, or `showInfo()` triggers a snackbar
- **THEN** the snackbar SHALL be configured with `politeness: 'polite'` so screen readers announce at next opportunity
