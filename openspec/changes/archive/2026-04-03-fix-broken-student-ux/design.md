## Context

The UX research study (`ux-research/student-onboarding_consolidated-findings_v1.md`) identified 27 usability issues across the student-facing experience. This change addresses the 11 highest-impact, lowest-effort fixes — all frontend-only, no backend or API changes required. These are bug fixes and missing baseline behaviours, not new features.

The student-facing frontend is an Angular 21 application using Material Design components. Error handling infrastructure exists (`ErrorHandlingService`, `ErrorDisplayComponent`, `MatSnackBar` with semantic panel classes) but is inconsistently adopted — some components use it correctly while others fall back to `console.error` or raw `alert()`.

## Goals / Non-Goals

**Goals:**
- Fix the broken wizard navigation so students can complete the intended 4-step onboarding flow
- Fix rendering bugs that intermittently block wizard progress (OnPush change detection)
- Fix the tenant ID mismatch in document upload so multi-tenant deployments show correct required documents
- Eliminate all raw `alert()` calls in student-facing flows, replacing with the existing MatSnackBar pattern
- Add user-facing error feedback for all critical-path actions (profile save, application load, document upload)
- Add unhappy-path states (CHANGES_REQUESTED, REJECTED, CANCELLED) to the dashboard journey stepper
- Add back navigation to all wizard steps
- Add a 404 page for unknown routes
- Remove hardcoded mock data that shows fake document statuses on API failure
- Fix the token expiry message to be human-readable

**Non-Goals:**
- No new features (pricing display, trust signals, messaging, waitlist view — those are Sprint 2-4)
- No backend changes — all fixes are frontend Angular components
- No redesign of visual layout or information architecture
- No changes to the admin-facing experience
- No accessibility audit beyond what's needed for the specific fixes

## Decisions

### D1: Fix wizard by restoring step 2 → step 3 navigation

**Decision:** Change `application-submission.component.ts:291` to navigate to `/document-upload` on success instead of `/dashboard`.

**Rationale:** The current spec (`application-document-submission`) explicitly says to navigate to `/dashboard`. However, the research found this breaks the 4-step wizard narrative shown in the progress strip. The progress strip promises Account → Profile → Application → Documents, but the flow dumps users to the dashboard after step 3 (Application). This contradicts the visual contract. The spec will be modified to restore the intended wizard flow.

**Alternative considered:** Keep dashboard navigation and remove the Documents step from the progress strip (making it a 3-step wizard). Rejected because document upload is a required step and hiding it increases the chance students forget to upload documents.

### D2: Convert OnPush async subscriptions to async pipe pattern

**Decision:** Convert subscription-based data loading in `profile-setup.component.ts` and `document-upload.component.ts` to use Angular's `async` pipe in templates, which works natively with OnPush change detection.

**Rationale:** The alternative (injecting `ChangeDetectorRef` and calling `markForCheck()`) is fragile — every future subscriber must remember to call it. The `async` pipe pattern is idiomatic Angular, self-managing, and eliminates the category of bug.

**Alternative considered:** Inject `ChangeDetectorRef` and add `markForCheck()` calls. Viable but more error-prone for future maintenance.

### D3: Use existing snackbar infrastructure for all error feedback

**Decision:** All `alert()` replacements and new error feedback will use `MatSnackBar` with the existing semantic panel classes (`error-snackbar`, `success-snackbar`) already defined in `snackbar-feedback` spec.

**Rationale:** The infrastructure exists and is already used correctly in some components (e.g., `application-submission.component.ts`). The fix is adoption consistency, not new infrastructure.

### D4: Journey stepper uses status-to-visual-state mapping

**Decision:** Extend the `journeySteps` configuration in `dashboard.component.ts` to include a mapping for all 8 `ApplicationStatus` values, including terminal states (REJECTED, CANCELLED) and action-required states (CHANGES_REQUESTED).

**Rationale:** The current array only maps 5 happy-path statuses. Adding a status → visual treatment mapping is simpler and more maintainable than conditional template logic.

### D5: 404 page as a simple standalone component

**Decision:** Create a minimal `NotFoundComponent` with navigation links (Home, Dashboard, Contact Support) and route the wildcard path to it.

**Rationale:** A dedicated component is cleaner than intercepting navigation events. The page needs to be simple — no data fetching, no guards, no dependencies.

## Risks / Trade-offs

- **[Wizard navigation change contradicts current spec]** → Mitigated by modifying the `application-document-submission` spec as part of this change. The spec was written before the UX research identified the broken flow.
- **[Async pipe conversion may require template refactoring]** → Low risk. The conversion is mechanical: replace `this.items = result` subscription with `items$ = this.service.getItems()` observable + `*ngIf="items$ | async as items"` in template.
- **[Stepper visual states need design input for REJECTED/CANCELLED]** → Mitigated by using standard Material Design colour semantics: red for rejected, grey for cancelled, amber for action-required. No custom design needed.
- **[Profile setup field split is NOT in this change]** → The profile still collects 7 fields. That restructure is Sprint 3 (trust-infrastructure change). This change only fixes bugs in the existing flow.
