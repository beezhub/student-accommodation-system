## ADDED Requirements

### Requirement: OfferExpiryComponent renders urgency-aware expiry display
The system SHALL provide an `OfferExpiryComponent` that accepts an ISO expiry date string and an `AllocationStatus`, computes remaining time, and renders one of five urgency levels with distinct visual treatment.

#### Scenario: Normal urgency (> 3 days remaining)
- **WHEN** the offer expiry is more than 3 days in the future
- **THEN** the component SHALL display an info-coloured chip with icon `schedule` and label "Offer expires in N days"

#### Scenario: Warning urgency (1–3 days remaining)
- **WHEN** the offer expiry is between 1 and 3 days in the future
- **THEN** the component SHALL display a warning-coloured chip with icon `warning` and label "Expires in N days — respond soon"

#### Scenario: Critical urgency (< 24 hours remaining)
- **WHEN** the offer expiry is less than 24 hours but at least 1 hour in the future
- **THEN** the component SHALL display an error-coloured chip with icon `timer`, a pulse animation, and label "Expires in N hours — respond now"

#### Scenario: Alarm urgency (< 1 hour remaining)
- **WHEN** the offer expiry is less than 1 hour in the future
- **THEN** the component SHALL display a red-background banner with icon `alarm` and label "Expires in N minutes"

#### Scenario: Expired offer
- **WHEN** the offer expiry date is in the past or AllocationStatus is EXPIRED
- **THEN** the component SHALL display an error chip with icon `timer_off` and label "This offer has expired"

### Requirement: OfferExpiryComponent updates countdown every minute
The component SHALL recalculate remaining time every 60 seconds and update the displayed label accordingly.

#### Scenario: Countdown updates while component is mounted
- **WHEN** the component is mounted and 60 seconds elapse
- **THEN** the displayed remaining time SHALL update to reflect the new value

#### Scenario: Interval cleaned up on destroy
- **WHEN** the component is destroyed
- **THEN** the countdown interval SHALL be cleared to prevent memory leaks

### Requirement: OfferExpiryComponent is accessible
The component SHALL announce urgency-level changes to screen readers and respect reduced-motion preferences.

#### Scenario: Screen reader announcement
- **WHEN** the urgency level of a displayed offer changes
- **THEN** the container element with `aria-live="polite"` SHALL cause screen readers to announce the new label

#### Scenario: Pulse animation disabled in reduced-motion mode
- **WHEN** the user's OS preference is `prefers-reduced-motion: reduce`
- **THEN** the critical-level pulse animation SHALL NOT play
