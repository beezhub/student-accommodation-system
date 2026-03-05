## ADDED Requirements

### Requirement: Status badge colour classes are self-contained
The `ModernStatusBadgeComponent` SHALL define all CSS colour classes it references in its own inline styles. It SHALL NOT rely on global stylesheet classes for its own visual presentation.

#### Scenario: under_review status renders with warning colour without global styles
- **WHEN** `ModernStatusBadge` is rendered with `status="UNDER_REVIEW"`
- **THEN** the badge displays with the warning colour palette (amber background, dark amber text) defined by the component's own styles

#### Scenario: offered status renders with info colour
- **WHEN** `ModernStatusBadge` is rendered with `status="OFFERED"`
- **THEN** the badge displays with the info colour palette (light blue background, dark blue text)

#### Scenario: withdrawn status renders with neutral colour
- **WHEN** `ModernStatusBadge` is rendered with `status="WITHDRAWN"`
- **THEN** the badge displays with the neutral colour palette (slate background, slate text)

#### Scenario: changes_requested status renders with orange-warning colour
- **WHEN** `ModernStatusBadge` is rendered with `status="CHANGES_REQUESTED"`
- **THEN** the badge displays with the orange warning palette (warm orange background, dark orange text)

### Requirement: Status badge labels display with correct capitalisation
The `ModernStatusBadgeComponent` SHALL display status labels exactly as defined in the config map, without CSS text transformation.

#### Scenario: Multi-word status label renders title-cased
- **WHEN** `ModernStatusBadge` is rendered with `status="UNDER_REVIEW"`
- **THEN** the visible label text is "Under Review" (not "under review" or "UNDER REVIEW")

#### Scenario: Single-word status label renders correctly
- **WHEN** `ModernStatusBadge` is rendered with `status="PENDING"`
- **THEN** the visible label text is "Pending"
