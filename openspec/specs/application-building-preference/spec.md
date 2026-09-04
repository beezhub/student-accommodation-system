## ADDED Requirements

### Requirement: Application stores up to 3 ordered building preferences
The system SHALL store up to 3 preferred buildings per application in order of student preference, using a dedicated `application_building_preference` table with a `preference_order` column (1, 2, 3).

#### Scenario: Student submits application with 1 preferred building
- **WHEN** a student submits an application with one preferred building
- **THEN** the system stores one record in `application_building_preference` with `preference_order = 1`

#### Scenario: Student submits application with 3 preferred buildings
- **WHEN** a student submits an application with three preferred buildings in order
- **THEN** the system stores three records in `application_building_preference` with `preference_order` 1, 2, and 3 respectively

#### Scenario: Student attempts to submit application with more than 3 buildings
- **WHEN** a student submits an application with 4 or more preferred building IDs
- **THEN** the system returns `400 Bad Request` indicating a maximum of 3 preferred buildings is allowed

### Requirement: Application form supports selecting up to 3 preferred buildings
The Angular application form SHALL replace the single building preference dropdown with a multi-building selector that allows the student to add up to 3 buildings in order of preference, displaying them as an ordered list.

#### Scenario: Student selects a first preferred building
- **WHEN** a student selects a building from the dropdown in the application form
- **THEN** the building is added as the first preference and appears in the ordered preference list

#### Scenario: Student adds a second and third preferred building
- **WHEN** the student selects additional buildings (up to 3)
- **THEN** each building is added to the ordered list with its preference rank (1st, 2nd, 3rd) displayed

#### Scenario: Student tries to add a fourth preferred building
- **WHEN** the student already has 3 preferred buildings selected
- **THEN** the building add control is disabled and a message indicates the maximum of 3 buildings has been reached

#### Scenario: Student removes a preferred building from the list
- **WHEN** the student clicks the remove action on a selected preferred building
- **THEN** the building is removed from the list and the remaining buildings are re-ranked

#### Scenario: Student selects the same building twice
- **WHEN** the student attempts to select a building that is already in their preference list
- **THEN** the building is not added again and a user-friendly message indicates it is already selected

### Requirement: Application submission API accepts array of preferred building IDs
The `POST /api/application` endpoint SHALL accept a `preferredBuildingIds` array (ordered, max 3 elements) in the request body in addition to (or instead of) the existing `preferredBuildingId` field.

#### Scenario: API receives a valid array of 3 building IDs
- **WHEN** the application payload includes `"preferredBuildingIds": [1, 5, 3]`
- **THEN** the system stores all three preferences with their respective order and returns `201 Created`

#### Scenario: API receives an empty preferred buildings array
- **WHEN** the application payload includes `"preferredBuildingIds": []`
- **THEN** the system accepts the application and stores no building preferences

#### Scenario: API receives an array with more than 3 building IDs
- **WHEN** the application payload includes `"preferredBuildingIds": [1, 2, 3, 4]`
- **THEN** the system returns `400 Bad Request`

### Requirement: Allocation scoring uses ordered building preferences
The allocation scoring system SHALL consider all stored preferred buildings (in order) when scoring a student's application for room allocation.

#### Scenario: Student is matched to their first preferred building
- **WHEN** the allocation algorithm runs and rooms are available in the student's first-choice building
- **THEN** the student receives a higher allocation score for rooms in that building

#### Scenario: Student's first preference is unavailable
- **WHEN** no rooms are available in the student's first-choice building
- **THEN** the algorithm considers the student's second and third preferred buildings for scoring
