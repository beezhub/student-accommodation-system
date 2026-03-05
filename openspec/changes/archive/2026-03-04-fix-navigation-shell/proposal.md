## Why

Admin users currently see two layers of navigation simultaneously: the student-facing `mat-toolbar` (always rendered by `AppComponent`) and the admin sidebar (rendered by `AdminLayoutComponent`). This double chrome wastes screen space, confuses role boundaries, and forces every admin page component to hardcode `margin-top: 64px` to compensate for the toolbar — a fragile coupling that will break whenever the toolbar height changes.

## What Changes

- `AppComponent` hides its `mat-toolbar` when the current route is under `/admin` (router events, URL prefix check)
- `AppComponent` also hides the toolbar on all unauthenticated auth routes (`/signup`, `/verify-email`, `/forgot-password`, `/reset-password`) in addition to the current `/home` and `/login` exclusions
- All admin page component SCSS files have the hardcoded `margin-top: 64px` removed from their container selectors (affects ~15 components)
- Admin page containers get normalised top padding (`$space-6` / 24px) to replace the removed offset

## Capabilities

### New Capabilities
- `admin-layout-isolation`: The admin shell renders without the student toolbar; the toolbar is only visible on student and public routes.

### Modified Capabilities

## Impact

- `app.component.ts` — new `isAdminRoute` computed signal/property; `isHomePage` logic expanded
- `app.component.html` — `@if (!isAdminRoute)` wraps `mat-toolbar`
- `app.component.css` — no changes required
- All admin page SCSS files — remove `margin-top: 64px` from container rules
- Affected SCSS files (~15): `admin-dashboard`, `application-review`, `document-verification`, `building-management`, `room-management`, `room-allocation`, `staged-allocation`, `user-management`, `activity-logs`, `institution-management`, `document-type-management`, `year-of-study-management`, `academic-year-management`, `accommodation-type-management`, `waiting-list-dashboard`, `allocation-rule-management`, `system-settings`
- No API changes. No backend changes. No routing changes.
