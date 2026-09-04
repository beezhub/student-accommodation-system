## ADDED Requirements

### Requirement: Post-submission dashboard displays expected response timeline
After a student submits an application, the dashboard SHALL display a message communicating the expected review timeline. The message SHALL read "Your application is now being reviewed. You'll receive an update within X business days." where X is sourced from the system setting `application_review_response_days`.

#### Scenario: Dashboard shows response time for submitted application
- **WHEN** a student's application has status SUBMITTED or UNDER_REVIEW
- **THEN** the dashboard displays a message including the expected response time (e.g., "You'll receive an update within 2 business days")

#### Scenario: Response time is configurable via system settings
- **WHEN** the system setting `application_review_response_days` is set to 3 for a tenant
- **THEN** the dashboard for students of that tenant shows "within 3 business days"

#### Scenario: Default response time when setting is not configured
- **WHEN** no `application_review_response_days` setting exists (neither platform nor tenant level)
- **THEN** the dashboard displays a generic message without a specific day count (e.g., "Your application is being reviewed. We'll notify you as soon as there's an update.")

---

### Requirement: System setting for application review response days exists
The system SHALL support a `application_review_response_days` setting in the cascade model (platform default at `tenant_id = 0`, with optional tenant overrides). The setting value SHALL be a positive integer representing business days.

#### Scenario: Platform admin sets default response days
- **WHEN** a platform admin sets `application_review_response_days = 2` at the platform level
- **THEN** all tenants without a tenant-specific override display "within 2 business days"

#### Scenario: Tenant admin overrides response days
- **WHEN** tenant admin for tenant 5 sets `application_review_response_days = 5`
- **THEN** students of tenant 5 see "within 5 business days"
- **AND** students of other tenants still see the platform default
