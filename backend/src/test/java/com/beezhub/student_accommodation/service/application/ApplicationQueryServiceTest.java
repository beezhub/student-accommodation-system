package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.exceptions.ApplicationNotFoundException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.model.dto.ApplicationFullDetailsResponse;
import com.beezhub.student_accommodation.model.dto.DocumentWithCommentsResponse;
import com.beezhub.student_accommodation.model.dto.RequiredDocumentsChecklistResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Institution;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.YearOfStudy;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationQueryServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private AuthService authService;
    @Mock
    private ApplicationDocumentService applicationDocumentService;

    @InjectMocks
    private ApplicationQueryService applicationQueryService;

    private Application application;
    private AppUser studentUser;
    private Student student;

    @BeforeEach
    void setUp() {
        // Setup student user
        studentUser = new AppUser();
        studentUser.setId(100L);
        studentUser.setEmail("student@example.com");
        studentUser.setFirstName("John");
        studentUser.setLastName("Doe");
        studentUser.setUserRole(UserRole.STUDENT);

        // Setup year of study
        YearOfStudy yearOfStudy = new YearOfStudy();
        yearOfStudy.setId(1L);
        yearOfStudy.setYearOfStudy("1");

        // Setup institution
        Institution institution = new Institution();
        institution.setId(1L);
        institution.setName("Test University");

        // Setup student
        student = new Student();
        student.setId(50L);
        student.setStudentNumber("STU12345");
        student.setAppUser(studentUser);
        student.setPhoneNumber("1234567890");
        student.setGender("Male");
        student.setSpecialRequirements("None");
        student.setYearOfStudy(yearOfStudy);
        student.setInstitution(institution);

        // Setup application
        application = Application.builder()
                .id(1L)
                .applicationCode("APP-2024-001")
                .status(ApplicationStatus.PENDING)
                .applicationDate(LocalDate.now())
                .student(student)
                .requiresAccessibility(false)
                .specialRequirements("None")
                .institutionName("Test University")
                .courseName("Computer Science")
                .build();
    }

    @Test
    void getApplicationFullDetailsEnhanced_asAdmin_shouldReturnFullDetails() {
        // Setup
        AppUser adminUser = new AppUser();
        adminUser.setId(200L);
        adminUser.setUserRole(UserRole.ADMIN);

        RequiredDocumentsChecklistResponse checklist = RequiredDocumentsChecklistResponse.builder()
                .totalRequired(2)
                .approved(1)
                .pending(1)
                .rejected(0)
                .notUploaded(0)
                .allApproved(false)
                .build();

        when(authService.getCurrentUser()).thenReturn(adminUser);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationDocumentService.getDocumentsWithComments(1L))
                .thenReturn(Collections.emptyList());
        when(applicationDocumentService.getRequiredDocumentsChecklist(1L))
                .thenReturn(checklist);

        // Execute
        ApplicationFullDetailsResponse response = applicationQueryService
                .getApplicationFullDetailsEnhanced(1L, "admin@example.com");

        // Verify
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getApplicationCode()).isEqualTo("APP-2024-001");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        
        // Verify student profile
        assertThat(response.getStudentProfile()).isNotNull();
        assertThat(response.getStudentProfile().getStudentNumber()).isEqualTo("STU12345");
        assertThat(response.getStudentProfile().getFirstName()).isEqualTo("John");
        assertThat(response.getStudentProfile().getLastName()).isEqualTo("Doe");
        assertThat(response.getStudentProfile().getEmail()).isEqualTo("student@example.com");
        
        // Verify documents and checklist
        assertThat(response.getDocuments()).isEmpty();
        assertThat(response.getRequiredDocumentsChecklist()).isEqualTo(checklist);
        
        // Verify allocation eligibility
        assertThat(response.getAllocationEligibility()).isNotNull();
        assertThat(response.getAllocationEligibility().getEligible()).isFalse();
        assertThat(response.getAllocationEligibility().getApplicationApproved()).isFalse();
        assertThat(response.getAllocationEligibility().getAllDocumentsApproved()).isFalse();
    }

    @Test
    void getApplicationFullDetailsEnhanced_asOwner_shouldReturnFullDetails() {
        // Setup - student accessing their own application
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationDocumentService.getDocumentsWithComments(1L))
                .thenReturn(Collections.emptyList());
        when(applicationDocumentService.getRequiredDocumentsChecklist(1L))
                .thenReturn(RequiredDocumentsChecklistResponse.builder()
                        .totalRequired(0)
                        .allApproved(true)
                        .build());

        // Execute
        ApplicationFullDetailsResponse response = applicationQueryService
                .getApplicationFullDetailsEnhanced(1L, "student@example.com");

        // Verify
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStudentProfile()).isNotNull();
    }

    @Test
    void getApplicationFullDetailsEnhanced_asUnauthorizedUser_shouldThrowException() {
        // Setup - different student trying to access application
        AppUser otherUser = new AppUser();
        otherUser.setId(999L);
        otherUser.setUserRole(UserRole.STUDENT);

        when(authService.getCurrentUser()).thenReturn(otherUser);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        // Execute & Verify
        assertThatThrownBy(() -> applicationQueryService
                .getApplicationFullDetailsEnhanced(1L, "other@example.com"))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("not authorized");
    }

    @Test
    void getApplicationFullDetailsEnhanced_nonExistentApplication_shouldThrowException() {
        // Setup
        AppUser adminUser = new AppUser();
        adminUser.setId(200L);
        adminUser.setUserRole(UserRole.ADMIN);

        when(authService.getCurrentUser()).thenReturn(adminUser);
        when(applicationRepository.findById(999L)).thenReturn(Optional.empty());

        // Execute & Verify
        assertThatThrownBy(() -> applicationQueryService
                .getApplicationFullDetailsEnhanced(999L, "admin@example.com"))
                .isInstanceOf(ApplicationNotFoundException.class)
                .hasMessageContaining("Application not found");
    }

    @Test
    void getApplicationFullDetailsEnhanced_approvedWithAllDocs_shouldBeEligible() {
        // Setup - application approved and all docs approved
        application.setStatus(ApplicationStatus.APPROVED);
        
        RequiredDocumentsChecklistResponse checklist = RequiredDocumentsChecklistResponse.builder()
                .totalRequired(2)
                .approved(2)
                .allApproved(true)
                .build();

        AppUser adminUser = new AppUser();
        adminUser.setId(200L);
        adminUser.setUserRole(UserRole.ADMIN);

        when(authService.getCurrentUser()).thenReturn(adminUser);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationDocumentService.getDocumentsWithComments(1L))
                .thenReturn(Collections.emptyList());
        when(applicationDocumentService.getRequiredDocumentsChecklist(1L))
                .thenReturn(checklist);

        // Execute
        ApplicationFullDetailsResponse response = applicationQueryService
                .getApplicationFullDetailsEnhanced(1L, "admin@example.com");

        // Verify allocation eligibility
        assertThat(response.getAllocationEligibility().getEligible()).isTrue();
        assertThat(response.getAllocationEligibility().getApplicationApproved()).isTrue();
        assertThat(response.getAllocationEligibility().getAllDocumentsApproved()).isTrue();
        assertThat(response.getAllocationEligibility().getReason()).isNull();
    }

    @Test
    void getApplicationFullDetailsEnhanced_approvedButMissingDocs_shouldNotBeEligible() {
        // Setup - application approved but docs not all approved
        application.setStatus(ApplicationStatus.APPROVED);
        
        RequiredDocumentsChecklistResponse checklist = RequiredDocumentsChecklistResponse.builder()
                .totalRequired(2)
                .approved(1)
                .allApproved(false)
                .build();

        AppUser adminUser = new AppUser();
        adminUser.setId(200L);
        adminUser.setUserRole(UserRole.ADMIN);

        when(authService.getCurrentUser()).thenReturn(adminUser);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationDocumentService.getDocumentsWithComments(1L))
                .thenReturn(Collections.emptyList());
        when(applicationDocumentService.getRequiredDocumentsChecklist(1L))
                .thenReturn(checklist);

        // Execute
        ApplicationFullDetailsResponse response = applicationQueryService
                .getApplicationFullDetailsEnhanced(1L, "admin@example.com");

        // Verify allocation eligibility
        assertThat(response.getAllocationEligibility().getEligible()).isFalse();
        assertThat(response.getAllocationEligibility().getApplicationApproved()).isTrue();
        assertThat(response.getAllocationEligibility().getAllDocumentsApproved()).isFalse();
        assertThat(response.getAllocationEligibility().getReason())
                .contains("Not all required documents are approved");
    }
}
