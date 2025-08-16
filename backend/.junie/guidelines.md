Project development guidelines for Student Accommodation System (Backend)

This document captures project-specific build, configuration, testing, and development practices that have proven useful for this codebase.

1. Build and run
- Java/Gradle toolchain
  - Java 21 is required. The project uses Gradle Toolchains (configured in build.gradle) so the Gradle wrapper can provision the proper JDK if configured. Use the included wrapper; do not install Gradle manually.
  - Commands (Windows PowerShell from repository root):
    - Build: .\gradlew.bat clean build
    - Run unit tests only: .\gradlew.bat test
    - Assemble runnable JAR: .\gradlew.bat bootJar
    - Run the app: .\gradlew.bat bootRun
- Docker image
  - Multi-stage Dockerfile builds the Boot JAR (tests are skipped in image build via -x test) and runs it on Eclipse Temurin JRE 21 as a non-root user. Typical build/run:
    - docker build -t student-accommodation-backend:local .
    - docker run --rm -p 8080:8080 --env-file .env student-accommodation-backend:local
  - The image exposes /actuator/health for its health check.
- Performance/runtime flags
  - The image sets UseContainerSupport and caps MaxRAMPercentage; JVM params live in Dockerfile, not in Gradle.

2. Configuration (application.yml and environment)
The application is externalized via Spring Boot properties with sensible local defaults. Override via environment variables in any runtime (bootRun, packaged JAR, Docker).
- Database (PostgreSQL)
  - Defaults target a local PostgreSQL: jdbc:postgresql://localhost:5432/studentaccommodationlocaldb with user localadmin / password admin.
  - Environment variables to override:
    - DB_URL — full JDBC URL
    - USER_DB — database username
    - PASSWORD_DB — database password
  - HikariCP is tuned modestly (max pool 10). Hibernate ddl-auto is set to update, while Flyway is enabled for schema management.
- Flyway
  - baseline-on-migrate: true, enabled: true
  - Locations: classpath:db/migration
  - Migrations follow Flyway’s versioned naming, e.g., V2025_05_14_2136__init_db.sql. Use the same convention for new scripts and keep them idempotent where possible.
  - For local dev, Flyway will run on application start using the configured DB credentials.
- JWT
  - jwt.secret — HMAC secret (string). Default is provided in application.yml for local usage only; replace in all non-local environments.
  - jwt.expiration-time-ms — token TTL as milliseconds (default 10 hours).
  - JwtUtil constructs a SecretKey from the raw secret bytes (UTF-8). Ensure the secret is sufficiently long (>= 256 bits for HS256).
- MinIO (Object storage)
  - minio.endpoint — default http://localhost:9000
  - minio.access-key, minio.secret-key — defaults to minioadmin/minioadmin
  - minio.bucket-name — default student-accommodation-bucket
  - MinioClient is configured in MinioConfig and injected where needed. Ensure the bucket exists or is created on startup if you add such logic.
- Swagger/OpenAPI
  - OpenAPI UI available at /swagger-ui.html; JSON at /v3/api-docs.
- Security & CORS
  - SecurityConfig permits /api/auth/** and Swagger endpoints, everything else requires JWT Bearer auth.
  - A simple global CorsFilter allows all origins and methods; consider tightening for production.

3. Testing
- Frameworks and conventions
  - JUnit 5 + Mockito are used for unit testing. Tests prefer isolated unit tests over SpringBootTest; they do not require a running database or Spring context.
  - Use @ExtendWith(MockitoExtension.class) and inject @Mock/@InjectMocks where applicable (see AuthServiceTest for canonical style).
  - Security-related classes (e.g., JwtUtil) are tested without bringing up the context; construct dependencies directly.
- Running tests
  - All tests: .\gradlew.bat test
  - One package/class (examples):
    - .\gradlew.bat test --tests "com.beezhub.student_accommodation.service.AuthServiceTest"
    - .\gradlew.bat test --tests "*AuthServiceTest" (glob)
  - One test method:
    - .\gradlew.bat test --tests "com.beezhub.student_accommodation.service.AuthServiceTest.validateLogin_ShouldReturnLoginResponse"
- Test logging and reports
  - Tests log passed, skipped, failed with full exception stack traces.
  - JaCoCo reports are generated after tests:
    - HTML: build/jacocoHtml/index.html
    - XML/CSV also generated. Coverage verification is enabled but currently does not enforce a minimum ratio (minimum = 0.0). Exclusions present in build.gradle appear legacy and do not match this project’s package; they are harmless.
- Adding a new test (guidance)
  - Prefer pure unit tests against services, mappers, and utility classes. Mock collaborators; avoid @SpringBootTest unless you need wiring.
  - If testing MapStruct mappers, either instantiate the mapper via Mappers.getMapper(YourMapper.class) for pure unit testing or rely on Spring injection in integration tests.
  - For enums/DTOs, simple presence/serialization tests are acceptable to ensure stability of public contracts.
- Example process (validated in this repo)
  - We created and executed a minimal unit test for enum wiring:
    - File path: src/test/java/com/beezhub/student_accommodation/model/enums/ApplicationStatusTest.java
    - Command used: .\gradlew.bat test (or targeted test run)
    - Result: Test passed locally. The file was then removed per repository hygiene requirements.

4. Local development tips
- Database
  - You can develop and run most unit tests without PostgreSQL. PostgreSQL is only required when running the app or integration tests touching persistence.
  - When adding new migrations, prefer additive, forward-only scripts; avoid destructive changes unless coordinated.
- JWT
  - Keep the secret out of VCS for non-local environments. Prefer environment variables or a secrets manager. Token TTL should be short in production.
- MinIO
  - For local dev, spin up MinIO via Docker: docker run -p 9000:9000 -p 9001:9001 -e MINIO_ROOT_USER=minioadmin -e MINIO_ROOT_PASSWORD=minioadmin quay.io/minio/minio server /data --console-address ":9001"
  - Ensure the configured bucket exists. Consider adding a startup initializer if you need auto-creation across environments.
- Code style and libraries
  - Java 21, Lombok, MapStruct 1.6.3, Spring Boot 3.4.x. Keep annotations minimal in domain entities; push mapping logic to mappers.
  - Validation should use jakarta validation annotations in DTOs and be enforced at controller boundaries.
  - Controller endpoints are JWT-protected except for /api/auth/**. Be mindful of the SecurityFilterChain rules when adding new endpoints.
- Troubleshooting
  - If tests fail to compile related to MapStruct, ensure annotation processing is enabled in your IDE and Gradle (annotationProcessor dependencies are declared).
  - If JwtUtil errors on key length, supply a longer jwt.secret.
  - Flyway failures on startup usually indicate DB connectivity or version conflicts; check spring.flyway.* props and applied migrations in flyway_schema_history.

5. Release and CI notes
- Build artifacts: bootJar produces a single executable JAR under build/libs. Dockerfile will rename it to /app/app.jar during image build.
- Consider enabling Gradle Enterprise/Build Cache for faster CI; current build scripts are compatible.
- If you introduce integration tests requiring Dockerized dependencies (Postgres, MinIO), prefer Testcontainers and annotate such tests with tags to exclude by default in CI.

Appendix: Key environment variables
- DB_URL, USER_DB, PASSWORD_DB
- JWT_SECRET, JWT_EXPIRATION
- MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY, MINIO_BUCKET
