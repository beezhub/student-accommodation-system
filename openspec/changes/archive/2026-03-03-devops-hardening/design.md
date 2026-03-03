## Context

The student accommodation system is a Spring Boot + Angular application with Docker Compose infrastructure, a Gradle build, and an Nginx frontend server. A thorough DevOps audit has identified 40+ issues across four severity levels:

- **Critical (4):** Committed JWT secrets with active default fallback; real Google Maps API keys on disk; live credential file in repository directory
- **High (9):** `hbm2ddl.auto: update` in production; unsafe Flyway settings; no data volume for PostgreSQL; no CI/CD pipeline; hardcoded MinIO/Grafana/DB credentials; SMTP auth/TLS ignored; Swagger UI unauthenticated
- **Medium (20):** Nginx missing security headers, HTTPS, and non-wildcard CORS; no healthchecks or restart policies; unpinned images; no resource limits; no network isolation; broken Prometheus scraping; deprecated Gradle APIs; Docker build cache disabled
- **Low (10):** Ports exposed on all host interfaces; H2 used for integration tests; `mavenLocal()` ordering; placeholder Grafana alert email; incomplete `.env.example`

The system has no CI/CD pipeline at all. Several fixes were developed on a `develop` branch worktree but were never merged to `main`/HEAD.

## Goals / Non-Goals

**Goals:**
- Eliminate all critical security vulnerabilities (committed/default secrets)
- Make the system safe and operable in a production-like environment
- Establish a CI/CD pipeline as the minimum viable automation baseline
- Fix broken infrastructure (missing volume, broken Prometheus, SMTP config)
- Apply Docker Compose best practices (healthchecks, restart policies, pinned images)
- Harden the Nginx configuration (security headers, CORS, timeouts)
- Fix build tooling issues (deprecated APIs, non-reproducible builds)

**Non-Goals:**
- Full Kubernetes/Helm migration
- ELK/Loki log aggregation (beyond scope of this change)
- Load testing (separate concern)
- Full HTTPS termination with certificate management in docker-compose (scaffolding only)
- Migrating JWT storage to HttpOnly cookies (separate security change)
- Upgrading FF4J (separate dependency upgrade)

## Decisions

### D-01: JWT Secret — Fail-Fast With No Default Fallback

**Decision:** Change `jwt.secret: ${JWT_SECRET:jCiWx+...}` to `jwt.secret: ${JWT_SECRET}` (no default). Add a `@PostConstruct` validation in the JWT config bean to throw an explicit startup error if the secret is empty or too short (< 64 chars).

**Alternatives considered:**
- Keep the default but rotate to a new unknown value → Still a hardcoded secret in source; any attacker with source access can forge tokens
- Use Spring Boot's `spring.application.fail-fast=true` → Does not validate semantic constraints like key length

**Rationale:** The application must refuse to start with an unconfigured secret. Silent fallback to a known-public value is the most dangerous pattern possible for a JWT-based auth system.

---

### D-02: Flyway + Hibernate Settings — Validate-Only in Production

**Decision:** Set `spring.jpa.hibernate.ddl-auto: validate` and `spring.flyway.validate-on-migrate: true`, `out-of-order: false`, remove `ignore-missing-migrations: true`. Use a Spring profile (`prod`) to enforce these; the `default`/`dev` profile can retain `update` for local development convenience.

**Alternatives considered:**
- Set `ddl-auto: none` → More restrictive but requires explicit schema pre-creation; `validate` is appropriate when Flyway manages schema
- Keep `out-of-order: true` for dev convenience → Dev and prod should use the same migration safety settings

**Rationale:** Hibernate and Flyway co-exist safely only when Hibernate is in read-only schema mode and Flyway applies all migrations in deterministic order. The `develop` branch already has these corrections.

---

### D-03: CI/CD — GitHub Actions (Not GitLab CI or Jenkins)

**Decision:** Use GitHub Actions. Create workflows:
1. `ci.yml` — on every PR and push: build backend JAR, run unit tests, run frontend lint, run `npm test`, scan for secrets with `gitleaks`
2. `docker-build.yml` — on push to `main`: build and push Docker images to GitHub Container Registry (GHCR) with semantic version tags

**Alternatives considered:**
- GitLab CI → Repository is on GitHub (confirmed by `CREDENTIAL_ROTATION.md` reference to "GitHub Secrets")
- Jenkins → Requires external server; GitHub Actions is free and zero-infrastructure

**Rationale:** GitHub Actions is already the natural fit given the repository platform. The `CREDENTIAL_ROTATION.md` explicitly references GitHub Secrets as the intended credential store.

---

### D-04: Credentials — Environment Variables via `.env.local` (Not Committing Secrets)

**Decision:**
- Remove the commented-out JWT secret line from `docker-compose.yml`
- Update all `docker-compose.yml` env vars to use `${VAR_NAME}` references with no inline defaults for security-sensitive values
- Provide a complete root `.env.example` with placeholder values and instructions
- Add `backend/.env` to `.gitignore` explicitly (not just via wildcard `*.env`)
- Document that developers must copy `.env.example` → `.env.local` and supply real values

**Alternatives considered:**
- Docker Secrets (Swarm mode) → Only works with Docker Swarm, not Compose for local dev
- HashiCorp Vault integration → Over-engineered for this stage of the project

**Rationale:** Environment variable injection via an untracked `.env.local` file is the standard twelve-factor app pattern for local development. It is simple, well-understood, and prevents accidental commits.

---

### D-05: Docker Compose Healthchecks — Use Native Commands

**Decision:** Add `healthcheck` blocks to `db` (PostgreSQL), `minio`, `redis`, and the `backend`. Change all `depends_on` conditions to `service_healthy`.

```yaml
# PostgreSQL
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U ${USER_DB:-localadmin}"]
  interval: 10s
  timeout: 5s
  retries: 5

# MinIO
healthcheck:
  test: ["CMD-SHELL", "mc ready local || exit 1"]
  interval: 10s
  timeout: 5s
  retries: 5

# Redis
healthcheck:
  test: ["CMD", "redis-cli", "ping"]
  interval: 10s
  timeout: 5s
  retries: 3
```

**Rationale:** `service_started` only guarantees the process launched, not that it accepted connections. `service_healthy` + proper healthchecks eliminates intermittent startup failures.

---

### D-06: Docker Network Segmentation

**Decision:** Define two Compose networks:
- `app-network`: backend, frontend, db, redis, minio
- `monitoring-network`: backend (for scraping), prometheus, grafana

Services only join the networks they need. PostgreSQL and Redis are NOT on the monitoring network.

**Rationale:** Minimizes blast radius if any container is compromised. Grafana and Prometheus should only reach the backend metrics endpoint, not the database directly.

---

### D-07: Nginx Security Hardening — Headers + CORS + Timeouts

**Decision:** Add to `nginx.conf`:
```nginx
# Security headers
add_header X-Frame-Options "SAMEORIGIN" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; connect-src 'self'" always;

# CORS — restrict to known origin
add_header Access-Control-Allow-Origin "http://localhost:4200" always;

# Proxy timeouts
proxy_connect_timeout 60s;
proxy_read_timeout 120s;
proxy_send_timeout 60s;
```

Replace `Access-Control-Allow-Origin: *` with the explicit origin. CSP removes `unsafe-inline` from `script-src` (addresses Phase 3 open issue #14).

**Rationale:** Security headers are a defense-in-depth measure. CORS wildcard with `Authorization` header allowed is a direct vulnerability. Proxy timeouts prevent connection exhaustion on backend hangs.

---

### D-08: Integration Tests — Testcontainers PostgreSQL (Not H2)

**Decision:** Update `application-test.yml` to remove H2 datasource override. Configure `@Testcontainers` + `@Container` PostgreSQL in integration test base class. Keep H2 only for pure unit tests that do not hit the database.

**Alternatives considered:**
- Embedded PostgreSQL (e.g., `embedded-postgres`) → Testcontainers is already a declared dependency and more standard
- Keep H2 → Flyway migrations use PostgreSQL-specific syntax; H2 PostgreSQL mode does not support all of it

**Rationale:** Real PostgreSQL via Testcontainers ensures Flyway migration compatibility and JPA query accuracy. The dependency is already declared in `build.gradle`.

## Risks / Trade-offs

| Risk | Mitigation |
|------|-----------|
| Removing JWT secret default causes startup failures in existing deployments that don't set `JWT_SECRET` | Document the requirement explicitly in `.env.example`; add clear startup error message. Breaking change is intentional and necessary. |
| SMTP hardcoded `auth: false` fix requires production operators to set `MAIL_SMTP_AUTH=true` | Include in `.env.example` with clear comment; document in `CREDENTIAL_ROTATION.md` |
| Testcontainers requires Docker to be running for tests | Acceptable for CI/CD (Docker is available); add note to `CLAUDE.md` for local dev |
| Nginx CSP `unsafe-inline` removal may break Angular styles | Angular Material uses inline styles in some cases; test with application before finalizing CSP. May need `'unsafe-inline'` in `style-src` initially. |
| Pinning Docker image versions requires periodic updates | Acceptable trade-off; unpinned `:latest` images are more dangerous. Use Renovate or Dependabot to automate updates. |
| `no_cache: true` removal will use Docker layer cache — stale layers on dependency changes | Normal Docker behavior; developers must run `docker-compose build --no-cache` when dependencies change |

## Migration Plan

**Phase 1 — Critical Fixes (unblock production)**
1. Remove JWT secret default fallback from `application.yml`
2. Remove/comment-out secrets from `docker-compose.yml`; use `${VAR}` references
3. Update `.env.example` with all required variables
4. Fix PostgreSQL data volume mount
5. Fix `hbm2ddl.auto: validate` and Flyway settings

**Phase 2 — Infrastructure Hardening**
6. Add healthchecks to all services
7. Change `depends_on` to `service_healthy`
8. Add `restart: unless-stopped` to all services
9. Pin all Docker image tags
10. Add Docker networks and resource limits
11. Fix `no_cache: true` (remove it)

**Phase 3 — Observability + Security**
12. Add `prometheus` to actuator exposure
13. Harden Nginx (security headers, CORS, timeouts)
14. Fix SMTP env var references
15. Add Swagger UI authentication gate for production profile

**Phase 4 — Build + Test Quality**
16. Fix deprecated `buildDir` Gradle API
17. Remove `mavenLocal()` from repositories
18. Add Testcontainers integration test setup

**Phase 5 — CI/CD**
19. Create `ci.yml` GitHub Actions workflow
20. Create `docker-build.yml` for image publishing

**Rollback:** All changes are configuration/infrastructure only (no schema migrations). Rolling back means reverting the specific files changed. The critical phase-1 changes (JWT secret removal) should NOT be rolled back — if the application fails to start, the fix is to provide the `JWT_SECRET` environment variable, not to restore the insecure default.

## Open Questions

- **Q1:** Should Swagger UI be disabled entirely in production (via Spring profile) or protected by Basic Auth? The current design gates it via a Spring Security rule, but disabling via profile is simpler. → Recommend: disable via `springdoc.api-docs.enabled=false` in production profile.
- **Q2:** Should the CI pipeline block on Jacoco coverage thresholds? Currently `jacocoTestCoverageVerification` is configured in `build.gradle`. → Recommend: yes, but only after the Testcontainers migration stabilizes the test suite.
- **Q3:** What is the production domain/CORS origin for the Nginx `Access-Control-Allow-Origin` header? Currently using `http://localhost:4200` as placeholder. → Needs to be parameterized via Nginx env var or templated config.
