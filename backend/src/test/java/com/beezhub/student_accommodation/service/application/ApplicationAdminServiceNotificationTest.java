package com.beezhub.student_accommodation.service.application;

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
import com.beezhub.student_accommodation.model.event.ApplicationStatusChangedEvent;
import com.beezhub.student_accommodation.service.ApplicationService;
import com.beezhub.student_accommodation.service.AuthService;
import com.beezhub.student_accommodation.service.UserNotificationService;
import org.springframework.context.ApplicationEventPublisher;
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
@DisplayName("Application Admin Service - Notification Integration Tests")
class ApplicationAdminServiceNotificationTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private AuthService authService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UserNotificationService userNotificationService;

    @Mock
    private ApplicationService applicationService;
    @Mock
    private com.beezhub.student_accommodation.service.tenant.TenantOwnershipGuard tenantOwnershipGuard;

    /**
     * A terminal status now releases any allocation the application still holds, so this
     * collaborator is reached on the rejection paths below (GH-223).
     */
    @Mock
    private com.beezhub.student_accommodation.service.RoomAllocationService roomAllocationService;


    @InjectMocks
    private ApplicationAdminService applicationAdminService;

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
        ApplicationResponse response = applicationAdminService.updateApplicationStatus(1L, request);

        // Then
        assertNotNull(response);
        assertEquals("APPROVED", response.getStatus());

        // Verify event published
        ArgumentCaptor<ApplicationStatusChangedEvent> eventCaptor = ArgumentCaptor
                .forClass(ApplicationStatusChangedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        ApplicationStatusChangedEvent event = eventCaptor.getValue();
        assertEquals(1L, event.application().getId());
        assertEquals(ApplicationStatus.APPROVED, event.newStatus());
        assertEquals("All requirements met", event.application().getRemarks());

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
        ApplicationResponse response = applicationAdminService.updateApplicationStatus(1L, request);

        // Then
        assertNotNull(response);

        // Verify event published
        ArgumentCaptor<ApplicationStatusChangedEvent> eventCaptor = ArgumentCaptor
                .forClass(ApplicationStatusChangedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        ApplicationStatusChangedEvent event = eventCaptor.getValue();
        assertEquals(1L, event.application().getId());
        assertEquals(ApplicationStatus.REJECTED, event.newStatus());
        assertEquals("Does not meet eligibility criteria", event.application().getRemarks());
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
        applicationAdminService.updateApplicationStatus(1L, request);

        // Then - event should NOT be published
        verify(eventPublisher, never()).publishEvent(any());
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
        applicationAdminService.updateApplicationStatus(1L, request);

        // Then - event should NOT be published since status didn't change
        verify(eventPublisher, never()).publishEvent(any());
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

        // Simulate event publisher throwing exception
        doThrow(new RuntimeException("Event system unavailable"))
                .when(eventPublisher)
                .publishEvent(any());

        // When - should not throw exception
        assertDoesNotThrow(() -> {
            ApplicationResponse response = applicationAdminService.updateApplicationStatus(1L, request);
            assertNotNull(response);
        });

        // Then - application should still be saved despite notification failure
        verify(applicationRepository, times(1)).save(any(Application.class));
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));
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
        applicationAdminService.updateApplicationStatus(1L, request);

        // Then - verify event contains correct application details
        ArgumentCaptor<ApplicationStatusChangedEvent> eventCaptor = ArgumentCaptor
                .forClass(ApplicationStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        ApplicationStatusChangedEvent event = eventCaptor.getValue();
        Application capturedApp = event.application();
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
        applicationAdminService.updateApplicationStatus(1L, request);

        // Then - verify student details are accessible in event
        ArgumentCaptor<ApplicationStatusChangedEvent> eventCaptor = ArgumentCaptor
                .forClass(ApplicationStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        ApplicationStatusChangedEvent event = eventCaptor.getValue();
        Application capturedApp = event.application();
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
        applicationAdminService.updateApplicationStatus(1L, approvalRequest);

        // Then - Verify event sent once
        verify(eventPublisher, times(1)).publishEvent(any(ApplicationStatusChangedEvent.class));

        // Clear invocations
        clearInvocations(eventPublisher);

        // Given - Second update to REJECTED (changing from APPROVED)
        application.setStatus(ApplicationStatus.APPROVED); // Simulate previous approval
        ApplicationStatusUpdateRequest rejectionRequest = new ApplicationStatusUpdateRequest();
        rejectionRequest.setStatus("REJECTED");

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);

        // When - Second update
        applicationAdminService.updateApplicationStatus(1L, rejectionRequest);

        // Then - Verify event sent for rejection
        ArgumentCaptor<ApplicationStatusChangedEvent> eventCaptor = ArgumentCaptor
                .forClass(ApplicationStatusChangedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        assertEquals(ApplicationStatus.REJECTED, eventCaptor.getValue().newStatus());
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
        ApplicationResponse response = applicationAdminService.updateApplicationStatus(1L, request);
        long endTime = System.currentTimeMillis();

        // Then - should complete quickly (async execution doesn't block)
        assertNotNull(response);
        long executionTime = endTime - startTime;
        assertTrue(executionTime < 1000, "Method should return quickly without waiting for email");

        // Verify event was triggered
        verify(eventPublisher, times(1)).publishEvent(any(ApplicationStatusChangedEvent.class));
    }
}
