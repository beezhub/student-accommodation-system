package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.config.FF4jConfig;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.BatchAllocationResult;
import com.beezhub.student_accommodation.model.dto.RoomAllocationRequest;
import com.beezhub.student_accommodation.model.dto.ScoredRoom;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.GenderRestriction;
import com.beezhub.student_accommodation.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ff4j.FF4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class RoomAllocationServiceBatchTest {

    @Mock
    private RoomAllocationRepository roomAllocationRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private WaitingListRepository waitingListRepository;

    @Mock
    private AllocationScoringService allocationScoringService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private FF4j ff4j;

    @Mock
    private Environment environment;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private RoomAllocationMapper roomAllocationMapper;

    @Mock
    private StagedAllocationRepository stagedAllocationRepository;

    @Mock
    private AllocationAuditLogRepository allocationAuditLogRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private RoomAttributesRepository roomAttributesRepository;

    @Mock
    private UserNotificationService userNotificationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RoomAllocationService roomAllocationService;

    private Student student1;
    private Student student2;
    private Student student3;
    private Room room1;
    private Room room2;
    private Room sharedRoom;
    private WaitingListEntry entry1;
    private WaitingListEntry entry2;
    private WaitingListEntry entry3;
    private Application application1;
    private Application application2;
    private Application application3;

    @BeforeEach
    void setUp() {
        // Mock FF4j feature flag to be enabled (lenient for tests that override)
        lenient().when(ff4j.check(FF4jConfig.FEATURE_AUTO_ALLOCATION)).thenReturn(true);

        // Mock environment properties (lenient as not all tests need these)
        lenient().when(environment.getProperty("app.allocation.batch-enabled", Boolean.class, true)).thenReturn(true);
        lenient().when(environment.getProperty("app.allocation.batch-size", Integer.class, 50)).thenReturn(50);
        lenient().when(environment.getProperty("app.allocation.batch-on-expiry", Boolean.class, true)).thenReturn(true);

        // Setup test data
        setupTestData();
    }

    private void setupTestData() {
        // Students
        AppUser appUser1 = new AppUser();
        appUser1.setId(1L);
        appUser1.setFirstName("John");
        appUser1.setLastName("Doe");

        student1 = new Student();
        student1.setId(1L);
        student1.setGender("MALE");
        student1.setDateOfBirth(new Date());
        student1.setAppUser(appUser1);

        AppUser appUser2 = new AppUser();
        appUser2.setId(2L);
        appUser2.setFirstName("Jane");
        appUser2.setLastName("Smith");

        student2 = new Student();
        student2.setId(2L);
        student2.setGender("FEMALE");
        student2.setDateOfBirth(new Date());
        student2.setAppUser(appUser2);

        AppUser appUser3 = new AppUser();
        appUser3.setId(3L);
        appUser3.setFirstName("Bob");
        appUser3.setLastName("Johnson");

        student3 = new Student();
        student3.setId(3L);
        student3.setGender("MALE");
        student3.setDateOfBirth(new Date());
        student3.setAppUser(appUser3);

        // Applications
        application1 = new Application();
        application1.setId(1L);
        application1.setStudent(student1);
        application1.setStatus(ApplicationStatus.APPROVED);

        application2 = new Application();
        application2.setId(2L);
        application2.setStudent(student2);
        application2.setStatus(ApplicationStatus.APPROVED);

        application3 = new Application();
        application3.setId(3L);
        application3.setStudent(student3);
        application3.setStatus(ApplicationStatus.APPROVED);

        // Rooms
        Building building = new Building();
        building.setId(1L);
        building.setName("Test Building");

        RoomAttributes attributes1 = RoomAttributes.builder()
                .genderRestriction(GenderRestriction.MALE_ONLY)
                .build();

        room1 = new Room();
        room1.setId(1L);
        room1.setRoomNumber("101");
        room1.setCapacity(1);
        room1.setBuilding(building);
        room1.setAttributes(attributes1);
        attributes1.setRoom(room1);

        RoomAttributes attributes2 = RoomAttributes.builder()
                .genderRestriction(GenderRestriction.FEMALE_ONLY)
                .build();

        room2 = new Room();
        room2.setId(2L);
        room2.setRoomNumber("102");
        room2.setCapacity(1);
        room2.setBuilding(building);
        room2.setAttributes(attributes2);
        attributes2.setRoom(room2);

        RoomAttributes attributes3 = RoomAttributes.builder()
                .genderRestriction(GenderRestriction.MALE_ONLY)
                .build();

        sharedRoom = new Room();
        sharedRoom.setId(3L);
        sharedRoom.setRoomNumber("201");
        sharedRoom.setCapacity(2);
        sharedRoom.setBuilding(building);
        sharedRoom.setAttributes(attributes3);
        attributes3.setRoom(sharedRoom);

        // Waiting list entries
        entry1 = new WaitingListEntry();
        entry1.setId(1L);
        entry1.setStudent(student1);
        entry1.setApplication(application1);
        entry1.setStatus("WAITING");
        entry1.setCreatedAt(LocalDateTime.now().minusDays(3));

        entry2 = new WaitingListEntry();
        entry2.setId(2L);
        entry2.setStudent(student2);
        entry2.setApplication(application2);
        entry2.setStatus("WAITING");
        entry2.setCreatedAt(LocalDateTime.now().minusDays(2));

        entry3 = new WaitingListEntry();
        entry3.setId(3L);
        entry3.setStudent(student3);
        entry3.setApplication(application3);
        entry3.setStatus("WAITING");
        entry3.setCreatedAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    void batchAllocate_shouldAllocateMultipleStudents_whenRoomsAvailable() {
        // Given: 3 waiting students and 2 available rooms
        List<WaitingListEntry> waitingList = Arrays.asList(entry1, entry2, entry3);
        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(waitingList);

        // Mock no active allocations for students
        when(roomAllocationRepository.existsActiveAllocationByStudentId(anyLong())).thenReturn(false);

        // Mock scored rooms
        ScoredRoom scoredRoom1 = createScoredRoom(room1.getId(), new BigDecimal("85.0"));
        ScoredRoom scoredRoom2 = createScoredRoom(room2.getId(), new BigDecimal("80.0"));

        when(allocationScoringService.scoreAvailableRooms(eq(student1), any(), any()))
                .thenReturn(Arrays.asList(scoredRoom1));
        when(allocationScoringService.scoreAvailableRooms(eq(student2), any(), any()))
                .thenReturn(Arrays.asList(scoredRoom2));
        when(allocationScoringService.scoreAvailableRooms(eq(student3), any(), any()))
                .thenReturn(Arrays.asList(scoredRoom1)); // No room available for student3

        // Mock available rooms
        when(roomRepository.findAll()).thenReturn(Arrays.asList(room1, room2));
        when(roomAllocationRepository.countActiveAllocationsByRoomId(room1.getId())).thenReturn(0L);
        when(roomAllocationRepository.countActiveAllocationsByRoomId(room2.getId())).thenReturn(0L);

        // Setup mocks for allocation creation
        setupAllocationCreationMocks();

        // When
        BatchAllocationResult result = roomAllocationService.batchAllocateFromWaitingList(10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSuccessCount()).isEqualTo(2); // Only 2 rooms available
        assertThat(result.getFailureCount()).isEqualTo(0);
        assertThat(result.getTotalAttempted()).isEqualTo(2);
        assertThat(result.getAllocatedStudentIds()).hasSize(2);
        assertThat(result.getProcessingTimeMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void batchAllocate_shouldRespectRoomCapacity() {
        // Given: 3 students, 1 shared room with capacity 2
        List<WaitingListEntry> waitingList = Arrays.asList(entry1, entry3);
        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(waitingList);

        when(roomAllocationRepository.existsActiveAllocationByStudentId(anyLong())).thenReturn(false);

        ScoredRoom scoredSharedRoom = createScoredRoom(sharedRoom.getId(), new BigDecimal("90.0"));

        when(allocationScoringService.scoreAvailableRooms(eq(student1), any(), any()))
                .thenReturn(Arrays.asList(scoredSharedRoom));
        when(allocationScoringService.scoreAvailableRooms(eq(student3), any(), any()))
                .thenReturn(Arrays.asList(scoredSharedRoom));

        when(roomRepository.findAll()).thenReturn(Arrays.asList(sharedRoom));
        when(roomAllocationRepository.countActiveAllocationsByRoomId(sharedRoom.getId())).thenReturn(0L);

        setupAllocationCreationMocks();

        // When
        BatchAllocationResult result = roomAllocationService.batchAllocateFromWaitingList(10);

        // Then
        assertThat(result.getSuccessCount()).isEqualTo(2); // Both students allocated to shared room
        assertThat(result.getTotalAttempted()).isEqualTo(2);
    }

    @Test
    void batchAllocate_shouldRespectMaxAllocationsLimit() {
        // Given: 3 students, 3 rooms, but maxAllocations = 2
        List<WaitingListEntry> waitingList = Arrays.asList(entry1, entry2, entry3);
        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(waitingList);

        when(roomAllocationRepository.existsActiveAllocationByStudentId(anyLong())).thenReturn(false);

        ScoredRoom scoredRoom1 = createScoredRoom(room1.getId(), new BigDecimal("85.0"));
        ScoredRoom scoredRoom2 = createScoredRoom(room2.getId(), new BigDecimal("80.0"));

        when(allocationScoringService.scoreAvailableRooms(any(), any(), any()))
                .thenReturn(Arrays.asList(scoredRoom1, scoredRoom2));

        when(roomRepository.findAll()).thenReturn(Arrays.asList(room1, room2));
        when(roomAllocationRepository.countActiveAllocationsByRoomId(anyLong())).thenReturn(0L);

        setupAllocationCreationMocks();

        // When: Limit to 2 allocations
        BatchAllocationResult result = roomAllocationService.batchAllocateFromWaitingList(2);

        // Then
        assertThat(result.getTotalAttempted()).isLessThanOrEqualTo(2);
    }

    @Test
    void batchAllocate_shouldHandlePartialFailures() {
        // Given: 2 students, but one allocation will fail
        List<WaitingListEntry> waitingList = Arrays.asList(entry1, entry2);
        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(waitingList);

        when(roomAllocationRepository.existsActiveAllocationByStudentId(anyLong())).thenReturn(false);

        ScoredRoom scoredRoom1 = createScoredRoom(room1.getId(), new BigDecimal("85.0"));
        ScoredRoom scoredRoom2 = createScoredRoom(room2.getId(), new BigDecimal("80.0"));

        when(allocationScoringService.scoreAvailableRooms(eq(student1), any(), any()))
                .thenReturn(Arrays.asList(scoredRoom1));
        when(allocationScoringService.scoreAvailableRooms(eq(student2), any(), any()))
                .thenReturn(Arrays.asList(scoredRoom2));

        when(roomRepository.findAll()).thenReturn(Arrays.asList(room1, room2));
        when(roomAllocationRepository.countActiveAllocationsByRoomId(anyLong())).thenReturn(0L);

        // Setup base mocks
        setupAllocationCreationMocks();

        // Reset and override transaction template: First allocation succeeds, second fails
        reset(transactionTemplate);
        when(transactionTemplate.execute(any()))
                .thenAnswer(invocation -> {
                    invocation.getArgument(0, org.springframework.transaction.support.TransactionCallback.class).doInTransaction(null);
                    return null;
                })
                .thenThrow(new RuntimeException("Simulated allocation failure"));

        // When
        BatchAllocationResult result = roomAllocationService.batchAllocateFromWaitingList(10);

        // Then
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0)).contains("Simulated allocation failure");
    }

    @Test
    void batchAllocate_shouldProcessInFIFOOrder() {
        // Given: 2 students for 1 room, entry1 added first (older created date)
        List<WaitingListEntry> waitingList = Arrays.asList(entry1, entry3);
        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(waitingList);

        when(roomAllocationRepository.existsActiveAllocationByStudentId(anyLong())).thenReturn(false);

        ScoredRoom scoredRoom1 = createScoredRoom(room1.getId(), new BigDecimal("85.0"));

        when(allocationScoringService.scoreAvailableRooms(any(), any(), any()))
                .thenReturn(Arrays.asList(scoredRoom1));

        when(roomRepository.findAll()).thenReturn(Arrays.asList(room1));
        when(roomAllocationRepository.countActiveAllocationsByRoomId(room1.getId())).thenReturn(0L);

        setupAllocationCreationMocks();

        // When
        BatchAllocationResult result = roomAllocationService.batchAllocateFromWaitingList(10);

        // Then: First student (entry1) should get the room
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getAllocatedStudentIds()).containsExactly(student1.getId());
    }

    @Test
    void batchAllocate_shouldSkipStudentsWithActiveAllocations() {
        // Given: 2 students, but student1 already has active allocation
        List<WaitingListEntry> waitingList = Arrays.asList(entry1, entry2);
        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(waitingList);

        when(roomAllocationRepository.existsActiveAllocationByStudentId(student1.getId())).thenReturn(true);
        when(roomAllocationRepository.existsActiveAllocationByStudentId(student2.getId())).thenReturn(false);

        ScoredRoom scoredRoom2 = createScoredRoom(room2.getId(), new BigDecimal("80.0"));

        when(allocationScoringService.scoreAvailableRooms(eq(student2), any(), any()))
                .thenReturn(Arrays.asList(scoredRoom2));

        when(roomRepository.findAll()).thenReturn(Arrays.asList(room2));
        when(roomAllocationRepository.countActiveAllocationsByRoomId(room2.getId())).thenReturn(0L);

        setupAllocationCreationMocks();

        // When
        BatchAllocationResult result = roomAllocationService.batchAllocateFromWaitingList(10);

        // Then: Only student2 should be allocated
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getAllocatedStudentIds()).containsExactly(student2.getId());
    }

    @Test
    void batchAllocate_shouldReturnEmptyResult_whenNoWaitingStudents() {
        // Given: Empty waiting list
        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(new ArrayList<>());

        // When
        BatchAllocationResult result = roomAllocationService.batchAllocateFromWaitingList(10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalAttempted()).isEqualTo(0);
        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailureCount()).isEqualTo(0);
    }

    @Test
    void batchAllocate_shouldReturnEmptyResult_whenNoAvailableRooms() {
        // Given: Students on waiting list but no available rooms
        List<WaitingListEntry> waitingList = Arrays.asList(entry1, entry2);
        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(waitingList);

        when(roomAllocationRepository.existsActiveAllocationByStudentId(anyLong())).thenReturn(false);

        when(allocationScoringService.scoreAvailableRooms(any(), any(), any()))
                .thenReturn(new ArrayList<>());

        when(roomRepository.findAll()).thenReturn(new ArrayList<>());

        // When
        BatchAllocationResult result = roomAllocationService.batchAllocateFromWaitingList(10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalAttempted()).isEqualTo(0);
        assertThat(result.getSuccessCount()).isEqualTo(0);
    }

    @Test
    void batchAllocate_shouldReturnEmptyResult_whenFeatureFlagDisabled() {
        // Given: Feature flag disabled
        when(ff4j.check(FF4jConfig.FEATURE_AUTO_ALLOCATION)).thenReturn(false);

        // When
        BatchAllocationResult result = roomAllocationService.batchAllocateFromWaitingList(10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getTotalAttempted()).isEqualTo(0);
        verify(waitingListRepository, never()).findByStatusOrderByCreatedAtAsc(anyString());
    }

    private ScoredRoom createScoredRoom(Long roomId, BigDecimal score) {
        ScoredRoom scoredRoom = new ScoredRoom();
        scoredRoom.setRoomId(roomId);
        scoredRoom.setTotalScore(score);
        return scoredRoom;
    }

    /**
     * Helper method to setup common mocks needed for createRoomAllocation
     */
    private void setupAllocationCreationMocks() {
        // Mock student repository lookups (lenient as not all students used in every test)
        lenient().when(studentRepository.findById(student1.getId())).thenReturn(Optional.of(student1));
        lenient().when(studentRepository.findById(student2.getId())).thenReturn(Optional.of(student2));
        lenient().when(studentRepository.findById(student3.getId())).thenReturn(Optional.of(student3));

        // Mock room repository lookups (lenient as not all rooms used in every test)
        lenient().when(roomRepository.findById(room1.getId())).thenReturn(Optional.of(room1));
        lenient().when(roomRepository.findById(room2.getId())).thenReturn(Optional.of(room2));
        lenient().when(roomRepository.findById(sharedRoom.getId())).thenReturn(Optional.of(sharedRoom));

        // Mock application repository
        lenient().when(applicationRepository.findById(anyLong())).thenAnswer(invocation -> {
            Long appId = invocation.getArgument(0);
            if (appId.equals(1L)) return Optional.of(application1);
            if (appId.equals(2L)) return Optional.of(application2);
            if (appId.equals(3L)) return Optional.of(application3);
            return Optional.empty();
        });

        // Mock allocation saves and checks
        lenient().when(roomAllocationRepository.existsByApplicationId(anyLong())).thenReturn(false);
        lenient().when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(new RoomAllocation());
        lenient().when(waitingListRepository.save(any(WaitingListEntry.class))).thenAnswer(i -> i.getArgument(0));

        // Mock transaction template to execute the callback
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            invocation.getArgument(0, org.springframework.transaction.support.TransactionCallback.class).doInTransaction(null);
            return null;
        });
    }
}
