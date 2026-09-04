## Requirements

### Requirement: Icon-only buttons have accessible labels
All icon-only action buttons (`mat-icon-button`) in admin table rows and dialogs SHALL have an `aria-label` attribute that describes the button's action.

#### Scenario: Screen reader announces edit button
- **WHEN** a screen reader user focuses an edit icon button in an admin table row
- **THEN** the button SHALL be announced with a descriptive label (e.g., "Edit") rather than being unlabeled

#### Scenario: Screen reader announces delete button
- **WHEN** a screen reader user focuses a delete icon button in an admin table row
- **THEN** the button SHALL be announced with label "Delete"

#### Scenario: Screen reader announces contextual action buttons
- **WHEN** a screen reader user focuses a view, download, or menu-trigger button
- **THEN** the button SHALL be announced with its specific action (e.g., "View Document", "Download", "More actions")

#### Scenario: aria-label matches tooltip where tooltip exists
- **WHEN** an icon button has both `matTooltip` and `aria-label`
- **THEN** the two values SHALL be identical or semantically equivalent
