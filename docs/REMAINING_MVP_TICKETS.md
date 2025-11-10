# Remaining MVP Tickets to Create in JIRA

## Summary
This document contains the detailed specifications for 11 remaining tickets that need to be created manually in JIRA for the Student Accommodation System MVP.

---

## DEPLOYMENT INFRASTRUCTURE TICKETS (Epic: SASS-78)

### Ticket 1: Create frontend Dockerfile with multi-stage build

**Issue Type:** Task  
**Summary:** [DevOps] Create frontend Dockerfile with multi-stage build  
**Epic Link:** SASS-78  
**Labels:** devops, docker, frontend, mvp, senior  
**Assignee:** Senior Backend Developer  
**Story Points:** 2  
**Estimated Time:** 1 day  

**Description:**

Build optimized Docker image for Angular frontend with nginx.

**Dockerfile Structure:**
```dockerfile
# Stage 1: Build Angular application
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build --configuration=production

# Stage 2: Serve with nginx
FROM nginx:alpine
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

**nginx.conf:**
```nginx
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    gzip on;
    gzip_types text/css application/javascript application/json;
}
```

**Acceptance Criteria:**
- ✅ Dockerfile uses multi-stage build
- ✅ Production build optimized (minified, tree-shaken)
- ✅ nginx configured for SPA routing
- ✅ API proxy configured
- ✅ Gzip compression enabled
- ✅ Image builds successfully
- ✅ Image size optimized (<100MB)
- ✅ Container runs and serves app correctly

**Testing:**
```bash
docker build -t student-accommodation-frontend .
docker run -p 4200:80 student-accommodation-frontend
# Test http://localhost:4200
```

---

### Ticket 2: Update docker-compose for production environment

**Issue Type:** Task  
**Summary:** [DevOps] Update docker-compose for production environment  
**Epic Link:** SASS-78  
**Labels:** devops, docker, configuration, mvp, senior  
**Assignee:** Senior Backend Developer  
**Story Points:** 2  
**Estimated Time:** 1 day  

**Description:**

Create production-ready docker-compose with all services and networking.

**docker-compose.prod.yml:**
```yaml
version: '3.8'

services:
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    ports:
      - "80:80"
    depends_on:
      - backend
    networks:
      - app-network
    restart: unless-stopped

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DATABASE_URL: jdbc:postgresql://postgres:5432/studentaccommodation
      DATABASE_USERNAME: ${DB_USERNAME}
      DATABASE_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      MINIO_URL: http://minio:9000
      MINIO_ACCESS_KEY: ${MINIO_ACCESS_KEY}
      MINIO_SECRET_KEY: ${MINIO_SECRET_KEY}
      MAIL_HOST: ${MAIL_HOST}
      MAIL_USERNAME: ${MAIL_USERNAME}
      MAIL_PASSWORD: ${MAIL_PASSWORD}
    depends_on:
      - postgres
      - minio
    networks:
      - app-network
    restart: unless-stopped

  postgres:
    image: postgres:16.2-alpine
    environment:
      POSTGRES_DB: studentaccommodation
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - app-network
    restart: unless-stopped

  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_ACCESS_KEY}
      MINIO_ROOT_PASSWORD: ${MINIO_SECRET_KEY}
    volumes:
      - minio-data:/data
    networks:
      - app-network
    restart: unless-stopped

networks:
  app-network:
    driver: bridge

volumes:
  postgres-data:
  minio-data:
```

**.env.example:**
```
DB_USERNAME=produser
DB_PASSWORD=changeme
JWT_SECRET=your-secret-key-here
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MAIL_HOST=smtp.gmail.com
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

**Acceptance Criteria:**
- ✅ docker-compose.prod.yml created
- ✅ All services defined with proper configuration
- ✅ Environment variables externalized
- ✅ Volumes for data persistence
- ✅ Network isolation configured
- ✅ Health checks defined
- ✅ Restart policies set
- ✅ Full stack runs successfully
- ✅ Documentation for environment setup

---

### Ticket 3: Set up CI/CD pipeline with GitHub Actions

**Issue Type:** Task  
**Summary:** [DevOps] Set up CI/CD pipeline with GitHub Actions  
**Epic Link:** SASS-78  
**Labels:** devops, ci-cd, automation, mvp, senior  
**Assignee:** Senior Backend Developer  
**Story Points:** 2  
**Estimated Time:** 1 day  

**Description:**

Create automated build, test, and deploy pipeline.

**.github/workflows/ci-cd.yml:**
```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  backend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Run tests
        run: cd backend && ./gradlew test
      - name: Generate coverage report
        run: cd backend && ./gradlew jacocoTestReport
      - name: Upload coverage
        uses: codecov/codecov-action@v3

  frontend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '20'
      - name: Install dependencies
        run: cd frontend && npm ci
      - name: Run linting
        run: cd frontend && npm run lint
      - name: Run tests
        run: cd frontend && npm run test:ci

  build-images:
    needs: [backend-test, frontend-test]
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Build backend image
        run: cd backend && docker build -t sama-backend .
      - name: Build frontend image
        run: cd frontend && docker build -t sama-frontend .
```

**Acceptance Criteria:**
- ✅ CI pipeline runs on every commit
- ✅ Backend tests run automatically
- ✅ Frontend tests run automatically
- ✅ Test coverage reported
- ✅ Docker images built on main branch
- ✅ Failed builds block merges
- ✅ Build status badges in README

---

### Ticket 4: Configure environment-specific settings

**Issue Type:** Task  
**Summary:** [DevOps] Configure environment-specific settings  
**Epic Link:** SASS-78  
**Labels:** devops, configuration, mvp, senior  
**Assignee:** Senior Backend Developer  
**Story Points:** 1  
**Estimated Time:** 0.5 day  

**Description:**

Set up configuration for dev, staging, and production environments.

**Backend Configuration Files:**

**application-dev.yml:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/studentaccommodationlocaldb
  jpa:
    show-sql: true
logging:
  level:
    com.beezhub: DEBUG
```

**application-prod.yml:**
```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate
logging:
  level:
    com.beezhub: INFO
server:
  error:
    include-message: false
    include-stacktrace: never
```

**Frontend Environment Files:**

**environment.ts (dev):**
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  minioUrl: 'http://localhost:9000'
};
```

**environment.prod.ts:**
```typescript
export const environment = {
  production: true,
  apiUrl: '/api',
  minioUrl: '/minio'
};
```

**Acceptance Criteria:**
- ✅ Dev, staging, prod configs created
- ✅ Sensitive data externalized to env vars
- ✅ Logging levels appropriate per environment
- ✅ Error details hidden in production
- ✅ Database settings per environment
- ✅ CORS configured per environment
- ✅ Documentation for configuration

---

### Ticket 5: Write deployment documentation

**Issue Type:** Task  
**Summary:** [DevOps] Write deployment documentation  
**Epic Link:** SASS-78  
**Labels:** devops, documentation, mvp, senior  
**Assignee:** Senior Backend Developer  
**Story Points:** 2  
**Estimated Time:** 1 day  

**Description:**

Create comprehensive deployment guide for MVP.

**Documentation Files:**

**DEPLOYMENT.md:**
- Prerequisites (Docker, docker-compose)
- Environment setup instructions
- Configuration guide
- Deployment steps (local, staging, production)
- Database migration procedures
- Troubleshooting guide
- Rollback procedures
- Monitoring and health checks
- Backup and restore procedures

**Sections to Include:**

1. **Prerequisites**
   - Docker 24+
   - docker-compose 2.20+
   - Git
   - Domain name (production)
   - SSL certificate (production)

2. **Environment Setup**
   - Clone repository
   - Copy .env.example to .env
   - Set environment variables
   - Create email account for notifications

3. **Local Deployment**
   - `docker-compose up -d`
   - Access URLs
   - Default admin credentials

4. **Production Deployment**
   - Server requirements
   - Domain and SSL setup
   - `docker-compose -f docker-compose.prod.yml up -d`
   - Initial data seeding
   - Creating admin user

5. **Database Management**
   - Running migrations
   - Backup commands
   - Restore commands

6. **Monitoring**
   - Health check endpoints
   - Log locations
   - Performance metrics

7. **Troubleshooting**
   - Common issues and solutions
   - Debug commands
   - Log analysis

**README.md Updates:**
- Quick start guide
- Development setup
- Running tests
- Contributing guidelines

**Acceptance Criteria:**
- ✅ DEPLOYMENT.md created and comprehensive
- ✅ README.md updated
- ✅ Step-by-step instructions clear
- ✅ Commands tested and verified
- ✅ Screenshots included where helpful
- ✅ Troubleshooting section complete
- ✅ Documentation reviewed and approved

---

## TESTING & QA TICKETS (Epic: SASS-79)

### Ticket 6: Generate and analyze test coverage reports

**Issue Type:** Task  
**Summary:** [Testing] Generate and analyze test coverage reports  
**Epic Link:** SASS-79  
**Labels:** backend, testing, coverage, mvp, senior  
**Assignee:** Senior Backend Developer  
**Story Points:** 1  
**Estimated Time:** 0.5 day  

**Description:**

Generate Jacoco test coverage reports and analyze coverage metrics.

**Tasks:**
1. Run `./gradlew test jacocoTestReport`
2. Generate HTML coverage report
3. Analyze coverage by package
4. Identify untested code
5. Document coverage gaps

**Commands:**
```bash
cd backend
./gradlew clean test jacocoTestReport
# Reports generated in build/jacocoHtml/test/html/index.html
```

**Coverage Thresholds:**
- Overall coverage: ≥ 80%
- Entity classes: ≥ 90%
- Service classes: ≥ 85%
- Controller classes: ≥ 75%
- Repository classes: ≥ 70%

**Deliverables:**
- Coverage report (HTML)
- Coverage analysis document
- List of untested methods/classes
- Priority list for missing tests

**Acceptance Criteria:**
- ✅ Jacoco reports generated successfully
- ✅ Coverage metrics documented
- ✅ Gaps identified and prioritized
- ✅ Report shared with team
- ✅ Action plan for missing tests created

---

### Ticket 7: Write missing backend unit tests

**Issue Type:** Task  
**Summary:** [Testing] Write missing backend unit tests to reach 80% coverage  
**Epic Link:** SASS-79  
**Labels:** backend, testing, unit-tests, mvp, senior  
**Assignee:** Senior Backend Developer  
**Story Points:** 5  
**Estimated Time:** 2 days  

**Description:**

Write unit tests for untested backend code to achieve 80%+ coverage.

**Priority Test Areas:**
1. **BuildingService** - All CRUD operations
2. **RoomAllocationService** - Business rules and validation
3. **NotificationService** - Email sending logic
4. **BuildingController** - All endpoints
5. **RoomAllocationController** - All endpoints
6. **BuildingMapper** - DTO mapping
7. **AllocationMapper** - DTO mapping

**Test Template:**
```java
@ExtendWith(MockitoExtension.class)
class BuildingServiceTest {
    
    @Mock
    private BuildingRepository repository;
    
    @InjectMocks
    private BuildingService service;
    
    @Test
    void createBuilding_withValidData_shouldSucceed() {
        // Arrange
        BuildingRequest request = new BuildingRequest(/*...*/);
        Building entity = new Building(/*...*/);
        when(repository.save(any())).thenReturn(entity);
        
        // Act
        BuildingResponse response = service.createBuilding(request);
        
        // Assert
        assertNotNull(response);
        verify(repository).save(any());
    }
}
```

**Acceptance Criteria:**
- ✅ All identified gaps have unit tests
- ✅ Overall coverage ≥ 80%
- ✅ All tests pass consistently
- ✅ Test names follow naming conventions
- ✅ Proper use of mocks and assertions
- ✅ Edge cases covered

---

### Ticket 8: Create E2E tests for critical user journeys

**Issue Type:** Task  
**Summary:** [Testing] Create E2E tests for critical user journeys  
**Epic Link:** SASS-79  
**Labels:** frontend, testing, e2e, mvp, junior  
**Assignee:** Junior Frontend Developer  
**Story Points:** 3  
**Estimated Time:** 2 days  

**Description:**

Create end-to-end tests using Cypress for critical user workflows.

**Critical Journeys:**

1. **Student Registration & Application**
   - Register new account
   - Complete profile
   - Upload documents
   - Submit application
   - Verify confirmation

2. **Admin Application Review**
   - Login as admin
   - Navigate to applications
   - Review application details
   - Approve application
   - Verify email sent

3. **Admin Room Allocation**
   - View approved applications
   - Search for available room
   - Allocate room to student
   - Verify allocation saved
   - Verify notification sent

4. **Student View Room Assignment**
   - Login as student
   - Navigate to dashboard
   - View assigned room
   - Verify room details displayed

**Cypress Test Example:**
```typescript
describe('Student Application Journey', () => {
  it('should complete full application process', () => {
    cy.visit('/signup');
    cy.get('[data-cy=email]').type('student@test.com');
    cy.get('[data-cy=password]').type('Password123!');
    cy.get('[data-cy=submit]').click();
    
    // Continue test flow...
    cy.url().should('include', '/dashboard');
  });
});
```

**Acceptance Criteria:**
- ✅ All 4 critical journeys have E2E tests
- ✅ Tests run successfully in CI
- ✅ Tests use realistic test data
- ✅ Tests clean up after execution
- ✅ Test reports generated
- ✅ Screenshots captured on failure

---

### Ticket 9: Perform accessibility audit (WCAG 2.1 AA)

**Issue Type:** Task  
**Summary:** [Testing] Perform accessibility audit (WCAG 2.1 AA)  
**Epic Link:** SASS-79  
**Labels:** frontend, testing, accessibility, mvp, junior  
**Assignee:** Junior Frontend Developer  
**Story Points:** 2  
**Estimated Time:** 1 day  

**Description:**

Conduct accessibility audit and fix issues to achieve WCAG 2.1 AA compliance.

**Audit Tools:**
- axe-core DevTools extension
- Lighthouse accessibility audit
- WAVE accessibility checker
- Screen reader testing (NVDA/JAWS)

**Pages to Audit:**
- Login page
- Registration flow
- Student dashboard
- Application form
- Admin dashboard
- Room browsing page
- Document upload page

**Common Issues to Check:**
- ✅ Color contrast ratios (4.5:1 minimum)
- ✅ Keyboard navigation support
- ✅ Focus indicators visible
- ✅ Alt text on images
- ✅ Form labels properly associated
- ✅ ARIA attributes where needed
- ✅ Heading hierarchy correct
- ✅ Error messages accessible
- ✅ Screen reader announcements

**Deliverables:**
- Accessibility audit report
- List of violations with severity
- Fixed issues list
- WCAG compliance certificate

**Acceptance Criteria:**
- ✅ All pages audited with axe-core
- ✅ Zero critical/serious violations
- ✅ Lighthouse accessibility score ≥ 90
- ✅ Keyboard navigation works on all pages
- ✅ Screen reader tested on key flows
- ✅ Documentation updated with accessibility features

---

### Ticket 10: Cross-browser testing

**Issue Type:** Task  
**Summary:** [Testing] Cross-browser testing (Chrome, Firefox, Safari, Edge)  
**Epic Link:** SASS-79  
**Labels:** frontend, testing, compatibility, mvp, junior  
**Assignee:** Junior Frontend Developer  
**Story Points:** 2  
**Estimated Time:** 1 day  

**Description:**

Test application across all major browsers to ensure compatibility.

**Browsers to Test:**
- Google Chrome (latest)
- Mozilla Firefox (latest)
- Safari (latest) - macOS/iOS
- Microsoft Edge (latest)

**Test Checklist per Browser:**

**Visual Testing:**
- ✅ Layout renders correctly
- ✅ Fonts load properly
- ✅ Colors/gradients display correctly
- ✅ Images load and display
- ✅ Animations work smoothly
- ✅ Responsive breakpoints work

**Functional Testing:**
- ✅ Login/logout works
- ✅ Forms submit correctly
- ✅ File upload works
- ✅ Navigation functions
- ✅ Buttons/links work
- ✅ Modals open/close
- ✅ Notifications display

**Performance:**
- ✅ Page load times acceptable
- ✅ No console errors
- ✅ No memory leaks
- ✅ Smooth scrolling

**Test Matrix:**
Create spreadsheet tracking:
- Browser version
- OS version
- Test status (Pass/Fail)
- Issues found
- Screenshots of issues

**Acceptance Criteria:**
- ✅ All browsers tested on Windows/Mac
- ✅ Mobile browsers tested (iOS Safari, Chrome Android)
- ✅ Test matrix completed
- ✅ Critical issues documented
- ✅ Browser-specific bugs fixed
- ✅ No blocking issues in any major browser

---

### Ticket 11: Performance testing and optimization

**Issue Type:** Task  
**Summary:** [Testing] Performance testing and optimization  
**Epic Link:** SASS-79  
**Labels:** backend, frontend, testing, performance, mvp, senior  
**Assignee:** Senior Backend Developer  
**Story Points:** 3  
**Estimated Time:** 1.5 days  

**Description:**

Conduct performance testing and optimize for production readiness.

**Performance Targets:**
- Page load time < 3 seconds
- API response time < 500ms
- Database queries < 100ms
- Time to Interactive < 5 seconds
- First Contentful Paint < 1.5 seconds

**Backend Performance:**

**Database Optimization:**
- Add missing indexes
- Optimize N+1 query problems
- Enable query caching
- Review slow query log

**API Optimization:**
- Enable response compression
- Implement Redis caching for frequent queries
- Optimize pagination
- Review connection pool settings

**Frontend Performance:**

**Bundle Optimization:**
- Analyze bundle size with webpack-bundle-analyzer
- Lazy load feature modules
- Remove unused dependencies
- Enable production optimizations

**Runtime Optimization:**
- Implement virtual scrolling for long lists
- Optimize change detection
- Use OnPush strategy where possible
- Optimize images (WebP format, lazy loading)

**Testing Tools:**
- Lighthouse performance audit
- JMeter for load testing
- Chrome DevTools Performance tab
- Database slow query log

**Load Testing Scenarios:**
- 100 concurrent users
- 1000 requests per minute
- Sustained load for 10 minutes

**Deliverables:**
- Performance test report
- Optimization recommendations
- Before/after metrics
- Lighthouse score ≥ 90

**Acceptance Criteria:**
- ✅ All performance targets met
- ✅ No performance regressions
- ✅ Load testing completed successfully
- ✅ Database queries optimized
- ✅ Bundle size reduced
- ✅ Caching implemented
- ✅ Performance monitoring in place

---

## Instructions for Adding to JIRA

### Quick Import Method:
1. Go to your JIRA project: https://student-accommodation-system.atlassian.net/jira/software/projects/SASS
2. Click on "..." menu → "Import issues"
3. Copy ticket details from this document
4. Create each ticket manually with the specifications above

### For Each Ticket:
1. **Issue Type:** Task
2. **Project:** SASS
3. **Epic Link:** Link to the corresponding Epic (SASS-78 or SASS-79)
4. **Labels:** Copy from ticket specification
5. **Story Points:** Set as specified
6. **Description:** Copy entire description section
7. **Assignee:** Assign to appropriate developer

### Epic Links:
- **SASS-78:** Deployment Infrastructure & DevOps (Tickets 1-5)
- **SASS-79:** MVP Testing & Quality Assurance (Tickets 6-11)

---

## Summary

**Total Remaining Tickets:** 11

**Deployment Tickets:** 5
- Frontend Dockerfile (1 day)
- Production docker-compose (1 day)
- CI/CD pipeline (1 day)
- Environment config (0.5 day)
- Deployment docs (1 day)

**Testing Tickets:** 6
- Coverage reports (0.5 day)
- Missing unit tests (2 days)
- E2E tests (2 days)
- Accessibility audit (1 day)
- Cross-browser testing (1 day)
- Performance testing (1.5 days)

**Total Effort:** ~12 days of work

---

## Next Steps After Creating Tickets

1. **Organize into Sprints:**
   - Week 7: Deployment tickets (Tickets 1-5)
   - Week 8: Testing tickets (Tickets 6-11)

2. **Set Dependencies:**
   - E2E tests depend on all features being complete
   - Performance testing should be done after optimization

3. **Daily Standup Topics:**
   - Deployment blockers
   - Test coverage progress
   - Performance metrics

4. **Definition of Done:**
   - All 11 tickets completed
   - All acceptance criteria met
   - MVP ready for staging deployment

---

**Document Version:** 1.0  
**Created:** November 10, 2025  
**Author:** GitHub Copilot  
**Status:** Ready for JIRA Import
