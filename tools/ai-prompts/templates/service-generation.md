````markdown
# Spring Boot Service Generation Template

Use this template when generating Spring Boot services with AI assistance.

## Prompt Template

```markdown
Create a Spring Boot service following microservices architecture principles:

## Service Context

- **Service Name**: [ServiceName]Service
- **Domain**: [Business domain this service handles]
- **Responsibilities**: [Specific business logic responsibilities]
- **Integration Points**: [Other services or external systems it interacts with]

## Requirements

- Implement [specific business functionality]
- Follow Single Responsibility Principle
- Use Spring Boot 3+ features and annotations
- Implement proper exception handling
- Include validation and security measures
- Support distributed tracing and monitoring

## Technical Specifications

- Use @Service annotation for service layer
- Implement proper dependency injection with constructor injection
- Use @Transactional where appropriate
- Include comprehensive logging with SLF4J
- Implement proper error handling with custom exceptions
- Use DTOs for data transfer
- Include input validation with @Valid annotations

## Architecture Requirements

- Follow Clean Architecture principles
- Implement repository pattern for data access
- Use domain-driven design concepts
- Support event-driven communication if applicable
- Include circuit breaker pattern for external calls
- Implement proper caching strategy

## Security Requirements

- Implement proper authorization checks
- Validate all inputs and sanitize data
- Use @PreAuthorize for method-level security
- Handle sensitive data appropriately
- Include audit logging for important operations

## Testing Requirements

- Generate comprehensive unit tests with JUnit 5
- Include integration tests with @SpringBootTest
- Mock external dependencies with @MockBean
- Test exception scenarios and edge cases
- Achieve minimum 85% code coverage
- Include performance tests for critical operations

## File Structure

Generate these files:

- [ServiceName]Service.java (main service implementation)
- [ServiceName]ServiceImpl.java (if interface is needed)
- [ServiceName]ServiceTest.java (unit tests)
- [ServiceName]ServiceIntegrationTest.java (integration tests)

## Code Style

- Use Java 21 features where appropriate
- Follow Spring Boot naming conventions
- Include comprehensive JavaDoc comments
- Use consistent formatting and indentation
- Apply code quality best practices

## Example Usage

[Provide service usage examples and integration patterns]

## Dependencies

List required Spring Boot starters and additional dependencies.
```

## Example Implementation Request

```markdown
Create a Spring Boot service for managing student accommodation bookings:

## Service Context

- **Service Name**: BookingManagementService
- **Domain**: Student accommodation booking and reservation management
- **Responsibilities**: Handle booking lifecycle, validate availability, process payments
- **Integration Points**: PaymentService, NotificationService, PropertyService

## Requirements

- Create, update, and cancel bookings
- Validate room availability and booking conflicts
- Calculate pricing with discounts and fees
- Send booking confirmations and updates
- Handle payment processing integration
- Support booking status transitions

## Technical Specifications

- Use @Transactional for booking operations
- Implement optimistic locking for concurrent bookings
- Include comprehensive validation for booking data
- Use events for booking state changes
- Implement retry logic for payment processing
- Include distributed caching for availability checks

## Architecture Requirements

- Follow domain-driven design with booking aggregate
- Implement saga pattern for complex booking workflows
- Use event sourcing for booking history
- Support eventual consistency across services
- Include compensation actions for failures

## Security Requirements

- Validate user permissions for booking operations
- Implement rate limiting for booking requests
- Audit all booking-related activities
- Sanitize all input data
- Protect against booking manipulation attacks

## Testing Requirements

- Test all booking scenarios and edge cases
- Mock external service dependencies
- Include concurrent booking conflict tests
- Test payment integration scenarios
- Validate event publishing and handling
- Performance tests for high-volume booking periods
```

## Quality Checklist

Before submitting AI-generated service code:

- [ ] Service follows Single Responsibility Principle
- [ ] Uses proper Spring Boot annotations and patterns
- [ ] Includes comprehensive error handling
- [ ] Implements proper security validations
- [ ] Has transaction management where needed
- [ ] Includes thorough unit and integration tests
- [ ] Follows Clean Architecture principles
- [ ] Has proper logging and monitoring
- [ ] Implements caching where appropriate
- [ ] Includes performance optimizations
- [ ] Code is properly documented with JavaDoc
- [ ] Follows project coding standards

## Spring Boot Best Practices

- **Constructor Injection**: Use constructor injection instead of field injection
- **Immutable DTOs**: Create immutable data transfer objects
- **Proper Exceptions**: Use custom exceptions with proper error codes
- **Validation**: Use Bean Validation (@Valid, @NotNull, @Size, etc.)
- **Configuration**: Externalize configuration with @ConfigurationProperties
- **Health Checks**: Implement custom health indicators
- **Metrics**: Add custom metrics with Micrometer
- **Documentation**: Generate API docs with SpringDoc OpenAPI
````
