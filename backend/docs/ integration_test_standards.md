# Integration Testing Standards
This guide defines the **process and patterns** for writing integration tests in Spring Boot applications. It is technology-agnostic — the same principles apply regardless of your build tool (Maven, Gradle), migration tool (Liquibase, Flyway), or the external services your application depends on.
---
## 1. Core Principles
1. **Reproducibility** — Tests run against real, disposable infrastructure (via Testcontainers), not shared or pre-existing environments.
2. **Isolation** — Every test starts from a known state. No test depends on the outcome of another.
3. **Full context** — The entire Spring application context is loaded. Tests exercise the real wiring, not mocked-out slices.
4. **No manual setup** — A developer should be able to clone the repo and run tests with zero local configuration beyond having Docker installed.
---
## 2. Abstract Base Class
> [!IMPORTANT]
> Every integration test class **must** extend a shared abstract base class. Individual test classes must never configure containers or Spring context on their own.
### 2.1. Purpose
The base class is the **single source of truth** for:
- Starting and sharing containers (database, mail server, message broker, etc.)
- Injecting container connection details into the Spring Environment
- Applying common annotations
### 2.2. Annotations
The base class must be annotated to:
1. **Boot the full application context on a random port** — avoids port conflicts in CI.
2. **Auto-configure a web test client** — enables REST endpoint testing.
3. Add any **domain-specific test annotations** required by your project (e.g., embedded workflow engines).
### 2.3. Container Lifecycle
Containers are declared as **`static` fields** and started in a **`static` initializer block**.
```java
public class AbstractIntegrationTest {
    private static final PostgreSQLContainer<?> database =
            new PostgreSQLContainer<>("postgres:14")
                    .withDatabaseName("mydb")
                    .withInitScript("init_schema.sql");
    protected static final GenericContainer<?> mailServer =
            new GenericContainer<>("mailhog/mailhog")
                    .withExposedPorts(1025);
    static {
        database.start();
        mailServer.start();
    }
}
```
**Why static?**
| Approach | Behaviour |
|---|---|
| `static` block | Containers start **once per JVM**, before Spring context init. Shared across all test classes. Fast. |
| `@BeforeAll` | Runs per test class. Would restart containers for each class. Slow and wasteful. |
| `@BeforeEach` | Runs per test method. Completely impractical. |
> [!TIP]
> Mark containers `protected` if subclasses need direct access (e.g., to query a mail server). Mark them `private` if they are only consumed via injected Spring properties.
### 2.4. Dynamic Property Injection
Use `@DynamicPropertySource` to wire the **running** container's connection details into Spring's Environment. This replaces whatever is in your application config file at runtime.
```java
@DynamicPropertySource
static void properties(DynamicPropertyRegistry registry) {
    // Database
    registry.add("spring.datasource.url", database::getJdbcUrl);
    registry.add("spring.datasource.username", database::getUsername);
    registry.add("spring.datasource.password", database::getPassword);
    // Migration tool — point it at the same container
    registry.add("spring.flyway.url", database::getJdbcUrl);        // or spring.liquibase.url
    registry.add("spring.flyway.user", database::getUsername);
    registry.add("spring.flyway.password", database::getPassword);
    // Mail server
    registry.add("spring.mail.host", mailServer::getHost);
    registry.add("spring.mail.port", mailServer::getFirstMappedPort);
}
```
> [!CAUTION]
> Always disable Hibernate's DDL auto-generation (`spring.jpa.hibernate.ddl-auto=none`) in integration tests. Schema management must go through your migration tool so that tests validate the same migration scripts used in production.
### 2.5. Adding a New Container
When your application gains a new external dependency (Redis, Kafka, S3, etc.), the process is:
1. Add the container as a `static` field in `AbstractIntegrationTest`.
2. Start it in the `static` block.
3. Map its connection properties via `@DynamicPropertySource`.
4. **That's it** — all test classes inherit the new container automatically.
---
## 3. Writing Test Classes
### 3.1. Structure
```java
class OrderServiceTests extends AbstractIntegrationTest {
    @Autowired
    private OrderRepository orderRepository;
    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        // Seed known test data
    }
    @AfterEach
    void tearDown() {
        // Release non-Spring resources if needed
    }
    @Test
    @DisplayName("Order is persisted when payment succeeds")
    void testOrderPersistedOnSuccessfulPayment() {
        // 1. ARRANGE — set up preconditions
        // 2. ACT    — invoke the system under test
        // 3. ASSERT — verify the outcome
    }
}
```
### 3.2. Rules
| Rule | Rationale |
|---|---|
| Every `@Test` must have a `@DisplayName` | Produces human-readable output in CI logs and IDE runners |
| Follow **Arrange → Act → Assert** | Makes tests scannable and self-documenting |
| One logical assertion group per test | Keeps failure messages actionable |
| Extract repetitive steps into **private helper methods** | Keeps test methods focused on business logic |
### 3.3. Naming Conventions
| Element | Convention | Example |
|---|---|---|
| Test class | `<Feature>Tests` or `<Feature>IT` | `PaymentProcessTests` |
| Test method | `test<Scenario>` or descriptive camelCase | `testOrderDeniedWhenInsufficientFunds()` |
| Display name | Plain English sentence | `"Order denied when funds are insufficient"` |
---
## 4. Test Data Management
> [!IMPORTANT]
> Tests must be **isolated**. Never rely on data created by a previous test.
### 4.1. Clean and Seed in `@BeforeEach`
```java
@BeforeEach
void setUp() {
    repository.deleteAll();
    // Create deterministic, explicit test data
    Entity entity = new Entity()
            .setName("Test Entity")
            .setEmail("test@example.com");
    repository.save(entity);
}
```
### 4.2. Do NOT Use `@Transactional` on Test Methods
`@Transactional` on a test silently rolls back at the end of each test. This hides real bugs:
- Transaction commit errors
- Database constraint violations that only surface on flush
- Event listeners triggered on commit
  Instead, clean up explicitly in `@BeforeEach`.
### 4.3. Resource Cleanup in `@AfterEach`
Use `@AfterEach` for releasing resources that Spring does **not** manage:
```java
@AfterEach
void tearDown() {
    if (client != null) {
        client.close();
    }
}
```
---
## 5. Assertion Standards
### 5.1. Use Static Imports
```java
import static org.junit.jupiter.api.Assertions.*;
assertNotNull(result);
assertEquals(expected, actual);
assertTrue(condition, "Failure message explaining what went wrong");
```
### 5.2. Domain-Specific Assertion Libraries
If your project uses a domain-specific assertion library (e.g., BPMN assertions for workflow engines, Kafka assertions for messaging), prefer those over raw JUnit assertions for domain objects — they produce better failure messages.
```java
// Example: workflow assertion (adapt to your engine)
assertThat(processInstance)
        .hasPassedElement("Task_ReviewRequest")
        .isCompleted();
```
### 5.3. Chain Related Assertions
When verifying a multi-step process or flow, chain assertions in a single block to make the expected path readable at a glance:
```java
assertThat(processInstance)
        .hasPassedElement("Step_Validate")
        .hasPassedElement("Step_Process")
        .hasPassedElement("Step_Notify")
        .hasPassedElement("EndEvent_Success");
```
---
## 6. Helper Methods
### 6.1. When to Extract
If the same sequence of steps appears in **two or more tests**, extract it into a `private` helper method in the test class.
### 6.2. Pattern
Helper methods should:
1. Accept **only the parameters that vary** between callers.
2. Contain their own assertions to **fail fast** with clear messages.
3. Be named descriptively (e.g., `waitForTaskAndComplete`, `createAndSubmitOrder`).
```java
private Order createOrder(String customerId, BigDecimal amount) {
    Order order = new Order()
            .setCustomerId(customerId)
            .setAmount(amount);
    return orderRepository.save(order);
}
```
---
## 7. Test Coverage Expectations
Every feature should have integration tests covering:
| Scenario Type | Description |
|---|---|
| **Happy path** | The primary success flow works end-to-end |
| **Rejection / failure paths** | Each decision branch that leads to a negative outcome |
| **Validation failures** | Invalid input is caught and handled correctly |
| **Edge cases** | Boundary values, empty collections, null fields |
---
## 8. Directory Structure
```
src/
└── test/
    ├── java/<package>/
    │   ├── AbstractIntegrationTest.java   ← Shared base class (one per project)
    │   ├── FeatureATests.java             ← Tests grouped by feature
    │   └── FeatureBTests.java
    └── resources/
        ├── init_schema.sql                ← Schema bootstrap for test DB container
        └── application-test.yml           ← Optional test-specific config overrides
```
---
## 9. Quick-Reference Checklist
Use this when writing or reviewing an integration test:
- [ ] Test class extends `AbstractIntegrationTest`
- [ ] No container or Spring context configuration in the test class itself
- [ ] `@BeforeEach` cleans and seeds test data
- [ ] `@AfterEach` releases non-Spring-managed resources
- [ ] Every `@Test` has a `@DisplayName`
- [ ] Tests follow **Arrange → Act → Assert**
- [ ] Repetitive logic is extracted into helper methods
- [ ] No `@Transactional` on test methods
- [ ] Assertions use clear failure messages
- [ ] All decision branches are covered (happy path, failures, edge cases)
---
## 10. Prerequisites
- **Docker must be running** before executing integration tests — Testcontainers requires a Docker daemon to start containers.
- **No other local setup required** — the test infrastructure is fully self-contained.