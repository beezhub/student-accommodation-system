## Why

The student accommodation system has 40+ identified DevOps issues spanning critical security vulnerabilities (committed secrets, default credentials), broken infrastructure (no data persistence, silently-failing monitoring), and absent operational practices (no CI/CD pipeline, no healthchecks, no restart policies). These issues must be resolved before the system can safely handle real student data in production.

## What Changes

- Remove all hardcoded and default credentials from tracked files and application configuration
- Fix JWT secret to require explicit environment variable with no fallback default
- Correct Hibernate DDL and Flyway settings to safe production values
- Add healthchecks and `service_healthy` dependencies to Docker Compose services
- Mount the PostgreSQL data volume so database state persists across restarts
- Add `restart: unless-stopped` policies to all containers
- Pin all Docker image tags to specific versions
- Harden Nginx: add security headers, remove wildcard CORS, add proxy timeouts
- Add a GitHub Actions CI/CD pipeline (build, test, lint, secrets scan)
- Expose the `prometheus` actuator endpoint so Prometheus scraping works
- Fix SMTP configuration to respect environment variable overrides for auth/TLS
- Add Swagger UI authentication gate (disable in production profile)
- Remove `no_cache: true` from docker-compose to restore Docker layer caching
- Fix deprecated `buildDir` Gradle API and `mavenLocal()` repository ordering
- Add Testcontainers PostgreSQL for integration tests (replace H2)
- Add Docker network segmentation and resource limits

## Capabilities

### New Capabilities

- `ci-cd-pipeline`: GitHub Actions workflow for automated build, test, lint, secrets scanning, and image publishing on every PR and push to main
- `docker-compose-hardening`: Production-safe docker-compose configuration with healthchecks, restart policies, pinned images, volume persistence, network isolation, and resource limits
- `nginx-security`: Nginx configuration with security headers (CSP, HSTS, X-Frame-Options, X-Content-Type-Options), restricted CORS, HTTPS redirect scaffolding, and proxy timeouts
- `secrets-hygiene`: Documented credential rotation procedure, removal of all default/fallback secrets, `.env.example` completeness, and secrets scanning integration

### Modified Capabilities

<!-- No existing spec files exist to modify -->

## Impact

- **`docker-compose.yml`** — multiple service changes (volumes, healthchecks, restart, image tags, credentials, networks, resource limits, cache settings)
- **`backend/src/main/resources/application.yml`** — JWT secret, Hibernate DDL mode, Flyway settings, SMTP settings, actuator exposure, Swagger access
- **`frontend/nginx.conf`** — security headers, CORS restriction, proxy timeouts
- **`backend/build.gradle`** — deprecated `buildDir` API, `mavenLocal()` removal
- **`.github/workflows/`** — new CI/CD pipeline (new files)
- **`backend/src/test/resources/application-test.yml`** — switch from H2 to Testcontainers
- **`CREDENTIAL_ROTATION.md`** — update rotation status and procedure
