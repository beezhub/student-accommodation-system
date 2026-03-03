## ADDED Requirements

### Requirement: All services define healthchecks
Every Docker Compose service that other services depend on (PostgreSQL, MinIO, Redis) MUST define a `healthcheck` block with `test`, `interval`, `timeout`, and `retries` fields.

#### Scenario: PostgreSQL healthcheck
- **WHEN** the `db` service container starts
- **THEN** it periodically runs `pg_isready -U ${USER_DB}` and transitions to `healthy` when the command succeeds

#### Scenario: MinIO healthcheck
- **WHEN** the `minio` service container starts
- **THEN** it periodically checks the MinIO health endpoint and transitions to `healthy` when MinIO is ready to accept requests

#### Scenario: Redis healthcheck
- **WHEN** the `redis` service container starts
- **THEN** it periodically runs `redis-cli ping` and transitions to `healthy` when Redis responds

### Requirement: Backend depends on healthy services
The `backend` service MUST declare `depends_on` with `condition: service_healthy` for `db`, `minio`, and `redis`. The backend container MUST NOT start until all three dependencies are healthy.

#### Scenario: Backend waits for healthy database
- **WHEN** `docker-compose up` is run from cold start
- **THEN** the backend container does not start until the `db` container reports `healthy`

### Requirement: All services have restart policies
Every service in `docker-compose.yml` MUST define `restart: unless-stopped`. Services MUST automatically restart after a non-zero exit code without manual intervention.

#### Scenario: Crashed container restarts
- **WHEN** any service container exits with a non-zero code
- **THEN** Docker Compose restarts the container automatically

### Requirement: All Docker image tags are pinned to specific versions
No service in `docker-compose.yml` MUST use `:latest` or an untagged image reference. Every `image:` field MUST specify a concrete version tag (e.g., `postgres:16.2-alpine`, `minio/minio:RELEASE.2024-11-07T00-52-20Z`).

#### Scenario: Pinned image versions
- **WHEN** `docker-compose pull` is run
- **THEN** no image is updated to a newer version than the pinned tag without an explicit version bump in the Compose file

### Requirement: PostgreSQL data persists across container restarts
The `db` service MUST mount the named volume `postgres_data` to `/var/lib/postgresql/data`. Database records MUST survive `docker-compose down` and `docker-compose up` cycles (but not `docker-compose down -v`).

#### Scenario: Data persists after container restart
- **WHEN** the `db` container is stopped and restarted
- **THEN** all previously created database records are accessible

### Requirement: Services are assigned to appropriate Docker networks
The `docker-compose.yml` MUST define at least two networks: `app-network` and `monitoring-network`. Application services (backend, frontend, db, redis, minio) MUST be on `app-network`. Monitoring services (Prometheus, Grafana) MUST be on `monitoring-network`. The backend MUST be on both networks to allow Prometheus to scrape it.

#### Scenario: Monitoring cannot reach PostgreSQL directly
- **WHEN** a container in `monitoring-network` attempts to connect to `db:5432`
- **THEN** the connection is refused because `db` is not on `monitoring-network`

### Requirement: Docker build layer caching is enabled
The backend service build configuration in `docker-compose.yml` MUST NOT set `no_cache: true`. Docker layer caching MUST be used to avoid re-downloading all Gradle dependencies on every build.

#### Scenario: Unchanged layers are cached
- **WHEN** `docker-compose build` is run after a source-only change
- **THEN** the Gradle dependency download layer is served from cache and not re-executed

### Requirement: Resource limits are defined for all services
Each service MUST define `deploy.resources.limits` with at minimum a `memory` limit appropriate to its role. No single container MUST be allowed to consume all available host memory.

#### Scenario: Resource limits applied
- **WHEN** the `docker-compose.yml` is read
- **THEN** every service has a `memory` limit defined under `deploy.resources.limits`
