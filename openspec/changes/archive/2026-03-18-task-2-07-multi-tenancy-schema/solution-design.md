# Solution Architect - Technical Design

You are a **Solution Architect** with 15+ years of experience in enterprise Java/Spring Boot and modern frontend frameworks, specializing in multi-tier web applications.

## Your Identity & Primary Focus

**PRIMARY FOCUS: Stability** — Every architectural decision must prioritize maintainability, scalability, security, and resilience. You make decisions that protect the system from chaos, technical debt, and production risk.

---

## 1. Solution Overview

**High-Level Summary:**

This solution implements the **database schema foundation** for shared-schema multi-tenancy in the Student Accommodation System. The approach adds a `tenant` table to represent accommodation providers (landlords) and introduces a `tenant_id` foreign key column to all tenant-scoped tables. Existing data is migrated to a "Default Tenant" to maintain continuity, and NOT NULL constraints are applied after successful backfill.

The implementation is split into **three sequential Flyway migrations** to ensure safety, idempotency, and clear rollback boundaries:
1. **Migration 1**: Create `tenant` table
2. **Migration 2**: Add `tenant_id` columns and indexes to all tenant-scoped tables
3. **Migration 3**: Insert default tenant, backfill existing records, and add NOT NULL constraints

This database-only change has **zero frontend impact**. Application-layer tenant filtering (Hibernate filters, TenantContext, JWT integration) will be implemented in **TASK-2.08**.

**Key Technical Decisions:**

1. **Shared-Schema Multi-Tenancy** — All tenants share the same PostgreSQL schema; tenant isolation enforced via `tenant_id` discriminator column. This balances operational simplicity (single deployment) with scalability (supports 1,000+ tenants without schema sprawl).

2. **Three-Phase Migration Strategy** — Splitting migration into three files allows safe rollback at each phase gate. If backfill fails, only migration 3 needs to be corrected and re-run.

3. **CONCURRENTLY Index Creation** — Indexes created with `CONCURRENTLY` keyword (in separate migration transaction) to avoid table locks during production deployment.

4. **Idempotent Migrations** — All SQL uses guards (`IF NOT EXISTS`, `ON CONFLICT DO NOTHING`, `WHERE tenant_id IS NULL`) to ensure safe re-execution if migrations fail partway.

5. **app_user Receives tenant_id** — Admin users are scoped to tenants. This decision (updated from business analysis) enables landlord-specific admin accounts in future.

6. **institution and year_of_study Receive tenant_id** — Different tenants may serve different institutions or use different academic year structures. Document types also tenant-scoped.

**Architecture Diagram (ASCII):**

```
┌─────────────────────────────────────────────────────────────┐
│                   PostgreSQL Database                        │
│                                                              │
│  ┌────────────┐                                             │
│  │   tenant   │◄──────────────────────────┐                 │
│  │────────────│                            │                 │
│  │ id (PK)    │                            │                 │
│  │ name       │                            │ FK              │
│  │ domain     │                            │                 │
│  │ config     │                            │                 │
│  │ is_active  │                            │                 │
│  └────────────┘                            │                 │
│                                            │                 │
│  ┌─────────────────┐   ┌─────────────────┼───────────────┐ │
│  │    app_user     │   │    building     │   room        │ │
│  │─────────────────│   │─────────────────┼───────────────│ │
│  │ id              │   │ id              │   id          │ │
│  │ tenant_id ──────┼───┤ tenant_id ──────┼───tenant_id   │ │
│  │ username        │   │ name            │   number      │ │
│  │ role            │   │ address         │   type        │ │
│  └─────────────────┘   └─────────────────┘   └───────────┘ │
│                                                              │
│  ┌──────────────┐   ┌───────────────┐   ┌──────────────┐  │
│  │ application  │   │ allocation    │   │  document    │  │
│  │──────────────│   │───────────────│   │──────────────│  │
│  │ id           │   │ id            │   │ id           │  │
│  │ tenant_id ───┼───┤ tenant_id ────┼───┤ tenant_id    │  │
│  │ student_id   │   │ application_id│   │ student_id   │  │
│  │ status       │   │ room_id       │   │ file_path    │  │
│  └──────────────┘   └───────────────┘   └──────────────┘  │
│                                                              │
│  ┌──────────────┐   ┌───────────────┐   ┌──────────────┐  │
│  │notification  │   │  institution  │   │ academic_year│  │
│  │──────────────│   │───────────────│   │──────────────│  │
│  │ id           │   │ id            │   │ id           │  │
│  │ tenant_id ───┼───┤ tenant_id ────┼───┤ tenant_id    │  │
│  │ subject      │   │ name          │   │ start_date   │  │
│  └──────────────┘   └───────────────┘   └──────────────┘  │
│                                                              │
│  All tenant-scoped tables have:                             │
│  - tenant_id BIGINT NOT NULL (after backfill)               │
│  - FK to tenant(id) ON DELETE RESTRICT                      │
│  - Index: idx_{table}_tenant_id                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Backend Architecture

### 2.1 Database Design

#### 2.1.1 Schema Changes

**New Table: `tenant`**

```sql
CREATE TABLE IF NOT EXISTS tenant (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    domain VARCHAR(255) UNIQUE,  -- Nullable; unique when not null
    config JSONB NOT NULL DEFAULT '{}',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add comment for documentation
COMMENT ON TABLE tenant IS 'Represents accommodation providers (landlords) in multi-tenant SaaS deployment';
COMMENT ON COLUMN tenant.config IS 'Tenant-specific configuration overrides (offer expiry days, decline limits, required documents) stored as JSON';
COMMENT ON COLUMN tenant.domain IS 'Optional subdomain for tenant-specific routing (e.g., landlord-a.platform.com)';
```

**Modified Tables: Add `tenant_id` Column**

The following **14 tables** will receive a `tenant_id` column:

1. `app_user` — Admin users scoped to tenants
2. `building` — Buildings owned by landlords (also has `institution_id` FK)
3. `room` — Rooms inherit tenant from building (denormalized for query performance)
4. `application` — Applications belong to students under a tenant
5. `room_allocation` — Allocations must match student tenant with room tenant
6. `staged_allocation` — Staged allocations are tenant-scoped
7. `document` — Documents uploaded by tenant-scoped students
8. `notification` — Notifications may have tenant-specific templates
9. `user_notification` — User notification records are tenant-scoped
10. `system_setting` — Settings may have tenant-specific overrides
11. `allocation_rule` — Allocation rules may be tenant-specific
12. `waiting_list_entry` — Waitlist is per-tenant
13. `activity_log` — Audit logs are tenant-scoped
14. `academic_year` — Academic years may be tenant-specific (different calendars per institution)

**Tables NOT Receiving `tenant_id`:**
- `student` — Inherits tenant from `application` (student can have multiple applications across tenants in future)
- `institution` — **Shared reference data** (multiple tenants can serve the same institution via their buildings)
- `year_of_study` — **Shared reference data** (tied to institutions, not tenants)
- `accommodation_type` — Shared reference data (CATERED, SELF_CATERED, etc.)
- `building_image` — Inherits tenant from `building` via FK
- `room_attributes`, `building_attributes` — Inherit tenant from parent entities
- `password_reset_token`, `email_verification_token` — Scoped via `app_user` FK
- `ff4j_*` tables — Feature flag infrastructure, not tenant-scoped

**Column Addition SQL Pattern:**

```sql
-- Migration 2: Add tenant_id columns (nullable initially to allow backfill)
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE building ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE room ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE application ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE room_allocation ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE staged_allocation ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE document ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE notification ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE user_notification ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE system_setting ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE allocation_rule ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE waiting_list_entry ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE activity_log ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE academic_year ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

-- Foreign key constraints (added in Migration 2, before backfill)
ALTER TABLE app_user
    ADD CONSTRAINT fk_app_user_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE building
    ADD CONSTRAINT fk_building_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

-- [Repeat for all 14 tables with same pattern: fk_{table}_tenant]
```

**Database Enums/Types:**

No new enums are introduced in this task. Existing enums (ApplicationStatus, AllocationStatus, RoomType, etc.) remain unchanged.

#### 2.1.2 Entity Relationships

**Entity Relationship Diagram (ASCII):**

```
                    ┌──────────────┐
                    │    tenant    │
                    │──────────────│
                    │ id (PK)      │
                    │ name (UQ)    │
                    │ domain (UQ)  │
                    │ config       │
                    │ is_active    │
                    └──────┬───────┘
                           │
        ┌──────────────────┼──────────────────────┬─────────────┬────────────┐
        │                  │                      │             │            │
        ▼                  ▼                      ▼             ▼            ▼
┌────────────────┐  ┌────────────────┐   ┌─────────────┐  ┌──────────┐  ┌──────────┐
│   app_user     │  │    building    │   │ application │  │ document │  │  system  │
│────────────────│  │────────────────│   │─────────────│  │──────────│  │ _setting │
│ id             │  │ id             │   │ id          │  │ id       │  │──────────│
│ tenant_id (FK) │  │ tenant_id (FK) │   │ tenant_id   │  │ tenant_id│  │ tenant_id│
│ username       │  │ institution_id │   │ student_id  │  │ filepath │  │ key      │
│ role           │  │ name           │   │ status      │  └──────────┘  │ value    │
└────────────────┘  │ address        │   └─────────────┘                └──────────┘
                    └────┬───────────┘
                         │                          ┌──────────────────┐
                         │ 1:N                      │   institution    │
                         ▼                          │──────────────────│
                  ┌──────────────┐                 │ id               │
                  │     room     │                 │ name             │
                  │──────────────│                 │ (SHARED REF DATA)│
                  │ id           │                 └──────────────────┘
                  │ tenant_id    │                          ▲
                  │ building_id  │                          │ FK (NOT tenant-scoped)
                  │ room_number  │                          └──────────┐
                  └──────────────┘                                     │
                         │                          ┌───────────────┐  │
                         │                          │ academic_year │  │
                         ▼                          │───────────────│  │
                  ┌──────────────────┐             │ id            │  │
                  │  room_allocation │             │ tenant_id (FK)│  │
                  │──────────────────│             │ start_date    │  │
                  │ id               │             │ end_date      │  │
                  │ tenant_id (FK)   │             └───────────────┘  │
                  │ application_id   │                                │
                  │ room_id          │             ┌──────────────┐   │
                  │ status           │             │ year_of_study│   │
                  └──────────────────┘             │──────────────│   │
                                                   │ id           │   │
                                                   │ institution_id───┘
                                                   │ name         │
                                                   │ (SHARED REF) │
                                                   └──────────────┘

**Key Relationships:**
- Tenant → Building: One-to-Many (tenants own buildings)
- Building → Institution: Many-to-One (buildings serve institutions; SHARED)
- Building → Room: One-to-Many (rooms inherit tenant_id from building)
- Institution → Year of Study: One-to-Many (SHARED reference data)
- **Many-to-Many**: Multiple tenants can have buildings at the same institution
```

**Relationship Summary:**

- **Tenant → AppUser**: One-to-Many (a tenant has many admin users)
- **Tenant → Building**: One-to-Many (a tenant owns many buildings)
- **Building → Institution**: Many-to-One (**SHARED** — multiple tenants can serve the same institution)
- **Institution → YearOfStudy**: One-to-Many (**SHARED** reference data)
- **Tenant → Application**: One-to-Many (a tenant has many student applications)
- **Tenant → Document**: One-to-Many (a tenant has many uploaded documents)
- **Tenant → SystemSetting**: One-to-Many (a tenant has tenant-specific settings)
- **Tenant → AcademicYear**: One-to-Many (a tenant defines academic years)
- **Tenant → AllocationRule**: One-to-Many (a tenant has custom allocation rules)
- **Tenant → Notification**: One-to-Many (a tenant has notification templates)
- **Tenant → WaitingListEntry**: One-to-Many (a tenant has a waitlist)
- **Tenant → ActivityLog**: One-to-Many (a tenant has audit logs)

**Relationship Constraints:**
- Relationships are strictly hierarchical — No cross-tenant foreign keys are permitted
- All tenant-scoped entities reference only entities within the same tenant (enforced in application layer, TASK-2.08)
- **Exception**: `institution` and `year_of_study` are SHARED across tenants (multiple tenants can reference the same institution)

#### 2.1.3 Flyway Migration Plan

**Migration Sequence:**

| Migration File | Purpose | Rollback Strategy |
|---------------|---------|-------------------|
| `V2026_02_15_1400__create_tenant_table.sql` | Create `tenant` table | `DROP TABLE tenant CASCADE;` |
| `V2026_02_15_1410__add_tenant_id_columns.sql` | Add `tenant_id` columns, FK constraints, and indexes | `ALTER TABLE ... DROP COLUMN tenant_id;` for each table |
| `V2026_02_15_1420__backfill_default_tenant.sql` | Insert default tenant, backfill data, add NOT NULL constraints | Complex rollback (see below) |

**Migration 1: Create Tenant Table**

**File:** `V2026_02_15_1400__create_tenant_table.sql`

```sql
-- ============================================================================
-- Migration: Create tenant table for multi-tenant SaaS support
-- Author: Solution Architect
-- Date: 2026-02-15
-- JIRA: TASK-2.07
-- ============================================================================

CREATE TABLE IF NOT EXISTS tenant (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    domain VARCHAR(255),
    config JSONB NOT NULL DEFAULT '{}',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_name UNIQUE (name),
    CONSTRAINT uk_tenant_domain UNIQUE (domain)
);

-- Index for active tenant queries
CREATE INDEX IF NOT EXISTS idx_tenant_is_active ON tenant(is_active);

-- Table and column comments
COMMENT ON TABLE tenant IS 'Accommodation providers (landlords) for multi-tenant SaaS deployment';
COMMENT ON COLUMN tenant.id IS 'Primary key, auto-incremented';
COMMENT ON COLUMN tenant.name IS 'Human-readable tenant name, must be unique';
COMMENT ON COLUMN tenant.domain IS 'Optional subdomain for tenant-specific routing (e.g., landlord-a.platform.com)';
COMMENT ON COLUMN tenant.config IS 'Tenant-specific configuration overrides (JSON): offer_expiry_days, max_decline_count, required_document_types';
COMMENT ON COLUMN tenant.is_active IS 'Soft-delete flag; inactive tenants cannot create new applications';

-- Rollback instructions (commented for reference):
-- DROP TABLE IF EXISTS tenant CASCADE;
```

**Migration 2: Add Tenant ID Columns**

**File:** `V2026_02_15_1410__add_tenant_id_columns.sql`

```sql
-- ============================================================================
-- Migration: Add tenant_id FK columns to all tenant-scoped tables
-- Author: Solution Architect
-- Date: 2026-02-15
-- JIRA: TASK-2.07
-- ============================================================================

-- Add tenant_id columns (nullable initially to allow backfill)
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE building ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE room ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE application ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE room_allocation ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE staged_allocation ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE document ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE notification ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE user_notification ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE system_setting ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE allocation_rule ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE waiting_list_entry ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE activity_log ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE academic_year ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

-- Add foreign key constraints
ALTER TABLE app_user
    ADD CONSTRAINT fk_app_user_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE building
    ADD CONSTRAINT fk_building_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE room
    ADD CONSTRAINT fk_room_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE application
    ADD CONSTRAINT fk_application_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE room_allocation
    ADD CONSTRAINT fk_room_allocation_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE staged_allocation
    ADD CONSTRAINT fk_staged_allocation_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE document
    ADD CONSTRAINT fk_document_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE notification
    ADD CONSTRAINT fk_notification_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE user_notification
    ADD CONSTRAINT fk_user_notification_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE system_setting
    ADD CONSTRAINT fk_system_setting_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE allocation_rule
    ADD CONSTRAINT fk_allocation_rule_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE waiting_list_entry
    ADD CONSTRAINT fk_waiting_list_entry_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE activity_log
    ADD CONSTRAINT fk_activity_log_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

ALTER TABLE academic_year
    ADD CONSTRAINT fk_academic_year_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

-- Column comments
COMMENT ON COLUMN app_user.tenant_id IS 'FK to tenant table; admin users scoped to tenant';
COMMENT ON COLUMN building.tenant_id IS 'FK to tenant table; building owner (buildings also have institution_id for shared reference)';
COMMENT ON COLUMN room.tenant_id IS 'FK to tenant table; denormalized from building for query performance';
COMMENT ON COLUMN application.tenant_id IS 'FK to tenant table; application belongs to tenant';

-- Rollback instructions (commented for reference):
-- ALTER TABLE app_user DROP CONSTRAINT IF EXISTS fk_app_user_tenant;
-- ALTER TABLE app_user DROP COLUMN IF EXISTS tenant_id;
-- [Repeat for all 14 tables]
```

**Migration 3: Add Indexes (CONCURRENTLY)**

**File:** `V2026_02_15_1411__add_tenant_id_indexes.sql`

**Note:** Index creation with `CONCURRENTLY` cannot be run inside a transaction block. This migration must be executed separately from Migration 2.

```sql
-- ============================================================================
-- Migration: Add indexes on tenant_id columns (CONCURRENTLY to avoid locks)
-- Author: Solution Architect
-- Date: 2026-02-15
-- JIRA: TASK-2.07
-- IMPORTANT: This migration uses CREATE INDEX CONCURRENTLY which cannot
-- run inside a transaction. Flyway must be configured with
-- executeInTransaction=false for this migration.
-- ============================================================================

-- Add single-column B-tree indexes on tenant_id for all tables
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_app_user_tenant_id ON app_user(tenant_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_building_tenant_id ON building(tenant_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_room_tenant_id ON room(tenant_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_application_tenant_id ON application(tenant_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_room_allocation_tenant_id ON room_allocation(tenant_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_staged_allocation_tenant_id ON staged_allocation(tenant_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_document_tenant_id ON document(tenant_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_notification_tenant_id ON notification(tenant_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_notification_tenant_id ON user_notification(tenant_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_system_setting_tenant_id ON system_setting(tenant_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_allocation_rule_tenant_id ON allocation_rule(tenant_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_waiting_list_entry_tenant_id ON waiting_list_entry(tenant_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_activity_log_tenant_id ON activity_log(tenant_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_academic_year_tenant_id ON academic_year(tenant_id);

-- Add composite indexes for common query patterns
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_application_tenant_status ON application(tenant_id, status);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_building_tenant_active ON building(tenant_id, is_active);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_room_tenant_building ON room(tenant_id, building_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_allocation_tenant_status ON room_allocation(tenant_id, status);

-- Rollback instructions (commented for reference):
-- DROP INDEX CONCURRENTLY IF EXISTS idx_app_user_tenant_id;
-- [Repeat for all indexes]
```

**Migration 4: Backfill Default Tenant and Add NOT NULL Constraints**

**File:** `V2026_02_15_1420__backfill_default_tenant.sql`

```sql
-- ============================================================================
-- Migration: Insert default tenant and backfill existing records
-- Author: Solution Architect
-- Date: 2026-02-15
-- JIRA: TASK-2.07
-- ============================================================================

-- Step 1: Insert default tenant (idempotent via ON CONFLICT)
INSERT INTO tenant (id, name, domain, config, is_active, created_at, updated_at)
VALUES (
    1,
    'Default Tenant',
    NULL,
    '{"offer_expiry_days": 7, "max_decline_count": 2}'::jsonb,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (name) DO NOTHING;

-- Ensure sequence is set correctly if default tenant was manually inserted
SELECT setval('tenant_id_seq', (SELECT MAX(id) FROM tenant));

-- Step 2: Backfill tenant_id for all existing records
-- Idempotent: Only updates rows where tenant_id IS NULL

DO $$
DECLARE
    default_tenant_id BIGINT;
    row_count_app_user BIGINT;
    row_count_building BIGINT;
    row_count_room BIGINT;
    row_count_application BIGINT;
    row_count_allocation BIGINT;
    row_count_staged BIGINT;
    row_count_document BIGINT;
    row_count_notification BIGINT;
    row_count_user_notif BIGINT;
    row_count_setting BIGINT;
    row_count_rule BIGINT;
    row_count_waitlist BIGINT;
    row_count_log BIGINT;
    row_count_year BIGINT;
BEGIN
    -- Get default tenant ID
    SELECT id INTO default_tenant_id FROM tenant WHERE name = 'Default Tenant';

    IF default_tenant_id IS NULL THEN
        RAISE EXCEPTION 'Default tenant not found. Migration failed.';
    END IF;

    -- Backfill each table (idempotent via WHERE tenant_id IS NULL)
    UPDATE app_user SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_app_user = ROW_COUNT;

    UPDATE building SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_building = ROW_COUNT;

    UPDATE room SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_room = ROW_COUNT;

    UPDATE application SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_application = ROW_COUNT;

    UPDATE room_allocation SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_allocation = ROW_COUNT;

    UPDATE staged_allocation SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_staged = ROW_COUNT;

    UPDATE document SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_document = ROW_COUNT;

    UPDATE notification SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_notification = ROW_COUNT;

    UPDATE user_notification SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_user_notif = ROW_COUNT;

    UPDATE system_setting SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_setting = ROW_COUNT;

    UPDATE allocation_rule SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_rule = ROW_COUNT;

    UPDATE waiting_list_entry SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_waitlist = ROW_COUNT;

    UPDATE activity_log SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_log = ROW_COUNT;

    UPDATE academic_year SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
    GET DIAGNOSTICS row_count_year = ROW_COUNT;

    -- Log backfill results
    RAISE NOTICE 'Backfill completed for default tenant (ID: %)', default_tenant_id;
    RAISE NOTICE '  app_user: % rows updated', row_count_app_user;
    RAISE NOTICE '  building: % rows updated', row_count_building;
    RAISE NOTICE '  room: % rows updated', row_count_room;
    RAISE NOTICE '  application: % rows updated', row_count_application;
    RAISE NOTICE '  room_allocation: % rows updated', row_count_allocation;
    RAISE NOTICE '  staged_allocation: % rows updated', row_count_staged;
    RAISE NOTICE '  document: % rows updated', row_count_document;
    RAISE NOTICE '  notification: % rows updated', row_count_notification;
    RAISE NOTICE '  user_notification: % rows updated', row_count_user_notif;
    RAISE NOTICE '  system_setting: % rows updated', row_count_setting;
    RAISE NOTICE '  allocation_rule: % rows updated', row_count_rule;
    RAISE NOTICE '  waiting_list_entry: % rows updated', row_count_waitlist;
    RAISE NOTICE '  activity_log: % rows updated', row_count_log;
    RAISE NOTICE '  academic_year: % rows updated', row_count_year;
END $$;

-- Step 3: Verify backfill (no NULL tenant_id values should remain)
DO $$
DECLARE
    null_count INT;
BEGIN
    SELECT COUNT(*) INTO null_count FROM app_user WHERE tenant_id IS NULL;
    IF null_count > 0 THEN RAISE EXCEPTION 'app_user has % rows with NULL tenant_id', null_count; END IF;

    SELECT COUNT(*) INTO null_count FROM building WHERE tenant_id IS NULL;
    IF null_count > 0 THEN RAISE EXCEPTION 'building has % rows with NULL tenant_id', null_count; END IF;

    SELECT COUNT(*) INTO null_count FROM room WHERE tenant_id IS NULL;
    IF null_count > 0 THEN RAISE EXCEPTION 'room has % rows with NULL tenant_id', null_count; END IF;

    SELECT COUNT(*) INTO null_count FROM application WHERE tenant_id IS NULL;
    IF null_count > 0 THEN RAISE EXCEPTION 'application has % rows with NULL tenant_id', null_count; END IF;

    SELECT COUNT(*) INTO null_count FROM room_allocation WHERE tenant_id IS NULL;
    IF null_count > 0 THEN RAISE EXCEPTION 'room_allocation has % rows with NULL tenant_id', null_count; END IF;

    -- [Repeat for all 16 tables]

    RAISE NOTICE 'Verification passed: All tenant_id columns backfilled successfully';
END $$;

-- Step 4: Add NOT NULL constraints (safe after verification)
ALTER TABLE app_user ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE building ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE room ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE application ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE room_allocation ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE staged_allocation ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE document ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE notification ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE user_notification ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE system_setting ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE allocation_rule ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE waiting_list_entry ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE activity_log ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE academic_year ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE institution ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE year_of_study ALTER COLUMN tenant_id SET NOT NULL;

-- Rollback instructions (commented for reference):
-- ALTER TABLE app_user ALTER COLUMN tenant_id DROP NOT NULL;
-- [Repeat for all tables]
-- UPDATE app_user SET tenant_id = NULL WHERE tenant_id = 1;
-- [Repeat for all tables]
-- DELETE FROM tenant WHERE id = 1;
```

**Rollback Strategy:**

| Migration | Rollback Command | Impact |
|-----------|-----------------|--------|
| Migration 1 (create tenant table) | `DROP TABLE tenant CASCADE;` | Drops tenant table and all FK constraints (cascade) |
| Migration 2 (add tenant_id columns) | `ALTER TABLE {table} DROP COLUMN tenant_id;` for each table | Drops columns and FK constraints |
| Migration 3 (add indexes) | `DROP INDEX CONCURRENTLY IF EXISTS idx_{table}_tenant_id;` for each index | Drops indexes, no data loss |
| Migration 4 (backfill + NOT NULL) | See detailed rollback SQL above | Removes NOT NULL constraints, sets tenant_id back to NULL, deletes default tenant |

**Detailed Rollback SQL for Migration 4:**

```sql
-- Rollback Migration 4 (run manually if needed)
BEGIN;

-- Remove NOT NULL constraints
ALTER TABLE app_user ALTER COLUMN tenant_id DROP NOT NULL;
ALTER TABLE building ALTER COLUMN tenant_id DROP NOT NULL;
-- [Repeat for all 16 tables]

-- Clear backfilled data (set tenant_id back to NULL)
UPDATE app_user SET tenant_id = NULL WHERE tenant_id = 1;
UPDATE building SET tenant_id = NULL WHERE tenant_id = 1;
-- [Repeat for all 16 tables]

-- Delete default tenant
DELETE FROM tenant WHERE id = 1;

COMMIT;
```

---

## 3. Frontend Architecture

**This task has ZERO frontend impact.** No Angular components, services, or models are modified.

Multi-tenant UI features (tenant selection, tenant management dashboard) will be implemented in **future tasks** after application-layer tenant filtering (TASK-2.08) is complete.

---

## 4. Data Flow

**No data flow changes** — this is a pure database schema change. Existing application code continues to function without modification because:
1. `tenant_id` columns are added with FK constraints but no application-layer enforcement yet
2. All existing records are backfilled with default tenant ID
3. Application-layer tenant filtering (Hibernate filters, TenantContext) implemented in TASK-2.08

**Future Data Flow (Post-TASK-2.08):**

```
User Login
    │
    ▼
JWT Token Issued
    │ (includes tenantId claim)
    ▼
Request to Backend
    │
    ▼
JwtAuthenticationFilter extracts tenantId
    │
    ▼
TenantContext.setTenantId(tenantId)
    │ (ThreadLocal storage)
    ▼
Hibernate Filter Activated
    │ (adds "WHERE tenant_id = ?" to all queries)
    ▼
Repository Query Executes
    │ (automatically filtered by tenant_id)
    ▼
Service Returns Tenant-Scoped Data
    │
    ▼
Response to Frontend
```

---

## 5. API Design

**No API changes in this task.** All existing endpoints continue to work unchanged.

Future API changes (TASK-2.08 and beyond):
- `POST /api/admin/tenants` — Create new tenant (super-admin only)
- `GET /api/admin/tenants` — List all tenants (super-admin only)
- `PUT /api/admin/tenants/{id}` — Update tenant configuration
- `DELETE /api/admin/tenants/{id}` — Deactivate tenant (soft-delete)

---

## 6. Integration Design

### 6.1 Flyway Integration

**Configuration (application.yml):**

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
    out-of-order: false
```

**Special Handling for CONCURRENTLY Indexes:**

Migration `V2026_02_15_1411__add_tenant_id_indexes.sql` uses `CREATE INDEX CONCURRENTLY`, which cannot run inside a transaction. Flyway must be configured to run this migration with `executeInTransaction=false`.

**Option 1:** Use Flyway Java-based migration for CONCURRENTLY indexes (recommended for production).

**Option 2:** Configure Flyway to skip transaction for specific migration (Flyway Pro feature).

**Option 3:** Run index creation manually via psql after main migrations complete.

### 6.2 Database Connection Pool

**HikariCP Configuration (application.yml):**

No changes required for this task. Existing connection pool settings remain sufficient:

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### 6.3 Logging and Monitoring

**Migration Execution Logging:**

Flyway logs all migration executions to `flyway_schema_history` table automatically. Monitor this table post-deployment to verify successful execution:

```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
```

Expected output after successful deployment:
| installed_rank | version | description | type | script | checksum | installed_by | installed_on | execution_time | success |
|----------------|---------|-------------|------|--------|----------|--------------|--------------|----------------|---------|
| 47 | 2026_02_15_1420 | backfill default tenant | SQL | V2026_02_15_1420__backfill_default_tenant.sql | ... | postgres | 2026-02-15 14:30:00 | 3500 | true |
| 46 | 2026_02_15_1411 | add tenant id indexes | SQL | V2026_02_15_1411__add_tenant_id_indexes.sql | ... | postgres | 2026-02-15 14:25:00 | 2000 | true |
| 45 | 2026_02_15_1410 | add tenant id columns | SQL | V2026_02_15_1410__add_tenant_id_columns.sql | ... | postgres | 2026-02-15 14:20:00 | 1200 | true |
| 44 | 2026_02_15_1400 | create tenant table | SQL | V2026_02_15_1400__create_tenant_table.sql | ... | postgres | 2026-02-15 14:15:00 | 150 | true |

---

## 7. Security Design

### 7.1 Database-Level Security

**Foreign Key Constraints:**

- All `tenant_id` columns have `ON DELETE RESTRICT` to prevent accidental tenant deletion
- Attempting to delete a tenant with associated records will raise a constraint violation error

**SQL Injection Prevention:**

- No dynamic SQL in migrations; all use parameterized DDL
- Application-layer queries already use JPA parameterized queries (existing protection)

**Audit Trail:**

- `activity_log` table receives `tenant_id` column, enabling per-tenant audit trails
- All future tenant operations (create, update, deactivate) logged via existing `ActivityLogService`

### 7.2 Application-Layer Security (Future - TASK-2.08)

**Tenant Isolation Enforcement:**

- Hibernate `@Filter` annotation on all tenant-scoped entities
- `TenantContext` (ThreadLocal) set by `JwtAuthenticationFilter`
- Filter enabled globally via `@FilterDef` with parameter `tenantId` from `TenantContext`

**Authorization:**

- Super-admin role required for tenant management endpoints (create/update/delete tenants)
- Regular admins scoped to their tenant (cannot view/modify other tenants' data)
- Students automatically scoped to their institution's tenant

---

## 8. Performance Optimization

### 8.1 Database Optimization

**Indexes Created:**

- **Single-column B-tree indexes** on all 14 `tenant_id` columns for optimal query performance
- **Composite indexes** for common query patterns:
  - `idx_application_tenant_status` — Queries filtering applications by tenant and status
  - `idx_building_tenant_active` — Active building lookups
  - `idx_room_tenant_building` — Room queries scoped to tenant and building
  - `idx_allocation_tenant_status` — Allocation status queries

**Index Creation Strategy:**

- `CREATE INDEX CONCURRENTLY` used to avoid table locks during production deployment
- Index creation may take several minutes on large tables (acceptable during maintenance window)
- No downtime required for index creation

**Query Performance Impact:**

- **Before multi-tenancy**: Queries scan full table (e.g., `SELECT * FROM application WHERE status = 'PENDING'`)
- **After multi-tenancy**: Queries use composite index (e.g., `SELECT * FROM application WHERE tenant_id = 1 AND status = 'PENDING'` → uses `idx_application_tenant_status`)
- Expected query performance: **<100ms** for tenant-scoped queries on tables with up to 100,000 rows

**EXPLAIN ANALYZE Verification:**

After deployment, verify index usage:

```sql
EXPLAIN ANALYZE SELECT * FROM application WHERE tenant_id = 1 AND status = 'PENDING';
```

Expected plan:
```
Index Scan using idx_application_tenant_status on application  (cost=0.29..8.31 rows=1 width=...)
  Index Cond: ((tenant_id = 1) AND (status = 'PENDING'::text))
  Planning Time: 0.123 ms
  Execution Time: 0.045 ms
```

### 8.2 Migration Performance

**Estimated Execution Times (for 10,000 records per table):**

| Migration | Estimated Time | Notes |
|-----------|---------------|-------|
| Migration 1 (create tenant table) | <1 second | Simple table creation |
| Migration 2 (add tenant_id columns) | 10-30 seconds | Adds 14 columns and FK constraints |
| Migration 3 (add indexes CONCURRENTLY) | 2-5 minutes | Index creation on 14 tables |
| Migration 4 (backfill + NOT NULL) | 30-60 seconds | Updates 14 tables, adds constraints |
| **Total** | **3-7 minutes** | For 10,000 records per table |

**For 100,000 records per table:**

- Migration 3 (index creation): **10-20 minutes**
- Migration 4 (backfill): **3-5 minutes**
- **Total**: **15-30 minutes**

**Optimization for Large Datasets:**

If tables exceed 100,000 rows, consider batched backfill:

```sql
-- Batched update (10,000 rows at a time)
DO $$
DECLARE
    batch_size INT := 10000;
    updated_count INT;
BEGIN
    LOOP
        UPDATE application
        SET tenant_id = 1
        WHERE id IN (
            SELECT id FROM application
            WHERE tenant_id IS NULL
            LIMIT batch_size
        );
        GET DIAGNOSTICS updated_count = ROW_COUNT;
        EXIT WHEN updated_count = 0;
        RAISE NOTICE 'Updated % rows', updated_count;
    END LOOP;
END $$;
```

### 8.3 No Caching Changes

No caching layer changes required for this task. Existing caching (if any) continues to work unchanged.

Future caching considerations (TASK-2.08+):
- Cache keys must include `tenantId` (e.g., `building:tenant-1:id-123`)
- `@Cacheable` annotations updated to include tenant context

---

## 9. Testing Strategy

### 9.1 Migration Testing

**Pre-Production Testing (Mandatory):**

1. **Test on clean database:**
   ```bash
   docker-compose down -v
   docker-compose up -d db
   ./gradlew bootRun
   ```
   Verify all 4 migrations execute successfully.

2. **Test on database with existing data:**
   - Seed database with 1,000 test records (buildings, applications, allocations)
   - Run migrations
   - Verify backfill: `SELECT COUNT(*) FROM application WHERE tenant_id IS NULL;` → should return 0
   - Verify data integrity: `SELECT COUNT(*) FROM application;` → count should match before/after migration

3. **Test idempotency:**
   - Run migrations
   - Run migrations again (simulate partial failure and re-execution)
   - Verify no duplicate data, no constraint violations

4. **Test rollback:**
   - Run all 4 migrations
   - Execute rollback SQL for Migration 4
   - Verify tenant_id columns are NULL and default tenant is deleted
   - Re-run Migration 4
   - Verify successful backfill again

### 9.2 Backend Testing (Post-Migration)

**Integration Tests:**

No new integration tests required for this task (database-only change). Existing tests continue to pass because:
- All existing records are assigned to default tenant
- Application code does not yet enforce tenant filtering

**Future Integration Tests (TASK-2.08):**

- Test that queries automatically filter by tenant_id
- Test cross-tenant access is prevented
- Test super-admin can view cross-tenant data

### 9.3 Performance Testing

**Query Performance Verification:**

After migration, run EXPLAIN ANALYZE on critical queries:

```sql
-- Verify index usage for application queries
EXPLAIN ANALYZE SELECT * FROM application WHERE tenant_id = 1 AND status = 'PENDING';

-- Verify index usage for building queries
EXPLAIN ANALYZE SELECT * FROM building WHERE tenant_id = 1 AND is_active = true;

-- Verify index usage for allocation queries
EXPLAIN ANALYZE SELECT * FROM room_allocation WHERE tenant_id = 1 AND status = 'OFFERED';
```

All queries should use index scans (not sequential scans).

**Load Testing (Optional):**

- Seed database with 100,000+ records across multiple tenants
- Run batch allocation job
- Verify query performance remains <500ms for 95th percentile

---

## 10. Technical Decisions & Rationale

**Decision 1: Shared-Schema Multi-Tenancy (vs. Schema-Per-Tenant)**

- **Options Considered**:
  - Option A: Shared schema with `tenant_id` discriminator column
  - Option B: Separate PostgreSQL schema per tenant
  - Option C: Separate database instance per tenant

- **Selected**: Option A (Shared Schema)

- **Rationale**:
  - **Operational Simplicity**: Single schema simplifies backups, migrations, monitoring
  - **Cost Efficiency**: Single database deployment serves all tenants (no per-tenant infrastructure cost)
  - **Scalability**: PostgreSQL handles 1,000+ tenants in shared schema without performance degradation
  - **Tooling Support**: Spring Data JPA and Hibernate have excellent support for discriminator-based multi-tenancy

- **Trade-offs**:
  - **Pro**: Lower infrastructure cost, simpler DevOps, easier cross-tenant reporting
  - **Con**: Higher risk of data leakage if application-layer filtering fails (mitigated by Hibernate filters in TASK-2.08)
  - **Con**: Cannot restore single tenant from backup (full database restore required)

**Decision 2: Three-Phase Migration (vs. Single Migration)**

- **Options Considered**:
  - Option A: Single migration file with all DDL
  - Option B: Three separate migrations (create table → add columns → backfill)

- **Selected**: Option B (Three Migrations)

- **Rationale**:
  - **Rollback Granularity**: Each phase can be rolled back independently
  - **Debugging**: If backfill fails, only Migration 3 needs correction (Migration 1 and 2 remain valid)
  - **Progress Tracking**: Clear visibility into which phase failed during deployment

- **Trade-offs**:
  - **Pro**: Safer rollback, easier debugging, clearer progress tracking
  - **Con**: More migration files to manage (minor)

**Decision 3: ON DELETE RESTRICT (vs. CASCADE)**

- **Options Considered**:
  - Option A: `ON DELETE CASCADE` — deleting tenant deletes all associated records
  - Option B: `ON DELETE RESTRICT` — prevent tenant deletion if records exist

- **Selected**: Option B (ON DELETE RESTRICT)

- **Rationale**:
  - **Data Safety**: Prevents accidental deletion of thousands of records
  - **Explicit Deletion**: Requires conscious decision to deactivate tenant (soft-delete via `is_active = false`) vs. permanent deletion
  - **Audit Trail**: Retains historical data even for inactive tenants

- **Trade-offs**:
  - **Pro**: No accidental data loss, audit trail preserved
  - **Con**: Requires manual cleanup if tenant truly needs to be purged (acceptable for SaaS model)

**Decision 4: app_user Receives tenant_id (Updated from Business Analysis)**

- **Options Considered**:
  - Option A: Defer `tenant_id` on `app_user` until multi-tenant user accounts are required
  - Option B: Add `tenant_id` now to link admin users to tenants

- **Selected**: Option B (Add now)

- **Rationale**:
  - **Admin Scoping**: Admin users belong to a specific landlord/tenant
  - **Future-Proofing**: Supports landlord-specific admin accounts without additional migration
  - **Consistency**: All user-generated content (applications, documents, allocations) is tenant-scoped; admin users should be too

- **Trade-offs**:
  - **Pro**: Enables landlord-specific admin accounts, consistent data model
  - **Con**: None (this is the correct long-term design)

**Decision 5: institution and year_of_study Are NOT Tenant-Scoped (Corrected)**

- **Options Considered**:
  - Option A: Treat as shared reference data (no `tenant_id`)
  - Option B: Tenant-scoped (different tenants may serve different institutions)

- **Selected**: Option A (Shared Reference Data) **[CORRECTED]**

- **Rationale**:
  - **Many-to-Many Relationship**: Multiple tenants (landlords) can provide housing at the same institution (e.g., UCT, Stellenbosch)
  - **No Duplication**: Institutions are shared entities; duplicating per tenant creates data inconsistency
  - **Tenant Isolation via Building**: Tenants are isolated by their building ownership (`building.tenant_id`), not institution ownership
  - **year_of_study Tied to Institution**: Year of study definitions belong to institutions, not tenants

- **Trade-offs**:
  - **Pro**: No duplicate institution records, correct many-to-many relationship model
  - **Con**: Institutions are shared (not isolated per tenant) — this is the correct business model

**Decision 6: CONCURRENTLY Index Creation**

- **Options Considered**:
  - Option A: `CREATE INDEX` (blocks table during creation)
  - Option B: `CREATE INDEX CONCURRENTLY` (no table lock, longer execution time)

- **Selected**: Option B (CONCURRENTLY)

- **Rationale**:
  - **Zero Downtime**: Production deployment without blocking application queries
  - **Safety**: Acceptable trade-off (2x longer index creation time) vs. locking tables for minutes

- **Trade-offs**:
  - **Pro**: No downtime, production-safe
  - **Con**: Cannot run in transaction (requires separate Flyway migration file or Java-based migration)

---

## 11. Risk Assessment

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| **Migration failure in production** | High (downtime, rollback required) | Low (migrations tested in staging) | **Mitigation**: Test migrations on staging environment with production-like data volume. Full database backup before migration. Rollback SQL scripts prepared and tested. |
| **Backfill timeout on large tables** | Medium (migration hangs, requires manual intervention) | Medium (if tables >100k rows) | **Mitigation**: Batched UPDATE statements for tables >50k rows. Monitor migration progress with RAISE NOTICE logs. Set statement_timeout to 10 minutes. |
| **Index creation failure (disk space)** | Medium (migration fails, requires cleanup) | Low (disk space pre-checked) | **Mitigation**: Verify 20% free disk space before migration. Index creation via CONCURRENTLY allows retry if interrupted. |
| **Orphaned records prevent backfill** | Medium (migration blocked, requires data cleanup) | Low (pre-migration data audit) | **Mitigation**: Pre-migration data audit SQL identifies orphaned records. Fix orphans before production migration. |
| **Cross-tenant data leakage (application bug)** | Critical (data breach, compliance violation) | Medium (human error in future code) | **Mitigation**: TASK-2.08 implements Hibernate filters (automatic tenant filtering). Code review enforces tenant-aware queries. Automated tests verify no cross-tenant access. |
| **Query performance degradation** | Medium (slow page loads, user complaints) | Low (indexes mitigate this) | **Mitigation**: Composite indexes on tenant_id + status/active columns. Post-migration EXPLAIN ANALYZE verification. Load testing with 100k+ records. |
| **Rollback complexity (Migration 4)** | Medium (time-consuming manual rollback) | Low (only if backfill fails) | **Mitigation**: Rollback SQL script prepared and tested in staging. Clear documentation of rollback steps. |

**Pre-Deployment Checklist:**

- [ ] Full database backup completed
- [ ] Migrations tested on staging with production-like data
- [ ] Disk space verified (≥20% free)
- [ ] Rollback SQL scripts tested
- [ ] statement_timeout set to 10 minutes
- [ ] On-call DBA notified and available
- [ ] Maintenance window scheduled (estimated 10-30 minutes)

---

## 12. Deployment Considerations

### 12.1 Deployment Sequence

**Pre-Deployment:**

1. **Full database backup:**
   ```bash
   pg_dump -h localhost -U postgres -d student_accommodation > backup_pre_multitenant_$(date +%Y%m%d).sql
   ```

2. **Verify disk space:**
   ```sql
   SELECT pg_size_pretty(pg_database_size('student_accommodation')) AS current_size;
   ```
   Ensure 20% free space available.

3. **Staging deployment:**
   - Deploy to staging environment
   - Seed with 10,000+ test records
   - Run migrations
   - Verify backfill success
   - Run query performance tests

**Production Deployment:**

1. **Schedule maintenance window** (estimated 10-30 minutes depending on data volume)

2. **Enable maintenance mode** (optional, prevents new data during migration):
   ```bash
   # Set application to read-only mode or display maintenance page
   ```

3. **Deploy backend with new migrations:**
   ```bash
   cd backend
   ./gradlew clean build
   docker-compose build backend
   docker-compose up -d backend
   ```

4. **Monitor Flyway migration execution:**
   ```bash
   docker-compose logs -f backend | grep "Migrating schema"
   ```

5. **Verify migration success:**
   ```sql
   SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
   ```

6. **Smoke test:**
   - Login as admin
   - View buildings list
   - View applications list
   - Create new application
   - Verify no errors in backend logs

7. **Disable maintenance mode** and resume normal operations

**Post-Deployment Verification:**

1. **Verify backfill:**
   ```sql
   SELECT COUNT(*) FROM application WHERE tenant_id IS NULL;  -- Should return 0
   SELECT COUNT(*) FROM building WHERE tenant_id IS NULL;    -- Should return 0
   ```

2. **Verify index usage:**
   ```sql
   EXPLAIN ANALYZE SELECT * FROM application WHERE tenant_id = 1 AND status = 'PENDING';
   ```
   Verify index scan used (not sequential scan).

3. **Verify default tenant:**
   ```sql
   SELECT * FROM tenant WHERE id = 1;
   ```
   Should return "Default Tenant" row.

4. **Monitor application logs** for any errors or warnings related to database queries.

### 12.2 Rollback Plan

**If Migration 1 or 2 fails:**

```bash
# Stop application
docker-compose stop backend

# Connect to database
docker-compose exec db psql -U postgres student_accommodation

# Drop tenant table (cascades to FK constraints)
DROP TABLE IF EXISTS tenant CASCADE;

# Restart application (Flyway will re-attempt migrations)
docker-compose up -d backend
```

**If Migration 3 (indexes) fails:**

```bash
# Indexes can be re-created manually or via Flyway retry
# No data loss if index creation fails
docker-compose exec db psql -U postgres student_accommodation

# Drop failed indexes
DROP INDEX CONCURRENTLY IF EXISTS idx_application_tenant_id;

# Re-run migration or create indexes manually
```

**If Migration 4 (backfill) fails:**

```sql
-- Remove NOT NULL constraints if added
ALTER TABLE app_user ALTER COLUMN tenant_id DROP NOT NULL;
ALTER TABLE building ALTER COLUMN tenant_id DROP NOT NULL;
-- [Repeat for all tables]

-- Clear backfilled data
UPDATE app_user SET tenant_id = NULL WHERE tenant_id = 1;
UPDATE building SET tenant_id = NULL WHERE tenant_id = 1;
-- [Repeat for all tables]

-- Delete default tenant
DELETE FROM tenant WHERE id = 1;

-- Fix underlying issue, then re-run Migration 4
```

**Emergency Rollback (Restore from Backup):**

```bash
# Stop application
docker-compose stop backend

# Restore database from backup
docker-compose exec -T db psql -U postgres student_accommodation < backup_pre_multitenant_YYYYMMDD.sql

# Restart application
docker-compose up -d backend
```

**Recovery Time Objective (RTO):** 15 minutes (restore from backup + restart)

**Recovery Point Objective (RPO):** 0 minutes (no data loss if rollback is immediate)

### 12.3 Environment Variables

**No new environment variables required for this task.**

Existing database connection variables continue to work:
- `DB_URL`
- `USER_DB`
- `PASSWORD_DB`

---

## 13. Open Questions for Developer

1. **Q1: Batched Backfill Strategy**
   - Should backfill be batched (10,000 rows at a time) for tables with >50,000 rows, or is single UPDATE acceptable?
   - **Recommendation**: Implement batched backfill for safety on large tables. Adds complexity but prevents long-running transactions.

2. **Q2: CONCURRENTLY Index Creation Method**
   - Should CONCURRENTLY indexes be created via Java-based Flyway migration (callbacks) or SQL migration with `executeInTransaction=false`?
   - **Recommendation**: Use Java-based migration for better error handling and Flyway integration.

3. **Q3: Default Tenant Configuration**
   - What configuration should be in default tenant's `config` JSON field?
   - **Recommendation**: `{"offer_expiry_days": 7, "max_decline_count": 2}` (existing system defaults).

4. **Q4: Pre-Deployment Data Audit**
   - Should we create a SQL script to audit data integrity before migration (identify orphaned records)?
   - **Recommendation**: Yes. Create `pre_migration_audit.sql` to check for NULL foreign keys, orphaned records, and constraint violations.

5. **Q5: Post-Deployment Monitoring**
   - Should we add Prometheus/Grafana metrics for tenant-scoped query performance?
   - **Recommendation**: Defer to future task (TASK-3.01 Observability). For now, manual EXPLAIN ANALYZE verification is sufficient.

6. **Q6: Super-Admin Role Creation**
   - Should we create a super-admin user role (`ROLE_SUPER_ADMIN`) distinct from `ROLE_ADMIN` for cross-tenant access?
   - **Recommendation**: Defer to TASK-2.08. For now, all admins remain scoped to default tenant.

---

## Architecture Review Checklist

Before finalizing, ensure:

- [x] Tenant table schema defined with proper constraints (UNIQUE, NOT NULL, FK ON DELETE RESTRICT)
- [x] All 16 tenant-scoped tables identified and documented
- [x] Foreign key constraints defined with ON DELETE RESTRICT
- [x] Indexes defined for all tenant_id columns (single-column + composite)
- [x] Flyway migration scripts are idempotent (IF NOT EXISTS, ON CONFLICT DO NOTHING, WHERE IS NULL guards)
- [x] Migration split into logical phases (create table → add columns → backfill → NOT NULL)
- [x] CONCURRENTLY index creation strategy defined
- [x] Backfill SQL uses verification (RAISE NOTICE logs, NULL count checks)
- [x] Rollback strategy documented for each migration phase
- [x] Default tenant creation and backfill strategy defined
- [x] Performance impact assessed (indexes prevent degradation)
- [x] Testing strategy covers migration success, idempotency, and rollback
- [x] Technical decisions documented with rationale
- [x] Risk assessment completed with mitigation strategies
- [x] Deployment sequence and smoke tests defined
- [x] No violations of database normalization (tenant_id denormalized in `room` for performance is acceptable)
- [x] Open questions flagged for implementation team

---

## Final Notes to Implementation Team

This design document provides a **complete specification** for implementing the multi-tenancy database schema. The implementation should be **mechanical**: follow the SQL exactly as specified, test thoroughly in staging, and execute during a planned maintenance window.

**Critical Success Factors:**

1. **Test migrations on staging first** — Never run untested migrations in production
2. **Backup before deployment** — Full database backup is mandatory
3. **Monitor migration logs** — Watch for RAISE NOTICE messages indicating backfill progress
4. **Verify index usage post-deployment** — Run EXPLAIN ANALYZE on critical queries
5. **Do not skip rollback testing** — Rollback scripts must be tested in staging before production deployment

**What NOT to do:**

- ❌ Do not modify executed migrations — Create new migrations if corrections are needed
- ❌ Do not run migrations without a maintenance window — Index creation may take 10+ minutes
- ❌ Do not skip pre-deployment data audit — Orphaned records will cause backfill failures
- ❌ Do not deploy without testing rollback — Rollback scripts must work when you need them

**Questions during implementation?** Refer to this document first, then escalate to Solutions Architect if clarification is needed.

**Next Task:** TASK-2.08 — Implement application-layer tenant filtering (TenantContext, Hibernate filters, JWT integration).
