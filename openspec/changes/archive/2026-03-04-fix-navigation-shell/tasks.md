## 1. AppComponent — toolbar visibility logic

- [x] 1.1 Add `isAdminRoute` boolean property to `AppComponent`, set to `false` initially
- [x] 1.2 In the existing `router.events.subscribe` handler, add a `NavigationEnd` branch that sets `isAdminRoute = event.urlAfterRedirects.startsWith('/admin')`
- [x] 1.3 Expand the `isHomePage` assignment to also cover `/signup`, `/verify-email`, `/forgot-password`, `/reset-password` (in addition to current `/home` and `/login`)
- [x] 1.4 In `app.component.html`, wrap the `<mat-toolbar>` block with `@if (!isAdminRoute)` (remove it from `isHomePage` toolbar suppression — toolbar should show on all non-admin, non-home pages)

## 2. Remove hardcoded 64px toolbar offsets from admin SCSS

- [x] 2.1 `admin-dashboard.component.scss` — remove `margin-top: 64px` from `.admin-dashboard-container`; replace `min-height: calc(100vh - 64px)` with `min-height: 100vh`; set `padding-top: $space-6`
- [x] 2.2 `application-review.component.scss` — same pattern: remove margin-top, fix min-height, set padding-top
- [x] 2.3 `document-verification.component.scss` — same
- [x] 2.4 `building-management.component.scss` — same
- [x] 2.5 `room-management.component.scss` — same
- [x] 2.6 `room-allocation.component.scss` — same
- [x] 2.7 `user-management.component.scss` — same
- [x] 2.8 `activity-logs.component.scss` — same
- [x] 2.9 `institution-management.component.scss` — not affected (no offset found)
- [x] 2.10 `document-type-management.component.scss` — not affected (no offset found)
- [x] 2.11 `year-of-study-management.component.scss` — not affected (no offset found)
- [x] 2.12 `academic-year-management.component.scss` — not affected (no offset found)
- [x] 2.13 `accommodation-type-management.component.scss` — not affected (no offset found)
- [x] 2.14 `waiting-list-dashboard.component.scss` — same
- [x] 2.15 `allocation-rule-management.component.scss` — same
- [x] 2.16 `system-settings.component.scss` — not affected (no offset found)
- [x] 2.17 `staged-allocation.component.scss` — not affected (no offset found)

## 3. Verify

- [x] 3.1 Run `ng build --configuration development` — confirm zero errors
- [x] 3.2 Confirm toolbar is absent on `/admin`, `/admin/applications`, and other nested admin routes
- [x] 3.3 Confirm toolbar is present on `/dashboard`, `/applications`, `/documents`
- [x] 3.4 Confirm toolbar is absent on `/signup`, `/forgot-password`, `/verify-email`
- [x] 3.5 Confirm no visible gap at top of any admin page (no 64px blank space)
