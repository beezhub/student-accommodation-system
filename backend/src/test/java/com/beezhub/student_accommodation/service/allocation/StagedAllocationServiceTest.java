package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.exceptions.*;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.mapper.StagedAllocationMapper;
import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.*;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.LegacyScoringStrategy;
import com.beezhub.student_accommodation.service.UserNotificationService;
import com.beezhub.student_accommodation.model.event.AllocationCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StagedAllocationServiceTest {

        @Mock
        private StagedAllocationRepository stagedAllocationRepository;
        @Mock
        private RoomAllocationRepository roomAllocationRepository;
        @Mock
        private StudentRepository studentRepository;
        @Mock
        private RoomRepository roomRepository;
        @Mock
        private ApplicationRepository applicationRepository;
        @Mock
        private AppUserRepository appUserRepository;
        @Mock
        private BuildingRepository buildingRepository;
        @Mock
        private LegacyScoringStrategy allocationScoringService;
        @Mock
        private AllocationAuditService allocationAuditService;

        @Mock
        private UserNotificationService userNotificationService;
        @Mock
        private ApplicationEventPublisher eventPublisher;
        @Mock
        private StagedAllocationMapper stagedAllocationMapper;
        @Mock
        private RoomAllocationMapper roomAllocationMapper;
        @Mock
        private WaitingListService waitingListService;
        @Mock
        private RoomOccupancyManager roomOccupancyManager;
    @Mock
    private com.beezhub.student_accommodation.service.tenant.TenantOwnershipGuard tenantOwnershipGuard;


        @InjectMocks
        private StagedAllocationService stagedAllocationService;

        private Student student;
        private Room room;
        private Building building;
        private Application application;
        private AppUser admin;
        private StagedAllocation stagedAllocation;

        @BeforeEach
        void setUp() {
                // Setup student
                student = new Student();
                student.setId(1L);
                AppUser studentUser = new AppUser();
                studentUser.setId(1L);
                studentUser.setFirstName("John");
                studentUser.setLastName("Doe");
                studentUser.setEmail("john.doe@example.com");
                student.setAppUser(studentUser);

                // Setup building
                building = new Building();
                building.setId(1L);
                building.setName("Test Building");

                // Setup room
                room = new Room();
                room.setId(1L);
                room.setRoomNumber("101");
                room.setCapacity(2);
                room.setBuilding(building);

                // Setup application
                application = new Application();
                application.setId(1L);
                application.setStudent(student);
                application.setStatus(ApplicationStatus.AWAITING_ALLOCATION);
                application.setPreferredBuilding(building);

                // Setup admin user
                admin = new AppUser();
                admin.setId(2L);
                admin.setFirstName("Admin");
                admin.setLastName("User");

                // Setup staged allocation
                stagedAllocation = StagedAllocation.builder()
                                .id(1L)
                                .student(student)
                                .room(room)
                                .application(application)
                                .status(StagedAllocationStatus.PROPOSED)
                                .score(BigDecimal.valueOf(85.5))
                                .proposedBy(admin)
                                .batchId("BATCH-001")
                                .build();
        }

        @Test
        void confirmAllocation_validProposal_createsAllocation() {
                // Arrange
                when(stagedAllocationRepository.findById(1L)).thenReturn(Optional.of(stagedAllocation));
                when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
                when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(i -> i.getArguments()[0]);
                when(roomAllocationMapper.toResponse(any(RoomAllocation.class)))
                                .thenReturn(new RoomAllocationResponse());
                when(appUserRepository.findById(2L)).thenReturn(Optional.of(admin));

                // Act
                RoomAllocationResponse response = stagedAllocationService.confirmAllocation(1L, 2L);

                // Assert
                assertThat(response).isNotNull();
                assertThat(stagedAllocation.getStatus()).isEqualTo(StagedAllocationStatus.CONFIRMED);
                assertThat(stagedAllocation.getConfirmedAt()).isNotNull();
                verify(roomAllocationRepository).save(any(RoomAllocation.class));
                verify(stagedAllocationRepository).save(stagedAllocation);
                verify(roomOccupancyManager).updateOccupancy(room, 1);
                verify(allocationAuditService).logAuditActionInCurrentTransaction(any(),
                                eq(AllocationAuditActionType.FORCE_ALLOCATE),
                                eq(admin),
                                anyString(), isNull(), eq(room));
                verify(eventPublisher).publishEvent(any(AllocationCreatedEvent.class));
        }

        /**
         * GH-146: only one of the five construction sites for tenant-owned allocation rows set
         * tenantId. The rest left it null, so TenantListener copied the acting context, which is 0
         * for a platform admin. The owning tenant could not then see the row through any filtered
         * query: the student stayed WAITING after confirmation, the already-allocated guard did
         * not fire, and a tenant admin was refused ownership of their own proposal.
         *
         * <p>From the room rather than the staged row, so a legacy staged row that is itself
         * mis-stamped still produces a correctly stamped allocation.
         */
        @Test
        void confirmAllocation_stampsTheAllocationWithTheRoomTenant() {
                room.setTenantId(7L);
                stagedAllocation.setTenantId(0L);
                when(stagedAllocationRepository.findById(1L)).thenReturn(Optional.of(stagedAllocation));
                when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
                when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(i -> i.getArguments()[0]);
                when(roomAllocationMapper.toResponse(any(RoomAllocation.class)))
                                .thenReturn(new RoomAllocationResponse());
                when(appUserRepository.findById(2L)).thenReturn(Optional.of(admin));

                stagedAllocationService.confirmAllocation(1L, 2L);

                ArgumentCaptor<RoomAllocation> saved = ArgumentCaptor.forClass(RoomAllocation.class);
                verify(roomAllocationRepository).save(saved.capture());
                assertThat(saved.getValue().getTenantId()).isEqualTo(7L);
        }

        @Test
        void confirmAllocation_invalidStatus_throwsException() {
                // Arrange
                stagedAllocation.setStatus(StagedAllocationStatus.CONFIRMED);
                when(stagedAllocationRepository.findById(1L)).thenReturn(Optional.of(stagedAllocation));

                // Act & Assert
                assertThatThrownBy(() -> stagedAllocationService.confirmAllocation(1L, 2L))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("Invalid status");
        }

        @Test
        void confirmAllocation_studentAlreadyAllocated_rejectedWithoutCreatingASecondOffer() {
                // GH-128: proposals go stale. The batch job or a manual allocation may have offered
                // this student a room since the proposal was made.
                when(stagedAllocationRepository.findById(1L)).thenReturn(Optional.of(stagedAllocation));
                when(roomAllocationRepository.existsActiveAllocationByStudentId(student.getId())).thenReturn(true);

                assertThatThrownBy(() -> stagedAllocationService.confirmAllocation(1L, 2L))
                                .isInstanceOf(StudentAlreadyAllocatedException.class);

                verify(roomAllocationRepository, never()).save(any(RoomAllocation.class));
                verify(roomOccupancyManager, never()).updateOccupancy(any(), anyInt());
                assertThat(stagedAllocation.getStatus()).isEqualTo(StagedAllocationStatus.PROPOSED);
        }

        @Test
        void confirmAllocation_marksTheStudentAllocatedOnTheWaitingList() {
                // GH-128: the manual path does this inside createRoomAllocation; the staged path
                // skipped it, leaving the student WAITING and eligible for a second offer.
                when(stagedAllocationRepository.findById(1L)).thenReturn(Optional.of(stagedAllocation));
                when(roomAllocationRepository.existsActiveAllocationByStudentId(student.getId())).thenReturn(false);
                when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
                when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(i -> i.getArguments()[0]);
                when(roomAllocationMapper.toResponse(any(RoomAllocation.class)))
                                .thenReturn(new RoomAllocationResponse());
                when(appUserRepository.findById(2L)).thenReturn(Optional.of(admin));

                stagedAllocationService.confirmAllocation(1L, 2L);

                verify(waitingListService).updateStatusToAllocated(student.getId());
        }

        @Test
        void confirmAllocation_roomFull_throwsException() {
                // Arrange
                when(stagedAllocationRepository.findById(1L)).thenReturn(Optional.of(stagedAllocation));
                when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(2L); // Room at capacity

                // Act & Assert
                assertThatThrownBy(() -> stagedAllocationService.confirmAllocation(1L, 2L))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("full capacity");
        }

        @Test
        void rejectAllocation_validProposal_updatesStatus() {
                // Arrange
                when(stagedAllocationRepository.findById(1L)).thenReturn(Optional.of(stagedAllocation));

                // Act
                stagedAllocationService.rejectAllocation(1L, "Not suitable for student", 2L);

                // Assert
                assertThat(stagedAllocation.getStatus()).isEqualTo(StagedAllocationStatus.REJECTED);
                verify(stagedAllocationRepository).save(stagedAllocation);
                verify(userNotificationService).createAllocationRejectionNotification(
                                eq(1L), eq("101"), eq("Not suitable for student"));
        }

        @Test
        void rejectAllocation_invalidStatus_throwsException() {
                // Arrange
                stagedAllocation.setStatus(StagedAllocationStatus.CONFIRMED);
                when(stagedAllocationRepository.findById(1L)).thenReturn(Optional.of(stagedAllocation));

                // Act & Assert
                assertThatThrownBy(() -> stagedAllocationService.rejectAllocation(1L, "Reason", 2L))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("Invalid status");
        }

        @Test
        void overrideConstraints_adminOverride_bypasses() {
                // Arrange
                List<String> constraintsBypassed = Arrays.asList("GENDER_RESTRICTION", "YEAR_OF_STUDY");
                when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
                when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
                when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
                when(applicationRepository.findByStudent_Id(1L)).thenReturn(Arrays.asList(application));
                when(appUserRepository.findById(2L)).thenReturn(Optional.of(admin));

                ScoredRoom scoredRoom = ScoredRoom.builder()
                                .roomId(1L)
                                .roomNumber("101")
                                .totalScore(BigDecimal.valueOf(50.0))
                                .warnings(new ArrayList<>())
                                .build();
                when(allocationScoringService.scoreSingleRoom(student, application, room)).thenReturn(scoredRoom);
                when(stagedAllocationRepository.save(any(StagedAllocation.class))).thenAnswer(i -> i.getArguments()[0]);
                when(stagedAllocationMapper.toResponse(any(StagedAllocation.class)))
                                .thenReturn(new StagedAllocationResponse());
                when(stagedAllocationMapper.serializeWarnings(anyList())).thenReturn("{\"warnings\":[]}");

                // Act
                StagedAllocationResponse response = stagedAllocationService.overrideConstraints(
                                1L, 1L, "Special accommodation needed", 2L, constraintsBypassed);

                // Assert
                assertThat(response).isNotNull();
                verify(stagedAllocationRepository).save(any(StagedAllocation.class));
                verify(allocationAuditService).logAuditActionWithConstraint(
                                isNull(),
                                eq(AllocationAuditActionType.OVERRIDE),
                                eq(admin),
                                eq("Special accommodation needed"),
                                isNull(),
                                eq(room),
                                eq("GENDER_RESTRICTION, YEAR_OF_STUDY"));
        }

        @Test
        void overrideConstraints_roomFull_throwsException() {
                // Arrange
                when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
                when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
                when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(2L); // Room full

                // Act & Assert
                assertThatThrownBy(() -> stagedAllocationService.overrideConstraints(
                                1L, 1L, "Reason", 2L, Arrays.asList("CONSTRAINT")))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("full capacity");
        }

        @Test
        void proposeAllocation_multipleStudents_createsProposals() {
                // Arrange
                BatchAllocationRequest request = new BatchAllocationRequest();
                request.setBatchId("BATCH-002");
                request.setStudentIds(Arrays.asList(1L, 2L));
                request.setPreferredBuildingId(1L);

                Student student2 = new Student();
                student2.setId(2L);
                AppUser user2 = new AppUser();
                user2.setId(3L);
                user2.setFirstName("Jane");
                user2.setLastName("Smith");
                student2.setAppUser(user2);

                Application application2 = new Application();
                application2.setId(2L);
                application2.setStudent(student2);
                application2.setStatus(ApplicationStatus.AWAITING_ALLOCATION);
                application2.setPreferredBuilding(building);

                ScoredRoom scoredRoom = ScoredRoom.builder()
                                .roomId(1L)
                                .roomNumber("101")
                                .totalScore(BigDecimal.valueOf(85.5))
                                .warnings(new ArrayList<>())
                                .build();

                when(appUserRepository.findById(2L)).thenReturn(Optional.of(admin));
                when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
                when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
                when(studentRepository.findById(2L)).thenReturn(Optional.of(student2));
                when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
                when(roomAllocationRepository.existsActiveAllocationByStudentId(anyLong())).thenReturn(false);
                when(stagedAllocationRepository.findByStudent_IdAndStatus(anyLong(),
                                eq(StagedAllocationStatus.PROPOSED)))
                                .thenReturn(Optional.empty());
                when(applicationRepository.findByStudent_Id(1L)).thenReturn(Arrays.asList(application));
                when(applicationRepository.findByStudent_Id(2L)).thenReturn(Arrays.asList(application2));
                when(allocationScoringService.scoreAvailableRooms(any(), any(), any()))
                                .thenReturn(Arrays.asList(scoredRoom));
                when(stagedAllocationRepository.save(any(StagedAllocation.class))).thenAnswer(i -> i.getArguments()[0]);
                when(stagedAllocationMapper.toResponse(any(StagedAllocation.class)))
                                .thenReturn(StagedAllocationResponse.builder().roomId(1L).build());
                when(stagedAllocationMapper.serializeWarnings(anyList())).thenReturn("[]");

                // Act
                BatchAllocationProposalResponse response = stagedAllocationService.proposeAllocation(request, 2L);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getBatchId()).isEqualTo("BATCH-002");
                assertThat(response.getTotalStudents()).isEqualTo(2);
                assertThat(response.getSuccessfulProposals()).isEqualTo(2);
                assertThat(response.getFailedProposals()).isEqualTo(0);
                verify(stagedAllocationRepository, times(2)).save(any(StagedAllocation.class));
        }

        @Test
        void proposeAllocation_noAvailableRooms_addsToWaitingList() {
                // Arrange
                BatchAllocationRequest request = new BatchAllocationRequest();
                request.setBatchId("BATCH-003");
                request.setStudentIds(Arrays.asList(1L));
                request.setPreferredBuildingId(1L);

                when(appUserRepository.findById(2L)).thenReturn(Optional.of(admin));
                when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
                when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
                when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(false);
                when(stagedAllocationRepository.findByStudent_IdAndStatus(1L, StagedAllocationStatus.PROPOSED))
                                .thenReturn(Optional.empty());
                when(applicationRepository.findByStudent_Id(1L)).thenReturn(Arrays.asList(application));
                when(allocationScoringService.scoreAvailableRooms(any(), any(), any()))
                                .thenReturn(new ArrayList<>()); // No rooms available

                // Act
                BatchAllocationProposalResponse response = stagedAllocationService.proposeAllocation(request, 2L);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getFailedProposals()).isEqualTo(1);
                verify(waitingListService).enqueueWaitingList(student, application);
        }

        @Test
        void confirmBatchAllocations_validBatch_confirmsAll() {
                // Arrange
                StagedAllocation staged1 = StagedAllocation.builder()
                                .id(1L)
                                .student(student)
                                .room(room)
                                .application(application)
                                .status(StagedAllocationStatus.PROPOSED)
                                .batchId("BATCH-001")
                                .build();

                StagedAllocation staged2 = StagedAllocation.builder()
                                .id(2L)
                                .student(student)
                                .room(room)
                                .application(application)
                                .status(StagedAllocationStatus.PROPOSED)
                                .batchId("BATCH-001")
                                .build();

                when(stagedAllocationRepository.findByBatchIdAndStatus("BATCH-001", StagedAllocationStatus.PROPOSED))
                                .thenReturn(Arrays.asList(staged1, staged2));
                when(stagedAllocationRepository.findById(anyLong()))
                                .thenReturn(Optional.of(staged1), Optional.of(staged2));
                when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
                when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(i -> i.getArguments()[0]);
                when(roomAllocationMapper.toResponse(any(RoomAllocation.class)))
                                .thenReturn(new RoomAllocationResponse());
                when(appUserRepository.findById(2L)).thenReturn(Optional.of(admin));

                // Act
                stagedAllocationService.confirmBatchAllocations("BATCH-001", 2L);

                // Assert
                verify(roomAllocationRepository, times(2)).save(any(RoomAllocation.class));
        }

        @Test
        void confirmBatchAllocations_partialFailure_continuesProcessing() {
                // Arrange
                StagedAllocation staged1 = StagedAllocation.builder()
                                .id(1L)
                                .student(student)
                                .room(room)
                                .application(application)
                                .status(StagedAllocationStatus.PROPOSED)
                                .batchId("BATCH-001")
                                .build();

                StagedAllocation staged2 = StagedAllocation.builder()
                                .id(2L)
                                .student(student)
                                .room(room)
                                .application(application)
                                .status(StagedAllocationStatus.PROPOSED)
                                .batchId("BATCH-001")
                                .build();

                when(stagedAllocationRepository.findByBatchIdAndStatus("BATCH-001", StagedAllocationStatus.PROPOSED))
                                .thenReturn(Arrays.asList(staged1, staged2));
                when(stagedAllocationRepository.findById(1L)).thenReturn(Optional.of(staged1));
                when(stagedAllocationRepository.findById(2L)).thenThrow(new RuntimeException("Allocation failed"));
                when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
                when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(i -> i.getArguments()[0]);
                when(roomAllocationMapper.toResponse(any(RoomAllocation.class)))
                                .thenReturn(new RoomAllocationResponse());
                when(appUserRepository.findById(2L)).thenReturn(Optional.of(admin));

                // Act
                stagedAllocationService.confirmBatchAllocations("BATCH-001", 2L);

        // Assert - should process first, fail on second, but continue
                verify(roomAllocationRepository, times(1)).save(any(RoomAllocation.class));
        }

        @Test
        void confirmBatchAllocationsWithResult_allSuccess_returnsSuccessResult() {
                // Arrange
                StagedAllocation staged1 = StagedAllocation.builder()
                                .id(1L).student(student).room(room).application(application)
                                .status(StagedAllocationStatus.PROPOSED).batchId("BATCH-001").build();
                StagedAllocation staged2 = StagedAllocation.builder()
                                .id(2L).student(student).room(room).application(application)
                                .status(StagedAllocationStatus.PROPOSED).batchId("BATCH-001").build();

                RoomAllocationResponse resp1 = new RoomAllocationResponse();
                resp1.setId(10L);
                RoomAllocationResponse resp2 = new RoomAllocationResponse();
                resp2.setId(11L);

                when(stagedAllocationRepository.findByBatchIdAndStatus("BATCH-001", StagedAllocationStatus.PROPOSED))
                                .thenReturn(Arrays.asList(staged1, staged2));
                when(stagedAllocationRepository.findById(1L)).thenReturn(Optional.of(staged1));
                when(stagedAllocationRepository.findById(2L)).thenReturn(Optional.of(staged2));
                when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
                when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(i -> i.getArguments()[0]);
                when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(resp1, resp2);
                when(appUserRepository.findById(2L)).thenReturn(Optional.of(admin));

                // Act
                BatchConfirmationResult result = stagedAllocationService.confirmBatchAllocationsWithResult("BATCH-001", 2L);

                // Assert
                assertThat(result.getSuccess()).isTrue();
                assertThat(result.getTotalAttempted()).isEqualTo(2);
                assertThat(result.getSuccessCount()).isEqualTo(2);
                assertThat(result.getFailureCount()).isEqualTo(0);
                assertThat(result.getErrors()).isNull();
                assertThat(result.getConfirmedAllocationIds()).containsExactly(10L, 11L);
                assertThat(result.getProcessedAt()).isNotNull();
        }

        @Test
        void confirmBatchAllocationsWithResult_partialFailure_returnsPartialResult() {
                // Arrange
                StagedAllocation staged1 = StagedAllocation.builder()
                                .id(1L).student(student).room(room).application(application)
                                .status(StagedAllocationStatus.PROPOSED).batchId("BATCH-001").build();
                StagedAllocation staged2 = StagedAllocation.builder()
                                .id(2L).student(student).room(room).application(application)
                                .status(StagedAllocationStatus.PROPOSED).batchId("BATCH-001").build();

                RoomAllocationResponse resp1 = new RoomAllocationResponse();
                resp1.setId(10L);

                when(stagedAllocationRepository.findByBatchIdAndStatus("BATCH-001", StagedAllocationStatus.PROPOSED))
                                .thenReturn(Arrays.asList(staged1, staged2));
                when(stagedAllocationRepository.findById(1L)).thenReturn(Optional.of(staged1));
                when(stagedAllocationRepository.findById(2L)).thenThrow(new RuntimeException("Room full"));
                when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
                when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(i -> i.getArguments()[0]);
                when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(resp1);
                when(appUserRepository.findById(2L)).thenReturn(Optional.of(admin));

                // Act
                BatchConfirmationResult result = stagedAllocationService.confirmBatchAllocationsWithResult("BATCH-001", 2L);

                // Assert
                assertThat(result.getSuccess()).isFalse();
                assertThat(result.getTotalAttempted()).isEqualTo(2);
                assertThat(result.getSuccessCount()).isEqualTo(1);
                assertThat(result.getFailureCount()).isEqualTo(1);
                assertThat(result.getErrors()).hasSize(1);
                assertThat(result.getErrors().get(0)).contains("2");
        }

        @Test
        void confirmBatchAllocationsWithResult_emptyBatch_returnsEmptyResult() {
                // Arrange
                when(stagedAllocationRepository.findByBatchIdAndStatus("BATCH-EMPTY", StagedAllocationStatus.PROPOSED))
                                .thenReturn(new ArrayList<>());

                // Act
                BatchConfirmationResult result = stagedAllocationService.confirmBatchAllocationsWithResult("BATCH-EMPTY", 2L);

                // Assert
                assertThat(result.getSuccess()).isTrue();
                assertThat(result.getTotalAttempted()).isEqualTo(0);
                assertThat(result.getSuccessCount()).isEqualTo(0);
                assertThat(result.getFailureCount()).isEqualTo(0);
                assertThat(result.getConfirmedAllocationIds()).isNull();
        }
}
