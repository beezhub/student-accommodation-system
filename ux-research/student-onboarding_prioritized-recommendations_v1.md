# Prioritized Recommendations: Student Onboarding & Post-Submission Experience

**Project:** BeeZhub Student Accommodation Platform
**Researcher:** Prism (UX Research Agent)
**Date:** 2026-04-02
**Phase:** 4 — Recommendations
**Depends on:** [Consolidated Findings v1](./student-onboarding_consolidated-findings_v1.md)

---

## How This Document Is Organized

This document provides **three stakeholder-adaptive views** of the same recommendation set, plus a unified prioritization matrix. Each recommendation is traced back to its originating finding, evidence sources, and confidence level.

1. **Impact x Effort Matrix** — Unified prioritization for sprint planning
2. **PM / Leadership Brief** — Decision-oriented: what, why, what order, what if we don't
3. **Design Implications** — Pattern recommendations with annotated references
4. **Engineering Requirements** — Component-level behavioral specifications

---

## Impact x Effort Matrix

Recommendations are scored on two axes:

- **Impact** (1–5): Expected improvement in conversion, trust, completion, or anxiety reduction, weighted by finding confidence and severity
- **Effort** (1–5): Engineering complexity, where 1 = hours (config/copy change) and 5 = weeks (new system/architecture)

### Quadrant 1: Quick Wins (High Impact, Low Effort) — Do First

| # | Recommendation | Impact | Effort | Finding | Confidence |
|---|---------------|--------|--------|---------|------------|
| R-01 | Fix wizard navigation: step 2 → step 3, not dashboard | 5 | 1 | F-3a | Strong |
| R-02 | Add pricing range to building detail pages | 5 | 2 | F-1 | Strong |
| R-03 | Add CHANGES_REQUESTED, REJECTED, CANCELLED states to journey stepper | 4 | 2 | F-4 | Strong |
| R-04 | Replace all 6 `alert()` calls with Material Design snackbar | 4 | 1 | F-5 | Strong |
| R-05 | Add back navigation to all wizard steps | 4 | 2 | F-3b | Strong |
| R-06 | Fix OnPush rendering bugs (markForCheck / async pipe) | 4 | 1 | F-3b | Strong |
| R-07 | Add user-facing error feedback for profile save failure | 3 | 1 | F-5 | Strong |
| R-08 | Remove hardcoded mock documents; use empty state | 3 | 1 | F-4 | Strong |
| R-09 | Fix token expiry message: "Your session has expired. Please sign in again." | 2 | 1 | F-5 | Strong |
| R-10 | Pass tenant ID in document upload step | 3 | 1 | F-7 | Emerging |
| R-11 | Add 404 page with navigation options | 2 | 1 | F-5 | Strong |

### Quadrant 2: Strategic Bets (High Impact, High Effort) — Plan and Prioritize

| # | Recommendation | Impact | Effort | Finding | Confidence |
|---|---------------|--------|--------|---------|------------|
| R-12 | Add pricing range to building browse cards | 5 | 3 | F-1 | Strong |
| R-13 | Add room types, availability, and amenities to building detail pages | 5 | 3 | F-1 | Strong |
| R-14 | Build trust signal infrastructure: testimonials, "How it works", data handling assurance, institutional description | 5 | 3 | F-2 | Strong |
| R-15 | Student-facing waitlist view: position, queue explanation, notification promise | 4 | 3 | F-4 | Strong |
| R-16 | Restructure profile setup: split into browse-essential and application-required fields | 4 | 3 | F-3c | Strong |
| R-17 | Audit and fix all 35+ silent console.error cases in student-facing flows | 4 | 3 | F-5 | Strong |
| R-18 | Add response time expectation to post-submission ("You'll hear back within X days") | 4 | 2 | F-4 | Strong |
| R-19 | Server-side pagination and filtering for building browse | 3 | 3 | F-1 | Strong |

### Quadrant 3: Future Differentiators (High Impact, Very High Effort) — Roadmap Items

| # | Recommendation | Impact | Effort | Finding | Confidence |
|---|---------------|--------|--------|---------|------------|
| R-20 | In-app messaging system (student ↔ admin) | 5 | 5 | F-4, F-6 | Moderate |
| R-21 | Save/favorite feature with localStorage + persistence after login | 3 | 3 | F-6 | Moderate |
| R-22 | Map view with distance-to-campus calculation | 3 | 4 | F-1 | Strong |
| R-23 | Third-party review integration (Trustpilot or Google Reviews) | 4 | 4 | F-2 | Strong |
| R-24 | Wizard save-and-resume (persist state across sessions) | 3 | 4 | F-3b | Strong |
| R-25 | Social login (Google Sign-In) | 2 | 3 | F-3b | Moderate |
| R-26 | Real-time dashboard updates (polling or WebSocket for active offers) | 3 | 4 | F-4 | Strong |
| R-27 | Rich detail rendering for all 17 notification types | 2 | 3 | F-4 | Strong |
| R-28 | "Notify me" email capture for full buildings | 2 | 3 | F-6 | Moderate |

### Quadrant 4: Low Priority (Low Impact or Uncertain Return)

| # | Recommendation | Impact | Effort | Finding | Confidence |
|---|---------------|--------|--------|---------|------------|
| R-29 | Fix "Complete Registration" button label on document upload | 1 | 1 | F-3b | Strong |
| R-30 | Wire "View all notifications" button or remove it | 1 | 1 | F-4 | Strong |
| R-31 | Move actual submission to review step (or change button text) | 2 | 2 | F-3b | Strong |
| R-32 | Safe-format check for backend error messages in interceptor | 2 | 2 | F-5 | Strong |

---

## Visual Prioritization

```
                         HIGH IMPACT
                              │
          R-01 ★              │  R-12, R-13, R-14
          R-02 ★              │  R-15, R-16, R-17
          R-03, R-04          │  R-18
  Q1      R-05, R-06         │  R-19          Q2
          R-07, R-08         │
          R-09, R-10, R-11    │
                              │
  LOW EFFORT ─────────────────┼───────────────── HIGH EFFORT
                              │
  Q4      R-29, R-30         │  R-20, R-21, R-22  Q3
          R-31, R-32          │  R-23, R-24, R-25
                              │  R-26, R-27, R-28
                              │
                         LOW IMPACT

  ★ = Highest priority (Impact 5, Effort 1-2)
```

---

## Recommended Implementation Sequence

### Sprint 1: Fix What's Broken (Quick Wins — ~1-2 weeks)

**Goal:** Fix functional bugs and critical UX failures. Zero new features — just stop the bleeding.

| Priority | Recommendation | Type | Estimate |
|----------|---------------|------|----------|
| P0 | **R-01:** Fix wizard navigation (step 2 → step 3) | Bug fix | 1 hour |
| P0 | **R-06:** Fix OnPush rendering bugs (profile setup, document upload) | Bug fix | 2 hours |
| P0 | **R-10:** Pass tenant ID in document upload step | Bug fix | 1 hour |
| P1 | **R-04:** Replace 6 `alert()` calls with MatSnackBar | Consistency fix | 2 hours |
| P1 | **R-07:** Add error feedback for profile save failure | Error handling | 1 hour |
| P1 | **R-08:** Remove hardcoded mock documents; empty state | Data integrity | 1 hour |
| P1 | **R-09:** Fix token expiry message | Copy fix | 30 minutes |
| P1 | **R-11:** Add 404 page | Missing page | 2 hours |
| P2 | **R-03:** Add unhappy-path states to journey stepper | Feature gap | 4 hours |
| P2 | **R-05:** Add back navigation to wizard steps | UX fix | 4 hours |
| P2 | **R-29:** Fix "Complete Registration" label | Copy fix | 15 minutes |
| P2 | **R-30:** Wire or remove "View all notifications" | Dead button | 30 minutes |

**Expected outcome:** The core flow works end-to-end. Students can complete the wizard. Errors are visible. The stepper handles all states. No more raw `alert()` boxes. This is not a user experience improvement — it's a repair of the minimum viable experience.

### Sprint 2: Make Browsing Useful (Information Density — ~2-3 weeks)

**Goal:** Surface the information students need to make informed decisions before signup.

| Priority | Recommendation | Type | Estimate |
|----------|---------------|------|----------|
| P0 | **R-02:** Add pricing range to building detail pages | Information | 3–5 days* |
| P0 | **R-12:** Add pricing range to building browse cards | Information | 2–3 days* |
| P1 | **R-13:** Add room types, availability, amenities to detail pages | Information | 3–5 days |
| P2 | **R-19:** Server-side pagination and filtering for browse | Performance + UX | 3–5 days |

**\* Effort depends on whether pricing data exists in the room/building tables.** If the `room` table has a `price` or `rate` field, this is a frontend change. If pricing is not currently tracked, a backend migration + admin UI for price entry is required first. **This must be verified before planning Sprint 2.**

**Expected outcome:** Students can compare properties on price, room type, and availability without creating an account. The browsing experience supports informed decision-making.

### Sprint 3: Build Trust (Trust Infrastructure — ~2-3 weeks)

**Goal:** Establish minimum viable trust for a private platform.

| Priority | Recommendation | Type | Estimate |
|----------|---------------|------|----------|
| P0 | **R-14:** Trust signal package: "How it works" (enhanced), data handling assurance, team/institutional description, testimonials (placeholder → real) | Content + UI | 5–7 days |
| P1 | **R-18:** Response time expectation on post-submission dashboard | Content + minor logic | 1–2 days |
| P1 | **R-16:** Split profile setup (browse-essential vs. application-required) | Restructure | 3–5 days |
| P2 | **R-17:** Audit and fix remaining silent error cases | Systematic fix | 3–5 days |
| P2 | **R-31:** Fix review step submission semantics | Flow fix | 2–3 days |

**Expected outcome:** First-time visitors encounter a platform that explains itself, provides assurances, and doesn't front-load personal data collection. The profile setup asks only what's needed for browsing (institution, year of study) until the student chooses to apply.

### Sprint 4+: Differentiate (Strategic Features — Roadmap)

| Recommendation | Type | When |
|---------------|------|------|
| **R-15:** Student waitlist view | Feature | Sprint 4 (no competitor has this — differentiator) |
| **R-20:** In-app messaging | System | Sprint 5-6 (largest effort, highest long-term impact) |
| **R-21:** Save/favorite buildings | Feature | Sprint 4-5 |
| **R-22:** Map view with distance | Feature | Sprint 5 |
| **R-23:** Third-party reviews | Integration | When user base supports it |
| **R-24:** Wizard save-and-resume | Feature | Sprint 5 |
| **R-25:** Social login (Google) | Integration | Sprint 4 |
| **R-26:** Real-time dashboard | Architecture | Sprint 6 |

---

## PM / Leadership Decision Brief

### The Situation

BeeZhub scores 14% against a competitive average of 50% across 6 UX dimensions. It is below baseline in every dimension. The platform has 3 catastrophe-level usability issues, including a broken onboarding wizard that prevents students from completing the intended flow.

### The Core Problem

BeeZhub asks students to commit to a high-stakes housing decision (creating an account, sharing personal data, uploading identity documents) while providing almost no information to support that decision and zero reasons to trust the platform. This is not a feature gap — it is a conversion barrier.

### The Three Decisions

**Decision 1: Should property browsing be richer before sign-up?**

**Yes.** (Confidence: Strong, 3 methods converge)

Currently, building pages show name, address, photos, and floor count. No pricing. No rooms. No amenities. No availability. Every competitor shows pricing publicly. Published research (Harley 2016, Baymard 2026) confirms that sites hiding pricing are immediately ruled out by users. The browsing experience functions as a *de facto* information wall even though the pages are technically public.

**Recommended action:** Surface pricing ranges, room types, and availability on public pages. This is the highest-ROI change for pre-auth conversion.

**Decision 2: What information do students need to feel confident applying through a private platform?**

Trust signals. Multiple mechanisms. (Confidence: Strong, 3 methods converge)

BeeZhub has 1/33 on trust signals vs. competitor average 15/33. As a private platform, it cannot inherit trust from a university. It must build trust through: (1) transparency (how it works, data handling, pricing), (2) social proof (testimonials, reviews), and (3) institutional mechanisms (verification badges, guarantees — even aspirational ones signal seriousness).

**Recommended action:** Build a "trust infrastructure" package (Sprint 3) before investing in conversion optimization. Optimizing the funnel for a platform nobody trusts is pointless.

**Decision 3: Which post-submission features would most reduce anxiety and support tickets?**

In order of impact-to-effort ratio: (1) Fix the broken status tracker, (2) Add response time expectations, (3) Build waitlist visibility, (4) Eventually: messaging.

The theoretical mechanism is confirmed: Uncertainty Reduction Theory predicts that high-stakes uncertainty without information will drive alternative-channel information seeking (support tickets). The status tracker currently breaks on 3 states, and the waitlist shows nothing. Fixing these is table-stakes. Adding response time expectations ("You'll hear back within 48 hours") is the highest-ROI single feature for anxiety reduction, based on HousingAnywhere's model.

Messaging is the highest-impact long-term feature (HousingAnywhere's pre-booking messaging is the only exemplary pattern for anxiety reduction) but also the highest-effort item.

### What Happens If We Don't Act

1. **Conversion rate will remain suppressed.** Students who compare BeeZhub to any competitor will find a richer, more trustworthy experience elsewhere. The information density gap alone is likely causing significant browse-to-signup abandonment.

2. **Support ticket volume will remain high.** The post-submission transparency gap creates predictable anxiety. Without structural fixes, admin staff will continue to manually answer "What's happening with my application?" queries.

3. **Trust debt will compound.** Every student who encounters the broken wizard, empty dropdowns, raw `alert()` boxes, or information-starved pages forms a negative impression. Word-of-mouth among student populations travels fast (WhatsApp groups, residence chats). Early product impressions are disproportionately sticky.

4. **The functional bugs erode credibility.** Fogg's research (2,500+ participants) found that broken functionality receives the lowest credibility ratings regardless of content quality. The broken wizard and rendering bugs actively damage whatever trust might otherwise exist.

### Timeline Recommendation

- **Weeks 1-2:** Sprint 1 (fix what's broken — zero new features, just repair)
- **Weeks 3-5:** Sprint 2 (information density — make browsing useful)
- **Weeks 5-7:** Sprint 3 (trust infrastructure — build credibility)
- **Weeks 8+:** Sprint 4+ (differentiation features per roadmap)

This sequence is deliberate: fix bugs first (so new visitors don't encounter broken experiences), then add information (so browsing drives conversion), then build trust (so conversion drives applications), then differentiate (so applications drive loyalty).

---

## Design Implications

### Pre-Auth Browsing Patterns

**Building Browse Cards (current → recommended):**

Current state: Name, address, thumbnail image.

Recommended information hierarchy on cards:
1. **Primary image** (keep, but ensure adequate quality/size)
2. **Name** (keep)
3. **Price range** — "From R2,500/month" (NEW — most critical addition)
4. **Location** — address + distance to nearest institution (NEW)
5. **Room type chips** — "Single | Double | Shared" (NEW)
6. **Availability indicator** — green dot "Available" / amber "Limited" / grey "Full" (NEW)
7. **Institution chips** (keep, move to secondary position)

**Pattern reference:** Amber Student card model — pricing, photo, room count, star rating, distance. HousingAnywhere — pricing inclusive of utilities, review rating, availability date, save button.

**Building Detail Page (current → recommended):**

Add below-the-fold sections:
1. **Room types table** — type, price, availability, capacity per room type
2. **Amenities grid** — icons with labels (laundry, WiFi, security, parking, etc.)
3. **Map embed** — location with distance to associated institutions
4. **"How it works" mini-section** — 3-step process specific to this building
5. **Trust elements** — verification badge (if applicable), data handling note

**Pattern reference:** HousingAnywhere property detail — room options with prices, amenity icons, map, landlord info, Tenant Protection badge. Student.com — distance in minutes and km to specific university.

### Trust Signal Design

**Minimum viable trust package:**

1. **"How It Works" section (enhanced)** — Current version has 3 generic steps. Enhance with:
   - Specific step descriptions ("Browse properties with transparent pricing → Apply with your details → Get matched within 48 hours")
   - Visual icons for each step
   - "Your data is encrypted and never shared with third parties" footer note

2. **Testimonial section** — 2-3 student testimonials. If no real testimonials exist yet:
   - Use institutional description ("BeeZhub partners with accredited accommodation providers")
   - Add team transparency ("Founded in [year] by [background]")
   - Plan for testimonial collection as a priority once users exist

3. **Security assurance section** — "How we protect your data"
   - Document handling policy (how uploaded IDs are stored/encrypted)
   - Payment security (if applicable)
   - POPIA compliance statement (South African data protection)

4. **Contact accessibility** — Surface support email prominently. Add business hours. Consider WhatsApp (Amber's model shows 2-min response via WhatsApp is a regional differentiator).

**Pattern reference:** HousingAnywhere's Tenant Protection section — explains the mechanism, not just claims it's safe. Student.com's university partner logos — borrowed credibility.

### Onboarding Wizard Restructure

**Current flow (broken):**
```
Signup → Email Verify → Profile Setup (7 fields) → Application → [BREAK → Dashboard] → Document Upload → Review
```

**Recommended flow:**
```
Signup → Email Verify → Quick Profile (2 fields: institution, year) → Browse with intent →
Application (includes remaining profile fields) → Document Upload → Review → Confirmation
```

Key design changes:
- **Reduce profile setup to 2 fields** (institution, year of study) — enough to filter buildings by relevance
- **Defer 5 fields** (student number, phone, DOB, gender, special requirements) to the application step where they're actually needed
- **Add back navigation** — every step should have a "Back" button; completed steps in the progress strip should be clickable
- **Fix progress strip** — show only steps that exist in the actual flow; current step should pulse/highlight; completed steps should show checkmark
- **Descriptive button labels** — "Save Profile" not "Continue"; "Submit Application" not "Submit"; "Upload Documents" not "Complete Registration"
- **Save state** — if the student navigates away, their progress should persist (localStorage minimum, server-preferred)

### Post-Submission Status Design

**Journey stepper — unhappy path states:**

| Status | Visual Treatment | Content |
|--------|-----------------|---------|
| CHANGES_REQUESTED | Amber pulsing step + "Action Required" badge | "Changes have been requested on your application. [View details]" |
| REJECTED | Red terminal step + clear explanation | "Unfortunately, your application was not approved. [View reason] [Browse other buildings]" |
| CANCELLED | Grey terminal step | "This application was cancelled. [Start a new application]" |

**Waitlist view (new component):**

| Element | Content |
|---------|---------|
| Position | "You are #12 on the waiting list" |
| Explanation | "Rooms are allocated based on [criteria]. As rooms become available, students are offered placements in order." |
| Timeline | "Average wait time: [X] days" (if data available) or "We'll notify you as soon as a room becomes available." |
| Action | "Update your preferences" link (if multiple buildings) |

**Response time expectation:**

Add to the post-submission confirmation: "Your application is now being reviewed. You'll receive an update within [X] business days." This single sentence, based on HousingAnywhere's "48 hours" model, may be the highest-ROI single feature for anxiety reduction.

### Error State Design System

**Principle:** Every state needs a designed response. Never show nothing.

| State | Component | Treatment |
|-------|-----------|-----------|
| API error (user can retry) | MatSnackBar (error variant) | "Something went wrong. Please try again." + Retry action |
| API error (user can't retry) | ErrorDisplayComponent (banner) | "We're having trouble loading [X]. Please refresh the page or try again later." |
| Empty state (no data yet) | Empty state illustration + CTA | "No documents uploaded yet. [Upload your first document]" |
| Loading state | Skeleton loader | Shimmer effect matching content layout |
| 404 | Full page | "This page doesn't exist. [Go to Dashboard] [Browse Buildings] [Contact Support]" |
| Session expired | Dialog | "Your session has expired. Sign in to continue where you left off." [Sign In] — preserve return URL |

---

## Engineering Behavioral Requirements

### Critical Path Fixes (Sprint 1)

**R-01: Fix wizard navigation**
- File: `application-submission.component.ts`
- Line: ~291
- Current: `this.router.navigate(['/dashboard'])`
- Required: `this.router.navigate(['/document-upload'])`
- Acceptance: After successful application submission, user arrives at document upload step with progress strip showing step 3 active.

**R-06: Fix OnPush rendering bugs**
- Files: `profile-setup.component.ts`, `document-upload.component.ts`
- Issue: Components use `ChangeDetectionStrategy.OnPush` but async subscription callbacks don't call `markForCheck()`
- Fix option A: Inject `ChangeDetectorRef`, call `this.cdr.markForCheck()` in each subscription callback
- Fix option B (preferred): Convert observable subscriptions to `async` pipe pattern in template — works natively with OnPush
- Acceptance: Institution and year-of-study dropdowns render reliably on first load. Document upload progress bar updates in real-time.

**R-10: Pass tenant ID in document upload**
- File: `document-upload.component.ts`
- Line: ~78
- Current: `this.documentTypeService.getRequiredDocumentTypes()` (no tenant param)
- Required: Pass tenant ID derived from the user's application or stored from the previous step
- Acceptance: Required document types shown in upload step match those shown in application step's readiness checklist, regardless of tenant context.

**R-04: Replace alert() calls**
- Files: `dashboard.component.ts` (lines ~208, ~213, ~224), `applications.component.ts` (line ~194), and any others
- Current: `alert("message")`
- Required: `this.snackBar.open("message", "Close", { panelClass: 'error-snackbar', duration: 8000 })`
- Acceptance: No browser `alert()` dialogs anywhere in student-facing flows.

**R-07: Profile save error feedback**
- File: `profile-setup.component.ts`
- Line: ~148
- Current: `console.error('Error creating student profile:', error)`
- Required: Add `this.snackBar.open('Failed to save your profile. Please try again.', 'Retry', { panelClass: 'error-snackbar' })` or use `ErrorHandlingService` pattern from `application-submission.component.ts`
- Acceptance: When profile save fails, user sees a visible error message with retry guidance.

**R-08: Remove hardcoded mock documents**
- File: `dashboard.component.ts`
- Lines: ~79-84
- Current: `uploadedDocuments` initialized with hardcoded test data array
- Required: Initialize as empty array `[]`. Add empty state in template: "No documents uploaded yet."
- Acceptance: On API failure, user sees empty state message, not fake document statuses.

**R-03: Journey stepper unhappy-path states**
- File: `dashboard.component.ts` (journeySteps definition), `dashboard.component.html` (stepper template)
- Current: Only maps SUBMITTED, UNDER_REVIEW, APPROVED, AWAITING_ALLOCATION, OFFERED
- Required: Add handling for CHANGES_REQUESTED (amber + action badge), REJECTED (red terminal), CANCELLED (grey terminal)
- Acceptance: Every `ApplicationStatus` enum value produces a meaningful visual state in the stepper.

**R-05: Back navigation in wizard**
- Files: `profile-setup.component.html`, `application-submission.component.html`, `document-upload.component.html`, `application-review.component.html`
- Current: Footer contains only forward button
- Required: Add "Back" button (except on step 1). Progress strip steps for completed stages should be clickable links.
- Acceptance: User can navigate backward through wizard. Clicking a completed step in the progress strip returns to that step with data preserved.

**R-09: Token expiry message**
- File: `token-expiry.interceptor.ts`
- Line: ~29-31
- Current: `throw new Error('Token expired')`
- Required: `throw new Error('Your session has expired. Please sign in again.')`
- Also: Preserve current URL as returnUrl parameter in login redirect.
- Acceptance: Users see human-readable session expiry message. After re-login, return to the page they were on.

**R-11: 404 page**
- File: `app-routing.module.ts` (line ~311) + new component
- Current: `{ path: '**', redirectTo: '/home' }`
- Required: Create `NotFoundComponent` with navigation options (Home, Dashboard, Contact Support). Route wildcard to it.
- Acceptance: Unknown URLs show a helpful 404 page instead of silently redirecting to home.

### Information Density (Sprint 2)

**R-02/R-12: Pricing on browse cards and detail pages**
- **Prerequisite check:** Does the `room` entity have a price/rate field? If yes, proceed with frontend. If no, backend migration required first.
- Browse card: Add "From R[min_price]/month" below building name. Source: minimum price across all rooms in building.
- Detail page: Add room type table with columns: Type | Price/month | Availability | Capacity
- API change: Building list endpoint should include `minPrice` and `roomCount` in response DTO. Building detail endpoint should include room summaries.

**R-13: Room types, availability, amenities on detail pages**
- Backend: Building detail API should include: room list (type, price, available count, total count), building amenities, building attributes
- Frontend: Add tabbed or accordion sections below current content: "Rooms & Pricing", "Amenities", "Location"
- Data: Check if `building_attributes` and `room_attributes` tables contain amenity-type data that can be surfaced

**R-19: Server-side pagination and filtering**
- Current: `getAllBuildingsNoPagination()` loads all buildings, client-side text filter
- Required: Replace with paginated endpoint. Add filter parameters: priceMin, priceMax, roomType, institutionId, available (boolean)
- Frontend: Add filter panel component with Material Design chips/sliders

### Trust Infrastructure (Sprint 3)

**R-14: Trust signal components**
- New components needed: `TrustSignalsComponent` (home page), `SecurityAssuranceComponent` (building detail footer), `TestimonialsComponent` (home page)
- Content: Will require copywriting input — placeholder structure should be built in code with clear content slots
- Admin capability: Consider adding a CMS-like system setting for testimonials so admins can add/edit without code changes

**R-16: Profile setup restructure**
- Current: Profile setup has 7 fields (6 required), all gating the browsing experience
- Phase 1: Keep existing fields but mark student number, phone, DOB, gender as optional at profile setup
- Phase 2: Move those 4 fields to the application step (required before submission, not before browsing)
- Guard change: `ProfileRequiredGuard` (if exists) should check for institution + year only, not full profile completion

**R-17: Silent error audit**
- Process: Search all student-facing components for `console.error` calls. For each:
  1. Determine if user can take action (retry, re-authenticate, contact support)
  2. If yes: add `MatSnackBar` with action button
  3. If no: add `ErrorDisplayComponent` in passive banner mode
  4. Keep `console.error` for developer debugging — add, don't replace
- Estimated scope: ~35 error handlers to review across ~15 component files

### Data Dependencies to Verify

Before Sprint 2, engineering should confirm:

| Question | Where to Look | Impact |
|----------|--------------|--------|
| Does `room` table have a price/rate column? | `room` entity, Flyway migrations | If no: Sprint 2 requires backend migration |
| Does `building_attributes` contain amenity data? | DB schema, admin UI | If no: amenity display requires data entry system |
| Is the platform currently single-tenant or multi-tenant? | Production config, tenant table | Determines severity of R-10 (tenant ID bug) |
| Are building images stored with consistent quality? | MinIO bucket | Affects browse card visual quality |
| What notification templates exist? | `notification` table, email templates | Determines scope of R-18 (response time messaging) |

---

## Measurement Plan

If analytics are implemented, the following metrics would validate whether the recommendations achieve their intended impact:

| Metric | Baseline (Unknown) | Target | Tied to |
|--------|-------------------|--------|---------|
| Building detail → Signup conversion rate | Unknown | Measurable improvement after Sprint 2 | F-1, R-02, R-12, R-13 |
| Wizard step-1 → step-4 completion rate | Likely near 0% (wizard is broken) | >60% after Sprint 1 fixes | F-3, R-01, R-05, R-06 |
| Support tickets per 100 applications | Unknown | Measurable reduction after Sprint 3 | F-4, R-03, R-15, R-18 |
| Time from signup to application submission | Unknown | Measurable reduction after Sprint 3 | F-3c, R-16 |
| Bounce rate on building detail pages | Unknown | Measurable reduction after Sprint 2 | F-1, R-02, R-13 |

**Important caveat:** No baseline metrics exist. The research strongly recommends implementing basic analytics (page views, funnel steps, bounce rates) as part of Sprint 1 to establish baselines before making UX changes. Without baselines, the impact of improvements cannot be measured.

---

## Final Note: Evidence Limitations

This research produced 5 Strong-confidence findings, 1 Moderate, and 1 Emerging — exceeding the success criterion of 3+ Strong findings. However, every finding is based on expert assessment, competitive patterns, and published literature. **No real users were consulted.**

The three highest-value next steps for user research are:

1. **Implement analytics** (funnel tracking, bounce rates) — zero-user-effort, pure data
2. **5–8 moderated usability tests** with current or prospective students — validates severity ratings and uncovers issues expert evaluation missed
3. **Support ticket content analysis** — validates F-4 (post-submission anxiety) with real behavioral data

Until primary data is collected, the recommendations in this document represent the best available evidence for action — but they should be treated as strong hypotheses, not proven solutions.
