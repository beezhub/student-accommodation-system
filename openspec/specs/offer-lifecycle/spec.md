## ADDED Requirements

### Requirement: Dashboard journey stepper handles all application statuses
The dashboard journey stepper SHALL display a visual treatment for every `ApplicationStatus` value, including unhappy-path states. The stepper SHALL use a status-to-visual-state mapping that covers: SUBMITTED, UNDER_REVIEW, APPROVED, AWAITING_ALLOCATION, ALLOCATED, CHANGES_REQUESTED, REJECTED, and CANCELLED. Statuses not in the happy path SHALL use distinct visual treatments to communicate their meaning.

#### Scenario: CHANGES_REQUESTED status shows amber action-required treatment
- **WHEN** a student's application has status `CHANGES_REQUESTED`
- **THEN** the journey stepper SHALL highlight the relevant step with an amber/warning colour
- **AND** the step SHALL display an action badge or label indicating the student needs to take action (e.g., "Action Required")

#### Scenario: REJECTED status shows red terminal treatment
- **WHEN** a student's application has status `REJECTED`
- **THEN** the journey stepper SHALL display the relevant step with a red/error colour
- **AND** the stepper SHALL indicate this is a terminal state (no further steps are active)

#### Scenario: CANCELLED status shows grey terminal treatment
- **WHEN** a student's application has status `CANCELLED`
- **THEN** the journey stepper SHALL display the relevant step with a grey/muted colour
- **AND** the stepper SHALL indicate this is a terminal state (no further steps are active)

#### Scenario: Happy-path statuses continue to render correctly
- **WHEN** a student's application has a happy-path status (SUBMITTED, UNDER_REVIEW, APPROVED, AWAITING_ALLOCATION, ALLOCATED)
- **THEN** the journey stepper SHALL render the step progression as it does currently, with completed steps, the active step, and remaining steps clearly distinguished
