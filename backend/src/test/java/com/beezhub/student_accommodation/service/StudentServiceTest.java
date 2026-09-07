package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.mapper.StudentMapper;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.dto.StudentRequest;
import com.beezhub.student_accommodation.model.dto.StudentResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Institution;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.YearOfStudy;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentMapper studentMapper;

    @Mock
    private AuthService authService;

    @Mock
    private com.beezhub.student_accommodation.repository.UserRepository userRepository;

    @InjectMocks
    private StudentService studentService;

    private Student student1;
    private Student student2;
    private StudentResponse response1;
    private StudentResponse response2;
    private AppUser appUser1;
    private AppUser appUser2;
    private AppUser adminUser;
    private Institution institution;
    private YearOfStudy yearOfStudy;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        institution = new Institution();
        institution.setId(1L);
        institution.setName("Test University");

        yearOfStudy = new YearOfStudy();
        yearOfStudy.setId(1L);
        yearOfStudy.setYearOfStudy("1");

        appUser1 = AppUser.builder()
                .id(1L)
                .email("student1@test.com")
                .firstName("John")
                .lastName("Doe")
                .userRole(UserRole.STUDENT)
                .build();

        appUser2 = AppUser.builder()
                .id(2L)
                .email("student2@test.com")
                .firstName("Jane")
                .lastName("Smith")
                .userRole(UserRole.STUDENT)
                .build();

        adminUser = AppUser.builder()
                .id(3L)
                .email("admin@test.com")
                .userRole(UserRole.ADMIN)
                .build();

        student1 = new Student();
        student1.setId(1L);
        student1.setStudentNumber("STU001");
        student1.setAppUser(appUser1);
        student1.setInstitution(institution);
        student1.setYearOfStudy(yearOfStudy);
        student1.setGender("MALE");
        student1.setDateOfBirth(new java.util.Date());

        student2 = new Student();
        student2.setId(2L);
        student2.setStudentNumber("STU002");
        student2.setAppUser(appUser2);
        student2.setInstitution(institution);
        student2.setYearOfStudy(yearOfStudy);
        student2.setGender("FEMALE");
        student2.setDateOfBirth(new java.util.Date());

        response1 = new StudentResponse();
        response1.setId(1L);
        response1.setStudentNumber("STU001");
        response1.setUserId(1L);

        response2 = new StudentResponse();
        response2.setId(2L);
        response2.setStudentNumber("STU002");
        response2.setUserId(2L);

        pageable = PageRequest.of(0, 20);
    }

    @Test
    void getAllStudents_ShouldReturnList() {
        // Arrange
        when(studentRepository.findAllStudentsWithDetails()).thenReturn(Arrays.asList(student1, student2));
        when(studentMapper.toResponse(student1)).thenReturn(response1);
        when(studentMapper.toResponse(student2)).thenReturn(response2);

        // Act
        List<StudentResponse> result = studentService.getAllStudents();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(studentRepository).findAllStudentsWithDetails();
    }

    @Test
    void getAllStudents_WithPageable_ShouldReturnPageResponse() {
        // Arrange
        Page<Student> page = new PageImpl<>(Arrays.asList(student1, student2), pageable, 2);
        when(studentRepository.findAll(pageable)).thenReturn(page);
        when(studentMapper.toResponse(student1)).thenReturn(response1);
        when(studentMapper.toResponse(student2)).thenReturn(response2);

        // Act
        PageResponse<StudentResponse> result = studentService.getAllStudents(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements());
        verify(studentRepository).findAll(pageable);
    }

    @Test
    void searchStudents_WithNameFilter_ShouldReturnFilteredResults() {
        // Arrange
        Page<Student> page = new PageImpl<>(Arrays.asList(student1), pageable, 1);
        when(studentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(studentMapper.toResponse(student1)).thenReturn(response1);

        // Act
        PageResponse<StudentResponse> result = studentService.searchStudents("John", null, null, null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("STU001", result.getContent().get(0).getStudentNumber());
        verify(studentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchStudents_WithStudentNumberFilter_ShouldReturnFilteredResults() {
        // Arrange
        Page<Student> page = new PageImpl<>(Arrays.asList(student1), pageable, 1);
        when(studentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(studentMapper.toResponse(student1)).thenReturn(response1);

        // Act
        PageResponse<StudentResponse> result = studentService.searchStudents(null, "STU001", null, null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("STU001", result.getContent().get(0).getStudentNumber());
        verify(studentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchStudents_WithEmailFilter_ShouldReturnFilteredResults() {
        // Arrange
        Page<Student> page = new PageImpl<>(Arrays.asList(student1), pageable, 1);
        when(studentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(studentMapper.toResponse(student1)).thenReturn(response1);

        // Act
        PageResponse<StudentResponse> result = studentService.searchStudents(null, null, "student1@test.com", null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(studentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchStudents_WithInstitutionFilter_ShouldReturnFilteredResults() {
        // Arrange
        Page<Student> page = new PageImpl<>(Arrays.asList(student1, student2), pageable, 2);
        when(studentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(studentMapper.toResponse(student1)).thenReturn(response1);
        when(studentMapper.toResponse(student2)).thenReturn(response2);

        // Act
        PageResponse<StudentResponse> result = studentService.searchStudents(null, null, null, 1L, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(studentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchStudents_WithYearOfStudyFilter_ShouldReturnFilteredResults() {
        // Arrange
        Page<Student> page = new PageImpl<>(Arrays.asList(student1, student2), pageable, 2);
        when(studentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(studentMapper.toResponse(student1)).thenReturn(response1);
        when(studentMapper.toResponse(student2)).thenReturn(response2);

        // Act
        PageResponse<StudentResponse> result = studentService.searchStudents(null, null, null, null, 1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(studentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchStudents_WithMultipleFilters_ShouldReturnFilteredResults() {
        // Arrange
        Page<Student> page = new PageImpl<>(Arrays.asList(student1), pageable, 1);
        when(studentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(studentMapper.toResponse(student1)).thenReturn(response1);

        // Act
        PageResponse<StudentResponse> result = studentService.searchStudents("John", "STU001", null, 1L, 1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(studentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchStudents_NoFilters_ShouldReturnAllResults() {
        // Arrange
        Page<Student> page = new PageImpl<>(Arrays.asList(student1, student2), pageable, 2);
        when(studentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(studentMapper.toResponse(student1)).thenReturn(response1);
        when(studentMapper.toResponse(student2)).thenReturn(response2);

        // Act
        PageResponse<StudentResponse> result = studentService.searchStudents(null, null, null, null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(studentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getStudentByUserId_AsOwner_ShouldReturnStudent() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(appUser1);
        when(studentRepository.findStudentWithDetailsByUserId(1L)).thenReturn(Optional.of(student1));
        when(studentMapper.toResponse(student1)).thenReturn(response1);

        // Act
        StudentResponse result = studentService.getStudentByUserId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(studentRepository).findStudentWithDetailsByUserId(1L);
    }

    @Test
    void getStudentByUserId_AsAdmin_ShouldReturnStudent() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(adminUser);
        when(studentRepository.findStudentWithDetailsByUserId(1L)).thenReturn(Optional.of(student1));
        when(studentMapper.toResponse(student1)).thenReturn(response1);

        // Act
        StudentResponse result = studentService.getStudentByUserId(1L);

        // Assert
        assertNotNull(result);
        verify(studentRepository).findStudentWithDetailsByUserId(1L);
    }

    @Test
    void getStudentByUserId_Unauthorized_ShouldThrowException() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(appUser2);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> studentService.getStudentByUserId(1L));
    }

    @Test
    void getMyProfile_ShouldReturnStudentProfile() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(appUser1);
        when(studentRepository.findStudentWithDetailsByUserId(1L)).thenReturn(Optional.of(student1));
        when(studentMapper.toResponse(student1)).thenReturn(response1);

        // Act
        StudentResponse result = studentService.getMyProfile();

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(studentRepository).findStudentWithDetailsByUserId(1L);
    }

    @Test
    void updateStudent_AsOwner_ShouldUpdateStudent() {
        // Arrange
        StudentRequest request = new StudentRequest();
        request.setStudentNumber("STU001-UPDATED");
        request.setPhoneNumber("123-456-7890");

        when(authService.getCurrentUser()).thenReturn(appUser1);
        when(studentRepository.findStudentWithDetailsByUserId(1L)).thenReturn(Optional.of(student1));
        when(studentRepository.save(student1)).thenReturn(student1);
        when(studentMapper.toResponse(student1)).thenReturn(response1);

        // Act
        StudentResponse result = studentService.updateStudent(1L, request);

        // Assert
        assertNotNull(result);
        verify(studentRepository).save(student1);
    }

    @Test
    void deleteStudentByUserId_ShouldDeleteStudent() {
        // Arrange
        when(studentRepository.findByAppUser_Id(1L)).thenReturn(Optional.of(student1));

        // Act
        studentService.deleteStudentByUserId(1L);

        // Assert
        verify(studentRepository).delete(student1);
    }

    // ── GH-239: the emergency contact ───────────────────────────────────────────────────────

    /**
     * The contact is someone else's personal data, so it has to be removable. Every other field on
     * this method treats null as "not sent, leave alone"; if blank were treated the same way, a
     * contact could never be withdrawn once set.
     */
    @Test
    void updateStudent_blankEmergencyContact_clearsItRatherThanLeavingItInPlace() {
        student1.setEmergencyContactName("Thandi Mokoena");
        student1.setEmergencyContactRelationship("Mother");
        student1.setEmergencyContactPhone("+27 82 123 4567");
        student1.setEmergencyContactEmail("thandi@example.com");

        StudentRequest request = new StudentRequest();
        request.setEmergencyContactName("");
        request.setEmergencyContactRelationship("");
        request.setEmergencyContactPhone("");
        request.setEmergencyContactEmail("");

        when(authService.getCurrentUser()).thenReturn(appUser1);
        when(studentRepository.findStudentWithDetailsByUserId(1L)).thenReturn(Optional.of(student1));
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));
        when(studentMapper.toResponse(any())).thenReturn(response1);

        studentService.updateStudent(1L, request);

        assertNull(student1.getEmergencyContactName());
        assertNull(student1.getEmergencyContactRelationship());
        assertNull(student1.getEmergencyContactPhone());
        assertNull(student1.getEmergencyContactEmail());
    }

    @Test
    void updateStudent_newEmergencyContact_isStoredTrimmed() {
        StudentRequest request = new StudentRequest();
        request.setEmergencyContactName("  Thandi Mokoena  ");
        request.setEmergencyContactPhone(" +27 82 123 4567 ");

        when(authService.getCurrentUser()).thenReturn(appUser1);
        when(studentRepository.findStudentWithDetailsByUserId(1L)).thenReturn(Optional.of(student1));
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));
        when(studentMapper.toResponse(any())).thenReturn(response1);

        studentService.updateStudent(1L, request);

        assertEquals("Thandi Mokoena", student1.getEmergencyContactName());
        assertEquals("+27 82 123 4567", student1.getEmergencyContactPhone());
    }

    /** A field not sent at all is untouched, matching every other field on this method. */
    @Test
    void updateStudent_omittedEmergencyContact_leavesTheExistingOneAlone() {
        student1.setEmergencyContactName("Thandi Mokoena");
        student1.setEmergencyContactPhone("+27 82 123 4567");

        StudentRequest request = new StudentRequest();
        request.setGender("Female");

        when(authService.getCurrentUser()).thenReturn(appUser1);
        when(studentRepository.findStudentWithDetailsByUserId(1L)).thenReturn(Optional.of(student1));
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));
        when(studentMapper.toResponse(any())).thenReturn(response1);

        studentService.updateStudent(1L, request);

        assertEquals("Thandi Mokoena", student1.getEmergencyContactName());
        assertEquals("+27 82 123 4567", student1.getEmergencyContactPhone());
    }
}
