## Implement Feature Flags UI

### 1. Create FeatureFlagService

- [x] 1.1 Create `frontend/src/app/core/services/feature-flag.service.ts` with:
  - `FeatureFlag` interface: `{ uid: string; enable: boolean; description?: string; group?: string; }`
  - `getAll()` → GET `/api/admin/features`, map the response object to `FeatureFlag[]`
  - `enable(uid: string)` → POST `/api/admin/features/${uid}/enable`
  - `disable(uid: string)` → POST `/api/admin/features/${uid}/disable`

### 2. Create FeatureFlagManagementComponent

- [x] 2.1 Create `frontend/src/app/features/admin/feature-flag-management/feature-flag-management.component.ts`:
  - Standalone, OnPush
  - Inject `FeatureFlagService` and `ErrorHandlingService`
  - Properties: `flags: FeatureFlag[]`, `loading = true`, `displayedColumns = ['name', 'description', 'group', 'status']`
  - `ngOnInit()` calls `loadFlags()`
  - `loadFlags()` fetches all flags, sets `loading = false`
  - `toggleFlag(flag)` optimistically flips `flag.enable`, calls enable/disable, reverts on error
  - Imports: CommonModule, MatTableModule, MatSlideToggleModule, MatIconModule, MatCardModule, MatButtonModule, MatProgressSpinnerModule, SkeletonLoaderComponent, EmptyStateComponent, RouterModule

- [x] 2.2 Create `frontend/src/app/features/admin/feature-flag-management/feature-flag-management.component.html`:
  - Page header: icon `toggle_on`, title "Feature Flags", subtitle "Manage FF4J feature flags"
  - `@if (loading)` → `<app-skeleton-loader>` (3 rows)
  - `@else if (flags.length === 0)` → `<app-empty-state icon="toggle_off" title="No Feature Flags" description="No feature flags are registered in the system.">`
  - `@else` → desktop mat-table + mobile card list
  - Desktop columns: Flag ID (uid), Description, Group, Status (MatSlideToggle)
  - Mobile cards: uid as title, description/group as meta, toggle in card actions
  - SlideToggle: `[checked]="flag.enable" (change)="toggleFlag(flag)" aria-label="Toggle {{ flag.uid }}"`

- [x] 2.3 Create `frontend/src/app/features/admin/feature-flag-management/feature-flag-management.component.scss`:
  - Follow patterns from other admin components (page-header, table-container, mobile-card styles)
  - Minimal — rely on global table styles

### 3. Register the route

- [x] 3.1 In `frontend/src/app/app-routing.module.ts`, add to admin children (before the catch-all):
  ```typescript
  {
    path: 'feature-flags',
    loadComponent: () =>
      import('./features/admin/feature-flag-management/feature-flag-management.component')
        .then((m) => m.FeatureFlagManagementComponent),
  },
  ```

### 4. Add sidebar nav item

- [x] 4.1 In `frontend/src/app/core/layout/admin-sidebar/admin-sidebar.component.ts`, add to the "Configuration" group items:
  ```typescript
  { path: '/admin/feature-flags', icon: 'toggle_on', label: 'Feature Flags' },
  ```

### 5. Add system-settings card

- [x] 5.1 In `frontend/src/app/features/admin/system-settings/system-settings.component.ts`, add to `settingsCategories`:
  ```typescript
  {
    title: 'Feature Flags',
    description: 'Toggle FF4J feature flags for the system',
    icon: 'toggle_on',
    route: '/admin/feature-flags',
    class: 'feature-flags',
  },
  ```

### 6. Verification

- [x] 6.1 Run `npm run build` in the worktree frontend and confirm zero new errors
