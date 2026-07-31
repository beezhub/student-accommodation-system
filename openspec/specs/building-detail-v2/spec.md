## Requirements

### Requirement: Building detail page displays a rooms and pricing table
The building detail page SHALL include a "Rooms & Pricing" section below the existing description. This section SHALL display room information grouped by capacity. For each capacity group, the table SHALL show: capacity label (Single, Double, etc.), price range within that group, number of available rooms out of total rooms in that group.

#### Scenario: Detail page shows rooms grouped by capacity
- **WHEN** a building has 5 single rooms (capacity 1, prices R2,000-R2,500) and 3 double rooms (capacity 2, prices R3,000-R3,500)
- **THEN** the rooms table shows two rows:
  - "Single" | "R2,000 - R2,500/month" | "3 of 5 available"
  - "Double" | "R3,000 - R3,500/month" | "2 of 3 available"

#### Scenario: Capacity group with no pricing shows placeholder
- **WHEN** a capacity group has no rooms with `monthly_rate` set
- **THEN** the price column for that group shows "Price on request"

#### Scenario: Building with no rooms shows empty state
- **WHEN** a building has no rooms
- **THEN** the "Rooms & Pricing" section shows an empty state message: "Room information not yet available"

---

### Requirement: Building detail page displays accessibility features
The building detail page SHALL display the building's `accessibilityFeatures` (from `building_attributes`) when present. The features SHALL be rendered as a list or chip set in an "Accessibility" section.

#### Scenario: Building with accessibility features
- **WHEN** a building has `accessibilityFeatures` JSON data (e.g., `["Wheelchair ramp", "Elevator", "Accessible bathroom"]`)
- **THEN** the detail page shows an "Accessibility" section listing each feature

#### Scenario: Building with no accessibility features
- **WHEN** a building has no `accessibilityFeatures` data (null or empty)
- **THEN** the "Accessibility" section is not rendered (no empty section heading)

---

### Requirement: Building detail page displays total room count and availability summary
The building detail page SHALL display a summary line showing the total number of rooms and how many are currently available, positioned near the building name or in the key facts area.

#### Scenario: Building with available rooms
- **WHEN** a building has 20 total rooms and 8 available
- **THEN** the detail page shows "8 of 20 rooms available"

#### Scenario: Building fully occupied
- **WHEN** a building has 15 total rooms and 0 available
- **THEN** the detail page shows "Fully occupied" or "0 of 15 rooms available"
