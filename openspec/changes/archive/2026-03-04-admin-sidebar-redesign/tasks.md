## 1. TypeScript — data model and component class

- [x] 1.1 Add `NavGroup` interface (`label: string; items: NavItem[]`) to `admin-sidebar.component.ts`
- [x] 1.2 Replace `navItems: NavItem[]` with `navGroups: NavGroup[]` structured into 5 groups: Overview (Dashboard), Applications (Applications, Allocations, Staged Allocations, Waiting List), Buildings & Rooms (Buildings, Rooms), Configuration (Allocation Rules, System Settings), Administration (Users, Documents, Activity Logs)
- [x] 1.3 Remove `CommonModule` from component `imports` array (no longer needed after directive migration)

## 2. Template — grouped layout and modern control flow

- [x] 2.1 Add branding header `<div class="sidebar-brand">` at top of template with mat-icon and "UniAccom" text
- [x] 2.2 Replace `*ngFor="let item of navItems"` with nested `@for (group of navGroups; track group.label)` outer loop and `@for (item of group.items; track item.path)` inner loop
- [x] 2.3 Add `<div class="nav-group-label">{{ group.label }}</div>` above each group's item list
- [x] 2.4 Remove any remaining `*ngIf` directives; replace with `@if` where needed

## 3. SCSS — width, active indicator, group labels, branding

- [x] 3.1 Change sidebar width from `250px` to `280px` (update the `:host` or `.sidebar` width rule)
- [x] 3.2 Update `.active` rule: keep `background: vars.$slate-100`, add `border-left: 3px solid vars.$accent-purple-dark`, compensate with `margin-left: -3px` and adjusted `padding-left` to prevent content shift
- [x] 3.3 Add `.nav-group-label` styles: `font-size: 0.625rem`, `font-weight: 700`, `letter-spacing: 0.08em`, `text-transform: uppercase`, `color: vars.$slate-400`, padding to align with nav items
- [x] 3.4 Add `.sidebar-brand` styles: height ~64px, `display: flex; align-items: center; gap: 0.75rem`, `padding: 0 1rem`, border-bottom `1px solid vars.$slate-200`, mat-icon colour `vars.$accent-purple-dark`, font-weight 700

## 4. Verification

- [x] 4.1 Run `ng build --configuration development` in `.worktrees/angular-frontend-redesign/frontend` and confirm zero new errors
