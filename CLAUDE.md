# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

University student-accommodation system: a Spring Boot 3.5 / Java 17 monolith in `backend/` and an Angular 21 +
Material standalone-component app in `frontend/`. PostgreSQL 16 (Flyway), Redis (token blacklist, rate limiting),
MinIO (documents), SMTP via MailHog locally, FF4j feature flags, Google Maps geocoding.

Planning docs at the root are authoritative for intent: `Requirements.md` (BRD), `Architecture.md` (stabilisation plan
with coding standards in §5 and ADRs in §8), `docs/task-list.md` (phased task backlog). `Architecture.md` §4 and its
`ROLES` example describe a *target* v2.0 role model; the code today has `UserRole { STUDENT, ADMIN, LANDLORD }`.

## Commands

### Backend (`cd backend`)

```bash
./gradlew bootRun                                   # :8080, Swagger at /swagger-ui.html, health at /actuator/health
./gradlew clean build                               # compiles, runs tests, then jacoco report + coverage verification
./gradlew test                                      # JUnit 5; jacoco HTML report lands in build/jacocoHtml
./gradlew test --tests "com.beezhub.student_accommodation.service.allocation.AllocationBatchServiceTest"
./gradlew test --tests "*AllocationBatchServiceTest.methodName*"
./gradlew jacocoTestCoverageVerification            # AllocationScoringService must stay >= 90% line coverage
./gradlew flywayInfo
```

Tests use the `test` profile (`src/test/resources/application-test.yml`): in-memory H2 in PostgreSQL mode with
`ddl-auto: create-drop` and Flyway disabled, so migrations are never exercised by the test suite. Most tests are plain
Mockito unit tests; only a few are `@SpringBootTest`. Test method naming is `methodName_scenario_expectedResult`.

### Frontend (`cd frontend`)

```bash
npm install
npm start                      # ng serve -> http://localhost:4200, calls http://localhost:8080/api
npm run build                  # no defaultConfiguration set; pass --configuration=production for an optimized build
npm run lint                   # ESLint (--max-warnings 0, so `any` warnings FAIL) + Stylelint
npm run lint:fix
npm run format                 # Prettier
npm test                       # Karma/Jasmine, ChromeHeadless, single run
npx ng test --watch=false --browsers=ChromeHeadless --include='src/app/core/services/token-storage.service.spec.ts'
npx playwright test            # e2e/; needs `npm start` running
```

Frontend unit coverage is near zero (one spec file) and the Playwright spec is a placeholder.

### Full stack

```bash
docker-compose up --build                  # db, minio, mailhog, redis, backend (no frontend service is defined)
docker-compose up db minio mailhog redis   # backing services only, then run backend/frontend from source
```

Ports: backend 8080, Postgres 5432, MinIO 9000 (console 9001), MailHog SMTP 1025 (UI 8025), Redis 6379. Backend config
is `application.yml` with `${ENV:default}` placeholders; `.env.example` at the root lists every variable and
`spring-dotenv` loads a `.env` from the backend working directory.

## Backend architecture

Package root: `com.beezhub.student_accommodation`. Strict layering controller -> service -> repository -> entity; DTOs
in `model/dto`, MapStruct mappers in `mapper/` (regenerated on build), JPA `Specification`s in `specification/` for
list filtering, `PageMapper` in `util/` for paged responses. Every custom exception in `exceptions/` is mapped to an
HTTP status in `RestResponseExceptionHandler`; throw domain exceptions, never generic runtime ones.

**Service decomposition.** Large domains are split into sub-packages with the original class kept as a thin facade
(Strangler pattern, mandated by `Architecture.md`): `service/allocation/` (batch, lifecycle, expiry, move, check-in,
staged, waiting list, scoring orchestrator), `service/application/`, `service/document/` (`FileStorageService`
interface with the MinIO implementation and `OrphanFileCleanupJob`), `service/notification/`. When adding allocation or
application logic, put it in the sub-service, not the facade.

**Multi-tenancy.** `JwtAuthenticationFilter` extracts `tenantId` from the JWT into the `TenantContext` ThreadLocal and
clears it after the request. `TenantFilterConfig` (a `HandlerInterceptor`) enables the Hibernate filter `tenantFilter`
on the session when a tenant is set; entities implementing `TenantAware` carry `@Filter(name = "tenantFilter")` and
`TenantListener` stamps `tenant_id` on persist. Consequences: queries outside a request (scheduled jobs, async
listeners) see all tenants unless they set the context themselves, and a new tenant-scoped entity needs the interface,
the filter annotation, and an isolation test (see `TenantFilterIntegrationTest`). Student accounts are platform-level
(ADR-001).

**Security.** Stateless JWT (`jjwt`), `@EnableMethodSecurity` with `@PreAuthorize("hasRole('ADMIN')")` on admin
endpoints plus a `/api/admin/**` matcher in `SecurityConfig`. Filter order: `RateLimitingFilter` (Redis counter per IP,
only on `/api/auth/**`, skipped if Redis is down) -> `JwtAuthenticationFilter` (rejects blacklisted `jti` via
`TokenBlacklistService` in Redis). Admin tokens expire in 4h, student tokens in 10h. `LoginAttemptService` locks
accounts after repeated failures.

**Events and async.** Services publish `model/event/*Event` objects; `NotificationEventListener` handles them with
`@TransactionalEventListener(AFTER_COMMIT)` + `@Async("notificationExecutor")`, so notifications never fire for
rolled-back transactions and never run inside the caller's transaction. Scheduled jobs live in
`AllocationBatchService`, `AllocationExpiryService`, `NotificationRetryService`, and `OrphanFileCleanupJob`; batch
auto-allocation is gated by the FF4j flag `auto-allocation` (`FF4jConfig.FEATURE_AUTO_ALLOCATION`, seeded from
`AUTO_ALLOCATION_ENABLED`).

**Status state machines.** `ApplicationStatus.canTransitionTo()` and `AllocationStatus.canTransitionTo()` define legal
transitions; `AllocationStatusValidator.validate()` throws `InvalidAllocationStatusTransitionException`. Call these
before any status change and add a unit test for both the valid and invalid path.

**Audit trail.** Annotate controller/service methods with `@LogActivity(action, entityType, description)`;
`ActivityLogAspect` writes an `activity_log` row (partitioned table, see migration `V2026_03_03_1000`) after a
successful return.

**Database.** Flyway migrations in `src/main/resources/db/migration/`, named `V{YYYY_MM_DD_HHMM}__{description}.sql`
with `out-of-order: true` and `validate-on-migrate: false`. Note that `hibernate.hbm2ddl.auto: update` is also on, so
schema drift can appear without a migration; always add the migration anyway and never edit an applied one. Enums are
stored as strings; there are no soft deletes.

## Frontend architecture

- `src/main.ts` bootstraps a standalone `AppComponent` with `provideRouter`, `provideHttpClient(withInterceptors(...))`,
  animations, and the native date adapter. No `app.config.ts`, no root `NgModule`.
- `src/app/app-routing.module.ts` exports a plain `routes` array (the name is legacy). Every route is lazy via
  `loadComponent`; the `/admin` parent route guards all admin children.
- `angular.json` project name is `demo`, output `dist/demo`. Full strict mode including `strictTemplates`,
  `strictStandalone`, and `noPropertyAccessFromIndexSignature`.
- `core/services/` holds one `providedIn: 'root'` service per backend resource, all built on `environment.apiUrl`.
  There is a single `environment.ts` (no prod replacement). No NgRx; shared state is `BehaviorSubject`s in services.
- Models are split between `core/models/` and `app/model/` (both contain a `user.model.ts`); check which one a service
  imports before adding fields. Likewise `shared/` and `features/shared/components/` are both reusable-component homes.
- Auth: JWT in `localStorage['token']` via `TokenStorageService` (decodes `exp` client-side); the user object in
  `localStorage['currentUser']` feeds `AuthService.currentUser`. Class-based guards: `AuthGuard` (token unexpired),
  `StudentGuard`/`AdminGuard` (`userRole`), `EmailVerifiedGuard` (reads localStorage directly).
- Interceptor order: `authInterceptor` (skips public `/api/auth/*`) -> `tokenExpiryInterceptor` -> `errorInterceptor`
  (retries GETs once, snackbar per status, clears auth and redirects to `/login` on 401, navigates to `/unauthorized`
  on 403 which has no route and falls to `/home`). Send header `X-Silent-Error` to suppress the snackbar.
- Components are standalone, mostly `OnPush`, and import Material modules individually; `shared/material.module.ts`
  exists but is unused by them.
- `features/student/room-browser/` is intentionally unrouted (deferred from MVP).
- Styling: `styles.scss` -> `styles/_variables`, `_mixins`, `custom-theme.scss`, `_utilities`, then legacy
  `global_styles.css`. `styles/_tokens.scss` is the intended token source but most component SCSS hardcodes values;
  `frontend/docs/STYLING_AUDIT_REPORT.md` catalogs the debt and per-module color themes. Prefer tokens/mixins.

## Conventions from Architecture.md §5 (enforced in review)

- Constructor injection only; `@Transactional` on methods (readOnly for reads), never class-level; `FetchType.LAZY`
  everywhere with `@EntityGraph`/`JOIN FETCH` for N+1 hotspots; paginate every list endpoint.
- Size limits: service < 400 lines, controller < 200, method < 50; Angular component TS < 300 lines.
- Structured `key=value` logging; never log PII at WARN/ERROR.
- DB naming: `snake_case`, booleans `is_*`, FKs `{entity}_id`, indexes `idx_{table}_{cols}`, uniques
  `uq_{table}_{cols}`. Adding NOT NULL is three steps: nullable column -> backfill -> constraint.
- Conventional Commits; feature branches `feature/TASK-{id}-{slug}` from `develop`; `develop` merges to `main` via PR.

## Known inconsistencies

- `frontend/Dockerfile` copies `dist/frontend/browser`, but the build outputs `dist/demo`; the image build fails as is.
- `frontend/README.md` mentions `npm run prepare`; no such script exists and there is no Husky/lint-staged/commitlint
  config despite the packages being in devDependencies.
- `frontend/.github/instructions/student-accomodation.instructions.md` is an aspirational Copilot spec (microservices,
  NgRx, Cypress). Follow the code, not that file, except for strict TypeScript, ESLint + Prettier, Conventional Commits.
- `TASK-2.04-IMPLEMENTATION-SUMMARY.md` at the root and in `docs/` are two different documents with the same name
  (application-service split vs. document-service review fixes).

## Tooling in the repo

- OpenSpec workflow (`/opsx:*` commands in `.claude/commands/opsx/`) operates on `openspec/`; `openspec/config.yaml`
  carries role-specific rules (BA, solution design, implementation plan, tech review) that mirror the conventions above.
- `tools/*.md` are role system prompts (architect, BA, developer, task planner, tech-lead reviewer) for AI-assisted
  planning, not application code.
