## ADDED Requirements

### Requirement: Application refuses to start without JWT_SECRET
The Spring Boot application MUST fail to start if the `JWT_SECRET` environment variable is not set or is shorter than 64 characters. The startup error MUST produce a clear, actionable message identifying that `JWT_SECRET` is missing or invalid. There MUST be no default fallback value for `JWT_SECRET` in `application.yml`.

#### Scenario: Application fails with missing JWT_SECRET
- **WHEN** the application starts without `JWT_SECRET` set in the environment
- **THEN** the application throws a startup exception with a message indicating that `JWT_SECRET` must be configured

#### Scenario: Application fails with short JWT_SECRET
- **WHEN** the application starts with `JWT_SECRET` set to a value shorter than 64 characters
- **THEN** the application throws a startup exception indicating the secret is too short

#### Scenario: Application starts with valid JWT_SECRET
- **WHEN** the application starts with `JWT_SECRET` set to a value of 64 or more characters
- **THEN** the application starts normally without any JWT secret configuration errors

### Requirement: docker-compose.yml contains no hardcoded credential values
The `docker-compose.yml` file MUST NOT contain any literal values for secrets, passwords, API keys, or tokens. All such values MUST use `${VARIABLE_NAME}` syntax referencing environment variables. There MUST be no commented-out lines containing real credential values.

#### Scenario: No literal secrets in docker-compose.yml
- **WHEN** `docker-compose.yml` is read
- **THEN** no line contains a recognizable credential pattern (JWT token, password assignment, API key)

### Requirement: .env.example documents all required environment variables
The root `.env.example` file MUST contain every environment variable consumed by `docker-compose.yml` and `application.yml`. Each variable MUST have a placeholder value (not a real value) and a comment explaining its purpose and format requirements. Variables that are required (no safe default) MUST be marked `# REQUIRED`.

#### Scenario: All docker-compose variables documented
- **WHEN** `.env.example` is compared against all `${VAR}` references in `docker-compose.yml`
- **THEN** every variable referenced in `docker-compose.yml` has a corresponding entry in `.env.example`

#### Scenario: Required variables are marked
- **WHEN** `.env.example` is read
- **THEN** `JWT_SECRET`, `GOOGLE_MAPS_API_KEY`, `GOOGLE_MAPS_FRONTEND_API_KEY`, and any other non-defaultable secrets are marked with a `# REQUIRED` comment

### Requirement: SMTP configuration respects environment variable overrides
The Spring Boot `application.yml` SMTP settings for `auth` and `starttls.enable` MUST use environment variable references with safe defaults: `${MAIL_SMTP_AUTH:false}` and `${MAIL_STARTTLS_ENABLE:false}`. Operators MUST be able to enable SMTP auth and TLS by setting these variables without modifying application configuration files.

#### Scenario: SMTP auth enabled via environment variable
- **WHEN** `MAIL_SMTP_AUTH=true` is set in the environment
- **THEN** the application sends email with SMTP authentication

#### Scenario: STARTTLS enabled via environment variable
- **WHEN** `MAIL_STARTTLS_ENABLE=true` is set in the environment
- **THEN** the application negotiates STARTTLS when connecting to the mail server

### Requirement: Prometheus actuator endpoint is exposed
The Spring Boot actuator endpoint `prometheus` MUST be included in `management.endpoints.web.exposure.include`. The Prometheus scrape target at `/actuator/prometheus` MUST return valid Prometheus metrics format.

#### Scenario: Prometheus scrape endpoint is accessible
- **WHEN** Prometheus sends a scrape request to `http://backend:8080/actuator/prometheus`
- **THEN** the endpoint returns HTTP 200 with a `text/plain; version=0.0.4` content type and valid metrics

### Requirement: Hibernate DDL is set to validate in non-development profiles
The `spring.jpa.hibernate.ddl-auto` property MUST be set to `validate` in the default and production Spring profiles. Setting it to `update` or `create` in production MUST be explicitly prevented. Flyway MUST be set with `validate-on-migrate: true` and `out-of-order: false`.

#### Scenario: Hibernate does not modify schema on startup
- **WHEN** the application starts in the default profile with an already-migrated database
- **THEN** Hibernate performs schema validation only and does not issue any DDL statements

#### Scenario: Application fails if schema does not match entities
- **WHEN** the application starts and the database schema does not match the JPA entity mapping
- **THEN** Hibernate validation fails and the application refuses to start

### Requirement: Swagger UI is disabled or protected in production
In any non-development profile, the Swagger UI (`/swagger-ui.html`) and API docs (`/v3/api-docs`) endpoints MUST either be disabled or require authentication. Unauthenticated access to these endpoints in production SHALL be prevented.

#### Scenario: Swagger UI disabled in production profile
- **WHEN** the application runs with the `prod` Spring profile
- **THEN** requests to `/swagger-ui.html` return 404 or 403
