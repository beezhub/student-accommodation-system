## Why

The UX research study (see `ux-research/student-onboarding_consolidated-findings_v1.md`) identified 3 catastrophe-level and 9 major usability issues in the student-facing experience. This change addresses the 11 quick-win fixes from Sprint 1 of the prioritized recommendations — all bug fixes and baseline UX repairs with high impact and low effort. The onboarding wizard is functionally broken (step 2 dumps users to the dashboard instead of step 3), error handling is systematically absent (35+ silent failures), and the post-submission status tracker breaks on 3 of 8 application states. These are not UX opinions — they are functional bugs and missing baseline behaviours confirmed by codebase inspection.

## What Changes

- **Fix wizard navigation bug:** Application submission (step 2) navigates to `/document-upload` (step 3) instead of `/dashboard`
- **Fix OnPush rendering bugs:** Add `markForCheck()` or convert to `async` pipe in profile-setup and document-upload components so dropdowns and progress bars render reliably
- **Fix tenant ID in document upload:** Pass tenant ID to `getRequiredDocumentTypes()` so required documents match the application context
- **Replace all `alert()` calls with MatSnackBar:** 6 instances in dashboard and applications components use raw browser alerts; replace with the existing `error-snackbar` pattern
- **Add error feedback for profile save failure:** Profile setup's error handler only logs to console; add user-facing snackbar
- **Remove hardcoded mock documents from dashboard:** Initialize `uploadedDocuments` as empty array; show empty state on API failure instead of fake data
- **Fix token expiry message:** Change "Error: Token expired" to "Your session has expired. Please sign in again." and preserve return URL
- **Add 404 page:** Replace wildcard redirect-to-home with a dedicated not-found page offering navigation options
- **Add unhappy-path states to journey stepper:** Handle CHANGES_REQUESTED (amber + action badge), REJECTED (red terminal), CANCELLED (grey terminal) in the dashboard stepper
- **Add back navigation to all wizard steps:** Add "Back" button to steps 2-4; make completed progress-strip steps clickable
- **Fix cosmetic issues:** Rename "Complete Registration" button to "Upload Documents"; wire or remove dead "View all notifications" button

## Capabilities

### New Capabilities
- `wizard-navigation`: Correct multi-step wizard flow with back navigation, accurate progress strip, and proper step-to-step routing
- `error-feedback-baseline`: Systematic user-facing error feedback across student-facing flows — no silent console-only failures on critical-path actions
- `not-found-page`: 404 page component for unknown routes with navigation options

### Modified Capabilities
- `application-document-submission`: Document upload step must pass tenant ID to required document types API; button label changes from "Complete Registration" to "Upload Documents"
- `offer-lifecycle`: Journey stepper must handle CHANGES_REQUESTED, REJECTED, and CANCELLED statuses with distinct visual treatments
- `snackbar-feedback`: All `alert()` calls replaced with MatSnackBar; token expiry message made user-friendly
- `empty-state-usage`: Dashboard document section uses empty state pattern instead of hardcoded mock data on API failure

## Impact

- **Frontend components modified:** `application-submission`, `profile-setup`, `document-upload`, `application-review`, `dashboard`, `applications`, `notifications`, `token-expiry.interceptor`, `app-routing`
- **No backend changes required** — all fixes are frontend-only
- **No API changes** — existing endpoints are sufficient; the document upload fix changes which parameters are sent, not the API contract
- **No database migrations**
- **No breaking changes** — all fixes improve existing behaviour without altering external contracts
