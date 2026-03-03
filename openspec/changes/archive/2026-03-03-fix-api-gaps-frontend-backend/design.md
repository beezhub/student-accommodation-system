## Context

The current `gemini` worktree exhibits API integration mismatches between the Angular frontend and the Spring Boot backend. Several endpoints exist only on one side of the stack, causing either unused backend code, or frontend components that would fail when invoked (such as fetching feature flags or missing maps config). This design addresses these gaps to ensure 100% parity and to remove duplicated logic across controllers.

## Goals / Non-Goals

**Goals:**
- Implement all missing endpoint requirements listed in the specs.
- Fix any malformed HTTP paths in the frontend services.
- Consolidate staged allocation endpoints into a single controller to reduce duplication.

**Non-Goals:**
- Rewriting the frontend framework or restructuring the backend architecture.
- Altering the underlying schema of any existing functional endpoints.

## Decisions

**Decision 4: Exposing Tenant Awareness in the Frontend**
- **Rationale**: Since multi-tenancy is entirely implicit right now, Platform Admins (who operate on `TenantContext.PLATFORM_TENANT_ID = 0L`) have no way to scope their actions. We will expose `tenantId` in the frontend `AuthService` state (derived from decoding the JWT token payload). The UI will use this to determine if a "Tenant Selector" should be rendered (i.e., only if `tenantId === 0` and the user is an admin) and passed via a query parameter or custom header for cross-tenant operations where the backend supports it (such as user creation).
- **Alternative Considered**: Building a separate admin portal just for the platform admin. Rejected because it duplicates massive amounts of dashboard logic.


**Decision 1: Consolidating Staged Allocation**
- **Rationale**: `RoomAllocationController` currently duplicates paths (like `/admin/allocations/staged/{stagedAllocationId}/confirm`) that are properly the responsibility of `StagedAllocationController`. We will remove these from `RoomAllocationController` to enforce a single source of truth for batch/staged allocations.
- **Alternative Considered**: Keeping both and proxying one to the other. Rejected as it increases maintenance overhead and confusion.

**Decision 2: Maps Config Location**
- **Rationale**: The frontend `building.service.ts` expects `/api/buildings/maps-config`. Rather than adding a new controller, we will add this single endpoint directly into `BuildingController` which already handles the `/api/buildings` domain. This config can return properties like default zoom and center coordinates.

**Decision 3: Feature Flags Service**
- **Rationale**: The backend uses FF4J (`FeatureController`), so the frontend needs an admin UI. We will create a `feature-flags.service.ts` under `admin` services and corresponding components in `admin-dashboard` to toggle these flags.

## Risks / Trade-offs

- **Risk: Breaking Existing Frontend Calls**
  - **Mitigation**: Fixing `application.service.ts` to call `/api/applications/status/{status}` could break logic if the backend previously silently allowed something else, or if the code in production relied on the error state. However, correcting it to the documented backend spec is the safest path forward.
- **Risk: Missing test coverage**
  - **Mitigation**: All new endpoint additions in backend controllers (Activity Logs, Buildings) must be accompanied by relevant test assertions.
