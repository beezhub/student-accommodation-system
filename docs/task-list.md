# Task List — Student Accommodation System
## Technical Stabilisation & Architecture Recovery

> **Generated from:** `Architecture.md` (Technical Stabilisation Document) and `Requirements.md` (Business Requirements Document)
> **Date:** 2026-02-12

---

### Status Dashboard

| Phase | Total | Done | In Progress | Todo | Blocked | Progress |
|-------|-------|------|-------------|------|---------|----------|
| Phase 1: Critical Stabilisation | 10 | 10 | 0 | 0 | 0 | 100% ✅ |
| Phase 2: Structural Improvement | 13 | 0 | 0 | 13 | 0 | 0% |
| Phase 3: Enhancement & Optimization | 10 | 0 | 0 | 10 | 0 | 0% |
| **Total** | **33** | **10** | **0** | **23** | **0** | **30%** |

**Current Blockers:** None
**Phase 1 Gate:** ✅ PASSED — Ready for Phase 2
**Next Recommended Tasks:** TASK-2.01 (Extract AllocationExpiryService - HIGH), TASK-2.02 (Extract AllocationLifecycleService - HIGH), TASK-2.04 (Decompose ApplicationService - MEDIUM, parallelizable)

---

## Phase 1: Critical Stabilisation

*Changes that must happen first to address CRITICAL and HIGH severity security and stability issues. All tasks must be complete before Phase 2 begins.*

---

### TASK-1.01: Remove .env from Repository and Rotate Credentials

- **Status:** DONE
- **Priority:** CRITICAL
- **Phase:** 1
- **Estimated Scope:** S (< 2 hrs)
- **Architecture Reference:** [ARCH 3.2.8] .env Credential Cleanup
- **Requirement References:** [REQ AUTH-003] (JWT secret configuration), [REQ BR-030] (token expiry config)
- **Risk References:** RISK-003 (Credential exposure from committed .env file)
- **Dependencies:** None — highest priority, do first
- **Blocked By:** None

**Description:**
Add `.env` to `.gitignore`. Remove `.env` from git tracking using `git rm --cached .env`. Create `.env.example` with placeholder values only (no real credentials). Rotate all API keys that were exposed: Google Maps API key, MinIO access/secret keys, JWT secret, SMTP credentials, database password. Update `README.md` with setup instructions referencing `.env.example`.

**Files to Touch:**
- `.gitignore` — add `.env` entry
- `.env` — remove from git tracking (do NOT delete from filesystem)
- `.env.example` — create with placeholder values for all environment variables referenced in `docker-compose.yml` and `backend/src/main/resources/application.yml`
- `README.md` — add setup instructions for environment setup

**Implementation Notes:**
- Run `git rm --cached .env` to stop tracking the file without deleting it locally
- Verify `.env` patterns in `.gitignore` cover nested `.env` files if any exist
- All previously exposed credentials MUST be rotated in the actual deployment environment
- Reference `docker-compose.yml` for the full list of environment variables to include in `.env.example`

**Acceptance Criteria:**
- [x] `.env` is listed in `.gitignore`
- [x] `.env` is not tracked by git (`git status` does not show `.env`)
- [x] `.env.example` exists with placeholder values (e.g., `JWT_SECRET=your-secret-here`)
- [x] `README.md` contains instructions for environment setup
- [x] No real credentials appear in any tracked file

**Tests to Write:**
- No automated tests required — manual verification of git tracking status

---

### TASK-1.02: Implement AllocationStatus State Machine

- **Status:** DONE
- **Priority:** HIGH
- **Phase:** 1
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 3.2.3] AllocationStatus State Machine Enforcement, [ARCH ADR-004]
- **Requirement References:** [REQ ALLOC-006] (offer accept/decline), [REQ ALLOC-007] (offer expiry), [REQ BR-007] (state transition enforcement)
- **Risk References:** RISK-004 (Invalid allocation status transition corrupts data)
- **Dependencies:** None
- **Blocked By:** None

**Description:**
Add a `canTransitionTo(AllocationStatus target)` method to the `AllocationStatus` enum, mirroring the existing pattern in `ApplicationStatus.java`. Define the valid transition map:
- OFFERED → ACCEPTED, DECLINED, EXPIRED, CANCELLED
- ACCEPTED → ACTIVE, CHECK_IN_EXPIRED, CANCELLED
- ACTIVE → COMPLETED, TERMINATED, CANCELLED
- DECLINED, EXPIRED, CHECK_IN_EXPIRED, COMPLETED, TERMINATED, CANCELLED → terminal (no outgoing transitions)

Add `validateStatusTransition()` calls before every allocation status change in the service layer. Throw `InvalidStatusException` (or a new `InvalidAllocationStatusTransitionException`) for invalid transitions.

**Files to Touch:**
- `backend/src/main/java/com/beezhub/student_accommodation/model/enums/AllocationStatus.java` — add `canTransitionTo()` method with valid transition map
- `backend/src/main/java/com/beezhub/student_accommodation/service/RoomAllocationService.java` — add `validateStatusTransition()` calls before all `setStatus()` calls
- `backend/src/main/java/com/beezhub/student_accommodation/exceptions/` — add `InvalidAllocationStatusTransitionException.java` if not reusing existing exception
- `backend/src/main/java/com/beezhub/student_accommodation/exceptions/RestResponseExceptionHandler.java` — map new exception to HTTP 409 CONFLICT

**Implementation Notes:**
- Follow the exact pattern used in `ApplicationStatus.canTransitionTo()` (see `ApplicationStatus.java` lines 46-62 per [ARCH 2.3.3])
- Use `EnumMap<AllocationStatus, Set<AllocationStatus>>` for the transition map for O(1) lookup
- Search the entire codebase for `.setStatus(AllocationStatus.` to find all transition points
- May uncover existing bugs where invalid transitions were occurring silently — these must be fixed
- Reference Requirements.md Diagram 2 (Allocation Status State Machine) for the canonical transition diagram

**Acceptance Criteria:**
- [x] `AllocationStatus.canTransitionTo()` method exists and returns `boolean`
- [x] All 9 statuses have defined valid transitions (including terminal states with empty sets)
- [x] Every allocation status change in the codebase passes through validation before applying
- [x] `InvalidAllocationStatusTransitionException` thrown for invalid transitions
- [x] Exception mapped to HTTP 409 CONFLICT in `RestResponseExceptionHandler`
- [x] No existing functionality broken (all existing tests pass)

**Tests to Write:**
- `AllocationStatusTest.java`: Test all valid transitions return `true`
- `AllocationStatusTest.java`: Test all invalid transitions return `false` (especially terminal → any, and reverse transitions like COMPLETED → OFFERED)
- `AllocationStatusTest.java`: Test every status against every other status (9×9 matrix)
- Integration test: Verify 409 returned when API attempts invalid transition

---

### TASK-1.03: Add AllocationScoringService Unit Tests

- **Status:** DONE
- **Priority:** HIGH
- **Phase:** 1
- **Estimated Scope:** L (4–8 hrs)
- **Architecture Reference:** [ARCH 3.2.6] AllocationScoringService — Test Coverage
- **Requirement References:** [REQ ALLOC-001] (scoring algorithm), [REQ ALLOC-002] (hard constraints), [REQ BR-011] (weighted scoring), [REQ BR-012] (hard constraints)
- **Risk References:** RISK-006 (Scoring algorithm produces incorrect allocations)
- **Dependencies:** None
- **Blocked By:** None

**Description:**
Write comprehensive unit tests for `AllocationScoringService` (630 lines) targeting ≥90% line coverage. The scoring algorithm has three components: Preference Match (40%), Desirability (30%), Accessibility (30%). Each component must be tested independently and in combination. Cover edge cases: missing preferences, tied scores, zero weights, boundary conditions (score 0 and 100), null inputs, empty collections.

**Files to Touch:**
- `backend/src/test/java/com/beezhub/student_accommodation/service/AllocationScoringServiceTest.java` — create or extend with comprehensive tests

**Implementation Notes:**
- Read `AllocationScoringService.java` thoroughly before writing tests — may discover scoring bugs
- Test each scoring component independently:
  1. Preference Match Score: room type match (+40), accommodation type match (+30), building attributes match (+20)
  2. Desirability Score: submission date ordering, year of study priority
  3. Accessibility Score: special needs (+30), building accessibility match (+20)
- Use Mockito to mock `AllocationRuleRepository` and `RoomRepository` dependencies
- Test tie-breaking logic (earlier application submission timestamp wins per [REQ ALLOC-001])
- Test combined scoring with all components
- Run `./gradlew jacocoTestReport` after to verify ≥90% coverage on the scoring service
- Follow test naming convention: `methodName_scenario_expectedResult()` per [ARCH 5.1.1]

**Acceptance Criteria:**
- [x] ≥90% line coverage on `AllocationScoringService` (verified by Jacoco) — achieved 92%
- [x] All 3 scoring components tested independently
- [x] Tie-breaking logic tested
- [x] Edge cases covered: missing preferences, zero weights, boundary scores (0 and 100)
- [x] Null/empty input cases covered
- [x] Test naming follows convention `methodName_scenario_expectedResult()`

**Tests to Write:**
- `calculateScore_perfectMatch_returns100` — all preferences match, all criteria met
- `calculateScore_noMatch_returnsMinimumScore` — no preferences match
- `calculatePreferenceScore_roomTypeMatches_adds40Points`
- `calculatePreferenceScore_accommodationTypeMatches_adds30Points`
- `calculateDesirabilityScore_earlierSubmission_higherScore`
- `calculateDesirabilityScore_graduatingYear_prioritized`
- `calculateAccessibilityScore_specialNeeds_adds30Points`
- `calculateScore_tiedScores_earlierSubmissionWins`
- `calculateScore_nullPreferences_handledGracefully`
- `calculateScore_zeroWeights_returnsZero`
- `calculateScore_customWeightsFromRules_appliedCorrectly`

---

### TASK-1.04: Add Redis to Docker Compose

- **Status:** DONE
- **Priority:** HIGH
- **Phase:** 1
- **Estimated Scope:** S (< 2 hrs)
- **Architecture Reference:** [ARCH 9 Phase 1 Item 4] — prerequisite for JWT blacklist and brute-force protection
- **Requirement References:** [REQ AUTH-003] (token management infrastructure)
- **Risk References:** RISK-001 (JWT token compromise), RISK-002 (brute-force attacks)
- **Dependencies:** None
- **Blocked By:** None

**Description:**
Add a Redis 7.x service to `docker-compose.yml`. Add Spring Data Redis dependency to the backend `build.gradle`. Configure Redis connection properties in `application.yml` with environment variable overrides. Verify Redis connectivity on application startup.

**Files to Touch:**
- `docker-compose.yml` — add Redis service (redis:7-alpine, port 6379, health check, volume for persistence)
- `backend/build.gradle` — add `implementation 'org.springframework.boot:spring-boot-starter-data-redis'`
- `backend/src/main/resources/application.yml` — add `spring.data.redis.host`, `spring.data.redis.port` config with env var defaults
- `.env.example` — add `REDIS_HOST=localhost`, `REDIS_PORT=6379` placeholders

**Implementation Notes:**
- Use `redis:7-alpine` image for minimal footprint
- Add health check: `redis-cli ping` with interval 10s, timeout 5s, retries 3
- Add `depends_on: redis: condition: service_healthy` to the backend service
- Configure Redis with no password for development (add password via env var for production)
- Pin Redis version to `7.4-alpine` or similar specific tag per [ARCH 5.5] (no version ranges)
- Add a volume for Redis data persistence: `redis-data:/data`

**Acceptance Criteria:**
- [x] `docker-compose up` starts Redis alongside other services
- [x] Redis health check passes (container shows healthy)
- [x] Backend can connect to Redis on startup (no connection errors in logs)
- [x] Spring Data Redis dependency resolves and compiles
- [x] `application.yml` has Redis configuration with environment variable overrides
- [x] `.env.example` updated with Redis connection placeholders

**Tests to Write:**
- No automated tests — verify via `docker-compose up` and backend startup logs

---

### TASK-1.05: Implement JWT Token Blacklist (Redis-Based)

- **Status:** DONE
- **Priority:** CRITICAL
- **Phase:** 1
- **Estimated Scope:** L (4–8 hrs)
- **Architecture Reference:** [ARCH 3.2.4] JWT Token Revocation, [ARCH ADR-001]
- **Requirement References:** [REQ AUTH-003] (JWT authentication), [REQ AUTH-007] (session timeout), [REQ BR-030] (token expiry)
- **Risk References:** RISK-001 (JWT token compromise with no revocation)
- **Dependencies:** TASK-1.04 (Redis must be operational)
- **Blocked By:** None

**Description:**
Implement Redis-based JWT token blacklisting. Steps:
1. Add a `jti` (JWT ID) claim to JWT generation in `JwtUtil` — generate UUID for each token
2. Create `TokenBlacklistService` with methods: `blacklistToken(String jti, long remainingTtlSeconds)`, `isBlacklisted(String jti) → boolean`
3. Implement `TokenBlacklistService` using Spring Data Redis `StringRedisTemplate` with TTL matching remaining token lifetime
4. Update `JwtAuthenticationFilter` to extract `jti` from token and check `TokenBlacklistService.isBlacklisted()` before accepting authentication
5. Add blacklist calls to: `AuthService.logout()`, `AuthService.changePassword()`, `UserManagementService.disableUser()`
6. Implement graceful degradation: if Redis is unavailable, accept token with WARNING log (do not block authentication)

**Files to Touch:**
- `backend/src/main/java/com/beezhub/student_accommodation/security/JwtUtil.java` — add `jti` claim generation and extraction
- `backend/src/main/java/com/beezhub/student_accommodation/service/TokenBlacklistService.java` — create new service
- `backend/src/main/java/com/beezhub/student_accommodation/security/JwtAuthenticationFilter.java` — add blacklist check
- `backend/src/main/java/com/beezhub/student_accommodation/service/AuthService.java` — add blacklist calls on logout and password change
- `backend/src/main/java/com/beezhub/student_accommodation/service/UserManagementService.java` — add blacklist call on user disable (if exists)

**Implementation Notes:**
- Use `StringRedisTemplate.opsForValue().set(jti, "blacklisted", ttl, TimeUnit.SECONDS)` for Redis storage
- Calculate remaining TTL from token expiry claim: `tokenExpiry - currentTime`
- Wrap Redis calls in try-catch — Redis unavailability must NOT block authentication per [ARCH ADR-001]
- Log `WARN` when Redis is unavailable: "Redis unavailable, token blacklist check skipped"
- Log `INFO` when token is blacklisted: "Token blacklisted: jti={}, reason={}"
- Follow [ARCH 5.1.1] logging standards — never log the token itself, only the jti

**Acceptance Criteria:**
- [x] JWT tokens include `jti` claim (UUID)
- [x] `TokenBlacklistService` can blacklist and check tokens via Redis
- [x] Blacklisted tokens are rejected with 401 UNAUTHORIZED
- [x] Tokens invalidated within 1 second of logout/password change/disable
- [x] Redis TTL auto-cleans expired blacklist entries
- [x] Redis failure degrades gracefully (tokens accepted, WARNING logged)
- [x] All existing tests pass (backward compatible — existing tokens without jti handled gracefully)

**Tests to Write:**
- `TokenBlacklistServiceTest.blacklistToken_validJti_storedInRedis`
- `TokenBlacklistServiceTest.isBlacklisted_blacklistedToken_returnsTrue`
- `TokenBlacklistServiceTest.isBlacklisted_nonBlacklistedToken_returnsFalse`
- `TokenBlacklistServiceTest.isBlacklisted_redisUnavailable_returnsFalse` (graceful degradation)
- `TokenBlacklistServiceTest.blacklistToken_ttlExpired_autoRemoved`
- `JwtAuthenticationFilterTest.doFilter_blacklistedToken_returns401`
- `AuthServiceTest.logout_validToken_blacklisted`
- `AuthServiceTest.changePassword_existingToken_blacklisted`

---

### TASK-1.06: Implement Brute-Force Protection

- **Status:** DONE
- **Priority:** HIGH
- **Phase:** 1
- **Estimated Scope:** L (4–8 hrs)
- **Architecture Reference:** [ARCH 3.2.5] Brute-Force Protection
- **Requirement References:** [REQ AUTH-003] (authentication), [REQ BR-028] (password rules), [REQ BR-029] (token expiry)
- **Risk References:** RISK-002 (Brute-force credential stuffing attack)
- **Dependencies:** TASK-1.04 (Redis for distributed counters)
- **Blocked By:** None

**Description:**
Implement account lockout after 5 failed login attempts within 15 minutes. Lockout duration: 15 minutes. Use Redis to store per-user failed attempt counters (supports future horizontal scaling). Add `account_locked_until` column to `app_user` table via Flyway migration. Implement rate limiting on `/api/auth/**` endpoints (10 requests per minute per IP). Listen for `AuthenticationFailureBadCredentialsEvent` from Spring Security to increment counters.

**Files to Touch:**
- `backend/src/main/resources/db/migration/V{YYYY_MM_DD_HHMM}__add_account_lockout_fields.sql` — add `account_locked_until TIMESTAMP`, `failed_login_attempts INTEGER DEFAULT 0` to `app_user`
- `backend/src/main/java/com/beezhub/student_accommodation/model/entity/AppUser.java` — add `accountLockedUntil`, `failedLoginAttempts` fields
- `backend/src/main/java/com/beezhub/student_accommodation/service/LoginAttemptService.java` — create new service for tracking failed attempts via Redis
- `backend/src/main/java/com/beezhub/student_accommodation/service/AuthService.java` — check lockout status before authentication, reset counter on success
- `backend/src/main/java/com/beezhub/student_accommodation/config/SecurityConfig.java` — add rate limiting filter for `/api/auth/**`
- `backend/src/main/java/com/beezhub/student_accommodation/exceptions/RestResponseExceptionHandler.java` — map lockout to 429 TOO_MANY_REQUESTS

**Implementation Notes:**
- Use Redis key pattern: `login:failed:{userId}` with TTL 15 minutes for auto-reset
- On successful login, delete the Redis key and reset `failed_login_attempts` to 0 in DB
- On 5th failed attempt, set `account_locked_until = now() + 15 minutes` in DB
- Check `account_locked_until > now()` before allowing login — works even if Redis is down
- Consider using Bucket4j library for IP-based rate limiting (evaluate per [ARCH 5.5] dependency rules)
- Admin endpoint to manually unlock accounts: `PUT /api/admin/users/{id}/unlock`
- Follow Flyway naming convention: `V{YYYY_MM_DD_HHMM}__add_account_lockout_fields.sql` per [ARCH 5.1.3]

**Acceptance Criteria:**
- [x] 6th failed login attempt within 15 minutes returns 429 TOO_MANY_REQUESTS with message "Account temporarily locked"
- [x] Successful login resets failed attempt counter
- [x] Lockout expires automatically after 15 minutes
- [x] Admin can unlock accounts via API
- [x] Lockout status visible in admin user management UI
- [x] Rate limiting active on `/api/auth/**` (10 req/min/IP)
- [x] Flyway migration applies cleanly
- [x] All existing authentication tests pass

**Tests to Write:**
- `LoginAttemptServiceTest.recordFailedAttempt_incrementsCounter`
- `LoginAttemptServiceTest.recordFailedAttempt_5thAttempt_locksAccount`
- `LoginAttemptServiceTest.isLocked_lockedAccount_returnsTrue`
- `LoginAttemptServiceTest.isLocked_lockExpired_returnsFalse`
- `AuthServiceTest.login_lockedAccount_returns429`
- `AuthServiceTest.login_successful_resetsFailedAttempts`
- `AuthServiceTest.login_4thFailedAttempt_stillAllowed`
- Integration test: 6 rapid login failures → 429 response

---

### TASK-1.07: Fix Student Entity EAGER Loading

- **Status:** DONE
- **Priority:** MEDIUM
- **Phase:** 1
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 3.2.7] Student Entity EAGER Loading Fix, [ARCH 2.3.5]
- **Requirement References:** [REQ PROF-001] (student profile), [REQ PROF-003] (application history)
- **Risk References:** RISK-010 (N+1 queries on Student entity degrade performance)
- **Dependencies:** None
- **Blocked By:** None

**Description:**
Change all 3 EAGER-loaded associations on the Student entity to `FetchType.LAZY`: AppUser, Institution, YearOfStudy. `@ManyToOne` defaults to EAGER, so explicit `fetch = FetchType.LAZY` must be added. Then run all tests and fix any `LazyInitializationException` by adding `@EntityGraph` or `JOIN FETCH` to repository methods that need the related data.

**Files to Touch:**
- `backend/src/main/java/com/beezhub/student_accommodation/model/entity/Student.java` — add `fetch = FetchType.LAZY` to all `@ManyToOne` associations
- `backend/src/main/java/com/beezhub/student_accommodation/repository/StudentRepository.java` — add `@EntityGraph` or `JOIN FETCH` queries for methods that need related data
- Potentially other repository/service files that access `student.getAppUser()`, `student.getInstitution()`, `student.getYearOfStudy()` outside transaction scope

**Implementation Notes:**
- Search codebase for `student.getAppUser()`, `student.getInstitution()`, `student.getYearOfStudy()` to identify all access points
- Service methods within `@Transactional` boundaries will work fine — lazy loading is within session
- Controller or mapper methods outside transaction scope may fail — these need `@EntityGraph` on the repository query
- Use `@EntityGraph(attributePaths = {"appUser", "institution", "yearOfStudy"})` for queries that need all related data
- Enable SQL logging temporarily (`spring.jpa.show-sql=true`) to verify no N+1 queries on student list operations
- Test with admin user list, allocation list, and application list endpoints

**Acceptance Criteria:**
- [x] No `FetchType.EAGER` on any `@ManyToOne` in Student entity
- [x] All explicit `fetch = FetchType.LAZY` added
- [x] No `LazyInitializationException` in any test or runtime scenario
- [x] No N+1 queries detectable in SQL logging during: list students, view application, allocation list
- [x] All existing tests pass

**Tests to Write:**
- Run all existing tests — any `LazyInitializationException` failures indicate access points to fix
- Verify student list endpoint returns correct data with lazy-loaded associations
- Verify allocation list endpoint loads student data correctly

---

### TASK-1.08: Remove Duplicate CorsFilter

- **Status:** DONE
- **Priority:** LOW
- **Phase:** 1
- **Estimated Scope:** S (< 2 hrs)
- **Architecture Reference:** [ARCH 2.4] Tight Coupling Analysis (item 4), [ARCH SEC-005]
- **Requirement References:** [REQ AUTH-004] (endpoint protection)
- **Risk References:** None (LOW severity, but reduces configuration confusion)
- **Dependencies:** None
- **Blocked By:** None

**Description:**
Remove the standalone `CorsFilter.java` class. Consolidate all CORS configuration in `SecurityConfig.java`. Verify that the CORS policy in `SecurityConfig` correctly allows the frontend origin(s). Remove the unused port 3000 from allowed origins if not intentional.

**Files to Touch:**
- `backend/src/main/java/com/beezhub/student_accommodation/config/CorsFilter.java` — delete this file
- `backend/src/main/java/com/beezhub/student_accommodation/config/SecurityConfig.java` — verify CORS config is complete and correct; update allowed origins to use environment variable for production flexibility

**Implementation Notes:**
- Before deleting, compare CORS config in `CorsFilter.java` with `SecurityConfig.java` to ensure no headers or methods are lost
- Production should restrict allowed origins to actual frontend domain only
- Use `@Value("${cors.allowed-origins}")` to make origins configurable via environment variable
- Allowed origins should include: `http://localhost:4200` (dev), production URL (env var)
- Remove port 3000 from allowed origins unless it's intentionally used

**Acceptance Criteria:**
- [x] `CorsFilter.java` deleted
- [x] `SecurityConfig.java` has complete CORS configuration
- [x] CORS allowed origins configurable via environment variable
- [x] Frontend requests succeed from `localhost:4200` (tested manually or via existing integration tests)
- [x] No CORS-related errors in browser console during normal operations
- [x] All existing tests pass

**Tests to Write:**
- Test CORS preflight (OPTIONS) request returns correct headers
- Test cross-origin request from allowed origin succeeds
- Test cross-origin request from disallowed origin is rejected

---

### TASK-1.09: Add Composite Database Indexes

- **Status:** DONE
- **Priority:** MEDIUM
- **Phase:** 1
- **Estimated Scope:** S (< 2 hrs)
- **Architecture Reference:** [ARCH 2.5 PB-002] Batch allocation query performance
- **Requirement References:** [REQ ALLOC-003] (batch allocation scheduling), [REQ BR-039] (batch runs every 6 hours)
- **Risk References:** RISK-008 (Batch allocation timeout under load)
- **Dependencies:** None
- **Blocked By:** None

**Description:**
Add composite database indexes to improve batch allocation query performance. The primary index needed is on `application(status, academic_year_id)` for the batch allocation query that fetches all APPROVED applications per cycle. Review other frequently queried columns and add indexes as identified.

**Files to Touch:**
- `backend/src/main/resources/db/migration/V{YYYY_MM_DD_HHMM}__add_composite_indexes.sql` — create new migration with indexes

**Implementation Notes:**
- Primary index: `CREATE INDEX IF NOT EXISTS idx_application_status_year ON application(status, academic_year_id);`
- Consider additional indexes based on common query patterns:
  - `idx_room_allocation_status ON room_allocation(status)` — for expiry job queries
  - `idx_room_allocation_student ON room_allocation(student_id)` — for student allocation lookups
  - `idx_document_application ON document(application_id, status)` — for document approval checks
  - `idx_notification_user ON notification(user_id, status)` — for notification polling
- Use `IF NOT EXISTS` guards for idempotency per [ARCH 5.1.3]
- Follow index naming convention: `idx_{table}_{column1}_{column2}` per [ARCH 5.1.3]

**Acceptance Criteria:**
- [x] Flyway migration applies cleanly
- [x] `idx_application_status_year` index exists on `application(status, academic_year_id)`
- [x] Additional indexes created for frequently queried columns
- [x] Application startup succeeds with new migration
- [x] No existing tests broken

**Tests to Write:**
- No automated tests — verify migration applies cleanly via `./gradlew bootRun` or `docker-compose up`
- Optionally verify via SQL: `SELECT indexname FROM pg_indexes WHERE tablename = 'application';`

---

### TASK-1.10: Increase Async Queue Capacity and Thread Pool

- **Status:** DONE
- **Priority:** MEDIUM
- **Phase:** 1
- **Estimated Scope:** S (< 2 hrs)
- **Architecture Reference:** [ARCH 2.3.4] NotificationService Overloaded, [ARCH 2.5 PB-003]
- **Requirement References:** [REQ BR-034] (async notifications), [REQ BR-035] (retry mechanism), [REQ ALLOC-003] (batch allocation triggers 100+ notifications)
- **Risk References:** RISK-009 (Notification queue exhaustion during batch)
- **Dependencies:** None
- **Blocked By:** None

**Description:**
Increase the async executor queue capacity from 100 to 500 and max threads from 10 to 20 in `AsyncConfiguration.java`. This prevents notification task rejection during batch allocation runs that can trigger 100+ notifications simultaneously. Make pool size configurable via application properties.

**Files to Touch:**
- `backend/src/main/java/com/beezhub/student_accommodation/config/AsyncConfiguration.java` — update core pool size, max pool size, and queue capacity; make configurable via `@Value`
- `backend/src/main/resources/application.yml` — add `async.core-pool-size`, `async.max-pool-size`, `async.queue-capacity` properties with defaults

**Implementation Notes:**
- Change: core pool 5→10, max pool 10→20, queue capacity 100→500
- Use `@Value("${async.queue-capacity:500}")` for configurable defaults
- Add `RejectedExecutionHandler` with `CallerRunsPolicy` as safety net (if queue is full, the calling thread executes the task instead of rejecting)
- Add `setWaitForTasksToCompleteOnShutdown(true)` for graceful shutdown
- Consider adding a separate executor for batch notifications vs. individual notifications (Phase 2 concern, document as future improvement)
- Log WARN when queue utilization exceeds 80%

**Acceptance Criteria:**
- [x] Async executor queue capacity set to 500 (or configurable via property)
- [x] Max thread pool set to 20 (or configurable via property)
- [x] `CallerRunsPolicy` set as rejection handler
- [x] Configuration values externalized to `application.yml`
- [x] Batch allocation of 100+ students does not cause task rejection
- [x] All existing tests pass

**Tests to Write:**
- Verify async configuration loads with correct pool sizes
- Verify batch notification scenario (100 concurrent async tasks) completes without rejection (integration test, optional)

---

## Phase 2: Structural Improvement

*Changes that improve architecture quality, enable multi-tenancy, and reduce technical debt. All Phase 1 gate criteria must be met before starting.*

---

### TASK-2.01: Decompose RoomAllocationService — Extract AllocationExpiryService

- **Status:** TODO
- **Priority:** HIGH
- **Phase:** 2
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 3.2.1] RoomAllocationService Decomposition, [ARCH ADR-003]
- **Requirement References:** [REQ ALLOC-007] (offer expiry), [REQ BR-040] (offer expiry job runs hourly), [REQ BR-041] (check-in expiry job)
- **Risk References:** RISK-005 (RoomAllocationService change causes cascade failure)
- **Dependencies:** TASK-1.02 (AllocationStatus state machine must be in place)
- **Blocked By:** Phase 1 gate

**Description:**
Extract `AllocationExpiryService` from `RoomAllocationService`. This is the first extraction (lowest coupling, easiest to extract per [ARCH 3.2.1]). Move the two scheduled expiry jobs: offer expiry (hourly) and check-in expiry (hourly/daily). Move all related helper methods. Update `@Scheduled` annotations to the new class. Create the new package `service/allocation/`.

**Files to Touch:**
- `backend/src/main/java/com/beezhub/student_accommodation/service/allocation/AllocationExpiryService.java` — create new service
- `backend/src/main/java/com/beezhub/student_accommodation/service/RoomAllocationService.java` — remove expiry methods, delegate to new service
- `backend/src/main/java/com/beezhub/student_accommodation/service/allocation/` — create package

**Implementation Notes:**
- Start with the `@Scheduled` offer expiry method and all methods it calls internally
- Move the check-in expiry `@Scheduled` method and its helpers
- `AllocationExpiryService` should depend on `RoomAllocationRepository`, `NotificationService`, and `AllocationStatus.canTransitionTo()`
- `RoomAllocationService` should NOT call `AllocationExpiryService` — the expiry service is autonomous (scheduled)
- Verify `@Transactional` boundaries move correctly — each scheduled job should be its own transaction
- Run all existing tests after extraction to ensure no regressions
- Target: <300 lines per [ARCH 3.2.1]

**Acceptance Criteria:**
- [ ] `AllocationExpiryService` exists in `service/allocation/` package
- [ ] All expiry-related `@Scheduled` methods moved from `RoomAllocationService`
- [ ] `AllocationExpiryService` < 300 lines
- [ ] Scheduled jobs still trigger on schedule (verify in logs)
- [ ] All existing API contracts unchanged
- [ ] All existing tests pass

**Tests to Write:**
- `AllocationExpiryServiceTest.expireOffers_offersExpired_statusUpdatedToExpired`
- `AllocationExpiryServiceTest.expireOffers_noExpiredOffers_noChanges`
- `AllocationExpiryServiceTest.expireCheckIns_expiredCheckIns_statusUpdated`
- `AllocationExpiryServiceTest.expireOffers_roomOccupancyDecremented`

---

### TASK-2.02: Decompose RoomAllocationService — Extract AllocationLifecycleService

- **Status:** TODO
- **Priority:** HIGH
- **Phase:** 2
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 3.2.1] RoomAllocationService Decomposition, [ARCH ADR-003]
- **Requirement References:** [REQ ALLOC-005] (create allocations), [REQ ALLOC-006] (offer accept/decline), [REQ ALLOC-008] (manual allocation)
- **Risk References:** RISK-005
- **Dependencies:** TASK-2.01 (first extraction done, pattern established)
- **Blocked By:** Phase 1 gate

**Description:**
Extract `AllocationLifecycleService` from `RoomAllocationService`. Move offer accept/decline logic, manual allocation, cancellation, and finalization of staged allocations. This service handles the core allocation lifecycle operations triggered by user actions.

**Files to Touch:**
- `backend/src/main/java/com/beezhub/student_accommodation/service/allocation/AllocationLifecycleService.java` — create new service
- `backend/src/main/java/com/beezhub/student_accommodation/service/RoomAllocationService.java` — remove lifecycle methods, delegate to new service
- `backend/src/main/java/com/beezhub/student_accommodation/controller/` — update controller injection if methods were called directly on `RoomAllocationService`

**Implementation Notes:**
- Move: `acceptOffer()`, `declineOffer()`, `manualAllocate()`, `finalizeAllocation()`, `cancelAllocation()`
- These methods call `AllocationStatus.canTransitionTo()` — ensure TASK-1.02 is done
- Update controller endpoints to use `AllocationLifecycleService` or keep `RoomAllocationService` as thin facade per [ARCH ADR-003]
- Verify transaction boundaries — `acceptOffer` and `declineOffer` should each be their own `@Transactional` method
- Target: <300 lines

**Acceptance Criteria:**
- [ ] `AllocationLifecycleService` exists in `service/allocation/` package
- [ ] Accept/decline/cancel/manual allocate methods extracted
- [ ] `AllocationLifecycleService` < 300 lines
- [ ] All existing API endpoints return same responses (backward compatible)
- [ ] All existing tests pass

**Tests to Write:**
- `AllocationLifecycleServiceTest.acceptOffer_validOffer_statusChangedToAccepted`
- `AllocationLifecycleServiceTest.acceptOffer_expiredOffer_throwsInvalidStatusException`
- `AllocationLifecycleServiceTest.declineOffer_secondDecline_applicationAutoCancelled`
- `AllocationLifecycleServiceTest.manualAllocate_validRoom_allocationCreated`
- `AllocationLifecycleServiceTest.cancelAllocation_activeAllocation_statusCancelled`

---

### TASK-2.03: Decompose RoomAllocationService — Extract AllocationBatchService and AllocationCheckInService

- **Status:** TODO
- **Priority:** HIGH
- **Phase:** 2
- **Estimated Scope:** L (4–8 hrs)
- **Architecture Reference:** [ARCH 3.2.1] RoomAllocationService Decomposition, [ARCH ADR-003]
- **Requirement References:** [REQ ALLOC-003] (batch allocation), [REQ ALLOC-004] (staged allocation review), [REQ BR-039] (batch schedule), [REQ BR-010] (check-in deadline)
- **Risk References:** RISK-005
- **Dependencies:** TASK-2.01, TASK-2.02 (incremental extraction)
- **Blocked By:** Phase 1 gate

**Description:**
Extract the remaining two services from `RoomAllocationService`:
1. `AllocationBatchService` — batch allocation scheduling, eligibility checks, staged allocation creation, scoring orchestration
2. `AllocationCheckInService` — check-in, move-out, termination

After extraction, `RoomAllocationService` should remain as a thin facade (< 200 lines) delegating to the four sub-services for backward compatibility.

**Files to Touch:**
- `backend/src/main/java/com/beezhub/student_accommodation/service/allocation/AllocationBatchService.java` — create
- `backend/src/main/java/com/beezhub/student_accommodation/service/allocation/AllocationCheckInService.java` — create
- `backend/src/main/java/com/beezhub/student_accommodation/service/RoomAllocationService.java` — reduce to thin facade

**Implementation Notes:**
- `AllocationBatchService` depends on `AllocationScoringService` — ensure no circular dependency
- Move `@Scheduled` batch allocation method to `AllocationBatchService`
- `AllocationCheckInService` handles: `checkIn()`, `moveOut()`, `terminateAllocation()`
- Final `RoomAllocationService` should only delegate — no business logic
- Verify no `@Autowired` circular dependencies between the four sub-services
- Run ALL tests after each extraction step
- Target: each service < 300 lines, facade < 200 lines

**Acceptance Criteria:**
- [ ] `AllocationBatchService` and `AllocationCheckInService` exist in `service/allocation/`
- [ ] `RoomAllocationService` is a thin facade (< 200 lines) with no business logic
- [ ] Total of 4 focused services + 1 facade replacing the original God class
- [ ] No circular dependencies between services
- [ ] All existing API contracts unchanged
- [ ] Each service < 300 lines
- [ ] All existing tests pass
- [ ] ≥70% test coverage on each extracted service

**Tests to Write:**
- `AllocationBatchServiceTest.executeBatch_approvedApplications_stagedAllocationsCreated`
- `AllocationBatchServiceTest.executeBatch_noEligible_noAllocations`
- `AllocationCheckInServiceTest.checkIn_acceptedAllocation_statusActive`
- `AllocationCheckInServiceTest.moveOut_activeAllocation_statusCompleted`
- `AllocationCheckInServiceTest.terminate_activeAllocation_statusTerminated`

---

### TASK-2.04: Decompose ApplicationService

- **Status:** TODO
- **Priority:** MEDIUM
- **Phase:** 2
- **Estimated Scope:** L (4–8 hrs)
- **Architecture Reference:** [ARCH 3.2.2] ApplicationService Decomposition
- **Requirement References:** [REQ APP-001] through [REQ APP-005], [REQ BR-001] through [REQ BR-007]
- **Risk References:** None directly, but reduces coupling chain identified in [ARCH 2.4]
- **Dependencies:** None within Phase 2 (parallelizable with TASK-2.01–2.03)
- **Blocked By:** Phase 1 gate

**Description:**
Decompose `ApplicationService` (1,080 lines) into three focused services:
1. `ApplicationService` — student-facing: create, submit, withdraw, view own applications (retained, slimmed)
2. `ApplicationAdminService` — admin-facing: approve, reject, status update, bulk operations
3. `ApplicationSearchService` — search, filtering, pagination using `ApplicationSpecification`

Extract `ApplicationSearchService` first (pure query logic, no side effects). Then extract admin operations.

**Files to Touch:**
- `backend/src/main/java/com/beezhub/student_accommodation/service/application/ApplicationAdminService.java` — create
- `backend/src/main/java/com/beezhub/student_accommodation/service/application/ApplicationSearchService.java` — create
- `backend/src/main/java/com/beezhub/student_accommodation/service/ApplicationService.java` — slim down to student-facing operations (or move to `service/application/` package)
- `backend/src/main/java/com/beezhub/student_accommodation/controller/` — update admin controller to inject `ApplicationAdminService`

**Implementation Notes:**
- Extract search/filter methods first — these are pure queries with no side effects (safest extraction)
- Admin operations share status transition logic with student operations — centralize `ApplicationStatus.canTransitionTo()` usage
- Update all controller injection points — admin controllers should use `ApplicationAdminService`
- Create `service/application/` package following [ARCH 5.1.1] package structure
- Each service < 400 lines per [ARCH 5.1.1]

**Acceptance Criteria:**
- [ ] `ApplicationAdminService` and `ApplicationSearchService` exist in `service/application/`
- [ ] `ApplicationService` contains only student-facing operations and is < 400 lines
- [ ] Each extracted service < 400 lines
- [ ] All API contracts unchanged
- [ ] Status transition validation centralized in `ApplicationStatus.canTransitionTo()`
- [ ] All existing tests pass
- [ ] ≥70% test coverage on each service

**Tests to Write:**
- `ApplicationSearchServiceTest.search_withFilters_returnsFilteredResults`
- `ApplicationSearchServiceTest.search_paginatedResults_correctPageReturned`
- `ApplicationAdminServiceTest.approveApplication_validTransition_statusApproved`
- `ApplicationAdminServiceTest.rejectApplication_validTransition_statusRejected`
- `ApplicationAdminServiceTest.bulkApprove_multipleApplications_allApproved`

---

### TASK-2.05: Decompose NotificationService

- **Status:** TODO
- **Priority:** MEDIUM
- **Phase:** 2
- **Estimated Scope:** L (4–8 hrs)
- **Architecture Reference:** [ARCH 3.2.9] NotificationService Decomposition
- **Requirement References:** [REQ BR-034] through [REQ BR-038] (notification rules)
- **Risk References:** RISK-009 (Notification queue exhaustion)
- **Dependencies:** TASK-2.01 (can begin after first RoomAllocationService extraction)
- **Blocked By:** Phase 1 gate

**Description:**
Decompose `NotificationService` (1,435 lines) into three focused services:
1. `NotificationService` — orchestration: decide what to send, create notification records (retained, slimmed)
2. `EmailDispatchService` — template rendering and SMTP sending
3. `NotificationRetryService` — retry logic and scheduled retry job

Extract retry logic first (self-contained scheduled job). Then extract email dispatch.

**Files to Touch:**
- `backend/src/main/java/com/beezhub/student_accommodation/service/notification/EmailDispatchService.java` — create
- `backend/src/main/java/com/beezhub/student_accommodation/service/notification/NotificationRetryService.java` — create
- `backend/src/main/java/com/beezhub/student_accommodation/service/NotificationService.java` — slim down to orchestration

**Implementation Notes:**
- `NotificationRetryService` should own the `@Scheduled` retry job and all retry/backoff logic
- `EmailDispatchService` wraps the existing `EmailService` with template selection logic
- Preserve `@Async` boundaries — email dispatch must remain non-blocking
- Create `service/notification/` package per [ARCH 5.1.1]
- Each service < 500 lines per [ARCH 3.2.9] acceptance criteria

**Acceptance Criteria:**
- [ ] `EmailDispatchService` and `NotificationRetryService` exist in `service/notification/`
- [ ] `NotificationService` contains only orchestration logic and is < 500 lines
- [ ] Each extracted service < 500 lines
- [ ] Batch allocation of 100+ students does not exhaust notification queue
- [ ] Retry logic independently testable
- [ ] All existing notification functionality preserved
- [ ] All existing tests pass

**Tests to Write:**
- `EmailDispatchServiceTest.dispatch_validTemplate_emailSent`
- `EmailDispatchServiceTest.dispatch_smtpFailure_exceptionPropagated`
- `NotificationRetryServiceTest.retryFailed_failedNotification_retried`
- `NotificationRetryServiceTest.retryFailed_maxRetriesExceeded_markedPermanentlyFailed`
- `NotificationRetryServiceTest.retryFailed_exponentialBackoff_correctDelays`

---

### TASK-2.06: Implement Domain Events for Notification Decoupling

- **Status:** TODO
- **Priority:** MEDIUM
- **Phase:** 2
- **Estimated Scope:** L (4–8 hrs)
- **Architecture Reference:** [ARCH 2.4] Tight Coupling Analysis, [ARCH ADR-005] Domain Events for Side Effects
- **Requirement References:** [REQ ALLOC-005] (allocation + notification), [REQ DOC-003] (document approval triggers)
- **Risk References:** None directly — reduces coupling risk
- **Dependencies:** TASK-2.01 through TASK-2.05 (decomposition should be complete before decoupling)
- **Blocked By:** Phase 1 gate

**Description:**
Implement Spring Application Events (`ApplicationEventPublisher`) for side effects to decouple the notification triggering chains identified in [ARCH 2.4]:
1. Create domain event classes: `AllocationCreatedEvent`, `AllocationAcceptedEvent`, `AllocationDeclinedEvent`, `AllocationExpiredEvent`, `DocumentApprovedEvent`, `ApplicationSubmittedEvent`, `ApplicationStatusChangedEvent`
2. Publish events from business services (allocation, application, document) instead of directly calling `NotificationService`
3. Create `@EventListener` methods in `NotificationService` to listen for events and trigger notifications
4. Create `@EventListener` in `ActivityLogService` for audit logging of events

**Files to Touch:**
- `backend/src/main/java/com/beezhub/student_accommodation/model/event/` — create package with event classes
- `backend/src/main/java/com/beezhub/student_accommodation/service/allocation/AllocationLifecycleService.java` — publish events instead of calling NotificationService directly
- `backend/src/main/java/com/beezhub/student_accommodation/service/allocation/AllocationBatchService.java` — publish events
- `backend/src/main/java/com/beezhub/student_accommodation/service/ApplicationService.java` — publish events
- `backend/src/main/java/com/beezhub/student_accommodation/service/DocumentService.java` — publish events
- `backend/src/main/java/com/beezhub/student_accommodation/service/NotificationService.java` — add `@EventListener` methods

**Implementation Notes:**
- Use `ApplicationEventPublisher.publishEvent()` — injected via constructor
- Event listeners run synchronously by default within the publishing transaction; use `@Async` on listeners for non-blocking side effects
- Use `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` for notifications — ensures event fires only after business transaction commits
- Event handlers MUST be idempotent per [ARCH ADR-005]
- Start by replacing one coupling chain (e.g., Allocation → Notification) and verify, then expand
- Keep direct calls as fallback during migration — remove once all events are verified

**Acceptance Criteria:**
- [ ] Domain event classes created for all major business actions
- [ ] Business services publish events instead of calling NotificationService directly
- [ ] NotificationService listens for events and sends appropriate notifications
- [ ] No direct NotificationService calls from business services (allocation, application, document)
- [ ] All notifications still sent correctly (same emails for same business actions)
- [ ] Event listeners are idempotent
- [ ] All existing tests pass

**Tests to Write:**
- `AllocationLifecycleServiceTest.acceptOffer_publishesAllocationAcceptedEvent`
- `NotificationEventListenerTest.onAllocationCreated_sendsOfferEmail`
- `NotificationEventListenerTest.onDocumentApproved_sendsApprovalNotification`
- `NotificationEventListenerTest.onApplicationSubmitted_sendsConfirmationEmail`
- Integration test: verify full event chain from allocation to notification email

---

### TASK-2.07: Implement Shared-Schema Multi-Tenancy — Database Schema

- **Status:** TODO
- **Priority:** HIGH
- **Phase:** 2
- **Estimated Scope:** L (4–8 hrs)
- **Architecture Reference:** [ARCH 3.2.10] Multi-Tenancy Implementation, [ARCH ADR-002]
- **Requirement References:** [REQ BR-022] (building names unique within institution/tenant)
- **Risk References:** RISK-007 (Cross-tenant data leakage)
- **Dependencies:** Phase 1 complete
- **Blocked By:** Phase 1 gate

**Description:**
Create the database schema for multi-tenancy:
1. Create `tenant` table (id, name, domain, config JSON, created_at, updated_at)
2. Add `tenant_id` foreign key column to all tenant-scoped tables: `building`, `room`, `application`, `room_allocation`, `staged_allocation`, `document`, `notification`, `user_notification`, `system_setting`, `allocation_rule`, `waiting_list_entry`, `activity_log`, `academic_year`
3. Create a default tenant for existing data migration
4. Backfill `tenant_id` on all existing records with the default tenant ID
5. Add NOT NULL constraint on `tenant_id` after backfill

**Files to Touch:**
- `backend/src/main/resources/db/migration/V{YYYY_MM_DD_HHMM}__create_tenant_table.sql` — create tenant table
- `backend/src/main/resources/db/migration/V{YYYY_MM_DD_HHMM}__add_tenant_id_columns.sql` — add tenant_id FK to all scoped tables
- `backend/src/main/resources/db/migration/V{YYYY_MM_DD_HHMM}__backfill_default_tenant.sql` — insert default tenant and backfill existing data

**Implementation Notes:**
- Split into multiple migrations per [ARCH 5.1.3] (one migration per logical change)
- `tenant` table schema: `id BIGSERIAL PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE, domain VARCHAR(255), config JSONB, is_active BOOLEAN DEFAULT true, created_at TIMESTAMP DEFAULT NOW(), updated_at TIMESTAMP DEFAULT NOW()`
- Add index `idx_{table}_tenant_id` on every tenant_id column for query performance
- Backfill migration: `INSERT INTO tenant (name) VALUES ('Default Tenant'); UPDATE building SET tenant_id = 1;` etc.
- Add NOT NULL constraint AFTER backfill to avoid migration failures on existing data
- Use `IF NOT EXISTS` guards per [ARCH 5.1.3]

**Acceptance Criteria:**
- [ ] `tenant` table created with correct schema
- [ ] `tenant_id` column exists on all ~15 tenant-scoped tables
- [ ] Default tenant created and all existing records backfilled
- [ ] `tenant_id` has NOT NULL constraint on all tables
- [ ] Foreign key constraints from tenant-scoped tables to `tenant` table
- [ ] Indexes on all `tenant_id` columns
- [ ] All migrations apply cleanly in sequence
- [ ] Application starts successfully after migration

**Tests to Write:**
- Verify migration applies cleanly via `./gradlew bootRun` or `docker-compose up`
- Verify default tenant exists and all existing records have `tenant_id` set

---

### TASK-2.08: Implement Shared-Schema Multi-Tenancy — Application Layer

- **Status:** TODO
- **Priority:** HIGH
- **Phase:** 2
- **Estimated Scope:** XL (8+ hrs — has subtasks)
- **Architecture Reference:** [ARCH 3.2.10] Multi-Tenancy Implementation, [ARCH ADR-002]
- **Requirement References:** [REQ BR-022], [REQ INST-001]
- **Risk References:** RISK-007 (Cross-tenant data leakage)
- **Dependencies:** TASK-2.07 (database schema must exist)
- **Blocked By:** TASK-2.07

#### TASK-2.08a: Implement TenantContext and JWT Integration

**Description:**
Create `TenantContext` using `ThreadLocal` to hold the current tenant ID. Update `JwtUtil` to include `tenantId` claim in JWT tokens. Update `JwtAuthenticationFilter` to extract `tenantId` from JWT and set in `TenantContext`. Create `Tenant` entity and `TenantRepository`.

**Files to Touch:**
- `backend/src/main/java/com/beezhub/student_accommodation/service/tenant/TenantContext.java` — create ThreadLocal-based context
- `backend/src/main/java/com/beezhub/student_accommodation/model/entity/Tenant.java` — create entity
- `backend/src/main/java/com/beezhub/student_accommodation/repository/TenantRepository.java` — create repository
- `backend/src/main/java/com/beezhub/student_accommodation/security/JwtUtil.java` — add tenantId claim
- `backend/src/main/java/com/beezhub/student_accommodation/security/JwtAuthenticationFilter.java` — extract tenantId, set in TenantContext

#### TASK-2.08b: Implement Hibernate Tenant Filtering

**Description:**
Implement automatic tenant filtering using Hibernate `@FilterDef` and `@Filter` annotations on all tenant-scoped entities. Or alternatively, create a base `TenantAwareSpecification` that auto-appends tenant predicate to all JPA Specification queries. Enable the filter in a `TenantFilterInterceptor` that runs before each request.

**Files to Touch:**
- All tenant-scoped entity classes — add `tenantId` field and `@Filter`/`@FilterDef` annotations
- `backend/src/main/java/com/beezhub/student_accommodation/config/TenantFilterConfig.java` — create interceptor to enable Hibernate filter per request

#### TASK-2.08c: Update Admin Endpoints for Tenant Scoping

**Description:**
Update all admin endpoints to enforce tenant scoping. Admin users should only see data for their tenant. Super-admin functionality for cross-tenant access (future enhancement — flag but don't implement).

**Files to Touch:**
- All admin controller classes — verify tenant context is applied
- All admin service classes — verify queries are tenant-scoped

**Acceptance Criteria (for all subtasks):**
- [ ] `TenantContext` holds current tenant ID per request
- [ ] JWT tokens include `tenantId` claim
- [ ] Hibernate filter automatically applies `tenant_id = :tenantId` to all tenant-scoped queries
- [ ] No cross-tenant data leakage (admin of Tenant A cannot see Tenant B data)
- [ ] All list/search endpoints return only tenant-scoped data
- [ ] TenantContext automatically applied — developers cannot accidentally bypass it
- [ ] ThreadLocal cleaned up after each request (prevent memory leaks)
- [ ] All existing tests pass (using default tenant)

**Tests to Write:**
- `TenantContextTest.setTenantId_getTenantId_returnsCorrectValue`
- `TenantContextTest.clear_afterSet_returnsNull`
- `TenantFilterTest.listBuildings_tenant1_onlyTenant1BuildingsReturned`
- `TenantFilterTest.listBuildings_tenant2_onlyTenant2BuildingsReturned`
- `TenantFilterTest.createBuilding_tenantIdAutoSet`
- Integration test: Two tenants with separate buildings — verify complete isolation

---

### TASK-2.09: Implement FileStorageService Interface with Orphan Cleanup

- **Status:** TODO
- **Priority:** MEDIUM
- **Phase:** 2
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 3.4.1] MinIO File Storage — Orphan Cleanup
- **Requirement References:** [REQ DOC-001] (document upload), [REQ BR-050] (file storage URLs)
- **Risk References:** RISK-011 (MinIO orphaned files accumulate storage)
- **Dependencies:** None within Phase 2
- **Blocked By:** Phase 1 gate

**Description:**
Create `FileStorageService` interface abstracting MinIO operations. Implement `MinioFileStorageService` as the concrete implementation. Create `OrphanFileCleanupJob` that runs daily to detect files in MinIO without matching database records and remove them. Wrap MinIO calls with try-catch and compensating delete on DB failure.

**Files to Touch:**
- `backend/src/main/java/com/beezhub/student_accommodation/service/document/FileStorageService.java` — create interface
- `backend/src/main/java/com/beezhub/student_accommodation/service/document/MinioFileStorageService.java` — create implementation wrapping existing MinIO logic
- `backend/src/main/java/com/beezhub/student_accommodation/service/document/OrphanFileCleanupJob.java` — create scheduled cleanup job
- `backend/src/main/java/com/beezhub/student_accommodation/service/DocumentService.java` — refactor to use `FileStorageService` interface instead of direct MinIO calls

**Implementation Notes:**
- Interface methods: `upload(String key, InputStream content, String contentType) → String url`, `download(String key) → InputStream`, `delete(String key)`, `exists(String key) → boolean`, `generatePresignedUrl(String key, Duration expiry) → String`
- Orphan cleanup: list all MinIO objects, cross-reference with `document` table, delete objects with no matching record
- Schedule cleanup daily at 3 AM (low traffic): `@Scheduled(cron = "0 0 3 * * *")`
- On file upload: if DB transaction fails after MinIO upload, catch exception and delete from MinIO
- This abstraction enables future cloud migration (AWS S3, Azure Blob) per [ARCH 3.4.1]

**Acceptance Criteria:**
- [ ] `FileStorageService` interface defined
- [ ] `MinioFileStorageService` implements interface and wraps all MinIO operations
- [ ] `DocumentService` uses `FileStorageService` interface (not direct MinIO calls)
- [ ] `OrphanFileCleanupJob` runs daily and removes orphaned files
- [ ] Compensating delete on DB failure after successful MinIO upload
- [ ] All existing document upload/download functionality preserved
- [ ] All existing tests pass

**Tests to Write:**
- `MinioFileStorageServiceTest.upload_validFile_returnUrl`
- `MinioFileStorageServiceTest.delete_existingFile_removed`
- `OrphanFileCleanupJobTest.cleanup_orphanedFiles_deleted`
- `OrphanFileCleanupJobTest.cleanup_noOrphans_noDeletes`
- `DocumentServiceTest.uploadDocument_dbFailure_minioFileDeleted`

---

### TASK-2.10: Implement FileValidationService Interface (No-Op)

- **Status:** TODO
- **Priority:** MEDIUM
- **Phase:** 2
- **Estimated Scope:** S (< 2 hrs)
- **Architecture Reference:** [ARCH 3.4.2] File Upload Security — Malware Scanning
- **Requirement References:** [REQ DOC-001] (document upload validation), [REQ BR-018] (file size), [REQ BR-019] (file formats)
- **Risk References:** RISK-012 (Malicious file upload via documents)
- **Dependencies:** TASK-2.09 (FileStorageService must exist to integrate with upload flow)
- **Blocked By:** TASK-2.09

**Description:**
Create `FileValidationService` interface with `scanFile(InputStream input, String filename) → ValidationResult` method. Implement `NoOpFileValidationService` as initial pass-through implementation. Wire into the upload flow in `DocumentService` so all files route through validation before MinIO storage. ClamAV integration to be done as a future enhancement.

**Files to Touch:**
- `backend/src/main/java/com/beezhub/student_accommodation/service/document/FileValidationService.java` — create interface
- `backend/src/main/java/com/beezhub/student_accommodation/service/document/NoOpFileValidationService.java` — create no-op implementation
- `backend/src/main/java/com/beezhub/student_accommodation/service/DocumentService.java` — inject and call `FileValidationService` before upload

**Implementation Notes:**
- `ValidationResult` record: `boolean clean`, `String reason` (null if clean)
- No-op implementation: always returns `ValidationResult(true, null)`
- Add file type verification by magic bytes (not just extension) as part of validation
- Future: swap `NoOpFileValidationService` with `ClamAvFileValidationService` via profile or config
- Use `@ConditionalOnMissingBean` pattern so ClamAV implementation can override

**Acceptance Criteria:**
- [ ] `FileValidationService` interface defined
- [ ] `NoOpFileValidationService` implements interface (always passes)
- [ ] All file uploads routed through `FileValidationService` before storage
- [ ] No functional change to existing upload behavior
- [ ] All existing tests pass

**Tests to Write:**
- `NoOpFileValidationServiceTest.scanFile_anyFile_returnsClean`
- `DocumentServiceTest.uploadDocument_validationCalled_beforeStorage`

---

### TASK-2.11: Implement Frontend TokenStorageService Abstraction

- **Status:** TODO
- **Priority:** MEDIUM
- **Phase:** 2
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 3.4.3] Frontend Token Storage
- **Requirement References:** [REQ AUTH-003] (JWT storage), [REQ AUTH-007] (session timeout)
- **Risk References:** RISK-013 (XSS attack steals JWT from localStorage)
- **Dependencies:** None within Phase 2
- **Blocked By:** Phase 1 gate

**Description:**
Create `TokenStorageService` abstraction in Angular to encapsulate all token read/write operations. Currently, JWT is stored directly in `localStorage` across multiple components. Consolidate all `localStorage.getItem('token')` / `localStorage.setItem('token', ...)` calls through a single service. Current implementation remains localStorage but behind the abstraction. Add Content Security Policy headers to backend responses to mitigate XSS risk.

**Files to Touch:**
- `frontend/src/app/core/services/token-storage.service.ts` — create service with `getToken()`, `setToken()`, `removeToken()`, `isTokenValid()` methods
- `frontend/src/app/core/services/auth.service.ts` — refactor to use `TokenStorageService` instead of direct localStorage
- `frontend/src/app/core/interceptors/auth.interceptor.ts` — refactor to use `TokenStorageService`
- `frontend/src/app/core/interceptors/token-expiry.interceptor.ts` — refactor to use `TokenStorageService`
- `frontend/src/app/core/guards/` — refactor guards to use `TokenStorageService`
- `backend/src/main/java/com/beezhub/student_accommodation/config/SecurityConfig.java` — add Content-Security-Policy header

**Implementation Notes:**
- Search frontend codebase for `localStorage` to find all access points
- `TokenStorageService` methods: `getToken(): string | null`, `setToken(token: string): void`, `removeToken(): void`, `isTokenValid(): boolean` (check expiry)
- Current implementation: localStorage. Future: httpOnly cookie or in-memory with refresh token
- Add CSP header: `Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'`
- Use `inject()` function for DI per [ARCH 5.1.2]

**Acceptance Criteria:**
- [ ] `TokenStorageService` created with all token operations
- [ ] No direct `localStorage` access for tokens anywhere in codebase
- [ ] All components use `TokenStorageService` for token operations
- [ ] Content-Security-Policy header added to backend responses
- [ ] All existing frontend behavior unchanged
- [ ] All existing tests pass

**Tests to Write:**
- `TokenStorageService.getToken_tokenExists_returnsToken`
- `TokenStorageService.getToken_noToken_returnsNull`
- `TokenStorageService.removeToken_tokenExists_tokenRemoved`
- `TokenStorageService.isTokenValid_expiredToken_returnsFalse`

---

### TASK-2.12: Add Jacoco Coverage Thresholds

- **Status:** TODO
- **Priority:** MEDIUM
- **Phase:** 2
- **Estimated Scope:** S (< 2 hrs)
- **Architecture Reference:** [ARCH 5.1.1] Backend Coding Standards (Test Requirements)
- **Requirement References:** None directly
- **Risk References:** None directly — enforces quality gates
- **Dependencies:** TASK-2.01 through TASK-2.05 (services must be decomposed before setting per-service thresholds)
- **Blocked By:** None

**Description:**
Configure Jacoco in `build.gradle` to enforce minimum 70% line coverage on all service classes and 90% on `AllocationScoringService`. Add `jacocoTestCoverageVerification` task to the build pipeline. Ensure coverage reports are generated and threshold violations fail the build.

**Files to Touch:**
- `backend/build.gradle` — configure Jacoco coverage verification rules with per-class thresholds

**Implementation Notes:**
- Use `jacocoTestCoverageVerification` with rules per class/package
- Global minimum: 70% line coverage on `com.beezhub.student_accommodation.service.**`
- Specific: 90% on `AllocationScoringService`
- Exclude from coverage: entities, DTOs, configuration classes, mappers (auto-generated)
- Add `check.dependsOn jacocoTestCoverageVerification` so `./gradlew check` enforces coverage
- Run `./gradlew jacocoTestCoverageVerification` to verify current coverage meets thresholds

**Acceptance Criteria:**
- [ ] Jacoco coverage verification configured in `build.gradle`
- [ ] 70% minimum line coverage enforced on all service classes
- [ ] 90% minimum enforced on `AllocationScoringService`
- [ ] Entities, DTOs, config classes excluded from coverage
- [ ] `./gradlew check` fails if coverage thresholds not met
- [ ] Current codebase passes coverage thresholds (may require additional tests)

**Tests to Write:**
- No new tests — verify by running `./gradlew jacocoTestCoverageVerification`

---

### TASK-2.13: Increase HikariCP Connection Pool for Production

- **Status:** TODO
- **Priority:** MEDIUM
- **Phase:** 2
- **Estimated Scope:** S (< 2 hrs)
- **Architecture Reference:** [ARCH 2.5 PB-006] HikariCP pool sizing
- **Requirement References:** [REQ NFR — Scalability] 500 concurrent users target
- **Risk References:** RISK-014 (HikariCP pool exhaustion under load)
- **Dependencies:** None
- **Blocked By:** None

**Description:**
Increase HikariCP pool configuration from min 5/max 10 to min 10/max 30 for production. Make pool sizes configurable via environment variables. Add connection pool monitoring via Spring Actuator metrics.

**Files to Touch:**
- `backend/src/main/resources/application.yml` — update `spring.datasource.hikari.minimum-idle` and `maximum-pool-size` with env var overrides
- `.env.example` — add `HIKARI_MIN_IDLE=10`, `HIKARI_MAX_POOL_SIZE=30`

**Implementation Notes:**
- Development: min 5, max 10 (keep existing defaults for local dev)
- Production: min 10, max 30 (via environment variables)
- Use `${HIKARI_MIN_IDLE:5}` and `${HIKARI_MAX_POOL_SIZE:10}` for backward-compatible defaults
- Add `connection-timeout: 30000` and `leak-detection-threshold: 60000` for production monitoring
- Monitor via `/actuator/metrics/hikaricp.connections.active` and `hikaricp.connections.pending`

**Acceptance Criteria:**
- [ ] HikariCP pool sizes configurable via environment variables
- [ ] Production-ready defaults documented in `.env.example`
- [ ] Connection timeout and leak detection configured
- [ ] Application starts successfully with new pool settings
- [ ] All existing tests pass

**Tests to Write:**
- No automated tests — verify via Actuator metrics endpoint

---

## Phase 3: Enhancement & Optimization

*Changes that improve performance, observability, and operational readiness. All Phase 2 gate criteria must be met before starting.*

---

### TASK-3.01: Add Structured Logging (JSON Format with MDC)

- **Status:** TODO
- **Priority:** MEDIUM
- **Phase:** 3
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 6.4] Observability — Logging
- **Requirement References:** [REQ BR-045] (admin action logging)
- **Risk References:** None directly — improves operational capability
- **Dependencies:** TASK-2.08 (multi-tenancy for tenant ID in MDC)
- **Blocked By:** Phase 2 gate

**Description:**
Implement structured JSON logging using Logback JSON encoder. Add MDC (Mapped Diagnostic Context) for request ID, user ID, and tenant ID. Create a `RequestLoggingFilter` that sets MDC values at the start of each request and clears them at the end. All log output should be JSON-formatted in production for log aggregation tools.

**Files to Touch:**
- `backend/src/main/resources/logback-spring.xml` — create or update with JSON encoder for production profile
- `backend/src/main/java/com/beezhub/student_accommodation/config/RequestLoggingFilter.java` — create filter that sets MDC with requestId (UUID), userId, tenantId
- `backend/build.gradle` — add Logback JSON encoder dependency (e.g., `logstash-logback-encoder`)

**Implementation Notes:**
- Use `net.logstash.logback:logstash-logback-encoder` for JSON formatting
- MDC fields: `requestId` (UUID generated per request), `userId` (from JWT), `tenantId` (from TenantContext)
- Use Spring Profile: JSON logging in `production` profile, standard console in `dev` profile
- Register filter with high priority (before business filters) to ensure MDC is available everywhere
- Clean up MDC in `finally` block to prevent ThreadLocal leaks
- Follow [ARCH 5.1.1] logging standards

**Acceptance Criteria:**
- [ ] Log output is JSON-formatted in production profile
- [ ] Every log line includes requestId, userId (if authenticated), tenantId
- [ ] MDC cleaned up after each request
- [ ] Standard console logging preserved for development
- [ ] All existing tests pass

**Tests to Write:**
- `RequestLoggingFilterTest.doFilter_authenticatedUser_mdcContainsUserId`
- `RequestLoggingFilterTest.doFilter_afterCompletion_mdcCleared`

---

### TASK-3.02: Add Micrometer Business Metrics

- **Status:** TODO
- **Priority:** MEDIUM
- **Phase:** 3
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 6.4] Observability — Metrics
- **Requirement References:** [REQ ALLOC-003] (batch allocation monitoring)
- **Risk References:** None directly — improves observability
- **Dependencies:** None within Phase 3
- **Blocked By:** Phase 2 gate

**Description:**
Add Micrometer counters and timers for key business metrics: allocation counts (created/accepted/declined/expired), email send rates (success/failure), batch allocation duration, login attempt counts, and async queue depth. Expose via Spring Actuator `/actuator/metrics` endpoint.

**Files to Touch:**
- `backend/src/main/java/com/beezhub/student_accommodation/service/allocation/AllocationLifecycleService.java` — add counters for allocation events
- `backend/src/main/java/com/beezhub/student_accommodation/service/allocation/AllocationBatchService.java` — add timer for batch duration
- `backend/src/main/java/com/beezhub/student_accommodation/service/notification/EmailDispatchService.java` — add counters for email success/failure
- `backend/src/main/java/com/beezhub/student_accommodation/service/AuthService.java` — add counter for login attempts
- `backend/src/main/resources/application.yml` — enable metrics endpoint in Actuator

**Implementation Notes:**
- Use `MeterRegistry` injected via constructor
- Counter names follow convention: `sas.allocations.created`, `sas.allocations.accepted`, `sas.emails.sent`, `sas.emails.failed`, `sas.auth.login.success`, `sas.auth.login.failure`
- Timer: `sas.batch.allocation.duration`
- Gauge: `sas.async.queue.size` (expose ThreadPoolTaskExecutor queue size)
- Tag metrics with tenant ID for multi-tenant filtering
- Enable Actuator metrics endpoint: `management.endpoints.web.exposure.include: health,info,metrics`

**Acceptance Criteria:**
- [ ] Business metrics registered with Micrometer
- [ ] Metrics available at `/actuator/metrics/sas.*`
- [ ] Allocation, email, batch, and auth metrics tracked
- [ ] Async queue depth visible as gauge
- [ ] All existing tests pass

**Tests to Write:**
- `AllocationLifecycleServiceTest.acceptOffer_success_counterIncremented`
- `AllocationBatchServiceTest.executeBatch_duration_timerRecorded`

---

### TASK-3.03: Implement WebSocket/SSE for Real-Time Notifications

- **Status:** TODO
- **Priority:** LOW
- **Phase:** 3
- **Estimated Scope:** L (4–8 hrs)
- **Architecture Reference:** [ARCH 3.4.4] Real-Time Notifications, [ARCH 2.5 PB-004]
- **Requirement References:** [REQ BR-037] (offer expiry reminders), [REQ BR-038] (critical notifications)
- **Risk References:** None directly
- **Dependencies:** TASK-2.05 (NotificationService decomposed)
- **Blocked By:** Phase 2 gate

**Description:**
Implement Server-Sent Events (SSE) or WebSocket endpoint for real-time notification push from backend to frontend. Replace the current 30-second polling with a persistent connection. Add polling fallback for clients that don't support SSE/WebSocket.

**Files to Touch:**
- `backend/src/main/java/com/beezhub/student_accommodation/controller/NotificationSseController.java` — create SSE endpoint
- `backend/src/main/java/com/beezhub/student_accommodation/config/SecurityConfig.java` — allow SSE endpoint
- `frontend/src/app/core/services/notification.service.ts` — update to use SSE with polling fallback

**Implementation Notes:**
- SSE is simpler than WebSocket and sufficient for one-way server→client push
- Backend: `SseEmitter` with per-user registration. On notification event, push to user's emitter.
- Frontend: `EventSource` API with automatic reconnection
- Keep polling as fallback: if SSE connection fails, revert to 30-second polling
- JWT authentication for SSE: pass token as query parameter (SSE doesn't support custom headers)
- Clean up emitters on user disconnect/logout
- Consider Redis Pub/Sub for multi-instance notification distribution

**Acceptance Criteria:**
- [ ] SSE endpoint available for real-time notifications
- [ ] Frontend receives notifications in real-time (< 1 second latency)
- [ ] Polling fallback works when SSE unavailable
- [ ] SSE connection authenticated via JWT
- [ ] Emitters cleaned up on disconnect
- [ ] All existing notification functionality preserved

**Tests to Write:**
- `NotificationSseControllerTest.subscribe_validToken_emitterCreated`
- `NotificationSseControllerTest.pushNotification_subscribedUser_receivesEvent`
- Frontend: verify SSE connection established and messages received

---

### TASK-3.04: Conduct Load Testing (500 Concurrent Users)

- **Status:** TODO
- **Priority:** MEDIUM
- **Phase:** 3
- **Estimated Scope:** L (4–8 hrs)
- **Architecture Reference:** [ARCH 6.1] Performance, [ARCH 6.2] Scalability
- **Requirement References:** [REQ NFR — Performance] P95 API response < 500ms
- **Risk References:** RISK-008 (Batch allocation timeout), RISK-014 (HikariCP exhaustion)
- **Dependencies:** TASK-2.13 (HikariCP pool sized for load), TASK-1.09 (indexes in place)
- **Blocked By:** Phase 2 gate

**Description:**
Create and execute load test suite using k6 or JMeter targeting 500 concurrent users. Test scenarios: authentication flow, application submission, document upload, allocation operations, notification polling. Identify bottlenecks and capture metrics for P95 response time, error rate, throughput, and connection pool utilization.

**Files to Touch:**
- `tools/load-tests/` — create directory with load test scripts
- `tools/load-tests/k6-auth.js` — authentication load test
- `tools/load-tests/k6-application.js` — application submission load test
- `tools/load-tests/k6-allocation.js` — allocation operations load test
- `tools/load-tests/README.md` — instructions for running load tests

**Implementation Notes:**
- Use k6 (open-source, scriptable, good reporting) — install via npm or binary
- Scenario: ramp from 0 to 500 VUs over 5 minutes, sustain for 10 minutes, ramp down 2 minutes
- Key metrics to capture: http_req_duration (P95 < 500ms for CRUD, < 2s for reports), http_req_failed (< 1%), iterations per second
- Test against Docker Compose deployment with production-like pool sizes
- Profile batch allocation specifically with 500+ applications
- Document findings and bottlenecks in test report

**Acceptance Criteria:**
- [ ] Load test scripts created for major user flows
- [ ] Tests execute successfully against Docker Compose deployment
- [ ] P95 API response < 500ms for standard CRUD operations
- [ ] Error rate < 1% under 500 concurrent users
- [ ] No connection pool exhaustion
- [ ] Test results documented with findings

**Tests to Write:**
- Load test scripts ARE the tests — not unit tests
- Document results in `tools/load-tests/RESULTS.md`

---

### TASK-3.05: Implement Activity Log Table Partitioning

- **Status:** TODO
- **Priority:** LOW
- **Phase:** 3
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 2.5 PB-007] Activity log growth
- **Requirement References:** [REQ BR-045] (admin action logging)
- **Risk References:** None (LOW, growing risk)
- **Dependencies:** None within Phase 3
- **Blocked By:** Phase 2 gate

**Description:**
Implement table partitioning on `activity_log` by year to prevent unbounded growth from degrading query performance. Create a Flyway migration to convert the existing table to a partitioned table. Set up automatic partition creation for new years.

**Files to Touch:**
- `backend/src/main/resources/db/migration/V{YYYY_MM_DD_HHMM}__partition_activity_log.sql` — migration to partition table

**Implementation Notes:**
- Use PostgreSQL declarative partitioning: `PARTITION BY RANGE (created_at)`
- Create partitions for current year and next year
- Partitioned table requires primary key to include partition key: `PRIMARY KEY (id, created_at)`
- Add a scheduled task or migration to create next year's partition before year-end
- Existing data must be migrated to the partitioned table — create new partitioned table, copy data, rename
- Test with empty and populated tables

**Acceptance Criteria:**
- [ ] `activity_log` table is partitioned by year
- [ ] Existing data preserved in correct partitions
- [ ] Queries on `activity_log` with date filters use partition pruning
- [ ] Flyway migration applies cleanly
- [ ] Application functions normally with partitioned table

**Tests to Write:**
- Verify migration applies cleanly
- Verify activity log queries still return correct data
- Verify partition pruning via `EXPLAIN ANALYZE` on date-filtered queries

---

### TASK-3.06: Add Custom Health Indicators

- **Status:** TODO
- **Priority:** LOW
- **Phase:** 3
- **Estimated Scope:** S (< 2 hrs)
- **Architecture Reference:** [ARCH 6.4] Observability — Health Checks
- **Requirement References:** None directly
- **Risk References:** None directly — improves operational monitoring
- **Dependencies:** TASK-1.04 (Redis must exist for Redis health indicator)
- **Blocked By:** Phase 2 gate

**Description:**
Add custom Spring Boot Health Indicators for MinIO, SMTP, and Redis connectivity. These supplement the default PostgreSQL health check already provided by Spring Actuator. Restrict Actuator endpoints to ADMIN role except `/actuator/health` (public for container health checks).

**Files to Touch:**
- `backend/src/main/java/com/beezhub/student_accommodation/config/health/MinioHealthIndicator.java` — create
- `backend/src/main/java/com/beezhub/student_accommodation/config/health/SmtpHealthIndicator.java` — create
- `backend/src/main/java/com/beezhub/student_accommodation/config/health/RedisHealthIndicator.java` — create (or rely on Spring's auto-configured one)
- `backend/src/main/java/com/beezhub/student_accommodation/config/SecurityConfig.java` — restrict `/actuator/**` to ADMIN role, keep `/actuator/health` public
- `backend/src/main/resources/application.yml` — configure Actuator endpoint exposure

**Implementation Notes:**
- Implement `HealthIndicator` interface with `health()` method
- MinIO: attempt `minioClient.bucketExists()` — UP if reachable, DOWN with error details
- SMTP: attempt socket connection to mail host:port — UP if reachable, DOWN with details
- Redis: Spring Boot auto-configures `RedisHealthIndicator` when `spring-data-redis` is on classpath — verify it's active
- Addresses [ARCH SEC-006] by restricting `/actuator/**` endpoints
- Health endpoint shows aggregate status for container orchestration

**Acceptance Criteria:**
- [ ] `/actuator/health` shows MinIO, SMTP, and Redis status
- [ ] Health endpoint returns DOWN if any critical service is unavailable
- [ ] `/actuator/**` restricted to ADMIN role (except `/actuator/health`)
- [ ] Container health checks continue to work
- [ ] All existing tests pass

**Tests to Write:**
- `MinioHealthIndicatorTest.health_minioAvailable_statusUp`
- `MinioHealthIndicatorTest.health_minioUnavailable_statusDown`
- `SmtpHealthIndicatorTest.health_smtpAvailable_statusUp`

---

### TASK-3.07: Conduct Accessibility Audit (WCAG 2.1 Level AA)

- **Status:** TODO
- **Priority:** MEDIUM
- **Phase:** 3
- **Estimated Scope:** L (4–8 hrs)
- **Architecture Reference:** [ARCH — implied from NFR]
- **Requirement References:** [REQ GAP-006] Accessibility Features
- **Risk References:** None directly — compliance risk
- **Dependencies:** None within Phase 3
- **Blocked By:** Phase 2 gate

**Description:**
Conduct WCAG 2.1 Level AA accessibility audit on the Angular frontend. Use automated tools (axe-core, Lighthouse) and manual testing (keyboard navigation, screen reader). Fix identified issues: add ARIA labels, ensure proper heading hierarchy, keyboard navigation, color contrast, focus management, form labels.

**Files to Touch:**
- Multiple frontend component HTML/SCSS files — add ARIA attributes, fix contrast, add labels
- `frontend/package.json` — add `axe-core` or similar as dev dependency for automated testing

**Implementation Notes:**
- Run Lighthouse accessibility audit first for automated detection
- Key areas to check: login form, application form, admin tables, notification alerts, modal dialogs
- Ensure all interactive elements are keyboard-accessible (Tab, Enter, Escape)
- Add `aria-label` to icon-only buttons (Angular Material icon buttons)
- Verify `mat-form-field` components have proper labels
- Test with screen reader (NVDA on Windows, VoiceOver on Mac)
- Document findings in `docs/accessibility-audit.md`

**Acceptance Criteria:**
- [ ] Lighthouse accessibility score ≥ 90 on all pages
- [ ] All interactive elements keyboard-accessible
- [ ] All form fields have associated labels
- [ ] Color contrast meets WCAG AA standards (4.5:1 for normal text)
- [ ] ARIA labels on icon-only buttons and dynamic content
- [ ] Findings documented in `docs/accessibility-audit.md`

**Tests to Write:**
- Add `axe-core` integration to Karma tests for automated accessibility testing
- Test keyboard navigation on key workflows (login, application submission)

---

### TASK-3.08: Implement Data Anonymization for Deleted Users

- **Status:** TODO
- **Priority:** MEDIUM
- **Phase:** 3
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH — implied from gaps]
- **Requirement References:** [REQ AUTH-006] (account deletion), [REQ GAP-013] (data anonymization), [REQ BR-032] (deletion cascade)
- **Risk References:** None directly — compliance improvement
- **Dependencies:** None within Phase 3
- **Blocked By:** Phase 2 gate

**Description:**
Implement anonymization option for deleted users instead of full hard delete. When a user deletes their account, replace personal data with "Deleted User #{id}" while retaining allocation/application records for reporting and audit. Add admin option to choose between full delete and anonymization. Ensure GDPR "right to be forgotten" compliance.

**Files to Touch:**
- `backend/src/main/java/com/beezhub/student_accommodation/service/AuthService.java` — add anonymization path alongside existing hard delete
- `backend/src/main/java/com/beezhub/student_accommodation/service/UserAnonymizationService.java` — create service for anonymization logic
- `backend/src/main/resources/db/migration/V{YYYY_MM_DD_HHMM}__add_anonymization_fields.sql` — add `is_anonymized BOOLEAN DEFAULT false` to `app_user`

**Implementation Notes:**
- Anonymize: replace first_name → "Deleted", last_name → "User", email → "deleted_{id}@anonymized.local", phone → null
- Retain: application records, allocation records, activity logs (with anonymized user reference)
- Clear: emergency contacts, dietary requirements, special requirements from student table
- Admin can choose: "Delete Account" (full cascade) or "Anonymize Account" (preserve records)
- Anonymized users should not be able to log in (mark as disabled)
- Legal: consult with legal team on retention requirements before implementing

**Acceptance Criteria:**
- [ ] Anonymization service replaces PII with non-identifying data
- [ ] Allocation and application records retained with anonymized user reference
- [ ] Anonymized users cannot log in
- [ ] Activity logs reference anonymized user correctly
- [ ] Admin can choose between full delete and anonymization
- [ ] All existing tests pass

**Tests to Write:**
- `UserAnonymizationServiceTest.anonymize_replacesPersonalData`
- `UserAnonymizationServiceTest.anonymize_retainsApplicationRecords`
- `UserAnonymizationServiceTest.anonymize_userCannotLogin`

---

### TASK-3.09: Add Prometheus Metrics Export and Grafana Dashboards

- **Status:** TODO
- **Priority:** LOW
- **Phase:** 3
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 6.4] Observability — Metrics, Alerting
- **Requirement References:** None directly — operational improvement
- **Risk References:** None directly
- **Dependencies:** TASK-3.02 (business metrics must exist)
- **Blocked By:** TASK-3.02

**Description:**
Add Prometheus metrics exporter to Spring Boot application. Add Prometheus and Grafana services to Docker Compose. Create pre-configured Grafana dashboards for: JVM metrics, HikariCP pool, business metrics (allocations, emails, batch), and API response times.

**Files to Touch:**
- `backend/build.gradle` — add `micrometer-registry-prometheus` dependency
- `backend/src/main/resources/application.yml` — expose Prometheus endpoint
- `docker-compose.yml` — add Prometheus and Grafana services
- `tools/grafana/` — create directory with dashboard JSON files
- `tools/prometheus/prometheus.yml` — Prometheus configuration targeting the backend

**Implementation Notes:**
- Add `implementation 'io.micrometer:micrometer-registry-prometheus'` to build.gradle
- Expose `/actuator/prometheus` endpoint (restrict to internal network or ADMIN role)
- Prometheus config: scrape backend every 15 seconds
- Grafana dashboard: create JSON dashboard file for import
- Include panels: JVM heap, GC, HikariCP active/pending connections, custom `sas.*` metrics
- Pin Prometheus and Grafana image versions per [ARCH 5.5]

**Acceptance Criteria:**
- [ ] `/actuator/prometheus` returns metrics in Prometheus format
- [ ] Prometheus scrapes backend metrics successfully
- [ ] Grafana dashboard displays JVM and business metrics
- [ ] Docker Compose starts Prometheus and Grafana alongside other services
- [ ] Dashboard JSON exportable for team use

**Tests to Write:**
- Verify `/actuator/prometheus` endpoint returns valid Prometheus format
- Verify Prometheus targets show "UP" status

---

### TASK-3.10: Implement Allocation Status History Table

- **Status:** TODO
- **Priority:** MEDIUM
- **Phase:** 3
- **Estimated Scope:** M (2–4 hrs)
- **Architecture Reference:** [ARCH 9 Phase 3 Item 10]
- **Requirement References:** [REQ GAP-004] (Allocation History & Audit Trail)
- **Risk References:** None directly
- **Dependencies:** TASK-2.01 through TASK-2.03 (allocation services must be decomposed)
- **Blocked By:** Phase 2 gate

**Description:**
Create `allocation_status_history` table to track the full lifecycle of each allocation with timestamps for each status change. Insert a history record on every allocation status transition. Provides detailed audit trail beyond the generic `activity_log` table for dispute resolution and reporting.

**Files to Touch:**
- `backend/src/main/resources/db/migration/V{YYYY_MM_DD_HHMM}__create_allocation_status_history.sql` — create table
- `backend/src/main/java/com/beezhub/student_accommodation/model/entity/AllocationStatusHistory.java` — create entity
- `backend/src/main/java/com/beezhub/student_accommodation/repository/AllocationStatusHistoryRepository.java` — create repository
- `backend/src/main/java/com/beezhub/student_accommodation/service/allocation/AllocationLifecycleService.java` — insert history record on status change

**Implementation Notes:**
- Table schema: `id BIGSERIAL, allocation_id BIGINT FK, previous_status VARCHAR(30), new_status VARCHAR(30), changed_by_user_id BIGINT FK, notes TEXT, created_at TIMESTAMP DEFAULT NOW()`
- Add index: `idx_allocation_history_allocation ON allocation_status_history(allocation_id)`
- Insert history record alongside every `AllocationStatus` change via domain events (TASK-2.06) or direct call
- Consider using `@EventListener` for `AllocationStatusChangedEvent` to automatically record history
- API endpoint: `GET /api/allocations/{id}/history` for admin to view timeline

**Acceptance Criteria:**
- [ ] `allocation_status_history` table created via Flyway migration
- [ ] Entity and repository created
- [ ] History record inserted on every allocation status transition
- [ ] History includes: previous status, new status, changed_by user, timestamp, notes
- [ ] Admin API endpoint returns allocation history timeline
- [ ] All existing tests pass

**Tests to Write:**
- `AllocationStatusHistoryTest.recordTransition_statusChange_historyCreated`
- `AllocationStatusHistoryTest.getHistory_multipleTransitions_orderedByTimestamp`
- Integration test: allocation lifecycle creates correct history trail

---

## Phase Gate Checklists

### Phase 1 Gate Criteria

Before any Phase 2 task can start, **all** of the following must be true:

- [ ] `.env` removed from repository, credentials rotated [TASK-1.01]
- [ ] `AllocationStatus.canTransitionTo()` implemented and enforced [TASK-1.02]
- [ ] `AllocationScoringService` has ≥90% unit test coverage [TASK-1.03]
- [ ] Redis added to Docker Compose and operational [TASK-1.04]
- [ ] JWT token blacklist implemented [TASK-1.05]
- [ ] Brute-force protection implemented [TASK-1.06]
- [ ] Student entity EAGER loading fixed [TASK-1.07]
- [ ] Duplicate CorsFilter removed [TASK-1.08]
- [ ] Composite database indexes added [TASK-1.09]
- [ ] Async queue capacity increased to 500, max threads to 20 [TASK-1.10]
- [ ] No regression in existing functionality (all existing tests pass)

### Phase 2 Gate Criteria

Before any Phase 3 task can start, **all** of the following must be true:

- [ ] No service class exceeds 400 lines [TASK-2.01 through TASK-2.05]
- [ ] Multi-tenancy operational with tenant isolation verified [TASK-2.07, TASK-2.08]
- [ ] File storage abstracted behind `FileStorageService` interface [TASK-2.09]
- [ ] Test coverage ≥70% on all service classes [TASK-2.12]
- [ ] Domain events used for notification decoupling [TASK-2.06]
- [ ] No regression in existing functionality (all existing tests pass)

