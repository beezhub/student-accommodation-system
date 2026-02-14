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
import com.beezhub.student_accommodation.service.ApplicationService;
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
        DocumentType docType = new DocumentType();
        docType.setId(1L);
        docType.setTypeName("ID");
        docType.setIsRequired(true);

        Document doc = new Document();
        doc.setDocumentType(docType);
        doc.setDocumentStatus(DocumentStatus.APPROVED);
        doc.setApplication(application);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(documentTypeRepository.findByIsRequiredTrue()).thenReturn(Collections.singletonList(docType));
        when(documentRepository.findByApplicationId(1L)).thenReturn(Collections.singletonList(doc));

        RequiredDocumentsChecklistResponse response = applicationAdminService.getRequiredDocumentsChecklist(1L);

        assertThat(response.getTotalRequired()).isEqualTo(1);
        assertThat(response.getApproved()).isEqualTo(1);
    }
}
