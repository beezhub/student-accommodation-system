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
import com.beezhub.student_accommodation.service.AcademicYearService;
import com.beezhub.student_accommodation.service.RoomAllocationService;
import com.beezhub.student_accommodation.service.AllocationScoringService;
import com.beezhub.student_accommodation.service.SystemSettingService;
import com.beezhub.student_accommodation.service.application.ApplicationAdminService;
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
        @Mock
        private AcademicYearService academicYearService;
        @Mock
        private RoomAllocationService roomAllocationService;
        @Mock
        private AllocationScoringService allocationScoringService;
        @Mock
        private SystemSettingService systemSettingService;
        @Mock
        private ApplicationAdminService applicationAdminService;

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
                applicationRequest.setRequiresAccessibility(false);
                applicationRequest.setCourseName("Computer Science");
        }

        @Test
        void submitApplication_shouldCreateApplication() {
                // Arrange
                com.beezhub.student_accommodation.model.entity.AcademicYear academicYear = com.beezhub.student_accommodation.model.entity.AcademicYear
                                .builder()
                                .id(1L)
                                .name("2025/2026")
                                .startDate(LocalDate.now().minusMonths(2))
                                .endDate(LocalDate.now().plusMonths(10))
                                .applicationOpenDate(LocalDate.now().minusDays(10))
                                .applicationCloseDate(LocalDate.now().plusDays(30))
                                .isCurrent(true)
                                .build();

                when(studentRepository.findStudentWithDetailsById(1L)).thenReturn(Optional.of(student));
                when(authService.getCurrentUser()).thenReturn(studentUser);
                when(applicationRepository.existsByStudent_IdAndStatus(1L, ApplicationStatus.PENDING))
                                .thenReturn(false);
                when(academicYearService.getCurrentAcademicYearEntity()).thenReturn(academicYear);
                when(academicYearService.isApplicationWindowOpen(academicYear)).thenReturn(true);
                lenient().when(applicationRepository.countByApplicationCodeStartingWith(anyString())).thenReturn(0L);
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
                when(studentRepository.findStudentWithDetailsById(1L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> applicationService.submitApplication(applicationRequest))
                                .isInstanceOf(StudentNotFoundException.class);
        }

        @Test
        void submitApplication_shouldThrowException_whenDuplicateApplication() {
                when(studentRepository.findStudentWithDetailsById(1L)).thenReturn(Optional.of(student));
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
        void updateApplicationStatus_shouldDelegateToAdminService() {
                ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
                request.setStatus("APPROVED");

                when(applicationAdminService.updateApplicationStatus(1L, request))
                                .thenReturn(new ApplicationResponse());

                ApplicationResponse response = applicationService.updateApplicationStatus(1L, request);

                assertThat(response).isNotNull();
                verify(applicationAdminService).updateApplicationStatus(1L, request);
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
        void moveToAllocationPhase_shouldDelegateToAdminService() {
                ApplicationResponse expectedResponse = new ApplicationResponse();
                when(applicationAdminService.moveToAllocationPhase(1L, 2L)).thenReturn(expectedResponse);

                ApplicationResponse response = applicationService.moveToAllocationPhase(1L, 2L);

                assertThat(response).isEqualTo(expectedResponse);
                verify(applicationAdminService).moveToAllocationPhase(1L, 2L);
        }

        @Test
        void getRequiredDocumentsChecklist_shouldDelegateToAdminService() {
                RequiredDocumentsChecklistResponse expectedResponse = new RequiredDocumentsChecklistResponse();
                when(applicationAdminService.getRequiredDocumentsChecklist(1L)).thenReturn(expectedResponse);

                RequiredDocumentsChecklistResponse response = applicationService.getRequiredDocumentsChecklist(1L);

                assertThat(response).isEqualTo(expectedResponse);
                verify(applicationAdminService).getRequiredDocumentsChecklist(1L);
        }

        // ==================== Auto-Allocation Tests ====================

        @Test
        void autoAllocation_featureFlagEnabled_successfulAllocation() {
                // Setup
                application.setStatus(ApplicationStatus.APPROVED);

                DocumentType docType = new DocumentType();
                docType.setId(1L);
                docType.setTypeName("ID");
                docType.setIsRequired(true);

                Document doc = new Document();
                doc.setDocumentType(docType);
                doc.setDocumentStatus(DocumentStatus.APPROVED);
                doc.setApplication(application);

                ScoredRoom scoredRoom = ScoredRoom.builder()
                                .roomId(1L)
                                .roomNumber("101")
                                .totalScore(new java.math.BigDecimal("100"))
                                .buildingName("Building A")
                                .build();

                // Mocking
                // Mocking
                when(systemSettingService.isAutoAllocationEnabled()).thenReturn(true);
                when(applicationAdminService.areAllRequiredDocumentsApproved(1L)).thenReturn(true);
                when(allocationScoringService.scoreAvailableRooms(any(), any(), any()))
                                .thenReturn(Collections.singletonList(scoredRoom));
                when(roomAllocationService.createRoomAllocation(any())).thenReturn(new RoomAllocationResponse());
                when(applicationRepository.save(any())).thenReturn(application);

                // Execute
                applicationService.attemptAutoAllocation(application);

                // Verify
                verify(systemSettingService).isAutoAllocationEnabled();
                verify(allocationScoringService).scoreAvailableRooms(any(), any(), any());
                verify(roomAllocationService).createRoomAllocation(any(RoomAllocationRequest.class));
                verify(applicationRepository, atLeastOnce()).save(any());
        }

        @Test
        void autoAllocation_featureFlagDisabled_skipsAllocation() {
                // Setup
                application.setStatus(ApplicationStatus.APPROVED);

                // Mocking
                // Mocking
                when(systemSettingService.isAutoAllocationEnabled()).thenReturn(false);

                // Execute
                applicationService.attemptAutoAllocation(application);

                // Verify
                verify(systemSettingService).isAutoAllocationEnabled();
                verify(allocationScoringService, never()).scoreAvailableRooms(any(), any(), any());
                verify(roomAllocationService, never()).createRoomAllocation(any());
        }

        @Test
        void autoAllocation_noRoomsAvailable_movesToAwaitingAllocation() {
                // Setup
                application.setStatus(ApplicationStatus.APPROVED);

                DocumentType docType = new DocumentType();
                docType.setId(1L);
                docType.setTypeName("ID");
                docType.setIsRequired(true);

                Document doc = new Document();
                doc.setDocumentType(docType);
                doc.setDocumentStatus(DocumentStatus.APPROVED);
                doc.setApplication(application);

                // Mocking - empty list means no rooms available
                // Mocking - empty list means no rooms available
                when(systemSettingService.isAutoAllocationEnabled()).thenReturn(true);
                when(applicationAdminService.areAllRequiredDocumentsApproved(1L)).thenReturn(true);
                when(allocationScoringService.scoreAvailableRooms(any(), any(), any()))
                                .thenReturn(Collections.emptyList());
                when(applicationRepository.save(any())).thenReturn(application);
                when(roomAllocationService.enqueueWaitingList(anyLong(), anyLong()))
                                .thenReturn(new MessageResponse("Added to waiting list"));

                // Execute
                applicationService.attemptAutoAllocation(application);

                // Verify
                verify(allocationScoringService).scoreAvailableRooms(any(), any(), any());
                verify(roomAllocationService).enqueueWaitingList(anyLong(), anyLong());
                verify(userNotificationService).createWaitingListNotification(anyLong(), anyLong());
                verify(roomAllocationService, never()).createRoomAllocation(any());
        }

        @Test
        void autoAllocation_documentsNotApproved_movesToAwaitingAllocation() {
                // Setup
                application.setStatus(ApplicationStatus.APPROVED);

                DocumentType docType = new DocumentType();
                docType.setId(1L);
                docType.setTypeName("ID");
                docType.setIsRequired(true);

                Document doc = new Document();
                doc.setDocumentType(docType);
                doc.setDocumentStatus(DocumentStatus.PENDING); // Not approved
                doc.setApplication(application);

                // Mocking
                // Mocking
                when(systemSettingService.isAutoAllocationEnabled()).thenReturn(true);
                when(applicationAdminService.areAllRequiredDocumentsApproved(1L)).thenReturn(false);
                when(applicationRepository.save(any())).thenReturn(application);

                // Execute
                applicationService.attemptAutoAllocation(application);

                // Verify - should not attempt allocation when docs not approved
                verify(allocationScoringService, never()).scoreAvailableRooms(any(), any(), any());
                verify(roomAllocationService, never()).createRoomAllocation(any());
        }

        @Test
        void autoAllocation_roomCapacityExceeded_gracefulFallback() {
                // Setup
                application.setStatus(ApplicationStatus.APPROVED);

                DocumentType docType = new DocumentType();
                docType.setId(1L);
                docType.setTypeName("ID");
                docType.setIsRequired(true);

                Document doc = new Document();
                doc.setDocumentType(docType);
                doc.setDocumentStatus(DocumentStatus.APPROVED);
                doc.setApplication(application);

                ScoredRoom scoredRoom = ScoredRoom.builder()
                                .roomId(1L)
                                .roomNumber("101")
                                .totalScore(new java.math.BigDecimal("100"))
                                .buildingName("Building A")
                                .build();

                // Mocking
                // Mocking
                when(systemSettingService.isAutoAllocationEnabled()).thenReturn(true);
                when(applicationAdminService.areAllRequiredDocumentsApproved(1L)).thenReturn(true);
                when(allocationScoringService.scoreAvailableRooms(any(), any(), any()))
                                .thenReturn(Collections.singletonList(scoredRoom));
                when(roomAllocationService.createRoomAllocation(any()))
                                .thenThrow(new RoomCapacityExceededException("Room is full"));
                when(applicationRepository.save(any())).thenReturn(application);
                when(roomAllocationService.enqueueWaitingList(anyLong(), anyLong()))
                                .thenReturn(new MessageResponse("Added to waiting list"));

                // Execute
                applicationService.attemptAutoAllocation(application);

                // Verify - should catch exception gracefully and add to waiting list
                verify(roomAllocationService).createRoomAllocation(any());
                verify(roomAllocationService).enqueueWaitingList(anyLong(), anyLong());
                verify(userNotificationService).createWaitingListNotification(anyLong(), anyLong());
        }

        @Test
        void autoAllocation_unexpectedError_gracefulFallback() {
                // Setup
                application.setStatus(ApplicationStatus.APPROVED);

                DocumentType docType = new DocumentType();
                docType.setId(1L);
                docType.setTypeName("ID");
                docType.setIsRequired(true);

                Document doc = new Document();
                doc.setDocumentType(docType);
                doc.setDocumentStatus(DocumentStatus.APPROVED);
                doc.setApplication(application);

                ScoredRoom scoredRoom = ScoredRoom.builder()
                                .roomId(1L)
                                .roomNumber("101")
                                .totalScore(new java.math.BigDecimal("100"))
                                .buildingName("Building A")
                                .build();

                // Mocking
                // Mocking
                when(systemSettingService.isAutoAllocationEnabled()).thenReturn(true);
                when(applicationAdminService.areAllRequiredDocumentsApproved(1L)).thenReturn(true);
                when(allocationScoringService.scoreAvailableRooms(any(), any(), any()))
                                .thenReturn(Collections.singletonList(scoredRoom));
                when(roomAllocationService.createRoomAllocation(any()))
                                .thenThrow(new RuntimeException("Unexpected error"));
                when(applicationRepository.save(any())).thenReturn(application);

                // Execute - should not throw exception
                applicationService.attemptAutoAllocation(application);

                // Verify - exception was caught gracefully
                verify(roomAllocationService).createRoomAllocation(any());
                verify(applicationRepository, atLeastOnce()).save(any());
        }
}
