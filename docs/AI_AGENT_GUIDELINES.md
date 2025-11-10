# AI Agent Guidelines - Quick Reference

This file serves as the primary reference for all AI agents working on the Student Accommodation System. It consolidates all guidelines, patterns, and requirements into a single, easily accessible document.

## 🎯 Project Overview

**Student Accommodation System** - Enterprise Angular + Spring Boot application for managing student housing with AI-driven development workflow.

## 🏗️ Architecture Principles

### Single Responsibility Principle (SRP)

- Each component serves ONE specific purpose
- Services handle single business domain
- Clear separation: Presentation → Business → Data
- Modules organized by feature boundaries

### Cloud-Native Design

- Container-first development (Docker)
- Microservices architecture
- Kubernetes-ready deployment
- Health checks and observability
- Horizontal scaling support

## 🎨 UI/UX Guidelines

### Apple-Inspired Design System

- **Clean, minimalist interfaces**
- **System fonts**: SF Pro, Helvetica Neue fallbacks
- **8pt grid system** for consistent spacing
- **Smooth animations** with Apple-style easing
- **WCAG 2.1 AA** accessibility compliance

### Angular Material Implementation

```typescript
// Always use Angular Material components as base
// Customize with Apple-inspired theming
// Example structure:
@Component({
  selector: 'app-feature-component',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush
})
```

## 🧪 Development Standards

### Test-Driven Development (TDD)

1. **RED**: Write failing tests first
2. **GREEN**: Implement minimal code to pass
3. **REFACTOR**: Improve while maintaining tests

### Code Quality Requirements

- **80%+ test coverage** minimum
- **TypeScript strict mode** enabled
- **ESLint + Prettier** formatting
- **Conventional commits** for version control

## 🚀 Technology Stack

### Frontend (Angular 18+)

```json
{
  "core": ["@angular/core", "@angular/material", "@angular/cdk"],
  "state": ["@ngrx/store", "@ngrx/effects"],
  "testing": ["jasmine", "karma", "cypress"],
  "utilities": ["rxjs", "lodash-es"]
}
```

### Backend (Spring Boot 3+)

```xml
<dependencies>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
  <artifactId>spring-boot-starter-security</artifactId>
  <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependencies>
```

## 📋 Mandatory Checklist for All Code Generation

### Before Writing Code

- [ ] Understand the Single Responsibility of the component/service
- [ ] Identify dependencies and interfaces needed
- [ ] Plan test scenarios (happy path + edge cases)
- [ ] Consider accessibility requirements
- [ ] Check existing patterns in codebase

### Component Generation Checklist

- [ ] Use standalone component architecture
- [ ] Implement OnPush change detection
- [ ] Include proper TypeScript typing
- [ ] Add Angular Material components with Apple styling
- [ ] Write comprehensive unit tests
- [ ] Include accessibility attributes (ARIA labels)
- [ ] Support responsive design (mobile-first)
- [ ] Handle loading and error states

### Service Generation Checklist

- [ ] Single responsibility focus
- [ ] Proper error handling with custom exceptions
- [ ] RxJS for reactive patterns
- [ ] Injectable with providedIn: 'root' or module
- [ ] Comprehensive unit tests with mocks
- [ ] Integration tests for external dependencies
- [ ] Proper logging and monitoring

### API Generation Checklist (Spring Boot)

- [ ] RESTful endpoint design
- [ ] OpenAPI/Swagger documentation
- [ ] Input validation with Bean Validation
- [ ] Proper HTTP status codes
- [ ] Exception handling with @ControllerAdvice
- [ ] Security annotations (@PreAuthorize)
- [ ] Integration tests with @SpringBootTest

## 🔄 AI Development Workflow

### Step 1: Analyze Requirements

```markdown
1. Read user story/requirement carefully
2. Identify which layer (UI/Service/Data) is affected
3. Determine single responsibility boundary
4. Check existing similar implementations
5. Plan test scenarios
```

### Step 2: Generate Tests First

```markdown
1. Use TDD template from tools/ai-prompts/templates/test-generation.md
2. Cover happy path, edge cases, and error scenarios
3. Include accessibility tests for UI components
4. Ensure tests initially fail (RED phase)
```

### Step 3: Implement Minimal Solution

```markdown
1. Generate code that makes tests pass (GREEN phase)
2. Follow established patterns in existing codebase
3. Include proper error handling and logging
4. Use TypeScript strict typing
```

### Step 4: Refactor and Optimize

```markdown
1. Improve code quality while maintaining tests
2. Apply SOLID principles
3. Optimize performance where needed
4. Ensure consistent code style
```

## 🎨 UI Component Patterns

### Standard Component Structure

```typescript
@Component({
  selector: "app-feature-name",
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule],
  template: `
    <mat-card class="apple-card">
      <mat-card-header>
        <mat-card-title>{{ title }}</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <!-- Content here -->
      </mat-card-content>
      <mat-card-actions align="end">
        <button mat-raised-button color="primary">Action</button>
      </mat-card-actions>
    </mat-card>
  `,
  styleUrls: ["./component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeatureComponent implements OnInit {
  // Component logic here
}
```

### Apple-Inspired CSS Classes

```scss
.apple-card {
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12), 0 1px 2px rgba(0, 0, 0, 0.24);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.apple-button {
  border-radius: 8px;
  font-weight: 500;
  letter-spacing: 0.25px;
}
```

## 🔐 Security Guidelines

### Authentication & Authorization

- Use JWT tokens with proper expiration
- Implement role-based access control (RBAC)
- Validate all inputs on both client and server
- Use HTTPS for all communications
- Implement CSRF protection

### Data Protection

- Encrypt sensitive data at rest
- Use parameterized queries to prevent SQL injection
- Implement rate limiting for APIs
- Log security events for monitoring
- Follow GDPR compliance for personal data

## 📊 Performance Guidelines

### Frontend Performance

- Lazy load feature modules
- Use OnPush change detection
- Implement virtual scrolling for large lists
- Optimize images and assets
- Use service workers for caching

### Backend Performance

- Use database connection pooling
- Implement caching with Redis
- Use pagination for large datasets
- Optimize database queries
- Implement circuit breakers

## 🐛 Error Handling Patterns

### Frontend Error Handling

```typescript
// Use RxJS catchError operator
this.service.getData().pipe(
  catchError((error) => {
    this.notificationService.showError(error.message);
    return throwError(() => error);
  })
);
```

### Backend Error Handling

```java
@ControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
    return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
  }
}
```

## 📝 Documentation Requirements

### Code Documentation

- JSDoc/Javadoc for all public methods
- README files for each module
- Architecture Decision Records (ADRs) for major decisions
- API documentation with OpenAPI/Swagger

### Comments in Code

- Explain WHY, not WHAT the code does
- Document complex business logic
- Include TODO comments for future improvements
- Reference related tickets/issues where applicable

## 🔍 Code Review Checklist

### Functionality

- [ ] Code meets requirements exactly
- [ ] Edge cases are handled properly
- [ ] Error scenarios are addressed
- [ ] Performance considerations are met

### Quality

- [ ] Follows Single Responsibility Principle
- [ ] Uses established patterns and conventions
- [ ] Has comprehensive test coverage
- [ ] Includes proper error handling
- [ ] Documentation is updated

### Security

- [ ] Input validation is implemented
- [ ] Authorization checks are in place
- [ ] Sensitive data is protected
- [ ] No hardcoded secrets or credentials

## 🏃‍♂️ Quick Start Commands

### Development Setup

```bash
# Frontend
cd frontend && npm install
ng serve --open

# Backend
cd backend && ./gradlew bootRun

# Docker
docker-compose up -d
```

### Testing Commands

```bash
# Frontend tests
npm test
npm run e2e

# Backend tests
./gradlew test
./gradlew integrationTest
```

## 📞 When in Doubt

1. **Check existing patterns** in the codebase first
2. **Refer to ADRs** in docs/architecture/
3. **Use prompt templates** from tools/ai-prompts/
4. **Follow TDD workflow** - tests first, always
5. **Ask for clarification** rather than making assumptions

## 🎯 Success Criteria

Every piece of generated code should:

- ✅ Serve a single, clear responsibility
- ✅ Have 80%+ test coverage
- ✅ Follow Apple-inspired design principles
- ✅ Be accessible (WCAG 2.1 AA)
- ✅ Handle errors gracefully
- ✅ Include proper documentation
- ✅ Pass all quality gates (ESLint, SonarQube)
- ✅ Be container/cloud-ready

---

**Remember**: Quality over speed. Always prioritize maintainable, testable, accessible code that follows our architectural principles.
