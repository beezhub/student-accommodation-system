# Senior Full Stack Developer - Implementation Plan

You are a **Senior Full Stack Developer** implementing **TASK-2.07: Multi-Tenancy Database Schema**.

## Primary Focus

**This is a PURE DATABASE MIGRATION task** — No backend entities, services, or controllers. No frontend changes. Just Flyway migrations to add multi-tenancy schema support.

Application-layer changes (Hibernate filters, TenantContext, JWT integration) will be implemented in **TASK-2.08**.

---

## Summary

**Feature**: Multi-Tenancy Database Schema Foundation

**Scope**:
- Create `tenant` table
- Add `tenant_id` FK to 14 tenant-scoped tables
- Backfill existing data with default tenant
- Add indexes for query performance

**Estimated Total Effort**: **6-8 hours** (including testing and validation)

**Critical Path**: Migration 1 → Migration 2 → Migration 3 → Migration 4 → Testing → Validation

**Parallel Work Opportunities**: None (migrations must run sequentially)

---

## Task Breakdown

### Phase 1: Migration 1 - Create Tenant Table

#### Task 1.1: Create Tenant Table Migration
- **File**: `backend/src/main/resources/db/migration/V2026_02_15_1400__create_tenant_table.sql`
- **Estimated Effort**: 45 minutes
- **Dependencies**: None
- **Description**:
  - Create `tenant` table with all columns and constraints
  - Add UNIQUE constraints on `name` and `domain`
  - Add index on `is_active` for active tenant queries
  - Add table and column comments for documentation

- **Implementation Notes**:
  ```sql
  -- Columns to create:
  -- 1. id BIGSERIAL PRIMARY KEY
  -- 2. name VARCHAR(100) NOT NULL UNIQUE
  -- 3. domain VARCHAR(255) UNIQUE (nullable)
  -- 4. config JSONB NOT NULL DEFAULT '{}'
  -- 5. is_active BOOLEAN NOT NULL DEFAULT true
  -- 6. created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
  -- 7. updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

  -- Constraints:
  -- - uk_tenant_name UNIQUE (name)
  -- - uk_tenant_domain UNIQUE (domain)

  -- Indexes:
  -- - idx_tenant_is_active ON tenant(is_active)

  -- Idempotency:
  -- Use IF NOT EXISTS for table and index creation
  ```

- **SQL Template** (from solution-design.md):
  ```sql
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

  CREATE INDEX IF NOT EXISTS idx_tenant_is_active ON tenant(is_active);

  COMMENT ON TABLE tenant IS 'Accommodation providers (landlords) for multi-tenant SaaS deployment';
  COMMENT ON COLUMN tenant.config IS 'Tenant-specific configuration overrides (JSON): offer_expiry_days, max_decline_count, required_document_types';
  ```

- **Acceptance Criteria**:
  - [ ] Migration file created with correct naming convention
  - [ ] Table created with all 7 columns
  - [ ] UNIQUE constraints on `name` and `domain`
  - [ ] Index created on `is_active`
  - [ ] Migration uses IF NOT EXISTS guards for idempotency
  - [ ] Table and column comments added
  - [ ] Rollback SQL documented in comments

#### Task 1.2: Test Migration 1 Locally
- **Command**: `./gradlew bootRun` (or Docker: `docker-compose up --build backend`)
- **Estimated Effort**: 15 minutes
- **Dependencies**: Task 1.1
- **Description**:
  - Start application with clean database
  - Verify Flyway executes migration successfully
  - Check `flyway_schema_history` table for migration record
  - Verify `tenant` table exists with correct schema

- **Verification Commands**:
  ```sql
  -- Check migration executed
  SELECT * FROM flyway_schema_history WHERE version = '2026_02_15_1400';

  -- Check table exists
  \d tenant

  -- Verify constraints
  SELECT constraint_name, constraint_type
  FROM information_schema.table_constraints
  WHERE table_name = 'tenant';

  -- Verify index exists
  SELECT indexname FROM pg_indexes WHERE tablename = 'tenant';
  ```

- **Acceptance Criteria**:
  - [ ] Application starts without errors
  - [ ] Flyway log shows successful migration
  - [ ] `tenant` table exists in database
  - [ ] All constraints present (PK, 2x UNIQUE)
  - [ ] Index `idx_tenant_is_active` exists
  - [ ] No errors in application logs

#### Task 1.3: Test Migration 1 Idempotency
- **Estimated Effort**: 10 minutes
- **Dependencies**: Task 1.2
- **Description**:
  - Stop application
  - Manually re-run migration SQL via psql
  - Verify no errors (IF NOT EXISTS guards work)

- **Verification**:
  ```bash
  # Run migration SQL manually
  docker-compose exec db psql -U postgres student_accommodation -f backend/src/main/resources/db/migration/V2026_02_15_1400__create_tenant_table.sql

  # Should complete without errors
  ```

- **Acceptance Criteria**:
  - [ ] Manual re-execution completes without errors
  - [ ] No duplicate tables or indexes created
  - [ ] Schema remains consistent

---

### Phase 2: Migration 2 - Add Tenant ID Columns

#### Task 2.1: Create Tenant ID Columns Migration
- **File**: `backend/src/main/resources/db/migration/V2026_02_15_1410__add_tenant_id_columns.sql`
- **Estimated Effort**: 1.5 hours
- **Dependencies**: Task 1.3
- **Description**:
  - Add `tenant_id BIGINT` column to 14 tenant-scoped tables
  - Add FK constraints with ON DELETE RESTRICT
  - Add column comments
  - **Columns are nullable initially** (to allow backfill in Migration 4)

- **Implementation Notes**:
  ```sql
  -- Tables to modify (14 tables):
  -- 1. app_user
  -- 2. building
  -- 3. room
  -- 4. application
  -- 5. room_allocation
  -- 6. staged_allocation
  -- 7. document
  -- 8. notification
  -- 9. user_notification
  -- 10. system_setting
  -- 11. allocation_rule
  -- 12. waiting_list_entry
  -- 13. activity_log
  -- 14. academic_year

  -- For each table:
  -- ALTER TABLE {table} ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
  -- ALTER TABLE {table} ADD CONSTRAINT fk_{table}_tenant
  --     FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;
  -- COMMENT ON COLUMN {table}.tenant_id IS '...';

  -- Tables NOT receiving tenant_id:
  -- - institution (SHARED reference data)
  -- - year_of_study (SHARED reference data)
  -- - student (inherits from application)
  -- - accommodation_type (SHARED)
  ```

- **SQL Template** (Partial - repeat pattern for all 14 tables):
  ```sql
  -- Add tenant_id columns (nullable initially)
  ALTER TABLE app_user ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
  ALTER TABLE building ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
  ALTER TABLE room ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
  -- ... (repeat for all 14 tables)

  -- Add FK constraints
  ALTER TABLE app_user
      ADD CONSTRAINT fk_app_user_tenant
      FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;

  ALTER TABLE building
      ADD CONSTRAINT fk_building_tenant
      FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT;
  -- ... (repeat for all 14 tables)

  -- Add comments
  COMMENT ON COLUMN app_user.tenant_id IS 'FK to tenant table; admin users scoped to tenant';
  COMMENT ON COLUMN building.tenant_id IS 'FK to tenant table; building owner (buildings also have institution_id for shared reference)';
  -- ... (repeat for all 14 tables)
  ```

- **Acceptance Criteria**:
  - [ ] Migration file created with correct naming
  - [ ] 14 tables receive `tenant_id BIGINT` column
  - [ ] 14 FK constraints added with ON DELETE RESTRICT
  - [ ] All columns are NULLABLE (for backfill)
  - [ ] IF NOT EXISTS guards used for idempotency
  - [ ] Column comments added for documentation
  - [ ] Rollback SQL documented in comments
  - [ ] **Verify institution and year_of_study NOT modified**

#### Task 2.2: Test Migration 2 Locally
- **Command**: `./gradlew bootRun`
- **Estimated Effort**: 20 minutes
- **Dependencies**: Task 2.1
- **Description**:
  - Start application (Flyway runs migrations)
  - Verify Migration 2 executes successfully
  - Check all 14 tables have `tenant_id` column
  - Verify FK constraints exist

- **Verification Commands**:
  ```sql
  -- Check migration executed
  SELECT * FROM flyway_schema_history WHERE version = '2026_02_15_1410';

  -- Verify tenant_id columns exist (sample check)
  \d app_user
  \d building
  \d application

  -- Verify FK constraints exist
  SELECT
      tc.table_name,
      tc.constraint_name,
      tc.constraint_type
  FROM information_schema.table_constraints tc
  WHERE tc.constraint_type = 'FOREIGN KEY'
    AND tc.constraint_name LIKE 'fk_%_tenant'
  ORDER BY tc.table_name;
  -- Should return 14 rows

  -- Verify institution and year_of_study unchanged
  \d institution
  \d year_of_study
  -- Should NOT have tenant_id column
  ```

- **Acceptance Criteria**:
  - [ ] Application starts without errors
  - [ ] Migration 2 logged as successful
  - [ ] All 14 tables have `tenant_id` column
  - [ ] All 14 FK constraints exist
  - [ ] `tenant_id` columns are NULLABLE
  - [ ] institution and year_of_study do NOT have `tenant_id`
  - [ ] No application errors

---

### Phase 3: Migration 3 - Add Indexes CONCURRENTLY

#### Task 3.1: Create Index Migration (CONCURRENTLY)
- **File**: `backend/src/main/resources/db/migration/V2026_02_15_1411__add_tenant_id_indexes.sql`
- **Estimated Effort**: 1 hour
- **Dependencies**: Task 2.2
- **Description**:
  - Add single-column B-tree indexes on all 14 `tenant_id` columns
  - Add 4 composite indexes for common query patterns
  - Use CREATE INDEX CONCURRENTLY for zero-downtime deployment
  - **Important**: CONCURRENTLY cannot run in transaction

- **Implementation Notes**:
  ```sql
  -- Single-column indexes (14 total):
  -- - idx_app_user_tenant_id ON app_user(tenant_id)
  -- - idx_building_tenant_id ON building(tenant_id)
  -- - idx_room_tenant_id ON room(tenant_id)
  -- - ... (repeat for all 14 tables)

  -- Composite indexes (4 total):
  -- - idx_application_tenant_status ON application(tenant_id, status)
  -- - idx_building_tenant_active ON building(tenant_id, is_active)
  -- - idx_room_tenant_building ON room(tenant_id, building_id)
  -- - idx_allocation_tenant_status ON room_allocation(tenant_id, status)

  -- Use CONCURRENTLY for production safety
  -- Add IF NOT EXISTS guards
  ```

- **SQL Template**:
  ```sql
  -- Single-column indexes
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

  -- Composite indexes
  CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_application_tenant_status ON application(tenant_id, status);
  CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_building_tenant_active ON building(tenant_id, is_active);
  CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_room_tenant_building ON room(tenant_id, building_id);
  CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_allocation_tenant_status ON room_allocation(tenant_id, status);
  ```

- **Special Configuration**:
  - **Option 1 (Recommended)**: Configure Flyway to skip transaction for this migration
  - **Option 2**: Run indexes manually via psql after main migrations
  - **Option 3**: Convert to Java-based Flyway migration (see solution-design.md)

- **Acceptance Criteria**:
  - [ ] Migration file created with CONCURRENTLY syntax
  - [ ] 14 single-column indexes defined
  - [ ] 4 composite indexes defined
  - [ ] IF NOT EXISTS guards used
  - [ ] Migration header notes CONCURRENTLY limitation
  - [ ] Rollback SQL documented

#### Task 3.2: Configure Flyway for CONCURRENTLY Migration
- **File**: `application.yml` or Flyway config
- **Estimated Effort**: 30 minutes
- **Dependencies**: Task 3.1
- **Description**:
  - Configure Flyway to run Migration 3 without transaction
  - **OR** manually execute Migration 3 via psql

- **Configuration Option 1** (Callback approach):
  Create Java-based callback:
  ```java
  // backend/src/main/java/com/beezhub/student_accommodation/config/FlywayCallback.java
  @Component
  public class FlywayCallback implements Callback {
      @Override
      public boolean handles(Event event, Context context) {
          return event == Event.AFTER_MIGRATE;
      }

      @Override
      public void handle(Event event, Context context) {
          // Execute CONCURRENTLY indexes if Migration 3 not yet applied
      }
  }
  ```

- **Configuration Option 2** (Manual execution):
  ```bash
  # Stop application
  docker-compose stop backend

  # Run index creation manually
  docker-compose exec db psql -U postgres student_accommodation -f backend/src/main/resources/db/migration/V2026_02_15_1411__add_tenant_id_indexes.sql

  # Restart application
  docker-compose up -d backend
  ```

- **Acceptance Criteria**:
  - [ ] Index migration executes successfully
  - [ ] No transaction conflicts
  - [ ] Flyway logs show successful execution
  - [ ] All 18 indexes created (14 single + 4 composite)

#### Task 3.3: Verify Index Creation
- **Estimated Effort**: 15 minutes
- **Dependencies**: Task 3.2
- **Description**:
  - Verify all indexes exist
  - Run EXPLAIN ANALYZE to confirm index usage

- **Verification Commands**:
  ```sql
  -- Check all tenant_id indexes exist
  SELECT tablename, indexname
  FROM pg_indexes
  WHERE indexname LIKE '%tenant_id%'
  ORDER BY tablename;
  -- Should return 18 rows (14 single + 4 composite)

  -- Verify index usage (example)
  EXPLAIN ANALYZE
  SELECT * FROM application
  WHERE tenant_id = 1 AND status = 'PENDING';
  -- Should show "Index Scan using idx_application_tenant_status"
  ```

- **Acceptance Criteria**:
  - [ ] All 18 indexes exist
  - [ ] EXPLAIN shows index scans (not sequential scans)
  - [ ] Index naming follows convention: `idx_{table}_tenant_id`

---

### Phase 4: Migration 4 - Backfill Default Tenant

#### Task 4.1: Create Backfill Migration
- **File**: `backend/src/main/resources/db/migration/V2026_02_15_1420__backfill_default_tenant.sql`
- **Estimated Effort**: 2 hours
- **Dependencies**: Task 3.3
- **Description**:
  - Insert "Default Tenant" with id=1
  - Backfill all 14 tables: `UPDATE ... SET tenant_id = 1 WHERE tenant_id IS NULL`
  - Verify no NULL tenant_id values remain
  - Add NOT NULL constraints to all 14 `tenant_id` columns
  - Log backfill row counts with RAISE NOTICE

- **Implementation Notes**:
  ```sql
  -- Step 1: Insert default tenant (idempotent)
  INSERT INTO tenant (id, name, config, is_active)
  VALUES (1, 'Default Tenant', '{"offer_expiry_days": 7, "max_decline_count": 2}'::jsonb, true)
  ON CONFLICT (name) DO NOTHING;

  -- Step 2: Backfill all tables (idempotent via WHERE tenant_id IS NULL)
  DO $$
  DECLARE
      default_tenant_id BIGINT;
      row_count_app_user BIGINT;
      -- ... (declare row count variables for all 14 tables)
  BEGIN
      SELECT id INTO default_tenant_id FROM tenant WHERE name = 'Default Tenant';

      UPDATE app_user SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
      GET DIAGNOSTICS row_count_app_user = ROW_COUNT;
      -- ... (repeat for all 14 tables)

      RAISE NOTICE 'Backfill completed for default tenant (ID: %)', default_tenant_id;
      RAISE NOTICE '  app_user: % rows updated', row_count_app_user;
      -- ... (log all 14 row counts)
  END $$;

  -- Step 3: Verify no NULLs remain
  DO $$
  DECLARE null_count INT;
  BEGIN
      SELECT COUNT(*) INTO null_count FROM app_user WHERE tenant_id IS NULL;
      IF null_count > 0 THEN
          RAISE EXCEPTION 'app_user has % rows with NULL tenant_id', null_count;
      END IF;
      -- ... (repeat for all 14 tables)
  END $$;

  -- Step 4: Add NOT NULL constraints
  ALTER TABLE app_user ALTER COLUMN tenant_id SET NOT NULL;
  ALTER TABLE building ALTER COLUMN tenant_id SET NOT NULL;
  -- ... (repeat for all 14 tables)
  ```

- **Acceptance Criteria**:
  - [ ] Migration file created
  - [ ] Default tenant inserted with id=1
  - [ ] Idempotent insert (ON CONFLICT DO NOTHING)
  - [ ] Backfill UPDATEs for all 14 tables
  - [ ] Idempotent backfill (WHERE tenant_id IS NULL)
  - [ ] Row count logging with RAISE NOTICE
  - [ ] NULL verification for all 14 tables
  - [ ] NOT NULL constraints added to all 14 columns
  - [ ] Rollback SQL documented in comments

#### Task 4.2: Test Migration 4 Locally
- **Command**: `./gradlew bootRun`
- **Estimated Effort**: 30 minutes
- **Dependencies**: Task 4.1
- **Description**:
  - Start application with existing test data
  - Verify Migration 4 executes successfully
  - Check all existing records have `tenant_id = 1`
  - Verify NOT NULL constraints applied

- **Verification Commands**:
  ```sql
  -- Check migration executed
  SELECT * FROM flyway_schema_history WHERE version = '2026_02_15_1420';

  -- Verify default tenant exists
  SELECT * FROM tenant WHERE id = 1;

  -- Check backfill results (sample tables)
  SELECT COUNT(*), tenant_id FROM app_user GROUP BY tenant_id;
  SELECT COUNT(*), tenant_id FROM building GROUP BY tenant_id;
  SELECT COUNT(*), tenant_id FROM application GROUP BY tenant_id;
  -- All should show tenant_id = 1

  -- Verify no NULLs
  SELECT COUNT(*) FROM app_user WHERE tenant_id IS NULL;  -- Should be 0
  SELECT COUNT(*) FROM building WHERE tenant_id IS NULL;  -- Should be 0

  -- Verify NOT NULL constraints
  \d app_user
  -- tenant_id column should show "not null"
  ```

- **Acceptance Criteria**:
  - [ ] Application starts successfully
  - [ ] Migration 4 completed
  - [ ] Default tenant exists (id=1, name='Default Tenant')
  - [ ] All existing records have tenant_id = 1
  - [ ] Zero NULL tenant_id values across all 14 tables
  - [ ] NOT NULL constraints applied
  - [ ] RAISE NOTICE logs show row counts
  - [ ] No application errors

#### Task 4.3: Test Migration 4 Idempotency
- **Estimated Effort**: 20 minutes
- **Dependencies**: Task 4.2
- **Description**:
  - Stop application
  - Manually re-run Migration 4 SQL
  - Verify no errors or duplicate data

- **Verification**:
  ```bash
  docker-compose stop backend
  docker-compose exec db psql -U postgres student_accommodation -f backend/src/main/resources/db/migration/V2026_02_15_1420__backfill_default_tenant.sql
  # Should complete without errors (ON CONFLICT, WHERE IS NULL guards)
  ```

- **Acceptance Criteria**:
  - [ ] Manual re-execution completes without errors
  - [ ] No duplicate tenant records
  - [ ] Row counts remain consistent
  - [ ] No constraint violations

---

### Phase 5: Integration Testing

#### Task 5.1: Test Complete Migration Sequence
- **Estimated Effort**: 1 hour
- **Dependencies**: Task 4.3
- **Description**:
  - Drop database and re-create from scratch
  - Run all 4 migrations via Flyway
  - Verify final schema matches design

- **Test Procedure**:
  ```bash
  # Clean database
  docker-compose down -v
  docker-compose up -d db

  # Start backend (runs all migrations)
  docker-compose up --build backend

  # Verify migrations
  docker-compose exec db psql -U postgres student_accommodation -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"
  ```

- **Acceptance Criteria**:
  - [ ] Clean database deployment succeeds
  - [ ] All 4 migrations execute in order
  - [ ] Application starts without errors
  - [ ] Schema matches solution-design specification
  - [ ] 44+ total migrations (existing + 4 new)

#### Task 5.2: Test Migration with Existing Data
- **Estimated Effort**: 1 hour
- **Dependencies**: Task 5.1
- **Description**:
  - Use existing database with test data
  - Run migrations
  - Verify all existing data preserved
  - Verify all existing data assigned to default tenant

- **Test Data Setup**:
  ```sql
  -- Before migrations, insert test data
  INSERT INTO app_user (username, password, role, email, email_verified)
  VALUES ('admin@test.com', 'password', 'ADMIN', 'admin@test.com', true);

  INSERT INTO building (name, address, total_floors, is_active)
  VALUES ('Test Building', '123 Main St', 5, true);

  -- Then run migrations and verify tenant_id = 1 assigned
  ```

- **Acceptance Criteria**:
  - [ ] All existing records preserved
  - [ ] All existing records have tenant_id = 1
  - [ ] No data loss
  - [ ] FK constraints enforced

#### Task 5.3: Test Application Functionality
- **Estimated Effort**: 45 minutes
- **Dependencies**: Task 5.2
- **Description**:
  - Login as admin
  - Create new building (should work despite tenant_id)
  - Create new application
  - Verify no errors in backend logs
  - **Note**: Application code does not yet enforce tenant filtering (TASK-2.08)

- **Manual Test Scenarios**:
  1. Admin login → Dashboard
  2. Buildings → Create new building
  3. Applications → View applications
  4. Documents → View documents
  5. Check backend logs for errors

- **Acceptance Criteria**:
  - [ ] Application functions normally
  - [ ] No errors when creating new records
  - [ ] New records automatically get tenant_id (NULL or error expected — application code unaware of tenant_id)
  - [ ] **Expected behavior**: New records fail FK constraint or get NULL tenant_id (application layer not updated yet)
  - [ ] Logs show FK constraint violation errors for new records (acceptable — TASK-2.08 will fix)

#### Task 5.4: Performance Testing (Query Verification)
- **Estimated Effort**: 30 minutes
- **Dependencies**: Task 5.2
- **Description**:
  - Run EXPLAIN ANALYZE on key queries
  - Verify indexes are used
  - Confirm query performance <100ms

- **Test Queries**:
  ```sql
  -- Test 1: Application query with tenant_id + status
  EXPLAIN ANALYZE
  SELECT * FROM application
  WHERE tenant_id = 1 AND status = 'PENDING'
  LIMIT 20;
  -- Expected: Index Scan using idx_application_tenant_status

  -- Test 2: Building query with tenant_id + is_active
  EXPLAIN ANALYZE
  SELECT * FROM building
  WHERE tenant_id = 1 AND is_active = true;
  -- Expected: Index Scan using idx_building_tenant_active

  -- Test 3: Room query with tenant_id + building_id
  EXPLAIN ANALYZE
  SELECT * FROM room
  WHERE tenant_id = 1 AND building_id = 5;
  -- Expected: Index Scan using idx_room_tenant_building
  ```

- **Acceptance Criteria**:
  - [ ] All queries use index scans
  - [ ] Query execution time <100ms
  - [ ] No sequential scans on tenant-scoped queries
  - [ ] Composite indexes used where expected

---

### Phase 6: Rollback Testing & Documentation

#### Task 6.1: Test Rollback Procedures
- **Estimated Effort**: 1 hour
- **Dependencies**: Task 5.4
- **Description**:
  - Document and test rollback SQL for each migration
  - Verify rollback restores database to previous state

- **Rollback SQL** (from solution-design.md):
  ```sql
  -- Rollback Migration 4
  ALTER TABLE app_user ALTER COLUMN tenant_id DROP NOT NULL;
  ALTER TABLE building ALTER COLUMN tenant_id DROP NOT NULL;
  -- ... (all 14 tables)

  UPDATE app_user SET tenant_id = NULL WHERE tenant_id = 1;
  UPDATE building SET tenant_id = NULL WHERE tenant_id = 1;
  -- ... (all 14 tables)

  DELETE FROM tenant WHERE id = 1;

  -- Rollback Migration 3
  DROP INDEX CONCURRENTLY IF EXISTS idx_app_user_tenant_id;
  DROP INDEX CONCURRENTLY IF EXISTS idx_building_tenant_id;
  -- ... (all 18 indexes)

  -- Rollback Migration 2
  ALTER TABLE app_user DROP CONSTRAINT IF EXISTS fk_app_user_tenant;
  ALTER TABLE app_user DROP COLUMN IF EXISTS tenant_id;
  -- ... (all 14 tables)

  -- Rollback Migration 1
  DROP TABLE IF EXISTS tenant CASCADE;
  ```

- **Acceptance Criteria**:
  - [ ] Rollback SQL documented in each migration file
  - [ ] Rollback tested in local environment
  - [ ] Database restored to pre-migration state
  - [ ] Application starts successfully after rollback

#### Task 6.2: Update Documentation
- **Files**:
  - `Architecture.md` (if needed)
  - `CLAUDE.md` (if needed)
- **Estimated Effort**: 30 minutes
- **Dependencies**: Task 6.1
- **Description**:
  - Document multi-tenancy schema in architecture docs
  - Note that application-layer changes are in TASK-2.08
  - Update migration count (44 → 48 migrations)

- **Documentation Updates**:
  ```markdown
  ## Multi-Tenancy Support

  The system uses shared-schema multi-tenancy:
  - `tenant` table represents accommodation providers (landlords)
  - 14 tenant-scoped tables have `tenant_id` FK
  - `institution` and `year_of_study` are SHARED (not tenant-scoped)
  - Application-layer tenant filtering implemented in TASK-2.08
  ```

- **Acceptance Criteria**:
  - [ ] Architecture.md updated with multi-tenancy section
  - [ ] Migration count updated
  - [ ] Tenant-scoped vs. shared tables documented

#### Task 6.3: Create Deployment Checklist
- **File**: `deployment-checklist-task-2-07.md` (or include in PR description)
- **Estimated Effort**: 30 minutes
- **Dependencies**: All tasks
- **Description**:
  - Create pre-deployment checklist
  - Create deployment procedure
  - Create post-deployment verification

- **Deployment Checklist**:
  ```markdown
  ## Pre-Deployment
  - [ ] Full database backup completed
  - [ ] Migrations tested on staging with production-like data
  - [ ] Disk space verified (≥20% free)
  - [ ] Rollback SQL scripts prepared
  - [ ] Maintenance window scheduled

  ## Deployment
  - [ ] Enable maintenance mode (optional)
  - [ ] Deploy backend with new migrations
  - [ ] Monitor Flyway logs
  - [ ] Verify all 4 migrations succeed

  ## Post-Deployment Verification
  - [ ] Check flyway_schema_history (4 new entries)
  - [ ] Verify tenant table exists
  - [ ] Verify all 14 tables have tenant_id
  - [ ] Verify default tenant (id=1) exists
  - [ ] Run EXPLAIN ANALYZE on key queries
  - [ ] Test admin login and dashboard
  - [ ] Check for errors in logs

  ## Rollback (if needed)
  - [ ] Stop application
  - [ ] Execute rollback SQL
  - [ ] Verify database restored
  - [ ] Restart application
  ```

- **Acceptance Criteria**:
  - [ ] Deployment checklist created
  - [ ] Rollback procedure documented
  - [ ] Verification queries listed

---

## Task Dependency Graph

```
Phase 1 (Migration 1)
    1.1 → 1.2 → 1.3

Phase 2 (Migration 2)
    1.3 → 2.1 → 2.2

Phase 3 (Migration 3)
    2.2 → 3.1 → 3.2 → 3.3

Phase 4 (Migration 4)
    3.3 → 4.1 → 4.2 → 4.3

Phase 5 (Integration Testing)
    4.3 → 5.1 → 5.2 → 5.3 → 5.4

Phase 6 (Rollback & Docs)
    5.4 → 6.1 → 6.2, 6.3
```

**Critical Path**: Sequential (no parallel work possible for database migrations)

---

## Effort Summary by Phase

| Phase | Total Effort | Tasks |
|-------|--------------|-------|
| Phase 1: Migration 1 (Tenant Table) | 1.25 hrs | 3 |
| Phase 2: Migration 2 (Tenant ID Columns) | 1.75 hrs | 2 |
| Phase 3: Migration 3 (Indexes) | 1.75 hrs | 3 |
| Phase 4: Migration 4 (Backfill) | 2.75 hrs | 3 |
| Phase 5: Integration Testing | 3.25 hrs | 4 |
| Phase 6: Rollback & Documentation | 2 hrs | 3 |
| **TOTAL** | **12.75 hrs** | **18 tasks** |

**Realistic Estimate**: 10-14 hours (includes debugging, testing iterations)

---

## Pre-Implementation Checklist

Before starting:
- [ ] Business requirements approved (business-analysis.md)
- [ ] Technical design reviewed (solution-design.md)
- [ ] Development database accessible
- [ ] Flyway version confirmed (9.x)
- [ ] PostgreSQL version confirmed (16.2)
- [ ] Backup strategy confirmed
- [ ] Rollback plan reviewed

---

## Post-Implementation Checklist

After completing all tasks:
- [ ] All 4 migrations executed successfully
- [ ] Flyway schema history shows 4 new entries
- [ ] Tenant table exists with correct schema
- [ ] 14 tables have tenant_id columns
- [ ] All 18 indexes created
- [ ] Default tenant (id=1) exists
- [ ] All existing data has tenant_id = 1
- [ ] Zero NULL tenant_id values
- [ ] NOT NULL constraints applied
- [ ] Query performance verified (<100ms)
- [ ] Rollback procedures tested
- [ ] Documentation updated
- [ ] Deployment checklist created
- [ ] Ready for code review

---

## Known Limitations (Post-Migration)

After this task completes:
1. **Application code does NOT yet enforce tenant filtering** — TASK-2.08 will add:
   - `TenantContext` (ThreadLocal tenant tracking)
   - Hibernate `@Filter` annotations
   - JWT `tenantId` claim
   - Automatic tenant filtering on queries

2. **New records will fail FK constraint** — Application code unaware of `tenant_id`, so new inserts will fail:
   ```
   ERROR: null value in column "tenant_id" violates not-null constraint
   ```
   This is **expected behavior**. TASK-2.08 will fix by auto-setting `tenant_id` from `TenantContext`.

3. **No tenant management UI** — Tenant CRUD endpoints added in future tasks.

4. **Manual tenant ID assignment** — Until TASK-2.08, all operations manual via SQL.

---

## Next Steps (After This Task)

1. **TASK-2.08a**: Implement `TenantContext` and JWT integration
2. **TASK-2.08b**: Add Hibernate tenant filters to all entities
3. **TASK-2.08c**: Update admin endpoints for tenant scoping

---

**Note**: This is a **database-only task**. All migrations must be idempotent, safe, and thoroughly tested before production deployment. Follow the deployment checklist rigorously.
