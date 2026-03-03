## 1. Backend Controller Updates

- [x] 1.1 Add `GET /api/admin/activity-logs/{id}` endpoint to `ActivityLogController`.
- [x] 1.2 Create `GET /api/buildings/maps-config` endpoint in `BuildingController`.
- [x] 1.3 Remove redundant staged allocation endpoints from `RoomAllocationController` to consolidate functionality in `StagedAllocationController`.

## 2. Frontend Service Fixes

- [x] 2.1 Update `application.service.ts` to call `GET /api/applications/status/{status}` instead of `GET /api/applications/{status}`.
- [x] 2.2 Add missing application methods to `application.service.ts` (`GET /student/{studentId}` and `DELETE /{applicationId}`).
- [x] 2.3 Add missing student methods to `student.service.ts` (`GET /`, `GET /search`, `DELETE /{id}`).

## 4. Multi-Tenancy

- [x] 4.1 Update frontend `User` model and `AuthService` to extract `tenantId` from JWT claims on login.
- [x] 4.2 Create a `TenantService` in frontend to fetch available tenants for Platform Admins.
- [x] 4.3 Update `UserManagementComponent` and `CreateUserDialogComponent` to include a Tenant dropdown for Platform Admins (sentinel `tenantId === 0`).
- [x] 4.4 Update `CreateUserRequest` and backend `UserManagementController` to accept and process an optional `tenantId` field when creating users.
