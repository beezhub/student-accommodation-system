## Context

The admin sidebar (`AdminSidebarComponent`) is a standalone Angular component rendered inside `mat-sidenav` in the admin layout shell. It currently holds a flat `navItems: NavItem[]` array of 17 items iterated with `*ngFor`. The UISpec mandates grouped navigation with overline labels — the flat structure cannot express this without a new data shape.

The component uses `ViewEncapsulation.Emulated` (default), so all styles are scoped. The active state is applied via `routerLinkActive="active"` and the `.active` CSS class.

## Goals / Non-Goals

**Goals:**
- Introduce `NavGroup` interface with `label` and `items: NavItem[]`
- Restructure 17 items into 5 semantic groups per UISpec section 3.13.2
- Widen sidebar to 280px
- Add 3px left-border active indicator in `$accent-purple-dark`
- Add overline group labels styled with `.type-overline` / `$slate-400`
- Add branding header area at top of sidebar
- Replace `*ngFor`/`*ngIf` with `@for`/`@if` control flow

**Non-Goals:**
- No route changes
- No collapsible groups
- No new API calls
- No changes to sidenav shell or overlay behaviour

## Decisions

**Grouped data structure over flat + metadata**
A `NavGroup[]` (each with `label` + `items`) is cleaner than adding a `group` field to each `NavItem` and filtering in the template. The template can `@for` over groups, then `@for` over items inside — more readable and directly mirrors the UISpec structure.

**`CommonModule` removal**
Since we're removing all `*ngFor`/`*ngIf` usages, `CommonModule` can be dropped from `imports`. The component already imports `RouterModule` for `routerLink`/`routerLinkActive` — this stays.

**Branding header as inline HTML** (not a sub-component)
The header is one `<div>` with a logo icon and text. Extracting to a sub-component would be over-engineering for a single-use slot.

**Active state: border not background replacement**
UISpec specifies the active item retains `$slate-100` background AND gains a 3px left border. Implemented via `border-left: 3px solid $accent-purple-dark` on `.active` list items (use negative left-margin to prevent content shift: `margin-left: -3px; padding-left: calc(original + 3px)`).

## Risks / Trade-offs

- **Content shift on active border**: Adding a 3px left border shifts text right. Mitigation: compensate with `margin-left: -3px` and adjust `padding-left` to maintain visual alignment.
- **`@for` track expression**: Angular `@for` requires `track` — use `track item.path` for nav items and `track group.label` for groups to avoid re-renders.

## Migration Plan

1. Update `.ts`: add `NavGroup` interface, replace `navItems` with `navGroups`, remove `CommonModule` from imports.
2. Rewrite `.html`: `@for (group of navGroups; track group.label)` → overline label → `@for (item of group.items; track item.path)`.
3. Update `.scss`: width 280px, `.active` border indicator, `.nav-group-label` overline styles, branding header.
4. Build verify: `ng build --configuration development` in worktree frontend.
