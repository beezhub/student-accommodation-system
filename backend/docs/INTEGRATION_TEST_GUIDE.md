# Integration Testing Guide
This guide outlines standards and best practices for writing integration tests in Spring Boot applications. It serves as a reference for developers to ensure consistency and reliability when verifying the interaction between Spring Boot components, databases, and external services.
## 1. Overview
Integration tests ensure that the application context loads correctly and that different parts of the system work together as expected. We recommend using **Testcontainers** to spin up ephemeral dependencies like databases and message brokers, ensuring a reproducible environment that does not depend on locally installed services.
## 2. Base Configuration Pattern
Integration tests should share a common configuration to reduce boilerplate and ensure consistency.
### 2.1. Abstract Base Class
Create an abstract base class (e.g., `AbstractIntegrationTest`) that your test classes extend.
**Key Annotations:**
- `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`: Bootstraps the full Spring application context on a random port.
- `@AutoConfigureWebTestClient`: Configures `WebTestClient` for testing REST endpoints.
### 2.2. Testcontainers Setup
Define your external dependencies (Databases, Message Queues, Mail Servers) as static container instances in the base class.
**Example (PostgreSQL):**
```java
// AbstractIntegrationTest.java
protected static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
static {
    postgreSQLContainer.start();
}
```
### 2.3. Dynamic Properties
Use `@DynamicPropertySource` to inject the running container's properties (host, port, credentials) into the Spring Environment at runtime. This overrides the default `application.yml` settings.
```java
@DynamicPropertySource
static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
    registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
    registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
}
```
## 3. Writing Integration Tests
### 3.1. Test Class Structure
Tests should extend the abstract base class and focus on a specific feature or component.
```java
import org.junit.jupiter.api.Test;
class MyFeatureTest extends AbstractIntegrationTest {
    @Test
    void testFeatureFlow() {
        // 1. Arrange (Setup data)
        // 2. Act (Call API or Service)
        // 3. Assert (Verify Database State / Response)
    }
}
```
### 3.2. Database State Management
**Crucial:** Automated tests must run in isolation. Since the database container is often reused across the test suite for performance:
- Use `@BeforeEach` or `@AfterEach` to clean up modified tables.
- Or use `@Transactional` on test methods (with caution, as it may not catch transaction commit issues).
```java
@Autowired
private UserRepository userRepository;
@BeforeEach
void setUp() {
    userRepository.deleteAll(); // Clean slate before each test
}
```
## 4. Optional: Workflow Testing (Camunda/Zeebe)
If your project involves Camunda 8 (Zeebe), use the `zeebe-process-test` library.
### 4.1. Configuration
- Add `@ZeebeSpringTest` to your `AbstractIntegrationTest`.
- Inject `ZeebeClient` and `ZeebeTestEngine`.
### 4.2. Testing Patterns
- **Start Process**: Use `zeebeClient.newCreateInstanceCommand()`.
- **Verify State**: Use `BpmnAssert.assertThat(processInstance)`.
    - `hasPassedElement("Task_Name")`
    - `isCompleted()`
- **Wait States**: Use `zeebeTestEngine.waitForIdleState(Duration)` when testing async behaviors to prevent flaky tests.
## 5. Directory Structure Best Practices
- **Test Classes**: `src/test/java/<package>/integration/`
    - Keeping integration tests in a separate package or suffixing them with `IT` (e.g., `OrderServiceIT.java`) helps separate them from Unit tests.
- **Resources**: `src/test/resources/`
    - `application-test.yml`: Specific test overrides (e.g., logging levels).
    - SQL scripts for schema creation or data seeding.
