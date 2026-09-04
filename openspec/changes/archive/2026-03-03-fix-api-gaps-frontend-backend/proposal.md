## Why

During a systematic analysis of the frontend and backend implementations in the `gemini` worktree, several API integration gaps were discovered. There are mismatches between what the frontend expects and what the backend provides (e.g., missing endpoints, incorrect paths), as well as unimplemented UI features and backend endpoints. Addressing these gaps ensures full functionality and consistency across the application stack.

## What Changes

- Introduce multi-tenancy context in the frontend to align with the backend TenantContext.

- Implement missing `GET /api/admin/activity-logs/{id}` endpoint in the backend `ActivityLogController`.
- Implement missing `GET /api/buildings/maps-config` endpoint in the backend `BuildingController`.
- Fix the frontend `application.service.ts` to correctly call `GET /api/applications/status/{status}` instead of `GET /api/applications/{status}` (**BREAKING** if existing frontend logic relied on the buggy route).
- Add support in the frontend `student.service.ts` for the existing `GET /`, `GET /search`, and `DELETE /{id}` endpoints in `StudentController`.
- Add support in the frontend `application.service.ts` for the existing `GET /student/{studentId}` and `DELETE /{applicationId}` endpoints in `ApplicationController`.
- Create a new UI/service for the `FeatureController` (`/api/admin/features`) to manage FF4j feature flags from the admin panel.
- Refactor and clean up `RoomAllocationController` to remove duplicate staged allocation endpoints that are already provided by `StagedAllocationController`.

## Capabilities

### New Capabilities
- rontend-multi-tenancy: Basic awareness and context handling of multi-tenancy in the frontend, reflecting the backend model.

### New Capabilities
- `feature-flags-ui`: Admin UI capability for managing system feature toggles (FF4J).

### Modified Capabilities
- `api-integration-fixes`: Various bug fixes and parity updates between frontend API clients and backend controllers.

## Impact

- **Backend**: `ActivityLogController`, `BuildingController`, `RoomAllocationController`.
- **Frontend**: `application.service.ts`, `student.service.ts`, new components for feature flags under `admin` module.
- **System**: Improved stability and complete end-to-end integration without broken or unused endpoints.
