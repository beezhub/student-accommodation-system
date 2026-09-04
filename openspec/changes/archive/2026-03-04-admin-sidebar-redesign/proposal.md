## Why

The admin sidebar uses flat navigation with `*ngFor`/`*ngIf` (deprecated Angular syntax), a 250px width, and no visual grouping — making it hard to scan 17 items quickly. UISpec.md section 3.13.2 defines a grouped, 280px sidebar with overline section labels and a 3px active-indicator border.

## What Changes

- Replace the flat `navItems: NavItem[]` array with a grouped `navGroups: NavGroup[]` structure (5 groups: Overview, Applications, Buildings & Rooms, Configuration, Administration)
- Migrate all `*ngFor`/`*ngIf` template directives to `@for`/`@if` Angular 17+ control flow
- Widen sidebar from 250px to 280px
- Update active-item style: retain `$slate-100` background, add a 3px `$accent-purple-dark` left-border indicator
- Add overline group-label rows between sections (`.type-overline`, `$slate-400`)
- Add a branding/logo header at the sidebar top

## Capabilities

### New Capabilities

- `admin-sidebar-nav`: Grouped sidebar navigation with overline labels, 280px width, and left-border active indicator

### Modified Capabilities

- `admin-layout-isolation`: Nav shell visual treatment extends to the sidebar component (no spec-level behaviour change — implementation detail only)

## Impact

- `frontend/src/app/core/layout/admin-sidebar/admin-sidebar.component.ts` — new `NavGroup` interface, restructured data
- `frontend/src/app/core/layout/admin-sidebar/admin-sidebar.component.html` — new template using `@for`/`@if`
- `frontend/src/app/core/layout/admin-sidebar/admin-sidebar.component.scss` — width, active state, group-label styles
- No API changes; no route changes; purely presentational
