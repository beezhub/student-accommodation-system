````markdown
# Java Test Generation Template

Use this template for generating comprehensive Java test suites with JUnit 5 and Mockito.

## Prompt Template

````markdown
Generate comprehensive Java test suite using JUnit 5 and Mockito:

## Test Context

- **Class Under Test**: [ClassName]
- **Test Type**: [Unit/Integration/End-to-End]
- **Testing Framework**: JUnit 5 + Mockito
- **Coverage Target**: 85%+ line coverage
- **Spring Boot Version**: 3.x (if applicable)

## Requirements

- Follow Test-Driven Development (TDD) principles
- Implement comprehensive test scenarios
- Include positive and negative test cases
- Test all public methods and edge cases
- Mock external dependencies appropriately
- Include integration tests where needed

## Technical Specifications

- Use JUnit 5 annotations (@Test, @BeforeEach, @AfterEach)
- Use Mockito for mocking (@Mock, @InjectMocks, @MockBean)
- Use AssertJ for fluent assertions
- Include @SpringBootTest for integration tests
- Use @WebMvcTest for controller tests
- Use @DataJpaTest for repository tests
- Include TestContainers for database integration

## Test Categories

Generate tests for:

### Unit Tests

- **Method Behavior**: Test each public method's expected behavior
- **Input Validation**: Test with valid and invalid inputs
- **Exception Scenarios**: Test error handling and exception throwing
- **Edge Cases**: Test boundary conditions and unusual inputs
- **State Verification**: Verify object state changes
- **Interaction Verification**: Verify method calls on dependencies

### Integration Tests

- **Database Integration**: Test repository operations with real database
- **External Service Integration**: Test API calls and responses
- **Spring Context Loading**: Test Spring Boot application context
- **Transaction Behavior**: Test transaction rollback scenarios
- **Security Integration**: Test authentication and authorization

### Test Structure

Follow the AAA (Arrange-Act-Assert) pattern:

```java
@Test
@DisplayName("Should [expected behavior] when [condition]")
void shouldDoSomethingWhenCondition() {
    // Arrange - Set up test data and mocks
    // Act - Execute the method under test
    // Assert - Verify the results
}
```
````

## Spring Boot Testing Patterns

### Service Layer Testing

```java
@ExtendWith(MockitoExtension.class)
class ServiceNameTest {
    @Mock
    private DependencyRepository repository;

    @InjectMocks
    private ServiceName serviceUnderTest;

    // Test methods
}
```

### Controller Testing

```java
@WebMvcTest(ControllerName.class)
class ControllerNameTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ServiceName service;

    // Test methods with MockMvc
}
```

### Repository Testing

```java
@DataJpaTest
class RepositoryNameTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RepositoryName repository;

    // Test methods with real database
}
```

## Testing Best Practices

- Use descriptive test method names with @DisplayName
- Group related tests with @Nested classes
- Use @ParameterizedTest for testing multiple inputs
- Include @TestMethodOrder for ordered test execution
- Use @DirtiesContext when test affects Spring context
- Mock only what you need to isolate the unit under test
- Use TestContainers for realistic integration testing

## Security Testing

Include tests for:

- Authentication scenarios
- Authorization with different roles
- Input validation and sanitization
- CSRF protection
- Rate limiting behavior

## Performance Testing

Include tests for:

- Response time verification
- Memory usage validation
- Concurrent access scenarios
- Database query optimization

## File Structure

Generate these files:

- [ClassName]Test.java (unit tests)
- [ClassName]IntegrationTest.java (integration tests)
- TestConfiguration.java (test-specific configuration)
- TestDataBuilder.java (test data creation utilities)

````

## Example Implementation Request

```markdown
Generate comprehensive test suite for BookingService class:

## Test Context
- **Class Under Test**: BookingService
- **Test Type**: Unit and Integration tests
- **Testing Framework**: JUnit 5 + Mockito + TestContainers
- **Coverage Target**: 90%+ line coverage
- **Spring Boot Version**: 3.2

## Requirements
- Test all booking lifecycle operations
- Include concurrent booking scenario tests
- Test payment integration workflows
- Validate booking business rules
- Test exception handling and rollback scenarios
- Include performance tests for high-volume periods

## Technical Specifications
- Mock BookingRepository, PaymentService, NotificationService
- Use TestContainers for PostgreSQL integration tests
- Include @Transactional tests for rollback scenarios
- Test optimistic locking behavior
- Include security context testing

## Test Categories

### Unit Tests
- createBooking() - successful booking creation
- createBooking() - duplicate booking prevention
- validateAvailability() - room availability checking
- calculatePricing() - price calculation with discounts
- processPayment() - payment processing scenarios
- cancelBooking() - cancellation with refund logic
- updateBooking() - booking modification rules

### Integration Tests
- End-to-end booking workflow
- Database transaction rollback scenarios
- External service integration (payment, notification)
- Concurrent booking conflict resolution
- Booking status transitions

### Performance Tests
- High-volume booking creation
- Concurrent availability checking
- Database query optimization validation
````

## Quality Checklist

Before submitting AI-generated test code:

- [ ] Tests follow AAA (Arrange-Act-Assert) pattern
- [ ] All public methods have corresponding tests
- [ ] Edge cases and error scenarios are covered
- [ ] Mocks are used appropriately for dependencies
- [ ] Integration tests validate end-to-end scenarios
- [ ] Test names clearly describe the scenario
- [ ] Code coverage meets or exceeds target
- [ ] Performance tests validate critical operations
- [ ] Security scenarios are tested
- [ ] Tests are maintainable and readable
- [ ] Test data setup is clean and reusable
- [ ] Proper cleanup in @AfterEach methods
````
