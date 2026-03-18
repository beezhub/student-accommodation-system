## ADDED Requirements

### Requirement: Dual-Model Tenancy Strategy
The system SHALL support two coexisting tenancy models: a tenant-scoped model for organisation-level users and a platform-level model for students and platform admins.

#### Scenario: Org user request activates tenant filter
- **WHEN** an HTTP request is processed for a user with a non-zero `tenantId` in `TenantContext`
- **THEN** `TenantFilterConfig` activates the Hibernate `tenantFilter` so all `TenantAware` entity queries are scoped to that tenant

#### Scenario: Platform-level request bypasses tenant filter
- **WHEN** an HTTP request is processed for a user with `tenantId = 0` (sentinel) or `null`
- **THEN** `TenantFilterConfig` does NOT activate the Hibernate `tenantFilter`, giving cross-tenant visibility

### Requirement: Tenant-Scoped Entity Annotation
Entities that belong to a specific institution SHALL implement the `TenantAware` interface and carry the `@Filter(name="tenantFilter", condition="tenant_id = :tenantId")` Hibernate annotation.

#### Scenario: TenantAware entity is automatically scoped
- **WHEN** a query is executed against a `TenantAware` entity while the tenant filter is active
- **THEN** Hibernate appends `WHERE tenant_id = :tenantId` to the query, restricting results to the current tenant

### Requirement: Student Platform-Level Scope
Students SHALL always use the platform-level sentinel (`tenantId = 0`) and SHALL NOT be scoped to a single institution tenant. Students are scoped to their own data by `student_id`/`user_id` at the service layer.

#### Scenario: Student data access is not tenant-filtered
- **WHEN** a student user makes a request
- **THEN** the Hibernate tenant filter is not activated, and student data is retrieved by ownership (student_id / user_id), not by tenant_id

### Requirement: Platform Admin Cross-Tenant Access
`PLATFORM_ADMIN` users SHALL always operate with the platform-level sentinel (`tenantId = 0`) and SHALL have cross-tenant visibility. No single-tenant impersonation mode is supported.

#### Scenario: Platform admin sees all tenant data
- **WHEN** a `PLATFORM_ADMIN` user makes a request
- **THEN** the Hibernate tenant filter is not activated and the admin has access to data across all tenants
