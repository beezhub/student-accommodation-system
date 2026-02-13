---
applyTo: '**'
---
# Student Accommodation System - AI-Driven Development Instructions

Create an enterprise-grade, cloud-native full-stack web application using Angular (frontend) and Spring Boot (backend) for managing student accommodation with modern development practices and AI-assisted development workflow.

## Core Features:
- Student registration and authentication system with SSO support
- Room/accommodation listing and search functionality with intelligent matching
- Booking and reservation management with real-time availability
- Payment processing integration with multiple providers
- Administrative dashboard for property management with analytics
- Document management system with AI-powered verification
- Communication system between students and property managers
- Multi-tenant support for multiple institutions

## Architecture Principles:

### Single Responsibility Pattern (SRP):
- Each component serves a single, well-defined purpose
- Services handle specific business logic domains
- Modules are organized by feature boundaries
- Utilities and helpers are isolated and reusable
- Clear separation between presentation, business, and data layers

### Cloud-Native & Modular Design:
- Microservices architecture with API gateway
- Container-first development with Docker
- Kubernetes-ready deployment manifests
- Environment-specific configuration management
- Health checks and observability built-in
- Horizontal scaling capabilities

## Technical Requirements:

### Frontend (Angular 18+):
- **Angular Material with Apple-Inspired Design System**:
  - Clean, minimalist UI following Apple's design principles
  - Consistent typography using SF Pro or similar system fonts
  - Apple-style navigation patterns and interactions
  - Smooth animations and micro-interactions
  - Accessibility-first component design (WCAG 2.1 AA)
  
- **Architecture & Patterns**:
  - Strict adherence to Single Responsibility Principle
  - Lazy-loaded feature modules with clear boundaries
  - Standalone components for better tree-shaking
  - NgRx for centralized state management
  - RxJS for reactive programming patterns
  - Smart/Dumb component architecture
  
- **Development Standards**:
  - TypeScript with strict mode enabled
  - ESLint with Angular-specific rules
  - Prettier for consistent code formatting
  - Husky for pre-commit hooks
  - Conventional commits for change tracking
  
- **Testing Strategy**:
  - Test-Driven Development (TDD) approach
  - Unit tests with Jasmine/Karma (80%+ coverage)
  - Component testing with Angular Testing Library
  - E2E testing with Cypress
  - Visual regression testing with Chromatic
  - Accessibility testing with axe-core

### Backend (Spring Boot 3+):
- **Microservices Architecture**:
  - Domain-driven design with bounded contexts
  - Single responsibility per service
  - API-first development with OpenAPI specifications
  - Event-driven communication between services
  - Circuit breaker pattern for resilience
  
- **Cloud-Native Features**:
  - Spring Cloud Gateway for API routing
  - Spring Cloud Config for centralized configuration
  - Service discovery with Eureka or Consul
  - Distributed tracing with Zipkin/Jaeger
  - Metrics and monitoring with Micrometer/Prometheus
  
- **Data Management**:
  - PostgreSQL for transactional data
  - Redis for caching and session management
  - MongoDB for document storage (optional)
  - Flyway for database migrations
  - CQRS pattern for read/write separation

### Open-Source Technology Stack:

#### Frontend Stack:
- **Angular Material**: UI component library
- **Tabler Icons**: Comprehensive icon library
- **RxJS**: Reactive programming
- **NgRx**: State management
- **Angular CDK**: Component development kit
- **Cypress**: E2E testing
- **Jest**: Unit testing (alternative to Karma)

#### Backend Stack:
- **Spring Boot**: Application framework
- **Spring Security**: Authentication/authorization
- **Spring Data JPA**: Data access layer
- **PostgreSQL**: Primary database
- **Redis**: Caching layer
- **RabbitMQ**: Message broker
- **MinIO**: Object storage
- **Testcontainers**: Integration testing

#### DevOps & Infrastructure:
- **Docker**: Containerization
- **Kubernetes**: Container orchestration
- **Helm**: Kubernetes package manager
- **ArgoCD**: GitOps deployment
- **Prometheus**: Monitoring
- **Grafana**: Visualization
- **ELK Stack**: Logging (Elasticsearch, Logstash, Kibana)

## AI-Driven Development Workflow:

### Prompt Engineering Standards:
- **Structured Prompt Templates**: Standardized formats for common development tasks
- **Context-Aware Prompts**: Include relevant code context and requirements
- **Version-Controlled Prompts**: Track prompt evolution and effectiveness
- **Team Prompt Library**: Shared repository of validated prompts
- **Prompt Quality Metrics**: Measure and improve prompt effectiveness

### AI Development Guidelines:
- **Test-First AI Generation**: Write tests before generating implementation
- **Code Review for AI Output**: Mandatory review of all AI-generated code
- **Consistency Checks**: Automated validation of code style and patterns
- **Documentation Generation**: AI-assisted documentation creation
- **Refactoring Assistance**: AI-powered code improvement suggestions

### Team Collaboration:
- **Standardized AI Tools**: Consistent AI assistants across team
- **Prompt Peer Review**: Review and validate prompts before use
- **AI-Generated Code Tracking**: Label and track AI contributions
- **Knowledge Sharing**: Regular sharing of effective prompt patterns
- **Continuous Learning**: Regular updates to AI development practices

## Development Environment Setup:

### Required Tools:
```bash
# Frontend
npm install -g @angular/cli@latest
npm install -g @angular/cdk@latest

# Backend
sdk install java 21.0.1-tem
sdk install gradle 8.5

# DevOps
docker --version
kubectl version --client
helm version
```

### Project Structure:
```
student-accommodation-system/
├── apps/
│   ├── frontend/                 # Angular application
│   ├── api-gateway/             # Spring Cloud Gateway
│   ├── student-service/         # Student management service
│   ├── accommodation-service/   # Room management service
│   └── notification-service/    # Communication service
├── libs/
│   ├── ui-components/          # Shared Angular components
│   ├── data-access/           # Shared data services
│   └── utils/                 # Shared utilities
├── tools/
│   ├── ai-prompts/           # Team prompt library
│   ├── scripts/              # Build and deployment scripts
│   └── k8s/                  # Kubernetes manifests
└── docs/
    ├── architecture/         # Architecture decision records
    ├── api/                  # API documentation
    └── prompts/              # AI prompt documentation
```

## Quality Assurance Standards:

### Code Quality:
- **SonarQube**: Static code analysis
- **ESLint/TSLint**: JavaScript/TypeScript linting
- **Prettier**: Code formatting
- **Husky**: Git hooks for quality checks
- **Conventional Commits**: Standardized commit messages

### Testing Requirements:
- **Unit Tests**: 80% minimum coverage
- **Integration Tests**: Critical user journeys
- **E2E Tests**: Complete user workflows
- **Performance Tests**: Load and stress testing
- **Security Tests**: OWASP compliance scanning

### Documentation Standards:
- **Architecture Decision Records (ADRs)**: Document key decisions
- **API Documentation**: OpenAPI/Swagger specifications
- **Component Documentation**: Storybook for UI components
- **README Files**: Clear setup and usage instructions
- **Prompt Documentation**: Document AI development patterns

## Architecture:
- Implement a clean architecture with proper separation of concerns
- Use DTOs for API communication
- Implement service layer pattern in Spring Boot
- Use repository pattern for data access
- Follow RESTful API design principles
- Implement proper CORS configuration for cross-origin requests

## Security:
- Implement proper password hashing (BCrypt)
- JWT token management with refresh tokens
- Role-based access control (RBAC)
- Input sanitization and validation
- Protection against common vulnerabilities (XSS, CSRF, SQL Injection)

Please provide a scalable solution that follows enterprise-level practices and includes proper monitoring and observability features.
