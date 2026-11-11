package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.dto.RescoreResult;
import com.beezhub.student_accommodation.model.dto.ScoredRoom;
import com.beezhub.student_accommodation.model.dto.WaitingListEntryResponse;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.allocation.ScoringStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AllocationScoringOrchestratorTest {

        @Mock
        private ScoringStrategy allocationScoringService;
        @Mock
        private RoomAllocationRepository roomAllocationRepository;
        @Mock
        private RoomRepository roomRepository;
        @Mock
        private WaitingListRepository waitingListRepository;
        @Mock
        private BuildingRepository buildingRepository;

        /**
         * Real, not a mock: the waiting-list order is what GH-225 changed, and this test asserts
         * that order. Backed by a rule repository with nothing in it, so the neutral weights apply.
         */
        @org.mockito.Spy
        private ApplicantPriorityScorer applicantPriorityScorer =
                new ApplicantPriorityScorer(org.mockito.Mockito.mock(
                        com.beezhub.student_accommodation.repository.AllocationRuleRepository.class));

        @org.mockito.Spy
        private java.time.Clock clock = java.time.Clock.fixed(
                        java.time.Instant.parse("2026-11-10T08:00:00Z"), java.time.ZoneId.of("Africa/Johannesburg"));

        @InjectMocks
        private AllocationScoringOrchestrator allocationScoringOrchestrator;

        private Student student1;
        private Student student2;
        private Room room1;
        private Room room2;
        private Building building;
        private Application application1;
        private Application application2;
        private WaitingListEntry waitingEntry1;
        private WaitingListEntry waitingEntry2;

        @BeforeEach
        void setUp() {
                // Setup building
                building = new Building();
                building.setId(1L);
                building.setName("Test Building");

                // Setup rooms
                room1 = new Room();
                room1.setId(1L);
                room1.setRoomNumber("101");
                room1.setCapacity(2);
                room1.setBuilding(building);

                room2 = new Room();
                room2.setId(2L);
                room2.setRoomNumber("102");
                room2.setCapacity(1);
                room2.setBuilding(building);

                // Setup students
                student1 = new Student();
                student1.setId(1L);
                student1.setStudentNumber("STU001");
                AppUser user1 = new AppUser();
                user1.setId(1L);
                user1.setFirstName("John");
                user1.setLastName("Doe");
                user1.setEmail("john@test.com");
                student1.setAppUser(user1);

                student2 = new Student();
                student2.setId(2L);
                student2.setStudentNumber("STU002");
                AppUser user2 = new AppUser();
                user2.setId(2L);
                user2.setFirstName("Jane");
                user2.setLastName("Smith");
                user2.setEmail("jane@test.com");
                student2.setAppUser(user2);

                // Setup applications
                application1 = new Application();
                application1.setId(1L);
                application1.setStudent(student1);
                application1.setStatus(ApplicationStatus.AWAITING_ALLOCATION);
                application1.setPreferredBuilding(building);

                application2 = new Application();
                application2.setId(2L);
                application2.setStudent(student2);
                application2.setStatus(ApplicationStatus.AWAITING_ALLOCATION);
                application2.setPreferredBuilding(building);

                // Setup waiting list entries
                waitingEntry1 = new WaitingListEntry();
                waitingEntry1.setId(1L);
                waitingEntry1.setStudent(student1);
                waitingEntry1.setApplication(application1);
                waitingEntry1.setStatus("WAITING");
                waitingEntry1.setCreatedAt(LocalDateTime.now().minusDays(2));

                waitingEntry2 = new WaitingListEntry();
                waitingEntry2.setId(2L);
                waitingEntry2.setStudent(student2);
                waitingEntry2.setApplication(application2);
                waitingEntry2.setStatus("WAITING");
                waitingEntry2.setCreatedAt(LocalDateTime.now().minusDays(1));
        }

        @Test
        void buildScoringMatrix_multipleStudents_rankedByScore() {
                // Arrange
                List<WaitingListEntry> waitingList = Arrays.asList(waitingEntry1, waitingEntry2);

                ScoredRoom scoredRoom1 = ScoredRoom.builder()
                                .roomId(1L)
                                .roomNumber("101")
                                .totalScore(BigDecimal.valueOf(85.0))
                                .warnings(new ArrayList<>())
                                .build();

                ScoredRoom scoredRoom2 = ScoredRoom.builder()
                                .roomId(2L)
                                .roomNumber("102")
                                .totalScore(BigDecimal.valueOf(75.0))
                                .warnings(new ArrayList<>())
                                .build();

                when(roomAllocationRepository.existsLiveAllocationForStudentDuring(anyLong(), any(), any())).thenReturn(false);
                when(allocationScoringService.scoreAvailableRooms(student1, application1, building))
                                .thenReturn(Arrays.asList(scoredRoom1));
                when(allocationScoringService.scoreAvailableRooms(student2, application2, building))
                                .thenReturn(Arrays.asList(scoredRoom2));

                // Act
                Map<Long, List<ScoredRoom>> scoringMatrix = allocationScoringOrchestrator
                                .buildScoringMatrix(waitingList);

                // Assert
                assertThat(scoringMatrix).hasSize(2);
                assertThat(scoringMatrix.get(1L)).hasSize(1);
                assertThat(scoringMatrix.get(1L).get(0).getTotalScore()).isEqualByComparingTo(BigDecimal.valueOf(85.0));
                assertThat(scoringMatrix.get(2L)).hasSize(1);
                assertThat(scoringMatrix.get(2L).get(0).getTotalScore()).isEqualByComparingTo(BigDecimal.valueOf(75.0));
        }

        @Test
        void buildScoringMatrix_studentWithAllocation_skipped() {
                // Arrange
                List<WaitingListEntry> waitingList = Arrays.asList(waitingEntry1, waitingEntry2);

                when(roomAllocationRepository.existsLiveAllocationForStudentDuring(eq(1L), any(), any())).thenReturn(true); // Student 1
                                                                                                       // already
                                                                                                       // allocated
                when(roomAllocationRepository.existsLiveAllocationForStudentDuring(eq(2L), any(), any())).thenReturn(false);

                ScoredRoom scoredRoom2 = ScoredRoom.builder()
                                .roomId(2L)
                                .roomNumber("102")
                                .totalScore(BigDecimal.valueOf(75.0))
                                .warnings(new ArrayList<>())
                                .build();

                when(allocationScoringService.scoreAvailableRooms(student2, application2, building))
                                .thenReturn(Arrays.asList(scoredRoom2));

                // Act
                Map<Long, List<ScoredRoom>> scoringMatrix = allocationScoringOrchestrator
                                .buildScoringMatrix(waitingList);

                // Assert
                assertThat(scoringMatrix).hasSize(1); // Only student2
                assertThat(scoringMatrix.containsKey(1L)).isFalse();
                assertThat(scoringMatrix.containsKey(2L)).isTrue();
        }

        @Test
        void buildScoringMatrix_emptyWaitingList_returnsEmptyMap() {
                // Arrange
                List<WaitingListEntry> emptyList = new ArrayList<>();

                // Act
                Map<Long, List<ScoredRoom>> scoringMatrix = allocationScoringOrchestrator.buildScoringMatrix(emptyList);

                // Assert
                assertThat(scoringMatrix).isEmpty();
        }

        /** GH-496: a stay that has already ended is never offered, so it is not scored. */
        @Test
        void buildScoringMatrix_stayHasEnded_skipped() {
                Application ended = new Application();
                ended.setId(99L);
                Intake intake = new Intake();
                intake.setStayStartDate(java.time.LocalDate.of(2026, 2, 1));
                intake.setStayEndDate(java.time.LocalDate.of(2026, 6, 30));
                ended.setIntake(intake);
                WaitingListEntry entry = new WaitingListEntry();
                entry.setId(7L);
                entry.setStudent(student1);
                entry.setApplication(ended);

                Map<Long, List<ScoredRoom>> scoringMatrix =
                                allocationScoringOrchestrator.buildScoringMatrix(List.of(entry));

                assertThat(scoringMatrix).isEmpty();
                verify(allocationScoringService, never()).scoreAvailableRooms(any(), any(), any());
        }

        /** GH-496: a 2026 resident is matched for a 2027 stay; the overlap query is asked for 2027. */
        @Test
        void buildScoringMatrix_residentForNextYear_scoredForThatStay() {
                Application nextYear = new Application();
                nextYear.setId(98L);
                Intake intake = new Intake();
                intake.setStayStartDate(java.time.LocalDate.of(2027, 2, 1));
                intake.setStayEndDate(java.time.LocalDate.of(2027, 11, 30));
                nextYear.setIntake(intake);
                WaitingListEntry entry = new WaitingListEntry();
                entry.setId(8L);
                entry.setStudent(student1);
                entry.setApplication(nextYear);
                when(roomAllocationRepository.existsLiveAllocationForStudentDuring(
                                student1.getId(), java.time.LocalDate.of(2027, 2, 1), java.time.LocalDate.of(2027, 11, 30)))
                                .thenReturn(false);
                when(allocationScoringService.scoreAvailableRooms(any(), any(), any()))
                                .thenReturn(List.of(ScoredRoom.builder().roomId(1L).build()));

                Map<Long, List<ScoredRoom>> scoringMatrix =
                                allocationScoringOrchestrator.buildScoringMatrix(List.of(entry));

                assertThat(scoringMatrix).containsKey(student1.getId());
        }

        @Test
        void getAvailableRoomsForBatch_returnsOnlyAvailable() {
                // Arrange: every room open for offers; beds are checked per stay by the ledger
                when(roomRepository.findRoomsOpenForOffers()).thenReturn(Arrays.asList(room1));

                // Act
                List<Room> availableRooms = allocationScoringOrchestrator.getAvailableRoomsForBatch();

                // Assert
                assertThat(availableRooms).hasSize(1);
                assertThat(availableRooms.get(0).getId()).isEqualTo(1L);
        }

        @Test
        void getWaitingListWithScores_sortedByScore() {
                // Arrange
                when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING"))
                                .thenReturn(Arrays.asList(waitingEntry1, waitingEntry2));

                ScoredRoom highScoreRoom = ScoredRoom.builder()
                                .roomId(1L)
                                .roomNumber("101")
                                .totalScore(BigDecimal.valueOf(90.0))
                                .warnings(new ArrayList<>())
                                .build();

                ScoredRoom lowScoreRoom = ScoredRoom.builder()
                                .roomId(2L)
                                .roomNumber("102")
                                .totalScore(BigDecimal.valueOf(60.0))
                                .warnings(new ArrayList<>())
                                .build();

                when(allocationScoringService.scoreAvailableRooms(student1, application1, building))
                                .thenReturn(Arrays.asList(highScoreRoom));
                when(allocationScoringService.scoreAvailableRooms(student2, application2, building))
                                .thenReturn(Arrays.asList(lowScoreRoom));

                // Act
                List<WaitingListEntryResponse> responses = allocationScoringOrchestrator.getWaitingListWithScores(null);

                // Assert
                assertThat(responses).hasSize(2);
                // Should be sorted by score descending
                assertThat(responses.get(0).getStudentId()).isEqualTo(1L); // Higher score first
                assertThat(responses.get(0).getPosition()).isEqualTo(1);
                assertThat(responses.get(0).getPriorityScore()).isEqualByComparingTo(BigDecimal.valueOf(90.0));
                assertThat(responses.get(1).getStudentId()).isEqualTo(2L);
                assertThat(responses.get(1).getPosition()).isEqualTo(2);
        }

        @Test
        void getWaitingListWithScores_withBuilding_filtersCorrectly() {
                // Arrange
                when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING"))
                                .thenReturn(Arrays.asList(waitingEntry1));
                when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));

                ScoredRoom scoredRoom = ScoredRoom.builder()
                                .roomId(1L)
                                .roomNumber("101")
                                .totalScore(BigDecimal.valueOf(85.0))
                                .warnings(new ArrayList<>())
                                .build();

                when(allocationScoringService.scoreAvailableRooms(student1, application1, building))
                                .thenReturn(Arrays.asList(scoredRoom));

                // Act
                List<WaitingListEntryResponse> responses = allocationScoringOrchestrator.getWaitingListWithScores(1L);

                // Assert
                assertThat(responses).hasSize(1);
                assertThat(responses.get(0).getBestMatchRoomId()).isEqualTo(1L);
        }

        @Test
        void rescoreWaitingList_updatesAllEntries() {
                // Arrange
                when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING"))
                                .thenReturn(Arrays.asList(waitingEntry1, waitingEntry2));

                ScoredRoom scoredRoom = ScoredRoom.builder()
                                .roomId(1L)
                                .roomNumber("101")
                                .totalScore(BigDecimal.valueOf(85.0))
                                .warnings(new ArrayList<>())
                                .build();

                when(allocationScoringService.scoreAvailableRooms(any(Student.class), any(Application.class),
                                any(Building.class)))
                                .thenReturn(Arrays.asList(scoredRoom));

                // Act
                RescoreResult result = allocationScoringOrchestrator.rescoreWaitingList();

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getSuccess()).isTrue();
                assertThat(result.getRescored()).isEqualTo(2);
                assertThat(result.getMessage()).contains("Successfully rescored 2");
        }

        @Test
        void rescoreWaitingList_emptyList_returnsZero() {
                // Arrange
                when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING"))
                                .thenReturn(new ArrayList<>());

                // Act
                RescoreResult result = allocationScoringOrchestrator.rescoreWaitingList();

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getSuccess()).isTrue();
                assertThat(result.getRescored()).isEqualTo(0);
                assertThat(result.getMessage()).contains("No waiting list entries");
        }
}
