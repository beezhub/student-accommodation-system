## ADDED Requirements

### Requirement: Building browse cards display pricing range
Each building card on the browse page SHALL display the building's pricing range. If the building has rooms with pricing data, the card SHALL show "From R[minMonthlyRate]/month". If the building has a price range (min differs from max), the card SHALL show "R[min] - R[max]/month". If no rooms have pricing data, the card SHALL show "Price on request".

#### Scenario: Building with uniform pricing shows single price
- **WHEN** a building has all rooms priced at R2,500/month
- **THEN** the browse card shows "From R2,500/month"

#### Scenario: Building with price range shows range
- **WHEN** a building has rooms priced between R2,000 and R3,500/month
- **THEN** the browse card shows "R2,000 - R3,500/month"

#### Scenario: Building with no pricing data shows placeholder
- **WHEN** a building has no rooms with `monthly_rate` set
- **THEN** the browse card shows "Price on request"

---

### Requirement: Building browse cards display room availability
Each building card SHALL display the number of available rooms. The availability SHALL use a colour-coded indicator: green when rooms are available, amber when availability is limited (1-2 rooms), and grey when no rooms are available.

#### Scenario: Building with multiple available rooms
- **WHEN** a building has 5 rooms with status AVAILABLE
- **THEN** the browse card shows "5 rooms available" with a green indicator

#### Scenario: Building with limited availability
- **WHEN** a building has 1 or 2 rooms with status AVAILABLE
- **THEN** the browse card shows "1 room available" (or "2 rooms available") with an amber indicator

#### Scenario: Building with no available rooms
- **WHEN** a building has 0 rooms with status AVAILABLE
- **THEN** the browse card shows "No rooms available" with a grey indicator

---

### Requirement: Building browse cards display room capacity types
Each building card SHALL display chips indicating the room capacity types available in the building. Capacity values SHALL be mapped to human-readable labels: 1 = "Single", 2 = "Double", 3 = "Triple", 4+ = "Shared ([n]-person)".

#### Scenario: Building with single and double rooms
- **WHEN** a building has rooms with capacity 1 and capacity 2
- **THEN** the browse card shows chips "Single" and "Double"

#### Scenario: Building with large shared rooms
- **WHEN** a building has rooms with capacity 4
- **THEN** the browse card shows a chip "Shared (4-person)"
