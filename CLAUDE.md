# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Student Accommodation System with a Spring Boot backend and Angular frontend. The system manages student housing applications, room allocations, documents, and notifications for university accommodation services.

**Tech Stack:**
- Backend: Spring Boot 3.5.7, Java 17, Gradle 8.11.1
- Frontend: Angular 21 with Material Design
- Database: PostgreSQL 16.2 with Flyway migrations
- Storage: MinIO for file uploads
- Authentication: JWT tokens with Spring Security
- API Documentation: SpringDoc OpenAPI (Swagger UI)
- Feature Flags: FF4j

## Development Commands

### Backend (Spring Boot)

**Build and run:**
```bash
cd backend
./gradlew build              # Build the project
./gradlew bootRun           # Run the application
./gradlew test              # Run tests
./gradlew clean build       # Clean build
```

**Testing and coverage:**
```bash
./gradlew test                          # Run tests
./gradlew jacocoTestReport             # Generate coverage report
./gradlew jacocoTestCoverageVerification  # Verify coverage thresholds
```

**Database migrations:**
- Flyway migrations are in `backend/src/main/resources/db/migration/`
- Migrations run automatically on application startup
- Naming: `V{YYYY_MM_DD_HHMM}__{description}.sql`

### Frontend (Angular)

**Development:**
```bash
cd frontend
npm install                 # Install dependencies
npm start                   # Start dev server (http://localhost:4200)
npm run build              # Production build
```

**Code quality:**
```bash
npm run lint               # Run ESLint and Stylelint
npm run lint:fix          # Auto-fix linting issues
npm run format            # Format code with Prettier
npm test                  # Run unit tests
```

### Docker Compose

**Full stack:**
```bash
docker-compose up --build  # Start all services (backend, frontend, db, minio, mailhog)
docker-compose down        # Stop all services
```

**Services:**
- Backend: http://localhost:8080
- Frontend: http://localhost:4200 (when frontend container is running)
- PostgreSQL: localhost:5432
- MinIO Console: http://localhost:9001
- MailHog UI: http://localhost:8025
- Swagger UI: http://localhost:8080/swagger-ui.html

## Architecture

### Backend Architecture

**Package Structure:** `com.beezhub.student_accommodation`
- `controller/` - REST endpoints organized by domain entity
- `service/` - Business logic layer
- `repository/` - Spring Data JPA repositories
- `model/entity/` - JPA entities (database tables)
- `model/dto/` - Data Transfer Objects for API requests/responses
- `model/enums/` - Enumerations (ApplicationStatus, RoomType, etc.)
- `mapper/` - MapStruct mappers for entity-DTO conversion
- `security/` - JWT authentication, user details, filters
- `config/` - Configuration classes (Security, CORS, Mail, MinIO, etc.)
- `exceptions/` - Custom exception classes
- `specification/` - JPA Specifications for dynamic filtering
- `validation/` - Custom validators
- `aspect/` - AOP aspects for cross-cutting concerns
- `util/` - Utility classes

**Key Services:**
- `ApplicationService` - Manages student applications (submission, review, approval)
- `RoomAllocationService` - Handles room allocation logic, auto-allocation batches
- `AllocationScoringService` - Scoring algorithm for allocation decisions
- `NotificationService` - Email notifications with retry logic
- `DocumentService` - Document upload/verification with MinIO integration
- `GoogleMapsService` - Geocoding for building addresses
- `SystemSettingService` - Cascade-model settings (platform defaults + tenant overrides)

**Multi-Tenancy Architecture:**
- Every request runs in a tenant context set by `TenantContext` (thread-local `Long tenantId`)
- `TenantListener` (`@PrePersist`) auto-stamps `tenant_id` on new entities from `TenantContext`
- Hibernate filter `tenantFilter` scopes all queries to the current tenant automatically
- **PLATFORM tenant** (`tenant_id = 0`) is seeded by Flyway; used for platform-wide defaults
- `TenantFilterAspect` (AOP) activates/deactivates the Hibernate filter around service methods
- Entities with a tenant FK have dual mapping: `@Column tenantId` (writable) + `@ManyToOne tenant` (read-only, `insertable=false`)
- Tests must set both `TenantContext.setTenantId(...)` and seed the Tenant row (id=0 always exists)

**System Settings (cascade model):**
- `system_setting` has composite PK `(tenant_id, setting_key)`
- Platform defaults live at `tenant_id = 0`; tenant admins can create override rows
- `getSetting(key)`: checks current tenant row first, falls back to platform row
- `getAllSettings()`: merges platform map, overlaid by current tenant's rows
- **API split:**
  - `GET/PUT /api/platform/system-settings` — PLATFORM_ADMIN only, manages `tenant_id=0` rows
  - `GET/PUT/DELETE /api/admin/system-settings` — ADMIN + PLATFORM_ADMIN, tenant-scoped view with cascade values
- `auto_allocation_enabled` is a platform-only setting (ADMIN sees it read-only in the UI)

**Allocation System:**
- Applications are submitted by students with preferences
- Auto-allocation runs in batches (configurable interval, default 6 hours)
- Scoring algorithm ranks students based on allocation rules
- StagedAllocation allows manual review before finalization
- Offers have expiry dates; declined offers trigger reallocation

### Frontend Architecture

**Module Structure:** `frontend/src/app/`
- `features/auth/` - Login, signup, password reset, email verification
- `features/admin/` - Admin dashboard and management modules
  - Building, room, application, allocation, document management
  - Activity logs, user management, system settings
- `features/dashboard/` - Student dashboard (applications, documents, profile)
- `features/student/` - Student-specific features
- `core/` - Core services, guards, interceptors, models
  - Guards: Auth, admin, student, email-verified
  - Interceptors: Auth token, error handling, token expiry
- `shared/` - Reusable components across features
- `model/` - TypeScript interfaces/models

**Authentication Flow:**
1. User signs up → Email verification token sent
2. Email verified → Profile setup (student details)
3. Login → JWT token issued → Stored in localStorage
4. Token included in requests via AuthInterceptor
5. Role-based routing with guards (admin.guard.ts, student.guard.ts)

**Frontend conventions:**
- Routing: flat routes in `app-routing.module.ts` — no `.routes.ts` files
- Admin sidebar nav groups defined in `admin-sidebar.component.ts` `navGroups` array
- Snackbar panel classes: both `snack-*` (UISpec) and `*-snackbar` (legacy) are valid — both defined in `styles.scss`
- **Always invoke the `ui-ux-pro-max` skill before implementing any frontend code** (components, templates, SCSS, Angular modules)

### Database Schema

**Core Entities:**
- `app_user` - Base user table (admin/student)
- `student` - Student profile (linked to app_user)
- `application` - Housing applications with preferences
- `room_allocation` - Final room assignments
- `staged_allocation` - Pending allocations for review
- `building`, `room` - Physical infrastructure
- `building_image` - Building photos
- `building_attributes`, `room_attributes` - Metadata
- `document` - Uploaded documents (stored in MinIO)
- `notification`, `user_notification` - Email tracking
- `allocation_rule` - Rules for allocation scoring
- `waiting_list_entry` - Waitlist management
- `academic_year`, `institution`, `year_of_study` - Reference data
- `activity_log` - Audit trail
- `system_setting` - Configuration key-value pairs; composite PK `(tenant_id, setting_key)`; cascade model (platform defaults at `tenant_id=0`, tenant overrides at their own `tenant_id`)

**Important Notes:**
- Flyway manages schema versions - never manually edit production DB
- Enums are stored as strings in the database
- Soft deletes are not used; records are permanently deleted

## Configuration

**Backend Environment Variables:**
- Database: `DB_URL`, `USER_DB`, `PASSWORD_DB`
- JWT: `JWT_SECRET`, `JWT_EXPIRATION`, `JWT_ADMIN_EXPIRATION`
- MinIO: `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET`
- Email: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`
- Google Maps: `GOOGLE_MAPS_API_KEY`, `GOOGLE_MAPS_FRONTEND_API_KEY`
- App: `APP_BASE_URL`, `APP_SUPPORT_EMAIL`, `APP_UNIVERSITY_NAME`
- Allocation: `BATCH_ENABLED`, `BATCH_INTERVAL_MS`, `OFFER_EXPIRY_DAYS`
- Feature Flags: `AUTO_ALLOCATION_ENABLED`, `GEOCODING_ENABLED`

Configured in: `backend/src/main/resources/application.yml`

**Frontend Environment:**
- API endpoints configured in Angular environment files
- Google Maps API key for building location features

## Key Features and Workflows

**Application Submission (Student):**
1. Student logs in and navigates to application form
2. Fills preferences: accommodation type, move-in date, duration, room type, special requirements
3. Uploads required documents (ID, proof of enrollment, etc.)
4. Submits application → Status: SUBMITTED
5. Receives email confirmation

**Application Review (Admin):**
1. Admin views applications in review queue
2. Verifies documents → Approves/requests changes
3. Application moves to APPROVED status
4. Enters allocation pool

**Room Allocation (Auto or Manual):**
- **Auto-allocation batch:** Runs periodically, scores applications, assigns rooms
- **Manual allocation:** Admin assigns specific room to student
- **Staged allocation:** Admin reviews proposed allocations before finalizing
- Student receives offer notification → Must accept/decline within expiry period

**Document Management:**
- Documents uploaded to MinIO bucket
- Supports multiple file types with validation
- Admins can add comments and request resubmissions
- Bulk upload support for building images

## Testing Conventions

**Backend:**
- JUnit tests in `backend/src/test/java/`
- Jacoco for coverage reporting
- Target coverage: Configurable per class in build.gradle
- Run specific test: `./gradlew test --tests "com.beezhub.student_accommodation.ClassName.methodName"`

**Test base classes** (always extend these — never configure containers directly):
- `AbstractIntegrationTest` — `@SpringBootTest(RANDOM_PORT)` with 4 Testcontainers: PostgreSQL 16.2, MailHog, Redis 7, MinIO. Flyway runs on startup (`ddl-auto=none`). `mailServer` field is `protected` for subclass assertion.
- `AbstractJpaTest` — `@DataJpaTest` with PostgreSQL Testcontainer only. Uses `ddl-auto=create-drop` (Flyway disabled). Use this for repository/filter/entity-lifecycle tests; use `AbstractIntegrationTest` when you need Flyway migrations or commit semantics.

**Critical DDL property path:** `application.yml` uses `spring.jpa.properties.hibernate.hbm2ddl.auto` (NOT the shortcut `spring.jpa.hibernate.ddl-auto`). The `properties.*` path takes precedence. `@DynamicPropertySource` must override **both**:
```java
registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
registry.add("spring.jpa.properties.hibernate.hbm2ddl.auto", () -> "none");
```

**Integration test setup patterns** (full-stack tests without `@Transactional`):
1. Delete in FK-safe order in `@AfterEach` (applications → academic_years → students → app_users)
2. Set security context: `SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, null, List.of()))` for `authService.getCurrentUser()`
3. Set `TenantContext.setTenantId(...)` for `TenantListener.@PrePersist` to stamp `tenant_id`
4. Clear both in `@AfterEach`: `SecurityContextHolder.clearContext(); TenantContext.clear()`

**Tenant FK in tests:** PostgreSQL enforces the `tenant_id` FK (unlike H2 which silently ignored it). Tests using specific tenant IDs must seed Tenant rows first via native SQL in `@BeforeEach`. `tenant(id=0)` always exists (seeded by Flyway `V2026_02_25_0013__platform_tenant_seed.sql`).

**Schema constraint:** `academic_year.name` is `VARCHAR(20)` — test data must be ≤ 20 characters.

**Pre-existing test failures (not our concern):**
- `RoomServiceTest`: 3 failures — `UnnecessaryStubbingException` + mock assertion bug in bulk room creation
- `ScheduledJobRunnerTest`: 2 failures — `NullPointerException` for `checkInReminderService` (injection issue)

**Frontend:**
- Jasmine/Karma for unit tests
- Test files: `*.spec.ts` alongside components
- Run tests: `npm test`

## Important Patterns

**Security:**
- JWT tokens are stateless; no server-side session storage
- Token expiry is enforced client-side and server-side
- Admin tokens have shorter expiry (4 hours vs 10 hours for students)
- Role-based access via Spring Security and Angular guards

**Error Handling:**
- Backend returns standardized error responses
- Frontend ErrorInterceptor displays user-friendly messages
- Validation errors mapped from backend to form fields

**Async Processing:**
- Email sending is asynchronous via `@Async`
- Batch allocation runs on scheduled intervals
- Notification retry mechanism for failed emails

**MapStruct:**
- Used for entity-DTO mapping
- Mappers are in `mapper/` package
- Regenerate mappers after entity/DTO changes: `./gradlew build`

## OpenSpec Workflow

Changes are tracked in `openspec/changes/` using a spec-driven workflow.

**Directory layout:**
- `openspec/changes/<slug>/` — in-progress change (proposal, design, specs, tasks)
- `openspec/changes/archive/` — completed/archived changes
- `openspec/specs/` — canonical spec snapshots (updated after archiving)

**Skills:** `/openspec-propose` → `/openspec-apply-change` → `/openspec-archive-change`

Each change contains:
- `proposal.md` — why/what/impact
- `design.md` — technical design
- `specs/<capability>/spec.md` — detailed capability spec
- `tasks.md` — checked-off implementation tasks

## Branch Strategy

- `main` - Production-ready code
- `develop` - Integration branch for features
- Feature branches merge into `develop`, then `develop` merges to `main`

## Local Setup

Before running docker-compose, you **must** create a `.env` file with the required credentials:

```bash
# 1. Copy the example file
cp .env.example .env

# 2. Fill in REQUIRED values (the app will not start without these):
#    - JWT_SECRET   (generate with: openssl rand -base64 64)
#    - PASSWORD_DB
#    - MINIO_ACCESS_KEY
#    - MINIO_SECRET_KEY
#    - GRAFANA_ADMIN_PASSWORD

# 3. Start all services
docker-compose up --build
```

**Never commit `.env` to version control.** The `.gitignore` is already configured to exclude it.

See `.env.example` for all available variables with descriptions, and `CREDENTIAL_ROTATION.md` for security notes on each credential.
