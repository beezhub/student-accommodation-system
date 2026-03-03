# Senior Full Stack Developer - Implementation Plan

## Summary

**Feature**: UI Spec Implementation (Token Consolidation, Status Badges, Skeleton Loaders, Empty States, Admin Sidebar)

**Estimated Total Effort**: 8 hours (Frontend only)

**Critical Path**: 
SCSS Consolidation -> Shared Components (Skeleton, EmptyState, Badge) -> Admin Sidebar Integration -> Replacing Spinners with Skeletons

**Parallel Work Opportunities**: 
Creating the independent shared components (SkeletonLoader, EmptyState, StatusBadge updates) can be done in parallel before integrating them into the broader application shell.

---

## Task Breakdown

### Phase 1 to Phase 7: Backend Layers (Skipped)
**Rationale**: This feature scope is purely Frontend UI/UX enhancements. No database schema, entity, repository, DTO, service, or controller changes are required.

---

### Phase 8: Global Styling & Tokens

#### Task 8.1: Consolidate SCSS Tokens
- **Files**:
  - `frontend/src/styles.scss`
  - `frontend/src/styles/_variables.scss` (or wherever defined)
  - `frontend/src/styles/_tokens.scss` (To Delete)
- **Estimated Effort**: 1 hour
- **Dependencies**: None
- **Description**: 
  - Delete `_tokens.scss`.
  - Ensure all tokens referenced map correctly to Material 3 variables in `_variables.scss` or `styles.scss`.
  - Add the new status-specific CSS classes (`.status-info`, `.status-neutral`, `.status-changes`) to `styles.scss`.
  - Add the `@media (prefers-reduced-motion: reduce)` block to `styles.scss` to disable animations globally for accessibility.
- **Acceptance Criteria**:
  - [ ] `_tokens.scss` is completely removed from the codebase.
  - [ ] The Angular application compiles successfully with no SCSS reference errors.
  - [ ] New status classes are available globally.
  - [ ] Reduced motion media query is properly applied.

---

### Phase 9: Shared Components Implementation

#### Task 9.1: Implement SkeletonLoaderComponent
- **Files**:
  - `frontend/src/app/shared/components/skeleton-loader/skeleton-loader.component.ts`
  - `frontend/src/app/shared/components/skeleton-loader/skeleton-loader.component.html`
  - `frontend/src/app/shared/components/skeleton-loader/skeleton-loader.component.scss`
- **Estimated Effort**: 1.5 hours
- **Dependencies**: Task 8.1
- **Description**:
  - Create a standalone component that accepts `type` ('card' | 'table-row' | 'text-block' | 'avatar') and `count` inputs.
  - Implement the shimmer CSS animation using `linear-gradient` over a slate background.
- **Acceptance Criteria**:
  - [ ] Component renders the specified number of skeleton blocks based on the `type` input.
  - [ ] Shimmer animation works smoothly and halts when `prefers-reduced-motion` is active.
  - [ ] Component is exported and accessible for use in other modules.

#### Task 9.2: Implement EmptyStateComponent
- **Files**:
  - `frontend/src/app/shared/components/empty-state/empty-state.component.ts`
  - `frontend/src/app/shared/components/empty-state/empty-state.component.html`
  - `frontend/src/app/shared/components/empty-state/empty-state.component.scss`
- **Estimated Effort**: 1 hour
- **Dependencies**: Task 8.1
- **Description**:
  - Create a standalone component that accepts `icon`, `title`, `description`, `ctaLabel`, and `ctaRoute`.
  - Apply standard centering and slate color typographies.
- **Acceptance Criteria**:
  - [ ] Component renders correctly with or without the optional CTA button.
  - [ ] Layout is fully responsive.

#### Task 9.3: Expand ModernStatusBadgeComponent
- **Files**:
  - `frontend/src/app/shared/components/modern-status-badge/modern-status-badge.component.ts`
  - `frontend/src/app/shared/components/modern-status-badge/modern-status-badge.component.html`
- **Estimated Effort**: 1 hour
- **Dependencies**: Task 8.1
- **Description**:
  - Expand the `configs` Map to include all 24 statuses defined in the Solution Design.
  - Apply `role="status"` and dynamic `[attr.aria-label]="...label"` to the HTML template for accessibility.
- **Acceptance Criteria**:
  - [ ] All 24 statuses map without fallback to the default unknown tag.
  - [ ] Screen readers correctly announce the status label.

---

### Phase 10: Routing & Application Shell

#### Task 10.1: Implement AdminSidebarComponent
- **Files**:
  - `frontend/src/app/core/layout/admin-sidebar/admin-sidebar.component.ts`
  - `frontend/src/app/core/layout/admin-sidebar/admin-sidebar.component.html`
  - `frontend/src/app/core/layout/admin-sidebar/admin-sidebar.component.scss`
- **Estimated Effort**: 2 hours
- **Dependencies**: Task 8.1
- **Description**:
  - Create a persistent sidebar using `@angular/material/sidenav`.
  - Use Angular CDK `BreakpointObserver` to handle responsive behavior (`auto` collapse on smaller screens).
  - Add navigation links utilizing `routerLink` for the 17 admin routes (e.g., Dashboard, Applications, Buildings).
- **Acceptance Criteria**:
  - [ ] Sidebar renders on large screens consistently.
  - [ ] Navigating clicks successfully load the respective routed component.
  - [ ] Responsive behavior accurately hides/displays the navigation menu toggle.

#### Task 10.2: Integrate AdminSidebar into Admin Shell
- **Files**:
  - Admin layout wrapper (e.g., `admin.component.html` or `app-nav-menu.component.ts`)
- **Estimated Effort**: 1 hour
- **Dependencies**: Task 10.1
- **Description**:
  - Wrap the primary admin `<router-outlet>` inside the new `admin-sidebar` container.
- **Acceptance Criteria**:
  - [ ] The admin shell correctly utilizes the new sidebar.
  - [ ] Existing view structures are unaffected by the layout injection.

---

### Phase 11 & 12: Refactoring & Testing

#### Task 11.1: Replace Spinners with SkeletonLoaders
- **Files**:
  - Any heavily loaded admin table or student dashboard view (e.g., `application-review.component.html`)
- **Estimated Effort**: 1 hour
- **Dependencies**: Task 9.1
- **Description**:
  - Search the codebase for `<mat-spinner>` instances that represent page-level loading.
  - Swap them to `<app-skeleton-loader [type]="'table-row'" [count]="5"></app-skeleton-loader>` (or relevant variant).
- **Acceptance Criteria**:
  - [ ] Spinner is no longer visible on major data fetches.
  - [ ] Skeletons display smoothly before populating the real data.

#### Task 12.1: End-to-End Visual QA
- **Estimated Effort**: 0.5 hours
- **Description**:
  - Spin up the frontend locally (`npm run start`).
  - Log in as Admin and verify sidebar navigation to all modules.
  - Trigger a loading state and verify skeleton loaders.
  - Emulate reduced motion OS preference and ensure animations halt.
  - Check status badges for correct rendering (especially "Changes Requested" and "Checked In").

---

## Pre-Implementation Checklist

- [x] Business requirements approved by stakeholders
- [x] Technical design reviewed and approved
- [x] Development environment set up (`cd frontend && npm install`)

## Post-Implementation Checklist

- [ ] All Angular unit tests passing (`npm test`)
- [ ] Linting and SCSS compilation checks passing (`npm run lint`)
- [ ] Responsive layout validated across `xs`, `sm`, `md`, `lg` breakpoints
- [ ] Accessibility validated (`aria-labels` and contrast ratios)
