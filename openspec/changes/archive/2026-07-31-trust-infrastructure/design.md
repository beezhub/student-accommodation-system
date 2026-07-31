## Context

The UX research (Finding F-2, Strong confidence) identified that BeeZhub has almost no trust signals — scoring 1/33 vs. competitor average 15/33. As a private platform, it cannot inherit trust from a university. Finding F-3c (Strong confidence) identified that the profile setup front-loads 7 fields before browsing, creating unnecessary friction. Finding F-5 (Strong confidence) identified 35+ silent error handlers that log to console without notifying users. R-31 identified that the application review step misleads students about submission.

The frontend is Angular 21 with Material Design. The backend is Spring Boot 3.5.7. The `Student` entity has fields: `studentNumber`, `phone`, `dateOfBirth`, `gender`, `specialRequirements`, `institution`, `yearOfStudy`, `academicYear`. Profile setup currently requires all fields before the student can proceed past step 2.

## Goals / Non-Goals

**Goals:**
- Add trust signal components to home page and building detail pages
- Add response time expectation messaging to the post-submission dashboard state
- Restructure profile setup: only institution + year of study required for browse-gating; defer other fields to application step
- Audit and fix all silent `console.error` handlers in student-facing components with user-facing MatSnackBar feedback
- Fix the review step to accurately represent what it does

**Non-Goals:**
- No third-party review integration (Trustpilot, Google Reviews — Sprint 4+, R-23)
- No CMS system for managing trust content — initial content will be hardcoded with clear content slots for future CMS integration
- No testimonial collection system — use placeholder testimonials with plan for real ones
- No changes to admin-facing error handling
- No changes to the profile data model itself — all fields remain on the Student entity

## Decisions

### D1: Trust signal components as standalone presentational components

**Decision:** Create three standalone Angular components: `HowItWorksComponent` (enhanced 3-step process with specific descriptions and data assurance footer), `SecurityAssuranceComponent` (document handling, encryption, privacy policy), and `TestimonialsComponent` (2-3 student testimonials — placeholder content with clear slots). Place `HowItWorksComponent` and `TestimonialsComponent` on the home page. Place `SecurityAssuranceComponent` on building detail pages.

**Rationale:** Standalone presentational components are reusable and testable. Placing trust signals on the home page catches visitors before signup. Security assurance on building detail pages addresses the "should I trust this platform with my documents?" concern at the decision point.

**Alternative considered:** A single `TrustSectionComponent` combining all trust elements. Rejected — different pages need different trust signals, and separate components allow flexible placement.

### D2: Response time from system settings, not hardcoded

**Decision:** Store the expected response time as a system setting (`application_review_response_days`, default: 2). The dashboard reads this setting value and displays "You'll receive an update within X business days." This uses the existing `SystemSettingService` cascade model (platform default + tenant override).

**Rationale:** Different tenants may have different review timelines. Using system settings keeps it configurable without code changes. The cascade model already supports this pattern.

**Alternative considered:** Hardcode "48 hours" like HousingAnywhere. Rejected — BeeZhub is multi-tenant and different institutions may have different review speeds.

### D3: Profile setup split — backend validation change

**Decision:** Make profile setup accept a partial profile (institution + year of study only). The remaining fields (student number, phone, DOB, gender, special requirements) move to the application submission form. Backend student profile validation is relaxed: `institution` and `yearOfStudy` are required at profile creation; other fields are required only when submitting an application (`POST /api/application` validates completeness).

**Rationale:** This is the minimum viable split that reduces browse-gating friction without restructuring the data model. The Student entity keeps all fields. The change is in where/when they're collected and validated.

**Alternative considered:** Create two separate profile stages (browse profile vs application profile) with different API endpoints. Rejected — over-engineering. A single profile endpoint with relaxed validation achieves the same result.

### D4: Review step becomes a confirmation preview, not a submission trigger

**Decision:** The application review step (`application-review.component`) SHALL display a read-only summary of the application data and a clearly labelled "Submit Application" button that triggers `POST /api/application`. Currently, submission happens on the previous step and the review step does nothing actionable. Moving submission to the review step makes the flow honest: review data, then explicitly submit.

**Rationale:** The current flow is misleading — the review step implies it will submit but doesn't. Moving the actual submission to the review step aligns user expectations with system behaviour.

### D5: Silent error audit uses spreadsheet-driven approach

**Decision:** Audit all student-facing components by searching for `console.error`, `console.warn`, and `.subscribe({error:` patterns. For each handler, determine if the user can take action (retry, re-authenticate, contact support). If yes, add `MatSnackBar` with action button. If no, add a passive error banner or informational snackbar. Keep the existing `console.error` for debugging — add user feedback, don't replace logging.

**Rationale:** A systematic audit ensures no error handler is missed. The decision tree (actionable vs passive) prevents inappropriate retry suggestions for non-retryable errors.

## Risks / Trade-offs

- **[Profile split requires backend validation change]** → The Student profile `PUT` endpoint must accept partial data. This requires coordination between frontend form changes and backend validation relaxation. Risk: if backend validation isn't relaxed, the split profile will fail to save.
- **[Testimonial content needs real students]** → Initial testimonials will be placeholder. The structure should make it easy to swap in real content. Risk of looking fake if placeholders are too generic.
- **[35+ error handlers is a large audit scope]** → The audit may uncover more handlers than expected. The task list should be treated as a checklist, not a creative exercise — each fix is mechanical (add snackbar call after existing console.error).
- **[Review step submission move requires careful state management]** → Application data must be preserved across steps. Currently the application is submitted on step 3 (application form). Moving submission to step 4 (review) means the form data must be passed or stored between steps.
