## Student Accommodation System – Comprehensive Recommendations

Version: 2025-08-21

This document consolidates technical, architectural, operational, and process recommendations based on the current repository structure (Spring Boot backend, Angular 19 frontend, Docker Compose, SonarQube, MinIO, PostgreSQL). It is organized by priority to help guide incremental adoption.

---
## 1. Priority Matrix (Actionable Summary)

| Priority | Theme | Recommendation | Rationale | Effort |
|----------|-------|----------------|-----------|--------|
| Critical | Data & Schema | Remove `spring.jpa.hibernate.hbm2ddl.auto=update`; rely solely on Flyway | Prevent drift & accidental destructive changes | Low |
| Critical | Secrets | Externalize DB / JWT / MinIO credentials from `docker-compose.yml` to `.env` / Vault | Avoid secret leakage & ease rotation | Low |
| Critical | Security | Rotate and shorten-lived JWT secret; implement refresh token & token invalidation strategy | Reduce breach blast radius | Medium |
| Critical | Test Quality | Enforce minimum Jacoco line coverage ≥ 0.70 for service / controller layers (currently 0.00) | Raise baseline code health | Low |
| High | CI/CD | Add pipeline stages: build → test → static analysis (Sonar) → security scan (Snyk/Trivy) → package → deploy | Shift-left quality & security | Medium |
| High | Observability | Add structured logging (JSON), request tracing (OpenTelemetry), metrics (Micrometer + Prometheus) | Production diagnosability | Medium |
| High | AuthZ | Introduce role & permission model (RBAC) with method-level security annotations | Principle of least privilege | Medium |
| High | Backend Config | Introduce Spring Profiles (`local`, `dev`, `staging`, `prod`) & profile-specific YAMLs | Environment parity | Low |
| High | Frontend Quality | Add ESLint, Prettier, strict Angular compiler options, unit + e2e test frameworks (Jest + Cypress) | Prevent regressions | Medium |
| High | API Contract | Generate and publish OpenAPI spec; add contract tests & typed client generation for Angular | Reduce integration fragility | Medium |
| Medium | Storage | Automate MinIO bucket creation & object lifecycle (retention policies) | Reliability & cost | Low |
| Medium | Performance | Introduce a caching layer (e.g., Redis) for heavy read endpoints | Lower DB load | Medium |
| Medium | Domain Modeling | Introduce explicit bounded contexts (User, Listing, Booking, Document) | Future scalability | Medium |
| Medium | Deployment | Provide container hardening (non-root user, distroless base) | Reduce attack surface | Low |
| Low | DX | Add Makefile / task runner & project-wide code generation scripts | Faster onboarding | Low |
| Low | Documentation | Add ADRs, sequence diagrams (PlantUML / C4) & operational runbooks | Knowledge retention | Low |

---
## 2. Backend (Spring Boot)

### 2.1 Configuration & Environment Management
- Split `application.yml` into `application-{profile}.yml`; set active profile via `SPRING_PROFILES_ACTIVE`.
- Remove `hibernate.hbm2ddl.auto=update`; rely on Flyway only. Add a Flyway baseline if production already exists.
- Adopt config layering order: (1) command-line/env vars, (2) profile YAML, (3) secrets from Vault/Parameter Store.
- Introduce central `ConfigProperties` classes annotated with `@ConfigurationProperties` for JWT and MinIO to avoid ad hoc access.

### 2.2 Dependency Hygiene
- Upgrade JJWT to latest minor if security advisories appear; consider migrating to Spring Security’s native JWT support (Nimbus JOSE JWT).
- Pin versions in a `libs.versions.toml` (Gradle Version Catalog) for clarity & consistent upgrades.
- Add security scanning (OWASP Dependency-Check or Snyk) as a CI step.

### 2.3 Domain Layering
- Standard package proposal: `domain` (entities, aggregates), `repository`, `service`, `web` (controllers/DTOs), `config`, `security`, `integration` (MinIO, external APIs), `support` (utilities), `mapping` (MapStruct mappers).
- Introduce DTOs for requests/responses; never expose JPA entities directly (if currently done, refactor gradually).
- Consider Value Objects for fields like email, phone, currency, to centralize validation.

### 2.4 Persistence Strategy
- Create Flyway migration for each schema change; reject PRs with entity diffs lacking migrations.
- Add DB naming conventions (snake_case) for tables & columns; ensure Flyway scripts enforce them.
- Add indexes for high-frequency query fields (e.g., `booking.student_id`, `listing.city`).
- Plan for soft deletes (boolean flag or `deleted_at`) instead of physical deletion for audit trails.

### 2.5 Transaction & Concurrency
- Mark service-layer methods with `@Transactional` where aggregate consistency required.
- Use optimistic locking (add `@Version` field) on core entities to prevent lost updates.

### 2.6 File/Object Storage (MinIO)
- Wrap MinIO interactions in a gateway/service with idempotent upload + checksum verification.
- Enforce content-type validation & size limits; reject executable content.
- Add object naming convention: `bucket/{entity}/{entityId}/{uuid}.{ext}`.
- Implement retention or lifecycle policy for stale temporary uploads.

### 2.7 Security
- JWT Hardening: shorter expiration (e.g., 15m access, 7d refresh) + refresh token rotation & blacklist (Redis or DB).
- Add `Content-Security-Policy`, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy` via Spring Security headers config.
- Validate all inputs with `@Validated`; custom validators for domain-specific invariants.
- Add rate limiting (e.g., Bucket4j filter) on auth & booking endpoints.
- Security Testing: Include dependency vulnerability scan + ZAP baseline scan in CI.

### 2.8 Observability
- Introduce OpenTelemetry (OTel Java Agent) + Micrometer: export traces to Jaeger / Tempo, metrics to Prometheus.
- Structured JSON logging (Logback encoder) with correlation IDs (`traceId`, `spanId`, `userId`).
- Define SLOs (e.g., 99% latency < 300ms for booking search) and alert thresholds.
- Add liveness (`/actuator/health/liveness`) & readiness (`/actuator/health/readiness`) probes for container orchestration.

### 2.9 Error Handling & Resilience
- Centralized `@ControllerAdvice` mapping domain exceptions → problem+json responses.
- Add retries (Spring Retry) for transient MinIO / DB connection issues where safe (idempotent operations only).
- Circuit Breakers (Resilience4j) for external calls once they appear (payments, notifications, etc.).

### 2.10 Performance & Scaling
- Introduce second-level cache (carefully) or targeted manual caching for read-heavy endpoints.
- Use pagination consistently; guard maximum page size.
- Conduct load test baseline (e.g., k6 scripts) and record throughput & p95 latency.

---
## 3. Frontend (Angular 19)

### 3.1 Project Structure & Modularity
- Introduce feature modules (or standalone grouped by domain): `auth`, `listings`, `booking`, `documents`, `admin`.
- Create `core` (singleton services, interceptors), `shared` (reusable components/pipes), `models` (TypeScript interfaces generated from OpenAPI), `state` (if using NgRx/Signals).

### 3.2 Type Safety & API Layer
- Generate typed API clients from OpenAPI using `openapi-typescript-codegen` or `ng-openapi-gen`.
- Centralize HTTP concerns (base URL, auth header, error mapping, retry) in an interceptor.

### 3.3 State Management
- Start simple with Angular Signals / component store; adopt NgRx only for complex cross-cutting state (auth + bookings + listings filters).

### 3.4 Quality Tooling
- Add ESLint with Angular recommended config; integrate Prettier.
- Add Jest for fast unit tests (replace or complement default test runner) and Cypress for e2e booking flows.
- Add `npm scripts`: `lint`, `test`, `test:watch`, `e2e`, `format`, `type-check`.
- Enable strict compiler flags (`"strict": true`) in `tsconfig.app.json`.

### 3.5 UI/UX & Accessibility
- Establish design system (theme tokens, spacing scale) & components documented in Storybook.
- Run automated accessibility scans (axe-core) in CI for key pages.
- Provide responsive & offline-friendly (PWA) capability if mobile usage is expected.

### 3.6 Performance
- Lazy-load feature modules / routes.
- Preload critical data with route resolvers where beneficial.
- Add image optimization & skeleton loading states for bookings/listings.

### 3.7 Security (Frontend)
- Sanitize dynamic HTML if ever used; avoid bypassing Angular’s DomSanitizer.
- Store tokens in `httpOnly` cookies (preferred) or, if localStorage, mitigate with strict CSP & same-site policies.

---
## 4. API Design & Contract Testing
- Version APIs under `/api/v1/...` to enable backward-compatible evolution.
- Use consistent response envelopes (e.g., data + meta + errors) or RFC7807 problem details for errors.
- Add consumer-driven contract tests (e.g., Pact) for frontend-backend integration to prevent breaking changes.

---
## 5. Testing Strategy (Holistic)
| Layer | Tooling | Focus |
|-------|---------|-------|
| Unit (Backend) | JUnit + Mockito + AssertJ | Services, mappers, validation |
| Unit (Frontend) | Jest | Components, pipes, services |
| Integration (Backend) | Spring Boot Test + Testcontainers (Postgres, MinIO) | Persistence, security, API endpoints |
| Contract | Spring Cloud Contract / Pact | API schemas compatibility |
| E2E | Cypress | Critical user journeys (register, search, book, upload doc) |
| Load | k6 / Gatling | Booking search throughput |
| Security | OWASP ZAP baseline | Detect common vulnerabilities |
| Accessibility | axe + Storybook a11y addon | WCAG compliance |

Add coverage thresholds: backend (lines ≥ 80%, branches ≥ 65%), frontend (statements ≥ 80%). Fail builds if below.

---
## 6. DevOps, CI/CD & Infrastructure
- Create pipeline (GitHub Actions / GitLab CI):
	1. Checkout & cache dependencies
	2. Backend build & unit tests (Jacoco report upload)
	3. Frontend build & unit tests
	4. Static analysis (Sonar) with `sonar.qualitygate.wait=true`
	5. Security scan (Trivy for images, Snyk or Dependency-Check for libs)
	6. Build Docker images (tag: commit SHA + semver) using multi-stage builds
	7. Publish images to registry
	8. Deploy to staging (Helm / K8s manifests)
	9. Run E2E & smoke tests against staging
	10. Manual approval → production deploy

- Convert docker-compose dev services to K8s (if scaling): Postgres (managed cloud), MinIO (or S3), Application Deployment (HorizontalPodAutoscaler).
- Add Infrastructure as Code (Terraform) for cloud resources (DB, object storage, networking, secrets manager).

### 6.1 Container Hardening
- Use `eclipse-temurin:21-jre-alpine` or distroless base, create non-root user, set `RUN addgroup -S app && adduser -S app -G app`.
- Drop build tool from runtime image (multi-stage: builder → slim runtime).
- Add healthcheck in Dockerfile or rely on orchestrator probes.

### 6.2 Secrets Management
- Use `.env` (dev only) excluded from VCS; production secrets in Vault / AWS Secrets Manager / GCP Secret Manager.
- Rotate secrets every 90 days; automate rotation for JWT signing key if implementing key id (kid) header.

---
## 7. Code Quality & Static Analysis
- Enforce style via Spotless (Gradle plugin) to auto-format Java.
- Add ErrorProne or NullAway for early detection of potential bugs.
- Add MapStruct mapper tests to ensure non-null and field coverage.
- Track code smells & maintainability metrics in Sonar; fail on new debt > threshold.

---
## 8. Performance & Scalability Enhancements
- Introduce asynchronous processing (Spring @Async or messaging) for heavy tasks (document processing, notifications).
- Add message broker (e.g., RabbitMQ / Kafka) when event-driven interactions (booking confirmed → notification) appear.
- Implement Rate Limiting & Quotas for public APIs to deter abuse.
- Introduce caching headers (ETag / Cache-Control) for static assets & listing queries.

---
## 9. Security & Compliance
- Add audit logging (who changed booking/listing) with immutable append-only store.
- Consider data encryption at rest (DB-managed) & field-level encryption for sensitive PII.
- Implement Data Retention policy and anonymization routine for aged records.
- Periodic security review: OWASP ASVS mapping and gap analysis.

---
## 10. Documentation & Knowledge Management
- Add Architecture Decision Records (ADRs) (`docs/architecture/decisions/NNN-title.md`).
- Provide C4 model diagrams (Context, Container, Component, Code) + sequence diagrams for booking flow.
- Create Runbooks: startup, on-call triage, disaster recovery.
- Update README sections: prerequisites, make targets, troubleshooting, environment profiles.

---
## 11. Developer Experience (DX)
- Introduce `Makefile` or `justfile` targets: `make backend-test`, `make frontend-test`, `make up`, `make sonar`.
- Pre-commit hooks (Husky for frontend, pre-commit for backend) running lint, tests (fast subset), format.
- Git Conventional Commits + automatic changelog generation (semantic-release).
- Onboarding script to bootstrap env (checking Java, Node, Docker versions).

---
## 12. AI & Automation Opportunities
- Use AI-assisted generation only with guardrails: tests first, manual review, ADR updates for architectural changes.
- Maintain curated prompt templates (already partially present) with version tags & success metrics.
- Add a `CONTRIBUTING_AI.md` describing allowed AI usage and review checklist.

---
## 13. Suggested 30 / 60 / 90 Day Roadmap
### Days 0–30 (Foundation)
- Secrets externalization, remove `hbm2ddl.auto`, set up profiles.
- Add linting & formatting (backend + frontend) + enforce in CI.
- Introduce coverage thresholds; raise gradually.
- Add OpenAPI generation + typed clients.

### Days 31–60 (Quality & Observability)
- Implement structured logging & tracing.
- Add contract tests + Cypress E2E for core flows.
- Add Redis (caching) & rate limiting.
- Introduce ADRs + initial C4 diagrams.

### Days 61–90 (Scalability & Resilience)
- Container hardening & multi-stage builds.
- Deploy to staging K8s cluster + HPA.
- Add async processing / message broker for notifications.
- Implement RBAC refinements & audit logs; load testing & performance tuning.

---
## 14. Tool & Library Suggestions (Curated)
| Category | Tool | Purpose |
|----------|------|---------|
| Formatting | Spotless / Prettier | Consistent code style |
| Static Analysis | SonarQube, ErrorProne, ESLint | Code quality & bug detection |
| Security | OWASP Dependency-Check, Trivy, ZAP | Vulnerability scanning |
| Testing | JUnit, Testcontainers, Jest, Cypress, Pact | Layered test coverage |
| Observability | OpenTelemetry, Micrometer, Prometheus, Jaeger | Metrics & tracing |
| Docs | SpringDoc OpenAPI, Storybook, PlantUML | API & UI documentation |
| Build | Gradle Version Catalog, semantic-release | Dependency mgmt & releases |
| Infra | Terraform, Helm | Declarative infra & deployment |

---
## 15. Configuration Changes (Concrete Examples)
1. Remove from `application.yml`:
```
spring:
	jpa:
		properties:
			hibernate:
				hbm2ddl:
					auto: update
```
2. Add profile files: `application-local.yml`, `application-dev.yml`, etc.
3. Add `.env` (NOT committed) for local docker-compose:
```
POSTGRES_USER=localadmin
POSTGRES_PASSWORD=changeMeLocal
JWT_SECRET=generateNewSecret
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
```
4. Reference env file in `docker-compose.yml` services with `env_file:`.

---
## 16. Risk Register (Top Items)
| Risk | Impact | Mitigation |
|------|--------|------------|
| Schema drift due to Hibernate auto-update | Data corruption / surprise prod changes | Disable auto-update; Flyway only |
| Secret exposure in repo (compose) | Credential leakage | Externalize & rotate secrets |
| Weak JWT lifecycle | Unauthorized long-term access | Short-lived access + refresh rotation |
| Low coverage threshold (0.00) | Undetected regressions | Enforce staged coverage targets |
| Lack of tracing | Slow incident resolution | Implement OTel tracing |

---
## 17. Success Metrics (KPIs)
- Build pipeline success rate ≥ 95%.
- Mean PR lead time < 24h.
- Test coverage (line) ≥ 80% backend / 80% frontend by Day 90.
- p95 Booking Search latency < 300ms under target load.
- < 2 high severity vulnerabilities open > 14 days.

---
## 18. Continuous Review Cadence
- Monthly architecture review (update ADRs & diagrams).
- Quarterly security review (dependency & penetration test summary).
- Bi-weekly performance snapshot (load test delta vs baseline).

---
## 19. Adoption Guidance
Adopt changes incrementally: start with non-invasive (linting, configurations), then structural (modularization, observability), then advanced (event-driven, caching, K8s). Each PR should include: rationale, migration notes, rollback plan, and updated docs/tests.

---
## 20. Closing Notes
This recommendations set is intentionally comprehensive to serve as a living improvement backlog. Prioritize by: (1) security & data integrity, (2) reliability & observability, (3) developer productivity, (4) scalability & optimization. Re-evaluate quarterly.

---
Maintainer: (Add owner)  |  Next Review: 2025-11-01

