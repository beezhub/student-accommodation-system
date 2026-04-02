# Proposal: Fix Document @Filter Latent Bug

## Problem

The `Document` entity currently carries `@Filter(name = "tenantFilter")` inherited from the generic multi-tenancy setup. However, all `Document` rows are stored with `tenant_id = 0` (the platform tenant) because documents belong to students, not to tenants — students are also platform-scoped (`tenant_id = 0`).

This creates a latent bug:

- When a `TENANT_ADMIN` makes a request, `TenantFilterAspect` activates the Hibernate filter for their tenant (e.g., `tenant_id = 42`).
- Any query on `Document` under this filter returns **zero rows** because all documents have `tenant_id = 0`.
- Admin document review pages, document verification workflows, and cross-document lookups will silently return empty results for tenant admins.

## Root Cause

The `@Filter` on `Document` was added blanket-style when the multi-tenancy filter was introduced. Documents are student-owned and therefore platform-scoped — they are not tenant-owned entities and should not be filtered by tenant.

## Proposed Fix

1. Remove `@Filter(name = "tenantFilter")` from the `Document` entity.
2. Access control for documents at the admin level should instead be enforced via the `Application` relationship: a `TENANT_ADMIN` can only access documents for students whose applications belong to their tenant.
3. Verify no admin document endpoints break after removing the filter.

## Impact

- **Low risk**: Removing `@Filter` from `Document` is additive — it can only return more rows, never fewer. The existing access-control logic already ensures tenant admins see only relevant documents through the application ownership chain.
- **Affected endpoints**: `GET /api/admin/documents`, document verification workflows, `ApplicationDocumentService.getRequiredDocumentsChecklist()`.
- **Tests needed**: Integration test confirming a `TENANT_ADMIN` can see a student's documents when the student has an application to their tenant.

## Status

Identified during `tenant-document-requirements` implementation (task 12.4). Not blocking the current change — documents still work end-to-end for students since student requests run at `TenantContext = 0` (platform). The bug only manifests for admin-side document queries under an active tenant filter.
