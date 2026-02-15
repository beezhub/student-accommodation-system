package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.exceptions.ApplicationNotFoundException;
import com.beezhub.student_accommodation.exceptions.InvalidStatusException;
import com.beezhub.student_accommodation.mapper.ApplicationMapper;
import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.model.dto.RequiredDocumentsChecklistResponse;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.application.ApplicationService;
import com.beezhub.student_accommodation.service.application.ApplicationDocumentService;
import com.beezhub.student_accommodation.service.AuthService;
import com.beezhub.student_accommodation.service.NotificationService;
import com.beezhub.student_accommodation.service.SystemSettingService;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.service.UserNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationAdminServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentTypeRepository documentTypeRepository;
    @Mock
    private ApplicationMapper applicationMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserNotificationService userNotificationService;
    @Mock
    private AuthService authService;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private ApplicationService applicationService;
    @Mock
    private DocumentCommentRepository documentCommentRepository;
    @Mock
    private ApplicationDocumentService applicationDocumentService;
    @Mock
    private ApplicationQueryService applicationQueryService;
    @Mock
    private com.beezhub.student_accommodation.service.RoomAllocationService roomAllocationService;

    @InjectMocks
    private ApplicationAdminService applicationAdminService;

    private Application application;

    @BeforeEach
    void setUp() {
        AppUser appUser = new AppUser();
        appUser.setId(100L);
        appUser.setEmail("student@example.com");

        Student student = new Student();
        student.setId(50L);
        student.setAppUser(appUser);

        application = Application.builder()
                .id(1L)
                .applicationCode("APP-2024-001")
                .status(ApplicationStatus.PENDING)
                .student(student)
                .build();
    }

    @Test
    void moveToAllocationPhase_shouldThrowException_whenNotApproved() {
        application.setStatus(ApplicationStatus.PENDING);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationAdminService.moveToAllocationPhase(1L, 2L))
                .isInstanceOf(InvalidStatusException.class)
                .hasMessageContaining("Application must be APPROVED");
    }

    @Test
    void markUnderReview_shouldUpdateStatus() {
        application.setStatus(ApplicationStatus.PENDING);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(new ApplicationResponse());

        ApplicationResponse response = applicationAdminService.markUnderReview(1L);

        assertThat(response).isNotNull();
        verify(applicationRepository).save(application);
        // markUnderReview does not send notification
    }

    @Test
    void requestChanges_shouldUpdateStatusAndReason() {
        application.setStatus(ApplicationStatus.UNDER_REVIEW);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(new ApplicationResponse());

        ApplicationResponse response = applicationAdminService.requestChanges(1L, "Missing details");

        assertThat(response).isNotNull();
        assertThat(application.getRemarks()).isEqualTo("Missing details");
        verify(applicationRepository).save(application);
        verify(notificationService).sendApplicationStatusNotification(application, ApplicationStatus.CHANGES_REQUESTED);
    }

    @Test
    void getRequiredDocumentsChecklist_shouldReturnChecklist() {
        RequiredDocumentsChecklistResponse checklistResponse = new RequiredDocumentsChecklistResponse();
        checklistResponse.setTotalRequired(1);
        checklistResponse.setApproved(1);

        when(applicationDocumentService.getRequiredDocumentsChecklist(1L)).thenReturn(checklistResponse);

        RequiredDocumentsChecklistResponse response = applicationAdminService.getRequiredDocumentsChecklist(1L);

        assertThat(response.getTotalRequired()).isEqualTo(1);
        assertThat(response.getApproved()).isEqualTo(1);
        verify(applicationDocumentService).getRequiredDocumentsChecklist(1L);
    }

    @Test
    void getApplicationFullDetailsEnhanced_shouldReturnFullRequirements() {
        // Setup
        AppUser adminUser = new AppUser();
        adminUser.setId(200L);
        adminUser.setUserRole(com.beezhub.student_accommodation.model.enums.UserRole.ADMIN);

        Student student = application.getStudent();
        student.setYearOfStudy(new com.beezhub.student_accommodation.model.entity.YearOfStudy());
        student.getYearOfStudy().setYearOfStudy("1");

        // Create expected response
        com.beezhub.student_accommodation.model.dto.ApplicationFullDetailsResponse expectedResponse = 
            com.beezhub.student_accommodation.model.dto.ApplicationFullDetailsResponse.builder()
                .id(1L)
                .applicationCode("APP-2024-001")
                .status("PENDING")
                .studentProfile(com.beezhub.student_accommodation.model.dto.ApplicationFullDetailsResponse.StudentProfile.builder()
                    .id(student.getId())
                    .build())
                .requiredDocumentsChecklist(RequiredDocumentsChecklistResponse.builder()
                    .totalRequired(1)
                    .build())
                .build();

        // Mock delegation to ApplicationQueryService
        when(applicationQueryService.getApplicationFullDetailsEnhanced(1L, "admin"))
            .thenReturn(expectedResponse);

        // Execute
        com.beezhub.student_accommodation.model.dto.ApplicationFullDetailsResponse response = applicationAdminService
                .getApplicationFullDetailsEnhanced(1L, "admin");

        // Verify delegation occurred
        verify(applicationQueryService).getApplicationFullDetailsEnhanced(1L, "admin");
        
        // Verify response
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStudentProfile()).isNotNull();
        assertThat(response.getRequiredDocumentsChecklist()).isNotNull();
    }
}
