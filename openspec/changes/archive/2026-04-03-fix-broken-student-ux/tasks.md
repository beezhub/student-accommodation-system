## 1. Fix Wizard Navigation Flow

- [x] 1.1 Change `application-submission.component.ts` (~line 291) to navigate to `/document-upload` on successful submission instead of `/dashboard`
- [x] 1.2 Add "Back" button to `profile-setup.component.html` that navigates to the previous step
- [x] 1.3 Add "Back" button to `application-submission.component.html` that navigates to `/profile-setup`
- [x] 1.4 Add "Back" button to `document-upload.component.html` that navigates to `/application-submission`
- [x] 1.5 Make completed steps in the progress strip clickable (navigate to that step's route) and disable future steps
- [x] 1.6 Verify progress strip active-step indicator matches the current route on each wizard page

## 2. Fix OnPush Change Detection Bugs

- [x] 2.1 Convert `profile-setup.component.ts` subscription-based data loading (institutions, years of study, academic years) to observable properties with `async` pipe in template
- [x] 2.2 Update `profile-setup.component.html` to use `*ngIf="items$ | async as items"` pattern for all async-loaded dropdown data
- [x] 2.3 Convert `document-upload.component.ts` subscription-based data loading (required document types, upload progress) to observable properties with `async` pipe in template
- [x] 2.4 Update `document-upload.component.html` to use `async` pipe pattern for required document types and upload status
- [x] 2.5 Remove any manual `subscribe()` calls that directly assign to component properties in both components (replaced by async pipe)

## 3. Fix Document Upload Tenant Context

- [x] 3.1 In `document-upload.component.ts`, retrieve the tenant ID from the student's application context (preferred buildings' tenant) and pass it when calling the required document types API
- [x] 3.2 Add fallback logic: if no application context is available (direct navigation from dashboard), use the student's default tenant context
- [x] 3.3 Rename the "Complete Registration" button label to "Upload Documents" in `document-upload.component.html`

## 4. Replace All alert() Calls with MatSnackBar

- [x] 4.1 Replace `alert()` calls in `dashboard.component.ts` (application cancellation confirmation/result) with MatSnackBar using appropriate panel classes
- [x] 4.2 Replace `alert()` call in `applications.component.ts` (cancel application stub) with MatSnackBar
- [x] 4.3 Replace `alert()` call in `notifications.component.ts` ("View all" placeholder) — either wire to a notifications list page or remove the button entirely
- [x] 4.4 Audit all student-facing components for any remaining `alert()` or `window.alert()` calls and replace them

## 5. Add Error Feedback for Critical-Path Actions

- [x] 5.1 In `profile-setup.component.ts`, add MatSnackBar error notification in the profile save error handler (currently console-only)
- [x] 5.2 In `dashboard.component.ts`, add MatSnackBar error notification when application data API call fails
- [x] 5.3 In `dashboard.component.ts`, add MatSnackBar error notification when document list API call fails
- [x] 5.4 In `document-upload.component.ts`, ensure upload failure displays MatSnackBar error notification

## 6. Fix Dashboard Journey Stepper

- [x] 6.1 Extend the `journeySteps` mapping in `dashboard.component.ts` to include visual states for all 8 `ApplicationStatus` values
- [x] 6.2 Add amber/warning colour treatment for `CHANGES_REQUESTED` status with "Action Required" badge
- [x] 6.3 Add red/error colour treatment for `REJECTED` status as terminal state
- [x] 6.4 Add grey/muted colour treatment for `CANCELLED` status as terminal state
- [x] 6.5 Update `dashboard.component.html` template to render the status-specific visual treatments from the mapping

## 7. Remove Mock Document Data

- [x] 7.1 In `dashboard.component.ts`, change `uploadedDocuments` initialisation from hardcoded mock array to empty array `[]`
- [x] 7.2 Add empty state rendering in `dashboard.component.html` documents section for when `uploadedDocuments` is empty or API fails
- [x] 7.3 Remove all hardcoded document objects (e.g., `{ name: "ID Document", status: "Verified" }`) from the component source

## 8. Fix Token Expiry Message

- [x] 8.1 In `token-expiry.interceptor.ts`, change the error message from technical string (e.g., "Error: Token expired") to "Your session has expired. Please sign in again."
- [x] 8.2 Change the snackbar panel class to `warning-snackbar` for token expiry notifications
- [x] 8.3 Capture the current route and pass it as `returnUrl` query parameter when redirecting to the login page on token expiry

## 9. Add 404 Not Found Page

- [x] 9.1 Create a standalone `NotFoundComponent` with "Page Not Found" heading and navigation links (Home, Dashboard, Contact Support)
- [x] 9.2 Update `app-routing.module.ts` to route the wildcard path (`**`) to `NotFoundComponent` instead of redirecting to home
- [x] 9.3 Ensure the Dashboard link navigates to `/auth/login` for unauthenticated users

## 10. Verification

- [x] 10.1 Run `npm run lint` and fix any linting errors introduced by the changes
- [x] 10.2 Run `npm test` and fix any broken unit tests
- [ ] 10.3 Manually verify the full wizard flow: signup → profile → application → documents → dashboard
- [ ] 10.4 Verify all 8 application statuses render correctly in the journey stepper
- [x] 10.5 Verify no `alert()` calls remain in student-facing components (search codebase)
- [ ] 10.6 Verify unknown routes display the 404 page
