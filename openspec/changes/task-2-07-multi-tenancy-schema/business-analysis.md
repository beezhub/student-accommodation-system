# Senior Business Analyst - Requirements Analysis

You are a **Senior Business Analyst** with 10+ years of experience in enterprise software development, specializing in university accommodation management systems.

## Your Identity & Primary Focus

**PRIMARY FOCUS: Clarity** — Above all else, every requirement you produce must be unambiguous, complete, and immediately actionable by downstream teams.

### Your Responsibilities

| # | Responsibility | What This Means |
|---|---------------|----------------|
| R1 | **Gather and refine requirements** | Extract requirements from code, configuration, schema, and documentation. Refine through clarifying questions until precise and unambiguous. |
| R2 | **Document business processes and workflows** | Map every end-to-end workflow with entry/exit conditions, decision points, and exception paths. |
| R3 | **Define clear acceptance criteria** | Every functional requirement must have testable acceptance criteria. If you cannot define a test for it, the requirement is not clear enough. |
| R4 | **Identify edge cases** | For every business rule, explicitly document what happens at boundaries — null inputs, maximum values, concurrent operations, expired tokens, full rooms, 3rd decline, etc. |
| R5 | **Map integrations** | Document every integration point (APIs, database, MinIO, SMTP, Google Maps, FF4J) with contracts, data flows, error handling, and failure modes. |
| R6 | **Create functional specifications** | Produce specifications detailed enough that a developer could implement from the document alone without reading source code. |

### Your Accountabilities (Hard Gates)

| # | Accountability | Verification |
|---|---------------|-------------|
| A1 | **No ambiguous requirements enter development** | Every requirement uses precise language ("shall"/"shall not"), has measurable acceptance criteria, and has been validated with the user where business intent is unclear. |
| A2 | **All requirements meet Definition of Ready** | Each requirement has: unique ID, clear description, acceptance criteria, priority, source traceability, identified edge cases, and dependencies noted. If any are missing, the requirement is NOT ready. |
| A3 | **Business rules fully understood before documented** | You must ask the user to confirm any business rule where the code's intent could be interpreted differently. Do not document a business rule you do not fully understand — ask first. |
| A4 | **Edge cases explicitly addressed** | For every state transition, validation rule, and business constraint, document the happy path AND what happens when things go wrong (invalid input, expired data, capacity exceeded, concurrent access, missing dependencies). |

### Impact on Downstream Teams

Your output directly determines quality of the entire development lifecycle:
- **Reduces rework**: Clear requirements mean developers build the right thing the first time
- **Reduces back-and-forth**: Complete requirements with acceptance criteria and edge cases prevent "what should happen when...?" questions
- **Protects from scope creep**: Well-defined boundaries prevent developers from handling unspecified cases
- **Enables accurate estimation**: Architects and developers can only estimate when they know exactly what to build
- **Accelerates QA**: Testable acceptance criteria translate directly into test cases

**IMPORTANT BEHAVIORS:**
- **ASK when ambiguous** — If code implies behavior but doesn't make it explicit, **ask the user** before documenting
- **Flag uncertainties** — Use `[INFERRED]` for assumptions, `[CONFLICT]` for contradictions, `[GAP]` for missing functionality
- **Validate business decisions** — Confirm domain-specific rules, edge cases, or policy decisions with the user
- **Do not invent requirements** — Only document what is evidenced by code, configuration, schema, or documentation

---

## System Context

This is a **Student Accommodation System** built with:
- **Backend**: Spring Boot 3.5.7 + Java 17 + PostgreSQL 16.2
- **Frontend**: Angular 21 + Material Design
- **Storage**: MinIO for document management
- **Auth**: JWT tokens with Spring Security

The system manages student housing applications, room allocations, documents, and notifications.

**Key User Roles:**
- **Students**: Submit applications, upload documents, manage bookings
- **Admins**: Review applications, allocate rooms, manage buildings and system settings
- **Landlords** (Planned): Building owners with scoped access to their properties

---

## 1. Problem Statement

**What problem are we solving?**

The Student Accommodation System currently operates as a **single-tenant application** where all data belongs to a single accommodation provider. However, the business model requires the platform to operate as a **multi-tenant SaaS solution** serving multiple privately-owned accommodation providers (landlords) who gain accreditation from various educational institutions.

**Current Pain Points:**
1. **No tenant isolation** — All buildings, rooms, applications, and allocations exist in a shared namespace with no ownership boundaries
2. **No data segregation** — Building owner A can potentially access building owner B's data (critical security risk)
3. **No per-tenant configuration** — Cannot customize rules (offer expiry days, decline limits, required documents) per landlord or institution
4. **Deployment inefficiency** — Each new landlord requires a separate deployment, increasing infrastructure costs and operational burden
5. **No cross-tenant reporting** — Platform administrators cannot view aggregated metrics across all tenants

**Stakeholders Affected:**
- **Landlords/Building Owners** (Primary) — Need data isolation and confidence their tenant data is secure
- **Students** — Indirectly affected; must only see accommodations from their institution/landlord
- **Platform Administrators** — Need cross-tenant management capabilities and oversight
- **University Management** — Require assurance that multi-institution data is properly segregated

**Background Context:**

This change implements the foundational **database schema** for shared-schema multi-tenancy. It is part of a larger multi-tenancy implementation roadmap:

1. **Phase 1 (This Task - TASK-2.07)**: Database schema foundation
   - Create `tenant` table
   - Add `tenant_id` foreign keys to all tenant-scoped tables
   - Migrate existing data to a default tenant

2. **Phase 2 (TASK-2.08)**: Application-layer tenant isolation
   - `TenantContext` for thread-local tenant tracking
   - JWT integration to include `tenantId` claim
   - Hibernate filters or JPA Specifications for automatic tenant filtering
   - Admin endpoint scoping

This task addresses **ADR-002** (Shared-Schema Multi-Tenancy) and **RISK-007** (Cross-tenant data leakage).

**How does this fit into the overall accommodation workflow?**

Multi-tenancy is a **cross-cutting concern** that affects every part of the system:
- **Registration/Authentication**: Users must be scoped to a tenant
- **Application Submission**: Applications belong to a tenant
- **Room Allocation**: Allocations only match students with rooms from the same tenant
- **Document Management**: Documents are tenant-scoped
- **Notifications**: Email templates may vary per tenant
- **System Settings**: Configuration (offer expiry, decline limits) may be tenant-specific

---

## 2. Stakeholder Analysis

**Primary Stakeholders:**

| Role | Needs | Success Criteria |
|------|-------|------------------|
| **Landlord/Building Owner** | Complete data isolation from other landlords. Confidence that their students, applications, and allocations are private. | Zero cross-tenant data leakage. Ability to configure tenant-specific rules. |
| **Platform Administrator (Super-Admin)** | Ability to manage all tenants from a single deployment. Cross-tenant reporting and analytics. Tenant provisioning and configuration. | Can create, configure, and monitor tenants. Can view aggregated platform metrics. |
| **Database Administrator** | Clear, maintainable schema with proper indexing. Safe migration path for existing data. | Migrations execute without downtime. Query performance maintained or improved. |
| **Application Developer** | Clear architectural boundaries. Automatic tenant filtering to prevent accidental leakage. | Tenant isolation enforced at framework level (Hibernate filters). No manual tenant filtering in every query. |
| **Security/Compliance Team** | Audit trail of tenant access. Assurance that tenant boundaries cannot be bypassed. | No SQL injection or ORM bypass paths for cross-tenant access. Audit logs capture tenant context. |

**Secondary Stakeholders:**
- **Students** — Indirectly benefit from platform scalability and multi-institution support
- **University Administrators** — Benefit from multi-institution reporting and better landlord oversight
- **Support Staff** — Need clear tenant context when debugging issues or responding to support tickets

---

## 3. Functional Requirements

### 3.1 Core Functionality

**FR-1: Tenant Table Creation**
- **Priority**: Must Have
- **Description**: The system shall create a `tenant` table to store metadata for each accommodation provider (landlord).
- **Business Rules**:
  - Tenant names must be unique across the platform
  - Tenant domains (optional) are used for future subdomain-based routing (e.g., `landlord-a.platform.com`)
  - Each tenant has a JSON configuration field for tenant-specific settings overrides
  - Tenants can be activated/deactivated without data deletion
- **Validation Rules**:
  - `name` field: NOT NULL, VARCHAR(100), UNIQUE
  - `domain` field: NULLABLE, VARCHAR(255), UNIQUE when not null
  - `config` field: JSONB, defaults to `{}`
  - `is_active` field: BOOLEAN, defaults to TRUE

**FR-2: Tenant Foreign Key Addition**
- **Priority**: Must Have
- **Description**: The system shall add a `tenant_id` foreign key column to all tenant-scoped entities to establish ownership boundaries.
- **Business Rules**:
  - Only tables representing tenant-owned data receive `tenant_id` (not reference data shared across tenants)
  - Foreign key constraints shall enforce referential integrity
  - Cascading deletes are NOT permitted (tenant deletion must be explicit and controlled)
- **Validation Rules**:
  - `tenant_id` column: BIGINT, FK to `tenant(id)`, ON DELETE RESTRICT
  - NOT NULL constraint added AFTER data backfill to avoid migration failures

**FR-3: Default Tenant Creation and Data Migration**
- **Priority**: Must Have
- **Description**: The system shall create a "Default Tenant" and backfill all existing records with this tenant ID to maintain continuity.
- **Business Rules**:
  - Default tenant is created with name "Default Tenant" and `id = 1`
  - All existing records in tenant-scoped tables are updated to `tenant_id = 1`
  - Backfill must be idempotent (safe to re-run if migration fails partway)
- **Validation Rules**:
  - Backfill SQL uses `UPDATE ... WHERE tenant_id IS NULL` to allow safe re-execution
  - Verify row counts before/after backfill match expected values

**FR-4: Tenant-Scoped Table Identification**
- **Priority**: Must Have
- **Description**: The system shall add `tenant_id` to the following 14 tables (tenant-scoped entities):
  - `app_user` — Admin users are scoped to tenants
  - `building` — Buildings are owned by landlords (also has `institution_id` FK for shared reference)
  - `room` — Rooms inherit tenant from building (but have own `tenant_id` for query performance)
  - `application` — Applications belong to students registered under a tenant
  - `room_allocation` — Allocations must match student tenant with room tenant
  - `staged_allocation` — Staged allocations are tenant-scoped
  - `document` — Documents uploaded by tenant-scoped students
  - `notification` — Notifications may have tenant-specific templates
  - `user_notification` — User notification records are tenant-scoped
  - `system_setting` — Settings may have tenant-specific overrides
  - `allocation_rule` — Allocation rules may be tenant-specific
  - `waiting_list_entry` — Waitlist is per-tenant
  - `activity_log` — Audit logs are tenant-scoped
  - `academic_year` — Academic years may be tenant-specific (different calendars per institution)
- **Business Rules**:
  - Tables NOT receiving `tenant_id`:
    - `student` — Inherits tenant from application
    - `institution` — **SHARED reference data** (multiple tenants can serve the same institution)
    - `year_of_study` — **SHARED reference data** (tied to institutions, not tenants)
    - `accommodation_type` — Shared reference data
    - `building_image`, `room_attributes`, `building_attributes` — Inherit tenant from parent entities
  - **Rationale for institution/year_of_study**: Multiple landlords (tenants) can provide housing at the same institution. This is a **many-to-many relationship** via `building` table (building has both `tenant_id` and `institution_id`).
- **Validation Rules**:
  - Each listed table receives exactly one `tenant_id BIGINT` column
  - Each `tenant_id` column has a foreign key constraint to `tenant(id)`
  - Each `tenant_id` column has an index `idx_{table}_tenant_id`

**FR-5: Indexing for Query Performance**
- **Priority**: Must Have
- **Description**: The system shall create indexes on all `tenant_id` columns to ensure query performance does not degrade with multi-tenancy.
- **Business Rules**:
  - Every `tenant_id` column receives a single-column B-tree index
  - Composite indexes with `tenant_id` as the leading column are added where tenant-scoped queries commonly filter on additional columns
- **Validation Rules**:
  - Index naming convention: `idx_{table}_tenant_id` for single-column indexes
  - Index exists before NOT NULL constraint is added

### 3.2 User Workflows

**Workflow 1: Platform Administrator Creates New Tenant**
```
Platform Admin Role:
1. Admin navigates to "Tenant Management" (future UI)
2. Admin clicks "Add New Tenant"
3. Admin fills form: Tenant Name, Domain (optional), Configuration JSON
4. System validates uniqueness of name and domain
5. System inserts new tenant record with is_active=true
6. System displays success message with tenant ID
7. Admin can now assign buildings to this tenant
```

**Workflow 2: Data Migration During Deployment**
```
Database Administrator Role:
1. DBA reviews migration plan and SQL scripts
2. DBA runs Flyway migrations in test environment first
3. Migration creates `tenant` table
4. Migration adds `tenant_id` columns (nullable initially)
5. Migration inserts "Default Tenant"
6. Migration backfills all `tenant_id` columns with default tenant ID
7. DBA verifies row counts match before/after backfill
8. Migration adds NOT NULL constraints on `tenant_id`
9. Migration adds indexes on `tenant_id`
10. DBA confirms zero errors, promotes to production
```

**Workflow 3: Tenant Deactivation**
```
Platform Admin Role (Future):
1. Admin identifies tenant to deactivate (e.g., landlord contract ended)
2. Admin sets `is_active = false` on tenant record
3. System prevents new applications for inactive tenant
4. Existing allocations remain unchanged (historical data)
5. Admin can reactivate tenant by setting `is_active = true`
```

---

## 4. User Stories with Acceptance Criteria

### Story 1: Create Tenant Table

**As a** Database Administrator
**I want to** create a `tenant` table
**So that** the platform can store metadata for multiple accommodation providers

**Acceptance Criteria:**

```gherkin
Given the database does not have a `tenant` table
When the Flyway migration V{YYYY_MM_DD_HHMM}__create_tenant_table.sql is executed
Then a `tenant` table is created with columns:
  - id BIGSERIAL PRIMARY KEY
  - name VARCHAR(100) NOT NULL UNIQUE
  - domain VARCHAR(255) UNIQUE (nullable)
  - config JSONB DEFAULT '{}'
  - is_active BOOLEAN DEFAULT true
  - created_at TIMESTAMP DEFAULT NOW()
  - updated_at TIMESTAMP DEFAULT NOW()
And the migration completes without errors
And the table can be queried successfully
```

**Edge Cases:**
- **EC-1**: Migration runs twice (idempotency) → `CREATE TABLE IF NOT EXISTS` guard prevents error
- **EC-2**: Tenant name exceeds 100 characters → Database constraint violation, transaction rolls back
- **EC-3**: Duplicate tenant name insertion → UNIQUE constraint violation with clear error message
- **EC-4**: NULL name insertion → NOT NULL constraint violation

**Dependencies:**
- Requires database superuser or schema owner privileges
- Flyway migration framework must be operational

---

### Story 2: Add Tenant ID Columns to Scoped Tables

**As a** Database Administrator
**I want to** add `tenant_id` foreign key columns to all tenant-scoped tables
**So that** every record can be associated with a specific landlord/tenant

**Acceptance Criteria:**

```gherkin
Given the `tenant` table exists
  And tenant-scoped tables (building, room, application, etc.) exist without `tenant_id` column
When the Flyway migration V{YYYY_MM_DD_HHMM}__add_tenant_id_columns.sql is executed
Then the following 14 tables have a new `tenant_id BIGINT` column:
  - app_user, building, room, application, room_allocation, staged_allocation
  - document, notification, user_notification, system_setting
  - allocation_rule, waiting_list_entry, activity_log, academic_year
And each `tenant_id` column has a foreign key constraint to `tenant(id)` with ON DELETE RESTRICT
And each `tenant_id` column has an index named `idx_{table}_tenant_id`
And the `tenant_id` column is initially NULLABLE (to allow backfill)
And institution and year_of_study tables do NOT receive tenant_id (shared reference data)
And the migration completes without errors
```

**Edge Cases:**
- **EC-1**: Column already exists (migration re-run) → `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` guard prevents error
- **EC-2**: Foreign key references non-existent tenant → Constraint violation prevents insertion
- **EC-3**: Orphaned records exist (no parent record to link to default tenant) → Backfill migration identifies and logs these for manual review
- **EC-4**: Table with millions of rows → Migration may take time; use `CONCURRENTLY` for index creation where possible (PostgreSQL)

**Dependencies:**
- Requires `tenant` table to exist (previous migration)
- Requires sufficient disk space for new columns and indexes

---

### Story 3: Backfill Existing Data with Default Tenant

**As a** Database Administrator
**I want to** assign all existing records to a "Default Tenant"
**So that** existing landlord data is preserved and the system continues to function

**Acceptance Criteria:**

```gherkin
Given the `tenant` table exists
  And tenant-scoped tables have `tenant_id` column (nullable)
  And existing records have `tenant_id = NULL`
When the Flyway migration V{YYYY_MM_DD_HHMM}__backfill_default_tenant.sql is executed
Then a tenant record is inserted with:
  - name = 'Default Tenant'
  - is_active = true
And all NULL `tenant_id` values are updated to the default tenant's ID (e.g., 1) for all scoped tables
And the migration logs the number of rows updated per table
And zero records remain with `tenant_id IS NULL` after backfill
And the migration is idempotent (safe to re-run)
And NOT NULL constraints are added to `tenant_id` columns after successful backfill
```

**Edge Cases:**
- **EC-1**: Default tenant already exists (migration re-run) → Use `INSERT ... ON CONFLICT DO NOTHING` or `WHERE NOT EXISTS` guard
- **EC-2**: Partial backfill (migration failed mid-execution) → Idempotent UPDATE ensures only NULL values are updated
- **EC-3**: Concurrent insertions during migration → Use transaction isolation or maintenance window to prevent new NULL records
- **EC-4**: Row count verification fails → Migration should log error and halt before adding NOT NULL constraints

**Dependencies:**
- Requires previous migration adding `tenant_id` columns
- Requires maintenance window or low-traffic period for production deployment

---

### Story 4: Query Performance with Tenant Filtering

**As a** Backend Developer
**I want to** ensure queries filtering by `tenant_id` are performant
**So that** multi-tenancy does not degrade system responsiveness

**Acceptance Criteria:**

```gherkin
Given the database has `tenant_id` columns with indexes
  And the database contains records for multiple tenants
When a query filters by `tenant_id = X`
Then the query execution plan uses the `idx_{table}_tenant_id` index
And query execution time is < 100ms for tables with up to 100,000 rows
And EXPLAIN ANALYZE shows index scan (not sequential scan)
```

**Edge Cases:**
- **EC-1**: Query does not include `tenant_id` filter → Query returns all records (potential data leakage if not caught by application layer)
- **EC-2**: Composite query with `tenant_id` and other filters → Database optimizer chooses appropriate composite index or index merge
- **EC-3**: Table has single tenant (low cardinality) → Index may be skipped by optimizer; this is acceptable as dataset is small
- **EC-4**: Index bloat over time → Monitor and rebuild indexes during maintenance windows

**Dependencies:**
- Requires indexes to be created in previous migrations
- Requires sufficient PostgreSQL shared_buffers and work_mem configuration

---

### Story 5: Enforce Tenant Boundaries at Database Level

**As a** Security Engineer
**I want to** ensure foreign key constraints prevent cross-tenant data linkage
**So that** accidental or malicious cross-tenant access is prevented at the database level

**Acceptance Criteria:**

```gherkin
Given tenant A has building ID 1 with tenant_id = 1
  And tenant B has application ID 100 with tenant_id = 2
When an allocation attempts to link application 100 (tenant 2) to room in building 1 (tenant 1)
Then the system shall prevent this allocation (application-layer validation in TASK-2.08)
And no database-level constraints are violated (tenant_id columns are independent)
```

**Edge Cases:**
- **EC-1**: Manual SQL injection bypasses application-layer tenant filtering → Database cannot prevent this at schema level alone; application-layer filtering (TASK-2.08) is mandatory
- **EC-2**: Admin accidentally assigns wrong `tenant_id` → Application should validate tenant consistency before insertion
- **EC-3**: Cross-tenant foreign key (e.g., allocation references room from different tenant) → Application must validate `allocation.tenant_id = room.tenant_id`

**Dependencies:**
- Requires application-layer tenant context and validation (TASK-2.08)

---

## 5. Non-Functional Requirements

### 5.1 Performance
- **NFR-P1**: Migration execution time < 10 minutes for databases with up to 100,000 records per table
- **NFR-P2**: Queries filtering by `tenant_id` execute in < 100ms for tables with up to 100,000 rows
- **NFR-P3**: Index creation uses `CONCURRENTLY` where possible to avoid table locks during production deployment

### 5.2 Security
- **NFR-S1**: `tenant_id` columns shall have foreign key constraints with ON DELETE RESTRICT to prevent orphaned records
- **NFR-S2**: Default tenant cannot be deleted (enforced via CHECK constraint or application-layer protection)
- **NFR-S3**: Migration scripts shall be reviewed for SQL injection vulnerabilities before production deployment

### 5.3 Usability
- **NFR-U1**: Migration failure messages shall be clear and actionable (e.g., "tenant_id column already exists" vs generic error)
- **NFR-U2**: Database schema documentation auto-generated from migrations (via tools like SchemaSpy or dbdocs)

### 5.4 Reliability
- **NFR-R1**: Migrations shall be idempotent (safe to re-run if they fail partway)
- **NFR-R2**: Migrations shall use transactions where possible; if migration fails, changes are rolled back automatically
- **NFR-R3**: Backfill migration shall log row counts before and after update for audit trail

### 5.5 Scalability
- **NFR-SC1**: Schema design shall support up to 1,000 tenants without requiring schema changes
- **NFR-SC2**: Indexes shall maintain query performance as tenant count grows

---

## 6. Data Requirements

### 6.1 Data to Capture

**Tenant Table:**

| Field Name | Type | Required | Validation | Notes |
|------------|------|----------|------------|-------|
| id | BIGSERIAL | Yes | Auto-increment primary key | Unique identifier for tenant |
| name | VARCHAR(100) | Yes | NOT NULL, UNIQUE | Human-readable tenant name |
| domain | VARCHAR(255) | No | UNIQUE (when not null) | Optional subdomain for tenant-specific routing |
| config | JSONB | No | Defaults to `{}` | Tenant-specific configuration overrides (offer expiry, decline limits, required documents) |
| is_active | BOOLEAN | Yes | Defaults to true | Soft-delete flag; inactive tenants cannot create new applications |
| created_at | TIMESTAMP | Yes | Defaults to NOW() | Audit timestamp |
| updated_at | TIMESTAMP | Yes | Defaults to NOW() | Audit timestamp |

**Tenant ID Columns (added to scoped tables):**

| Field Name | Type | Required | Validation | Notes |
|------------|------|----------|------------|-------|
| tenant_id | BIGINT | Yes (after backfill) | FK to tenant(id), NOT NULL, indexed | Links record to owning tenant |

### 6.2 Data Relationships

- **Tenant** has many **Buildings** (one-to-many)
- **Tenant** has many **Rooms** (one-to-many via building, but direct FK for query efficiency)
- **Tenant** has many **Applications** (one-to-many)
- **Tenant** has many **Room Allocations** (one-to-many)
- **Tenant** has many **Staged Allocations** (one-to-many)
- **Tenant** has many **Documents** (one-to-many)
- **Tenant** has many **Notifications** (one-to-many)
- **Tenant** has many **User Notifications** (one-to-many)
- **Tenant** has many **System Settings** (one-to-many for tenant-specific overrides)
- **Tenant** has many **Allocation Rules** (one-to-many for tenant-specific scoring)
- **Tenant** has many **Waiting List Entries** (one-to-many)
- **Tenant** has many **Activity Logs** (one-to-many for audit trail)
- **Tenant** has many **Academic Years** (one-to-many for institution-specific calendars)

**Cardinality Notes:**
- Relationship is strictly hierarchical (no cross-tenant references allowed)
- Cascading deletes are prohibited (`ON DELETE RESTRICT`)
- Soft-delete pattern used for tenant deactivation (`is_active = false`)

### 6.3 Data Migration

**Existing Data to Migrate:**

1. **Default Tenant Creation:**
   ```sql
   INSERT INTO tenant (name, is_active) VALUES ('Default Tenant', true) ON CONFLICT DO NOTHING;
   ```

2. **Backfill Scoped Tables:**
   ```sql
   UPDATE building SET tenant_id = 1 WHERE tenant_id IS NULL;
   UPDATE room SET tenant_id = 1 WHERE tenant_id IS NULL;
   UPDATE application SET tenant_id = 1 WHERE tenant_id IS NULL;
   UPDATE room_allocation SET tenant_id = 1 WHERE tenant_id IS NULL;
   UPDATE staged_allocation SET tenant_id = 1 WHERE tenant_id IS NULL;
   UPDATE document SET tenant_id = 1 WHERE tenant_id IS NULL;
   UPDATE notification SET tenant_id = 1 WHERE tenant_id IS NULL;
   UPDATE user_notification SET tenant_id = 1 WHERE tenant_id IS NULL;
   UPDATE system_setting SET tenant_id = 1 WHERE tenant_id IS NULL;
   UPDATE allocation_rule SET tenant_id = 1 WHERE tenant_id IS NULL;
   UPDATE waiting_list_entry SET tenant_id = 1 WHERE tenant_id IS NULL;
   UPDATE activity_log SET tenant_id = 1 WHERE tenant_id IS NULL;
   UPDATE academic_year SET tenant_id = 1 WHERE tenant_id IS NULL;
   ```

3. **Data Cleanup:**
   - Verify no orphaned records exist (records that reference non-existent parent entities)
   - Log any records that cannot be backfilled for manual review

**Data Transformation:**
- No data transformation required; this is purely additive (new columns, no column type changes)

---

## 7. Integration Points

### 7.1 Internal Integrations

- **Flyway Migrations**: All schema changes applied via Flyway versioned migrations following naming convention `V{YYYY_MM_DD_HHMM}__{description}.sql`
- **JPA Entities** (TASK-2.08): All tenant-scoped entity classes will be updated to include `@Column(name = "tenant_id")` field
- **Hibernate Filters** (TASK-2.08): Automatic tenant filtering implemented at ORM level using `@FilterDef` and `@Filter` annotations
- **JWT Token Claims** (TASK-2.08): JWT tokens will include `tenantId` claim extracted during authentication
- **Activity Logging**: All audit logs will capture tenant context for compliance and debugging

### 7.2 External Systems

- No external system integration required for this task (database schema only)

### 7.3 Data Import/Export

- **Database Backup**: Ensure backup strategy accounts for tenant-scoped data; restore operations must preserve tenant_id integrity
- **CSV Export** (Future): When exporting data, tenant context must be included to prevent cross-tenant data mixing during re-import

---

## 8. UI/UX Requirements

**This task has NO frontend impact.** This is a pure database schema change. Frontend changes for tenant management UI will be addressed in future tasks.

**Future UI Enhancements (Out of Scope for TASK-2.07):**
- Admin UI for tenant creation, configuration, and management
- Tenant selection dropdown for super-admins (cross-tenant access)
- Tenant branding customization (logos, color schemes)

---

## 9. Business Rules and Constraints

**BR-1**: Tenant Name Uniqueness
- **Rule**: Tenant names must be unique across the entire platform.
- **Rationale**: Prevents confusion when managing multiple tenants. Tenant name is a natural key for lookups.
- **Exception Handling**: If duplicate name is inserted, database raises UNIQUE constraint violation; transaction rolls back.

**BR-2**: Tenant Domain Uniqueness (When Not Null)
- **Rule**: If a tenant has a domain specified, it must be unique across all tenants.
- **Rationale**: Future subdomain-based routing (e.g., `tenant-a.platform.com`) requires unique domains.
- **Exception Handling**: Duplicate domain insertion raises UNIQUE constraint violation; transaction rolls back.

**BR-3**: Tenant Deletion Restriction
- **Rule**: Tenants cannot be deleted if they have associated records (buildings, applications, allocations, etc.).
- **Rationale**: Prevents accidental data loss and orphaned records. Use soft-delete (`is_active = false`) instead.
- **Exception Handling**: Foreign key constraints with `ON DELETE RESTRICT` prevent deletion; database raises constraint violation error.

**BR-4**: Default Tenant Immutability
- **Rule**: The default tenant (created during migration) shall not be deleted or have its ID changed.
- **Rationale**: Existing data references this tenant; deletion would cause referential integrity violations.
- **Exception Handling**: Application-layer protection prevents deletion of default tenant (future implementation in TASK-2.08).

**BR-5**: Tenant ID Required on All Scoped Records
- **Rule**: All records in tenant-scoped tables must have a valid `tenant_id` referencing an existing tenant.
- **Rationale**: Enforces tenant ownership and prevents orphaned records.
- **Exception Handling**: NOT NULL constraint and foreign key constraint prevent insertion of invalid `tenant_id` values.

**BR-6**: Cross-Tenant Data Isolation
- **Rule**: Applications, allocations, and documents from tenant A shall never reference buildings or rooms from tenant B.
- **Rationale**: Fundamental security requirement to prevent cross-tenant data leakage.
- **Exception Handling**: Application-layer validation (TASK-2.08) enforces this; database schema alone cannot prevent cross-tenant FKs.

**BR-7**: Index Creation for Query Performance
- **Rule**: Every `tenant_id` column must have a B-tree index.
- **Rationale**: Queries filtering by tenant_id are the most common; without indexes, performance degrades as data grows.
- **Exception Handling**: Migration includes index creation as mandatory step; if index creation fails, migration fails.

**BR-8**: Idempotent Migrations
- **Rule**: All migrations must be safe to re-run if they fail partway through execution.
- **Rationale**: Production migrations may fail due to network issues, timeouts, or constraint violations; must be resumable.
- **Exception Handling**: Use `IF NOT EXISTS`, `ON CONFLICT DO NOTHING`, and `WHERE IS NULL` guards in SQL.

---

## 10. Success Metrics

**How will we measure success?**

| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| **Migration Execution Success Rate** | 100% (zero failures in test and production) | Flyway migration status logs |
| **Data Integrity Post-Migration** | 100% of records have valid `tenant_id` | SQL query: `SELECT COUNT(*) FROM {table} WHERE tenant_id IS NULL` (should be 0) |
| **Query Performance** | <100ms for tenant-scoped queries | EXPLAIN ANALYZE on sample queries with tenant_id filter |
| **Index Usage** | 100% of tenant-scoped queries use tenant_id index | PostgreSQL query planner logs (index scans vs seq scans) |
| **Zero Cross-Tenant Data Leakage** | No instances of cross-tenant access | Code review, integration tests, penetration testing (TASK-2.08) |
| **Developer Productivity** | Zero developer questions about "which tables need tenant_id?" | Clear documentation and architectural guidance |

---

## 11. Edge Cases and Error Scenarios

### 11.1 Edge Cases

**EC-1: Migration Runs Twice (Idempotency Test)**
- **Description**: Migration script executed a second time (e.g., manual re-run after partial failure)
- **Expected Behavior**: Migration completes successfully with no duplicate data. `IF NOT EXISTS` guards prevent errors.
- **Frequency**: Occasional (during troubleshooting or deployment rollback)

**EC-2: Concurrent Writes During Migration**
- **Description**: New records inserted into tenant-scoped tables while backfill migration is running
- **Expected Behavior**: New records are inserted with `tenant_id = NULL`, then backfilled by UPDATE. Application must handle this gracefully or migration runs during maintenance window.
- **Frequency**: Rare (only if migration runs in production without maintenance window)

**EC-3: Large Table Backfill Performance**
- **Description**: Table with 1 million+ rows requires `tenant_id` backfill
- **Expected Behavior**: Migration may take several minutes. Use batched UPDATEs if necessary (`UPDATE ... WHERE id BETWEEN X AND Y`). Progress logging every 10,000 rows.
- **Frequency**: Rare (only on large production databases)

**EC-4: Orphaned Records (No Parent Entity)**
- **Description**: Database contains records (e.g., applications) that reference non-existent students or buildings
- **Expected Behavior**: Backfill migration logs these records for manual review. May require data cleanup before migration.
- **Frequency**: Rare (indicates pre-existing data integrity issues)

**EC-5: Null Domain Uniqueness Violation**
- **Description**: Multiple tenants have `domain = NULL`
- **Expected Behavior**: PostgreSQL treats multiple NULLs as distinct (UNIQUE constraint allows multiple NULLs). No error occurs.
- **Frequency**: Common (most tenants will not have custom domains initially)

**EC-6: Default Tenant Deletion Attempt**
- **Description**: Admin attempts to delete default tenant (e.g., via SQL or future admin UI)
- **Expected Behavior**: Foreign key constraints prevent deletion (ON DELETE RESTRICT raises error). Application UI (future) disables delete button for default tenant.
- **Frequency**: Rare (accidental admin action)

**EC-7: Tenant ID Mismatch in Allocation (Cross-Tenant Allocation)**
- **Description**: Application from tenant A is allocated to room from tenant B
- **Expected Behavior**: Database schema does not prevent this (no cross-table CHECK constraint). Application-layer validation (TASK-2.08) must enforce `allocation.tenant_id = application.tenant_id = room.tenant_id`.
- **Frequency**: Rare (logic error or malicious intent)

**EC-8: Index Creation Fails (Disk Space or Timeout)**
- **Description**: CREATE INDEX command fails due to insufficient disk space or statement timeout
- **Expected Behavior**: Migration fails and rolls back. DBA investigates (free disk space, increase timeout setting). Re-run after fix.
- **Frequency**: Rare (production databases with resource constraints)

**EC-9: Config JSON Invalid Format**
- **Description**: Tenant config field contains invalid JSON (e.g., malformed JSON string)
- **Expected Behavior**: PostgreSQL JSONB type validates JSON format on insert. Invalid JSON raises error and transaction rolls back.
- **Frequency**: Rare (only if application bypasses validation or manual SQL insertion)

**EC-10: Multi-Tenant Query Without Tenant Filter (Application Bug)**
- **Description**: Developer writes query without `tenant_id` filter, potentially exposing cross-tenant data
- **Expected Behavior**: At database level, query succeeds and returns all records. Application-layer Hibernate filters (TASK-2.08) must prevent this.
- **Frequency**: Common during development (caught by code review and testing)

### 11.2 Error Scenarios

| Error Type | User Action | System Response | User Message |
|------------|-------------|-----------------|--------------|
| **Duplicate Tenant Name** | Admin inserts tenant with existing name | Database UNIQUE constraint violation; transaction rolls back | "Tenant name 'ABC Properties' already exists. Please choose a unique name." |
| **Duplicate Tenant Domain** | Admin inserts tenant with existing domain | Database UNIQUE constraint violation; transaction rolls back | "Domain 'abc.platform.com' is already in use. Please choose a unique domain." |
| **Null Tenant Name** | Admin inserts tenant without name | Database NOT NULL constraint violation; transaction rolls back | "Tenant name is required." |
| **Foreign Key Violation (Non-Existent Tenant)** | Application tries to insert allocation with `tenant_id = 999` (non-existent) | Database FK constraint violation; transaction rolls back | "Invalid tenant reference. Please contact support." |
| **Tenant Deletion with Associated Records** | Admin attempts to delete tenant with existing buildings | Database FK constraint violation (ON DELETE RESTRICT); transaction rolls back | "Cannot delete tenant 'ABC Properties' because it has associated buildings. Please deactivate the tenant instead." |
| **Migration Rollback** | Migration fails partway due to syntax error or constraint violation | Flyway rolls back transaction; database state unchanged | "Migration V2026_02_15_1400__add_tenant_id_columns failed: syntax error at line 15. See logs for details." |

---

## 12. Dependencies and Prerequisites

**Technical Dependencies:**
- **Flyway 9.x** — Migration execution framework
- **PostgreSQL 16.2** — Database server with JSONB support
- **Database Superuser Access** — Required to create tables, add columns, and create indexes
- **Sufficient Disk Space** — Estimate ~10% additional space for new columns and indexes

**Feature Dependencies:**
- **Phase 1 Complete** (TASK-1.01 through TASK-1.10) — Multi-tenancy is a Phase 2 task per Architecture.md
- **No concurrent schema changes** — Ensure no other team members are running migrations during deployment

**Data Dependencies:**
- **Clean Database State** — No orphaned records (applications without students, allocations without rooms)
- **Backup Before Migration** — Full database backup completed before running production migrations

---

## 13. Out of Scope

**Explicitly NOT included in this feature:**

1. **Application-Layer Tenant Filtering** — Hibernate filters, TenantContext, and JWT integration (covered in TASK-2.08)
2. **Tenant Management UI** — Admin interface for creating, editing, and deleting tenants
3. **Tenant Configuration UI** — Interface for setting tenant-specific offer expiry, decline limits, required documents
4. **Cross-Tenant Reporting** — Super-admin dashboard showing aggregated metrics across all tenants
5. **Tenant-Specific Branding** — Custom logos, color schemes, email templates per tenant
6. **Subdomain-Based Routing** — URL routing like `tenant-a.platform.com` (domain field is placeholder for future)
7. **Multi-Tenant User Accounts** — Users belonging to multiple tenants (app_user does not receive tenant_id in this task)
8. **Tenant Billing and Subscriptions** — Payment processing, subscription tiers, usage-based billing
9. **Data Archival** — Archiving or purging data from inactive tenants
10. **Tenant Data Export** — Per-tenant data export for GDPR compliance or data portability

**Future Enhancements (Parking Lot):**

- **Tenant Onboarding Wizard** — Guided setup for new landlords (create tenant, configure settings, add buildings)
- **Tenant Usage Analytics** — Dashboard showing per-tenant application volume, allocation success rate, occupancy rate
- **Tenant Isolation Testing** — Automated tests to verify no cross-tenant data leakage
- **Performance Monitoring** — Per-tenant query performance dashboards
- **Tenant-Specific Feature Flags** — Ability to enable/disable features per tenant (using FF4J)

---

## 14. Assumptions and Open Questions

**Assumptions:**

1. **Single-Tenant Operation During Migration** — Production system is currently used by a single landlord; all existing data belongs to "Default Tenant"
2. **Low Concurrent Traffic** — Migration can be executed during a maintenance window with minimal concurrent writes
3. **PostgreSQL 16.2 Stability** — No known bugs in PostgreSQL JSONB or Flyway integration
4. **Developer Training** — Backend developers understand multi-tenancy concepts and will not bypass tenant filtering (reinforced in TASK-2.08)
5. **No Historical Data Purge** — All existing data must be preserved (no cleanup or archival during migration)

**Open Questions for Architect/Developer:**

1. **Q1**: Should `app_user` table receive `tenant_id` now, or defer until multi-tenant user accounts are required?
   - **Recommendation**: Add tenent_id this will link admin users to their tenent

2. **Q2**: Should `institution` and `year_of_study` tables be tenant-scoped (different institutions per tenant)?
   - **Recommendation**: these should be tenent-scoped this should include document Types as well

3. **Q3**: Should `academic_year` be tenant-scoped (tenant-specific calendars)?
   - **Recommendation**: Yes. Different institutions may have different academic calendars. Include `tenant_id` on `academic_year`.

4. **Q4**: Should migrations use `CONCURRENTLY` for index creation to avoid table locks?
   - **Recommendation**: Yes for production. Not supported in transactions, so index creation must be in a separate migration file.

5. **Q5**: Should tenant deletion be hard-delete or soft-delete only?
   - **Recommendation**: Soft-delete only (`is_active = false`). Hard-delete allowed only for test tenants with zero associated records.

6. **Q6**: Should backfill migration batch updates (e.g., 10,000 rows at a time) for large tables?
   - **Recommendation**: Defer to DBA. For tables with <100k rows, single UPDATE is acceptable. For larger tables, batch updates may reduce lock contention.

---

## 15. Approval and Sign-off

**Business Owner**: Platform Product Manager
**Product Manager**: [To be confirmed]
**Estimated Business Value**: **HIGH**

**Risk Assessment:**

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| **Cross-tenant data leakage** | Critical (data breach, compliance violation) | Medium (if TASK-2.08 not implemented correctly) | Mandatory application-layer tenant filtering (Hibernate filters). Automated tests for cross-tenant access attempts. Security code review. |
| **Migration failure in production** | High (downtime, rollback required) | Low (migrations tested in staging) | Test migrations in staging environment with production-like data volume. Full database backup before migration. Rollback plan documented. |
| **Query performance degradation** | Medium (slow page loads, user complaints) | Low (indexes mitigate this) | Load testing with multi-tenant data. Monitor query execution plans. Add composite indexes if needed. |
| **Developer bypasses tenant filtering** | High (accidental data leakage) | Medium (human error) | Code review enforcement. Automated linting rules to detect queries without tenant_id filter. Developer training. |
| **Orphaned records prevent backfill** | Medium (migration blocked) | Low (pre-migration data quality check) | Pre-migration data audit. Identify and fix orphaned records before production migration. |
| **Disk space exhaustion during migration** | Medium (migration fails, requires cleanup) | Low (disk space pre-checked) | Monitor disk usage. Ensure 20% free space before migration. |

---

## Analysis Checklist

Before finalizing, ensure you've covered:

- [x] Clear problem statement with business context
- [x] All stakeholders identified with their needs
- [x] 5 user stories with Given/When/Then acceptance criteria
- [x] Edge cases analyzed (10 scenarios documented)
- [x] Data requirements fully specified (tenant table schema, tenant_id columns)
- [x] Integration points identified (Flyway, JPA, Hibernate filters)
- [x] UI/UX requirements described (none for this task)
- [x] Business rules documented with rationale (8 rules)
- [x] Success metrics defined and measurable
- [x] Out-of-scope items clearly listed (10 items)
- [x] Security considerations addressed (cross-tenant data leakage prevention)
- [x] Dependencies and prerequisites listed (Phase 1 complete, database access, backup)
- [x] Open questions flagged for technical team (6 questions)

---

**Remember**: This is the **foundational schema change** for multi-tenancy. Success depends on:
1. **Idempotent migrations** — Safe to re-run if they fail
2. **Comprehensive indexing** — Query performance must not degrade
3. **Clear documentation** — Developers must understand which tables are tenant-scoped
4. **Data integrity** — All existing records preserved and assigned to default tenant
5. **Follow-up implementation** (TASK-2.08) — Application-layer tenant filtering is **mandatory** to prevent cross-tenant data leakage

This document will guide the next artifacts (solution-design, implementation-plan, tech-review) and ensure the technical team implements multi-tenancy correctly and safely.
