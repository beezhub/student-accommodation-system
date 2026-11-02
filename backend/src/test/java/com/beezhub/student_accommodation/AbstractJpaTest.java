package com.beezhub.student_accommodation;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared base class for JPA-layer slice tests.
 *
 * <p>Use this class — not {@link AbstractIntegrationTest} — when the test exercises only the
 * JPA layer (repositories, Hibernate filters, entity-lifecycle callbacks, etc.) and does not
 * need the full application context or any non-JPA infrastructure (Redis, object storage, mail).
 *
 * <p>Like {@link AbstractIntegrationTest}, this class starts containers once per JVM and
 * injects connection details via {@link DynamicPropertySource}, so individual test classes
 * must never configure containers themselves.
 *
 * <h2>Transaction behaviour</h2>
 * {@code @DataJpaTest} wraps each test in a transaction that is rolled back on completion.
 * This is intentional for JPA-layer tests that verify query behaviour (e.g. Hibernate
 * filters, entity graphs) rather than transaction-commit correctness. Tests that need to
 * validate commit semantics should extend {@link AbstractIntegrationTest} instead.
 *
 * <h2>Schema</h2>
 * {@code ddl-auto=create-drop} is used so Hibernate derives the schema directly from entity
 * mappings. This keeps JPA slice tests self-contained and fast. If you need to validate
 * Flyway migration scripts, use {@link AbstractIntegrationTest}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractJpaTest {

    // ── Container ─────────────────────────────────────────────────────────────

    private static final PostgreSQLContainer<?> database =
            new PostgreSQLContainer<>("postgres:16.2")
                    .withDatabaseName("accommodation_test")
                    .withUsername("test")
                    .withPassword("test");

    static {
        database.start();
    }

    // ── Spring property injection ─────────────────────────────────────────────

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // Database
        registry.add("spring.datasource.url", database::getJdbcUrl);
        registry.add("spring.datasource.username", database::getUsername);
        registry.add("spring.datasource.password", database::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // Override the H2Dialect set in application-test.yml
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");

        // Schema managed by Hibernate for this JPA slice (not by Flyway).
        // Both keys are set because application.yml uses the spring.jpa.properties.* path which
        // takes precedence over the spring.jpa.hibernate.ddl-auto shortcut.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.hbm2ddl.auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }
}
