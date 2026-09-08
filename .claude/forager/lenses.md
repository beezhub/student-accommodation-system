# Forager's lenses

One lens per run. Each check below is something `CLAUDE.md`, `CONTRIBUTING.md`, or an ADR
says about this codebase, turned into something you can run. The rule is quoted or
paraphrased first so you can judge whether a hit is really a violation.

Paths are relative to the repository root. Backend package root is
`backend/src/main/java/com/beezhub/student_accommodation`, written `$B` below. Frontend
app root is `frontend/src/app`, written `$F`.

A hit is a candidate, not a finding. Every candidate goes through Step 4 of the playbook.

---

## `tenancy` (Monday)

Rules: every `TenantAware` entity carries the Hibernate filter and has an isolation test;
the filter does not apply to `findById`, so every admin path loading a tenant-owned row by
id calls `TenantOwnershipGuard`; a service read without `@Transactional` is unfiltered;
students are tenant 0, so student-reachable endpoints by id need `AllocationAccessGuard`;
scheduled and async code sees every tenant unless it sets `TenantContext`.

- **Entities without the filter.** List classes that `implements TenantAware` (or extend
  one that does). For each, confirm `@Filter(name = "tenantFilter"` on the class and a test
  under `backend/src/test` whose name or body mentions the entity and `tenant`. A missing
  test is a `tests`-lens finding too, but file it here: isolation is the point.
- **Loads by id without a guard.** In `$B/service`, find `findById(` and
  `getReferenceById(` calls on repositories of tenant-aware entities, reached from a method
  a `PLATFORM_ADMIN` or `TENANT_ADMIN` can call. Check whether `TenantOwnershipGuard`
  (`assertOwned` for writes, `assertReadable` for reads) runs on the loaded row before it
  is returned or changed. Read the calling controller's `@PreAuthorize` to know who reaches
  it.
- **Unfiltered reads.** Public service methods that call a tenant-aware repository and carry
  no `@Transactional`. Look for `action=tenantFilterNotApplied` in the log output of any
  test you run; the aspect logs it for you.
- **Student endpoints by id.** In controllers with `hasAnyRole('STUDENT'`, every method
  taking an id must lead to `AllocationAccessGuard` or `DocumentAccessGuard` in the
  service. Trace one level down; do not assume.
- **Admin controllers without a role check.** Any `@RequestMapping` under `/api/admin` or
  `/api/platform` whose class or methods lack `@PreAuthorize`. `SecurityConfig` matches
  `/api/admin/**` as well, so this is defence in depth, not the only gate: rate it low unless
  the method is platform-only and `TENANT_ADMIN` could reach it.
- **Background code and tenant context.** Methods called from `ScheduledJobRunner`, and
  every `@Async` or `@TransactionalEventListener` method, that read or write tenant-aware
  entities. Check whether they set `TenantContext` or deliberately run across all tenants,
  and whether the code says which.
- **Dual FK mapping.** Tenant-aware entities should have a writable `@Column tenantId` and
  a read-only `@ManyToOne tenant`. One without the other is a candidate.

## `backend` (Tuesday)

Rules: constructor injection only; `@Transactional` on methods, `readOnly` for reads, never
class-level; `FetchType.LAZY` everywhere; paginate every list endpoint; throw domain
exceptions, never generic runtime ones; `ScheduledJobRunner` owns every `@Scheduled`; only
`FF4JFeatureFlagService` touches the FF4j bean; status changes go through
`canTransitionTo()` or `AllocationStatusValidator`; service under 400 lines, controller
under 200, method under 50; structured `key=value` logging with no PII at WARN or ERROR;
admin mutations carry `@LogActivity`.

- **Class-level `@Transactional`.** `grep -rln -B1 '^@Transactional' --include=*.java $B`
  and confirm the annotation sits on a `class`, not a method.
- **Field injection.** `@Autowired` on a field, or `@Autowired` anywhere in `$B` other than
  a single constructor.
- **EAGER loading.** `FetchType.EAGER` anywhere. Also `@ManyToOne` and `@OneToOne` with no
  `fetch =` at all, since their default is EAGER.
- **Unpaginated lists.** Controller methods returning `List<...>` or `ResponseEntity<List<...>>`
  from a `GET` that can grow with data: buildings, rooms, applications, users, logs. A
  fixed enum list is not a candidate.
- **Generic exceptions.** `throw new RuntimeException`, `IllegalStateException`, or
  `IllegalArgumentException` in `$B/service`, where `exceptions/` has a fitting class or
  `RestResponseExceptionHandler` would map the wrong status.
- **Scheduling and flags outside their owners.** `@Scheduled` in any file but
  `ScheduledJobRunner`. `FF4j` or `ff4j.` referenced in any class but `FF4JFeatureFlagService`
  and `FF4jConfig`.
- **Unvalidated status changes.** `.setStatus(` on an `Application` or `RoomAllocation` with
  no `canTransitionTo` or `AllocationStatusValidator.validate` on the path to it.
- **Size limits.** `wc -l` over `$B/service/**/*.java` above 400 and `$B/controller/*.java`
  above 200. This is one issue listing the offenders with their line counts, `enhancement`,
  `severity:low`, and only if no open issue already tracks the list. Do not file the same
  list twice; if an open issue has it, add a comment only when the set of offenders changed.
- **PII in logs.** `log.warn(` and `log.error(` lines whose arguments include an email,
  a name, an ID number, or a phone field. Also any log line that is not `key=value` shaped
  in code that is otherwise structured.
- **Audit gaps.** Public methods in admin-facing services that create, update, or delete and
  carry no `@LogActivity`, where sibling methods in the same class do.
- **Facade creep.** New logic in `RoomAllocationService`, `ApplicationService`, or
  `NotificationService` rather than in the sub-package. Compare method counts against
  `git log -p` for the last month if the facade has grown.

## `frontend` (Wednesday)

Rules: role checks go through `RoleService`; platform-only routes add `PlatformAdminGuard`
and `/admin/settings` and `/admin/activity-logs` must not; components are standalone and
mostly `OnPush`; component TS under 300 lines; no `console.log` of user data; native
`confirm`, `alert`, `prompt` are not used; `X-Silent-Error` when a component handles the
error itself; tokens and mixins over hardcoded values.

- **String role checks.** `=== 'PLATFORM_ADMIN'`, `'TENANT_ADMIN'`, `'STUDENT'`,
  `'BUILDING_STAFF'`, or `.role ===` anywhere in `$F` except `role.service.ts` and specs.
- **Route guards.** In `app-routing.module.ts`, every child under `/admin` that the backend
  restricts to `PLATFORM_ADMIN` should carry `PlatformAdminGuard`; check against the
  controller's `@PreAuthorize`. And the two routes named above must not carry it (GH-158).
- **Oversized components.** `wc -l` over `$F/**/*.component.ts` above 300. One issue listing
  them, same rule as the backend size list.
- **Console output and dialogs.** `console.log`, `console.debug` outside specs, and
  `window.confirm`, `confirm(`, `alert(`, `prompt(` in components.
- **Subscription leaks.** `.subscribe(` in a component with no `takeUntilDestroyed`,
  `DestroyRef`, `ngOnDestroy` unsubscribe, or `async` pipe alternative. Ignore one-shot HTTP
  calls that complete; flag `interval`, `valueChanges`, router events, and service
  `BehaviorSubject`s.
- **Error handling.** HTTP calls whose `error:` handler shows its own message while the
  request lacks `X-Silent-Error`, so the user sees two messages.
- **Change detection.** Components without `changeDetection: ChangeDetectionStrategy.OnPush`
  where every sibling in the same feature has it.
- **Dead and duplicate code.** Components not referenced from any route or template.
  Duplicate models between `core/models/` and `app/model/` that have drifted apart in shape.
- **Hardcoded styling.** New or heavily changed `.scss` files using literal colours or
  spacing where `styles/_variables` has a token. Check `frontend/docs/STYLING_AUDIT_REPORT.md`
  first; it already lists most of the old debt.

## `tests` (Thursday)

Rules: 70% line coverage overall and per direct service class, 90% on
`AllocationScoringService`; state machines are tested on the valid and invalid path; test
names are `methodName_scenario_expectedResult`; never skip or disable a test to get green;
the toolchain version must agree across `build.gradle`, `Dockerfile`, and `ci.yml`.

- **Services with no test.** For each class in `$B/service/**`, look for `<Name>Test.java`
  or a test that imports it. List the ones with none; prefer the ones handling money,
  status, tenancy, or documents.
- **State machines.** For `ApplicationStatus.canTransitionTo` and
  `AllocationStatus.canTransitionTo`, confirm a test asserts a rejected transition, not only
  accepted ones.
- **Vacuous and disabled tests.** `@Disabled`, `@Ignore`, a test method with no assertion,
  `assertTrue(true)`, `xit(`, `fit(`, `xdescribe(`, `fdescribe(` in the frontend.
- **Coverage exclusions.** New entries under jacoco exclusions in `backend/build.gradle`
  since the rule was written. `git log -p -- backend/build.gradle` shows when.
- **Frontend coverage.** Guards, interceptors, and `core/services` with no `.spec.ts`. One
  issue per meaningful gap, not a count.
- **End-to-end.** `frontend/e2e` tests that assert only a title or a URL.
- **CI and toolchain drift.** `java-version` and `node-version` across every file in
  `.github/workflows/`, against `JavaLanguageVersion` in `backend/build.gradle`, the
  `FROM` tags in `backend/Dockerfile` and `frontend/Dockerfile`, and `engines` in
  `frontend/package.json`. `ToolchainVersionConsistencyTest` covers three of these; the
  workflow files it does not cover are where drift hides.
- **Workflow hygiene.** Actions not pinned to a major version, a workflow without
  `concurrency`, a job with no `timeout-minutes`, `paths-ignore` lists that disagree
  between workflows.

## `docs-config` (Friday)

Rules: `.env.example` documents every variable; `CLAUDE.md` describes the code as it is;
README commands exist; compose files agree with each other about services, ports, and
health checks; ADR status matches what shipped.

- **Undocumented variables.** Every `${NAME` placeholder in
  `backend/src/main/resources/application.yml` (and any other `application*.yml` that is
  actually loaded) must appear in `.env.example`. And the reverse: variables in
  `.env.example` that nothing reads. One issue for the set.
- **CLAUDE.md against the tree.** For every backticked identifier in `CLAUDE.md` that
  looks like a class, file, or migration name, confirm it exists. For every number
  (line limits, counts, versions, ports, token lifetimes), confirm the code agrees. A
  stale sentence in `CLAUDE.md` misleads every agent and every new contributor.
- **README and docs commands.** Every `npm run <script>` mentioned anywhere under `docs/`,
  `README*.md`, or `frontend/README.md` must exist in `frontend/package.json`; every
  `./gradlew <task>` must exist. `CLAUDE.md` already records `npm run prepare` as known
  debt; do not refile it.
- **Compose agreement.** `docker-compose.yml`, `docker-compose.prod.yml`, and
  `docker-compose.deploy.yml`: the same service must use the same image tag, the same
  health check command, and ports that do not collide. A health check that calls a binary
  the image does not have is high; issue 256 was exactly that.
- **ADR drift.** Each `docs/adr/*.md` has a status. Confirm the code matches it: a
  "Deferred" ADR whose change has landed, or an "Accepted" one whose change has not.
- **Runbook drift.** `docs/OPERATOR_RUNBOOK.md` and `docs/BACKUP_AND_RESTORE.md` name
  workflows, secrets, and hosts. Each named workflow file and each named secret should
  appear in `.github/workflows/`.
- **Swagger.** New controller methods with no `@Operation` summary where the rest of the
  class has them. Low severity; file only if a whole controller is bare.

## `database` (Saturday)

Rules: Hibernate `validate` checks names and types only, not nullability, defaults, length,
foreign keys, unique constraints, or indexes; adding NOT NULL is three steps; names are
`snake_case`, booleans `is_*`, FKs `{entity}_id`, indexes `idx_{table}_{cols}`, uniques
`uq_{table}_{cols}`; enums are stored as strings; never edit an applied migration.

- **Entity against migration, the part validate skips.** For each entity in `$B/model/entity`,
  compare `nullable = false`, `length =`, and `unique = true` on `@Column` against the
  `CREATE TABLE` and `ALTER TABLE` statements for that table across
  `backend/src/main/resources/db/migration/`. A mismatch fails at runtime, never in CI.
- **Foreign keys without an index.** Every `REFERENCES` column in the migrations should have
  a matching `CREATE INDEX`. `tenant_id` on every tenant-aware table especially, since the
  filter adds it to every query.
- **Unique constraints entities assume.** `@Table(uniqueConstraints` or `unique = true` in
  an entity with no matching constraint in a migration, and the reverse.
- **Naming.** Any column, index, or constraint in a migration that breaks the naming rules.
  One issue for the set.
- **NOT NULL in one step.** A migration adding `NOT NULL` to an existing table without a
  `DEFAULT` or a preceding backfill `UPDATE`.
- **Enum storage.** `@Enumerated` without `EnumType.STRING`, or an enum field with no
  `@Enumerated` at all.
- **Edited migrations.** `git log --follow --oneline -- <migration>` showing more than one
  commit for any migration that is not the newest. Read the diff before you decide; a
  whitespace fix is not a schema edit, but it still breaks the checksum, so say so.
- **Migration file names.** Anything not matching `V{YYYY_MM_DD_HHMM}__{description}.sql`.

## `housekeeping` (Sunday)

Playbook Step 1 only. Re-verify every open `forager:found` issue, close what is fixed,
answer what was asked. File nothing new. Report what you closed.

---

## Security, any day

A finding that exposes data across tenants, lets a student reach another student's record,
or shows a real credential in a tracked file does not wait for its lens. File it the day
you see it, `severity:high`, `area:security`, and for a credential name the file and line
and nothing else. It still counts toward the three.
