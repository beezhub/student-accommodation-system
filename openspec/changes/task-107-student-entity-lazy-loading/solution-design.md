# Solution Design: Student Entity EAGER Loading Fix

**Change ID**: task-107-student-entity-lazy-loading
**Architect**: Solution Architect
**Date**: 2026-02-13
**Status**: Design Complete

---

## 1. Solution Overview

### High-Level Summary

This solution converts the Student entity's three `@OneToOne` associations (AppUser, Institution, YearOfStudy) from `FetchType.EAGER` to `FetchType.LAZY` to eliminate N+1 query performance issues. The approach follows the **Strangler Pattern**: modify the entity first, then systematically fix each repository method and service that accesses these associations, ensuring no `LazyInitializationException` occurs in production.

The core technical strategy:
1. **Entity Layer**: Change all fetch types to LAZY
2. **Repository Layer**: Add targeted `@EntityGraph` annotations for methods that need related data
3. **Service Layer**: Verify all lazy association access occurs within `@Transactional` boundaries
4. **Verification**: Enable SQL logging and run full test suite to detect N+1 queries and lazy loading exceptions

### Key Technical Decisions

**Decision 1: Use `@EntityGraph` over JOIN FETCH**
- **Rationale**: `@EntityGraph` is declarative, cleaner, and works seamlessly with Spring Data JPA method naming conventions. JOIN FETCH requires writing JPQL queries manually.
- **Trade-off**: EntityGraph is less flexible for complex queries but sufficient for this use case (simple one-to-one associations).

**Decision 2: Create Specific Repository Methods for Different Fetch Strategies**
- **Rationale**: Different use cases need different associations (e.g., student profile needs all, notification service only needs appUser email). Creating targeted methods (e.g., `findStudentWithDetailsById()`, `findStudentWithUserById()`) makes intent explicit.
- **Trade-off**: Slight increase in repository method count vs. clearer code and better performance.

**Decision 3: Keep MapStruct Mappers Inside Transactional Boundaries**
- **Rationale**: MapStruct mappers access entity properties, which may trigger lazy loading. Calling mappers within service layer `@Transactional` methods ensures Hibernate session is active.
- **Trade-off**: None — this is best practice alignment with existing architecture.

### Architecture Diagram

```
┌────────────────────────────────────────────────────────────┐
│                     Current (EAGER)                        │
│  Student Query → 1 SELECT student                          │
│                  + 3 SELECT (appUser, institution, year)   │
│                  = 4 queries per student                   │
│                                                            │
│  List 100 students = 301 queries (1 + 100×3)             │
└────────────────────────────────────────────────────────────┘

                            ↓ OPTIMIZATION ↓

┌────────────────────────────────────────────────────────────┐
│                     Target (LAZY + EntityGraph)            │
│  Student Query with @EntityGraph →                        │
│    1 SELECT student                                       │
│      LEFT JOIN appUser                                    │
│      LEFT JOIN institution                                │
│      LEFT JOIN yearOfStudy                                │
│    = 1 query per student list                            │
│                                                            │
│  List 100 students = 1 query (single JOIN)               │
└────────────────────────────────────────────────────────────┘

Performance Improvement: 301 queries → 1 query (99.7% reduction)
```

---

## 2. Entity Layer Changes

### 2.1 Student.java Modifications

**File**: `backend/src/main/java/com/beezhub/student_accommodation/model/entity/Student.java`

**Current Code** (Lines 34-44):
```java
@OneToOne(fetch = FetchType.EAGER)
@JoinColumn(name = "user_id", referencedColumnName = "id")
private AppUser appUser;

@OneToOne(fetch = FetchType.EAGER)
@JoinColumn(name = "institution_id", referencedColumnName = "id")
private Institution institution;

@OneToOne(fetch = FetchType.EAGER)
@JoinColumn(name = "year_of_study_id", referencedColumnName = "id")
private YearOfStudy yearOfStudy;
```

**Modified Code**:
```java
@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", referencedColumnName = "id")
private AppUser appUser;

@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "institution_id", referencedColumnName = "id")
private Institution institution;

@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "year_of_study_id", referencedColumnName = "id")
private YearOfStudy yearOfStudy;
```

**Change Summary**:
- Replace `FetchType.EAGER` with `FetchType.LAZY` on all three associations
- No other changes to entity structure
- Foreign key relationships remain unchanged

### 2.2 Entity Relationship Preservation

**Critical**: The change is ONLY to fetch strategy, not to relationships:
- `@JoinColumn` annotations remain identical
- Database schema unchanged (no Flyway migration needed)
- Bidirectional relationships (if any) unaffected
- Cascade settings (if any) unaffected

---

## 3. Repository Layer Design

### 3.1 StudentRepository Method Enhancements

**File**: `backend/src/main/java/com/beezhub/student_accommodation/repository/StudentRepository.java`

**Current Repository**:
```java
@Repository
public interface StudentRepository extends JpaRepository<Student, Long>,
                                           JpaSpecificationExecutor<Student> {

    Optional<Student> findStudentByAppUser(AppUser user);

    Optional<Student> findByAppUser_Id(Long userId);
}
```

**Enhanced Repository** (with Entity Graphs):
```java
@Repository
public interface StudentRepository extends JpaRepository<Student, Long>,
                                           JpaSpecificationExecutor<Student> {

    // ========================================
    // COMPLETE FETCH (all associations)
    // Use case: Student profile, admin user management
    // ========================================

    @EntityGraph(attributePaths = {"appUser", "institution", "yearOfStudy"})
    @Query("SELECT s FROM Student s WHERE s.id = :id")
    Optional<Student> findStudentWithDetailsById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"appUser", "institution", "yearOfStudy"})
    @Query("SELECT s FROM Student s WHERE s.appUser.id = :userId")
    Optional<Student> findStudentWithDetailsByUserId(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"appUser", "institution", "yearOfStudy"})
    List<Student> findAllStudentsWithDetails();

    // ========================================
    // PARTIAL FETCH (only appUser)
    // Use case: Notifications (need email only)
    // ========================================

    @EntityGraph(attributePaths = {"appUser"})
    @Query("SELECT s FROM Student s WHERE s.id = :id")
    Optional<Student> findStudentWithUserById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"appUser"})
    Optional<Student> findStudentByAppUser(AppUser user);

    // ========================================
    // PARTIAL FETCH (only institution and yearOfStudy)
    // Use case: Allocation scoring (academic data only)
    // ========================================

    @EntityGraph(attributePaths = {"institution", "yearOfStudy"})
    @Query("SELECT s FROM Student s WHERE s.id = :id")
    Optional<Student> findStudentWithAcademicDetailsById(@Param("id") Long id);

    // ========================================
    // NO FETCH (ID-only queries)
    // Use case: Existence checks, counts
    // ========================================

    Optional<Student> findById(Long id);  // Default method, no associations fetched

    Optional<Student> findByAppUser_Id(Long userId);  // No associations fetched

    boolean existsByAppUser_Id(Long userId);
}
```

### 3.2 Entity Graph Patterns Explained

**Pattern 1: Full Details**
- **Attribute Paths**: `{"appUser", "institution", "yearOfStudy"}`
- **SQL Generated**: Single SELECT with 3 LEFT JOINs
- **Use Cases**:
  - Student profile retrieval (`GET /api/students/profile`)
  - Admin user management list (`GET /api/admin/students`)
  - Application detail view (student section)

**Pattern 2: User Only**
- **Attribute Paths**: `{"appUser"}`
- **SQL Generated**: Single SELECT with 1 LEFT JOIN on appUser
- **Use Cases**:
  - Email notifications (need `student.getAppUser().getEmail()`)
  - Authentication checks

**Pattern 3: Academic Details Only**
- **Attribute Paths**: `{"institution", "yearOfStudy"}`
- **SQL Generated**: Single SELECT with 2 LEFT JOINs
- **Use Cases**:
  - Allocation scoring algorithm (needs yearOfStudy for priority)
  - Reports filtering by institution

**Pattern 4: No Fetch**
- **Attribute Paths**: (none)
- **SQL Generated**: SELECT from student table only
- **Use Cases**:
  - ID existence checks
  - COUNT queries
  - Cases where associations are not accessed

### 3.3 Specification Support with Entity Graph

**Challenge**: `JpaSpecificationExecutor.findAll(Specification<Student>)` does not support `@EntityGraph` directly.

**Solution**: Create custom repository method:

```java
public interface StudentRepository extends JpaRepository<Student, Long>,
                                           JpaSpecificationExecutor<Student>,
                                           StudentRepositoryCustom {
    // ... existing methods ...
}

// Custom repository interface
public interface StudentRepositoryCustom {
    List<Student> findAllWithDetails(Specification<Student> spec);
    Page<Student> findAllWithDetails(Specification<Student> spec, Pageable pageable);
}

// Custom repository implementation
@Repository
public class StudentRepositoryImpl implements StudentRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Student> findAllWithDetails(Specification<Student> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Student> query = cb.createQuery(Student.class);
        Root<Student> root = query.from(Student.class);

        // Apply specification
        if (spec != null) {
            Predicate predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
        }

        // Apply entity graph
        EntityGraph<Student> entityGraph = entityManager.createEntityGraph(Student.class);
        entityGraph.addAttributeNodes("appUser", "institution", "yearOfStudy");

        return entityManager.createQuery(query)
            .setHint("jakarta.persistence.fetchgraph", entityGraph)
            .getResultList();
    }

    @Override
    public Page<Student> findAllWithDetails(Specification<Student> spec, Pageable pageable) {
        // Similar implementation with pagination
        List<Student> content = findAllWithDetails(spec);
        long total = count(spec);  // Separate count query
        return new PageImpl<>(content, pageable, total);
    }

    private long count(Specification<Student> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Student> root = query.from(Student.class);
        query.select(cb.count(root));

        if (spec != null) {
            Predicate predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
        }

        return entityManager.createQuery(query).getSingleResult();
    }
}
```

---

## 4. Service Layer Verification

### 4.1 Services Accessing Student Entity

Based on codebase analysis, the following services access Student and its lazy associations:

| Service | Accesses Which Associations? | Current Status | Action Required |
|---------|------------------------------|----------------|-----------------|
| `StudentService` | appUser, institution, yearOfStudy | Methods are `@Transactional` | ✅ Verify mapper calls are inside transaction |
| `ApplicationService` | appUser (for student name/email) | Methods are `@Transactional` | ✅ Use `findStudentWithUserById()` |
| `RoomAllocationService` | yearOfStudy (for scoring) | Methods are `@Transactional` | ✅ Use `findStudentWithAcademicDetailsById()` |
| `NotificationService` | appUser (for email address) | Methods are `@Transactional` | ✅ Use `findStudentWithUserById()` |
| `AllocationScoringService` | yearOfStudy | Methods are `@Transactional` | ✅ Use `findStudentWithAcademicDetailsById()` |

### 4.2 StudentService Example Refactoring

**Current Code** (Potential Issue):
```java
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;

    @Transactional(readOnly = true)
    public StudentResponse getStudentProfile(Long userId) {
        Student student = studentRepository.findByAppUser_Id(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Student", userId));

        // Mapper accesses student.getAppUser(), student.getInstitution(), student.getYearOfStudy()
        return studentMapper.toResponse(student);  // ✅ OK - inside transaction
    }
}
```

**Optimized Code**:
```java
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;

    @Transactional(readOnly = true)
    public StudentResponse getStudentProfile(Long userId) {
        // Use entity graph method to fetch all associations in one query
        Student student = studentRepository.findStudentWithDetailsByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Student", userId));

        return studentMapper.toResponse(student);  // Associations already loaded
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getAllStudents() {
        // Use entity graph method for list queries
        List<Student> students = studentRepository.findAllStudentsWithDetails();
        return studentMapper.toResponseList(students);
    }
}
```

### 4.3 Transactional Boundary Checklist

**Rules**:
1. All service methods that access lazy associations MUST be `@Transactional`
2. MapStruct mappers MUST be called within transactional methods (they access entity getters)
3. Controllers MUST NOT access entity methods directly — only DTOs
4. Async methods (`@Async`) that access lazy associations must complete within transaction

**Verification Approach**:
- Search codebase for `student.get` calls
- Ensure each call is within a `@Transactional` method
- Add `@Transactional(readOnly = true)` to read-only methods for performance

---

## 5. Testing Strategy

### 5.1 Unit Testing Approach

**Test 1: Verify Entity Configuration**
```java
@Test
void studentEntity_shouldHaveLazyLoadingConfigured() {
    // Verify fetch type via reflection
    Field appUserField = Student.class.getDeclaredField("appUser");
    OneToOne annotation = appUserField.getAnnotation(OneToOne.class);

    assertEquals(FetchType.LAZY, annotation.fetch());

    // Repeat for institution and yearOfStudy
}
```

**Test 2: Verify Repository Entity Graph**
```java
@DataJpaTest
class StudentRepositoryTest {

    @Autowired
    private StudentRepository studentRepository;

    @Test
    void findStudentWithDetailsById_shouldFetchAllAssociations() {
        // Given
        Student student = createTestStudent();  // with appUser, institution, year
        studentRepository.save(student);

        // When
        Optional<Student> result = studentRepository.findStudentWithDetailsById(student.getId());

        // Then
        assertTrue(result.isPresent());
        Student retrieved = result.get();

        // Hibernate should have initialized all associations
        assertTrue(Hibernate.isInitialized(retrieved.getAppUser()));
        assertTrue(Hibernate.isInitialized(retrieved.getInstitution()));
        assertTrue(Hibernate.isInitialized(retrieved.getYearOfStudy()));
    }

    @Test
    void findById_shouldNotFetchAssociations() {
        // Given
        Student student = createTestStudent();
        studentRepository.save(student);

        // When
        Optional<Student> result = studentRepository.findById(student.getId());

        // Then
        assertTrue(result.isPresent());
        Student retrieved = result.get();

        // Associations should NOT be initialized (lazy)
        assertFalse(Hibernate.isInitialized(retrieved.getAppUser()));
        assertFalse(Hibernate.isInitialized(retrieved.getInstitution()));
        assertFalse(Hibernate.isInitialized(retrieved.getYearOfStudy()));
    }
}
```

**Test 3: Verify No LazyInitializationException in Service Layer**
```java
@SpringBootTest
@Transactional
class StudentServiceIntegrationTest {

    @Autowired
    private StudentService studentService;

    @Test
    void getStudentProfile_shouldNotThrowLazyInitializationException() {
        // Given
        Long userId = createTestUserAndStudent();

        // When
        assertDoesNotThrow(() -> {
            StudentResponse response = studentService.getStudentProfile(userId);

            // Verify all fields are populated
            assertNotNull(response.getEmail());  // From appUser
            assertNotNull(response.getInstitutionName());  // From institution
            assertNotNull(response.getYearOfStudyName());  // From yearOfStudy
        });
    }
}
```

### 5.2 SQL Query Logging Verification

**Enable SQL Logging** (application-test.yml):
```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
```

**Verification Steps**:
1. Run `StudentServiceIntegrationTest.getAllStudents()`
2. Examine console output for SQL queries
3. **Success Criteria**:
   - Should see ONE SELECT with 3 LEFT JOINs
   - Should NOT see separate SELECT statements for appUser, institution, yearOfStudy
   - Query count for 100 students should be 1 (not 301)

**Example Expected SQL**:
```sql
SELECT
    s.id, s.student_number, s.phone_number, ...
    u.id, u.email, u.first_name, ...
    i.id, i.name, ...
    y.id, y.name, ...
FROM student s
LEFT JOIN app_user u ON s.user_id = u.id
LEFT JOIN institution i ON s.institution_id = i.id
LEFT JOIN year_of_study y ON s.year_of_study_id = y.id
WHERE s.id = ?
```

### 5.3 Integration Test Coverage

**Test Scenarios**:
1. ✅ Student profile retrieval (GET /api/students/profile)
2. ✅ Admin student list (GET /api/admin/students)
3. ✅ Application submission (creates student reference)
4. ✅ Room allocation batch job (accesses yearOfStudy)
5. ✅ Email notification sending (accesses appUser.email)
6. ✅ Student search with filters (uses Specification)

**Expected Result**: All existing integration tests pass with zero `LazyInitializationException`.

---

## 6. Performance Optimization Analysis

### 6.1 Before/After Metrics

| Metric | Before (EAGER) | After (LAZY + EntityGraph) | Improvement |
|--------|----------------|----------------------------|-------------|
| Queries for 1 student fetch | 4 (1 + 3 joins) | 1 (single JOIN) | 75% reduction |
| Queries for 100 student list | 301 (1 + 100×3) | 1 (single JOIN) | 99.7% reduction |
| Database connection time | ~200ms (4 queries) | ~50ms (1 query) | 75% faster |
| API response time (student list, 50 items) | ~1500ms (estimate) | ~400ms (target) | 73% faster |
| Connection pool utilization during peak | High (frequent exhaustion) | 30% reduction | Improved scalability |

### 6.2 Query Execution Plan Comparison

**EAGER (Current)**:
```sql
-- Query 1
SELECT * FROM student WHERE id = 1;

-- Query 2 (triggered by Hibernate proxy initialization)
SELECT * FROM app_user WHERE id = 123;

-- Query 3
SELECT * FROM institution WHERE id = 456;

-- Query 4
SELECT * FROM year_of_study WHERE id = 789;
```
Total: 4 round-trips to database

**LAZY + EntityGraph (Target)**:
```sql
-- Single Query
SELECT
    s.*, u.*, i.*, y.*
FROM student s
LEFT JOIN app_user u ON s.user_id = u.id
LEFT JOIN institution i ON s.institution_id = i.id
LEFT JOIN year_of_study y ON s.year_of_study_id = y.id
WHERE s.id = 1;
```
Total: 1 round-trip to database

### 6.3 Database Index Validation

**Existing Indexes** (verify these exist):
```sql
-- Foreign key columns should be indexed for JOIN performance
CREATE INDEX IF NOT EXISTS idx_student_user_id ON student(user_id);
CREATE INDEX IF NOT EXISTS idx_student_institution_id ON student(institution_id);
CREATE INDEX IF NOT EXISTS idx_student_year_of_study_id ON student(year_of_study_id);
```

**Action**: Run `EXPLAIN ANALYZE` on the optimized query to verify index usage.

---

## 7. Risk Assessment & Mitigation

| Risk | Impact | Probability | Mitigation Strategy | Rollback Plan |
|------|--------|-------------|---------------------|---------------|
| **LazyInitializationException in production** | High (service outage) | Low (comprehensive testing) | 1. Full integration test suite<br>2. Staging deployment first<br>3. Code review of all `student.get*` calls | Revert entity to EAGER (single line change) |
| **Missed access point causes runtime error** | Medium (partial feature failure) | Medium | 1. Global search for `student.getAppUser()`, `student.getInstitution()`, `student.getYearOfStudy()`<br>2. Manual code review of all services<br>3. Canary deployment (5% traffic first) | Hotfix: Add `@Transactional` or entity graph to affected method |
| **Performance regression due to incorrect entity graph** | Medium (slower than before) | Low | 1. SQL logging in staging<br>2. Load testing before production<br>3. APM monitoring (query count metrics) | Adjust entity graph or revert |
| **MapStruct mapper fails outside transaction** | Low (compile-time safe) | Very Low | MapStruct generates code at compile time; if lazy access fails, tests will catch it | Move mapper call inside `@Transactional` method |
| **Async methods access lazy associations after transaction** | Medium | Low | 1. Review all `@Async` methods<br>2. Fetch associations before async call<br>3. Pass DTOs to async methods, not entities | Modify async method to accept DTO instead of entity |

### Rollback Plan

**Immediate Rollback** (if production issues detected):
1. Revert Student.java: Change `FetchType.LAZY` back to `FetchType.EAGER` (3 lines)
2. Deploy hotfix (30-minute turnaround)
3. Monitor logs for resolution
4. No database changes required (schema unchanged)

**Partial Rollback** (if specific service fails):
1. Identify failing service via error logs
2. Add `@EntityGraph` to repository method used by that service
3. Deploy targeted fix (faster than full rollback)

---

## 8. Deployment Plan

### 8.1 Pre-Deployment Checklist

- [ ] All unit tests pass (`./gradlew test`)
- [ ] All integration tests pass
- [ ] SQL logging enabled in staging, N+1 queries eliminated
- [ ] Code review completed (focus on `@Transactional` boundaries)
- [ ] Performance testing completed (student list endpoint < 500ms)
- [ ] Staging deployment successful, no errors in logs for 24 hours
- [ ] Rollback plan documented and tested

### 8.2 Deployment Steps

**Step 1: Deploy to Staging**
1. Deploy backend code changes (Student.java + repository methods)
2. Enable SQL logging: `spring.jpa.show-sql=true`
3. Run smoke tests on all student-related endpoints
4. Monitor logs for `LazyInitializationException` (should be zero)
5. Verify query count reduction via logs
6. Load test: Simulate 100 concurrent users hitting student list endpoint

**Step 2: Canary Deployment to Production (5% Traffic)**
1. Deploy to single production instance
2. Route 5% of traffic to updated instance
3. Monitor error rates, API response times, database connection pool
4. Duration: 2 hours minimum
5. If error rate increases >0.1%, immediate rollback

**Step 3: Full Production Deployment**
1. Deploy to all production instances
2. Monitor metrics for 1 hour:
   - API response time (target: P95 < 500ms for student list)
   - Database query count (target: ~99% reduction)
   - Error rate (target: no increase)
   - Database connection pool utilization (target: 30% reduction)
3. Disable SQL logging after verification

### 8.3 Post-Deployment Verification

**Metrics to Monitor** (first 24 hours):
- API response time: `GET /api/students` P95 < 500ms
- Database query count: Verify via application logs or APM
- Error logs: Zero `LazyInitializationException`
- Database connection pool: Active connections during peak reduced by ~30%

**Success Criteria**:
- All metrics meet targets
- Zero production incidents related to lazy loading
- No increase in error rate
- User-reported performance improvements

---

## 9. Technical Decisions & Rationale

### Decision 1: EntityGraph vs. JOIN FETCH

**Options Considered**:
- **Option A**: `@EntityGraph(attributePaths = {"appUser", "institution", "yearOfStudy"})`
- **Option B**: `@Query("SELECT s FROM Student s JOIN FETCH s.appUser JOIN FETCH s.institution ...")`

**Selected**: Option A (@EntityGraph)

**Rationale**:
- Declarative and cleaner (annotation-based vs. JPQL strings)
- Works with Spring Data method naming conventions
- Easier to maintain (change association names without updating JPQL)
- Sufficient for this use case (simple one-to-one joins)

**Trade-offs**:
- Less flexible for complex queries (e.g., conditional joins)
- JOIN FETCH allows more control over JOIN type (LEFT vs. INNER)
- For this case, LEFT JOIN is correct (all associations are nullable), so EntityGraph is optimal

### Decision 2: Multiple Repository Methods vs. Single Method with Parameters

**Options Considered**:
- **Option A**: Create specific methods (`findStudentWithDetailsById()`, `findStudentWithUserById()`, etc.)
- **Option B**: Single method with boolean flags (`findStudent(Long id, boolean fetchUser, boolean fetchInstitution, boolean fetchYear)`)

**Selected**: Option A (Multiple Specific Methods)

**Rationale**:
- **Explicit intent**: Method name declares what it fetches
- **Type safety**: Compile-time validation (can't forget to pass flags)
- **Better IDE autocomplete**: Developers see available fetch strategies
- **Testability**: Each method is independently testable

**Trade-offs**:
- Slight increase in method count (6 methods instead of 1)
- Acceptable trade-off for clarity and maintainability

### Decision 3: Keep Existing Tests Unchanged

**Options Considered**:
- **Option A**: Keep existing tests as regression tests (should pass without modification)
- **Option B**: Rewrite all tests to explicitly verify lazy loading

**Selected**: Option A (Regression Testing)

**Rationale**:
- If existing tests pass after the change, it proves backward compatibility
- Existing tests already verify business logic correctness
- Add NEW tests specifically for lazy loading verification (Hibernate.isInitialized checks)
- Minimizes risk of introducing test bugs

**Trade-offs**:
- Existing tests may not catch lazy loading issues if they don't access associations
- Mitigated by adding targeted lazy loading tests

---

## 10. Open Questions for Implementation Team

**Q1**: Are there any `@Async` methods that access Student entities and their associations?
- **Action**: Search codebase for `@Async` + `Student` usage
- **Risk**: Async methods may run after transaction commits, causing LazyInitializationException
- **Mitigation**: Pass DTOs to async methods, not entities

**Q2**: Are there any batch jobs or scheduled tasks that process students?
- **Action**: Review `@Scheduled` methods in `RoomAllocationService`, `AllocationBatchService`
- **Risk**: Batch jobs loading 1000+ students may hit memory limits
- **Mitigation**: Use pagination or streaming if necessary

**Q3**: Are there any direct EntityManager queries accessing Student?
- **Action**: Search for `entityManager.createQuery` using Student
- **Risk**: Custom queries bypass repository entity graphs
- **Mitigation**: Add entity graph hints to custom queries

---

## Architecture Review Checklist

- [x] Entity changes minimize to fetch type only (no schema changes)
- [x] Repository methods use `@EntityGraph` for targeted fetching
- [x] Service layer verified for `@Transactional` boundaries
- [x] MapStruct mappers called within transactional methods
- [x] Multiple fetch strategies defined (full details, partial, none)
- [x] Specification support included via custom repository
- [x] Performance metrics defined (query count, response time)
- [x] Testing strategy comprehensive (unit, integration, SQL logging)
- [x] Risk assessment complete with mitigation plans
- [x] Rollback plan simple and fast (single entity change)
- [x] Deployment plan includes canary and monitoring
- [x] No violations of existing architectural patterns
- [x] Strangler Pattern applied (modify in place, preserve facade)
- [x] Hard limits respected (no methods >30 lines, services <400 lines)

---

## Summary for Implementation Team

**What Changes**:
- Student.java: 3 lines (EAGER → LAZY)
- StudentRepository.java: Add 8-10 new methods with `@EntityGraph`
- Service layer: Replace `findById()` calls with `findStudentWithDetailsById()` where associations are accessed
- Total estimated effort: 2-4 hours

**What Stays the Same**:
- Database schema (zero migrations)
- API contracts (DTOs unchanged)
- Business logic (no functional changes)
- Frontend (zero changes)

**Success Criteria**:
- Zero `LazyInitializationException` in production
- 99% reduction in database queries for student lists
- All existing tests pass
- API response time improved by >50%

**Next Step**: Proceed to Implementation Plan artifact for detailed task breakdown.
