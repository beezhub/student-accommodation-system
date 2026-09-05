package com.beezhub.student_accommodation;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Shared base class for all full-stack integration tests.
 *
 * <p>Every integration test class must extend this class. Test classes must never configure
 * containers or the Spring context on their own.
 *
 * <p>Containers are started once per JVM (static initialiser) and shared across all
 * subclasses, keeping the suite fast. {@link DynamicPropertySource} wires each container's
 * runtime connection details into the Spring {@code Environment}, overriding whatever is in
 * {@code application.yml} or {@code application-test.yml}.
 *
 * <h2>Adding a new external dependency</h2>
 * <ol>
 *   <li>Declare a new {@code static} container field below.</li>
 *   <li>Start it in the {@code static} block.</li>
 *   <li>Map its connection properties in {@link #properties}.</li>
 *   <li>All subclasses inherit the new container automatically.</li>
 * </ol>
 *
 * <h2>HTTP testing</h2>
 * Subclasses that need to make HTTP calls can inject {@code @Autowired TestRestTemplate}
 * — it is auto-configured by Spring Boot when {@code webEnvironment = RANDOM_PORT}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    /** Test-only signing key; long enough to satisfy {@code JwtConfig} and HS512. Not a real secret. */
    private static final String TEST_JWT_SECRET =
            "test-only-jwt-secret-not-for-production-use-0123456789abcdefghijklmnopqrstuvwxyz";

    // ── Containers ────────────────────────────────────────────────────────────

    private static final PostgreSQLContainer<?> database =
            new PostgreSQLContainer<>("postgres:16.2")
                    .withDatabaseName("accommodation_test")
                    .withUsername("test")
                    .withPassword("test");

    /** Exposed so subclasses can query the mail server (e.g. assert emails were sent). */
    protected static final GenericContainer<?> mailServer =
            new GenericContainer<>("mailhog/mailhog")
                    .withExposedPorts(1025, 8025);

    private static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7")
                    .withExposedPorts(6379);

    private static final GenericContainer<?> minio =
            new GenericContainer<>("minio/minio")
                    .withEnv("MINIO_ROOT_USER", "minioadmin")
                    .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
                    .withCommand("server /data --console-address :9001")
                    .withExposedPorts(9000)
                    .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));

    static {
        database.start();
        mailServer.start();
        redis.start();
        minio.start();
    }

    // ── Spring property injection ─────────────────────────────────────────────

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // Database
        registry.add("spring.datasource.url", database::getJdbcUrl);
        registry.add("spring.datasource.username", database::getUsername);
        registry.add("spring.datasource.password", database::getPassword);

        // Flyway — must point at the same container as the datasource
        registry.add("spring.flyway.url", database::getJdbcUrl);
        registry.add("spring.flyway.user", database::getUsername);
        registry.add("spring.flyway.password", database::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");

        // Mail server
        registry.add("spring.mail.host", mailServer::getHost);
        registry.add("spring.mail.port", mailServer::getFirstMappedPort);

        // Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);

        // MinIO
        registry.add("minio.endpoint",
                () -> "http://" + minio.getHost() + ":" + minio.getMappedPort(9000));
        registry.add("minio.access-key", () -> "minioadmin");
        registry.add("minio.secret-key", () -> "minioadmin");

        // Schema management is handled by Flyway — Hibernate must not validate or generate DDL.
        // Both keys are set because application.yml uses the spring.jpa.properties.* path which
        // takes precedence over the spring.jpa.hibernate.ddl-auto shortcut.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.properties.hibernate.hbm2ddl.auto", () -> "none");

        // JWT — application.yml has no default for jwt.secret (fail-fast startup check requires
        // >= 64 chars), so the suite must supply one rather than depend on a JWT_SECRET env var.
        registry.add("jwt.secret", () -> TEST_JWT_SECRET);

        // Settings that application.yml deliberately leaves without a default, so a real deployment
        // fails fast rather than starting on development credentials or localhost URLs (GH-135).
        // The suite supplies its own values for the same reason it supplies jwt.secret above:
        // depending on ambient environment variables would make the build machine-specific.
        registry.add("app.base-url", () -> "http://localhost:4200");
        registry.add("app.security.allowed-origins", () -> "http://localhost:4200");
        registry.add("app.security.storage-public-origin", () -> "http://localhost:9000");
    }
}
