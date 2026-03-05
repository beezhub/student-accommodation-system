## Why

The backend exposes FF4J feature flags via `/api/admin/features` but there is no frontend UI to view or toggle them. Admins currently have no way to control feature flags without direct API calls. The existing system-settings page has a single hardcoded toggle (auto-allocation) that would be better expressed through the generic feature flags interface.

## What Changes

- New `FeatureFlagService` in `core/services/` — wraps GET `/api/admin/features` and POST `/api/admin/features/{id}/enable|disable`
- New `FeatureFlagManagementComponent` in `features/admin/feature-flag-management/` — table of flags with toggle switches
- New admin route `admin/feature-flags` registered in `app-routing.module.ts`
- New nav item "Feature Flags" added to the Configuration group in `admin-sidebar.component.ts`
- Link card added to `system-settings.component.html` pointing to the new route

## Capabilities

### New Capabilities
- `feature-flags-management-ui`: Admin interface for viewing and toggling FF4J feature flags via the backend API

### Modified Capabilities
<!-- none -->

## Impact

- 4 new files: `feature-flag.service.ts`, `feature-flag-management.component.ts/html/scss`
- 3 modified files: `app-routing.module.ts`, `admin-sidebar.component.ts`, `system-settings.component.html`
- No backend changes required
