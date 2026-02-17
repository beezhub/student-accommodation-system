package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Institution;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.YearOfStudy;
import com.beezhub.student_accommodation.model.enums.UserRole;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StudentRepository EntityGraph methods.
 * Verifies that lazy loading is configured correctly and entity graphs work as expected.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StudentRepositoryTest {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Student testStudent;
    private AppUser testUser;
    private Institution testInstitution;
    private YearOfStudy testYearOfStudy;

    @BeforeEach
    void setUp() {
        // Create test AppUser
        testUser = new AppUser();
        testUser.setEmail("test.student@university.edu");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setUserPassword("hashedPassword123");
        testUser.setUserRole(UserRole.STUDENT);
        testUser.setEmailVerified(true);
        entityManager.persist(testUser);

        // Create test Institution
        testInstitution = new Institution();
        testInstitution.setName("Test University");
        entityManager.persist(testInstitution);

        // Create test YearOfStudy
        testYearOfStudy = new YearOfStudy();
        testYearOfStudy.setYearOfStudy("1");
        entityManager.persist(testYearOfStudy);

        // Create test Student
        testStudent = new Student();
        testStudent.setStudentNumber("S2024001");
        testStudent.setPhoneNumber("+1234567890");
        testStudent.setGender("Male");
        testStudent.setAppUser(testUser);
        testStudent.setInstitution(testInstitution);
        testStudent.setYearOfStudy(testYearOfStudy);
        testStudent = entityManager.persist(testStudent);

        // Flush and clear to ensure fresh load from database
        entityManager.flush();
        entityManager.clear();
    }

    // ========================================
    // Test 1: Verify LAZY loading is configured
    // ========================================

    @Test
    void findById_shouldNotLoadAssociations_whenUsingDefaultMethod() {
        // When
        Optional<Student> result = studentRepository.findById(testStudent.getId());

        // Then
        assertTrue(result.isPresent(), "Student should be found");
        Student student = result.get();

        // Verify associations are NOT initialized (lazy)
        assertFalse(Hibernate.isInitialized(student.getAppUser()),
                "AppUser should NOT be initialized with default findById");
        assertFalse(Hibernate.isInitialized(student.getInstitution()),
                "Institution should NOT be initialized with default findById");
        assertFalse(Hibernate.isInitialized(student.getYearOfStudy()),
                "YearOfStudy should NOT be initialized with default findById");
    }

    // ========================================
    // Test 2: Full fetch methods
    // ========================================

    @Test
    void findStudentWithDetailsById_shouldLoadAllAssociations() {
        // When
        Optional<Student> result = studentRepository.findStudentWithDetailsById(testStudent.getId());

        // Then
        assertTrue(result.isPresent(), "Student should be found");
        Student student = result.get();

        // Verify all associations ARE initialized
        assertTrue(Hibernate.isInitialized(student.getAppUser()),
                "AppUser should be initialized");
        assertTrue(Hibernate.isInitialized(student.getInstitution()),
                "Institution should be initialized");
        assertTrue(Hibernate.isInitialized(student.getYearOfStudy()),
                "YearOfStudy should be initialized");

        // Verify data correctness
        assertNotNull(student.getAppUser(), "AppUser should not be null");
        assertEquals("test.student@university.edu", student.getAppUser().getEmail());
        assertEquals("John", student.getAppUser().getFirstName());
        assertEquals("Doe", student.getAppUser().getLastName());

        assertNotNull(student.getInstitution(), "Institution should not be null");
        assertEquals("Test University", student.getInstitution().getName());

        assertNotNull(student.getYearOfStudy(), "YearOfStudy should not be null");
        assertEquals("1", student.getYearOfStudy().getYearOfStudy());
    }

    @Test
    void findStudentWithDetailsByUserId_shouldLoadAllAssociations() {
        // When
        Optional<Student> result = studentRepository.findStudentWithDetailsByUserId(testUser.getId());

        // Then
        assertTrue(result.isPresent(), "Student should be found by user ID");
        Student student = result.get();

        // Verify all associations ARE initialized
        assertTrue(Hibernate.isInitialized(student.getAppUser()),
                "AppUser should be initialized");
        assertTrue(Hibernate.isInitialized(student.getInstitution()),
                "Institution should be initialized");
        assertTrue(Hibernate.isInitialized(student.getYearOfStudy()),
                "YearOfStudy should be initialized");

        // Verify correct student was retrieved
        assertEquals(testStudent.getId(), student.getId());
        assertEquals("S2024001", student.getStudentNumber());
    }

    @Test
    void findAllStudentsWithDetails_shouldLoadAllAssociationsForAllStudents() {
        // Given - create another student for list test
        AppUser user2 = new AppUser();
        user2.setEmail("jane.smith@university.edu");
        user2.setFirstName("Jane");
        user2.setLastName("Smith");
        user2.setUserPassword("hashedPassword456");
        user2.setUserRole(UserRole.STUDENT);
        user2.setEmailVerified(true);
        entityManager.persist(user2);

        Student student2 = new Student();
        student2.setStudentNumber("S2024002");
        student2.setPhoneNumber("+0987654321");
        student2.setGender("Female");
        student2.setAppUser(user2);
        student2.setInstitution(testInstitution);
        student2.setYearOfStudy(testYearOfStudy);
        entityManager.persist(student2);

        entityManager.flush();
        entityManager.clear();

        // When
        List<Student> students = studentRepository.findAllStudentsWithDetails();

        // Then
        assertFalse(students.isEmpty(), "Should find students");
        assertTrue(students.size() >= 2, "Should find at least 2 students");

        // Verify all associations loaded for all students
        for (Student student : students) {
            assertTrue(Hibernate.isInitialized(student.getAppUser()),
                    "AppUser should be initialized for student " + student.getId());
            assertTrue(Hibernate.isInitialized(student.getInstitution()),
                    "Institution should be initialized for student " + student.getId());
            assertTrue(Hibernate.isInitialized(student.getYearOfStudy()),
                    "YearOfStudy should be initialized for student " + student.getId());
        }
    }

    // ========================================
    // Test 3: Partial fetch - user only
    // ========================================

    @Test
    void findStudentWithUserById_shouldLoadOnlyAppUser() {
        // When
        Optional<Student> result = studentRepository.findStudentWithUserById(testStudent.getId());

        // Then
        assertTrue(result.isPresent(), "Student should be found");
        Student student = result.get();

        // Verify only appUser is initialized
        assertTrue(Hibernate.isInitialized(student.getAppUser()),
                "AppUser should be initialized");
        assertFalse(Hibernate.isInitialized(student.getInstitution()),
                "Institution should NOT be initialized");
        assertFalse(Hibernate.isInitialized(student.getYearOfStudy()),
                "YearOfStudy should NOT be initialized");

        // Verify appUser data is correct
        assertNotNull(student.getAppUser());
        assertEquals("test.student@university.edu", student.getAppUser().getEmail());
    }

    @Test
    void findStudentWithUserByAppUser_shouldLoadOnlyAppUser() {
        // Given - fetch the user first
        AppUser user = entityManager.find(AppUser.class, testUser.getId());

        // When
        Optional<Student> result = studentRepository.findStudentWithUserByAppUser(user);

        // Then
        assertTrue(result.isPresent(), "Student should be found by AppUser");
        Student student = result.get();

        // Verify only appUser is initialized
        assertTrue(Hibernate.isInitialized(student.getAppUser()),
                "AppUser should be initialized");
        assertFalse(Hibernate.isInitialized(student.getInstitution()),
                "Institution should NOT be initialized");
        assertFalse(Hibernate.isInitialized(student.getYearOfStudy()),
                "YearOfStudy should NOT be initialized");
    }

    // ========================================
    // Test 4: Partial fetch - academic details
    // ========================================

    @Test
    void findStudentWithAcademicDetailsById_shouldLoadAcademicFieldsOnly() {
        // When
        Optional<Student> result = studentRepository.findStudentWithAcademicDetailsById(testStudent.getId());

        // Then
        assertTrue(result.isPresent(), "Student should be found");
        Student student = result.get();

        // Verify institution and yearOfStudy are initialized, but NOT appUser
        assertFalse(Hibernate.isInitialized(student.getAppUser()),
                "AppUser should NOT be initialized");
        assertTrue(Hibernate.isInitialized(student.getInstitution()),
                "Institution should be initialized");
        assertTrue(Hibernate.isInitialized(student.getYearOfStudy()),
                "YearOfStudy should be initialized");

        // Verify academic data is correct
        assertNotNull(student.getInstitution());
        assertEquals("Test University", student.getInstitution().getName());
        assertNotNull(student.getYearOfStudy());
        assertEquals("1", student.getYearOfStudy().getYearOfStudy());
    }

    // ========================================
    // Test 5: Existing methods still work
    // ========================================

    @Test
    void findStudentByAppUser_shouldReturnStudent() {
        // Given
        AppUser user = entityManager.find(AppUser.class, testUser.getId());

        // When
        Optional<Student> result = studentRepository.findStudentByAppUser(user);

        // Then
        assertTrue(result.isPresent(), "Student should be found by AppUser");
        assertEquals(testStudent.getId(), result.get().getId());
    }

    @Test
    void findByAppUser_Id_shouldReturnStudent() {
        // When
        Optional<Student> result = studentRepository.findByAppUser_Id(testUser.getId());

        // Then
        assertTrue(result.isPresent(), "Student should be found by AppUser ID");
        assertEquals(testStudent.getId(), result.get().getId());
    }

    // ========================================
    // Test 6: Edge cases
    // ========================================

    @Test
    void findStudentWithDetailsById_shouldReturnEmpty_whenStudentNotFound() {
        // When
        Optional<Student> result = studentRepository.findStudentWithDetailsById(999999L);

        // Then
        assertFalse(result.isPresent(), "Should return empty for non-existent student");
    }

    @Test
    void findStudentWithDetailsByUserId_shouldReturnEmpty_whenUserHasNoStudent() {
        // Given - create user without student
        AppUser userWithoutStudent = new AppUser();
        userWithoutStudent.setEmail("no.student@university.edu");
        userWithoutStudent.setFirstName("No");
        userWithoutStudent.setLastName("Student");
        userWithoutStudent.setUserPassword("password");
        userWithoutStudent.setUserRole(UserRole.ADMIN);
        userWithoutStudent.setEmailVerified(true);
        userWithoutStudent = entityManager.persist(userWithoutStudent);
        entityManager.flush();

        // When
        Optional<Student> result = studentRepository.findStudentWithDetailsByUserId(userWithoutStudent.getId());

        // Then
        assertFalse(result.isPresent(), "Should return empty when user has no student profile");
    }

    @Test
    void findAllStudentsWithDetails_shouldReturnEmptyList_whenNoStudentsExist() {
        // Given - delete all students
        studentRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // When
        List<Student> students = studentRepository.findAllStudentsWithDetails();

        // Then
        assertTrue(students.isEmpty(), "Should return empty list when no students exist");
    }
}
