## 1. Trust Signal Components

- [x] 1.1 Create standalone `HowItWorksComponent` with 3+ steps (icon, title, specific description), data handling footer note
- [x] 1.2 Create standalone `TestimonialsComponent` with 2-3 content slots (placeholder testimonials, structured as replaceable data array)
- [x] 1.3 Create standalone `SecurityAssuranceComponent` explaining document handling and data protection
- [x] 1.4 Add `HowItWorksComponent` and `TestimonialsComponent` to home/landing page
- [x] 1.5 Add `SecurityAssuranceComponent` to building detail page below main content
- [x] 1.6 Add visible support contact section to home page (support email, optionally business hours)

## 2. Response Time Expectation

- [x] 2.1 Add `application_review_response_days` system setting with default value 2 (seed via Flyway or application startup for platform tenant)
- [x] 2.2 Create a frontend service method or API call to fetch the `application_review_response_days` setting value
- [x] 2.3 Add response time message to dashboard for SUBMITTED/UNDER_REVIEW status: "You'll receive an update within X business days"
- [x] 2.4 Handle missing setting gracefully: show generic "We'll notify you as soon as there's an update" when no setting exists

## 3. Profile Setup Restructure

- [x] 3.1 Backend: Relax `PUT /api/students/profile` validation to require only `institution` and `yearOfStudy`; make other fields optional
- [x] 3.2 Frontend: Update `profile-setup.component.html` form to show only institution and year of study as required fields; remove or make optional: student number, phone, DOB, gender, special requirements
- [x] 3.3 Frontend: Add deferred profile fields (student number, phone, DOB, gender, special requirements) to `application-submission.component.html` form
- [x] 3.4 Frontend: Pre-populate deferred fields from existing student profile data if available
- [x] 3.5 Update profile-required guard to check only institution and year of study (not full profile completion)
- [x] 3.6 Backend: Ensure `POST /api/application` validates that all required student fields are present at submission time

## 4. Review Step Fix

- [x] 4.1 Move application submission (`POST /api/application` call) from `application-submission.component.ts` to `application-review.component.ts`
- [x] 4.2 Update `application-submission.component.ts` to save form data locally (e.g., service state or localStorage) instead of submitting
- [x] 4.3 Update `application-review.component.html` to display read-only summary of all application data
- [x] 4.4 Add "Submit Application" button to `application-review.component.html` that triggers the API call
- [x] 4.5 Disable submit button when required data is incomplete; show message indicating what is missing
- [x] 4.6 On successful submission from review step, navigate to `/document-upload` with success snackbar

## 5. Silent Error Audit

- [x] 5.1 Search all student-facing components (`features/auth/`, `features/dashboard/`, `features/student/`, `shared/`) for `console.error` patterns
- [x] 5.2 Create inventory of all silent error handlers (file, line, context, retryable yes/no)
- [x] 5.3 For each retryable error handler: add MatSnackBar with `error-snackbar` panelClass and descriptive message
- [x] 5.4 For each non-retryable error handler: add MatSnackBar with informational message ("Something went wrong. Please refresh the page.")
- [x] 5.5 Preserve all existing `console.error` calls (add snackbar alongside, do not replace)
- [x] 5.6 Verify no student-facing component has a `console.error` without a corresponding user notification

## 6. Verification

- [ ] 6.1 Run backend tests (`./gradlew test`) and fix any failures from validation changes
- [ ] 6.2 Run frontend lint (`npm run lint`) and fix any errors
- [ ] 6.3 Run frontend tests (`npm test`) and fix any failures
- [ ] 6.4 Verify trust signals appear on home page (How it Works, Testimonials, Support contact)
- [ ] 6.5 Verify security assurance appears on building detail pages
- [ ] 6.6 Verify profile setup completes with only institution + year of study
- [ ] 6.7 Verify application form includes deferred profile fields and submits from review step
- [ ] 6.8 Verify response time message appears on dashboard for SUBMITTED status
- [ ] 6.9 Verify no student-facing `console.error` exists without user-facing feedback
