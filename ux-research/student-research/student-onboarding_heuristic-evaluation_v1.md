# Heuristic Evaluation Report: BeeZhub Student-Facing Experience

**Project:** BeeZhub Student Accommodation Platform
**Evaluator:** Prism (UX Research Agent)
**Date:** 2026-04-02
**Method:** Expert Heuristic Evaluation (single evaluator, codebase-level access)
**Depends on:** [Heuristic Rubric v1](student-onboarding_heuristic-rubric_v1.md)

---

## Executive Summary

**27 findings** identified across 4 flows. Distribution by severity:

| Severity | Count | Label |
|----------|-------|-------|
| 4 (Catastrophe) | 3 | Imperative to fix |
| 3 (Major) | 9 | Important to fix |
| 2 (Minor) | 10 | Low priority |
| 1 (Cosmetic) | 5 | Fix if time allows |

**Top 5 Critical Findings:**
1. Onboarding wizard is broken — step 2 navigates to dashboard, not step 3 (Severity 4)
2. Building detail pages show no rooms, pricing, availability, or amenities (Severity 4)
3. Zero trust signals anywhere in the product (Severity 4)
4. No back navigation in any wizard step (Severity 3)
5. 35+ error cases silently swallowed with no user feedback (Severity 3)

**Systemic patterns:** The most frequently violated heuristics are H1 (Visibility of system status — 7 findings), H3 (User control and freedom — 5 findings), and S2 (Trust and credibility — 4 findings).

---

## Findings by Flow

---

### Flow 1: Pre-Auth Conversion Funnel

**Path:** Home → Building Browse → Building Detail → Signup CTA

---

### HE-01: Building detail pages critically lack decision-enabling information

**Flow:** Flow 1 — Pre-Auth Conversion Funnel
**Touchpoint:** Building detail page (`/buildings/:id`)
**Severity:** 4 (Catastrophe)
**Heuristic(s) Violated:** S3 (Appropriate information density), H6 (Recognition rather than recall)

**Observation:**
The building detail page shows: name, address, total floors, description, institution chips, and an image gallery. It does NOT show: room types, pricing/fees, capacity, availability, amenities, map location, distance to university, reviews, or ratings.

**Evidence:**
`building-detail.component.html` — the info column contains only name, institutions, address (location_on icon), totalFloors (layers icon), and photo count (photo_library icon). The CTA section reads: "Create an account or sign in to submit a housing application."

**Impact:**
Students cannot make an informed decision about whether to apply. They are asked to create an account based on a building name, address, and photos alone. This is the equivalent of asking someone to apply for an apartment after showing them only the exterior. Every major competitor (Amber, Student.com, HousingAnywhere) shows pricing and room types on their public listing pages.

**Recommendation:**
Add to public building detail: room types with pricing, availability status, key amenities, capacity/occupancy, and map location. Even if exact pricing is unavailable, show a range ("From R2,500/month"). This is the single highest-impact change for pre-auth conversion.

**Confidence:** Expert Assessment + Competitive Pattern (all 6 audited competitors show pricing publicly)

---

### HE-02: Home page has zero trust signals for a private platform

**Flow:** Flow 1 — Pre-Auth Conversion Funnel
**Touchpoint:** Home page (`/home`)
**Severity:** 4 (Catastrophe)
**Heuristic(s) Violated:** S2 (Trust and credibility)

**Observation:**
The home page shows a hero section, 3 stats ("5,000+ Students housed", "200+ Rooms available", "48h Avg. response time"), a "How it works" section with 3 steps, and a footer. There are no testimonials, no reviews, no partner logos, no security badges, no "Featured in" press mentions, no Trustpilot integration, no data handling/privacy assurances, and no university partner mentions.

**Evidence:**
`home.component.ts` — the `stats` and `features` arrays are the only content. `home.component.html` — 70 lines, no trust-building elements.

**Impact:**
For a private (non-university) platform, trust is not inherited from an institutional brand. Students and parents must trust the platform with personal data, documents, and housing decisions. With zero trust signals, the platform asks users to create accounts and upload sensitive documents on faith alone. Amber has Trustpilot (4.7/5), verified listing badges, and press logos. Student.com has university partner logos, Trustpilot integration, and testimonials. HousingAnywhere has Tenant Protection (escrow) and user testimonials. BeeZhub has nothing.

**Recommendation:**
Add at minimum: (1) 2–3 student testimonials with names/photos, (2) university partner or institutional association logos if any exist, (3) a "Your data is secure" section explaining how documents are handled, (4) any press/media mentions. If no real testimonials exist yet, add a "Verified by [entity]" or "Trusted by [N] institutions" badge once applicable. This is existential for a private platform.

**Confidence:** Expert Assessment + Competitive Pattern + Literature-Backed (Fogg, 2003 — Stanford Web Credibility Research)

---

### HE-03: Building browse shows no pricing on listing cards

**Flow:** Flow 1 — Pre-Auth Conversion Funnel
**Touchpoint:** Building browse page (`/buildings`)
**Severity:** 3 (Major)
**Heuristic(s) Violated:** S3 (Appropriate information density), H6 (Recognition rather than recall)

**Observation:**
Building cards in the browse list show: primary image, name, address, image count badge, and institution chips. No pricing, no room count, no availability indicator.

**Evidence:**
`building-browse.component.html` — card body contains only `building.name` and `building.address`. `building-browse.component.ts` loads `getAllBuildingsNoPagination()` — a flat list with client-side text search only.

**Impact:**
Students cannot compare properties on the most important dimension — cost. They must click into each building to find there's no pricing there either. This creates a dead-end browsing experience with no decision support. Additionally, loading all buildings without pagination will cause performance issues as the catalog grows.

**Recommendation:**
Add pricing range ("From RX,XXX/month"), room type summary, and availability indicator to browse cards. Implement server-side pagination and filtering (by price range, location, room type at minimum).

**Confidence:** Expert Assessment + Competitive Pattern (Amber, Student.com, HousingAnywhere all show pricing on browse cards)

---

### HE-04: No intermediate engagement between browsing and signup

**Flow:** Flow 1 — Pre-Auth Conversion Funnel
**Touchpoint:** Browse → Signup transition
**Severity:** 2 (Minor)
**Heuristic(s) Violated:** S1 (Progressive disclosure), H7 (Flexibility and efficiency of use)

**Observation:**
The only actions available from building browse/detail are: "Get Started" (→ /signup) and "Sign In" (→ /login). There is no "Save" or "Favorite" feature, no "Enquire" option, no email capture ("Get notified when rooms are available"), and no way to engage with the platform without full account creation.

**Evidence:**
`building-detail.component.html` — CTA section has exactly two buttons: "Get Started" → `/signup` and "Sign In" → `/login`.

**Impact:**
Progressive commitment theory (Cialdini, 1984) suggests that small, low-risk commitments lead to larger ones. Jumping from "browsing" to "create an account" is a large commitment gap. A "Save this building" or "Get notified" option creates an intermediate step that captures interest without requiring full registration.

**Recommendation:**
Add a "Save" / "Favorite" feature that works with local storage for anonymous users and persists after login. Consider an email capture for "Notify me when rooms are available" on buildings that are full.

**Confidence:** Expert Assessment + Literature-Backed (Cialdini, 1984 — commitment and consistency principle)

---

### HE-05: Building browse has no advanced filtering

**Flow:** Flow 1 — Pre-Auth Conversion Funnel
**Touchpoint:** Building browse page
**Severity:** 2 (Minor)
**Heuristic(s) Violated:** H7 (Flexibility and efficiency of use)

**Observation:**
The browse page has a single text search box that filters client-side on name, address, and description. There are no filters for: price range, room type, availability, distance to campus, amenities, institution.

**Evidence:**
`building-browse.component.ts` — `onSearch()` method performs case-insensitive string match on `name`, `address`, `description` only.

**Impact:**
As the property catalog grows, a single text search will be insufficient. Students with specific requirements (budget, accessibility, proximity) cannot narrow results efficiently. Competitors like Amber and HousingAnywhere offer multi-criteria filtering.

**Recommendation:**
Add filter panels for: price range, room type, institution/university, availability, and accessibility features. Implement server-side filtering to support this.

**Confidence:** Expert Assessment + Competitive Pattern

---

### Flow 2: Onboarding Wizard

**Path:** Signup → Email Verification → Profile Setup → Application → Document Upload → Review → Confirmation

---

### HE-06: Wizard flow is broken — step 2 navigates to dashboard, not step 3

**Flow:** Flow 2 — Onboarding Wizard
**Touchpoint:** Application submission → next step
**Severity:** 4 (Catastrophe)
**Heuristic(s) Violated:** H4 (Consistency and standards), H1 (Visibility of system status)

**Observation:**
The progress strip on each wizard step shows a 4-step flow: Account → Profile → Application → Documents. After successfully submitting the application (step 2 of content, step 3 of the strip), the component navigates to `/dashboard` — not to `/document-upload` (step 3 of content).

**Evidence:**
`application-submission.component.ts:291` — on success, calls `this.router.navigate(['/dashboard'])`. The progress strip in `application-submission.component.html` shows Documents as the next step, creating a visual promise that is broken.

**Impact:**
Students complete the application step and are dumped onto the dashboard, expecting the wizard to continue. They must then independently discover how to upload documents. The progress strip explicitly tells them Documents is next, but the navigation contradicts this. This is not a UX friction issue — it is a broken flow.

**Recommendation:**
Navigate to `/document-upload` on successful application submission. If the intent is to allow document upload later, change the progress strip to reflect the actual flow (3 steps, not 4) and make the dashboard's document section prominent.

**Confidence:** Expert Assessment (this is a functional bug, not a judgment call)

---

### HE-07: No back navigation in any wizard step

**Flow:** Flow 2 — Onboarding Wizard
**Touchpoint:** All wizard steps
**Severity:** 3 (Major)
**Heuristic(s) Violated:** H3 (User control and freedom)

**Observation:**
None of the wizard steps (Profile Setup, Application Submission, Document Upload, Application Review) have a "Back" or "Previous" button. The progress strip is purely visual — clicking on completed steps does nothing. Users can only move forward.

**Evidence:**
All four template files (`profile-setup.component.html`, `application-submission.component.html`, `document-upload.component.html`, `application-review.component.html`) — footer sections contain only a "Continue" or "Submit" button. No back button exists.

**Impact:**
If a student realizes they entered wrong information in a previous step, they cannot go back to fix it. They must complete the entire wizard and then navigate to their profile or application settings separately (if such editing capability exists). This violates the core "emergency exit" principle — users feel trapped in a one-way flow.

**Recommendation:**
Add "Back" button to each wizard step (except step 1). Wire progress strip steps as clickable links for completed stages.

**Confidence:** Expert Assessment + Literature-Backed (Nielsen, 1994 — H3; Tidwell, 2020 — wizard patterns)

---

### HE-08: Profile setup collects information prematurely

**Flow:** Flow 2 — Onboarding Wizard
**Touchpoint:** Profile setup (step 1 of wizard content)
**Severity:** 3 (Major)
**Heuristic(s) Violated:** S1 (Progressive disclosure)

**Observation:**
Profile setup asks for: student number, phone number, date of birth, gender, institution, year of study, and special requirements — all required except special requirements. This is the first screen after signup, before the student has seen any property details, pricing, or availability within the platform. The student has committed to an account but received zero value in return before being asked for 6 required personal data points.

**Evidence:**
`profile-setup.component.html` — 7 form fields, 6 required. This screen appears immediately after signup, before any property browsing or engagement.

**Impact:**
Progressive disclosure principle: request information when it's needed, not before. Student number, date of birth, and gender are not needed for browsing or even for initial application interest. Asking for them upfront increases perceived friction and raises data sensitivity concerns (especially for a private platform with no trust signals). Students who just want to explore the platform are forced through a data-collection gate.

**Recommendation:**
Split into two tiers: (1) Essential for browsing/exploring: institution and year of study only (needed for building filtering), (2) Required at application time: student number, phone, DOB, gender, special requirements. Let students browse and explore after providing minimal information.

**Confidence:** Expert Assessment + Literature-Backed (Wroblewski, 2008 — Web Form Design; Shneiderman, 2016)

---

### HE-09: Profile setup has rendering bug — dropdowns may not appear

**Flow:** Flow 2 — Onboarding Wizard
**Touchpoint:** Profile setup — institution and year of study dropdowns
**Severity:** 3 (Major)
**Heuristic(s) Violated:** H1 (Visibility of system status)

**Observation:**
The component uses `ChangeDetectionStrategy.OnPush` but does not call `ChangeDetectorRef.markForCheck()` after the async subscriptions for institutions and years of study complete. This means the dropdown options may never render — the selects remain empty even though data was fetched.

**Evidence:**
`profile-setup.component.ts` — `OnPush` strategy declared at line ~15. `ngOnInit` subscribes to `institutionService.getInstitutions()` and `yearOfStudyService.getYearsOfStudy()`, but neither subscription callback calls `cdr.markForCheck()`.

**Impact:**
Students may see empty dropdowns for required fields and be unable to proceed. This is intermittent (depends on Angular change detection timing) which makes it harder to diagnose. When it occurs, the wizard is completely blocked.

**Recommendation:**
Inject `ChangeDetectorRef` and call `markForCheck()` in both subscription callbacks. Alternatively, convert to `async` pipe pattern which works natively with OnPush.

**Confidence:** Expert Assessment (this is a technical bug with known Angular behavior)

---

### HE-10: Profile setup shows no error on save failure

**Flow:** Flow 2 — Onboarding Wizard
**Touchpoint:** Profile setup — submit action
**Severity:** 3 (Major)
**Heuristic(s) Violated:** H9 (Help users recognize, diagnose, and recover from errors)

**Observation:**
When `studentService.createStudentProfile()` fails, the error handler only calls `console.error()`. No snackbar, no error display, no feedback to the user. The form reverts to its idle state and the student doesn't know their profile wasn't saved.

**Evidence:**
`profile-setup.component.ts:148` — error callback contains only `console.error('Error creating student profile:', error)`. No `MatSnackBar`, no `ErrorHandlingService`, no `ErrorDisplayComponent`.

**Impact:**
The student may attempt to continue to the next step thinking their profile was saved. Depending on downstream guards, they'll either be redirected back (confusing) or encounter application errors that reference missing profile data.

**Recommendation:**
Use `ErrorHandlingService` or `MatSnackBar` to show a user-facing error message. Follow the pattern used in `application-submission.component.ts` which correctly uses `ErrorHandlingService`.

**Confidence:** Expert Assessment (this is a missing error handler, not a judgment call)

---

### HE-11: Document upload doesn't pass tenant ID — may show wrong required documents

**Flow:** Flow 2 — Onboarding Wizard
**Touchpoint:** Document upload step
**Severity:** 3 (Major)
**Heuristic(s) Violated:** H5 (Error prevention), H4 (Consistency and standards)

**Observation:**
The document upload step calls `getRequiredDocumentTypes()` without passing a tenant ID parameter. The application submission step (previous step) correctly passes the tenant ID derived from the selected building. This means the document upload step may display different required document types than what the application step showed as needed.

**Evidence:**
`document-upload.component.ts:78` — `this.documentTypeService.getRequiredDocumentTypes()` called with no tenant parameter. Compare with `application-submission.component.ts` which calls `loadDocumentReadiness(tenantId?)` with tenant-scoped logic.

**Impact:**
In a multi-tenant setup, students may see different required documents in the upload step than what was shown in the document readiness checklist. They may upload the wrong documents or miss required ones, causing application rejection.

**Recommendation:**
Pass the tenant ID (derived from the application's preferred building or stored from the previous step) to the document type fetch call in the upload step.

**Confidence:** Expert Assessment (this is a data consistency bug)

---

### HE-12: Application review step doesn't actually submit

**Flow:** Flow 2 — Onboarding Wizard
**Touchpoint:** Application review — submit button
**Severity:** 2 (Minor)
**Heuristic(s) Violated:** H4 (Consistency and standards), H2 (Match between system and real world)

**Observation:**
The "Submit Application" button on the review page does not submit anything. The application was already submitted in the previous step. The review step merely re-fetches the application list and navigates to the confirmation page.

**Evidence:**
`application-review.component.ts:155` — `onSubmit()` calls `getMyApplications()`, finds the latest application, and navigates to `/application-confirmation/${latestApplication.id}`. No API call to submit or update anything.

**Impact:**
The mental model violation is significant but not catastrophic: users expect the "Submit" button on a review page to be the final submission. If the application was already submitted in the previous step, the review step is deceptive — it creates the appearance of a final confirmation that doesn't exist. If the student abandons at the review step thinking they haven't submitted yet, they actually have.

**Recommendation:**
Either: (a) move actual submission to the review step (preferred — matches user expectation), or (b) change the button text to "View My Application" and clearly indicate the application was already submitted.

**Confidence:** Expert Assessment

---

### HE-13: "Complete Registration" label on document upload is misleading

**Flow:** Flow 2 — Onboarding Wizard
**Touchpoint:** Document upload — submit button
**Severity:** 1 (Cosmetic)
**Heuristic(s) Violated:** H2 (Match between system and real world)

**Observation:**
The submit button on the document upload step reads "Complete Registration." But registration was completed at signup. This step is document upload, part of the application flow. The label conflates registration with application.

**Evidence:**
`document-upload.component.html` — submit button text: "Complete Registration"

**Impact:**
Minor confusion about what action is being taken. Low severity because users will likely understand the intent from context.

**Recommendation:**
Change to "Upload Documents" or "Continue to Review."

**Confidence:** Expert Assessment

---

### HE-14: Signup form lacks social login options

**Flow:** Flow 2 — Onboarding Wizard
**Touchpoint:** Signup page
**Severity:** 2 (Minor)
**Heuristic(s) Violated:** H7 (Flexibility and efficiency of use)

**Observation:**
Signup requires: first name, last name, email, password, confirm password, and terms checkbox — 6 fields minimum. No social login (Google, Facebook, Apple) is offered. Password requires minimum 8 characters.

**Evidence:**
`signup.component.html` — form contains 6 fields. No social login buttons.

**Impact:**
Social login reduces signup friction by eliminating password creation and reducing fields to 1–2 clicks. Student populations have high Google account penetration. Competitors like Amber and Student.com offer social login options.

**Recommendation:**
Add Google Sign-In at minimum. Reduce visible fields: first/last name could be auto-populated from social login or deferred to profile setup.

**Confidence:** Expert Assessment + Competitive Pattern

---

### HE-15: Email verification has no clear re-entry path

**Flow:** Flow 2 — Onboarding Wizard
**Touchpoint:** Post-signup → email verification gate
**Severity:** 2 (Minor)
**Heuristic(s) Violated:** H1 (Visibility of system status), H10 (Help and documentation)

**Observation:**
After signup, the student is redirected to `/profile-setup?verificationPending=true` with a snackbar: "Please check your email to verify your account before submitting applications." If they log out and log back in without verifying, they hit the `EmailVerifiedGuard` which redirects to `/email-verification-required`. This page has: "Resend Verification Email", "I've Verified My Email" (checks localStorage), and "Sign out." However, there is no indication of how long verification takes, what to do if the email doesn't arrive (check spam), or a link to contact support.

**Evidence:**
`email-verification-required.component.html` — 3 actions, no help text about email delivery. `email-verification-required.component.ts:103` — `checkVerificationStatus()` reads localStorage, which may be stale (verification could have happened on another device/browser).

**Impact:**
Students who don't receive the email have limited recourse. The "I've Verified My Email" button only checks localStorage — if verification happened via a link opened on a different device, this check fails. No server-side verification status check is performed.

**Recommendation:**
(1) Add help text: "Check your spam/junk folder. Email may take a few minutes." (2) Make "I've Verified" check the server, not localStorage. (3) Add "Contact Support" link.

**Confidence:** Expert Assessment

---

### Flow 3: Post-Submission Experience

**Path:** Dashboard → Status Tracking → Offer → Accept/Decline

---

### HE-16: Journey stepper breaks on 3 application statuses

**Flow:** Flow 3 — Post-Submission
**Touchpoint:** Dashboard journey stepper
**Severity:** 3 (Major)
**Heuristic(s) Violated:** H1 (Visibility of system status), H5 (Error prevention)

**Observation:**
The journey stepper maps 5 statuses: SUBMITTED → UNDER_REVIEW → APPROVED → AWAITING_ALLOCATION → OFFERED. The `ApplicationStatus` enum also includes CHANGES_REQUESTED, REJECTED, and CANCELLED. When the application is in any of these 3 statuses, no step highlights as active — the stepper appears frozen with partial progress.

**Evidence:**
`dashboard.component.ts` — `journeySteps` array maps only the 5 happy-path statuses. There is no handling for CHANGES_REQUESTED, REJECTED, or CANCELLED states.

**Impact:**
A student whose application was rejected sees the same stepper showing some steps completed and some pending — with no indication of failure. This is worse than no information; it's misleading information. Students in CHANGES_REQUESTED status don't know what action to take.

**Recommendation:**
Add visual states for: (a) CHANGES_REQUESTED — highlight current step with an "Action Required" badge and clear instructions, (b) REJECTED — show the full stepper with a failure indicator and explanation, (c) CANCELLED — show the stepper with a "Cancelled" state. Use the `ErrorDisplayComponent` patterns (red for rejected, amber for changes requested).

**Confidence:** Expert Assessment

---

### HE-17: No student-facing waitlist visibility

**Flow:** Flow 3 — Post-Submission
**Touchpoint:** Dashboard — waitlist state
**Severity:** 3 (Major)
**Heuristic(s) Violated:** S4 (Reduce anxiety), H1 (Visibility of system status)

**Observation:**
The `WaitingListEntry` model has `position` and `priorityScore` fields. The admin dashboard has a full waitlist management view. Students see only a generic "On the waiting list" indicator with no position, no estimated wait time, no explanation of how the queue works, and no alternative suggestions.

**Evidence:**
`WaitingListEntry` model includes `position: number` and `priorityScore: number`. The admin-only component `waiting-list-dashboard` has full CRUD. The student dashboard has no waitlist-specific component or view.

**Impact:**
Waitlist opacity is a primary driver of anxiety and support tickets. When students don't know their position or estimated timeline, they contact support repeatedly to ask. This is the most commonly cited source of support-ticket volume in accommodation platforms (based on industry patterns from Student.com and HousingAnywhere public reports).

**Recommendation:**
Build a student-facing waitlist view showing: (a) queue position (e.g., "You are #12 in the waiting list"), (b) how the queue works (brief explanation of priority scoring), (c) estimated timeline based on historical data if available, (d) "We'll notify you when a room becomes available" reassurance.

**Confidence:** Expert Assessment + Competitive Pattern

---

### HE-18: Offer decline uses raw alert() for errors

**Flow:** Flow 3 — Post-Submission
**Touchpoint:** Dashboard — decline offer flow
**Severity:** 2 (Minor)
**Heuristic(s) Violated:** H4 (Consistency and standards), H9 (Error recognition and recovery)

**Observation:**
The offer decline error handling uses `alert()` browser dialogs in 3 places, while the rest of the application uses `MatSnackBar` or `ErrorDisplayComponent`.

**Evidence:**
`dashboard.component.ts:208` — `alert("Your application has been closed due to reaching the maximum number of declines.")`.
`dashboard.component.ts:213` — `alert("Failed to decline offer. Please try again or contact support.")`.
`dashboard.component.ts:224` — `alert("Unable to process decline request. Please try again.")`.

**Impact:**
Inconsistent with the Material Design pattern used elsewhere. Browser alerts are blocking, non-styled, and inaccessible. The UX is jarring.

**Recommendation:**
Replace all `alert()` calls with `MatSnackBar` using the existing `error-snackbar` class, or with `ErrorDisplayComponent` in banner mode.

**Confidence:** Expert Assessment

---

### HE-19: Application cancellation is a non-functional stub

**Flow:** Flow 3 — Post-Submission
**Touchpoint:** Applications list — cancel button
**Severity:** 2 (Minor)
**Heuristic(s) Violated:** H3 (User control and freedom), H4 (Consistency and standards)

**Observation:**
The application list shows a "Cancel" button that, when clicked, displays: `alert("Application cancellation is not currently available. Please contact support if you need to cancel your application.")`.

**Evidence:**
`applications.component.ts:194` — `cancelApplication()` method contains only an `alert()` call.

**Impact:**
Showing a button that doesn't work is worse than not showing the button at all. It creates false expectation and then frustration. Users who need to cancel have no self-service path.

**Recommendation:**
Either: (a) implement the cancellation flow, or (b) remove the button entirely and add a help text: "To cancel an application, please contact support at [email]."

**Confidence:** Expert Assessment

---

### HE-20: No real-time updates on dashboard

**Flow:** Flow 3 — Post-Submission
**Touchpoint:** Dashboard — all data
**Severity:** 2 (Minor)
**Heuristic(s) Violated:** H1 (Visibility of system status)

**Observation:**
All dashboard data loads once on `ngOnInit`. There is no WebSocket, SSE, or polling mechanism. If an offer expires, only the countdown timer updates — the room card, journey stepper, and allocation status remain stale until manual page refresh.

**Evidence:**
`dashboard.component.ts` — all data loading happens in `ngOnInit` subscriptions. No `setInterval`, no WebSocket, no SSE subscription.

**Impact:**
Time-sensitive actions (offer acceptance, deadline approaching) may show stale data. A student could see an "Accept Offer" button for an already-expired offer, leading to a confusing error on submission.

**Recommendation:**
Implement polling (simplest: every 60 seconds for active offer states) or WebSocket for real-time updates. At minimum, refresh dashboard data when the offer expiry timer hits zero.

**Confidence:** Expert Assessment

---

### HE-21: Dashboard shows hardcoded mock documents on API failure

**Flow:** Flow 3 — Post-Submission
**Touchpoint:** Dashboard — document section
**Severity:** 2 (Minor)
**Heuristic(s) Violated:** H5 (Error prevention), H4 (Consistency and standards)

**Observation:**
The dashboard initializes `uploadedDocuments` with hardcoded mock data: "Student ID" (Rejected), "Proof of Enrollment" (Pending), "Passport" (Verified). The `loadDocuments()` method overwrites this with real data, but if the API call fails, the mock data remains visible.

**Evidence:**
`dashboard.component.ts:79-84` — hardcoded initial data array.

**Impact:**
On API failure, students see fake document statuses that don't match reality. A student might think their "Passport" is verified when it hasn't been uploaded, or that their "Student ID" was rejected when they haven't submitted one.

**Recommendation:**
Initialize with an empty array. Use the `ErrorDisplayComponent` or a loading skeleton to handle the API failure state.

**Confidence:** Expert Assessment (this is a data integrity issue)

---

### Flow 4: Notification System

---

### HE-22: Only 2 of 17 notification types have rich detail rendering

**Flow:** Flow 4 — Notification System
**Touchpoint:** Notification dropdown
**Severity:** 2 (Minor)
**Heuristic(s) Violated:** H6 (Recognition rather than recall), H4 (Consistency and standards)

**Observation:**
The notification component renders rich contextual details (room number, building name, amount, date) only for `ROOM_ALLOCATION` and `PAYMENT_DUE` types. The other 15 types (including OFFER_EXPIRY_REMINDER, CHECK_IN_REMINDER, WAITING_LIST, ROOM_MOVED) show only a title and generic message.

**Evidence:**
`notifications.component.html` — `@if` blocks only match `ROOM_ALLOCATION` and `PAYMENT_DUE` for expanded detail.

**Impact:**
High-priority notifications (offer expiring, check-in reminder) don't show the contextual information that makes them actionable without clicking through.

**Recommendation:**
Add rich detail rendering for at least: OFFER_EXPIRY_REMINDER (show expiry date/countdown), CHECK_IN_REMINDER (show date and room), WAITING_LIST (show position if available), ROOM_MOVED (show old/new room).

**Confidence:** Expert Assessment

---

### HE-23: "View all notifications" button is non-functional

**Flow:** Flow 4 — Notification System
**Touchpoint:** Notification dropdown — footer
**Severity:** 1 (Cosmetic)
**Heuristic(s) Violated:** H3 (User control and freedom)

**Observation:**
The "View all notifications" button at the bottom of the notification dropdown has no `routerLink` or click handler. It renders but does nothing.

**Evidence:**
`notifications.component.html:103` — button exists but has no bound action.

**Impact:**
Low impact since the dropdown shows all notifications. However, a button that doesn't work creates false expectation.

**Recommendation:**
Either wire the button to a full notifications page (preferred for scalability) or remove it.

**Confidence:** Expert Assessment

---

### Cross-Flow Findings

---

### HE-24: 35+ error cases silently swallowed with console.error only

**Flow:** All flows
**Touchpoint:** Multiple components
**Severity:** 3 (Major)
**Heuristic(s) Violated:** H9 (Error recognition and recovery)

**Observation:**
51 `console.log/error/warn` statements exist across student-facing components, with approximately 35 `console.error` calls that have no corresponding user-facing feedback. Errors are swallowed — the user sees either a blank/empty state or a stale previous state.

**Evidence:**
Full inventory documented in the error handling audit. Key examples:
- `profile-setup.component.ts:148` — profile save failure, console.error only
- `applications.component.ts:116` — application load failure, sets empty array
- `dashboard.component.ts:112,138,164` — multiple load failures, console only

**Impact:**
When things go wrong, students don't know. They see empty screens, stale data, or forms that appear to work but didn't save. This is the systemic version of "the car dashboard has no warning lights."

**Recommendation:**
Audit every `console.error` call. For each: (a) if the user can take action (retry, re-login, contact support) — show a user-facing message, (b) if the user can't act — show a passive "Something went wrong, please try again later" state using `ErrorDisplayComponent`.

**Confidence:** Expert Assessment

---

### HE-25: No 404 page — unknown URLs silently redirect to home

**Flow:** All flows
**Touchpoint:** Any invalid URL
**Severity:** 1 (Cosmetic)
**Heuristic(s) Violated:** H1 (Visibility of system status), H9 (Error recognition and recovery)

**Observation:**
The wildcard route redirects all unknown URLs to `/home` with no feedback.

**Evidence:**
`app-routing.module.ts:311` — `{ path: '**', redirectTo: '/home' }`.

**Impact:**
Stale links, mistyped URLs, or bookmarks to removed pages silently redirect without explanation. Users don't know they were redirected or why.

**Recommendation:**
Add a 404 page that explains the page wasn't found and offers navigation options (Home, Dashboard, Contact Support).

**Confidence:** Expert Assessment

---

### HE-26: Token expiry shows raw technical error message

**Flow:** All flows
**Touchpoint:** Any authenticated page when token expires
**Severity:** 1 (Cosmetic)
**Heuristic(s) Violated:** H9 (Error recognition and recovery), H2 (Match between system and real world)

**Observation:**
When the JWT token expires mid-session, the token expiry interceptor throws `Error('Token expired')`. This is caught by the error interceptor and displayed in a snackbar as: "Error: Token expired" — a technical message. Additionally, the return URL is not preserved, so after re-login, the user returns to the default page rather than where they were.

**Evidence:**
`token-expiry.interceptor.ts:29-31` — throws bare `Error('Token expired')`. `error.interceptor.ts` — catches non-HTTP errors and displays their `.message` directly.

**Impact:**
Jarring for students who see a technical-sounding error. Minor because the 401 handler in the error interceptor handles the same case more gracefully with "Your session has expired" — but the token expiry interceptor fires first.

**Recommendation:**
Show "Your session has expired. Please sign in again." (matching the 401 message). Preserve the return URL in the redirect to `/login`.

**Confidence:** Expert Assessment

---

### HE-27: Backend error messages can leak to users via snackbar

**Flow:** All flows
**Touchpoint:** Any API error
**Severity:** 1 (Cosmetic)
**Heuristic(s) Violated:** H2 (Match between system and real world)

**Observation:**
The error interceptor prefers the backend's `error.message` or `error.error` field over the default user-friendly messages. If the backend returns a technical message (stack trace snippet, SQL error, Spring exception class), that raw text is shown in the snackbar.

**Evidence:**
`error.interceptor.ts:16-24` — `getErrorMessage()` extracts `error.error?.message || error.error || error.message || defaultMessage`.

**Impact:**
Occasional technical message leakage. Low frequency but unprofessional when it occurs.

**Recommendation:**
Only use backend messages that match a known safe format (e.g., check for reasonable length and absence of technical indicators like class names or stack traces). Fall back to default messages otherwise.

**Confidence:** Expert Assessment

---

## Heuristic Violation Distribution

| Heuristic | Findings | Pattern |
|-----------|----------|---------|
| H1 — Visibility of system status | HE-06, HE-09, HE-15, HE-16, HE-17, HE-20, HE-25 (7) | **Systemic.** The product consistently fails to tell users what's happening. |
| H2 — Match with real world | HE-12, HE-13, HE-26, HE-27 (4) | Labels and messages don't match user mental models. |
| H3 — User control and freedom | HE-04, HE-07, HE-19, HE-23 (4) | Users lack escape routes and navigation freedom. |
| H4 — Consistency and standards | HE-06, HE-11, HE-12, HE-18, HE-21, HE-22 (6) | Inconsistent patterns across flows. |
| H5 — Error prevention | HE-11, HE-16, HE-21 (3) | System allows preventable errors. |
| H6 — Recognition > recall | HE-01, HE-03, HE-04, HE-22 (4) | Key information is hidden, forcing memory. |
| H7 — Flexibility/efficiency | HE-04, HE-05, HE-14 (3) | Missing accelerators and flexible paths. |
| H9 — Error recovery | HE-10, HE-18, HE-24, HE-25, HE-26, HE-27 (6) | **Systemic.** Error handling is inconsistent and often absent. |
| S1 — Progressive disclosure | HE-04, HE-08 (2) | Information requested too early. |
| S2 — Trust and credibility | HE-02 (1) | Single finding but severity 4 — existential gap. |
| S3 — Information density | HE-01, HE-03 (2) | Critical information missing from public pages. |
| S4 — Reduce anxiety | HE-17 (1) | Waitlist opacity creates unnecessary anxiety. |

---

## Findings Summary by Severity

### Severity 4 — Catastrophe (3 findings)
- **HE-01:** Building details lack rooms, pricing, availability
- **HE-02:** Zero trust signals on the platform
- **HE-06:** Wizard is broken (step 2 → dashboard, not step 3)

### Severity 3 — Major (9 findings)
- **HE-03:** No pricing on browse cards
- **HE-07:** No back navigation in wizard
- **HE-08:** Profile setup collects too much info too early
- **HE-09:** Rendering bug hides dropdowns (OnPush)
- **HE-10:** No user error feedback on profile save
- **HE-11:** Document upload tenant ID mismatch
- **HE-16:** Journey stepper breaks on 3 statuses
- **HE-17:** No student waitlist visibility
- **HE-24:** 35+ silent error cases

### Severity 2 — Minor (10 findings)
- **HE-04:** No intermediate engagement (save/favorite)
- **HE-05:** No advanced browse filtering
- **HE-12:** Review step doesn't actually submit
- **HE-14:** No social login
- **HE-15:** Email verification re-entry unclear
- **HE-18:** Alert() used for decline errors
- **HE-19:** Cancel application is a stub
- **HE-20:** No real-time dashboard updates
- **HE-21:** Hardcoded mock documents
- **HE-22:** Most notifications lack rich detail

### Severity 1 — Cosmetic (5 findings)
- **HE-13:** "Complete Registration" misleading label
- **HE-23:** "View all notifications" button dead
- **HE-25:** No 404 page
- **HE-26:** Token expiry shows technical error
- **HE-27:** Backend errors can leak to snackbar

---

## Evidence Strength Disclaimer

> All findings in this report are **Expert Assessments** grounded in established usability heuristics and codebase analysis. They represent likely usability issues based on decades of HCI research and interface design principles. They have NOT been validated with actual users. A follow-up usability study with 5–8 students is recommended to validate the severity ratings and prioritization.
