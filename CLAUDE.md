# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Multi-tenant student-accommodation platform: a Spring Boot 3.5 / Java 21 monolith in `backend/` and an Angular 21 +
Material standalone-component app in `frontend/`. PostgreSQL 16 (Flyway), Redis (token blacklist, rate limiting),
MinIO (documents), SMTP via MailHog locally, FF4j feature flags, Prometheus + Grafana metrics, Google Maps geocoding.

Where the intent lives:

- `docs/adr/` — architecture decision records (ADR-007 JWT cookie storage is *deferred*, ADR-008 application partitioning).
- `test-cases/` — manual UAT plan per domain; `ux-research/` — student and admin research; `tools/k6/` — load tests;
  `tools/grafana/`, `tools/prometheus/` — dashboards, alerts, scrape config mounted by docker-compose.
- `tools/*-prompt.md` — role system prompts for AI-assisted planning, not application code.

## Commands

### Backend (`cd backend`)

```bash
./gradlew bootRun                                   # :8080, Swagger at /swagger-ui.html, health at /actuator/health
./gradlew clean build                               # compile + tests + jacoco report + coverage verification
./gradlew test                                      # JUnit 5; HTML report in build/jacocoHtml
./gradlew test --tests "com.beezhub.student_accommodation.service.allocation.AllocationBatchServiceTest"
./gradlew test --tests "*AllocationBatchServiceTest.methodName*"
./gradlew jacocoTestCoverageVerification            # 70% line coverage overall and per direct service class; 90% on AllocationScoringService
./gradlew flywayInfo
```

Integration tests (`AbstractIntegrationTest`, `AbstractJpaTest`) start Testcontainers, so Docker must be running for the
full suite. CI (`.github/workflows/ci.yml`) runs a secrets scan, `./gradlew build -x test` then `test` + jacoco, and
frontend `lint`, `test`, `build`.

### Frontend (`cd frontend`)

```bash
npm install
npm start                      # ng serve -> http://localhost:4200, calls http://localhost:8080/api
npm run build                  # no defaultConfiguration set; pass --configuration=production for an optimized build
npm run lint                   # ESLint (`no-explicit-any` is an error, --max-warnings 0) + Stylelint
npm run lint:fix
npm run format                 # Prettier
npm test                       # Karma/Jasmine, ChromeHeadless, single run
npx ng test --watch=false --browsers=ChromeHeadless --include='src/app/core/services/auth.service.spec.ts'
npx playwright test            # e2e/; needs `npm start` running
```

### Full stack

```bash
cp .env.example .env           # required first: JWT_SECRET (no default in application.yml), PASSWORD_DB, MINIO_ACCESS_KEY,
                               # MINIO_SECRET_KEY, GRAFANA_ADMIN_PASSWORD; generate the JWT secret with `openssl rand -base64 64`
docker-compose up --build                  # db, backend, minio, mailhog, redis, prometheus, grafana (no frontend service)
docker-compose up db minio mailhog redis   # backing services only, then run backend/frontend from source
```

Ports: backend 8080, Postgres 5432, MinIO 9000 (console 9001), MailHog SMTP 1025 (UI 8025), Redis 6379, Prometheus
9090, Grafana 3000. Backend config is `application.yml` with `${ENV:default}` placeholders; `.env.example` documents
every variable and `spring-dotenv` loads a `.env` from the backend working directory. See `CREDENTIAL_ROTATION.md`
before touching any credential.

## Backend architecture

Package root: `com.beezhub.student_accommodation`. Strict layering controller -> service -> repository -> entity; DTOs
in `model/dto`, MapStruct mappers in `mapper/` (regenerated on build), JPA `Specification`s in `specification/` for
list filtering, `PageMapper` in `util/` for paged responses. Every custom exception in `exceptions/` is mapped to an
HTTP status in `RestResponseExceptionHandler`; throw domain exceptions, never generic runtime ones.

**Service decomposition.** Large domains are split into sub-packages with the original class kept as a thin facade:
`service/allocation/` (batch, lifecycle, expiry, move, check-in, reminders, staged, waiting list, scoring orchestrator
and the `ScoringStrategy` interface with `LegacyScoringStrategy` / `OwnerConfigurableScoringStrategy`),
`service/application/`, `service/document/` (`FileStorageService` interface, MinIO implementation, `OrphanFileCleanupJob`, which only
deletes under the `{role}/` document key prefixes and skips objects newer than `app.storage.cleanup.grace-period-minutes`;
any new bucket writer must use a different prefix, as `BuildingImageService` does with `buildings/`),
`service/notification/` (one `*Context` per email type consumed by `EmailContentBuilder`), `service/tenant/`. Put new
allocation or application logic in the sub-service, not the facade.

**Scheduling.** `ScheduledJobRunner` owns every `@Scheduled` annotation (offer/check-in expiry and reminders, batch
allocation, orphan file cleanup, notification retry) and wraps each job with logging and metrics. Do not add
`@Scheduled` elsewhere; add a method there that delegates to the service.

**Feature flags.** Service code calls the `FeatureFlagService` interface; `FF4JFeatureFlagService` is the only class
allowed to touch the FF4j bean. Batch auto-allocation is gated by the `auto-allocation` flag
(`FF4jConfig.FEATURE_AUTO_ALLOCATION`). The stored row is the source of truth: the flag ships disabled
from `V2026_02_25_0012__fix_ff4j_schema.sql` and is toggled through `/api/admin/features`. There is no
environment variable for it, and none should be added; the one that existed overwrote admin changes on
every boot (GH-142). It is the *only* switch for auto-allocation: both the batch path and the
per-application path read it. The separate `auto_allocation_enabled` system setting that used to gate
the per-application path is retired (GH-207). The FF4j REST API jar
(`ff4j-spring-boot-web-api`) is excluded from the build and `/api/ff4j/**` is PLATFORM_ADMIN-only in `SecurityConfig`;
manage flags through `FeatureController` (`/api/admin/features`).

**Multi-tenancy.**
- `JwtAuthenticationFilter` reads `tenantId` from the JWT into the `TenantContext` ThreadLocal and clears it after the
  request. `TenantContext.PLATFORM_TENANT_ID = 0` is the platform tenant, seeded by Flyway
  (`V2026_02_25_0013__platform_tenant_seed.sql`); student accounts are platform-level.
- The Hibernate filter `tenantFilter` is enabled by `TenantFilterAspect` alone; there is no MVC interceptor. It only
  works on the transactional session, so `TenantFilterConfig` pins the transaction advisor to `HIGHEST_PRECEDENCE`,
  making the aspect run inside the transaction. A service read with no `@Transactional` is therefore unfiltered and
  logs `action=tenantFilterNotApplied`. Entities implementing `TenantAware` carry
  `@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")` and `TenantListener` stamps `tenant_id` on
  persist. Tenant FKs use a dual mapping: writable `@Column tenantId` plus read-only `@ManyToOne tenant`.
- Code that runs outside a request (scheduled jobs, async listeners) sees all tenants unless it sets the context
  itself. A new tenant-scoped entity needs the interface, the filter annotation, and an isolation test.
- Hibernate's filter does NOT apply to `findById`. Every admin path that loads a tenant-owned row by id must call
  `TenantOwnershipGuard.assertOwned` (writes) or `assertReadable` (reads, which also allow platform-tenant rows).
  `applyToLoadByKey` is deliberately not enabled: `AppUser` and other tenant-aware tables hold platform rows that
  tenant admins must still be able to read.
- `Document` is deliberately NOT tenant-scoped: a student's document pack is reused across applications to different
  tenants, and stamping the uploader's platform tenant made every upload invisible to tenant admins. Tenant admins are
  scoped explicitly by `DocumentAccessGuard` (student must have an application in the caller's tenant).

**Roles and security.** `UserRole` is `STUDENT`, `PLATFORM_ADMIN`, `TENANT_ADMIN`, `BUILDING_STAFF`, plus deprecated
`ADMIN` kept for migration. `SecurityConfig` declares the hierarchy `PLATFORM_ADMIN > TENANT_ADMIN` and
`PLATFORM_ADMIN > BUILDING_STAFF`; `STUDENT` is outside it, so shared endpoints need
`hasAnyRole('STUDENT', 'PLATFORM_ADMIN')`. Admin endpoints use `@PreAuthorize("hasAnyRole('PLATFORM_ADMIN',
'TENANT_ADMIN')")`, platform-only ones `hasRole('PLATFORM_ADMIN')`; `/api/admin/**` and `/actuator/**` (except health)
are also matched in `SecurityConfig`. Filter order: `RateLimitingFilter` (Redis counter per IP on `/api/auth/**`,
skipped if Redis is down) -> `JwtAuthenticationFilter` (rejects blacklisted `jti` via `TokenBlacklistService`).
Admin tokens expire in 4h, student tokens in 10h; `LoginAttemptService` locks accounts after repeated failures.
`MustChangePasswordFilter` runs after the JWT filter and rejects every request except login, refresh, me, logout and
change-password while the token carries the `mustChangePassword` claim. The seeded demo admin is disabled by
`V2026_09_05_1000__disable_seeded_admin.sql` unless `DEMO_SEED=true`; provision the real first admin with
`ADMIN_BOOTSTRAP_EMAIL` and `ADMIN_BOOTSTRAP_PASSWORD` (`AdminBootstrap`).
Endpoints a STUDENT can reach by id (accept/decline offer, `GET /room-allocation/{id}`, active allocation, eligibility) must
call `AllocationAccessGuard` in the service: students are tenant 0, so the tenant filter does nothing for them and only an
explicit ownership check stops one student acting on another student's allocation.

**System settings (cascade).** `system_setting` has composite PK `(tenant_id, setting_key)`. Platform defaults live at
`tenant_id = 0`; tenants add override rows. `getSetting(key)` checks the current tenant then falls back to platform;
`getAllSettings()` overlays tenant rows on the platform map. `PlatformSettingController` (`/api/platform/system-settings`,
PLATFORM_ADMIN) manages tenant 0; `SystemSettingController` (`/api/admin/system-settings`) is the tenant-scoped view.
`auto_allocation_enabled` was platform-only and is now retired; its key stays in `PLATFORM_ONLY_KEYS`
so no tenant override can be created for a key that controls nothing (GH-207).

**Events and async.** Services publish `model/event/*Event`; `NotificationEventListener` handles them with
`@TransactionalEventListener(AFTER_COMMIT)` + `@Async("notificationExecutor")`, so notifications never fire for
rolled-back transactions and never run inside the caller's transaction.
The waiting-list refill after a decline or cancel is the same pattern: `RoomVacatedEvent` -> `WaitingListRefillListener`
(AFTER_COMMIT). `AllocationBatchService` commits each allocation in the `requiresNewTransactionTemplate` bean
(`TransactionTemplateConfig`) and its entry points must stay non-`@Transactional`, or one failing item silently rolls back
the whole batch while the result reports success.

**Status state machines.** `ApplicationStatus.canTransitionTo()` and `AllocationStatus.canTransitionTo()` define legal
transitions; `AllocationStatusValidator.validate()` throws `InvalidAllocationStatusTransitionException`. Call these
before any status change and test both the valid and invalid path.

**Audit trail.** Annotate methods with `@LogActivity(action, entityType, description)`; `ActivityLogAspect` writes an
`activity_log` row after a successful return.

**Database.** Flyway migrations in `src/main/resources/db/migration/`, named `V{YYYY_MM_DD_HHMM}__{description}.sql`,
baselined at `2026.02.19.2100` (`init_full_schema`), with `out-of-order: false` and `validate-on-migrate: true`.
Hibernate runs `hbm2ddl.auto: validate`, so an entity change without a matching migration fails startup. Never edit an
applied migration. Enums are stored as strings; no soft deletes.

## Backend testing

- Most tests are Mockito unit tests. For anything needing a database, extend a base class rather than configuring
  containers: `AbstractIntegrationTest` (`@SpringBootTest(RANDOM_PORT)`, Testcontainers PostgreSQL 16.2 + MailHog +
  Redis + MinIO, Flyway on, `ddl-auto=validate`, `protected mailServer` for assertions) or `AbstractJpaTest`
  (`@DataJpaTest`, PostgreSQL container only, `create-drop`, Flyway off). `application-test.yml` is dead config:
  nothing sets `spring.profiles.active` and no `@ActiveProfiles("test")` exists, so its H2 datasource is never loaded.
- DDL property path: `application.yml` sets `spring.jpa.properties.hibernate.hbm2ddl.auto`, which overrides the
  `spring.jpa.hibernate.ddl-auto` shortcut. A `@DynamicPropertySource` must override both.
- Entity/schema agreement is enforced only by `AbstractIntegrationTest` running `validate` against the Flyway
  schema (GH-148). An entity change and its migration must land in the same commit, or every subclass fails at
  context load. Never set `hbm2ddl.auto` back to `none` to clear that failure. Note what `validate` does *not*
  check: nullability, defaults, column length, foreign keys, unique constraints and indexes. A migration adding
  `NOT NULL` with no default still passes CI and still fails at runtime.
- Full-stack tests without `@Transactional`: delete in FK-safe order in `@AfterEach` (applications -> academic_years ->
  students -> app_users); set `SecurityContextHolder` with a `UsernamePasswordAuthenticationToken(email, null, List.of())`
  for `authService.getCurrentUser()`; set `TenantContext.setTenantId(...)` so `TenantListener` stamps `tenant_id`; clear
  both afterwards.
- PostgreSQL enforces the `tenant_id` FK (H2 did not). Seed `tenant` rows via native SQL before using a tenant id other
  than 0. `academic_year.name` is `VARCHAR(20)`.
- Test naming: `methodName_scenario_expectedResult`.
- `AbstractIntegrationTest` supplies its own `jwt.secret`, so the suite does not need a `JWT_SECRET` env var or a
  `backend/.env`. If every `@SpringBootTest` class fails with "JWT_SECRET environment variable must be set", a
  subclass is loading a context that bypasses the base class properties.
- CI baseline: do not trust a pinned number here, it goes stale on every merge. Read the test count off the most
  recent green backend job on `develop` and compare your branch against that. Testcontainers cannot
  start on the maintainer's Docker Desktop, so integration tests are verified in CI only, and a local run failing
  ~57 container tests is the expected local baseline.

## Frontend architecture

- `src/main.ts` bootstraps a standalone `AppComponent` with `provideRouter`, `provideHttpClient(withInterceptors(...))`,
  animations, and the native date adapter. No `app.config.ts`, no root `NgModule`.
- `/admin/settings` and `/admin/activity-logs` are NOT platform-only: the backend allows
  `TENANT_ADMIN` on both, and guarding them with `PlatformAdminGuard` made the tenant read-only
  branch in `system-settings.component.ts` unreachable (GH-158).
- Routing is flat in `src/app/app-routing.module.ts` (a plain `routes` array, no `.routes.ts` files), every route lazy
  via `loadComponent`. `/admin` is guarded by `AuthGuard, AdminGuard, MustChangePasswordGuard`; platform-only children
  (`tenants`, `feature-flags`, and others) add `PlatformAdminGuard`. `/buildings` and `/buildings/:id` are public.
- Role checks go through `RoleService` (`isPlatformAdmin`, `isTenantAdmin`, `isBuildingStaff`, `isStudent`,
  `isAdmin`), never string comparisons in components or guards. `PlatformAdminGuard` sends tenant admins back to
  `/admin`, everyone else to `/dashboard` or `/login`.
- Auth: JWT in `localStorage['token']` via `TokenStorageService` (decodes `exp` client-side); the user object in
  `localStorage['currentUser']` feeds `AuthService.currentUser`. Moving to HttpOnly cookies is ADR-007, deferred.
- Interceptor order: `authInterceptor` (skips public `/api/auth/*`) -> `tokenExpiryInterceptor` -> `errorInterceptor`
  (retries only 5xx/network errors, snackbar per status, clears auth and redirects to `/login` on 401, navigates to
  `/unauthorized` on 403 which has no route and falls through to `/home`). Send header `X-Silent-Error` to suppress the
  snackbar when you handle the error yourself.
- `core/services/` has one `providedIn: 'root'` service per backend resource on `environment.apiUrl` (single
  `environment.ts`, no prod replacement). No NgRx; shared state is `BehaviorSubject`s in services.
- Admin sidebar groups are the `navGroups` array in `core/layout/admin-sidebar/admin-sidebar.component.ts`.
- Models are split between `core/models/` and `app/model/` (both contain a `user.model.ts`); check which one a service
  imports. Reusable components live in both `shared/` and `shared/components/`.
- Components are standalone, mostly `OnPush`, importing Material modules individually; `shared/material.module.ts` is
  unused by them. `features/student/room-browser/` is intentionally unrouted.
- Production builds enforce Angular budgets: 1mb warning / 1.5mb error on the initial bundle, and
  8kb warning / 20kb error per component stylesheet. Six component stylesheets are over the 8kb
  warning today; that warning is the debt list, and the error threshold sits above the worst of
  them so an unrelated change cannot fail the build (GH-158).
- Styling: `styles.scss` -> `styles/_variables`, `_mixins`, `custom-theme.scss`, `_utilities`, then legacy
  `global_styles.css`. Snackbar panel classes `snack-*` and `*-snackbar` are both defined and valid. Prefer the tokens
  and mixins over hardcoded values; `frontend/docs/STYLING_AUDIT_REPORT.md` catalogs the existing debt.
- Team convention: invoke the `ui-ux-pro-max` skill before implementing frontend code, when it is installed.

## Conventions enforced in review

- Constructor injection only; `@Transactional` on methods (readOnly for reads), never class-level; `FetchType.LAZY`
  everywhere with `@EntityGraph`/`JOIN FETCH` for N+1 hotspots; paginate every list endpoint.
- Size limits: service < 400 lines, controller < 200, method < 50; Angular component TS < 300 lines.
- Structured `key=value` logging; never log PII at WARN/ERROR.
- DB naming: `snake_case`, booleans `is_*`, FKs `{entity}_id`, indexes `idx_{table}_{cols}`, uniques
  `uq_{table}_{cols}`. Adding NOT NULL is three steps: nullable column -> backfill -> constraint.
- Conventional Commits; feature branches `feature/TASK-{id}-{slug}` from `develop`; `develop` merges to `main` via PR.
  Never commit directly to `main` or `develop`.

## Known inconsistencies

- `frontend/Dockerfile` builds with `npm run build -- --configuration=production` and copies `dist/demo/browser`; production
  swaps in `src/environments/environment.prod.ts` (`apiUrl: '/api'`), so the image is environment-independent and nginx
  proxies `/api/` to the backend. `npm run build` now defaults to the production configuration.
- `frontend/README.md` mentions `npm run prepare`; no such script exists and there is no Husky/lint-staged/commitlint
  config despite the packages being in devDependencies.
- `frontend/.github/instructions/student-accomodation.instructions.md` is an aspirational Copilot spec (microservices,
  NgRx, Cypress). Follow the code, not that file, except for strict TypeScript, ESLint + Prettier, Conventional Commits.
- `dummy.pdf` at the root is a 17-byte placeholder with no references in code.

## Contribution rules

Read `CONTRIBUTING.md` before committing or opening a pull request. The
rules that matter most:

- Do not identify a contribution as agent-generated anywhere in the
  repository or its history: no `Co-Authored-By:` trailers naming a
  model or tool, no "Generated with" footers, no AI markers in commit
  messages, PR text, code comments, or docs.
- Commit under the configured `user.name` and `user.email`. Never
  override them with `--author`, `GIT_AUTHOR_*`, or `GIT_COMMITTER_*`.
- Name branches with the project convention, `feature/TASK-{id}-{slug}`.
  Never put an agent, model, or tool name in a branch.
- Always branch from `develop` and open the pull request back against
  `develop`. One issue per pull request, no bundled or drive-by changes.
- Write Conventional Commits with an imperative subject of 72
  characters or less. The body says what changed and why, not that a
  tool made it.
- Do not use em dashes, and do not use en dashes as sentence
  punctuation.

Before pushing, confirm `git log --format='%an <%ae>%n%b'
origin/develop..HEAD` and `git rev-parse --abbrev-ref HEAD` are clean.
