# Test-Driven Development (TDD) Template

Use this template for AI-assisted Test-Driven Development workflow.

## TDD Prompt Template

```markdown
Implement [feature/component/service] using Test-Driven Development approach:

## TDD Context
- **Feature**: [Feature name]
- **Technology**: [Angular/Spring Boot/etc.]
- **Test Framework**: [Jasmine/JUnit/etc.]
- **Coverage Target**: 80%+ line coverage

## Phase 1: Write Failing Tests
Generate comprehensive test cases that cover:

### Unit Tests
- **Happy Path Tests**: Normal execution scenarios
- **Edge Cases**: Boundary conditions and unusual inputs  
- **Error Scenarios**: Exception handling and error states
- **Integration Points**: Interactions with dependencies
- **Accessibility Tests**: A11y compliance (for UI components)

### Test Categories
- [ ] Component/Class initialization
- [ ] Public method behaviors
- [ ] Input validation and sanitization
- [ ] Output/return value verification
- [ ] State management and side effects
- [ ] Error handling and recovery
- [ ] Performance considerations
- [ ] Security validations

## Phase 2: Minimal Implementation
Create the simplest possible implementation that makes tests pass:
- Focus on making tests pass, not on perfect code
- Implement minimal functionality
- Avoid over-engineering at this stage
- Maintain test coverage requirements

## Phase 3: Refactor
Improve code quality while maintaining test coverage:
- Apply SOLID principles
- Remove code duplication
- Improve readability and maintainability
- Optimize performance where necessary
- Ensure consistent coding standards

## Testing Best Practices
- Use descriptive test names that explain the scenario
- Follow Arrange-Act-Assert pattern
- Mock external dependencies appropriately
- Test one thing at a time
- Keep tests independent and isolated
- Include both positive and negative test cases

## Angular-Specific Testing
For Angular components/services include:
- TestBed configuration with proper imports
- Component fixture and instance setup
- DOM testing for UI components
- Service dependency injection testing
- HTTP interceptor and API call testing
- Router and navigation testing
- Form validation testing

## Spring Boot Testing
For Spring Boot services include:
- @MockMvc for web layer testing
- @DataJpaTest for repository testing
- @SpringBootTest for integration testing
- MockBean for service dependencies
- TestContainers for database integration
- Security testing with mock users
- API endpoint testing with different roles

## Quality Gates
Ensure generated code meets these criteria:
- [ ] All tests pass
- [ ] Minimum 80% code coverage
- [ ] No code smells or violations
- [ ] Follows project coding standards
- [ ] Includes performance tests where applicable
- [ ] Security tests for sensitive operations
- [ ] Documentation is updated
```

## Example TDD Request

```markdown
Implement a StudentApplicationService using TDD approach:

## TDD Context
- **Feature**: Student Application Management
- **Technology**: Spring Boot 3+ with JUnit 5
- **Test Framework**: JUnit 5, Mockito, TestContainers
- **Coverage Target**: 85%+ line coverage

## Phase 1: Write Failing Tests
Generate tests for StudentApplicationService covering:

### Unit Tests
- submitApplication() - successful submission
- submitApplication() - duplicate application handling
- getApplicationsByStudentId() - valid student ID
- getApplicationsByStudentId() - invalid student ID
- updateApplicationStatus() - valid status transitions
- updateApplicationStatus() - invalid status transitions
- validateApplicationData() - complete vs incomplete data

### Integration Tests
- Database persistence operations
- Transaction rollback scenarios
- External service integration (notification service)

## Phase 2: Minimal Implementation
Create StudentApplicationService with:
- Basic CRUD operations
- Simple validation logic
- Error handling for common scenarios
- Repository interaction patterns

## Phase 3: Refactor
Improve implementation with:
- Comprehensive input validation
- Proper exception handling
- Performance optimizations
- Security considerations
- Logging and monitoring
```

## TDD Quality Checklist

- [ ] Tests written before implementation
- [ ] All tests initially fail (Red phase)
- [ ] Minimal implementation makes tests pass (Green phase)  
- [ ] Code refactored for quality (Refactor phase)
- [ ] Tests cover all requirements
- [ ] Edge cases and error scenarios included
- [ ] Integration tests validate end-to-end behavior
- [ ] Code coverage meets or exceeds target
- [ ] Performance tests included where applicable
- [ ] Security tests validate access controls
- [ ] Documentation reflects current implementation
