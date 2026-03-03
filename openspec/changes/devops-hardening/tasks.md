## 1. Critical Security Fixes

- [x] 1.1 Remove default JWT secret fallback from `backend/src/main/resources/application.yml` — change `${JWT_SECRET:jCiWx+...}` to `${JWT_SECRET}`
- [x] 1.2 Add `@PostConstruct` validation in JWT config bean to throw a clear startup error if `JWT_SECRET` is empty or shorter than 64 characters
- [x] 1.3 Remove the commented-out `JWT_SECRET=jvc+QC...` line from `docker-compose.yml`
- [x] 1.4 Replace all hardcoded credential values in `docker-compose.yml` with `${VARIABLE_NAME}` references (JWT_SECRET, PASSWORD_DB, MINIO_ACCESS_KEY, MINIO_SECRET_KEY, Grafana admin password)
- [x] 1.5 Delete `backend/.env` from the filesystem and confirm it is listed in `.gitignore` (not just via `*.env` wildcard — add `backend/.env` explicitly)
- [x] 1.6 Rotate the two exposed JWT secrets and both Google Maps API keys (mark as done in `CREDENTIAL_ROTATION.md`)

## 2. Application Configuration Fixes

- [x] 2.1 Set `spring.jpa.hibernate.ddl-auto: validate` in `backend/src/main/resources/application.yml`
- [x] 2.2 Set `spring.flyway.validate-on-migrate: true` and `out-of-order: false`, remove `ignore-missing-migrations: true`
- [x] 2.3 Change SMTP `auth` and `starttls.enable` to use env var references: `${MAIL_SMTP_AUTH:false}` and `${MAIL_STARTTLS_ENABLE:false}`
- [x] 2.4 Add `prometheus` to `management.endpoints.web.exposure.include` in `application.yml`
- [x] 2.5 Add a `prod` Spring profile (`application-prod.yml`) that sets `springdoc.api-docs.enabled: false` and `springdoc.swagger-ui.enabled: false`
- [x] 2.6 Add a Spring Security rule to block `/swagger-ui*` and `/v3/api-docs*` for unauthenticated requests (or disable via prod profile per 2.5)

## 3. Docker Compose Infrastructure

- [x] 3.1 Mount `postgres_data` named volume to `db` service at `/var/lib/postgresql/data`
- [x] 3.2 Add `healthcheck` to `db` service: `pg_isready -U ${USER_DB:-localadmin}`
- [x] 3.3 Add `healthcheck` to `minio` service using MinIO health endpoint
- [x] 3.4 Add `healthcheck` to `redis` service: `redis-cli ping`
- [x] 3.5 Change backend `depends_on` for `db`, `minio`, and `redis` from `service_started` to `service_healthy`
- [x] 3.6 Add `restart: unless-stopped` to every service in `docker-compose.yml`
- [x] 3.7 Pin all Docker image tags to specific versions (replace `minio/minio` → `minio/minio:RELEASE.2024-11-07T00-52-20Z`, `mailhog/mailhog:latest` → specific version, etc.)
- [x] 3.8 Remove `no_cache: true` and `BUILDKIT_INLINE_CACHE=0` from the backend build configuration
- [x] 3.9 Remove the obsolete top-level `version: "3"` key from `docker-compose.yml`
- [x] 3.10 Define `app-network` and `monitoring-network` Compose networks; assign each service to appropriate networks only
- [x] 3.11 Add `deploy.resources.limits` with memory caps to all services
- [x] 3.12 Fix duplicate Grafana alert volume mount (remove the conflicting second entry)
- [x] 3.13 Set Grafana admin credentials via env var references (not hardcoded `admin`/`admin`)

## 4. Nginx Security Hardening

- [x] 4.1 Add `X-Frame-Options: SAMEORIGIN`, `X-Content-Type-Options: nosniff`, `X-XSS-Protection: 1; mode=block`, `Referrer-Policy: strict-origin-when-cross-origin` headers to `frontend/nginx.conf`
- [x] 4.2 Add a `Content-Security-Policy` header with `default-src 'self'` and appropriate source directives for Angular Material
- [x] 4.3 Replace `Access-Control-Allow-Origin: *` with a specific origin value (e.g., `http://localhost:4200` for dev; parameterizable for production)
- [x] 4.4 Add `proxy_connect_timeout 60s`, `proxy_read_timeout 120s`, and `proxy_send_timeout 60s` to the `/api/` location block
- [x] 4.5 Add a commented-out HTTPS server block (port 443 with TLS scaffolding) to `nginx.conf`

## 5. Secrets Documentation and .env.example

- [x] 5.1 Update root `.env.example` to include every `${VAR}` reference from `docker-compose.yml` with placeholder values and comments
- [x] 5.2 Mark `JWT_SECRET`, `GOOGLE_MAPS_API_KEY`, `GOOGLE_MAPS_FRONTEND_API_KEY` as `# REQUIRED` with format requirements documented
- [x] 5.3 Add `MAIL_SMTP_AUTH`, `MAIL_STARTTLS_ENABLE`, `GRAFANA_ADMIN_PASSWORD`, `GRAFANA_DB_PASSWORD` to `.env.example`
- [x] 5.4 Update `CREDENTIAL_ROTATION.md` to reflect completed rotations and the new `JWT_SECRET` requirement
- [x] 5.5 Add a `## Local Setup` section to `CLAUDE.md` documenting that developers must copy `.env.example` to `.env` before running docker-compose

## 6. Build Tooling Fixes

- [x] 6.1 Replace deprecated `buildDir` with `layout.buildDirectory.dir(...)` in `backend/build.gradle` Jacoco configuration
- [x] 6.2 Remove `mavenLocal()` from the repositories block in `backend/build.gradle` (or move it after `mavenCentral()`)
- [x] 6.3 Update `version` in `backend/build.gradle` from `0.0.1-SNAPSHOT` to a meaningful version (e.g., `1.0.0`)

## 7. Integration Test Quality

- [x] 7.1 Add a `BaseIntegrationTest` abstract class in `backend/src/test/java/` that uses `@Testcontainers` and `@Container` to start a PostgreSQL container
- [x] 7.2 Update `backend/src/test/resources/application-test.yml` to remove the H2 datasource override and use the Testcontainers-provided datasource URL via `@DynamicPropertySource`
- [x] 7.3 Run `./gradlew test` to verify all existing tests pass with the new Testcontainers PostgreSQL setup
- [x] 7.4 Verify Flyway migrations execute successfully against the Testcontainers PostgreSQL instance

## 8. CI/CD Pipeline

- [x] 8.1 Create `.github/workflows/ci.yml` — triggered on PR and push to `main`/`develop`; runs backend build + test, frontend lint, frontend test, and gitleaks secrets scan
- [x] 8.2 Create `.github/workflows/docker-build.yml` — triggered on push to `main` and on git tags; builds and pushes backend and frontend Docker images to GHCR
- [x] 8.3 Configure `gitleaks` in CI by adding `.gitleaks.toml` or using the official GitHub Action
- [x] 8.4 Add a GitHub Actions badge to `README.md` showing CI status
- [x] 8.5 Document required GitHub Secrets (`JWT_SECRET`, `GHCR credentials`) in `CREDENTIAL_ROTATION.md` under a `## CI/CD Secrets` section

## 9. Verification

- [x] 9.1 Run `docker-compose up --build` from a clean state and verify all services reach `healthy` status
- [x] 9.2 Run `./gradlew test` and confirm all tests pass with Testcontainers
- [x] 9.3 Start the backend without `JWT_SECRET` set and confirm it fails with a clear error message
- [x] 9.4 Verify Prometheus is successfully scraping backend metrics at `/actuator/prometheus`
- [x] 9.5 Run a browser security headers check (e.g., `curl -I http://localhost:4200`) and confirm all required headers are present
- [x] 9.6 Run `gitleaks detect --source .` locally and confirm no secrets are flagged
