# Task List — Student Accommodation System
## Technical Stabilisation & Architecture Recovery

> **Generated:** 2026-02-23 | **Source:** Architecture.md v1.0 + Requirements.md v2.0
> **Branch Assessed:** `feature/verification-report-fixes`
> **Living Document:** Update task statuses as work progresses. Never delete DONE tasks — they form the audit trail.

---

## Status Dashboard

| Phase | Total | Done | In Progress | Todo | Blocked | Progress |
|-------|-------|------|-------------|------|---------|----------|
| Phase 1: Critical Stabilisation | 8 | 8 | 0 | 0 | 0 | 100% |
| Phase 2: Structural Improvement | 13 | 12 | 0 | 1 | 0 | 92% |
| Phase 3: Enhancement & Optimisation | 13 | 0 | 0 | 13 | 0 | 0% |
| **Total** | **34** | **20** | **0** | **14** | **0** | **59%** |

**Current Blockers:** None — Phase 1 gate passed. Phase 2 is now open.

**Next Recommended Tasks (start here):**
- `TASK-2.04` — Increase HikariCP Connection Pool Size *(single config change, zero risk)*
- `TASK-2.05` — Add intake_period Table Migration *(new table, no existing code touched)*
- `TASK-2.06` — Add building_owner and building_staff Tables Migration *(new tables)*
- `TASK-2.07` — Add custom_application_field and custom_field_answer Tables Migration *(new tables)*
- `TASK-2.08` — Add required_document_type Table Migration *(new table)*
- `TASK-2.09` — Extract ScoringStrategy Interface *(refactor, parallel-safe)*
- `TASK-2.12` — Establish Frontend Test Coverage Baseline *(independent of all backend tasks)*

> ✅ **Phase 1 Gate PASSED** — All TASK-1.01 through TASK-1.08 are DONE. CI passing.

> ⚠️ **Phase 2 Dependency Order:**
> - `TASK-2.10` depends on `TASK-2.09`
> - `TASK-2.11` depends on `TASK-2.09` and `TASK-2.10`
> - `TASK-2.06` depends on `TASK-2.03` (v2.0 roles needed)
> - `TASK-2.13` depends on `TASK-2.01`, `TASK-2.02`, `TASK-2.05`–`TASK-2.08`

> 🔒 **Phase 3 is locked** until all Phase 2 tasks are DONE.

---

## Phase 1: Critical Stabilisation

> *These items block all other work or represent immediate production risk. Complete before any v2.0 feature development. All items are parallel except TASK-1.07, which depends on TASK-1.01.*

---

### TASK-1.01: Disable Hibernate DDL Auto-Management

- **Status:** DONE
- **Priority:** CRITICAL
- **Phase:** 1
- **Estimated Scope:** S (< 2 hrs)
- **Architecture Reference:** [ARCH 3.2.1], [ARCH ADR-005]
- **Requirement References:** N/A
- **Risk References:** [ARCH RISK-001]
- **Dependencies:** None
- **Blocked By:** None

**Description:**
Change `spring.jpa.properties.hibernate.hbm2ddl.auto` from `update` to `validate` in `application.yml`. This eliminates the dual schema management conflict where Hibernate silently modifies the schema alongside Flyway, creating untracked drift. With `validate`, Hibernate will check that the schema matches entity annotations on every startup and fail fast if they diverge — but will make no DDL changes. Flyway becomes the sole schema manager.

After the change, run the full integration test suite. If any `SchemaManagementException` appears at startup, it means Hibernate previously added columns that are not tracked in any Flyway migration. In that case, write a new Flyway migration to capture those changes before merging. This surfacing of hidden drift is the desired fast-feedback outcome.

**Files to Touch:**
- `backend/src/main/resources/application.yml` — Change line `hbm2ddl: auto: update` to `hbm2ddl: auto: validate`

**Implementation Notes:**
- The path in `application.yml` is under `spring.jpa.properties.hibernate.hbm2ddl.auto` (line 24 per the architecture assessment)
- Do NOT set `validate` only for test profile — it must apply to all profiles. Tests that create an in-memory H2 database are typically configured separately in `application-test.yml`
- If you use `spring.jpa.hibernate.ddl-auto` (the Spring shorthand) AND `spring.jpa.properties.hibernate.hbm2ddl.auto` (the raw Hibernate property), both must be updated — the raw Hibernate property takes precedence
- After the change: `./gradlew bootRun` must start without `SchemaManagementException`
- [ARCH 5.1.3] Coding standard: Migrations are immutable once applied — if drift is found, write a forward migration, do not modify existing ones

**Acceptance Criteria:**
- [ ] `application.yml` shows `hbm2ddl: auto: validate` (not `update`, `create`, or `create-drop`)
- [ ] Application starts successfully with `validate` mode — no `SchemaManagementException` in startup logs
- [ ] All existing tests pass (`./gradlew test`)
- [ ] CI pipeline includes application startup verification step

**Tests to Write:**
- No new unit tests required — the acceptance criterion is application startup success
- Verify: `./gradlew test` passes without schema validation failures

---

### TASK-1.02: Fix room.room_number Uniqueness Scope

- **Status:** DONE
- **Priority:** CRITICAL
- **Phase:** 1
- **Estimated Scope:** S (< 2 hrs)
- **Architecture Reference:** [ARCH 3.2.3]
- **Requirement References:** [REQ BM-003]
- **Risk References:** [ARCH RISK-002]
- **Dependencies:** None
- **Blocked By:** None

**Description:**
The `room` table has a global `UNIQUE (room_number)` constraint, meaning only one room numbered "101" can exist on the entire platform. When a second building owner creates a room numbered "101", the database returns a constraint violation that surfaces as an unhandled 500 error. This is a **certain production failure** — it will manifest on the day the second tenant onboards.

Write a Flyway migration that:
1. Drops the global unique constraint: `ALTER TABLE room DROP CONSTRAINT IF EXISTS room_room_number_key;`
2. Adds a per-building composite constraint: `ALTER TABLE room ADD CONSTRAINT uq_room_number_building UNIQUE (building_id, room_number);`

Then update the `Room` entity's `@Table` annotation to reflect the new composite uniqueness, if it specifies `uniqueConstraints`. Finally, update the `RoomService` duplicate-check logic: the constraint now applies per-building, so the `409 CONFLICT` error message should read "A room with this number already exists in this building" — not "across the platform."

**Files to Touch:**
- `backend/src/main/resources/db/migration/V2026_02_24_0001__fix_room_number_unique_per_building.sql` — New migration (create this file)
- `backend/src/main/java/com/beezhub/student_accommodation/model/entity/Room.java` — Update `@Table(uniqueConstraints=...)` if present
- `backend/src/main/java/com/beezhub/student_accommodation/service/RoomService.java` — Update duplicate-check query to scope by `building_id`

**Implementation Notes:**
- Migration naming convention per [ARCH 5.1.3]: `V{YYYY_MM_DD_HHMM}__snake_case_description.sql`
- Constraint naming convention: `uq_{table}_{columns}` → `uq_room_number_building`
- No data conflict risk: existing rooms across different buildings with the same number will satisfy the new per-building constraint (building_id differs)
- Risk flag: if existing rooms in the **same** building have duplicate numbers, the new `UNIQUE (building_id, room_number)` will also fail to add. Run `SELECT building_id, room_number, COUNT(*) FROM room GROUP BY building_id, room_number HAVING COUNT(*) > 1;` before applying the migration
- The `RoomService` 409 response message needs a wording update for clarity

**Acceptance Criteria:**
- [ ] New Flyway migration file exists and is correctly named
- [ ] Migration applies cleanly (`./gradlew bootRun` succeeds with `validate` mode)
- [ ] Two different buildings can each have a room numbered "101"
- [ ] Attempting to create a duplicate room number **within the same building** returns 409 CONFLICT
- [ ] Global constraint `room_room_number_key` no longer exists in the database
- [ ] Composite constraint `uq_room_number_building` is present

**Tests to Write:**
- `RoomServiceTest`: `createRoom_withDuplicateNumberInDifferentBuilding_succeeds()`
- `RoomServiceTest`: `createRoom_withDuplicateNumberInSameBuilding_returns409()`

---

### TASK-1.03: Fix system_settings.setting_key Uniqueness Scope

- **Status:** DONE
- **Priority:** CRITICAL
- **Phase:** 1
- **Estimated Scope:** S (< 2 hrs)
- **Architecture Reference:** [ARCH 3.2.4]
- **Requirement References:** [REQ CFG-002]
- **Risk References:** [ARCH RISK-013]
- **Dependencies:** None
- **Blocked By:** None

**Description:**
The `system_settings` table has a global `UNIQUE (setting_key)` constraint. Only one tenant can ever have a setting key like `auto_allocation_enabled`. The second building owner to configure their settings will hit a constraint violation. This is a **certain multi-tenant failure** the moment the second tenant is onboarded.

Write a Flyway migration that:
1. Drops the global constraint: `ALTER TABLE system_settings DROP CONSTRAINT IF EXISTS system_settings_setting_key_key;`
2. Adds a per-tenant composite constraint: `ALTER TABLE system_settings ADD CONSTRAINT uq_system_settings_tenant_key UNIQUE (tenant_id, setting_key);`

Update `SystemSettingService` to scope all setting lookups by `tenant_id` (this should already be the case given Hibernate's tenant filter, but verify explicitly).

**Files to Touch:**
- `backend/src/main/resources/db/migration/V2026_02_24_0002__fix_system_settings_unique_per_tenant.sql` — New migration (create this file)
- `backend/src/main/java/com/beezhub/student_accommodation/model/entity/SystemSetting.java` — Update `@Table(uniqueConstraints=...)` if present
- `backend/src/main/java/com/beezhub/student_accommodation/service/SystemSettingService.java` — Verify all lookups are tenant-scoped

**Implementation Notes:**
- Constraint name per convention: `uq_system_settings_tenant_key`
- The existing global constraint's actual name may vary — check with `SELECT constraint_name FROM information_schema.table_constraints WHERE table_name = 'system_settings' AND constraint_type = 'UNIQUE';` if needed, or use `DROP CONSTRAINT IF EXISTS` with the expected name and also check the migration file for the original name
- Hibernate's tenant filter should already scope queries to the current tenant, but add an explicit check in `SystemSettingService.findByKey()` if any method bypasses the filter

**Acceptance Criteria:**
- [ ] New Flyway migration applies cleanly
- [ ] Two different tenants can each have `auto_allocation_enabled` as a setting key
- [ ] One tenant cannot read or overwrite another tenant's settings
- [ ] Global unique constraint on `setting_key` alone no longer exists

**Tests to Write:**
- `SystemSettingServiceTest`: `getSettingByKey_returnsOnlyCurrentTenantValue()`
- Integration: two tenants with same key → both readable independently without conflict

---

### TASK-1.04: Remove JWT Secret Default Value and Add Fail-Fast Startup Check

- **Status:** DONE
- **Priority:** CRITICAL
- **Phase:** 1
- **Estimated Scope:** S (< 2 hrs)
- **Architecture Reference:** [ARCH 3.2.5]
- **Requirement References:** [REQ AUTH-001]
- **Risk References:** [ARCH RISK-005]
- **Dependencies:** None
- **Blocked By:** None

**Description:**
`application.yml` currently defines the JWT secret as `${JWT_SECRET:jCiWx+J9UA...}` — if `JWT_SECRET` is not set in the environment, a known default secret is silently used in production. This means any attacker can forge JWT tokens by using the published default. Remove the default value. Add a `@PostConstruct` validation in `JwtConfig` that throws `IllegalStateException` with a clear message if `JWT_SECRET` is absent or shorter than 32 characters.

Apply the same pattern to `MAIL_PASSWORD`: ensure it has no hardcoded fallback.

**⚠️ Risk Flag:** Before merging, ensure every environment (dev Docker Compose, CI, any staging) has `JWT_SECRET` set in its environment. A missing env var will cause application startup failure — the desired behaviour, but it will break unattended environments that rely on the default.

**Files to Touch:**
- `backend/src/main/resources/application.yml` — Remove `:jCiWx+J9UA...` default from `jwt.secret` property
- `backend/src/main/java/com/beezhub/student_accommodation/config/JwtConfig.java` — Add `@PostConstruct` validation: throw `IllegalStateException` if `secret` is null, blank, or < 32 chars
- `docker-compose.yml` — Verify `JWT_SECRET` is set in environment block for the backend service

**Implementation Notes:**
- Change: `jwt.secret: ${JWT_SECRET:jCiWx+J9UA...}` → `jwt.secret: ${JWT_SECRET}`
- The `@PostConstruct` method in `JwtConfig`:
  ```java
  @PostConstruct
  public void validateSecurityConfiguration() {
      if (secret == null || secret.isBlank() || secret.length() < 32) {
          throw new IllegalStateException(
              "JWT_SECRET environment variable must be set and be at least 32 characters. " +
              "Application startup aborted for security reasons."
          );
      }
  }
  ```
- Check if `MAIL_PASSWORD` also has a hardcoded default and remove it the same way
- Per [ARCH 4.1 principle 10]: "No environment-specific security defaults."

**Acceptance Criteria:**
- [ ] `application.yml` `jwt.secret` property has no default value: `${JWT_SECRET}` only
- [ ] Application throws `IllegalStateException` with clear message at startup when `JWT_SECRET` is absent
- [ ] Application starts normally when `JWT_SECRET` is a valid 512-bit base64 value
- [ ] `docker-compose.yml` backend service has `JWT_SECRET` in environment
- [ ] CI pipeline provides `JWT_SECRET` in test environment

**Tests to Write:**
- `JwtConfigTest`: `startup_withoutJwtSecret_throwsIllegalStateException()`
- `JwtConfigTest`: `startup_withShortJwtSecret_throwsIllegalStateException()`
- `JwtConfigTest`: `startup_withValidJwtSecret_succeeds()`

---

### TASK-1.05: Parametrise SMTP Security Configuration

- **Status:** DONE
- **Priority:** CRITICAL
- **Phase:** 1
- **Estimated Scope:** S (< 2 hrs)
- **Architecture Reference:** [ARCH 3.2.6]
- **Requirement References:** [REQ NOTIF-001]
- **Risk References:** [ARCH RISK-006]
- **Dependencies:** None
- **Blocked By:** None

**Description:**
`application.yml` hardcodes `starttls.enable: false` and `smtp.auth: false`. In production, emails are sent in plaintext over an unauthenticated connection — a CRITICAL security gap. Parametrise both settings via environment variables so that dev environments explicitly opt into the insecure MailHog defaults, and production enforces TLS + authentication.

**Files to Touch:**
- `backend/src/main/resources/application.yml` — Replace `starttls.enable: false` with `starttls.enable: ${MAIL_STARTTLS_ENABLE:false}` and `auth: false` with `auth: ${MAIL_SMTP_AUTH:false}`
- `docker-compose.yml` — Add `MAIL_STARTTLS_ENABLE=false` and `MAIL_SMTP_AUTH=false` explicitly to the backend service environment (documenting the intentional dev default)

**Implementation Notes:**
- Using `${MAIL_STARTTLS_ENABLE:false}` preserves the current dev behaviour (MailHog doesn't support TLS). The `:false` default is intentional and explicitly documented
- Production deployment checklist must include: `MAIL_STARTTLS_ENABLE=true`, `MAIL_SMTP_AUTH=true`, `MAIL_USERNAME=<real>`, `MAIL_PASSWORD=<real>`
- The `MailConfig.java` file may bind these properties — check if it holds additional SMTP settings that also need env-var parametrisation
- Per [ARCH 4.1 principle 10]: security config must come from environment variables, never hardcoded

**Acceptance Criteria:**
- [ ] `application.yml` uses `${MAIL_STARTTLS_ENABLE:false}` and `${MAIL_SMTP_AUTH:false}`
- [ ] Dev environment (MailHog) continues to work with no env var changes required
- [ ] A test environment with `MAIL_STARTTLS_ENABLE=true` will attempt STARTTLS (verifiable in MailHog logs or integration test)
- [ ] `docker-compose.yml` explicitly documents `MAIL_STARTTLS_ENABLE=false` for dev

**Tests to Write:**
- No new tests needed — this is a configuration parametrisation
- Manual verification: existing email integration tests still pass with MailHog

---

### TASK-1.06: Fix postgres_data Volume Mount

- **Status:** DONE
- **Priority:** HIGH
- **Phase:** 1
- **Estimated Scope:** S (< 2 hrs)
- **Architecture Reference:** [ARCH 3.2.7]
- **Requirement References:** N/A
- **Risk References:** [ARCH RISK-007]
- **Dependencies:** None
- **Blocked By:** None

**Description:**
`docker-compose.yml` declares a `postgres_data` named volume at the top level, but the `db` service does not mount it. Every `docker-compose down` silently destroys all database data. This is a high-risk gap in any developer environment — migrations and seed data are lost, requiring a full rebuild.

Add the volume mount to the `db` service definition: `volumes: - postgres_data:/var/lib/postgresql/data`

**Files to Touch:**
- `docker-compose.yml` — Add `volumes: - postgres_data:/var/lib/postgresql/data` under the `db` service

**Implementation Notes:**
- The `postgres_data` volume must already be declared in the top-level `volumes:` block of `docker-compose.yml` — verify it is (it should be per the architecture assessment)
- After this change: `docker-compose down && docker-compose up` will preserve the database. Confirm by creating a record, downing the stack, bringing it back up, and verifying the record persists
- ⚠️ If any developer's `postgres_data` volume from before this fix was populated with schema from `hbm2ddl.auto: update` (untracked by Flyway), the data may be in an unexpected state. After TASK-1.01 is merged, developers should remove their old volume: `docker volume rm <project>_postgres_data` then `docker-compose up --build` to start fresh with clean Flyway migrations

**Acceptance Criteria:**
- [ ] `db` service in `docker-compose.yml` has `volumes: - postgres_data:/var/lib/postgresql/data`
- [ ] Database data persists across `docker-compose down` + `docker-compose up` cycle
- [ ] Running `docker volume ls` shows `<project>_postgres_data` volume after first startup

**Tests to Write:**
- Manual verification only — run `docker-compose down`, `docker-compose up`, query the database

---

### TASK-1.07: Restore Flyway Safety Settings

- **Status:** DONE
- **Priority:** HIGH
- **Phase:** 1
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 3.2.8]
- **Requirement References:** N/A
- **Risk References:** [ARCH RISK-012]
- **Dependencies:** TASK-1.01 (must confirm `hbm2ddl: validate` passes cleanly first)
- **Blocked By:** None

**Description:**
Flyway settings were relaxed (`out-of-order: true`, `validate-on-migrate: false`, `ignore-missing-migrations: true`) to accommodate the migration consolidation (55 files → 2 files). Now that consolidation is stable, restore the safety settings to enforce strict migration ordering and validate checksums.

Steps:
1. Confirm TASK-1.01 is DONE and the application starts with `validate` mode
2. Apply a Flyway baseline at the current consolidated migration version so Flyway treats all earlier historical versions (the 55 original files) as "already applied"
3. Restore `application.yml`: `out-of-order: false`, `validate-on-migrate: true`, `ignore-missing-migrations: false`
4. Run `./gradlew bootRun` and confirm no Flyway errors

**⚠️ Risk Flag:** The baseline step is critical. If Flyway tries to re-apply historical migrations that were part of the consolidation (but weren't tracked via `baseline`), it will fail. Read the [Flyway Baseline documentation](https://flywaydb.org/documentation/command/baseline) carefully before proceeding. The baseline version should be set to the version *just before* the first consolidated migration (`V2026_02_19_2100`).

**Files to Touch:**
- `backend/src/main/resources/application.yml` — Change `out-of-order: true` → `false`, `validate-on-migrate: false` → `true`, `ignore-missing-migrations: true` → `false`
- Possibly a Flyway baseline configuration property: `spring.flyway.baseline-on-migrate: true` and `spring.flyway.baseline-version: <version-before-consolidation>`

**Implementation Notes:**
- Confirm the current Flyway settings location in `application.yml` — typically under `spring.flyway.*`
- The consolidated migration history starts at `V2026_02_19_2100` — set baseline accordingly
- After restoring safety settings, any future migration file checked in with an out-of-order timestamp will fail the startup validation — this is the desired behaviour
- Per [ARCH 5.1.3]: "Migrations are immutable once applied"

**Acceptance Criteria:**
- [ ] `application.yml` shows `out-of-order: false`, `validate-on-migrate: true`, `ignore-missing-migrations: false`
- [ ] Application starts without Flyway errors
- [ ] Creating a migration file with an older version number (simulating out-of-order) causes startup failure
- [ ] `flyway:validate` Gradle task passes cleanly

**Tests to Write:**
- No unit tests — verifiable via application startup and Flyway validate task
- CI: add `./gradlew flywayValidate` step to the pipeline

---

### TASK-1.08: Scheduled Jobs Observability Wrapper

- **Status:** DONE
- **Priority:** HIGH
- **Phase:** 1
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 3.4.2], [ARCH ADR-006]
- **Requirement References:** N/A
- **Risk References:** [ARCH RISK-008]
- **Dependencies:** None
- **Blocked By:** None

**Description:**
Five `@Scheduled` methods run silently with no error containment, structured logging, or Prometheus metrics. If any scheduled job throws an unhandled exception, the Spring task scheduler logs it and may skip future executions — with no alert to operators.

Wrap all five scheduled methods in the standardised observability pattern defined in [ARCH 3.4.2]:

```java
String jobName = "jobNameHere";
log.info("job={} action=start", jobName);
try {
    int processed = underlyingService.doWork();
    log.info("job={} action=complete processed={}", jobName, processed);
    meterRegistry.counter("jobs." + jobName + ".processed").increment(processed);
} catch (Exception e) {
    log.error("job={} action=failed", jobName, e);
    meterRegistry.counter("jobs." + jobName + ".failures").increment();
}
```

The five scheduled jobs are:
1. **Offer expiry** — `OfferExpiryService.java` (runs every 1h)
2. **Check-in expiry** — `CheckInExpiryService.java` (runs every 1h)
3. **Batch allocation** — `AllocationBatchService.java` (runs every 6h)
4. **Notification retry** — `NotificationRetryService.java` (runs every 5m)
5. **Orphan file cleanup** — `OrphanFileCleanupJob.java` (runs at 3am daily)

If the `@Scheduled` annotation is defined on the service class itself (not a separate scheduler class), create a `ScheduledJobRunner` wrapper class that holds all five scheduled methods and delegates to the underlying services. This keeps the `@Scheduled` annotation and the try/catch boilerplate in one place, separate from business logic.

**Files to Touch:**
- `backend/src/main/java/com/beezhub/student_accommodation/service/allocation/OfferExpiryService.java` — Wrap scheduled method or extract to runner
- `backend/src/main/java/com/beezhub/student_accommodation/service/allocation/CheckInExpiryService.java` — Wrap scheduled method or extract to runner
- `backend/src/main/java/com/beezhub/student_accommodation/service/allocation/AllocationBatchService.java` — Wrap scheduled method or extract to runner
- `backend/src/main/java/com/beezhub/student_accommodation/service/notification/NotificationRetryService.java` — Wrap scheduled method or extract to runner
- `backend/src/main/java/com/beezhub/student_accommodation/service/document/OrphanFileCleanupJob.java` — Wrap scheduled method or extract to runner
- *(Optional)* `backend/src/main/java/com/beezhub/student_accommodation/service/ScheduledJobRunner.java` — New class consolidating all `@Scheduled` methods with observability wrappers

**Implementation Notes:**
- `MeterRegistry` is already available via Micrometer — inject it with `@RequiredArgsConstructor`
- Log format per [ARCH 5.1.1]: `log.info("job={} action=complete processed={}", jobName, count)`
- The underlying service methods (e.g., `OfferExpiryService.expireOutstandingOffers()`) should return `int` (count of records processed) so the `processed` counter is meaningful. If they currently return `void`, update the return type
- Prometheus counter names: `jobs.offerExpiry.processed`, `jobs.offerExpiry.failures`, `jobs.checkInExpiry.processed`, etc.
- Per [ARCH ADR-006]: one job failure does NOT affect other jobs — the try/catch boundary ensures this

**Acceptance Criteria:**
- [ ] All 5 scheduled jobs have try/catch wrappers with structured log output
- [ ] Each job emits `jobs.{name}.processed` and `jobs.{name}.failures` Prometheus counters
- [ ] A simulated exception in a scheduled job is logged as ERROR and increments `failures` counter
- [ ] The exception does NOT cause the next scheduled execution to be skipped
- [ ] Counters are visible at `/actuator/metrics/jobs.offerExpiry.processed` (and equivalent for other jobs)
- [ ] Log format matches: `job=offerExpiry action=start`, `job=offerExpiry action=complete processed=5`

**Tests to Write:**
- `OfferExpiryServiceTest`: `whenServiceThrows_failureCounterIncremented_noExceptionPropagated()`
- `AllocationBatchServiceTest`: `whenServiceThrows_failureCounterIncremented()`
- At minimum, test that exceptions thrown by the underlying service do not escape the wrapper

---

## Phase 2: Structural Improvement

> *Resolve v2.0 architecture blockers and build the missing data model. Requires Phase 1 gate criteria to be met.*
>
> **Phase 1 Gate (all must be DONE before starting Phase 2):**
> - `TASK-1.01` through `TASK-1.08` all merged and CI passing
> - Application starts with `hbm2ddl.auto: validate` — no schema drift
> - All scheduled jobs emit Prometheus metrics

---

### TASK-2.01: Decouple Documents from Applications

- **Status:** DONE
- **Priority:** CRITICAL
- **Phase:** 2
- **Estimated Scope:** L (4–8 hrs)
- **Architecture Reference:** [ARCH 3.2.2], [ARCH ADR-002]
- **Requirement References:** [REQ ST-004], [REQ DOC-001], [REQ DOC-002], [REQ DOC-003]
- **Risk References:** [ARCH RISK-004]
- **Dependencies:** TASK-1.01 (Flyway is sole schema manager), TASK-1.07 (Flyway safety restored)
- **Blocked By:** None

**Description:**
`document.application_id BIGINT NOT NULL` ties every document to exactly one application. This blocks the v2.0 core differentiator: student uploads an ID once to their profile, attaches it to multiple applications, and once verified by any owner it is trusted platform-wide. The constraint must be removed and replaced with a junction table.

**Execute this incremental migration sequence — do not skip steps or combine steps:**

**Step 1 — Migration A (non-destructive):** Make `application_id` nullable.
```sql
ALTER TABLE document ALTER COLUMN application_id DROP NOT NULL;
```

**Step 2 — Migration B:** Create the junction table and migrate existing data.
```sql
CREATE TABLE application_document (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES application(id),
    document_id BIGINT NOT NULL REFERENCES document(id),
    attached_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_application_document UNIQUE (application_id, document_id)
);
CREATE INDEX idx_application_document_application ON application_document(application_id);
CREATE INDEX idx_application_document_document ON application_document(document_id);

-- Migrate existing associations
INSERT INTO application_document (application_id, document_id)
SELECT application_id, id FROM document WHERE application_id IS NOT NULL;
```

**Step 3 — Code changes:** Update `DocumentLifecycleService`, `ApplicationDocumentService`, and `DocumentRepository` to use the junction table. Ensure `DocumentService.uploadDocument()` no longer requires `applicationId`.

**Step 4 — Frontend change:** Update `frontend/src/app/core/services/document.service.ts` — remove `applicationId` from the upload request body.

**Step 5 — Migration C (destructive — only after Step 3 code is deployed and tested):**
```sql
ALTER TABLE document DROP COLUMN application_id;
```

**⚠️ Risk Flag:** Steps 1–2 are non-destructive (nullable column + junction table). Steps 3–4 must be deployed and tested before Step 5. Do NOT drop the column until all code paths confirmed to use the junction table. Steps 1 and 2 can be in one migration; Step 5 must be a separate migration deployed after code verification.

**Files to Touch:**
- `backend/src/main/resources/db/migration/V2026_02_24_0010__decouple_document_from_application.sql` — Steps 1 + 2
- `backend/src/main/resources/db/migration/V2026_02_24_0011__drop_document_application_id.sql` — Step 5 (separate file, deployed after code verified)
- `backend/src/main/java/com/beezhub/student_accommodation/model/entity/Document.java` — Remove `application_id` FK mapping (after Step 5 migration)
- `backend/src/main/java/com/beezhub/student_accommodation/model/entity/ApplicationDocument.java` — New entity for junction table (create this file)
- `backend/src/main/java/com/beezhub/student_accommodation/repository/ApplicationDocumentRepository.java` — New repository (create this file)
- `backend/src/main/java/com/beezhub/student_accommodation/service/DocumentLifecycleService.java` — Remove `applicationId` parameter from `uploadDocument()`
- `backend/src/main/java/com/beezhub/student_accommodation/service/application/ApplicationDocumentService.java` — Use junction table for attach/detach operations
- `frontend/src/app/core/services/document.service.ts` — Remove `applicationId` from upload request

**Implementation Notes:**
- `ApplicationDocument` entity follows [ARCH 5.1.1]: `@Transactional(readOnly = true)` on query methods
- Per [ARCH ADR-002] compliance: write an integration test where one document is attached to two applications and the same document record is referenced by both
- Existing API response DTOs that return `applicationId` on documents must be updated to remove that field — this is a **breaking DTO change** per [ARCH 5.4]: requires mapper update + OpenAPI doc update + frontend service update

**Acceptance Criteria:**
- [ ] Student can upload a document without any active application
- [ ] The same document can be attached to two different applications (via junction table)
- [ ] `DocumentService.uploadDocument()` no longer accepts `applicationId` parameter
- [ ] `application_document` junction table exists with correct indexes and unique constraint
- [ ] `document.application_id` column removed (after Step 5 migration)
- [ ] Verified document status on `document` entity persists after one application is cancelled
- [ ] All existing document-related tests pass

**Tests to Write:**
- `DocumentLifecycleServiceTest`: `uploadDocument_withoutApplicationId_succeeds()`
- `ApplicationDocumentServiceTest`: `attachDocument_toTwoApplications_singleDocumentRecord()`
- Integration: `verifyDocument_persistsStatusAcrossApplications()`

---

### TASK-2.02: Student Platform-Level Identity (Sentinel Tenant)

- **Status:** DONE
- **Priority:** MEDIUM
- **Phase:** 3
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 9 Phase 3 item 11]
- **Requirement References:** [REQ NOTIF-001 item 13]
- **Risk References:** None
- **Dependencies:** TASK-3.10 (same pattern — implement offer reminder first to establish pattern), Phase 2 complete
- **Blocked By:** None
 - **Priority:** CRITICAL
 - **Phase:** 2
 - **Estimated Scope:** L (4–8 hrs)
 - **Architecture Reference:** [ARCH 3.2.9], [ARCH ADR-001]
 - **Requirement References:** [REQ AUTH-002], [REQ BR-P05], [REQ BR-P07]
 - **Risk References:** [ARCH RISK-003], [ARCH RISK-011]
 - **Dependencies:** TASK-1.01, TASK-1.07
 - **Blocked By:** None (can run in parallel with TASK-2.03)

**Description:**
`app_user.tenant_id NOT NULL` for students makes it architecturally impossible for a student to apply to buildings owned by different tenants — their Hibernate filter scopes all data access to one tenant. In v2.0, students are platform-level entities who can apply to any building.

**Implementation approach (ADR-001 — PLATFORM sentinel):**

1. **Migration A:** Ensure a `PLATFORM` tenant row exists with `id = 0`:
   ```sql
   INSERT INTO tenant (id, name, is_active) VALUES (0, 'PLATFORM', true)
   ON CONFLICT (id) DO NOTHING;
   ```

2. **Migration B:** Update existing student user records to use `tenant_id = 0`:
   ```sql
   UPDATE app_user SET tenant_id = 0
   WHERE id IN (SELECT user_id FROM student);
   ```

3. **Update `TenantContext` filter logic:** In `TenantFilterConfig.java` or `TenantContext.java`, add: if the authenticated user's JWT `tenantId` claim is `0`, do NOT apply the Hibernate tenant filter. Student-owned data (`document`, `application`, `room_allocation`, `user_notifications`) is instead scoped by `student_id` / `user_id` ownership, not by tenant filter.

4. **Update `AuthService.signup()`:** When registering a `STUDENT` role, set `tenant_id = 0` (the PLATFORM sentinel) instead of the calling context's tenant ID.

5. **Update `JwtUtil.generateToken()`:** For `STUDENT` role tokens, embed `tenantId = 0` in the JWT claims.

**⚠️ Risk Flag: HIGHEST RISK TASK IN PHASE 2.** This touches the core security isolation mechanism. A mistake here can cause cross-tenant data leakage. Mandatory:
- Full regression of `TenantFilterIntegrationTest` before merging
- New integration tests asserting the three-part compliance (see Acceptance Criteria)
- Code review by at least two developers

**Files to Touch:**
- `backend/src/main/resources/db/migration/V2026_02_24_0012__platform_tenant_seed.sql` — Seed PLATFORM tenant row + migrate existing students
- `backend/src/main/java/com/beezhub/student_accommodation/config/TenantFilterConfig.java` — Add sentinel bypass logic
- `backend/src/main/java/com/beezhub/student_accommodation/service/tenant/TenantContext.java` — Update filter bypass condition
- `backend/src/main/java/com/beezhub/student_accommodation/service/AuthService.java` — Set `tenant_id = 0` for STUDENT signups
- `backend/src/main/java/com/beezhub/student_accommodation/security/JwtAuthenticationFilter.java` *(or JwtUtil)* — Embed `tenantId = 0` for student tokens

**Implementation Notes:**
- Per [ARCH ADR-001]: "student ownership filtering (by `student_id` / `user_id`) replaces tenant filtering for student-owned tables"
- Building owner-scoped tables (`building`, `room`, `allocation_rule`) must STILL be filtered by building owner's tenant ID — the bypass is only for student-owned resources
- `JwtAuthenticationFilter` populates `TenantContext` from the JWT — verify it handles `tenantId = 0` by skipping the Hibernate filter
- Check `model/entity/package-info.java` for global `@FilterDef` and `@Filter` annotations that apply to all entities

**Acceptance Criteria:**
- [ ] A student account has `tenant_id = 0` in `app_user` table
- [ ] Student JWT token has `tenantId = 0` in claims
- [ ] A student can read buildings from two different building owner tenants
- [ ] A student cannot read another student's applications or documents
- [ ] A building owner cannot read another owner's buildings (tenant isolation preserved)
- [ ] All existing `TenantFilterIntegrationTest` tests pass
- [ ] New cross-tenant compliance tests pass (see Tests to Write)

**Tests to Write:**
- `TenantFilterIntegrationTest.student_canSeeBuildings_fromTwoDifferentTenants()`
- `TenantFilterIntegrationTest.student_cannotSee_anotherStudentsDocuments()`
- `TenantFilterIntegrationTest.buildingOwner_cannotSee_anotherOwnersBuildings()`

---

 ### TASK-2.03: Migrate UserRole to v2.0 Role Model

- **Status:** DONE
- **Priority:** CRITICAL
- **Phase:** 2
- **Estimated Scope:** L (4–8 hrs)
- **Architecture Reference:** [ARCH 3.2.10], [ARCH ADR-004]
- **Requirement References:** [REQ AUTH-001], [REQ SEC-005], [REQ BR-S04]
- **Risk References:** [ARCH RISK-009]
- **Dependencies:** TASK-1.01, TASK-1.07
- **Blocked By:** None (can run in parallel with TASK-2.02)

**Description:**
`UserRole { STUDENT, ADMIN, LANDLORD }` is a v1.1 concept. v2.0 requires `{ STUDENT, PLATFORM_ADMIN, BUILDING_OWNER, BUILDING_STAFF }` with distinct, non-overlapping permission scopes. This is an incremental, zero-downtime migration per [ARCH ADR-004].

**Incremental steps:**

1. **Add new roles to enum** alongside existing values — `UserRole.PLATFORM_ADMIN`, `BUILDING_OWNER`, `BUILDING_STAFF`. Annotate `ADMIN` and `LANDLORD` with `@Deprecated`.

2. **Data migration:** Update existing user records:
   ```sql
   UPDATE app_user SET role = 'PLATFORM_ADMIN' WHERE role = 'ADMIN';
   UPDATE app_user SET role = 'BUILDING_OWNER' WHERE role = 'LANDLORD';
   ```

3. **Update `SecurityConfig.roleHierarchy()`:** `PLATFORM_ADMIN > BUILDING_OWNER > BUILDING_STAFF`. Students remain independent (not in the hierarchy).

4. **Update all `@PreAuthorize` annotations** across all controllers — replace `hasRole('ADMIN')` with `hasRole('PLATFORM_ADMIN')` and `hasRole('LANDLORD')` with `hasRole('BUILDING_OWNER')`.

5. **Update Angular guards:** Replace `currentUser.userRole === 'ADMIN'` string literals in `admin.guard.ts` and any other guard/service with a `RoleService.hasRole(Role.PLATFORM_ADMIN)` abstraction per [ARCH 5.1.2] rule 4.

6. **Remove deprecated roles** in the follow-up release (after full regression pass).

**⚠️ Risk Flag: HIGH RISK** — Role checks are pervasive in controllers and Angular guards. A single missed check silently grants or denies access. A comprehensive role-access test suite must pass for all controller endpoints before `ADMIN` and `LANDLORD` are removed from the enum.

**Files to Touch:**
- `backend/src/main/resources/db/migration/V2026_02_24_0013__migrate_user_roles.sql` — Data migration
- `backend/src/main/java/com/beezhub/student_accommodation/model/enums/UserRole.java` — Add new roles, deprecate old
- `backend/src/main/java/com/beezhub/student_accommodation/config/SecurityConfig.java` — Update role hierarchy
- All `@RestController` files with `@PreAuthorize` annotations — search with `grep -r "hasRole('ADMIN')" backend/src/main/java/`
- `frontend/src/app/core/guards/admin.guard.ts` — Replace string literal `'ADMIN'` with `RoleService` abstraction
- `frontend/src/app/core/services/auth.service.ts` — Update role comparisons
- *(New)* `frontend/src/app/core/services/role.service.ts` — New `RoleService` with `hasRole(role: UserRole): boolean`

**Implementation Notes:**
- Per [ARCH 5.1.2] rule 4: "Guards enforce role access at route level. Use a `RoleService.hasRole(Role.BUILDING_OWNER)` abstraction — not inline string comparisons."
- The `RoleService` should read the role from `TokenStorageService` (which already exists in `frontend/src/app/core/services/token-storage.service.ts`)
- Per [ARCH ADR-004]: temporary period with both old and new roles coexisting is acceptable. The old roles (`ADMIN`, `LANDLORD`) should still work after Step 1 until the data migration in Step 2 has run

**Acceptance Criteria:**
- [ ] `UserRole` enum contains `PLATFORM_ADMIN`, `BUILDING_OWNER`, `BUILDING_STAFF` alongside (deprecated) `ADMIN`, `LANDLORD`
- [ ] `SecurityConfig.roleHierarchy()` reflects v2.0 hierarchy
- [ ] All `@PreAuthorize("hasRole('ADMIN')")` annotations updated to `PLATFORM_ADMIN`
- [ ] All `@PreAuthorize("hasRole('LANDLORD')")` annotations updated to `BUILDING_OWNER`
- [ ] Angular `admin.guard.ts` uses `RoleService` — not string literal `'ADMIN'`
- [ ] `PLATFORM_ADMIN` can access all tenants (audited)
- [ ] `BUILDING_OWNER` cannot access another owner's data
- [ ] Existing tests pass with new roles

**Tests to Write:**
- `SecurityConfigTest`: `platformAdmin_canAccess_allTenantEndpoints()`
- `SecurityConfigTest`: `buildingOwner_cannotAccess_anotherOwnersBuildingId()`
- Frontend: `AdminGuardSpec`: `hasRole_withPlatformAdmin_returnsTrue()`

---

### TASK-2.04: Increase HikariCP Connection Pool Size

- **Status:** DONE
- **Priority:** MEDIUM
- **Phase:** 2
- **Estimated Scope:** S (< 2 hrs)
- **Architecture Reference:** [ARCH 4.4], [ARCH 2.5 PB-002]
- **Requirement References:** [REQ scalability NFR — 500 concurrent users]
- **Risk References:** [ARCH RISK-014]
- **Dependencies:** TASK-1.01, TASK-1.07
- **Blocked By:** None

**Description:**
HikariCP is configured with `maximum-pool-size: 10` (likely the default). Under ~50+ concurrent API calls, connection starvation will occur — requests queue waiting for a connection, degrading response times and eventually causing 500 errors. The 500-user target requires a pool size of at least 25.

Update `application.yml` with the recommended HikariCP configuration from [ARCH 4.4]:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 25
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 300000
      max-lifetime: 600000
      leak-detection-threshold: 60000
```

**Files to Touch:**
- `backend/src/main/resources/application.yml` — Update HikariCP settings

**Implementation Notes:**
- `leak-detection-threshold: 60000` (60s) will log a warning if a connection is held for more than 60s without being returned — helpful for detecting slow queries or forgotten transaction holds
- PostgreSQL has a default `max_connections = 100`. A pool of 25 from one application instance is reasonable. If multiple instances are deployed, this needs revisiting
- The `connection-timeout: 30000` (30s) is already configured per [ARCH 2.6] — verify it's still present after changes

**Acceptance Criteria:**
- [ ] `application.yml` shows `maximum-pool-size: 25` and `minimum-idle: 10`
- [ ] `leak-detection-threshold: 60000` is configured
- [ ] Application starts and connects to the database successfully
- [ ] `/actuator/metrics/hikaricp.connections.max` reports 25

**Tests to Write:**
- No unit tests — verify via `/actuator/metrics/hikaricp.connections.max` after startup

---

### TASK-2.05: Add intake_period Table Migration

- **Status:** DONE
- **Priority:** HIGH
- **Phase:** 2
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 4.4], [ARCH 9 Phase 2 item 5]
- **Requirement References:** [REQ BO-003], [REQ APP-001], [REQ BR-O02]
- **Risk References:** None
- **Dependencies:** TASK-1.01, TASK-1.07
- **Blocked By:** None

**Description:**
The v1.1 system used a platform-wide `academic_year` entity. v2.0 requires **per-building intake periods** (fixed window with open/close dates, or rolling/always-open). Write a Flyway migration creating the `intake_period` table. Then create the corresponding JPA entity and repository.

```sql
CREATE TABLE intake_period (
    id BIGSERIAL PRIMARY KEY,
    building_id BIGINT NOT NULL REFERENCES building(id),
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    name VARCHAR(100) NOT NULL,
    mode VARCHAR(20) NOT NULL CHECK (mode IN ('FIXED', 'ROLLING')),
    open_date TIMESTAMP,
    close_date TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_intake_fixed_dates CHECK (
        mode != 'FIXED' OR (open_date IS NOT NULL AND close_date IS NOT NULL)
    ),
    CONSTRAINT chk_intake_close_after_open CHECK (
        close_date IS NULL OR open_date IS NULL OR close_date > open_date
    )
);
CREATE INDEX idx_intake_period_building ON intake_period(building_id);
CREATE INDEX idx_intake_period_tenant ON intake_period(tenant_id);
```

**Files to Touch:**
- `backend/src/main/resources/db/migration/V2026_02_24_0020__add_intake_period.sql` — New migration
- `backend/src/main/java/com/beezhub/student_accommodation/model/entity/IntakePeriod.java` — New entity (create this file)
- `backend/src/main/java/com/beezhub/student_accommodation/repository/IntakePeriodRepository.java` — New repository (create this file)

**Implementation Notes:**
- Per [ARCH 5.1.1]: the entity must include `@Filter` for tenant isolation: `@FilterDef(name = "tenantFilter", ...)` + `@Filter(name = "tenantFilter", ...)`
- `mode` stored as `VARCHAR(20)` per [ARCH 5.1.3] — not a PostgreSQL native enum
- All `TIMESTAMP` columns per [ARCH 5.1.3] convention: `NOT NULL DEFAULT CURRENT_TIMESTAMP` for lifecycle fields
- Per [ARCH RISK-011]: this new entity **must** be added to `TenantFilterIntegrationTest` — link to `TASK-2.13`
- `IntakePeriodRepository` should have `findByBuilding_IdAndIsActiveTrue()` and a method to check if a building has an open intake period for the current date

**Acceptance Criteria:**
- [ ] Migration applies cleanly with `hbm2ddl: validate`
- [ ] `intake_period` table exists with correct columns, constraints, and indexes
- [ ] JPA entity maps to the table without `SchemaManagementException`
- [ ] Repository has `findActiveIntakePeriodForBuilding(Long buildingId, LocalDateTime now)` query
- [ ] `TenantFilterIntegrationTest` is updated to include `intake_period` (see TASK-2.13)

**Tests to Write:**
- `IntakePeriodRepositoryTest`: `findActiveIntake_forBuilding_returnsOnlyActiveOpenPeriods()`

---

### TASK-2.06: Add building_owner and building_staff Tables Migration

- **Status:** DONE
- **Priority:** HIGH
- **Phase:** 2
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 4.4], [ARCH 9 Phase 2 item 6]
- **Requirement References:** [REQ PA-001], [REQ BO-001], [REQ BO-002]
- **Risk References:** None
- **Dependencies:** TASK-1.01, TASK-1.07, TASK-2.03 (v2.0 roles needed before building_owner entity meaningful)
- **Blocked By:** None

**Description:**
The v2.0 platform requires `building_owner` and `building_staff` entity tables. Currently, building owners are just `app_user` records with `LANDLORD` role — there is no `building_owner` profile table with onboarding status, business name, or accreditation proof. Write the migration and create corresponding entities and repositories.

```sql
CREATE TABLE building_owner (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES app_user(id),
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    business_name VARCHAR(200) NOT NULL,
    contact_phone VARCHAR(20),
    region VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED')),
    rejection_reason VARCHAR(500),
    approved_by BIGINT REFERENCES app_user(id),
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_building_owner_user UNIQUE (user_id)
);
CREATE INDEX idx_building_owner_tenant ON building_owner(tenant_id);
CREATE INDEX idx_building_owner_status ON building_owner(status);

CREATE TABLE building_staff (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id),
    owner_id BIGINT NOT NULL REFERENCES building_owner(id),
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    permission_set VARCHAR(20) NOT NULL CHECK (permission_set IN ('FULL', 'RESTRICTED')),
    is_active BOOLEAN NOT NULL DEFAULT true,
    invited_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at TIMESTAMP,
    invitation_expires_at TIMESTAMP,
    CONSTRAINT uq_staff_user_owner UNIQUE (user_id, owner_id)
);
CREATE INDEX idx_building_staff_owner ON building_staff(owner_id);
CREATE INDEX idx_building_staff_tenant ON building_staff(tenant_id);
```

**Files to Touch:**
- `backend/src/main/resources/db/migration/V2026_02_24_0021__add_building_owner_staff.sql` — New migration
- `backend/src/main/java/com/beezhub/student_accommodation/model/entity/BuildingOwner.java` — New entity
- `backend/src/main/java/com/beezhub/student_accommodation/model/entity/BuildingStaff.java` — New entity
- `backend/src/main/java/com/beezhub/student_accommodation/repository/BuildingOwnerRepository.java` — New repository
- `backend/src/main/java/com/beezhub/student_accommodation/repository/BuildingStaffRepository.java` — New repository

**Implementation Notes:**
- `building_staff` permission sets (`FULL`, `RESTRICTED`) are MVP-level; per-permission granularity is a future enhancement
- Both entities require Hibernate tenant filter annotations — include in `TASK-2.13`'s `TenantFilterIntegrationTest` extension
- The `invitation_expires_at` supports [REQ BO-002] "Staff invitation expires after 48 hours"

**Acceptance Criteria:**
- [ ] Migration applies cleanly
- [ ] Both tables exist with correct constraints and indexes
- [ ] JPA entities map to tables without schema validation errors
- [ ] `BuildingOwnerRepository.findPendingRegistrations()` query works
- [ ] Added to `TenantFilterIntegrationTest` (TASK-2.13)

**Tests to Write:**
- `BuildingOwnerRepositoryTest`: `findByStatus_PENDING_returnsCorrectResults()`

---

### TASK-2.07: Add custom_application_field and custom_field_answer Tables Migration

- **Status:** DONE
- **Priority:** HIGH
- **Phase:** 2
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 4.4], [ARCH 9 Phase 2 item 7]
- **Requirement References:** [REQ BO-004]
- **Risk References:** None
- **Dependencies:** TASK-1.01, TASK-1.07
- **Blocked By:** None

**Description:**
Building owners need to define custom application fields per building (text, number, dropdown, checkbox, date). Students answer these fields when applying. Write the migration creating both tables and their entities.

```sql
CREATE TABLE custom_application_field (
    id BIGSERIAL PRIMARY KEY,
    building_id BIGINT NOT NULL REFERENCES building(id),
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    field_label VARCHAR(200) NOT NULL,
    field_type VARCHAR(20) NOT NULL CHECK (field_type IN ('TEXT', 'NUMBER', 'DROPDOWN', 'CHECKBOX', 'DATE')),
    is_required BOOLEAN NOT NULL DEFAULT false,
    help_text VARCHAR(500),
    display_order INT NOT NULL DEFAULT 0,
    options_json TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_custom_field_building ON custom_application_field(building_id);

CREATE TABLE custom_field_answer (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES application(id),
    field_id BIGINT NOT NULL REFERENCES custom_application_field(id),
    answer_value TEXT,
    answered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_field_answer UNIQUE (application_id, field_id)
);
CREATE INDEX idx_custom_answer_application ON custom_field_answer(application_id);
```

**Files to Touch:**
- `backend/src/main/resources/db/migration/V2026_02_24_0022__add_custom_application_fields.sql` — New migration
- `backend/src/main/java/com/beezhub/student_accommodation/model/entity/CustomApplicationField.java` — New entity
- `backend/src/main/java/com/beezhub/student_accommodation/model/entity/CustomFieldAnswer.java` — New entity
- `backend/src/main/java/com/beezhub/student_accommodation/repository/CustomApplicationFieldRepository.java` — New repository
- `backend/src/main/java/com/beezhub/student_accommodation/repository/CustomFieldAnswerRepository.java` — New repository

**Implementation Notes:**
- `options_json` stores dropdown choices as a JSON array string (e.g., `["Option A", "Option B"]`)
- Per [REQ BO-004] edge case: "Field deleted after students have answered it → retain historical answers" — enforced by keeping the `custom_field_answer` row even when `custom_application_field.is_active = false`
- Both entities need Hibernate tenant filter; add to TASK-2.13

**Acceptance Criteria:**
- [ ] Both tables created with correct indexes and constraints
- [ ] JPA entities map without schema validation errors
- [ ] Soft-delete via `is_active = false` on `custom_application_field` preserves existing `custom_field_answer` rows
- [ ] Added to `TenantFilterIntegrationTest`

**Tests to Write:**
- `CustomApplicationFieldRepositoryTest`: `findByBuilding_IdAndIsActiveTrue_excludesInactiveFields()`

---

### TASK-2.08: Add required_document_type Table Migration

- **Status:** DONE
- **Priority:** HIGH
- **Phase:** 2
- **Estimated Scope:** S (< 2 hrs)
- **Architecture Reference:** [ARCH 4.4], [ARCH 9 Phase 2 item 8]
- **Requirement References:** [REQ BO-005], [REQ DOC-001], [REQ BR-D01]
- **Risk References:** None
- **Dependencies:** TASK-1.01, TASK-1.07
- **Blocked By:** None

**Description:**
Building owners need to define which document types are required for their building. Currently, document types are platform-wide and fixed (`document_type` table). v2.0 requires per-building configurable document requirements.

```sql
CREATE TABLE required_document_type (
    id BIGSERIAL PRIMARY KEY,
    building_id BIGINT NOT NULL REFERENCES building(id),
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    document_name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    is_required BOOLEAN NOT NULL DEFAULT true,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_required_doc_type_building ON required_document_type(building_id);
```

**Files to Touch:**
- `backend/src/main/resources/db/migration/V2026_02_24_0023__add_required_document_type.sql` — New migration
- `backend/src/main/java/com/beezhub/student_accommodation/model/entity/RequiredDocumentType.java` — New entity
- `backend/src/main/java/com/beezhub/student_accommodation/repository/RequiredDocumentTypeRepository.java` — New repository

**Implementation Notes:**
- This table replaces the platform-level `document_type` table's role for application requirements. The existing `document_type` table may still be used for categorising documents on `document` entity — assess the dependency before removing
- Add to TASK-2.13 for TenantFilterIntegrationTest

**Acceptance Criteria:**
- [ ] Table created with correct indexes
- [ ] JPA entity maps without errors
- [ ] `findByBuilding_IdAndIsActiveTrue()` returns only active document requirements for a building
- [ ] Added to `TenantFilterIntegrationTest`

**Tests to Write:**
- `RequiredDocumentTypeRepositoryTest`: `findActive_byBuilding_returnsCorrectSubset()`

---

 ### TASK-2.09: Extract ScoringStrategy Interface

- **Status:** DONE
- **Priority:** HIGH
- **Phase:** 2
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 3.4.1], [ARCH ADR-003]
- **Requirement References:** [REQ ALLOC-001], [REQ ALLOC-003]
- **Risk References:** [ARCH RISK-010]
- **Dependencies:** TASK-1.01, TASK-1.07
- **Blocked By:** None (must complete before TASK-2.10 and TASK-2.11)

**Description:**
`AllocationScoringService` is a concrete class with hardcoded v1.1 scoring constants, directly injected into `AllocationBatchService` and `AutoAllocationService`. v2.0 requires owner-configurable scoring rules. Extract a `ScoringStrategy` interface, rename the existing class to `LegacyScoringStrategy`, and wire the interface into callers. No behaviour changes to existing scoring logic.

**Interface to create:**
```java
public interface ScoringStrategy {
    List<ScoredRoom> scoreAvailableRooms(Student student, Application application, Building preferred);
    ScoredRoom scoreSingleRoom(Student student, Application application, Room room, AllocationRule rule);
    boolean satisfiesHardConstraints(Student student, Room room);
}
```

Steps:
1. Create `ScoringStrategy` interface in `service/allocation/`
2. Rename `AllocationScoringService` → `LegacyScoringStrategy` (implements `ScoringStrategy`). All existing tests still pass — no logic changes.
3. Annotate `LegacyScoringStrategy` with `@Primary` temporarily until v2.0 strategy is ready
4. Update `AllocationBatchService` and `AutoAllocationService` to inject `ScoringStrategy` (interface), not `AllocationScoringService` (concrete class)
5. Update `AllocationScoringOrchestrator` if it directly references the concrete class

**Files to Touch:**
- *(New)* `backend/src/main/java/com/beezhub/student_accommodation/service/allocation/ScoringStrategy.java` — New interface
- `backend/src/main/java/com/beezhub/student_accommodation/service/AllocationScoringService.java` → rename to `LegacyScoringStrategy.java` + `implements ScoringStrategy`
- `backend/src/main/java/com/beezhub/student_accommodation/service/allocation/AllocationBatchService.java` — Inject `ScoringStrategy` interface
- `backend/src/main/java/com/beezhub/student_accommodation/service/allocation/AutoAllocationService.java` — Inject `ScoringStrategy` interface
- `backend/src/main/java/com/beezhub/student_accommodation/service/allocation/AllocationScoringOrchestrator.java` — Update reference if it uses the concrete class

**Implementation Notes:**
- Per [ARCH ADR-003]: "The existing `AllocationScoringService` is renamed `LegacyScoringStrategy implements ScoringStrategy` — no behaviour changes, all existing tests pass"
- All existing `AllocationScoringServiceTest` tests must pass after rename — update test class name and import only
- `@Primary` on `LegacyScoringStrategy` ensures existing allocation flows continue to use v1.1 logic until `OwnerConfigurableScoringStrategy` (TASK-2.11) is ready and marked `@Primary`
- **Do not rename the class file in-place if you have git history on it** — use `git mv` to preserve history

**Acceptance Criteria:**
- [x] `ScoringStrategy` interface exists in `service/allocation/` package
- [x] `AllocationScoringService` renamed to `LegacyScoringStrategy` implementing `ScoringStrategy`
- [x] All existing allocation scoring tests pass without modification to test logic
- [x] `AllocationBatchService` and `AutoAllocationService` compile against `ScoringStrategy` interface
- [x] Allocation batch job still produces correct results end-to-end

**Tests to Write:**
- No new tests — verify all existing `AllocationScoringServiceTest` (renamed) tests still pass

---

### TASK-2.10: Fix AllocationScoringService N+1 Query Problem

- **Status:** DONE
- **Priority:** HIGH
- **Phase:** 2
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 2.4], [ARCH 2.5 PB-001]
- **Requirement References:** [REQ ALLOC-001], [REQ performance NFR — batch < 60s]
- **Risk References:** [ARCH RISK-017]
- **Dependencies:** TASK-2.09 (ScoringStrategy interface must be extracted first)
- **Blocked By:** None

**Description:**
`LegacyScoringStrategy.scoreSingleRoom()` (previously `AllocationScoringService`) calls `roomAttributesRepository.findByRoom_Id()` and `buildingAttributesRepository.findByBuilding_Id()` once per room in a loop. With 100 available rooms, this generates 200+ extra queries per allocation run — O(n) query growth that will cause the batch allocation NFR (< 60s for 50 applications) to fail under load.

**Fix:** Before the scoring loop, batch-load all `RoomAttributes` and `BuildingAttributes` for the set of candidate rooms into `Map<Long, RoomAttributes>` and `Map<Long, BuildingAttributes>` lookups. Pass these maps into `scoreSingleRoom()`.

```java
// Before loop
Set<Long> roomIds = candidateRooms.stream().map(Room::getId).collect(toSet());
Set<Long> buildingIds = candidateRooms.stream().map(r -> r.getBuilding().getId()).collect(toSet());
Map<Long, RoomAttributes> roomAttrMap = roomAttributesRepository
    .findByRoom_IdIn(roomIds)
    .stream().collect(toMap(ra -> ra.getRoom().getId(), ra -> ra));
Map<Long, BuildingAttributes> buildingAttrMap = buildingAttributesRepository
    .findByBuilding_IdIn(buildingIds)
    .stream().collect(toMap(ba -> ba.getBuilding().getId(), ba -> ba));

// Inside loop
RoomAttributes roomAttr = roomAttrMap.get(room.getId());
BuildingAttributes buildingAttr = buildingAttrMap.getOrDefault(room.getBuilding().getId(), null);
```

This collapses N+1 repository calls into 2 bulk queries regardless of room count.

**Files to Touch:**
- `backend/src/main/java/com/beezhub/student_accommodation/service/AllocationScoringService.java` (now `LegacyScoringStrategy.java`) — Refactor `scoreAvailableRooms()` to batch-load before the loop
- `backend/src/main/java/com/beezhub/student_accommodation/repository/RoomAttributesRepository.java` — Add `findByRoom_IdIn(Collection<Long> ids)` if not present
- `backend/src/main/java/com/beezhub/student_accommodation/repository/BuildingAttributesRepository.java` — Add `findByBuilding_IdIn(Collection<Long> ids)` if not present

**Implementation Notes:**
- The scoring loop must now receive the pre-loaded maps, not call the repositories internally
- Handle the null case for rooms/buildings without attributes (use `Optional.ofNullable()` on map lookups)
- Per [ARCH 5.1.1]: `@Transactional(readOnly = true)` on the `scoreAvailableRooms()` method — all reads happen in one transaction
- Verify the fix using query count logging: add `spring.jpa.show-sql: true` temporarily and count queries before/after

**Acceptance Criteria:**
- [x] `findByRoom_IdIn()` and `findByBuilding_IdIn()` methods exist in their respective repositories
- [x] Scoring 100 rooms generates exactly 2 repository queries (one for room attributes, one for building attributes) instead of 200
- [x] All existing allocation scoring tests pass
- [x] No regression in allocation correctness

**Tests to Write:**
- `LegacyScoringStrategyTest` (or `AllocationScoringServiceTest`): `scoreAvailableRooms_with100Rooms_generates2AttributeQueries()`
  - Use a `@Spy` on repositories to count invocations

---

### TASK-2.11: Implement OwnerConfigurableScoringStrategy

- **Status:** DONE
- **Priority:** HIGH
- **Phase:** 2
- **Estimated Scope:** L (4–8 hrs)
- **Architecture Reference:** [ARCH 3.4.1], [ARCH ADR-003]
- **Requirement References:** [REQ ALLOC-003], [REQ BO-006], [REQ BR-A01]–[REQ BR-A04]
- **Risk References:** [ARCH RISK-010]
- **Dependencies:** TASK-2.09 (interface), TASK-2.10 (N+1 fix — same class, sequential)
- **Blocked By:** None

**Description:**
Implement `OwnerConfigurableScoringStrategy implements ScoringStrategy`. This v2.0 strategy replaces the hardcoded v1.1 constants with owner-configured `AllocationRule` weights from the database. Scoring components per [REQ ALLOC-003]:

1. **FCFS Score:** Earlier `application.created_at` = higher score (always included; max 100 points normalised within the current application pool)
2. **University Preference:** Owner specifies priority universities in `AllocationRule`; student from a priority university = configured bonus points
3. **NSFAS Priority:** Student has confirmed NSFAS funding = configured bonus points
4. **Accessibility Match:** Student requires accessible room AND room is `accessibility_compliant = true` = configured bonus points
5. **Tiebreaker:** `application.created_at` ascending (earliest first)

Hard constraints (must ALL pass before scoring per [REQ ALLOC-002]):
- Application status = APPROVED
- All required documents = VERIFIED
- Student has no active allocation (OFFERED, ACCEPTED, ACTIVE)
- Room has available capacity
- Room is marked `is_available = true`
- Gender compatibility
- Accessibility compatibility

**Files to Touch:**
- *(New)* `backend/src/main/java/com/beezhub/student_accommodation/service/allocation/OwnerConfigurableScoringStrategy.java` — New class
- `backend/src/main/java/com/beezhub/student_accommodation/model/entity/AllocationRule.java` — Verify it has fields for FCFS weight, university preference (list of institution IDs), NSFAS priority weight, accessibility weight
- `backend/src/main/java/com/beezhub/student_accommodation/service/AllocationScoringService.java` (now `LegacyScoringStrategy.java`) — Change `@Primary` from this class to `OwnerConfigurableScoringStrategy`

**Implementation Notes:**
- `@Primary` on `OwnerConfigurableScoringStrategy` — this becomes the default once ready. `LegacyScoringStrategy` retains `@Component` but no `@Primary` for fallback if needed
- A feature flag can be used to toggle between implementations (`auto-allocation-scoring-v2`) — wire via `FF4jConfig` if needed
- Per [ARCH ADR-003]: both strategy implementations must independently satisfy the 90% line coverage threshold before Phase 2 closes
- Owner's `AllocationRule` entity must have the four scoring dimension fields — if they don't exist yet, add them via migration first

**Acceptance Criteria:**
- [ ] `OwnerConfigurableScoringStrategy` implements `ScoringStrategy` interface
- [x] `OwnerConfigurableScoringStrategy` implements `ScoringStrategy` interface (DONE)
- [x] FCFS scoring with normalised time-based score works
- [x] University preference score applies configured bonus to matching university students
- [x] NSFAS priority score applies configured bonus to NSFAS-confirmed students
- [x] Accessibility match score applies bonus when both student and room flags align
- [x] Hard constraints (`satisfiesHardConstraints()`) enforce gender and accessibility compatibility
- [x] Tiebreaker: earlier `application.created_at` wins when scores are equal
- [x] `OwnerConfigurableScoringStrategyTest` achieves ≥ 90% line coverage
- [x] Allocation batch job uses this strategy by default after `@Primary` switch

**Tests to Write:**
- `OwnerConfigurableScoringStrategyTest`: full coverage including each scoring dimension, hard constraint failures, and tiebreaker resolution (≥ 90% line coverage required per [ARCH ADR-003])
- `OwnerConfigurableScoringStrategyTest`: `satisfiesHardConstraints_genderMismatch_returnsFalse()`
- `OwnerConfigurableScoringStrategyTest`: `scoreAllocation_FCFSOnly_earliestApplicationWins()`
- `OwnerConfigurableScoringStrategyTest`: `scoreAllocation_NSFASPriority_NSFASStudentScoresHigher()`

---

### TASK-2.12: Establish Frontend Test Coverage Baseline

- **Status:** DONE
- **Priority:** HIGH
- **Phase:** 2
- **Estimated Scope:** L (4–8 hrs)
- **Architecture Reference:** [ARCH 3.2.11]
- **Requirement References:** [REQ maintainability NFR — 70% coverage]
- **Risk References:** [ARCH RISK-015]
- **Dependencies:** None (can proceed in parallel with all backend Phase 2 tasks)
- **Blocked By:** None

**Description:**
The Angular frontend has 1.5% test coverage (2 spec files across 133 TypeScript files). The target is 70% service coverage and 30% component coverage, enforced by Karma coverage thresholds in `angular.json`. Start with service-layer tests (isolated, no DOM rendering required), prioritising the 5 critical services.

**Services to test (in priority order):**
1. `auth.service.ts` — Full coverage: login, signup, JWT decode, token storage, logout, password reset
2. `application.service.ts` — Full coverage: submit, get, withdraw, status transitions
3. `room-allocation.service.ts` — Full coverage: accept offer, decline offer, check-in
4. `document.service.ts` — Full coverage: upload, get by student, attach to application
5. `notification.service.ts` — Full coverage: get notifications, mark read

After services, add coverage for critical guards:
6. `admin.guard.ts` — All role scenarios (update to use `RoleService` per TASK-2.03)
7. `student.guard.ts` — All role scenarios

Add Karma coverage threshold enforcement to `angular.json`:
```json
"codeCoverage": true,
"codeCoverageExclude": ["**/index.ts", "**/*.module.ts"],
"karmaConfig": "karma.conf.js"
```
And in `karma.conf.js`:
```javascript
coverageReporter: {
  thresholds: {
    global: {
      statements: 70, branches: 60, functions: 70, lines: 70
    }
  }
}
```

**Files to Touch:**
- `frontend/src/app/core/services/auth.service.spec.ts` — Create/expand (already exists as `token-storage.service.spec.ts` suggests structure)
- `frontend/src/app/core/services/application.service.spec.ts` — Create
- `frontend/src/app/core/services/room-allocation.service.spec.ts` — Create
- `frontend/src/app/core/services/document.service.spec.ts` — Create
- `frontend/src/app/core/services/notification.service.spec.ts` — Create
- `frontend/src/app/core/guards/admin.guard.spec.ts` — Create
- `frontend/src/app/core/guards/student.guard.spec.ts` — Create
- `frontend/karma.conf.js` — Add coverage thresholds
- `frontend/angular.json` — Enable `codeCoverage: true`

**Implementation Notes:**
- Per [ARCH 5.1.2] rule 7: "Every service file has a corresponding spec file. Missing spec = PR blocked." — this task creates the baseline; future PRs enforce it
- Use `HttpClientTestingModule` + `HttpTestingController` for HTTP mocking — no real HTTP calls in unit tests
- `RouterTestingModule` for guard tests
- JWT decode tests: use a known test JWT with known payload to test `getUserRole()` and `getTenantId()` parsing
- Handle the `uploadDocuments()` and `submitApplication()` methods in `auth.service.ts` — these are misplaced business operations ([ARCH 2.4]) but do NOT move them in this task (that's a separate refactor). Test them where they currently are
- CI must fail if `npm test` reports coverage below thresholds

**Acceptance Criteria:**
- [ ] 5 service spec files created with ≥ 80% coverage each
- [ ] 2 guard spec files created with 100% branch coverage
- [ ] Karma coverage thresholds enforced: ≥ 70% statements, ≥ 60% branches
- [ ] `npm test` fails CI if coverage drops below threshold
- [ ] All new tests pass in CI

**Tests to Write:**
- `AuthServiceSpec`: login, signup, logout, token decode, role extraction
- `ApplicationServiceSpec`: submit, get all, get by id, withdraw
- `RoomAllocationServiceSpec`: accept offer, decline offer (with reason), check-in
- `DocumentServiceSpec`: upload, get documents, (post-TASK-2.01) no `applicationId` in upload
- `NotificationServiceSpec`: get all, mark as read, get unread count

---

### TASK-2.13: Extend TenantFilterIntegrationTest for New Entities

- **Status:** DONE
 - **Priority:** HIGH
 - **Phase:** 2
 - **Estimated Scope:** M (2–4 hrs)
 - **Architecture Reference:** [ARCH 4.1 principle 4], [ARCH 5.1.1 rule 9]
 - **Requirement References:** [REQ AUTH-002], [REQ BR-P07], [REQ BR-S04]
 - **Risk References:** [ARCH RISK-011]
 - **Dependencies:** TASK-2.05, TASK-2.06, TASK-2.07, TASK-2.08, TASK-2.01, TASK-2.02 (all new entity tables must exist)
 - **Blocked By:** None (once entities are created in TASK-2.05–2.08)

**Description:**
[ARCH 5.1.1 rule 9]: "Every new JPA entity table with tenant data has a `tenant_id` FK and Hibernate filter annotation. Verified by `TenantFilterIntegrationTest`." The existing test class verifies existing entities; it must be extended to cover every entity added in Phase 2.

For each new entity, the test must assert:
1. A record created by Tenant A is visible when the request context carries Tenant A's JWT
2. The same record is NOT visible when the request context carries Tenant B's JWT
3. The same record is NOT visible when the request context carries `tenantId = 0` (student) — unless the entity IS student-owned

**New entities to add tests for:**
- `IntakePeriod` (TASK-2.05)
- `BuildingOwner` + `BuildingStaff` (TASK-2.06)
- `CustomApplicationField` + `CustomFieldAnswer` (TASK-2.07)
- `RequiredDocumentType` (TASK-2.08)
- `ApplicationDocument` (junction table from TASK-2.01 — verify student can access their own, owner can access for their building's applications)

**Files to Touch:**
- `backend/src/test/java/.../TenantFilterIntegrationTest.java` — Add test methods for each new entity

**Implementation Notes:**
- Use the existing test pattern from `TenantFilterIntegrationTest` — two tenant contexts, one record, assert visibility
- The student platform-level identity test (from TASK-2.02 acceptance criteria) may already live here — confirm and don't duplicate
- `ApplicationDocument` is special: it bridges a student-owned `document` and a tenant-scoped `application` — test that both the student and the building owner can see it (from their respective contexts) but a different building owner cannot

**Acceptance Criteria:**
- [ ] One test method per new entity verifying tenant isolation
- [ ] Tests for `ApplicationDocument` verifying student-owned document is accessible to the student AND the relevant building owner, but NOT to a different building owner
- [ ] All tests pass with the updated `TenantContext` sentinel logic from TASK-2.02
- [ ] CI passes

**Tests to Write:**
- `TenantFilterIntegrationTest.intakePeriod_notVisibleToCrossTenantQuery()`
- `TenantFilterIntegrationTest.buildingOwner_notVisibleToCrossTenantQuery()`
- `TenantFilterIntegrationTest.customApplicationField_notVisibleToCrossTenantQuery()`
- `TenantFilterIntegrationTest.requiredDocumentType_notVisibleToCrossTenantQuery()`

---

## Phase 3: Enhancement & Optimisation

> *Performance hardening, operational readiness, and non-critical improvements. Requires Phase 2 gate criteria to be met.*
>
> **Phase 2 Gate (all must be DONE before starting Phase 3):**
> - `TASK-2.01` through `TASK-2.13` all merged and CI passing
> - Second building owner tenant onboards without constraint violations
> - Student can apply to buildings from two different tenants
> - Documents exist at profile level, attachable to multiple applications
> - Angular services at ≥ 70% test coverage

---

### TASK-3.01: Configure MinIO SDK Timeout

- **Status:** DONE
- **Priority:** HIGH
- **Phase:** 3
- **Estimated Scope:** S (< 2 hrs)
- **Architecture Reference:** [ARCH 2.6], [ARCH 4.5]
- **Requirement References:** [REQ DOC-001]
- **Risk References:** None
- **Dependencies:** Phase 2 complete
- **Blocked By:** None

**Description:**
The MinIO SDK client is configured with no timeout. If MinIO is slow or unreachable, the `DocumentLifecycleService` upload call will block indefinitely — holding a thread and potentially exhausting the thread pool under burst conditions. Per [ARCH 4.5], the target timeout is 30s for both read and write operations.

In `MinioConfig.java`, configure the `MinioClient` builder with connection and read timeouts via OkHttp's timeout settings.

**Files to Touch:**
- `backend/src/main/java/com/beezhub/student_accommodation/config/MinioConfig.java` — Add `httpClient(new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build())` to `MinioClient.builder()`

**Implementation Notes:**
- `MinioClient` uses OkHttp internally — the builder accepts a custom `OkHttpClient`
- Import `okhttp3.OkHttpClient` (already on classpath via MinIO SDK)
- The timeout values should be driven by environment variables for flexibility: `${MINIO_CONNECT_TIMEOUT_S:30}`, `${MINIO_READ_TIMEOUT_S:30}`

**Acceptance Criteria:**
- [ ] `MinioConfig` builds `MinioClient` with 30s connect, read, and write timeouts
- [ ] If MinIO is slow (simulated), the SDK call fails within ~30s with a `MinioException` rather than hanging
- [ ] Timeout values are configurable via environment variables

**Tests to Write:**
- `MinioConfigTest`: `minioClient_hasTimeoutsConfigured()` — verify the OkHttpClient timeout settings are applied

---

### TASK-3.02: Wrap FF4J Behind FeatureFlagService Interface

- **Status:** DONE
- **Priority:** MEDIUM
- **Phase:** 3
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 3.5]
- **Requirement References:** [REQ CFG-001]
- **Risk References:** [ARCH RISK-016]
- **Dependencies:** Phase 2 complete
- **Blocked By:** None

**Description:**
FF4J 1.9 (released 2020) is deeply integrated but has a small surface area (only `auto-allocation` flag in use). Wrapping it behind a `FeatureFlagService` interface decouples all call sites from the FF4J API, enabling a future swap to a different flag library without touching business code.

Create `FeatureFlagService` interface:
```java
public interface FeatureFlagService {
    boolean isEnabled(String featureName);
    void enable(String featureName);
    void disable(String featureName);
}
```

Create `FF4JFeatureFlagService implements FeatureFlagService` delegating to the existing `FF4j` bean. Update all call sites that currently use `ff4j.check("auto-allocation")` to inject `FeatureFlagService` and call `isEnabled("auto-allocation")`.

**Files to Touch:**
- *(New)* `backend/src/main/java/com/beezhub/student_accommodation/service/FeatureFlagService.java` — New interface
- *(New)* `backend/src/main/java/com/beezhub/student_accommodation/service/FF4JFeatureFlagService.java` — New implementation
- All service/controller files that directly reference `FF4j` bean — replace with `FeatureFlagService` injection

**Acceptance Criteria:**
- [ ] `FeatureFlagService` interface created
- [ ] No direct `FF4j` references in business/service code (only in `FF4JFeatureFlagService`)
- [ ] Existing feature flag behaviour unchanged
- [ ] `FF4JFeatureFlagServiceTest` covers `isEnabled()`, `enable()`, `disable()`

**Tests to Write:**
- `FF4JFeatureFlagServiceTest`: `isEnabled_whenFlagActive_returnsTrue()`, `isEnabled_whenFlagInactive_returnsFalse()`

---

### TASK-3.03: Upgrade JJWT from 0.11.5 to 0.12.x

- **Status:** DONE
- **Priority:** MEDIUM
- **Phase:** 3
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 5.5], [ARCH 11.2]
- **Requirement References:** [REQ AUTH-001]
- **Risk References:** None
- **Dependencies:** Phase 2 complete
- **Blocked By:** None

**Description:**
JJWT `0.11.5` is outdated. `0.12.x` is the current stable series with breaking API changes (`JwtBuilder`, `JwtParserBuilder`, key handling). Upgrade `build.gradle` and refactor `JwtUtil.java` to use the new API.

Key API changes in `0.12.x`:
- `Jwts.builder().setSubject()` → `subject()`
- `Jwts.parserBuilder()` → `Jwts.parser()`
- Key is now passed as `SecretKey` built via `Keys.hmacShaKeyFor()`
- Claims extraction uses `claims.get("field", Type.class)`

**⚠️ Risk Flag:** This is a breaking API change. All JWT generation and parsing code in `JwtUtil.java` must be updated and regression-tested. Ensure token expiry, role extraction, and tenant claim extraction all work correctly after upgrade.

**Files to Touch:**
- `backend/build.gradle` — Update JJWT dependency versions to `0.12.x`
- `backend/src/main/java/com/beezhub/student_accommodation/config/JwtConfig.java` — Update key construction
- `backend/src/main/java/com/beezhub/student_accommodation/security/JwtAuthenticationFilter.java` *(or `JwtUtil.java`)* — Update `JwtParserBuilder` API calls

**Acceptance Criteria:**
- [ ] `build.gradle` uses JJWT `0.12.x`
- [ ] All JWT generation (login, signup) works correctly — tokens are valid
- [ ] All JWT parsing (filter authentication) works correctly — tokens are validated
- [ ] Role, tenant, and user ID claims extracted correctly
- [ ] All existing auth-related tests pass

**Tests to Write:**
- `JwtUtilTest`: verify all claims round-trip correctly after upgrade — generate token, parse it, assert all claims match

---

### TASK-3.04: Add RedisHealthIndicator

- **Status:** DONE
- **Priority:** MEDIUM
- **Phase:** 3
- **Estimated Scope:** S (< 2 hrs)
- **Architecture Reference:** [ARCH 6.4], [ARCH 2.7 SEC-001]
- **Requirement References:** N/A
- **Risk References:** None
- **Dependencies:** Phase 2 complete
- **Blocked By:** None

**Description:**
The `/actuator/health` endpoint shows MinIO and SMTP health indicators but not Redis. If Redis goes down, the token blacklist degrades to fail-open — blacklisted tokens become valid until Redis recovers. Without a health indicator, operators have no visibility into this condition.

Create a `RedisHealthIndicator` that pings Redis and reports `UP` or `DOWN`.

**Files to Touch:**
- *(New)* `backend/src/main/java/com/beezhub/student_accommodation/config/RedisHealthIndicator.java` — Implement `HealthIndicator`, ping Redis with `PING` command

**Implementation Notes:**
- Use `StringRedisTemplate` or `RedisConnectionFactory` to execute a `PING` command
- Per [ARCH 2.1]: Redis uses Lettuce with `fail-open` — `RedisHealthIndicator` must NOT throw exceptions; catch all Redis exceptions and return `Health.down(e).build()`
- The indicator should show in `/actuator/health` as `"redis": { "status": "UP" }`

**Acceptance Criteria:**
- [ ] `/actuator/health` includes `redis` component with `UP`/`DOWN` status
- [ ] Stopping Redis container changes status to `DOWN` without affecting application availability
- [ ] Exception from Redis returns `DOWN` (not an error response from `/actuator/health`)

**Tests to Write:**
- `RedisHealthIndicatorTest`: `health_whenRedisUp_returnsUp()`, `health_whenRedisDown_returnsDown()`

---

### TASK-3.05: Add Grafana Alerting Rules

- **Status:** TODO
- **Priority:** MEDIUM
- **Phase:** 3
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 6.4]
- **Requirement References:** N/A
- **Risk References:** [ARCH RISK-008]
- **Dependencies:** TASK-1.08 (scheduled job Prometheus counters must exist), TASK-3.04 (Redis health indicator), Phase 2 complete
- **Blocked By:** None

**Description:**
Configure Grafana alerting rules for the three critical alert scenarios:
1. **Scheduled job failure:** `jobs.*.failures` counter increments → alert
2. **PERMANENTLY_FAILED notifications:** notification status count for `PERMANENTLY_FAILED` > 0 → alert Platform Admin
3. **High error rate:** HTTP 5xx responses > 1% of total requests over 5 minutes → alert

Grafana alert rules should be stored as JSON provisioning files in `tools/grafana/alerts/` alongside the existing Grafana provisioning config.

**Files to Touch:**
- `tools/grafana/alerts/job-failure-alerts.json` — New alert rules file
- `tools/grafana/alerts/notification-failure-alerts.json` — New alert rules file
- `tools/grafana/alerts/error-rate-alerts.json` — New alert rules file
- `docker-compose.yml` — Verify Grafana provisioning mounts the `alerts/` directory

**Implementation Notes:**
- Grafana alerting uses the provisioning API — alert rules are YAML or JSON files mounted into the container
- The `jobs.*.failures` counter name from TASK-1.08 must match what's used in the alert query
- For PERMANENTLY_FAILED: add a Prometheus counter in `NotificationRetryService` for each time a notification reaches PERMANENTLY_FAILED status
- For error rate: use `http_server_requests_seconds_count{status=~"5.."}` metric from Micrometer

**Acceptance Criteria:**
- [ ] Three alert rule files exist in `tools/grafana/alerts/`
- [ ] Grafana loads the alert rules on startup (visible in Grafana Alerting UI)
- [ ] Simulating a scheduled job failure triggers the job failure alert
- [ ] Alert notification channel is configured (e.g., email or webhook)

**Tests to Write:**
- Manual verification — trigger each alert condition, confirm Grafana shows alert state

---

### TASK-3.06: Commit Grafana Dashboard JSON to Repository

- **Status:** TODO
- **Priority:** LOW
- **Phase:** 3
- **Estimated Scope:** S (< 2 hrs)
- **Architecture Reference:** [ARCH 6.4]
- **Requirement References:** N/A
- **Risk References:** None
- **Dependencies:** TASK-3.05 (alerts added), Phase 2 complete
- **Blocked By:** None

**Description:**
Grafana dashboards exist in the running container but are not committed to the repository. If the container is recreated, dashboards are lost. Provision all dashboards as JSON files in `tools/grafana/dashboards/`.

Export each dashboard from Grafana UI (Dashboard → Share → Export JSON) and commit the file. Add provisioning config so Grafana loads them on startup.

**Files to Touch:**
- `tools/grafana/dashboards/job-health.json` — Scheduled job metrics (processed/failures per job)
- `tools/grafana/dashboards/allocation-metrics.json` — Allocation runs, offers issued/accepted/expired
- `tools/grafana/dashboards/notification-health.json` — Email delivery rates, PERMANENTLY_FAILED count
- `tools/grafana/provisioning/dashboards/dashboards.yml` — Provisioning config pointing to `dashboards/` directory

**Acceptance Criteria:**
- [ ] Dashboard JSON files committed to `tools/grafana/dashboards/`
- [ ] `docker-compose down && docker-compose up` — dashboards are present in Grafana without manual re-import
- [ ] Dashboards show data from Prometheus metrics

**Tests to Write:**
- Manual verification only

---

### TASK-3.07: Add @Timed to Critical Service Methods

- **Status:** TODO
- **Priority:** MEDIUM
- **Phase:** 3
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 6.1], [ARCH 6.4]
- **Requirement References:** [REQ performance NFR — P95 < 500ms]
- **Risk References:** None
- **Dependencies:** Phase 2 complete
- **Blocked By:** None

**Description:**
No P95 API response time baseline exists. The NFR requires P95 < 500ms. Add `@Timed` Micrometer annotations to critical service methods so their execution time is tracked in Prometheus and visible in Grafana.

**Services to instrument:**
1. `ApplicationService` (facade) — `submitApplication()`, `getApplicationById()`, `approveApplication()`
2. `RoomAllocationService` (facade) — `acceptOffer()`, `declineOffer()`, `runAllocationBatch()`
3. `DocumentLifecycleService` — `uploadDocument()`
4. `AuthService` — `login()`, `signup()`
5. `AllocationBatchService` — `runBatch()` (full batch execution time)

**Files to Touch:**
- `backend/src/main/java/com/beezhub/student_accommodation/service/ApplicationService.java` — Add `@Timed`
- `backend/src/main/java/com/beezhub/student_accommodation/service/RoomAllocationService.java` — Add `@Timed`
- `backend/src/main/java/com/beezhub/student_accommodation/service/DocumentLifecycleService.java` — Add `@Timed`
- `backend/src/main/java/com/beezhub/student_accommodation/service/AuthService.java` — Add `@Timed`
- `backend/src/main/java/com/beezhub/student_accommodation/service/allocation/AllocationBatchService.java` — Add `@Timed`

**Implementation Notes:**
- `@Timed(value = "service.application.submit", percentiles = {0.5, 0.95, 0.99})` — the `percentiles` array enables P50/P95/P99 tracking in Prometheus
- Requires `@EnableAspectJAutoProxy` and the Micrometer AOP dependency — confirm it's on the classpath
- The metric name convention: `service.{domain}.{method}` (e.g., `service.allocation.acceptOffer`)

**Acceptance Criteria:**
- [ ] `@Timed` annotations added to all 9 listed methods
- [ ] Prometheus scrape shows `service.application.submit_seconds_max` (and equivalent for others)
- [ ] Grafana P95 SLA panel shows latency data after load

**Tests to Write:**
- No new tests — observability instrumentation only; verify via `/actuator/metrics`

---

### TASK-3.08: k6 Load Test — 500 VU Soak Test

- **Status:** TODO
- **Priority:** MEDIUM
- **Phase:** 3
- **Estimated Scope:** L (4–8 hrs)
- **Architecture Reference:** [ARCH 6.2]
- **Requirement References:** [REQ scalability NFR — 500 concurrent users]
- **Risk References:** None
- **Dependencies:** TASK-3.07 (metrics must be instrumented before load test to capture P95), Phase 2 complete
- **Blocked By:** None

**Description:**
No load testing has been performed. The platform has a scalability NFR of 500 concurrent users without degradation. Write a k6 load test script simulating the primary user journey — student login, application submission, document upload, offer acceptance — and run a 30-minute soak test at 500 virtual users. Document the results.

**k6 scenarios to implement:**
- **Student login** (`POST /api/auth/login`)
- **Get applications** (`GET /api/applications/my`)
- **Submit application** (`POST /api/applications`)
- **Get notifications** (`GET /api/notifications`)
- **Accept offer** (`POST /api/allocations/{id}/accept`)

**Thresholds to assert:**
```javascript
thresholds: {
  http_req_duration: ['p(95)<500'],  // P95 < 500ms
  http_req_failed: ['rate<0.01'],    // < 1% error rate
}
```

**Files to Touch:**
- *(New)* `tools/k6/load-test.js` — k6 test script
- *(New)* `tools/k6/README.md` — Instructions for running the test and interpreting results

**Implementation Notes:**
- k6 must run against the full Docker Compose stack (not mocked services)
- Pre-create 500 student accounts + 1 building with 500 rooms before the soak test begins
- The `AllocationScoringService` N+1 fix (TASK-2.10) must be in place before this test is meaningful
- HikariCP pool increase (TASK-2.04) must be in place
- Record and commit the results: P95 response times per endpoint, error rates, DB connection wait times

**Acceptance Criteria:**
- [ ] k6 test script in `tools/k6/load-test.js` covers all 5 API scenarios
- [ ] Soak test runs for ≥ 30 minutes at 500 VUs
- [ ] P95 API response time ≤ 500ms for all endpoints
- [ ] Error rate < 1%
- [ ] HikariCP connection wait time does not exceed 30s during the test
- [ ] Test results documented in `tools/k6/README.md`

**Tests to Write:**
- The k6 script IS the test — run with `k6 run tools/k6/load-test.js`

---

### TASK-3.09: Lighthouse CI Audit — Frontend Load Performance

- **Status:** TODO
- **Priority:** MEDIUM
- **Phase:** 3
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 6.1]
- **Requirement References:** [REQ performance NFR — < 3s on 3G]
- **Risk References:** None
- **Dependencies:** Phase 2 complete
- **Blocked By:** None

**Description:**
No Lighthouse baseline exists for the Angular frontend. The NFR requires initial page load < 3s on 3G. Integrate `lighthouse-ci` into the CI pipeline to enforce a Lighthouse performance score ≥ 80 and First Contentful Paint ≤ 3s.

**Files to Touch:**
- *(New)* `.lighthouserc.json` — Lighthouse CI configuration
- *(New)* `.github/workflows/lighthouse.yml` (or update existing CI config) — Lighthouse CI step

**Implementation Notes:**
- `lighthouserc.json` assertions:
  ```json
  {
    "ci": {
      "assert": {
        "assertions": {
          "categories:performance": ["warn", {"minScore": 0.8}],
          "first-contentful-paint": ["error", {"maxNumericValue": 3000}]
        }
      }
    }
  }
  ```
- Lighthouse CI must run against the production Angular build (`npm run build`), not the dev server
- Run against the Docker Compose stack or a deployed preview environment

**Acceptance Criteria:**
- [ ] `.lighthouserc.json` committed to repository root
- [ ] CI pipeline runs Lighthouse audit on every PR affecting frontend files
- [ ] Lighthouse performance score ≥ 80
- [ ] First Contentful Paint ≤ 3s
- [ ] PRs failing the audit are blocked from merging

**Tests to Write:**
- Lighthouse CI itself is the test

---

### TASK-3.10: Offer Expiry Reminder Notifications (24h Before Deadline)

- **Status:** DONE
- **Priority:** MEDIUM
- **Phase:** 3
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 9 Phase 3 item 10]
- **Requirement References:** [REQ NOTIF-001 item 12]
- **Risk References:** None
- **Dependencies:** TASK-1.08 (scheduled jobs pattern established), Phase 2 complete
- **Blocked By:** None

**Description:**
Students currently receive no reminder that their room offer is about to expire. [REQ NOTIF-001 item 12] requires an offer expiry reminder sent 24 hours before the offer deadline.

Add a new scheduled job (following the TASK-1.08 observability pattern) that:
1. Runs every hour
2. Queries for `room_allocation` records with `status = 'OFFERED'` AND `offer_expiry_date BETWEEN now() AND now() + 24 hours`
3. Checks that a reminder has not already been sent (add `reminder_sent_at` column OR a flag to prevent duplicate reminders)
4. Sends email + in-app notification: "Your room offer expires in 24 hours. Accept or decline now."

**Files to Touch:**
- `backend/src/main/resources/db/migration/V2026_02_24_0030__add_offer_reminder_sent.sql` — Add `reminder_24h_sent BOOLEAN DEFAULT false` to `room_allocation`
- `backend/src/main/java/com/beezhub/student_accommodation/model/entity/RoomAllocation.java` — Add `reminder24hSent` field
- `backend/src/main/java/com/beezhub/student_accommodation/service/allocation/OfferExpiryService.java` *(or new `OfferReminderService.java`)* — Add scheduled reminder method with observability wrapper
- `backend/src/main/java/com/beezhub/student_accommodation/service/notification/` — Add `OFFER_EXPIRY_REMINDER` notification type/template

**Implementation Notes:**
- The observability wrapper pattern from TASK-1.08 must be applied to the new scheduled method
- Use `idx_room_allocation_offer_expiry` partial index (already exists) to make the query efficient
- Reminder deduplication: `reminder_24h_sent = false` in query; set to `true` after sending
- Per [ARCH 5.1.1]: `@Transactional` on the method that queries + updates the flag to prevent race conditions

**Acceptance Criteria:**
- [ ] Scheduled job runs hourly and sends reminders to students with offers expiring within 24h
- [ ] Reminder sent exactly once per offer (no duplicates)
- [ ] Reminder includes: building name, room number, exact expiry time, accept/decline link
- [ ] Email + in-app notification both sent
- [ ] Job emits Prometheus counter `jobs.offerReminder.processed`

**Tests to Write:**
- `OfferReminderServiceTest`: `reminderJob_withOfferExpiring24hFromNow_sendsReminder()`
- `OfferReminderServiceTest`: `reminderJob_withReminderAlreadySent_doesNotSendAgain()`

---

### TASK-3.11: Check-in Deadline Reminder Notifications (24h Before Deadline)

- **Status:** DONE

**Description:**
Students who have accepted a room offer receive no reminder that their check-in deadline is approaching. [REQ NOTIF-001 item 13] requires a reminder 24 hours before the check-in deadline.

Follow the identical pattern established in TASK-3.10:
1. Scheduled job runs every hour
2. Queries `room_allocation` with `status = 'ACCEPTED'` AND `check_in_deadline BETWEEN now() AND now() + 24 hours`
3. Sends: "Reminder: You must check in to [Building] by [deadline]. Contact your building manager if you need assistance."

**Files to Touch:**
- `backend/src/main/resources/db/migration/V2026_02_24_0031__add_checkin_reminder_sent.sql` — Add `checkin_reminder_24h_sent BOOLEAN DEFAULT false` to `room_allocation`
- `backend/src/main/java/com/beezhub/student_accommodation/model/entity/RoomAllocation.java` — Add `checkInReminder24hSent` field
- `backend/src/main/java/com/beezhub/student_accommodation/service/allocation/CheckInExpiryService.java` *(or new `CheckInReminderService.java`)* — Add reminder scheduled method

**Acceptance Criteria:**
- [ ] Check-in reminder sent exactly once per accepted allocation
- [ ] Reminder sent 24h before check-in deadline
- [ ] Email + in-app notification both sent
- [ ] Job emits `jobs.checkInReminder.processed` Prometheus counter

**Tests to Write:**
- `CheckInReminderServiceTest`: `reminderJob_withDeadlineIn24h_sendsReminder()`
- `CheckInReminderServiceTest`: `reminderJob_withReminderAlreadySent_skips()`

---

### TASK-3.12: Evaluate HttpOnly Cookie JWT Storage

- **Status:** DONE
- **Priority:** LOW
- **Phase:** 3
- **Estimated Scope:** M (2–4 hrs) for evaluation; XL (8+ hrs) for implementation if approved
- **Architecture Reference:** [ARCH 2.7 SEC-006], [ARCH 3.5]
- **Requirement References:** [REQ AUTH-001]
- **Risk References:** None
- **Dependencies:** Phase 2 complete
- **Blocked By:** None

**Description:**
JWT tokens are currently stored in `localStorage`. XSS attacks can read localStorage tokens. The "Too Risky to Touch Now" classification was appropriate for single-first-customer MVP, but should be re-evaluated once multi-building-owner is live (expanded XSS attack surface).

This task is an **evaluation and decision task**, not necessarily a full implementation:
1. Assess the scope of migrating from `localStorage` to `HttpOnly` cookie storage
2. Document the required backend changes (cookie endpoint, CSRF protection)
3. Document the required frontend changes (`AuthService`, `AuthInterceptor`, logout flow)
4. Assess if the current CSP headers mitigate the risk sufficiently for current customer scale
5. Make a recommendation: implement now, or defer to Phase 4

**Files to Touch:**
- *(New)* `docs/adr/ADR-007-jwt-cookie-storage.md` — Document the evaluation and decision

**Acceptance Criteria:**
- [ ] Written evaluation: current risk level, migration scope, recommendation
- [ ] If recommendation is "implement now": implementation plan scoped as subtasks
- [ ] If recommendation is "defer": clear trigger criteria for when to revisit

**Tests to Write:**
- If implementation proceeds: `AuthServiceSpec`: `login_storesTokenInHttpOnlyCookie_notLocalStorage()`

---

### TASK-3.13: Plan application Table Partitioning

- **Status:** DONE
- **Priority:** LOW
- **Phase:** 3
- **Estimated Scope:** S (< 2 hrs) for planning; M (2–4 hrs) for implementation
- **Architecture Reference:** [ARCH 6.2]
- **Requirement References:** [REQ scalability NFR — 50,000 applications per year]
- **Risk References:** None
- **Dependencies:** Phase 2 complete
- **Blocked By:** None

**Description:**
The `application` table has no partitioning. Per [ARCH 6.2]: "Plan range partitioning on `application(created_at)` at 100,000 rows." This is a planning task — monitor row counts and create the partitioning migration when the 100,000 threshold is approached, not before.

This task produces a documented plan and a monitoring query — implementation is deferred until the threshold is reached.

**Deliverables:**
1. Monitoring query: `SELECT COUNT(*) FROM application;` — add to Grafana dashboard as a data table
2. Partitioning plan documented: year-range partitions on `created_at`, similar to the existing `activity_logs` partitioning pattern
3. Estimated timeline: at 50,000 applications/year (target), partitioning is needed after year 2

**Files to Touch:**
- *(New)* `docs/adr/ADR-008-application-table-partitioning.md` — Document the plan and trigger conditions
- `tools/grafana/dashboards/` — Add application row count panel to an existing dashboard

**Acceptance Criteria:**
- [ ] Written partitioning plan with trigger criteria (100,000 row threshold)
- [ ] Grafana panel monitoring application table row count
- [ ] Plan references existing `activity_logs` partitioning as the pattern to follow

**Tests to Write:**
- None — planning task

---

## Phase Gate Checklists

### Phase 1 → Phase 2 Gate

All of the following must be verified before any Phase 2 task begins:

- [x] `TASK-1.01` DONE — `hbm2ddl.auto: validate` — application starts clean
- [x] `TASK-1.02` DONE — `room.room_number` constraint is per-building
- [x] `TASK-1.03` DONE — `system_settings.setting_key` constraint is per-tenant
- [x] `TASK-1.04` DONE — JWT secret has no default; startup fails without `JWT_SECRET`
- [x] `TASK-1.05` DONE — SMTP TLS/auth driven by env vars
- [x] `TASK-1.06` DONE — `postgres_data` volume mounted; data persists across restarts
- [x] `TASK-1.07` DONE — Flyway safety settings restored; `flyway:validate` passes
- [x] `TASK-1.08` DONE — All 5 scheduled jobs emit Prometheus counters
- [x] CI pipeline passes with all Phase 1 changes merged
- [x] No regression in existing functionality (existing tests all pass)

### Phase 2 → Phase 3 Gate

All of the following must be verified before any Phase 3 task begins:

- [x] `TASK-2.01` DONE — Documents are student-owned; `application_document` junction table active
- [ ] `TASK-2.02` DONE — Student records use `tenant_id = 0`; cross-tenant building access works
- [ ] `TASK-2.03` DONE — v2.0 roles active; all `@PreAuthorize` annotations updated
- [ ] `TASK-2.04` DONE — HikariCP `maximum-pool-size: 25`
- [ ] `TASK-2.05` DONE — `intake_period` table exists
- [ ] `TASK-2.06` DONE — `building_owner` and `building_staff` tables exist
- [ ] `TASK-2.07` DONE — `custom_application_field` and `custom_field_answer` tables exist
- [ ] `TASK-2.08` DONE — `required_document_type` table exists
- [x] `TASK-2.09` DONE — `ScoringStrategy` interface extracted; `LegacyScoringStrategy` all tests pass
- [x] `TASK-2.10` DONE — N+1 scoring queries eliminated; 2 bulk queries per batch run
- [ ] `TASK-2.11` DONE — `OwnerConfigurableScoringStrategy` at ≥ 90% coverage; is `@Primary`
- [x] `TASK-2.12` DONE — Angular service coverage ≥ 70%; Karma thresholds enforced in CI
- [ ] `TASK-2.13` DONE — All new entities covered by `TenantFilterIntegrationTest`
- [ ] A second building owner tenant can onboard without any constraint violations
- [ ] A student can apply to buildings from two different building owner tenants
- [ ] CI pipeline passes with all Phase 2 changes merged

---

*Document End — Task List v1.0 — Generated 2026-02-23*
*Update task statuses as work progresses. Add new tasks discovered during implementation with the next available ID.*
