# Solution Architect - Technical Design

## 1. Solution Overview

**High-Level Summary:**
This solution design targets the frontend layer of the Student Accommodation System to resolve technical debt in the styling system and complete the required UX patterns. The backend architecture, API contracts, and database schema remain entirely untouched. The focus is on consolidating SCSS design tokens, expanding the reusable `ModernStatusBadgeComponent` to handle 24 backend state enums, introducing reusable `SkeletonLoaderComponent` and `EmptyStateComponent` in the shared module, and building a persistent admin sidebar navigation component.

**Key Technical Decisions:**
1. **Token Consolidation** - `_tokens.scss` will be completely removed. All SCSS styling will depend exclusively on `_variables.scss` to eliminate conflicting definitions and unpredictable component rendering.
2. **Component-Driven UX** - `mat-spinner` instances used for page-level data fetching will be systematically replaced with instances of a new `SkeletonLoaderComponent`, ensuring perceived performance improvements without backend changes.
3. **Accessibility-First Implementation** - The updated status badges and new components will strictly adhere to WCAG 2.1 AA standards, utilizing proper `aria-` attributes (e.g., `aria-label`, `role="status"`). A global `prefers-reduced-motion` media query will be added to pause all CSS animations when requested by the OS.
4. **Admin Shell Extension** - The `AppNavMenuComponent` (or equivalent shell) will be extended to include an `AdminSidebarComponent` that maps directly to the 17 admin routes, using Angular's BreakpointObserver for responsive rendering.

**Architecture Diagram (ASCII):**
```
ΓöîΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÉ
Γöé           Angular Frontend                   Γöé
Γöé  ΓöîΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÉ  ΓöîΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÉ  ΓöîΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÉ  Γöé
Γöé  Γöé Admin    Γöé  Γöé Shared Modules Γöé  Γöé Student  Γöé  Γöé
Γöé  Γöé Module   Γöé  Γöé (Badge, Loader,Γöé  Γöé Module   Γöé  Γöé
Γöé  Γöé          Γöé  Γöé Empty State)   Γöé  Γöé          Γöé  Γöé
Γöé  ΓööΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÿ  ΓööΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÿ  ΓööΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÿ  Γöé
Γöé             ↑          ↑           ↑           Γöé
Γöé        ΓöîΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÉ          Γöé
Γöé        Γöé Global SCSS (_variables.scss) Γöé          Γöé
Γöé        ΓööΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÿ          Γöé
ΓööΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓö¼ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÿ
                  Γöé HTTP/REST (Untouched)
                  Γû╝
         Spring Boot Backend
```

---

## 2. Backend Architecture

**NO CHANGES REQUIRED.** 
The backend Spring Boot application, PostgreSQL database, and MinIO storage integrations remain wholly untouched by this UI specification change. Existing data DTOs and API contracts will naturally support the frontend UX improvements.

---

## 3. Frontend Architecture

### 3.1 Module Structure

The shared UI components will be implemented in the `shared` module, making them accessible to both `admin` and `student` features.

```
frontend/src/app/
├── shared/
│   ├── components/
│   │   ├── skeleton-loader/
│   │   │   ├── skeleton-loader.component.ts
│   │   │   ├── skeleton-loader.component.html
│   │   │   └── skeleton-loader.component.scss
│   │   ├── empty-state/
│   │   │   ├── empty-state.component.ts
│   │   │   ├── empty-state.component.html
│   │   │   └── empty-state.component.scss
│   │   └── modern-status-badge/
│   │       ├── modern-status-badge.component.ts
│   │       ├── modern-status-badge.component.html
│   │       └── modern-status-badge.component.scss
├── core/
│   └── layout/
│       ├── admin-sidebar/
│       │   ├── admin-sidebar.component.ts
│       │   ├── admin-sidebar.component.html
│       │   └── admin-sidebar.component.scss
├── styles/
│   ├── _variables.scss
│   └── styles.scss (where _tokens.scss is pruned)
```

### 3.2 Global Theming

**File**: `styles.scss` (and related `_variables.scss`)

- **Deletion**: Remove `_tokens.scss` entirely.
- **Addition**: Introduce the missing CSS colour classes for the new status badge states:
```scss
.status-info {
  background: var(--color-info-bg);
  color: var(--color-info);
  border: 1px solid var(--color-info-border);
}

.status-neutral {
  background: var(--color-neutral-bg);
  color: var(--color-neutral);
  border: 1px solid var(--color-slate-200);
}

.status-changes {
  background: #fff7ed;
  color: #c2410c;
  border: 1px solid #fb923c;
}

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 1ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 1ms !important;
  }
}
```

### 3.3 Angular Components

#### Skeleton Loader Component

**File**: `shared/components/skeleton-loader/skeleton-loader.component.ts`

```typescript
import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-skeleton-loader',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './skeleton-loader.component.html',
  styleUrls: ['./skeleton-loader.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SkeletonLoaderComponent {
  @Input() type: 'card' | 'table-row' | 'text-block' | 'avatar' = 'text-block';
  @Input() count: number = 1;

  get iterableCount(): number[] {
    return Array(this.count).fill(0);
  }
}
```

#### Empty State Component

**File**: `shared/components/empty-state/empty-state.component.ts`

```typescript
import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, RouterModule],
  templateUrl: './empty-state.component.html',
  styleUrls: ['./empty-state.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmptyStateComponent {
  @Input({ required: true }) icon!: string;
  @Input({ required: true }) title!: string;
  @Input({ required: true }) description!: string;
  @Input() ctaLabel?: string;
  @Input() ctaRoute?: string;
}
```

#### Modern Status Badge Component Update

**File**: `shared/components/modern-status-badge/modern-status-badge.component.ts`

Update the `configs` Map to include:
```typescript
{ key: 'under_review', icon: 'rate_review', label: 'Under Review', colorClass: 'status-warning' },
{ key: 'changes_requested', icon: 'edit_note', label: 'Changes Requested', colorClass: 'status-changes' },
{ key: 'awaiting_allocation', icon: 'hourglass_empty', label: 'Awaiting Allocation', colorClass: 'status-info' },
{ key: 'withdrawn', icon: 'undo', label: 'Withdrawn', colorClass: 'status-neutral' },
{ key: 'checked_in', icon: 'meeting_room', label: 'Checked In', colorClass: 'status-approved' },
{ key: 'offered', icon: 'local_offer', label: 'Offer Sent', colorClass: 'status-info' },
{ key: 'accepted', icon: 'check_circle', label: 'Accepted', colorClass: 'status-approved' },
{ key: 'declined', icon: 'thumb_down', label: 'Declined', colorClass: 'status-neutral' },
{ key: 'completed', icon: 'task_alt', label: 'Completed', colorClass: 'status-neutral' },
{ key: 'terminated', icon: 'block', label: 'Terminated', colorClass: 'status-rejected' },
{ key: 'submitted', icon: 'send', label: 'Submitted', colorClass: 'status-info' }
```

Ensure the HTML applies the ARIA properties properly:
```html
<div
  [class]="'modern-badge ' + config().colorClass"
  role="status"
  [attr.aria-label]="'Status: ' + config().label"
>
```

#### Admin Sidebar Component

**File**: `core/layout/admin-sidebar/admin-sidebar.component.ts`
Implement using `MatSidenav` module to be toggled by the `BreakpointObserver` from Angular CDK for responsiveness. Will contain static route configurations mapping to existing feature modules.

---

## 4. Security Design

### 4.1 Authentication & Authorization
- **Frontend Authorization**: The new `AdminSidebarComponent` will only be rendered within the Admin feature module context, protecting it behind existing Route Guards (`AuthGuard`, `AdminGuard`).

### 4.2 Accessibility Security (Compliance)
- **WCAG 2.1 AA**: All new components will strictly adhere to best accessibility practices. Contrast ratios for the new status classes have been validated in the UI Spec (e.g., `--color-success-bg` vs `--color-success-text`).

---

## 5. Performance Optimization

### 5.1 Frontend Performance
- **Perceived Performance**: Replacing `mat-spinner` instances with `SkeletonLoaderComponent` maintains the layout's structural integrity, reducing Cumulative Layout Shift (CLS) on data fetches.
- **OnPush Change Detection**: All shared presentation components (`SkeletonLoader`, `EmptyState`, `ModernStatusBadge`) will utilize `ChangeDetectionStrategy.OnPush` to minimize rendering overhead.

---

## 6. Testing Strategy

### 6.1 Frontend Testing

**Unit Tests** (`*.spec.ts`)
- **Shared Components**:
  - `ModernStatusBadgeComponent`: Verify newly added configs map correctly and ARIA tags bind correctly.
  - `SkeletonLoaderComponent`: Verify conditional rendering of correct types of skeletons and iterations based on inputs.
  - `EmptyStateComponent`: Verify conditionally rendering of CTA button based on Input provided.
- **Admin Layout**:
  - `AdminSidebarComponent`: Validate breakpoint observers dynamically collapse/expand the sidebar context.

*(Note: Target test coverage remains >80% for frontend)*

---

## 7. Technical Decisions & Rationale

**Decision 1: Skeleton Loader Shimmer CSS vs Angular Animation**
- **Selected**: CSS Keyframes for shimmer
- **Rationale**: CSS animations perform off the main thread, resulting in smoother animations that don't block JavaScript execution during potentially heavy data processing. It also trivially hooks into the `prefers-reduced-motion` global query.

**Decision 2: Status Configuration Map vs Object Dictionary**
- **Selected**: Retain the existing `configs` Typescript Map approach in `ModernStatusBadgeComponent`.
- **Rationale**: Promotes an easy, structured query `configs.get(status)` and remains consistent with the pre-existing structure of the component, adhering to the Strangler/Extension Pattern.

---

## 8. Risk Assessment

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Missing `_tokens.scss` references fail to compile | High | Medium | Execute `npm run lint` and global string search across the repository. Utilize `Angular CLI` strict mode to catch compilation failures. |
| Incomplete route integration in Admin Sidebar | Medium | Low | Route mappings verified against the `admin-routing.module.ts`. |

---

## Architecture Review Checklist

- [x] All required frontend components designed
- [x] SCSS token migration path defined
- [x] Accessibility (ARIA + Reduced Motion) addressed
- [x] Responsive admin layout mapped
- [x] Unnecessary backend changes avoided
- [x] Testing strategy covers critical UI paths
- [x] Technical decisions documented with rationale
- [x] Risk assessment completed
