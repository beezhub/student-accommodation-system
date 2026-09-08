# Research Brief: Student Onboarding & Post-Submission Experience Redesign

**Project:** BeeZhub Student Accommodation Platform
**Researcher:** Prism (UX Research Agent)
**Date:** 2026-04-02
**Status:** Phase 0 — Problem Framing Complete

---

## 1. Decision Context

This research will inform the redesign of three interconnected areas of the student-facing experience:

| Decision Area | Specific Question |
|---|---|
| **Pre-auth browsing** | Should property browsing be richer before sign-up, and what information moves students from browsing to creating an account? |
| **Onboarding & application** | What causes students to abandon the onboarding wizard, and how should it be restructured to maximize completion? |
| **Post-submission experience** | Which post-submission features (status tracking, waitlist transparency, messaging) most reduce student anxiety and support-ticket volume? |

**Why now:** The platform is positioned as a private (non-university) accommodation provider. Trust is not inherited from an institutional brand — it must be earned through product experience. Getting onboarding and post-submission right is existential for conversion.

---

## 2. Current Product State (Grounded in Codebase Audit)

### Pre-Auth Browsing — What Exists
- `/buildings` is publicly accessible (no auth required)
- Students can browse buildings by name/location/description (client-side text search)
- Building detail pages show: name, address, total floors, description, images, institution chips
- **Missing from public view:** Room types, pricing/fees, capacity, availability, amenities, map location, any social proof (reviews, ratings, student count)
- CTA on detail page: "Create an account or sign in to submit a housing application" — no intermediate engagement option

### Onboarding Wizard — What Exists (4 steps)
| Step | Fields | Friction Points Found |
|---|---|---|
| 1. Profile Setup | Student number, phone, DOB, gender, institution, year of study, special requirements | No back navigation; `OnPush` rendering bug may hide dropdowns; no user-facing error on save failure |
| 2. Application | Course name, accessibility toggle, preferred buildings (gallery picker), special requirements | Document readiness checker blocks submission; on success navigates to `/dashboard` **not** step 3 (breaks wizard flow) |
| 3. Document Upload | Dynamic file inputs per required doc type; PDF/JPG/PNG, 5MB max | No tenant-scoped doc types (mismatch risk); `OnPush` bug may hide progress bar; "Complete Registration" label is misleading (registration happened at signup) |
| 4. Application Review | Read-only summary, terms checkbox | Submit button doesn't actually submit (application was submitted in step 2); review step is cosmetic |

**Critical flow break:** Step 2 sends users to the dashboard, not step 3. The 4-step wizard narrative shown in the progress strip is not the actual flow.

### Post-Submission — What Exists
- Journey stepper: Submitted → Reviewing → Approved → Awaiting → Offered
- Room allocation card with accept/decline offer dialogs
- Offer expiry countdown with 4 urgency tiers
- Notification bell with 17 notification types (but only 2 have rich detail rendering)

### Post-Submission — What's Missing
| Gap | Impact |
|---|---|
| No student-facing waitlist view | Students on the waiting list see a generic hourglass with no position, ETA, or queue context |
| No chat/messaging | Zero in-app communication between student and admin; only a "Contact Support" link |
| Journey stepper breaks on CHANGES_REQUESTED, REJECTED, CANCELLED | Stepper appears frozen or incomplete — no failure states |
| Cancel application is a stub | Button renders but shows `alert()` — "not currently available" |
| No real-time updates | All data loads once on page load; no WebSocket/SSE/polling |
| Hardcoded mock documents on dashboard | If API fails, stale test data is visible to students |

---

## 3. Stakeholder Assumptions to Test

These are the beliefs currently held by stakeholders. Each is testable. None should be treated as validated until evidence is collected.

| # | Assumption | Risk Level | Why It Matters |
|---|---|---|---|
| A1 | "Requiring signup before browsing properties drives users away" | **High** | The current browse page is already public, but shows minimal info. The real question is whether the *information gap* (no rooms, prices, availability) kills intent — not the auth wall itself. |
| A2 | "Students don't trust a non-university platform and hesitate to commit" | **High** | If true, the fix is trust signals and transparency. If false, the team may invest in the wrong problem. Currently the product has zero trust-building elements (no testimonials, no security badges, no partner logos, no data handling transparency). |
| A3 | "The 5-step onboarding flow creates too much friction" | **High** | The wizard is actually **broken** (step 2 → dashboard, not step 3). Before optimizing flow length, the flow must be functionally coherent. Additionally: is the friction from *number of steps* or from *information demanded too early* (e.g., student number before seeing any value)? |
| A4 | "Students complete applications but flood support because they can't see what happens next" | **Medium** | The journey stepper exists but breaks on several statuses. Waitlist is opaque. No messaging channel exists. The hypothesis is plausible but untested. |
| A5 | "Chat with property owner would reduce anxiety" | **Medium** | This assumes students want to communicate with *owners*. They might want to communicate with *the platform* (support) or with *other students* (social proof). Method matters. |

---

## 4. Target User Segments

| Segment | Description | Why They Matter |
|---|---|---|
| **First-time applicants** | Students applying for accommodation for the first time, likely first-year | Highest friction, lowest platform familiarity, most trust-sensitive |
| **Returning students** | Students who have used the platform before and are re-applying | Benchmark for "learned" behavior; may tolerate more friction |
| **International students** | Students from different countries with different housing expectations | Edge-case segment for accessibility, language, and cultural trust norms |
| **Students with accessibility needs** | Students requiring accessible rooms | The accessibility toggle exists but its downstream impact is unclear to users |
| **Students on waiting list** | Students who completed applications but didn't get immediate placement | Highest anxiety, highest support-ticket risk |

---

## 5. Constraints

| Constraint | Impact on Research Design |
|---|---|
| **No access to real users** | Cannot run interviews, usability tests, or surveys with actual students. Must rely on expert evaluation, competitive analysis, heuristic review, and secondary research. |
| **2–3 week timeline** | Eliminates longitudinal methods (diary studies, A/B tests). Favors rapid heuristic evaluation and competitive benchmarking. |
| **Minimal budget** | No recruiting, no incentives, no paid tools. All methods must use freely available resources. |
| **No existing data** | No analytics, no support ticket history, no prior research. Starting from zero — all findings will be heuristic-inferred or pattern-matched from prior research literature. |

---

## 6. Research Questions (Prioritized)

### Primary (Must Answer)
1. **What information do students need to see about a property BEFORE creating an account** to feel confident enough to sign up?
2. **Where in the onboarding flow do design/implementation issues create unnecessary friction**, and what is the minimum viable information we should collect at each step?
3. **What post-submission transparency features are table-stakes** in the student accommodation market, and which ones differentiate?

### Secondary (Should Answer)
4. What trust signals do competing platforms use, and which are most relevant to a private (non-university) platform?
5. How do competing platforms handle waitlist visibility and student-to-platform communication?
6. What is the industry-standard onboarding flow length and information sequencing for accommodation platforms?

### Tertiary (Nice to Know)
7. Do students prefer chat with the platform, chat with owners/staff, or peer-to-peer community features?
8. What role does mobile responsiveness play in application completion for student demographics?

---

## 7. Evidence Strength Disclaimer

> **All findings from this research will be labeled as "Expert Assessment" or "Competitive Pattern" — not "User-Evidenced."** Given the constraints (no user access, no existing data), this research can identify likely friction points and industry-standard solutions, but cannot validate user preferences or behavioral patterns. A follow-up study with real users is strongly recommended once the platform has an active user base.

---

## Next Step

→ **Phase 1: Research Plan** — Method selection, competitive audit targets, heuristic evaluation framework, and timeline.
