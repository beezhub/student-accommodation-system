## Context

`AppComponent` is the single root component for the entire SPA. Its template unconditionally renders a `mat-toolbar` followed by `<main class="main-content"><router-outlet></router-outlet></main>`. When a user navigates to `/admin/**`, Angular loads `AdminLayoutComponent` inside that `<router-outlet>`, which in turn renders `AdminSidebarComponent` — a full-height `mat-sidenav-container`. The result is a 64px-tall student toolbar sitting above the admin sidenav shell with no way to dismiss it.

Every admin page component works around this by setting `margin-top: 64px` (or `min-height: calc(100vh - 64px)`) in its container SCSS to prevent content from being hidden beneath the toolbar. This 64px magic number is duplicated across ~15 files.

The Angular router exposes the current URL through `Router.events` — the same mechanism used by the existing `isHomePage` check in `AppComponent`.

## Goals / Non-Goals

**Goals:**
- The `mat-toolbar` is not rendered at all when the URL starts with `/admin`
- The `mat-toolbar` is not rendered on the five unauthenticated auth routes (`/login`, `/signup`, `/verify-email`, `/forgot-password`, `/reset-password`) — only `/home` currently suppresses it
- All `margin-top: 64px` / `calc(100vh - 64px)` admin SCSS overrides are removed
- Admin page containers get consistent `padding-top: $space-6` (24px) as standard page top spacing

**Non-Goals:**
- Redesigning the toolbar itself (content, branding, height) — that is a separate change
- Redesigning the admin sidebar — that is the `admin-sidebar-redesign` change
- Adding new routes or guards

## Decisions

### Decision 1: URL-prefix check vs route data flag

**Options:**
- A) Check `router.url.startsWith('/admin')` on every `NavigationEnd` event
- B) Add `data: { hideToolbar: true }` to the `/admin` route definition and read it from `ActivatedRoute`

**Choice: A — URL prefix check**

Route data (B) is cleaner architecturally but requires touching `app-routing.module.ts` and traversing the activated route tree to find the deepest `data` value. A URL prefix check is one line in the existing `NavigationEnd` handler and is immediately understandable. Given the admin section is always under `/admin/**`, the URL check is reliable and maintenance-free.

### Decision 2: Signal vs BehaviorSubject for `isAdminRoute`

The existing `isHomePage` property is a plain boolean mutated inside `router.events.subscribe`. Keep the same pattern — a plain boolean — for consistency. Converting to signals would require touching `app.component.html` bindings and is out of scope for this fix.

### Decision 3: Admin page container top padding after margin removal

Once `margin-top: 64px` is removed, admin containers need their own top spacing. The admin sidebar's `<div class="content">` already has `padding: 0`, so the content slot is flush. Each admin page container should own `padding-top: $space-6` (24px) as normal page internal padding. This is the same padding already set on the horizontal sides; making all four sides consistent is correct.

## Risks / Trade-offs

**[Risk] A future route added under `/admin` could be missed by the URL check if its path doesn't start with `/admin`**
→ Mitigation: document the convention in `CLAUDE.md`; all admin routes must live under `/admin`. This is already the established pattern.

**[Risk] Removing `margin-top: 64px` from admin pages while the toolbar is still shown (e.g., if a bug prevents the hide logic from firing) would push admin content under the toolbar**
→ Mitigation: the toolbar hide fires on `NavigationEnd`, which always fires before content renders. The risk window is negligible. Manual test on dev before merge.

**[Risk] Some admin SCSS files use `min-height: calc(100vh - 64px)` rather than `margin-top: 64px`**
→ Mitigation: tasks list all affected files explicitly; reviewer confirms each one during PR.

## Migration Plan

1. Update `AppComponent` — add `/admin` and remaining auth routes to the hide-toolbar logic
2. Run dev server — confirm toolbar disappears on admin routes, still appears on student routes
3. Remove `margin-top: 64px` and `calc(100vh - 64px)` admin SCSS overrides file-by-file
4. Spot-check each admin page visually (no gap at top, no content cut off)
5. Build clean (`ng build --configuration development`) — confirm no regressions
6. No rollback complexity: the change is purely presentational, easily reverted by restoring the removed lines

## Open Questions

None. The approach is straightforward.
