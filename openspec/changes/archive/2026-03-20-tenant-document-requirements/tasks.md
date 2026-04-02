## 1. Database Migration

- [x] 1.1 Write Flyway migration to add `tenant_id BIGINT NOT NULL DEFAULT 0` to `document_type` with FK to `tenant(id)`
- [x] 1.2 Drop the existing `UNIQUE(type_name)` constraint and add `UNIQUE(tenant_id, type_name)` in the same migration
- [x] 1.3 Verify migration applies cleanly against the local DB (all existing rows get `tenant_id = 0`)

## 2. Backend Entity & Repository

- [x] 2.1 Add `tenant_id` column and `tenant` ManyToOne (insertable=false) to the `DocumentType` entity, following the dual-column pattern
- [x] 2.2 Do NOT add `@Filter` to `DocumentType` — the merged view bypasses the Hibernate filter intentionally (student requests skip the filter; admin requests need the full merged view regardless)
- [x] 2.3 Add `findAllByTenantIdIn(List<Long> tenantIds)` and `findByIsRequiredTrueAndTenantIdIn(List<Long> tenantIds)` to `DocumentTypeRepository`
- [x] 2.4 Add `findDocumentTypeNamesByStudentId(@Param("studentId") Long studentId)` to `DocumentRepository` returning `Set<String>` (replaces the current ID-based query for the completeness check)

## 3. Merged View Query (Service Layer)

- [x] 3.1 Add `getEffectiveDocumentTypes(Long tenantId)` to `DocumentTypeService` — queries `tenant_id IN (0, tenantId)` directly (bypasses Hibernate filter), deduplicates in-memory by `type_name` (tenant row wins)
- [x] 3.2 Add `getEffectiveRequiredTypeNames(Long tenantId)` returning `Set<String>` — filters the merged view to `isRequired = true` and extracts `type_name`s for use in the name-based completeness check
- [x] 3.3 Update `getAllDocumentTypes()` (admin and student list endpoints) to call `getEffectiveDocumentTypes(tenantId)` with explicit tenant param, not via TenantContext
- [x] 3.3a Update `getAllDocumentTypes(Pageable)` and `searchDocumentTypes(name, isRequired, pageable)` to also use the merged view (in-memory filter + `PageImpl` paging) — plain `findAll` was bypassing the merge
- [x] 3.4 Update `getRequiredDocumentTypes()` to accept optional `tenantId` param; default to `0L` (platform only) when not supplied

## 4. Tenant Admin Mutation Guards

- [x] 4.1 In `DocumentTypeService.updateDocumentType()`, throw `403 Forbidden` if the resolved document type has `tenant_id = 0` and the caller is not `PLATFORM_ADMIN`
- [x] 4.2 In `DocumentTypeService.deleteDocumentType()`, apply the same `tenant_id = 0` guard
- [x] 4.3 In `DocumentTypeService.createDocumentType()`, set `tenantId` from `TenantContext` if the caller is `TENANT_ADMIN`; set `0L` if `PLATFORM_ADMIN`

## 5. Platform Admin API Route

- [x] 5.1 Create `PlatformDocumentTypeController` at `/api/platform/document-types` with `POST`, `PUT/{id}`, `DELETE/{id}` — `PLATFORM_ADMIN` only
- [x] 5.2 Reuse `DocumentTypeService` methods; the service sets `tenant_id = 0` when the caller is `PLATFORM_ADMIN`
- [x] 5.3 Ensure existing `/api/document-type` endpoints remain accessible and serve the merged view for the current tenant

## 6. All Callsites: Required Document Type Resolution

There are four places that call `findByIsRequiredTrue()` — each must be updated to use the application's tenant and name-based matching:

- [x] 6.1 **`ApplicationCoreService` (submission gate)**: Call `getEffectiveRequiredTypeNames(applicationTenantId)` using `applicationTenantId` already derived at line 129 from buildings. Switch from ID-based (`findDocumentTypeIdsByStudentId`) to name-based (`findDocumentTypeNamesByStudentId`) matching.
- [x] 6.2 **`ApplicationDocumentService.getRequiredDocumentsChecklist()`** (admin checklist): Use `getEffectiveDocumentTypes(application.getTenantId())` — `application` is already fetched at line 60. Switch the per-type document match from `.getId().equals(docType.getId())` to `.getTypeName().equals(docType.getTypeName())` (name-based).
- [x] 6.3 **`ApplicationDocumentCoordinator.tryAllocateOrQueue()`** (allocation gate): Use `getEffectiveRequiredTypeNames(application.getTenantId())` — `application` is already received as a parameter. Replace the `DocumentType::getIsRequired` stream filter with a name-set lookup: approved document names ∩ effective required names. This prevents the platform flag from overriding a tenant shadow.
- [x] 6.4 **`ApplicationSearchService.enrichApplicationDetails()`** (display count): All applications in a given admin list belong to the same tenant (Hibernate filter is active for admin requests). Replace the global `findByIsRequiredTrue().size()` with `getEffectiveRequiredTypeNames(TenantContext.getTenantId()).size()` — one call for the whole batch.
- [x] 6.5 Verify missing-type error messages still return human-readable type names in all callsites

## 7. Backend Tests

- [x] 7.1 Update existing `DocumentTypeService` unit tests to seed `tenant_id` on fixture document types
- [x] 7.2 Add unit tests for `getEffectiveDocumentTypes` shadow semantics (tenant row wins over platform row with same `type_name`)
- [x] 7.3 Add unit tests for `getEffectiveRequiredTypeNames` (required flag resolved from shadow; result is a `Set<String>`)
- [x] 7.4 Add mutation guard tests: `TENANT_ADMIN` cannot update/delete a `tenant_id = 0` type
- [x] 7.5 Update `ApplicationDocumentCoordinatorTest` and `ApplicationServiceIntegrationTest` — completeness check now name-based, uses `applicationTenantId` not `TenantContext`
- [x] 7.6 Update `ApplicationDocumentServiceTest` — checklist now uses effective tenant required types with name-based matching
- [x] 7.7 Add integration test: student with platform-type document satisfies a tenant shadow of the same name (name-based match succeeds)
- [x] 7.8 Add integration test: student of Tenant A is not blocked by Tenant B's custom required type
- [x] 7.9 Add integration test: student applying to Tenant A is blocked when Tenant A has a custom required type and student has not uploaded it
- [x] 7.10 Add unit test: `tryAllocateOrQueue` does NOT trigger allocation when a tenant shadow marks a platform-required type as required and the document is not yet approved (allocation gate uses tenant effective view)

## 8. Frontend — Document Type Management

- [x] 8.1 Update the admin document type management page to call `GET /api/admin/document-types` (merged view) — no URL change needed
- [x] 8.2 Add a "Platform default" badge to document types that originate from `tenant_id = 0` in the API response (`DocumentTypeResponse` should include a `isPlatformDefault: boolean` field)
- [x] 8.3 Disable Edit/Delete actions in the UI for platform-default types when the logged-in user is a `TENANT_ADMIN`
- [x] 8.4 Add a "Shadow / Override" action for `TENANT_ADMIN` on platform-default types that pre-fills the create form with the platform type's name

## 9. Frontend — Tenant-Aware Required Types Endpoint

- [x] 9.1 Add `?tenantId=` query param to `GET /api/document-type/required` backend endpoint; returns platform defaults when param is absent
- [x] 9.2 Update `DocumentTypeController` to accept and pass the optional `tenantId` param through to `DocumentTypeService`

## 10. Frontend — Application Submission Form

- [x] 10.1 Once buildings are selected on the application form, derive `applicationTenantId` from the first selected building's tenant (already available in the buildings list response)
- [x] 10.2 Call `GET /api/document-type/required?tenantId={applicationTenantId}` to fetch the per-tenant required types for the readiness panel
- [x] 10.3 Render the per-tenant requirements panel — list required types with present/absent status, link absent types to the upload section with the specific `documentTypeId` pre-selected
- [x] 10.4 Disable the submit button when any tenant-required type is absent (same logic as before, now using the tenant-scoped list)

## 11. Frontend — Student Dashboard Document Pack

- [x] 11.1 Keep `/dashboard/documents` as platform-types-only portfolio — no tenant requirement panels here
- [x] 11.2 On the applications list page, add a per-application document status summary showing each active application's target tenant required types and the student's upload status against them
- [x] 11.3 Upload links in the per-application summary pre-select the correct `documentTypeId` (including tenant-specific type IDs) so students can satisfy tenant-only requirements directly

## 12. Documentation & Cleanup

- [x] 12.1 Add `isPlatformDefault` field to `DocumentTypeResponse` DTO and `DocumentTypeMapper`
- [x] 12.2 Update Swagger `@Operation` descriptions on document type endpoints to reflect tenant scoping and the `?tenantId=` param
- [x] 12.3 Verify `./gradlew test` passes with no regressions
- [x] 12.4 Open a separate change proposal for the `Document @Filter` latent bug — `Document` entity should not use `tenantFilter` since student-owned documents are always `tenant_id = 0` and invisible to admin queries under the active filter
