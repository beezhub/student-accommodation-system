# Implementation Plan: Student Entity EAGER Loading Fix

**Change ID**: task-107-student-entity-lazy-loading
**Developer**: Senior Full Stack Developer
**Date**: 2026-02-13
**Estimated Total Effort**: 2-4 hours

---

## Summary

**Feature**: Convert Student entity's three `@OneToOne` associations from EAGER to LAZY loading

**Goal**: Eliminate N+1 query performance degradation (301 queries → 1 query for 100-student list)

**Strategy**: Modify entity fetch types, add targeted `@EntityGraph` repository methods, verify transactional boundaries

**Critical Path**: Entity Change → Repository Methods → Service Verification → Testing → Performance Validation

**Parallel Work Opportunities**:
- Repository tests can be written while service layer is being verified
- Test SQL logging setup can run in parallel with code changes

---

## Task Breakdown

### Phase 1: Entity Layer Modification

#### Task 1.1: Update Student Entity to Use LAZY Loading

**File**: `backend/src/main/java/com/beezhub/student_accommodation/model/entity/Student.java`

**Estimated Effort**: 5 minutes

**Dependencies**: None

**Description**:
- Change `FetchType.EAGER` to `FetchType.LAZY` on three `@OneToOne` associations:
  1. `appUser` (line 34)
  2. `institution` (line 38)
  3. `yearOfStudy` (line 42)

**Implementation Steps**:
1. Open `Student.java`
2. Locate the three `@OneToOne` annotations with `fetch = FetchType.EAGER`
3. Replace each with `fetch = FetchType.LAZY`
4. Save file

**Code Changes**:
```java
// BEFORE:
@OneToOne(fetch = FetchType.EAGER)
@JoinColumn(name = "user_id", referencedColumnName = "id")
private AppUser appUser;

@OneToOne(fetch = FetchType.EAGER)
@JoinColumn(name = "institution_id", referencedColumnName = "id")
private Institution institution;

@OneToOne(fetch = FetchType.EAGER)
@JoinColumn(name = "year_of_study_id", referencedColumnName = "id")
private YearOfStudy yearOfStudy;

// AFTER:
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

**Acceptance Criteria**:
- [ ] All three `@OneToOne` annotations use `FetchType.LAZY`
- [ ] No other changes to Student entity
- [ ] File compiles without errors
- [ ] `@JoinColumn` annotations remain unchanged

---

### Phase 2: Repository Layer Enhancement

#### Task 2.1: Add EntityGraph Repository Methods

**File**: `backend/src/main/java/com/beezhub/student_accommodation/repository/StudentRepository.java`

**Estimated Effort**: 45 minutes

**Dependencies**: Task 1.1

**Description**:
- Add new repository methods with `@EntityGraph` for different fetch strategies:
  1. **Full fetch** (all 3 associations) - for student profile, admin lists
  2. **Partial fetch - user only** - for notifications (email access)
  3. **Partial fetch - academic only** - for allocation scoring
  4. **No fetch** - for existence checks (already exists via default methods)

**Implementation Steps**:
1. Open `StudentRepository.java`
2. Add new methods with `@EntityGraph` and `@Query` annotations
3. Follow Spring Data JPA naming conventions
4. Add JavaDoc comments for each method
5. Save file

**Code Changes**:
```java
@Repository
public interface StudentRepository extends JpaRepository<Student, Long>,
                                           JpaSpecificationExecutor<Student> {

    // ==================================================
    // EXISTING METHODS (keep as-is, no fetch)
    // ==================================================

    Optional<Student> findStudentByAppUser(AppUser user);

    Optional<Student> findByAppUser_Id(Long userId);

    // ==================================================
    // NEW METHODS: FULL FETCH (all associations)
    // Use case: Student profile, admin user management
    // ==================================================

    /**
     * Finds a student by ID with all associations eagerly loaded.
     * @param id Student ID
     * @return Student with appUser, institution, and yearOfStudy loaded
     */
    @EntityGraph(attributePaths = {"appUser", "institution", "yearOfStudy"})
    @Query("SELECT s FROM Student s WHERE s.id = :id")
    Optional<Student> findStudentWithDetailsById(@Param("id") Long id);

    /**
     * Finds a student by user ID with all associations eagerly loaded.
     * @param userId AppUser ID
     * @return Student with all details
     */
    @EntityGraph(attributePaths = {"appUser", "institution", "yearOfStudy"})
    @Query("SELECT s FROM Student s WHERE s.appUser.id = :userId")
    Optional<Student> findStudentWithDetailsByUserId(@Param("userId") Long userId);

    /**
     * Finds all students with full details.
     * @return List of students with all associations loaded
     */
    @EntityGraph(attributePaths = {"appUser", "institution", "yearOfStudy"})
    List<Student> findAllStudentsWithDetails();

    // ==================================================
    // NEW METHODS: PARTIAL FETCH (appUser only)
    // Use case: Notifications (need email only)
    // ==================================================

    /**
     * Finds a student by ID with appUser association loaded.
     * @param id Student ID
     * @return Student with appUser loaded
     */
    @EntityGraph(attributePaths = {"appUser"})
    @Query("SELECT s FROM Student s WHERE s.id = :id")
    Optional<Student> findStudentWithUserById(@Param("id") Long id);

    /**
     * Finds a student by AppUser with appUser association loaded.
     * @param user AppUser entity
     * @return Student with appUser loaded
     */
    @EntityGraph(attributePaths = {"appUser"})
    @Query("SELECT s FROM Student s WHERE s.appUser = :user")
    Optional<Student> findStudentWithUserByAppUser(@Param("user") AppUser user);

    // ==================================================
    // NEW METHODS: PARTIAL FETCH (academic details)
    // Use case: Allocation scoring (needs institution, yearOfStudy)
    // ==================================================

    /**
     * Finds a student by ID with academic details loaded.
     * @param id Student ID
     * @return Student with institution and yearOfStudy loaded
     */
    @EntityGraph(attributePaths = {"institution", "yearOfStudy"})
    @Query("SELECT s FROM Student s WHERE s.id = :id")
    Optional<Student> findStudentWithAcademicDetailsById(@Param("id") Long id);
}
```

**Acceptance Criteria**:
- [ ] 7 new methods added with `@EntityGraph`
- [ ] Full fetch methods include all 3 associations
- [ ] Partial fetch methods include only needed associations
- [ ] All methods have JavaDoc comments
- [ ] File compiles without errors
- [ ] No duplicate method signatures

---

### Phase 3: Service Layer Verification & Updates

#### Task 3.1: Update StudentService to Use Entity Graph Methods

**File**: `backend/src/main/java/com/beezhub/student_accommodation/service/StudentService.java`

**Estimated Effort**: 30 minutes

**Dependencies**: Task 2.1

**Description**:
- Replace calls to default repository methods with entity graph methods
- Verify `@Transactional` annotations are present
- Ensure MapStruct mapper calls are within transactional methods

**Implementation Steps**:
1. Open `StudentService.java`
2. Find all uses of `studentRepository.findById()` or `studentRepository.findByAppUser_Id()`
3. Replace with appropriate entity graph method:
   - For full details (profile, admin): use `findStudentWithDetailsById/ByUserId()`
   - For email only: use `findStudentWithUserById/ByAppUser()`
4. Verify `@Transactional` or `@Transactional(readOnly = true)` is present
5. Save file

**Example Changes**:
```java
// BEFORE:
@Transactional(readOnly = true)
public StudentResponse getStudentProfile(Long userId) {
    Student student = studentRepository.findByAppUser_Id(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Student", userId));
    return studentMapper.toResponse(student);  // May cause LazyInitializationException
}

// AFTER:
@Transactional(readOnly = true)
public StudentResponse getStudentProfile(Long userId) {
    Student student = studentRepository.findStudentWithDetailsByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Student", userId));
    return studentMapper.toResponse(student);  // All associations loaded
}
```

**Acceptance Criteria**:
- [ ] All repository calls updated to use entity graph methods
- [ ] All methods accessing lazy associations are `@Transactional`
- [ ] Mapper calls remain inside transactional methods
- [ ] File compiles without errors

---

#### Task 3.2: Verify and Update Other Services Using Student

**Files**:
- `backend/src/main/java/com/beezhub/student_accommodation/service/ApplicationService.java`
- `backend/src/main/java/com/beezhub/student_accommodation/service/RoomAllocationService.java`
- `backend/src/main/java/com/beezhub/student_accommodation/service/NotificationService.java`
- `backend/src/main/java/com/beezhub/student_accommodation/service/AllocationScoringService.java`

**Estimated Effort**: 45 minutes

**Dependencies**: Task 2.1

**Description**:
- Search for all uses of Student entity in services
- Verify transactional boundaries
- Update repository calls to use appropriate entity graph methods
- Focus on methods that access `student.getAppUser()`, `student.getInstitution()`, `student.getYearOfStudy()`

**Search Command**:
```bash
# Find all Student entity usages
grep -r "student\\.get" backend/src/main/java/com/beezhub/student_accommodation/service/
```

**Service-Specific Changes**:

**ApplicationService** (needs appUser for email):
```java
// Use findStudentWithUserById() when accessing student.getAppUser()
```

**RoomAllocationService** (needs yearOfStudy for scoring):
```java
// Use findStudentWithAcademicDetailsById() when accessing yearOfStudy
```

**NotificationService** (needs appUser for email):
```java
// Use findStudentWithUserById() when accessing appUser.getEmail()
```

**AllocationScoringService** (needs yearOfStudy):
```java
// Use findStudentWithAcademicDetailsById() for allocation scoring
```

**Acceptance Criteria**:
- [ ] All services verified for Student entity access
- [ ] Repository calls updated with appropriate entity graph methods
- [ ] All methods accessing lazy associations are `@Transactional`
- [ ] No calls to default `findById()` that access lazy associations
- [ ] All files compile without errors

---

### Phase 4: Testing

#### Task 4.1: Write Repository Tests for Entity Graph Methods

**File**: `backend/src/test/java/com/beezhub/student_accommodation/repository/StudentRepositoryTest.java`

**Estimated Effort**: 1 hour

**Dependencies**: Task 2.1

**Description**:
- Test new entity graph repository methods
- Verify associations are initialized
- Verify SQL query count (single query with JOINs)

**Implementation Steps**:
1. Create or update `StudentRepositoryTest.java`
2. Add `@DataJpaTest` annotation
3. Create test data (student with appUser, institution, yearOfStudy)
4. Test each new repository method
5. Use `Hibernate.isInitialized()` to verify associations loaded
6. Run tests

**Code Template**:
```java
@DataJpaTest
class StudentRepositoryTest {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private YearOfStudyRepository yearOfStudyRepository;

    private Student testStudent;

    @BeforeEach
    void setUp() {
        // Create test data
        AppUser user = new AppUser();
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user = appUserRepository.save(user);

        Institution institution = new Institution();
        institution.setName("Test University");
        institution = institutionRepository.save(institution);

        YearOfStudy year = new YearOfStudy();
        year.setName("Year 1");
        year = yearOfStudyRepository.save(year);

        testStudent = new Student();
        testStudent.setStudentNumber("S12345");
        testStudent.setAppUser(user);
        testStudent.setInstitution(institution);
        testStudent.setYearOfStudy(year);
        testStudent = studentRepository.save(testStudent);
    }

    @Test
    void findStudentWithDetailsById_shouldLoadAllAssociations() {
        // When
        Optional<Student> result = studentRepository.findStudentWithDetailsById(testStudent.getId());

        // Then
        assertTrue(result.isPresent());
        Student student = result.get();

        // Verify all associations are initialized
        assertTrue(Hibernate.isInitialized(student.getAppUser()));
        assertTrue(Hibernate.isInitialized(student.getInstitution()));
        assertTrue(Hibernate.isInitialized(student.getYearOfStudy()));

        // Verify data correctness
        assertEquals("test@example.com", student.getAppUser().getEmail());
        assertEquals("Test University", student.getInstitution().getName());
        assertEquals("Year 1", student.getYearOfStudy().getName());
    }

    @Test
    void findById_shouldNotLoadAssociations() {
        // When
        Optional<Student> result = studentRepository.findById(testStudent.getId());

        // Then
        assertTrue(result.isPresent());
        Student student = result.get();

        // Verify associations are NOT initialized (lazy)
        assertFalse(Hibernate.isInitialized(student.getAppUser()));
        assertFalse(Hibernate.isInitialized(student.getInstitution()));
        assertFalse(Hibernate.isInitialized(student.getYearOfStudy()));
    }

    @Test
    void findStudentWithUserById_shouldLoadOnlyAppUser() {
        // When
        Optional<Student> result = studentRepository.findStudentWithUserById(testStudent.getId());

        // Then
        assertTrue(result.isPresent());
        Student student = result.get();

        // Verify only appUser is initialized
        assertTrue(Hibernate.isInitialized(student.getAppUser()));
        assertFalse(Hibernate.isInitialized(student.getInstitution()));
        assertFalse(Hibernate.isInitialized(student.getYearOfStudy()));
    }

    @Test
    void findStudentWithAcademicDetailsById_shouldLoadAcademicFields() {
        // When
        Optional<Student> result = studentRepository.findStudentWithAcademicDetailsById(testStudent.getId());

        // Then
        assertTrue(result.isPresent());
        Student student = result.get();

        // Verify institution and yearOfStudy are initialized
        assertFalse(Hibernate.isInitialized(student.getAppUser()));
        assertTrue(Hibernate.isInitialized(student.getInstitution()));
        assertTrue(Hibernate.isInitialized(student.getYearOfStudy()));
    }

    @Test
    void findAllStudentsWithDetails_shouldLoadAllAssociationsForList() {
        // When
        List<Student> students = studentRepository.findAllStudentsWithDetails();

        // Then
        assertFalse(students.isEmpty());
        Student student = students.get(0);

        // Verify all associations loaded
        assertTrue(Hibernate.isInitialized(student.getAppUser()));
        assertTrue(Hibernate.isInitialized(student.getInstitution()));
        assertTrue(Hibernate.isInitialized(student.getYearOfStudy()));
    }
}
```

**Acceptance Criteria**:
- [ ] Tests cover all new entity graph methods
- [ ] `Hibernate.isInitialized()` used to verify association loading
- [ ] Tests verify lazy loading works (default `findById` doesn't load associations)
- [ ] All tests pass
- [ ] No `LazyInitializationException` in any test

---

#### Task 4.2: Run Full Test Suite and Fix LazyInitializationExceptions

**Command**: `./gradlew test`

**Estimated Effort**: 1 hour (includes fixing any failures)

**Dependencies**: Task 1.1, 3.1, 3.2

**Description**:
- Run complete test suite
- Identify any `LazyInitializationException` failures
- Fix by either:
  1. Adding `@Transactional` to test method
  2. Using entity graph repository method in code under test
  3. Updating service to use entity graph method

**Implementation Steps**:
1. Run `./gradlew test` from backend directory
2. Review test results
3. For each `LazyInitializationException`:
   - Identify which service/method caused it
   - Check if method is `@Transactional`
   - Check if repository call uses entity graph method
   - Fix the issue
4. Re-run tests until all pass

**Common Fixes**:
```java
// Fix 1: Add @Transactional to test
@Test
@Transactional  // <-- Add this
void testMethodAccessingLazyAssociation() {
    // ...
}

// Fix 2: Update service to use entity graph method
// Instead of: studentRepository.findById(id)
// Use: studentRepository.findStudentWithDetailsById(id)

// Fix 3: Ensure service method is @Transactional
@Transactional(readOnly = true)  // <-- Add this
public StudentResponse getStudent(Long id) {
    // ...
}
```

**Acceptance Criteria**:
- [ ] All existing tests pass
- [ ] Zero `LazyInitializationException` in test suite
- [ ] No new test failures introduced
- [ ] Test coverage remains >80%

---

### Phase 5: Performance Validation

#### Task 5.1: Enable SQL Logging and Verify Query Optimization

**File**: `backend/src/main/resources/application.yml`

**Estimated Effort**: 30 minutes

**Dependencies**: Task 4.2 (all tests passing)

**Description**:
- Temporarily enable SQL logging
- Run application and test student list endpoint
- Verify single query with JOINs instead of N+1 queries
- Measure performance improvement

**Implementation Steps**:
1. Open `application.yml`
2. Add or update JPA logging config:
```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
```
3. Start application: `./gradlew bootRun`
4. Make API call to student list endpoint (e.g., GET /api/admin/students)
5. Check console logs for SQL queries
6. Count queries (should be 1-2, not 301)
7. Disable SQL logging before committing

**Expected SQL Output**:
```sql
-- Should see ONE query like this:
SELECT
    s.id, s.student_number, s.phone_number, ...
    u.id as user_id, u.email, u.first_name, ...
    i.id as institution_id, i.name as institution_name, ...
    y.id as year_id, y.name as year_name, ...
FROM student s
LEFT JOIN app_user u ON s.user_id = u.id
LEFT JOIN institution i ON s.institution_id = i.id
LEFT JOIN year_of_study y ON s.year_of_study_id = y.id
-- Plus optional pagination count query

-- Should NOT see multiple queries like:
SELECT * FROM student;
SELECT * FROM app_user WHERE id = ?;
SELECT * FROM institution WHERE id = ?;
SELECT * FROM year_of_study WHERE id = ?;
```

**Verification Checklist**:
- [ ] SQL logging enabled
- [ ] Application starts successfully
- [ ] Student list endpoint returns data correctly
- [ ] Console shows single JOIN query (not N+1 queries)
- [ ] Query count for 100 students is 1-2 (not 301)
- [ ] SQL logging disabled before commit

**Acceptance Criteria**:
- [ ] Query count reduced by 99% (301 → 1-2 queries)
- [ ] Single SELECT with LEFT JOINs observed in logs
- [ ] No N+1 query pattern detected
- [ ] API response time improved (measure with stopwatch)
- [ ] All data fields populated correctly in response

---

#### Task 5.2: Performance Benchmark (Optional)

**Tools**: Postman, JMeter, or curl with time measurement

**Estimated Effort**: 30 minutes

**Dependencies**: Task 5.1

**Description**:
- Measure API response time before and after optimization
- Document performance improvement
- Verify connection pool utilization

**Benchmark Tests**:
1. **Single student fetch**: GET /api/students/profile
   - Before: ~4 queries, ~200ms
   - After: ~1 query, ~50ms

2. **Student list (50 items)**: GET /api/admin/students?page=0&size=50
   - Before: ~151 queries, ~1500ms
   - After: ~1 query, ~400ms

3. **Student list (100 items)**: GET /api/admin/students?page=0&size=100
   - Before: ~301 queries, ~3000ms
   - After: ~1 query, ~500ms

**Acceptance Criteria**:
- [ ] Response time improved by >50% for all endpoints
- [ ] Larger lists show greater improvement
- [ ] Database connection pool active connections reduced
- [ ] Results documented in commit message or PR description

---

### Phase 6: Final Verification & Cleanup

#### Task 6.1: Code Review Checklist

**Estimated Effort**: 15 minutes

**Dependencies**: All previous tasks

**Description**: Self-review code changes before committing

**Checklist**:
- [ ] Student.java: All 3 associations use `FetchType.LAZY`
- [ ] StudentRepository.java: 7 new entity graph methods added
- [ ] StudentService.java: Repository calls updated to use entity graph methods
- [ ] Other services: All Student usages verified and updated
- [ ] All tests pass: `./gradlew test`
- [ ] No `LazyInitializationException` in any test
- [ ] SQL logging verified (single query with JOINs)
- [ ] No new Checkstyle/PMD violations
- [ ] Code follows existing patterns and conventions
- [ ] JavaDoc comments added to new methods
- [ ] No debug code or commented-out code
- [ ] SQL logging disabled in application.yml

---

#### Task 6.2: Update Tests (if needed)

**Estimated Effort**: 15 minutes

**Dependencies**: Task 6.1

**Description**: Ensure test coverage for new repository methods

**Files to Review**:
- `StudentRepositoryTest.java` - Should have tests for all entity graph methods
- `StudentServiceTest.java` - May need updates if method signatures changed
- Integration tests - Should pass without modification

**Acceptance Criteria**:
- [ ] All new repository methods have unit tests
- [ ] Test coverage >80% maintained
- [ ] No skipped or ignored tests
- [ ] All assertions meaningful and correct

---

## Task Dependency Graph

```
Phase 1: Entity
    1.1 (Student.java LAZY change)

Phase 2: Repository
    1.1 → 2.1 (Add EntityGraph methods)

Phase 3: Services
    2.1 → 3.1 (Update StudentService)
    2.1 → 3.2 (Verify other services)

Phase 4: Testing
    2.1 → 4.1 (Write repository tests)
    3.1, 3.2 → 4.2 (Run full test suite)

Phase 5: Performance
    4.2 → 5.1 (SQL logging verification)
    5.1 → 5.2 (Benchmark - optional)

Phase 6: Final
    5.1 → 6.1 (Code review)
    6.1 → 6.2 (Test coverage check)
```

---

## Parallel Work Opportunities

**Can be done simultaneously:**
- Task 4.1 (Write repository tests) + Task 3.2 (Verify other services)
- Task 5.2 (Benchmark) is optional and can be skipped if time-constrained

**Must be sequential:**
- Entity change → Repository methods → Service updates → Testing
- All tests passing → SQL logging verification

---

## Effort Summary

| Phase | Task | Effort | Cumulative |
|-------|------|--------|------------|
| 1 | Entity modification | 5 min | 5 min |
| 2 | Repository methods | 45 min | 50 min |
| 3 | Service updates (StudentService) | 30 min | 80 min |
| 3 | Service updates (Other services) | 45 min | 125 min |
| 4 | Repository tests | 60 min | 185 min |
| 4 | Full test suite | 60 min | 245 min |
| 5 | SQL logging verification | 30 min | 275 min |
| 5 | Benchmark (optional) | 30 min | 305 min (optional) |
| 6 | Code review | 15 min | 320 min |
| 6 | Test coverage check | 15 min | 335 min |
| **TOTAL (with optional)** | | **5.6 hours** | |
| **TOTAL (without benchmark)** | | **5.1 hours** | |

**Realistic Estimate**: **3-4 hours** for experienced developer familiar with codebase

---

## Implementation Order

**Recommended Sequence** (single developer):
1. ✅ **Phase 1**: Entity modification (5 min) — Quick win
2. ✅ **Phase 2**: Repository methods (45 min) — Core change
3. ✅ **Phase 3**: Service updates (75 min total) — Fix all callers
4. ✅ **Phase 4**: Write & run tests (120 min) — Verify correctness
5. ✅ **Phase 5**: Performance validation (30-60 min) — Measure improvement
6. ✅ **Phase 6**: Final checks (30 min) — Quality assurance

**Checkpoint Strategy**:
- After Phase 2: Compile and ensure no syntax errors
- After Phase 3: Run tests (expect failures)
- After Phase 4: All tests must pass before proceeding
- After Phase 5: SQL logs must show single query

---

## Pre-Implementation Checklist

Before starting:
- [ ] Understand EAGER vs LAZY loading in JPA/Hibernate
- [ ] Understand `@EntityGraph` and how it works
- [ ] Understand `@Transactional` boundaries
- [ ] Review solution design document
- [ ] Development environment running (backend compiles, tests pass)
- [ ] Git branch created: `feature/TASK-1.07-lazy-loading`

---

## Post-Implementation Checklist

After completing all tasks:
- [ ] All tests passing (100%)
- [ ] Zero `LazyInitializationException`
- [ ] SQL logging shows optimized queries (single JOIN)
- [ ] Query count reduced by >99%
- [ ] Code review checklist complete
- [ ] No Checkstyle/PMD violations
- [ ] Performance improvement measured and documented
- [ ] SQL logging disabled in committed code
- [ ] Git commit with clear message
- [ ] Ready for pull request

---

## Rollback Plan

**If issues discovered after deployment:**

**Immediate Rollback** (< 5 minutes):
1. Revert Student.java changes: `FetchType.LAZY` → `FetchType.EAGER` (3 lines)
2. Commit and deploy
3. System reverts to original behavior

**Targeted Fix** (if specific service fails):
1. Identify failing service via error logs
2. Add `@EntityGraph` to repository method
3. Update service to use entity graph method
4. Deploy hotfix (faster than full rollback)

**No database changes required** — schema unchanged, zero migration risk

---

## Success Criteria

**Code Quality**:
- ✅ All associations use `FetchType.LAZY`
- ✅ Entity graph methods added for all use cases
- ✅ Services updated to use appropriate repository methods
- ✅ All code follows existing conventions

**Testing**:
- ✅ 100% of existing tests pass
- ✅ Zero `LazyInitializationException`
- ✅ New repository methods have unit tests
- ✅ Test coverage >80%

**Performance**:
- ✅ Query count reduced by 99% (301 → 1-2 for 100-student list)
- ✅ Single SELECT with LEFT JOINs observed in logs
- ✅ API response time improved by >50%
- ✅ No N+1 query pattern detected

**Production Ready**:
- ✅ All acceptance criteria from business analysis met
- ✅ Rollback plan tested and documented
- ✅ Code reviewed and approved
- ✅ Ready for staging deployment

---

**Next Step**: Begin implementation with Task 1.1 (Entity modification). Estimated time to first commit: 5 minutes. Estimated time to completion: 3-4 hours.
