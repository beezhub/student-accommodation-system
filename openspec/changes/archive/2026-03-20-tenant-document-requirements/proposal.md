## Why

`DocumentType` currently has no tenant scope — every tenant shares the same global set of document types and the same `isRequired` flag. This means a tenant admin cannot define which documents are required for their students; any change to `isRequired` affects all tenants simultaneously.

## What Changes

- Add `tenant_id` to `document_type`, following the same cascade model used by `system_setting` (platform defaults at `tenant_id = 0`, tenant overrides at their own `tenant_id`).
- Platform admins manage the platform-level document types (the defaults all tenants inherit).
- Tenant admins can create tenant-specific document types or override the `isRequired` flag on platform types for their tenant.
- All document type lookups (submission checks, student pack display, admin management) become tenant-scoped.
- The `typeName` uniqueness constraint changes from globally unique to unique within a `(tenant_id)` scope.
- The application submission document completeness check resolves required types against the **application's target tenant** (derived from the selected buildings), not the student's own TenantContext (which is always `0`).
- The completeness check matches documents to required types by **type name**, not by ID — this is necessary because a student uploads against platform type IDs while a tenant shadow row carries a different ID for the same named type.
- The application submission form surfaces tenant-specific requirements once buildings are selected; the standalone document upload page remains a tenant-agnostic portfolio manager (platform types only).

## Capabilities

### New Capabilities

- `tenant-document-type-management`: Tenant-scoped document type catalog — each tenant can define its own required document types, inheriting platform defaults and optionally overriding the `isRequired` flag.

### Modified Capabilities

- `application-document-submission`: The required document check at submission time must resolve required types from the current tenant's scoped view (not a global query).
- `student-document-pack`: The document pack display (student dashboard) must show tenant-scoped document types as the reference list.

## Impact

- **Backend schema**: `document_type` gains `tenant_id` FK; uniqueness constraint changes to `(tenant_id, type_name)`.
- **Backend services**: `DocumentTypeService` — merged view queries explicitly use `WHERE tenant_id IN (0, targetTenantId)`; the Hibernate `tenantFilter` is deliberately bypassed for document type lookups because student requests run with `TenantContext = 0` (filter is skipped for students by design).
- **Backend services**: `ApplicationCoreService` — required-type lookup passes `applicationTenantId` (derived from buildings) explicitly; switches from ID-based to name-based matching against the student's uploaded documents.
- **Backend API**: New `GET /api/document-type/required?tenantId={id}` query param to support the application form's pre-submission readiness panel.
- **Backend API**: `DocumentTypeController` — platform admin routes (`/api/platform/document-types`) for managing platform defaults; existing `/api/admin/document-types` becomes tenant-scoped.
- **Frontend**: Admin document type management page becomes tenant-aware; platform admin gets a separate view.
- **Migrations**: Flyway migration to add `tenant_id` column, assign existing rows to `tenant_id = 0` (platform), update unique constraint.

## Related Known Issue (Out of Scope)

`Document` entity carries `@Filter(name = "tenantFilter")` and `tenant_id` stamped at `0` by `TenantListener` for all student uploads. When a `TENANT_ADMIN` (filter active at their `tenant_id`) queries student documents via `findAllByStudent_AppUser_Id`, the filter hides all student documents (`tenant_id = 0`). This is a pre-existing latent bug unrelated to this change — admin document access works today only because the affected query paths have not been exercised under active filter conditions. A separate change should remove `@Filter` from `Document` and scope document access by `student_id` ownership instead of tenant.
