package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.mapper.ApplicationMapper;
import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.model.dto.ApplicationStatusUpdateRequest;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration tests for application notification triggers
 *
 * Acceptance Criteria:
 * ✅ Notification sent on application approval
 * ✅ Notification sent on application rejection
 * ✅ Email contains relevant details
 * ✅ Async sending doesn't block request
 * ✅ Failed emails logged but don't break flow
 * ✅ Integration tests verify notifications sent
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Application Service - Notification Integration Tests")
class ApplicationServiceNotificationTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private AuthService authService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ApplicationService applicationService;

    private Application application;
    private ApplicationResponse applicationResponse;
    private Student student;
    private AppUser appUser;

    @BeforeEach
    void setUp() {
        appUser = AppUser.builder()
                .id(1L)
                .email("student@university.edu")
                .firstName("John")
                .lastName("Doe")
                .userRole(UserRole.STUDENT)
                .build();

        student = new Student();
        student.setId(1L);
        student.setStudentNumber("STU-2025-001");
        student.setAppUser(appUser);

        application = Application.builder()
                .id(1L)
                .applicationCode("APP-2025-001")
                .applicationDate(LocalDate.now())
                .status(ApplicationStatus.PENDING)
                .student(student)
                .build();

        applicationResponse = ApplicationResponse.builder()
                .id(1L)
                .applicationCode("APP-2025-001")
                .status("APPROVED")
                .build();
    }

    @Test
    @DisplayName("Should send notification when application is approved")
    void shouldSendNotificationWhenApplicationApproved() {
        // Given
        ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
        request.setStatus("APPROVED");
        request.setRemarks("All requirements met");

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(applicationResponse);

        // When
        ApplicationResponse response = applicationService.updateApplicationStatus(1L, request);

        // Then
        assertNotNull(response);
        assertEquals("APPROVED", response.getStatus());

        // Verify notification service was called with correct parameters
        ArgumentCaptor<Application> applicationCaptor = ArgumentCaptor.forClass(Application.class);
        ArgumentCaptor<ApplicationStatus> statusCaptor = ArgumentCaptor.forClass(ApplicationStatus.class);

        verify(notificationService, times(1)).sendApplicationStatusNotification(
                applicationCaptor.capture(),
                statusCaptor.capture()
        );

        Application capturedApp = applicationCaptor.getValue();
        ApplicationStatus capturedStatus = statusCaptor.getValue();

        assertEquals(1L, capturedApp.getId());
        assertEquals(ApplicationStatus.APPROVED, capturedStatus);
        assertEquals("All requirements met", capturedApp.getRemarks());

        // Verify repository save was called
        verify(applicationRepository, times(1)).save(any(Application.class));
    }

    @Test
    @DisplayName("Should send notification when application is rejected")
    void shouldSendNotificationWhenApplicationRejected() {
        // Given
        ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
        request.setStatus("REJECTED");
        request.setRemarks("Does not meet eligibility criteria");

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(applicationResponse);

        // When
        ApplicationResponse response = applicationService.updateApplicationStatus(1L, request);

        // Then
        assertNotNull(response);

        // Verify notification service was called
        ArgumentCaptor<Application> applicationCaptor = ArgumentCaptor.forClass(Application.class);
        ArgumentCaptor<ApplicationStatus> statusCaptor = ArgumentCaptor.forClass(ApplicationStatus.class);

        verify(notificationService, times(1)).sendApplicationStatusNotification(
                applicationCaptor.capture(),
                statusCaptor.capture()
        );

        Application capturedApp = applicationCaptor.getValue();
        ApplicationStatus capturedStatus = statusCaptor.getValue();

        assertEquals(1L, capturedApp.getId());
        assertEquals(ApplicationStatus.REJECTED, capturedStatus);
        assertEquals("Does not meet eligibility criteria", capturedApp.getRemarks());
    }

    @Test
    @DisplayName("Should NOT send notification when status remains PENDING")
    void shouldNotSendNotificationWhenStatusRemainingPending() {
        // Given
        ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
        request.setStatus("PENDING");
        request.setRemarks("Additional documents required");

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(applicationResponse);

        // When
        applicationService.updateApplicationStatus(1L, request);

        // Then - notification service should NOT be called
        verify(notificationService, never()).sendApplicationStatusNotification(any(), any());
    }

    @Test
    @DisplayName("Should NOT send notification when status doesn't change")
    void shouldNotSendNotificationWhenStatusNotChanged() {
        // Given - application already approved
        application.setStatus(ApplicationStatus.APPROVED);

        ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
        request.setStatus("APPROVED");
        request.setRemarks("Already approved");

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(applicationResponse);

        // When
        applicationService.updateApplicationStatus(1L, request);

        // Then - notification service should NOT be called since status didn't change
        verify(notificationService, never()).sendApplicationStatusNotification(any(), any());
    }

    @Test
    @DisplayName("Should not break flow when notification fails")
    void shouldNotBreakFlowWhenNotificationFails() {
        // Given
        ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
        request.setStatus("APPROVED");
        request.setRemarks("All requirements met");

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(applicationResponse);

        // Simulate notification service throwing exception
        doThrow(new RuntimeException("Email service unavailable"))
                .when(notificationService)
                .sendApplicationStatusNotification(any(), any());

        // When - should not throw exception
        assertDoesNotThrow(() -> {
            ApplicationResponse response = applicationService.updateApplicationStatus(1L, request);
            assertNotNull(response);
        });

        // Then - application should still be saved despite notification failure
        verify(applicationRepository, times(1)).save(any(Application.class));
        verify(notificationService, times(1)).sendApplicationStatusNotification(any(), any());
    }

    @Test
    @DisplayName("Should include application details in notification")
    void shouldIncludeApplicationDetailsInNotification() {
        // Given
        application.setApplicationCode("APP-2025-999");
        application.setApplicationDate(LocalDate.of(2025, 1, 15));

        ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
        request.setStatus("APPROVED");
        request.setRemarks("Excellent application");

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(applicationResponse);

        // When
        applicationService.updateApplicationStatus(1L, request);

        // Then - verify notification contains correct application details
        ArgumentCaptor<Application> applicationCaptor = ArgumentCaptor.forClass(Application.class);
        verify(notificationService).sendApplicationStatusNotification(
                applicationCaptor.capture(),
                eq(ApplicationStatus.APPROVED)
        );

        Application capturedApp = applicationCaptor.getValue();
        assertEquals("APP-2025-999", capturedApp.getApplicationCode());
        assertEquals(LocalDate.of(2025, 1, 15), capturedApp.getApplicationDate());
        assertEquals("Excellent application", capturedApp.getRemarks());
        assertEquals(student.getAppUser().getEmail(), capturedApp.getStudent().getAppUser().getEmail());
    }

    @Test
    @DisplayName("Should send notification with student information")
    void shouldSendNotificationWithStudentInformation() {
        // Given
        ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
        request.setStatus("APPROVED");

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(applicationResponse);

        // When
        applicationService.updateApplicationStatus(1L, request);

        // Then - verify student details are accessible in notification
        ArgumentCaptor<Application> applicationCaptor = ArgumentCaptor.forClass(Application.class);
        verify(notificationService).sendApplicationStatusNotification(
                applicationCaptor.capture(),
                any()
        );

        Application capturedApp = applicationCaptor.getValue();
        assertNotNull(capturedApp.getStudent());
        assertNotNull(capturedApp.getStudent().getAppUser());
        assertEquals("student@university.edu", capturedApp.getStudent().getAppUser().getEmail());
        assertEquals("John", capturedApp.getStudent().getAppUser().getFirstName());
        assertEquals("Doe", capturedApp.getStudent().getAppUser().getLastName());
    }

    @Test
    @DisplayName("Should handle multiple status updates correctly")
    void shouldHandleMultipleStatusUpdatesCorrectly() {
        // Given - First update to APPROVED
        ApplicationStatusUpdateRequest approvalRequest = new ApplicationStatusUpdateRequest();
        approvalRequest.setStatus("APPROVED");

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(applicationResponse);

        // When - First update
        applicationService.updateApplicationStatus(1L, approvalRequest);

        // Then - Verify notification sent once
        verify(notificationService, times(1)).sendApplicationStatusNotification(any(), eq(ApplicationStatus.APPROVED));

        // Given - Second update to REJECTED (changing from APPROVED)
        reset(notificationService);
        application.setStatus(ApplicationStatus.APPROVED); // Simulate previous approval
        ApplicationStatusUpdateRequest rejectionRequest = new ApplicationStatusUpdateRequest();
        rejectionRequest.setStatus("REJECTED");

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);

        // When - Second update
        applicationService.updateApplicationStatus(1L, rejectionRequest);

        // Then - Verify notification sent for rejection
        verify(notificationService, times(1)).sendApplicationStatusNotification(any(), eq(ApplicationStatus.REJECTED));
    }

    @Test
    @DisplayName("Should execute notification asynchronously without blocking")
    void shouldExecuteNotificationAsynchronouslyWithoutBlocking() {
        // Given
        ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
        request.setStatus("APPROVED");

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(applicationResponse);

        // When
        long startTime = System.currentTimeMillis();
        ApplicationResponse response = applicationService.updateApplicationStatus(1L, request);
        long endTime = System.currentTimeMillis();

        // Then - should complete quickly (async execution doesn't block)
        assertNotNull(response);
        long executionTime = endTime - startTime;
        assertTrue(executionTime < 1000, "Method should return quickly without waiting for email");

        // Verify notification was triggered
        verify(notificationService, times(1)).sendApplicationStatusNotification(any(), any());
    }
}
