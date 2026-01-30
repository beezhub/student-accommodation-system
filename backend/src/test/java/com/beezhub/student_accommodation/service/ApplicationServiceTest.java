package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.*;
import com.beezhub.student_accommodation.mapper.ApplicationMapper;
import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.NotificationService;
import com.beezhub.student_accommodation.service.UserNotificationService;
import com.beezhub.student_accommodation.service.AuthService;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private BuildingRepository buildingRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentTypeRepository documentTypeRepository;
    @Mock
    private DocumentCommentRepository documentCommentRepository;
    @Mock
    private StagedAllocationRepository stagedAllocationRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private ApplicationMapper applicationMapper;
    @Mock
    private AuthService authService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserNotificationService userNotificationService;

    @InjectMocks
    private ApplicationService applicationService;

    private Student student;
    private AppUser studentUser;
    private Application application;
    private ApplicationRequest applicationRequest;

    @BeforeEach
    void setUp() {
        studentUser = new AppUser();
        studentUser.setId(1L);
        studentUser.setEmail("student@test.com");
        studentUser.setUserRole(UserRole.STUDENT);

        student = new Student();
        student.setId(1L);
        student.setStudentNumber("STU001");
        student.setAppUser(studentUser);

        application = Application.builder()
                .id(1L)
                .applicationCode("APP-2024-001")
                .status(ApplicationStatus.PENDING)
                .student(student)
                .build();

        applicationRequest = new ApplicationRequest();
        applicationRequest.setStudentId(1L);
        applicationRequest.setPreferredMoveInDate(LocalDate.now().plusDays(30));
        applicationRequest.setPreferredDurationMonths(12);
    }

    @Test
    void submitApplication_shouldCreateApplication() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(applicationRepository.existsByStudent_IdAndStatus(1L, ApplicationStatus.PENDING)).thenReturn(false);
        when(applicationRepository.countByApplicationCodeStartingWith(anyString())).thenReturn(0L);
        when(applicationRepository.save(any(Application.class))).thenReturn(application);
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(new ApplicationResponse());

        // Act
        ApplicationResponse response = applicationService.submitApplication(applicationRequest);

        // Assert
        assertThat(response).isNotNull();
        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    void submitApplication_shouldThrowException_whenStudentNotFound() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.submitApplication(applicationRequest))
                .isInstanceOf(StudentNotFoundException.class);
    }

    @Test
    void submitApplication_shouldThrowException_whenDuplicateApplication() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(applicationRepository.existsByStudent_IdAndStatus(1L, ApplicationStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.submitApplication(applicationRequest))
                .isInstanceOf(DuplicateApplicationException.class);
    }

    @Test
    void getApplicationById_shouldReturnApplication() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(applicationMapper.toResponse(application)).thenReturn(new ApplicationResponse());

        ApplicationResponse response = applicationService.getApplicationById(1L);

        assertThat(response).isNotNull();
    }

    @Test
    void updateApplicationStatus_shouldUpdateStatus() {
        ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
        request.setStatus("APPROVED");
        request.setRemarks("Approved");

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(new ApplicationResponse());

        ApplicationResponse response = applicationService.updateApplicationStatus(1L, request);

        assertThat(response).isNotNull();
        verify(applicationRepository).save(application);
        verify(notificationService).sendApplicationStatusNotification(any(Application.class), eq(ApplicationStatus.APPROVED));
    }

    @Test
    void cancelApplication_shouldDeleteApplication() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(authService.getCurrentUser()).thenReturn(studentUser);

        applicationService.cancelApplication(1L);

        verify(applicationRepository).delete(application);
    }

    @Test
    void cancelApplication_shouldThrowException_whenNotPending() {
        application.setStatus(ApplicationStatus.APPROVED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(authService.getCurrentUser()).thenReturn(studentUser);

        assertThatThrownBy(() -> applicationService.cancelApplication(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void moveToAllocationPhase_shouldThrowException_whenNotApproved() {
        application.setStatus(ApplicationStatus.PENDING);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.moveToAllocationPhase(1L, 2L))
                .isInstanceOf(InvalidStatusException.class)
                .hasMessageContaining("Application must be APPROVED");
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
        doc.setApplication(application); // Ensure relation

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(documentTypeRepository.findByIsRequiredTrue()).thenReturn(Collections.singletonList(docType));
        when(documentRepository.findByApplicationId(1L)).thenReturn(Collections.singletonList(doc));

        RequiredDocumentsChecklistResponse response = applicationService.getRequiredDocumentsChecklist(1L);

        assertThat(response.getTotalRequired()).isEqualTo(1);
        assertThat(response.getApproved()).isEqualTo(1);
    }

    @Test
    void searchApplications_shouldReturnPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Application> page = new PageImpl<>(Collections.singletonList(application));
        
        when(applicationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(documentRepository.findByApplicationIdIn(anyList())).thenReturn(Collections.emptyList());
        when(stagedAllocationRepository.findByApplication_IdIn(anyList())).thenReturn(Collections.emptyList());
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(new ApplicationResponse());

        PageResponse<ApplicationResponse> response = applicationService.searchApplications(
                null, null, null, null, null, null, null, pageable);

        assertThat(response.getTotalElements()).isEqualTo(1);
    }
}
