````markdown
# API Endpoint Generation Template

Use this template when generating RESTful API endpoints with AI assistance.

## Prompt Template

```markdown
Create a RESTful API endpoint following OpenAPI 3.0 specifications:

## API Context

- **Endpoint Path**: [/api/v1/resource-name]
- **HTTP Methods**: [GET, POST, PUT, DELETE]
- **Resource**: [Resource being managed]
- **Authentication**: JWT Bearer token required
- **Authorization**: [Required roles/permissions]

## Requirements

- Implement CRUD operations for [resource]
- Follow RESTful API design principles
- Include proper HTTP status codes
- Implement request/response validation
- Support pagination and filtering
- Include comprehensive error handling

## Technical Specifications

- Use Spring Boot 3+ with Spring Web
- Implement @RestController with proper mappings
- Use @Valid for request validation
- Include @PreAuthorize for security
- Support content negotiation (JSON/XML)
- Implement proper CORS configuration
- Include rate limiting and throttling

## OpenAPI Documentation

- Generate OpenAPI 3.0 specifications
- Include detailed operation descriptions
- Document all request/response schemas
- Provide example requests and responses
- Include authentication requirements
- Document error responses and codes

## Request/Response Design

- Use DTOs for request/response bodies
- Implement proper HTTP status codes:
  - 200 OK for successful GET
  - 201 Created for successful POST
  - 204 No Content for successful DELETE
  - 400 Bad Request for validation errors
  - 401 Unauthorized for authentication failures
  - 403 Forbidden for authorization failures
  - 404 Not Found for missing resources
  - 500 Internal Server Error for system errors

## Security Requirements

- Validate JWT tokens for all endpoints
- Implement role-based access control
- Sanitize all input data
- Include CSRF protection where needed
- Log all API access attempts
- Implement request size limits

## Testing Requirements

- Generate @WebMvcTest tests for endpoints
- Test all HTTP methods and status codes
- Include security testing with different roles
- Test validation scenarios and error cases
- Mock service layer dependencies
- Include integration tests with @SpringBootTest

## File Structure

Generate these files:

- [Resource]Controller.java (main controller)
- [Resource]Request.java (request DTOs)
- [Resource]Response.java (response DTOs)
- [Resource]ControllerTest.java (unit tests)
- [Resource]ControllerIntegrationTest.java (integration tests)

## Code Style

- Follow REST API naming conventions
- Use proper HTTP method semantics
- Include comprehensive JavaDoc
- Apply consistent error handling patterns
- Use Spring Boot best practices

## Example Usage

Provide curl examples for testing the API endpoints.
```

## Example Implementation Request

```markdown
Create RESTful API endpoints for student accommodation management:

## API Context

- **Endpoint Path**: /api/v1/accommodations
- **HTTP Methods**: GET, POST, PUT, DELETE
- **Resource**: Student accommodation properties and rooms
- **Authentication**: JWT Bearer token required
- **Authorization**: STUDENT, PROPERTY_MANAGER, ADMIN roles

## Requirements

- List accommodations with filtering and pagination
- Create new accommodation listings (PROPERTY_MANAGER, ADMIN)
- Update accommodation details (PROPERTY_MANAGER, ADMIN)
- Delete accommodations (ADMIN only)
- Search accommodations by location, price, amenities
- Get accommodation details with availability

## Technical Specifications

- Support query parameters for filtering:
  - location, priceRange, amenities, availability
- Implement pagination with page, size, sort parameters
- Include HATEOAS links for navigation
- Support partial updates with PATCH method
- Implement optimistic locking with version field
- Include caching for frequently accessed data

## OpenAPI Documentation

- Document all query parameters and their formats
- Include examples for common search scenarios
- Document accommodation data model schema
- Provide authentication examples with JWT
- Include error response schemas

## Request/Response Design

- AccommodationRequest DTO with validation annotations
- AccommodationResponse DTO with HATEOAS links
- PagedAccommodationResponse for list endpoints
- ErrorResponse DTO for consistent error format
- Include metadata in responses (version, timestamp)

## Security Requirements

- Students can only view accommodations
- Property managers can manage their own properties
- Admins have full access to all accommodations
- Implement input validation and sanitization
- Log all modification operations with user context

## Testing Requirements

- Test all CRUD operations with different user roles
- Test search and filtering functionality
- Test pagination edge cases
- Test validation scenarios with invalid data
- Test security access controls
- Performance tests for search operations
```

## Quality Checklist

Before submitting AI-generated API code:

- [ ] Follows RESTful API design principles
- [ ] Uses proper HTTP status codes and methods
- [ ] Includes comprehensive input validation
- [ ] Implements proper authentication and authorization
- [ ] Has detailed OpenAPI documentation
- [ ] Includes proper error handling and responses
- [ ] Uses DTOs for request/response mapping
- [ ] Has comprehensive test coverage
- [ ] Follows security best practices
- [ ] Implements proper logging and monitoring
- [ ] Includes performance optimizations
- [ ] Code is properly documented

## REST API Best Practices

- **Resource Naming**: Use nouns, not verbs in URLs
- **HTTP Methods**: Use appropriate HTTP methods for operations
- **Status Codes**: Return meaningful HTTP status codes
- **Versioning**: Include API version in URL or headers
- **Pagination**: Implement cursor-based or offset-based pagination
- **Filtering**: Support query parameters for filtering data
- **Sorting**: Allow multiple sort criteria
- **HATEOAS**: Include hypermedia links for API navigation
- **Rate Limiting**: Implement request throttling
- **Caching**: Use appropriate HTTP caching headers
````
