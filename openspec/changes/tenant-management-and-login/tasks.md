## 1. Backend — DTOs and Service

- [x] 1.1 Create `TenantRequestDto` (name: String, domain: String) with `@NotBlank` on name
- [x] 1.2 Create `TenantResponseDto` (id, name, domain, isActive, createdAt, updatedAt)
- [x] 1.3 Create `TenantStatusDto` (isActive: Boolean) for PATCH toggle endpoint
- [x] 1.4 Add `createTenant(TenantRequestDto)` method to `TenantService` — validates unique name, saves, returns `TenantResponseDto`
- [x] 1.5 Add `updateTenant(Long id, TenantRequestDto)` to `TenantService` — guards `id != 0`, validates unique name, saves, returns `TenantResponseDto`
- [x] 1.6 Add `toggleTenantStatus(Long id, TenantStatusDto)` to `TenantService` — guards `id != 0` on deactivate, saves, returns `TenantResponseDto`

## 2. Backend — Controller

- [x] 2.1 Add `POST /api/admin/tenants` endpoint to `TenantController` returning `201 Created`
- [x] 2.2 Add `PUT /api/admin/tenants/{id}` endpoint to `TenantController`
- [x] 2.3 Add `PATCH /api/admin/tenants/{id}/status` endpoint to `TenantController`
- [x] 2.4 Handle `DataIntegrityViolationException` for duplicate name → return `409 Conflict` in global exception handler

## 3. Backend — Validation for Admin User Creation

- [x] 3.1 Locate the user-creation service/controller that handles `ADMIN` role assignment
- [x] 3.2 Add validation: if role is `ADMIN`, `tenantId` must be provided and `!= 0`, returning `400` with message "Tenant admins must be scoped to a specific institution tenant"

## 4. Frontend — TenantService Update

- [x] 4.1 Add `createTenant(dto)` method to `frontend/src/app/core/services/tenant.service.ts`
- [x] 4.2 Add `updateTenant(id, dto)` method to `TenantService`
- [x] 4.3 Add `toggleTenantStatus(id, isActive)` method to `TenantService`

## 5. Frontend — Tenant Management Module

- [x] 5.1 Create `frontend/src/app/features/admin/tenant-management/` directory with `tenant-management.component.ts`, `.html`, `.scss`
- [x] 5.2 Implement tenant list table (columns: Name, Domain, Status chip, Created At, Actions)
- [x] 5.3 Use `EmptyStateComponent` when the tenant list is empty
- [x] 5.4 Create `tenant-form-dialog/tenant-form-dialog.component.ts` with name (required) and domain (optional) fields
- [x] 5.5 Wire Create button → open dialog → call `createTenant` → refresh table → success snackbar
- [x] 5.6 Wire Edit button → open dialog pre-populated → call `updateTenant` → refresh table → success snackbar; disable Edit for `id=0`
- [x] 5.7 Wire toggle action → call `toggleTenantStatus` → update row → success snackbar; disable for `id=0`
- [x] 5.8 Show inline error in dialog on 409 Conflict response

## 6. Frontend — Routing and Sidebar

- [x] 6.1 Add route `{ path: 'tenants', component: TenantManagementComponent }` under the admin route in `app-routing.module.ts`
- [x] 6.2 Add `{ path: '/admin/tenants', icon: 'corporate_fare', label: 'Tenants' }` to the Administration nav group in `admin-sidebar.component.ts`
- [x] 6.3 Add role-guard or `*ngIf` so the Tenants sidebar item only renders for `PLATFORM_ADMIN` users

## 7. Frontend — User Creation Dialog Update

- [x] 7.1 In `create-user-dialog.component.ts`, extend tenant selector to also appear when role is `ADMIN` (currently only `BUILDING_OWNER`)
- [x] 7.2 Ensure platform tenant (id=0) is excluded from the tenant dropdown for both `ADMIN` and `BUILDING_OWNER` roles
- [x] 7.3 Verify form validation prevents submission when tenant is not selected for `ADMIN` role

## 8. Testing

- [x] 8.1 Write unit tests for `TenantService` create/update/toggleStatus methods
- [x] 8.2 Verify `POST /api/admin/tenants` returns 403 for non-PLATFORM_ADMIN
- [x] 8.3 Verify platform tenant guard (id=0) returns 400 on PUT and PATCH
- [x] 8.4 Verify 409 on duplicate tenant name
