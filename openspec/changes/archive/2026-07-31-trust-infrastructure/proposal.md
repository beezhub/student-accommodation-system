## Why

The UX research (Finding F-2, Strong confidence) identified trust as a catastrophe-level gap: BeeZhub scores 1/33 on trust signals vs. a competitor average of 15/33. As a private platform without inherited university trust, BeeZhub must actively build credibility through transparency, social proof, and institutional mechanisms. Additionally, the profile setup front-loads 7 fields before the student can browse with intent (Finding F-3c), and 35+ silent `console.error` cases across student-facing flows erode confidence when things go wrong silently (Finding F-5). The review step in the onboarding wizard also misleads students — it appears to submit the application but does not (Finding F-3b, R-31).

## What Changes

- **Trust signal components:** Add "How it Works" enhanced section, data handling assurance, institutional description, and testimonial placeholders to the home/landing page and building detail pages
- **Response time expectation:** Add post-submission messaging ("You'll receive an update within X business days") to the dashboard confirmation state
- **Profile setup restructure:** Reduce profile setup to 2 fields (institution, year of study) for browse-gating; defer remaining 5 fields (student number, phone, DOB, gender, special requirements) to the application step
- **Silent error audit:** Systematically add user-facing MatSnackBar feedback to all 35+ silent `console.error` handlers in student-facing components
- **Review step fix:** Change the application review step so that it either performs the actual submission or changes its button text to accurately describe its action (navigation to next step)

## Capabilities

### New Capabilities
- `trust-signals`: Trust signal infrastructure for home page and building detail pages — "How it Works", data handling assurance, institutional description, testimonials
- `response-time-expectation`: Post-submission messaging communicating expected review timeline to students
- `silent-error-audit`: Systematic error feedback coverage across all student-facing components

### Modified Capabilities
- `application-document-submission`: Profile setup field requirements change — only institution and year of study required for browse-gating; remaining fields deferred to application step. Review step submission semantics clarified.

## Impact

- **Frontend components created:** `TrustSignalsComponent`, `SecurityAssuranceComponent`, `TestimonialsComponent`
- **Frontend components modified:** `profile-setup`, `application-submission`, `application-review`, `dashboard`, `home/landing page`, `building-detail`, plus ~15 components for silent error audit
- **Backend changes:** Profile validation may need to be relaxed (allow partial profile with just institution + year of study); application submission validation may need additional fields
- **Admin capability:** System settings for response time expectation value (e.g., "48 hours"), potentially a CMS-like setting for testimonials
- **No database migration** — profile fields already exist, just changing when they're required
- **Guard changes:** `ProfileRequiredGuard` (if exists) should check only institution + year of study, not full profile
