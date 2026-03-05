## Context

FF4J exposes feature flags via REST. The existing codebase pattern is: a `@Injectable({ providedIn: 'root' })` service that injects `HttpClient` and uses `environment.apiUrl`. Components are standalone with `ChangeDetectionStrategy.OnPush`.

## Goals / Non-Goals

**Goals:**
- List all feature flags returned by `/api/admin/features`
- Toggle each flag on/off with optimistic UI update + revert on error
- Follow existing admin component patterns (table, mobile cards, MatSlideToggle)

**Non-Goals:**
- Deep FF4J group management or property editing
- Role gating beyond the existing `AdminGuard`

## Decisions

### D1: FeatureFlag model
FF4J GET `/api/admin/features` returns a map `{ [id: string]: { uid, enable, description, group, permissions, ... } }`. The service maps this to `FeatureFlag[]`:
```typescript
export interface FeatureFlag {
  uid: string;
  enable: boolean;
  description?: string;
  group?: string;
}
```

### D2: Toggle via enable/disable endpoints
`POST /api/admin/features/{featureId}/enable` and `POST /api/admin/features/{featureId}/disable` (no body required). Optimistic update: flip `flag.enable`, revert on error.

### D3: Component layout
- Page header matching other admin pages
- Desktop: `mat-table` with columns: name, description, group, status toggle
- Mobile: card list (same pattern as other admin components)
- Use `MatSlideToggle` for the toggle (matches system-settings)
- Skeleton loader while loading, `EmptyStateComponent` when empty

### D4: Nav placement
Add under the "Configuration" group in the sidebar (between System Settings and Allocation Rules makes sense). Icon: `toggle_on`.

### D5: System settings link card
Add a new card in the `settingsCategories` array pointing to `/admin/feature-flags` using icon `toggle_on` and class `feature-flags`.
