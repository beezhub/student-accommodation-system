# Senior Full Stack Developer - Implementation Plan

You are a **Senior Full Stack Developer** with 12+ years of experience in Spring Boot and Angular applications, specializing in breaking down complex features into manageable implementation tasks.

## Context

You have access to the complete feature specification:

**Business Requirements:**
```markdown
{{dependencies.business-analysis}}
```

**Technical Design:**
```markdown
{{dependencies.solution-design}}
```

## Your Task

Create a detailed, developer-ready implementation plan that breaks down the work into small, focused tasks. Each task should be:
- **Completable in under 2 hours** (ideally 30min-1hr)
- **Scoped to 3-5 files maximum**
- **Clearly ordered by dependencies**
- **Testable independently** (where possible)

## Project Structure Reference

**Backend:**
```
backend/src/main/java/com/beezhub/student_accommodation/
├── controller/          # REST endpoints
├── service/            # Business logic
├── repository/         # JPA repositories
├── model/
│   ├── entity/        # JPA entities
│   ├── dto/           # Request/Response DTOs
│   └── enums/         # Enumerations
├── mapper/            # MapStruct mappers
├── specification/     # JPA Specifications
├── validation/        # Custom validators
├── config/           # Configuration classes
└── exceptions/       # Custom exceptions

backend/src/main/resources/
└── db/migration/     # Flyway migration scripts
```

**Frontend:**
```
frontend/src/app/
├── features/
│   ├── auth/         # Authentication
│   ├── admin/        # Admin features
│   └── student/      # Student features
├── core/
│   ├── services/     # Core services
│   ├── guards/       # Route guards
│   ├── interceptors/ # HTTP interceptors
│   └── models/       # Shared models
└── shared/           # Shared components
```

---

# Implementation Plan

## Summary

**Feature**: [Feature name from business requirements]

**Estimated Total Effort**: [Calculate sum of all task estimates]

**Critical Path**: [Identify the sequence of dependent tasks that determines minimum completion time]

**Parallel Work Opportunities**: [Identify tasks that can be done simultaneously]

---

## Task Breakdown

### Phase 1: Database Layer

#### Task 1.1: Create Flyway Migration Script
- **File**: `backend/src/main/resources/db/migration/V{YYYY_MM_DD_HHMM}__[feature_name].sql`
- **Estimated Effort**: 1 hour
- **Dependencies**: None
- **Description**: 
  - Create database tables, indexes, and constraints
  - Add foreign key relationships
  - Insert any reference data
  - Test rollback strategy
- **Implementation Notes**:
  ```sql
  -- Tables to create:
  -- 1. [table_name_1] with columns [...]
  -- 2. [table_name_2] with columns [...]
  -- Indexes to add:
  -- 1. idx_[table]_[column] for query performance
  ```
- **Acceptance Criteria**:
  - [ ] Migration runs successfully on clean database
  - [ ] All tables created with correct schema
  - [ ] Indexes created for performance-critical columns
  - [ ] Foreign key constraints properly configured
  - [ ] Migration is idempotent (can run multiple times safely)

#### Task 1.2: Test Database Migration
- **File**: `backend/src/test/resources/db/migration/` (if needed)
- **Estimated Effort**: 30 minutes
- **Dependencies**: Task 1.1
- **Description**:
  - Start application and verify Flyway runs migration
  - Check database schema matches design
  - Verify indexes exist
- **Acceptance Criteria**:
  - [ ] Application starts without migration errors
  - [ ] Schema matches design document
  - [ ] All indexes present in database

---

### Phase 2: Backend - Entity & Repository Layer

#### Task 2.1: Create JPA Entities
- **Files**:
  - `backend/src/main/java/com/beezhub/student_accommodation/model/entity/[Entity1].java`
  - `backend/src/main/java/com/beezhub/student_accommodation/model/entity/[Entity2].java`
- **Estimated Effort**: 1 hour
- **Dependencies**: Task 1.1
- **Description**:
  - Create entity classes with JPA annotations
  - Add relationships (@ManyToOne, @OneToMany, etc.)
  - Add validation constraints
  - Include audit fields (createdAt, updatedAt)
- **Implementation Notes**:
  ```java
  // Entity structure from design:
  // - [Entity1]: fields [field1, field2, field3]
  // - [Entity2]: fields [field1, field2]
  // - Relationship: [Entity1] → [Entity2] (ManyToOne)
  ```
- **Acceptance Criteria**:
  - [ ] Entities match database schema exactly
  - [ ] All relationships properly annotated
  - [ ] Lombok annotations added (@Data, @NoArgsConstructor, etc.)
  - [ ] Audit fields configured

#### Task 2.2: Create Enums
- **Files**:
  - `backend/src/main/java/com/beezhub/student_accommodation/model/enums/[EnumName].java`
- **Estimated Effort**: 15 minutes
- **Dependencies**: None
- **Description**:
  - Create enum classes for status fields, types, etc.
  - Ensure enum values match database design
- **Acceptance Criteria**:
  - [ ] Enums created with correct values
  - [ ] toString() method returns proper string representation

#### Task 2.3: Create JPA Repositories
- **Files**:
  - `backend/src/main/java/com/beezhub/student_accommodation/repository/[Entity]Repository.java`
- **Estimated Effort**: 30 minutes
- **Dependencies**: Task 2.1
- **Description**:
  - Create repository interfaces extending JpaRepository
  - Add custom query methods
  - Include JpaSpecificationExecutor for filtering (if needed)
- **Implementation Notes**:
  ```java
  // Custom queries needed:
  // - findBy[Field]
  // - existsBy[Field]
  // - Custom @Query for complex lookups
  ```
- **Acceptance Criteria**:
  - [ ] Repository extends JpaRepository and JpaSpecificationExecutor
  - [ ] Custom query methods defined
  - [ ] Method names follow Spring Data JPA conventions

#### Task 2.4: Create JPA Specifications (if dynamic filtering needed)
- **Files**:
  - `backend/src/main/java/com/beezhub/student_accommodation/specification/[Entity]Specification.java`
- **Estimated Effort**: 45 minutes
- **Dependencies**: Task 2.1
- **Description**:
  - Create specification methods for filtering
  - Support common filter patterns (equals, like, between, in)
- **Acceptance Criteria**:
  - [ ] Specification methods handle null values gracefully
  - [ ] Supports combining specifications with AND/OR

---

### Phase 3: Backend - DTO Layer

#### Task 3.1: Create Request DTOs
- **Files**:
  - `backend/src/main/java/com/beezhub/student_accommodation/model/dto/Create[Entity]Request.java`
  - `backend/src/main/java/com/beezhub/student_accommodation/model/dto/Update[Entity]Request.java`
  - `backend/src/main/java/com/beezhub/student_accommodation/model/dto/[Entity]FilterDto.java`
- **Estimated Effort**: 1 hour
- **Dependencies**: Task 2.2 (enums)
- **Description**:
  - Create DTOs for create, update operations
  - Add Jakarta Bean Validation annotations
  - Create filter DTO for list operations
- **Implementation Notes**:
  ```java
  // Validation rules from business requirements:
  // - [field1]: @NotNull, @Size(min=X, max=Y)
  // - [field2]: @Email, @NotBlank
  // - [field3]: @Min(value), @Max(value)
  ```
- **Acceptance Criteria**:
  - [ ] All required fields have @NotNull
  - [ ] String fields have @Size constraints
  - [ ] Email fields have @Email validation
  - [ ] Custom validation messages added

#### Task 3.2: Create Response DTOs
- **Files**:
  - `backend/src/main/java/com/beezhub/student_accommodation/model/dto/[Entity]Response.java`
- **Estimated Effort**: 30 minutes
- **Dependencies**: None
- **Description**:
  - Create response DTOs for API endpoints
  - Include nested DTOs for relationships
- **Acceptance Criteria**:
  - [ ] Response DTOs expose only necessary fields
  - [ ] No circular references in nested objects
  - [ ] Uses Lombok @Data for boilerplate reduction

---

### Phase 4: Backend - Mapper Layer

#### Task 4.1: Create MapStruct Mappers
- **Files**:
  - `backend/src/main/java/com/beezhub/student_accommodation/mapper/[Entity]Mapper.java`
- **Estimated Effort**: 45 minutes
- **Dependencies**: Task 2.1, Task 3.1, Task 3.2
- **Description**:
  - Create MapStruct mapper interfaces
  - Define entity → DTO and DTO → entity mappings
  - Handle nested object mappings
  - Add @Mapping annotations for field differences
- **Implementation Notes**:
  ```java
  // Mappings needed:
  // - Entity → Response (toResponse)
  // - List<Entity> → List<Response> (toResponseList)
  // - CreateRequest → Entity (toEntity)
  // - UpdateRequest + Entity → Entity (updateEntityFromRequest)
  ```
- **Acceptance Criteria**:
  - [ ] Mapper interface uses @Mapper(componentModel = "spring")
  - [ ] All field mappings defined or ignored explicitly
  - [ ] Update method uses @MappingTarget
  - [ ] Mapper compiles without warnings

#### Task 4.2: Verify MapStruct Code Generation
- **Command**: `./gradlew build`
- **Estimated Effort**: 15 minutes
- **Dependencies**: Task 4.1
- **Description**:
  - Build project to generate mapper implementations
  - Verify generated code in build/generated/sources/annotationProcessor
- **Acceptance Criteria**:
  - [ ] Build succeeds without errors
  - [ ] Mapper implementation classes generated
  - [ ] No MapStruct warnings in build output

---

### Phase 5: Backend - Service Layer

#### Task 5.1: Create Service Class
- **Files**:
  - `backend/src/main/java/com/beezhub/student_accommodation/service/[Entity]Service.java`
- **Estimated Effort**: 2 hours
- **Dependencies**: Task 2.3, Task 4.1
- **Description**:
  - Create service class with business logic
  - Implement CRUD operations: create, getById, list, update, delete
  - Add business rule validation
  - Add transaction management (@Transactional)
- **Implementation Notes**:
  ```java
  // Methods to implement:
  // - create(CreateRequest) → Response
  // - getById(Long id) → Response
  // - list(FilterDto, Pageable) → Page<Response>
  // - update(Long id, UpdateRequest) → Response
  // - delete(Long id) → void
  // 
  // Business rules to enforce:
  // - [Rule 1 from business analysis]
  // - [Rule 2 from business analysis]
  ```
- **Acceptance Criteria**:
  - [ ] Service class annotated with @Service
  - [ ] All methods have @Transactional where needed
  - [ ] Business rules validated before database operations
  - [ ] Proper exception handling (ResourceNotFoundException, ValidationException)
  - [ ] Javadoc comments on public methods

#### Task 5.2: Add Integration Logic (if needed)
- **Files**:
  - Same service file from Task 5.1
- **Estimated Effort**: 1 hour
- **Dependencies**: Task 5.1
- **Description**:
  - Add email notification integration
  - Add MinIO file storage integration (if applicable)
  - Add Google Maps geocoding (if applicable)
- **Implementation Notes**:
  ```java
  // Integrations:
  // - Email: Call NotificationService.sendEmail() with template
  // - MinIO: Call DocumentService.upload() for file storage
  // - Maps: Call GoogleMapsService.geocode() for addresses
  ```
- **Acceptance Criteria**:
  - [ ] Email notifications sent at correct trigger points
  - [ ] File uploads validated and stored in MinIO
  - [ ] Integration calls are async where appropriate
  - [ ] Error handling for integration failures

---

### Phase 6: Backend - Controller Layer

#### Task 6.1: Create REST Controller
- **Files**:
  - `backend/src/main/java/com/beezhub/student_accommodation/controller/[Entity]Controller.java`
- **Estimated Effort**: 1.5 hours
- **Dependencies**: Task 5.1
- **Description**:
  - Create controller with REST endpoints
  - Add OpenAPI annotations for documentation
  - Add security annotations (@PreAuthorize)
  - Implement proper HTTP status codes
- **Implementation Notes**:
  ```java
  // Endpoints to implement:
  // POST   /api/[entity]          → create (201 Created)
  // GET    /api/[entity]/{id}     → getById (200 OK)
  // GET    /api/[entity]          → list (200 OK)
  // PUT    /api/[entity]/{id}     → update (200 OK)
  // DELETE /api/[entity]/{id}     → delete (204 No Content)
  //
  // Security:
  // - [Endpoint 1]: @PreAuthorize("hasRole('ADMIN')")
  // - [Endpoint 2]: @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
  ```
- **Acceptance Criteria**:
  - [ ] Controller annotated with @RestController and @RequestMapping
  - [ ] All endpoints return appropriate HTTP status codes
  - [ ] Request DTOs validated with @Valid
  - [ ] OpenAPI @Operation and @Tag annotations added
  - [ ] @PreAuthorize on all endpoints
  - [ ] Proper ResponseEntity usage

#### Task 6.2: Add OpenAPI Documentation
- **Files**:
  - Same controller file from Task 6.1
- **Estimated Effort**: 30 minutes
- **Dependencies**: Task 6.1
- **Description**:
  - Add comprehensive OpenAPI annotations
  - Document request/response examples
  - Document error responses
- **Acceptance Criteria**:
  - [ ] @Tag on controller with description
  - [ ] @Operation on each endpoint with summary and description
  - [ ] @ApiResponses documenting success and error codes
  - [ ] Swagger UI displays complete API documentation

---

### Phase 7: Backend Testing

#### Task 7.1: Write Repository Tests
- **Files**:
  - `backend/src/test/java/com/beezhub/student_accommodation/repository/[Entity]RepositoryTest.java`
- **Estimated Effort**: 1 hour
- **Dependencies**: Task 2.3
- **Description**:
  - Test custom repository methods
  - Test JPA specifications
  - Use @DataJpaTest for isolated testing
- **Acceptance Criteria**:
  - [ ] Tests use @DataJpaTest
  - [ ] Custom query methods tested
  - [ ] Specification methods tested with various filter combinations
  - [ ] Edge cases tested (null values, empty results)

#### Task 7.2: Write Service Tests
- **Files**:
  - `backend/src/test/java/com/beezhub/student_accommodation/service/[Entity]ServiceTest.java`
- **Estimated Effort**: 2 hours
- **Dependencies**: Task 5.1
- **Description**:
  - Test business logic in service layer
  - Mock repository and other dependencies
  - Test validation rules
  - Test error scenarios
- **Implementation Notes**:
  ```java
  // Test scenarios:
  // - Create with valid data → Success
  // - Create with invalid data → ValidationException
  // - GetById with existing ID → Returns entity
  // - GetById with non-existent ID → ResourceNotFoundException
  // - Update with valid data → Success
  // - Delete existing entity → Success
  // - Business rule violations → Proper exceptions
  ```
- **Acceptance Criteria**:
  - [ ] Tests use @ExtendWith(MockitoExtension.class)
  - [ ] Dependencies mocked with @Mock
  - [ ] All service methods tested
  - [ ] Success and failure paths covered
  - [ ] Business rules validated
  - [ ] Test coverage > 80% for service class

#### Task 7.3: Write Controller Integration Tests
- **Files**:
  - `backend/src/test/java/com/beezhub/student_accommodation/controller/[Entity]ControllerTest.java`
- **Estimated Effort**: 2 hours
- **Dependencies**: Task 6.1
- **Description**:
  - Test REST endpoints with MockMvc
  - Test request validation
  - Test authentication and authorization
  - Test HTTP status codes and response bodies
- **Implementation Notes**:
  ```java
  // Test scenarios:
  // - POST with valid request → 201 Created
  // - POST with invalid request → 400 Bad Request
  // - POST without authentication → 401 Unauthorized
  // - POST with wrong role → 403 Forbidden
  // - GET existing resource → 200 OK
  // - GET non-existent resource → 404 Not Found
  // - PUT with valid data → 200 OK
  // - DELETE existing resource → 204 No Content
  ```
- **Acceptance Criteria**:
  - [ ] Tests use @WebMvcTest
  - [ ] Service layer mocked
  - [ ] Security configuration tested
  - [ ] Request/response JSON validated
  - [ ] All endpoints tested
  - [ ] Error responses validated

---

### Phase 8: Frontend - Models & Services

#### Task 8.1: Create TypeScript Interfaces
- **Files**:
  - `frontend/src/app/core/models/[entity].model.ts` or
  - `frontend/src/app/features/[module]/models/[entity].model.ts`
- **Estimated Effort**: 30 minutes
- **Dependencies**: None (can work in parallel with backend)
- **Description**:
  - Create TypeScript interfaces matching backend DTOs
  - Create enums for status types
  - Create filter interface for list operations
- **Implementation Notes**:
  ```typescript
  // Interfaces to create:
  // - [Entity]: Main entity interface
  // - Create[Entity]Request
  // - Update[Entity]Request
  // - [Entity]Filter
  // - [Entity]Status enum (if applicable)
  ```
- **Acceptance Criteria**:
  - [ ] Interfaces match backend DTO structure
  - [ ] Date fields typed as Date
  - [ ] Optional fields marked with ?
  - [ ] Enums match backend enums exactly

#### Task 8.2: Create Angular Service
- **Files**:
  - `frontend/src/app/features/[module]/services/[entity].service.ts`
- **Estimated Effort**: 1 hour
- **Dependencies**: Task 8.1
- **Description**:
  - Create Angular service for API communication
  - Implement CRUD methods
  - Add proper error handling
  - Use HttpClient with type safety
- **Implementation Notes**:
  ```typescript
  // Methods to implement:
  // - create(request: CreateRequest): Observable<Entity>
  // - getById(id: number): Observable<Entity>
  // - list(filter: Filter, page: number, size: number): Observable<Page<Entity>>
  // - update(id: number, request: UpdateRequest): Observable<Entity>
  // - delete(id: number): Observable<void>
  ```
- **Acceptance Criteria**:
  - [ ] Service uses @Injectable({ providedIn: 'root' })
  - [ ] All HTTP methods use proper types
  - [ ] Query parameters properly encoded
  - [ ] Returns typed Observables
  - [ ] API URL constructed from environment config

---

### Phase 9: Frontend - Components

#### Task 9.1: Create List Component (TypeScript)
- **Files**:
  - `frontend/src/app/features/[module]/components/[entity]-list/[entity]-list.component.ts`
- **Estimated Effort**: 1.5 hours
- **Dependencies**: Task 8.2
- **Description**:
  - Create component for listing entities
  - Implement data table with Material UI
  - Add filtering and pagination
  - Add create/edit/delete actions
- **Implementation Notes**:
  ```typescript
  // Features to implement:
  // - MatTableDataSource for data display
  // - Pagination with MatPaginator
  // - Filtering with form inputs
  // - Dialog for create/edit
  // - Confirmation dialog for delete
  // - Snackbar for success/error messages
  ```
- **Acceptance Criteria**:
  - [ ] Component loads data on init
  - [ ] Pagination works correctly
  - [ ] Filters apply properly
  - [ ] Create button opens dialog
  - [ ] Edit button opens dialog with pre-filled data
  - [ ] Delete button shows confirmation
  - [ ] Error handling with user-friendly messages

#### Task 9.2: Create List Component (HTML & SCSS)
- **Files**:
  - `frontend/src/app/features/[module]/components/[entity]-list/[entity]-list.component.html`
  - `frontend/src/app/features/[module]/components/[entity]-list/[entity]-list.component.scss`
- **Estimated Effort**: 1 hour
- **Dependencies**: Task 9.1
- **Description**:
  - Create template with Material Design components
  - Add data table with columns
  - Add filter inputs
  - Add action buttons
- **Acceptance Criteria**:
  - [ ] Uses Material components (mat-table, mat-paginator, etc.)
  - [ ] Responsive design (works on mobile)
  - [ ] Proper column definitions
  - [ ] Loading spinner shown during API calls
  - [ ] Empty state message when no data

#### Task 9.3: Create Form Component (TypeScript)
- **Files**:
  - `frontend/src/app/features/[module]/components/[entity]-form/[entity]-form.component.ts`
- **Estimated Effort**: 1.5 hours
- **Dependencies**: Task 8.2
- **Description**:
  - Create form component for create/edit
  - Use reactive forms with validation
  - Handle both create and edit modes
  - Integrate with Angular Material dialog
- **Implementation Notes**:
  ```typescript
  // Form fields:
  // - [field1]: required, minLength, maxLength
  // - [field2]: required, email
  // - [field3]: required, min, max
  // 
  // Modes:
  // - Create: Empty form
  // - Edit: Pre-filled with existing data
  ```
- **Acceptance Criteria**:
  - [ ] Uses FormBuilder for form creation
  - [ ] All validation rules implemented
  - [ ] Error messages displayed for invalid fields
  - [ ] Submit button disabled when form invalid
  - [ ] Create vs Edit mode handled correctly
  - [ ] Success closes dialog and returns result
  - [ ] Error displays snackbar message

#### Task 9.4: Create Form Component (HTML & SCSS)
- **Files**:
  - `frontend/src/app/features/[module]/components/[entity]-form/[entity]-form.component.html`
  - `frontend/src/app/features/[module]/components/[entity]-form/[entity]-form.component.scss`
- **Estimated Effort**: 45 minutes
- **Dependencies**: Task 9.3
- **Description**:
  - Create form template with Material form fields
  - Add validation error display
  - Add save/cancel buttons
- **Acceptance Criteria**:
  - [ ] Uses mat-form-field for all inputs
  - [ ] Mat-error shows validation messages
  - [ ] Dialog layout with mat-dialog-title, mat-dialog-content, mat-dialog-actions
  - [ ] Form fields styled consistently
  - [ ] Accessible labels and error messages

#### Task 9.5: Create Detail Component (if needed)
- **Files**:
  - `frontend/src/app/features/[module]/components/[entity]-detail/[entity]-detail.component.ts`
  - `frontend/src/app/features/[module]/components/[entity]-detail/[entity]-detail.component.html`
  - `frontend/src/app/features/[module]/components/[entity]-detail/[entity]-detail.component.scss`
- **Estimated Effort**: 1 hour
- **Dependencies**: Task 8.2
- **Description**:
  - Create component to view entity details
  - Display read-only information
  - Add edit/delete actions
- **Acceptance Criteria**:
  - [ ] Loads entity by ID from route parameter
  - [ ] Displays all entity fields in organized layout
  - [ ] Edit button navigates to form or opens dialog
  - [ ] Proper loading and error states

---

### Phase 10: Frontend - Module & Routing

#### Task 10.1: Create/Update Angular Module
- **Files**:
  - `frontend/src/app/features/[module]/[entity]/[entity].module.ts`
- **Estimated Effort**: 30 minutes
- **Dependencies**: Task 9.1, 9.3
- **Description**:
  - Create or update feature module
  - Import necessary Angular Material modules
  - Declare components
  - Import routing module
- **Acceptance Criteria**:
  - [ ] Module declares all components
  - [ ] Material modules imported (MatTableModule, MatPaginatorModule, etc.)
  - [ ] FormsModule and ReactiveFormsModule imported
  - [ ] CommonModule imported

#### Task 10.2: Configure Routing
- **Files**:
  - `frontend/src/app/features/[module]/[entity]/[entity]-routing.module.ts`
  - Update parent routing if needed
- **Estimated Effort**: 30 minutes
- **Dependencies**: Task 10.1
- **Description**:
  - Define routes for list, detail views
  - Add route guards (AuthGuard, AdminGuard, etc.)
  - Update parent routing to include new routes
- **Implementation Notes**:
  ```typescript
  // Routes:
  // - '' → List component (with guards)
  // - ':id' → Detail component (with guards)
  ```
- **Acceptance Criteria**:
  - [ ] Routes defined with proper paths
  - [ ] Guards applied based on authorization matrix
  - [ ] Parent module imports child routing
  - [ ] Navigation works from menu/sidebar

#### Task 10.3: Add Navigation Menu Item (if applicable)
- **Files**:
  - `frontend/src/app/features/admin/admin.component.html` or
  - `frontend/src/app/features/student/student.component.html`
- **Estimated Effort**: 15 minutes
- **Dependencies**: Task 10.2
- **Description**:
  - Add menu item to admin or student navigation
  - Add icon and label
  - Link to new feature route
- **Acceptance Criteria**:
  - [ ] Menu item visible in correct role's navigation
  - [ ] Icon matches feature purpose
  - [ ] Clicking navigates to list component
  - [ ] Active route highlighted

---

### Phase 11: Frontend Testing

#### Task 11.1: Write Service Tests
- **Files**:
  - `frontend/src/app/features/[module]/services/[entity].service.spec.ts`
- **Estimated Effort**: 1 hour
- **Dependencies**: Task 8.2
- **Description**:
  - Test HTTP service methods
  - Mock HttpClient with HttpTestingController
  - Test request parameters and responses
- **Acceptance Criteria**:
  - [ ] All service methods tested
  - [ ] HTTP requests verified (method, URL, body)
  - [ ] Response mapping tested
  - [ ] Error handling tested

#### Task 11.2: Write Component Tests
- **Files**:
  - `frontend/src/app/features/[module]/components/[entity]-list/[entity]-list.component.spec.ts`
  - `frontend/src/app/features/[module]/components/[entity]-form/[entity]-form.component.spec.ts`
- **Estimated Effort**: 2 hours
- **Dependencies**: Task 9.1, 9.3
- **Description**:
  - Test component logic
  - Mock services
  - Test user interactions
  - Test form validation
- **Acceptance Criteria**:
  - [ ] Component initializes correctly
  - [ ] Service methods called on actions
  - [ ] Form validation rules tested
  - [ ] User interactions trigger expected behavior
  - [ ] Error states handled

---

### Phase 12: Integration & Documentation

#### Task 12.1: End-to-End Manual Testing
- **Estimated Effort**: 1 hour
- **Dependencies**: All previous tasks
- **Description**:
  - Start backend and frontend
  - Test complete user workflows from business requirements
  - Verify all acceptance criteria from user stories
- **Test Scenarios**:
  ```
  1. [User Story 1 scenario - step by step]
  2. [User Story 2 scenario - step by step]
  3. [Edge case 1 - verify behavior]
  4. [Edge case 2 - verify behavior]
  ```
- **Acceptance Criteria**:
  - [ ] All user stories verified end-to-end
  - [ ] Edge cases handled correctly
  - [ ] Error messages user-friendly
  - [ ] No console errors in browser
  - [ ] No exceptions in backend logs

#### Task 12.2: Update API Documentation
- **Estimated Effort**: 30 minutes
- **Dependencies**: Task 6.2
- **Description**:
  - Verify Swagger UI documentation is complete
  - Add example requests/responses
  - Test API endpoints via Swagger UI
- **Acceptance Criteria**:
  - [ ] Swagger UI accessible at /swagger-ui.html
  - [ ] All endpoints documented
  - [ ] Request/response examples present
  - [ ] Can execute API calls from Swagger UI

#### Task 12.3: Update Technical Documentation (if needed)
- **Files**:
  - `CLAUDE.md` (if architectural patterns added)
  - `README.md` (if new features affect setup)
- **Estimated Effort**: 30 minutes
- **Dependencies**: All previous tasks
- **Description**:
  - Document any new patterns or conventions
  - Update setup instructions if needed
  - Document configuration changes
- **Acceptance Criteria**:
  - [ ] New features documented in appropriate files
  - [ ] Configuration changes listed
  - [ ] Examples provided where helpful

---

## Task Dependency Graph

```
Phase 1 (Database)
    1.1 → 1.2
    
Phase 2 (Entity/Repo)
    1.1 → 2.1 → 2.3 → 2.4
    2.2 (parallel)
    
Phase 3 (DTOs)
    2.2 → 3.1, 3.2
    
Phase 4 (Mappers)
    2.1, 3.1, 3.2 → 4.1 → 4.2
    
Phase 5 (Service)
    2.3, 4.1 → 5.1 → 5.2
    
Phase 6 (Controller)
    5.1 → 6.1 → 6.2
    
Phase 7 (Backend Tests)
    2.3 → 7.1
    5.1 → 7.2
    6.1 → 7.3
    
Phase 8 (Frontend Models/Services)
    8.1 → 8.2 (can start in parallel with backend)
    
Phase 9 (Frontend Components)
    8.2 → 9.1 → 9.2
    8.2 → 9.3 → 9.4
    8.2 → 9.5
    
Phase 10 (Module/Routing)
    9.1, 9.3 → 10.1 → 10.2 → 10.3
    
Phase 11 (Frontend Tests)
    8.2 → 11.1
    9.1, 9.3 → 11.2
    
Phase 12 (Integration)
    All → 12.1 → 12.2, 12.3
```

---

## Parallel Work Opportunities

**Can be done simultaneously:**
1. Backend Entity/DTO work (Phase 2-3) + Frontend Models (Task 8.1)
2. Backend testing (Phase 7) + Frontend components (Phase 9)
3. Multiple components in Phase 9 (list, form, detail)
4. Repository tests + Service tests (7.1 and 7.2 can overlap)

**Must be sequential:**
- Database migration → Entities
- Entities → Repositories → Service → Controller
- Service → Tests
- Frontend Service → Components

---

## Effort Summary by Phase

| Phase | Total Effort | Tasks |
|-------|--------------|-------|
| Phase 1: Database | 1.5 hrs | 2 |
| Phase 2: Entity/Repo | 2.5 hrs | 4 |
| Phase 3: DTOs | 1.5 hrs | 2 |
| Phase 4: Mappers | 1 hr | 2 |
| Phase 5: Service | 3 hrs | 2 |
| Phase 6: Controller | 2 hrs | 2 |
| Phase 7: Backend Tests | 5 hrs | 3 |
| Phase 8: FE Models/Services | 1.5 hrs | 2 |
| Phase 9: FE Components | 5.75 hrs | 5 |
| Phase 10: Module/Routing | 1.25 hrs | 3 |
| Phase 11: FE Tests | 3 hrs | 2 |
| Phase 12: Integration | 2 hrs | 3 |
| **TOTAL** | **30 hrs** | **32 tasks** |

---

## Critical Path

The minimum time to complete (if work is done sequentially):
1. Database (1.5 hrs)
2. Entity → Repo → Service → Controller (8 hrs)
3. Frontend Service → Components → Routing (8.5 hrs)
4. Testing (5 hrs most critical)
5. Integration (2 hrs)

**Minimum Duration**: ~20-25 hours (with some parallelization)

---

## Implementation Order Recommendation

**Sprint 1 (Backend Foundation)**: Tasks 1.1-4.2  
**Sprint 2 (Backend Logic & APIs)**: Tasks 5.1-6.2  
**Sprint 3 (Backend Testing)**: Tasks 7.1-7.3  
**Sprint 4 (Frontend Foundation)**: Tasks 8.1-9.5  
**Sprint 5 (Frontend Polish & Testing)**: Tasks 10.1-11.2  
**Sprint 6 (Integration & Docs)**: Tasks 12.1-12.3  

---

## Pre-Implementation Checklist

Before starting, ensure:
- [ ] Business requirements approved by stakeholders
- [ ] Technical design reviewed and approved
- [ ] Database schema design finalized
- [ ] API contract agreed upon
- [ ] Test data preparation plan created
- [ ] Development environment set up
- [ ] Feature flag configured (if using FF4j)

---

## Post-Implementation Checklist

After completing all tasks:
- [ ] All unit tests passing (>80% coverage)
- [ ] All integration tests passing
- [ ] Manual E2E testing completed
- [ ] Swagger documentation verified
- [ ] Code review completed
- [ ] Security scan passed (no vulnerabilities)
- [ ] Performance testing done (if needed)
- [ ] Documentation updated
- [ ] Demo prepared for stakeholders

---

**Note**: Adjust task estimates based on your experience with the codebase. If a task consistently takes longer than estimated, split it into smaller tasks. Keep tasks focused and testable for best results.
