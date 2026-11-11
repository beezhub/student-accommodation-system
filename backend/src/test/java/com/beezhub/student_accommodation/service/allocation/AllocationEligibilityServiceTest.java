package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.exceptions.StudentNotFoundException;
import com.beezhub.student_accommodation.model.dto.AllocationEligibilityResponse;
import com.beezhub.student_accommodation.model.dto.EligibleRoomResponse;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.GenderRestriction;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.LegacyScoringStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AllocationEligibilityServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private RoomAllocationRepository roomAllocationRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private BuildingRepository buildingRepository;
    @Mock
    private RoomAttributesRepository roomAttributesRepository;
    @Mock
    private LegacyScoringStrategy allocationScoringService;
    @Mock
    private AllocationAccessGuard allocationAccessGuard;

    @InjectMocks
    private AllocationEligibilityService allocationEligibilityService;

    private Student student;
    private Building building;
    private Room room1;
    private Room room2;
    private RoomAttributes roomAttributes1;
    private Application application;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(allocationEligibilityService, "stayCapacityGuard",
                new com.beezhub.student_accommodation.service.allocation.StayCapacityGuard(roomAllocationRepository,
                        java.time.Clock.fixed(java.time.Instant.parse("2026-11-10T08:00:00Z"),
                                java.time.ZoneId.of("Africa/Johannesburg"))));
        // Setup student
        student = new Student();
        student.setId(1L);
        student.setStudentNumber("STU001");
        AppUser appUser = new AppUser();
        appUser.setId(1L);
        appUser.setFirstName("John");
        appUser.setLastName("Doe");
        student.setAppUser(appUser);

        // Setup building
        building = new Building();
        building.setId(1L);
        building.setName("Test Building");
        building.setAddress("123 Test St");

        // Setup rooms
        room1 = new Room();
        room1.setId(1L);
        room1.setRoomNumber("101");
        room1.setCapacity(2);
        room1.setStatus("AVAILABLE");
        room1.setBuilding(building);

        room2 = new Room();
        room2.setId(2L);
        room2.setRoomNumber("102");
        room2.setCapacity(1);
        room2.setStatus("AVAILABLE");
        room2.setBuilding(building);

        // Setup room attributes
        roomAttributes1 = new RoomAttributes();
        roomAttributes1.setId(1L);
        roomAttributes1.setRoom(room1);
        roomAttributes1.setGenderRestriction(GenderRestriction.ANY);
        roomAttributes1.setAccessibilityCompliant(true);
        roomAttributes1.setMinYearOfStudy(1);
        roomAttributes1.setMaxYearOfStudy(4);

        // Setup application
        application = new Application();
        application.setId(1L);
        application.setStudent(student);
        application.setStatus(ApplicationStatus.APPROVED);
    }

    @Test
    void checkStudentEligibility_callerNotThatStudent_throwsBeforeLookup() {
        doThrow(new com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException("not yours"))
                .when(allocationAccessGuard).assertCanAccessStudent(1L);

        assertThatThrownBy(() -> allocationEligibilityService.checkStudentEligibility(1L))
                .isInstanceOf(com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException.class);

        verify(studentRepository, never()).findById(any());
        verify(roomAllocationRepository, never()).existsLiveAllocationForStudentDuring(any(), any(), any());
    }

    @Test
    void checkStudentEligibility_noAllocation_eligible() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomAllocationRepository.existsLiveAllocationForStudentDuring(eq(1L), any(), any())).thenReturn(false);
        when(applicationRepository.existsByStudent_IdAndStatus(1L, ApplicationStatus.APPROVED)).thenReturn(true);

        // Act
        AllocationEligibilityResponse response = allocationEligibilityService.checkStudentEligibility(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStudentId()).isEqualTo(1L);
        assertThat(response.isEligible()).isTrue();
        assertThat(response.isHasActiveAllocation()).isFalse();
        assertThat(response.isHasApprovedApplication()).isTrue();
        assertThat(response.getReason()).contains("eligible for room allocation");
    }

    @Test
    void checkStudentEligibility_alreadyAllocated_notEligible() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomAllocationRepository.existsLiveAllocationForStudentDuring(eq(1L), any(), any())).thenReturn(true);

        // Act
        AllocationEligibilityResponse response = allocationEligibilityService.checkStudentEligibility(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStudentId()).isEqualTo(1L);
        assertThat(response.isEligible()).isFalse();
        assertThat(response.isHasActiveAllocation()).isTrue();
        assertThat(response.getReason()).contains("already has an active room allocation");
    }

    @Test
    void checkStudentEligibility_studentNotFound_throwsException() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> allocationEligibilityService.checkStudentEligibility(1L))
                .isInstanceOf(StudentNotFoundException.class)
                .hasMessageContaining("Student not found");
    }

    @Test
    void getEligibleRoomsForStudent_noBuilding_returnsAll() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(allocationScoringService.getEligibleRoomsForStudent(student, null, StayPeriod.UNBOUNDED))
                .thenReturn(Arrays.asList(room1, room2));
        when(roomAllocationRepository.countHoldingBeds(eq(1L), any(), any())).thenReturn(0L);
        when(roomAllocationRepository.countHoldingBeds(eq(2L), any(), any())).thenReturn(0L);
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.of(roomAttributes1));
        when(roomAttributesRepository.findByRoom_Id(2L)).thenReturn(Optional.empty());

        // Act
        List<EligibleRoomResponse> responses = allocationEligibilityService.getEligibleRoomsForStudent(1L, null);

        // Assert
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getRoomId()).isEqualTo(1L);
        assertThat(responses.get(0).getRoomNumber()).isEqualTo("101");
        assertThat(responses.get(0).getAvailableSpots()).isEqualTo(2);
        assertThat(responses.get(0).getGenderRestriction()).isEqualTo("ANY");
        assertThat(responses.get(0).getAccessibilityCompliant()).isTrue();
    }

    @Test
    void getEligibleRoomsForStudent_withBuilding_filtersCorrectly() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(allocationScoringService.getEligibleRoomsForStudent(student, building, StayPeriod.UNBOUNDED))
                .thenReturn(Arrays.asList(room1));
        when(roomAllocationRepository.countHoldingBeds(eq(1L), any(), any())).thenReturn(1L); // 1 occupied
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.of(roomAttributes1));

        // Act
        List<EligibleRoomResponse> responses = allocationEligibilityService.getEligibleRoomsForStudent(1L, 1L);

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getRoomId()).isEqualTo(1L);
        assertThat(responses.get(0).getCurrentOccupancy()).isEqualTo(1);
        assertThat(responses.get(0).getAvailableSpots()).isEqualTo(1); // 2 capacity - 1 occupied
        assertThat(responses.get(0).getBuildingId()).isEqualTo(1L);
        assertThat(responses.get(0).getBuildingName()).isEqualTo("Test Building");
        assertThat(responses.get(0).getBuildingAddress()).isEqualTo("123 Test St");
    }

    @Test
    void getEligibleRoomsForStudent_fullRooms_showsZeroAvailability() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(allocationScoringService.getEligibleRoomsForStudent(student, null, StayPeriod.UNBOUNDED))
                .thenReturn(Arrays.asList(room1));
        when(roomAllocationRepository.countHoldingBeds(eq(1L), any(), any())).thenReturn(2L); // Full capacity
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.of(roomAttributes1));

        // Act
        List<EligibleRoomResponse> responses = allocationEligibilityService.getEligibleRoomsForStudent(1L, null);

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getCurrentOccupancy()).isEqualTo(2);
        assertThat(responses.get(0).getAvailableSpots()).isEqualTo(0);
    }

    @Test
    void getEligibleRoomsForStudent_noStudent_throwsException() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> allocationEligibilityService.getEligibleRoomsForStudent(1L, null))
                .isInstanceOf(StudentNotFoundException.class)
                .hasMessageContaining("Student not found");
    }

    @Test
    void getEligibleRoomsForStudent_noRoomAttributes_handlesGracefully() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(allocationScoringService.getEligibleRoomsForStudent(student, null, StayPeriod.UNBOUNDED))
                .thenReturn(Arrays.asList(room2));
        when(roomAllocationRepository.countHoldingBeds(eq(2L), any(), any())).thenReturn(0L);
        when(roomAttributesRepository.findByRoom_Id(2L)).thenReturn(Optional.empty()); // No attributes

        // Act
        List<EligibleRoomResponse> responses = allocationEligibilityService.getEligibleRoomsForStudent(1L, null);

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getRoomId()).isEqualTo(2L);
        assertThat(responses.get(0).getGenderRestriction()).isNull();
        assertThat(responses.get(0).getAccessibilityCompliant()).isNull();
        assertThat(responses.get(0).getMinYearOfStudy()).isNull();
        assertThat(responses.get(0).getMaxYearOfStudy()).isNull();
    }

    // ── GH-496: eligibility by overlapping stays ─────────────────────────────────────────────

    private Application applicationFor(LocalDate stayStart, LocalDate stayEnd) {
        Intake intake = new Intake();
        intake.setStayStartDate(stayStart);
        intake.setStayEndDate(stayEnd);
        Application app = new Application();
        app.setId(5L);
        app.setStudent(student);
        app.setStatus(ApplicationStatus.AWAITING_ALLOCATION);
        app.setIntake(intake);
        return app;
    }

    @Test
    void checkStudentEligibility_2026ResidentApplyingFor2027_eligible() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(applicationRepository.findByStudent_Id(1L))
                .thenReturn(List.of(applicationFor(LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30))));
        // The 2026 room does not overlap 2027.
        when(roomAllocationRepository.existsLiveAllocationForStudentDuring(
                1L, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30))).thenReturn(false);

        AllocationEligibilityResponse response = allocationEligibilityService.checkStudentEligibility(1L);

        assertThat(response.isEligible()).isTrue();
        assertThat(response.isHasActiveAllocation()).isFalse();
    }

    @Test
    void checkStudentEligibility_overlappingStayAlreadyHeld_notEligible() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(applicationRepository.findByStudent_Id(1L))
                .thenReturn(List.of(applicationFor(LocalDate.of(2026, 7, 15), LocalDate.of(2026, 11, 30))));
        when(roomAllocationRepository.existsLiveAllocationForStudentDuring(
                1L, LocalDate.of(2026, 7, 15), LocalDate.of(2026, 11, 30))).thenReturn(true);

        AllocationEligibilityResponse response = allocationEligibilityService.checkStudentEligibility(1L);

        assertThat(response.isEligible()).isFalse();
        assertThat(response.getReason()).contains("already has an active room allocation");
    }

    @Test
    void checkStudentEligibility_onlyEndedStays_fallsBackToAnyLiveAllocation() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        // Ended on 30 June 2026; today is 10 November 2026, so it is never offered.
        when(applicationRepository.findByStudent_Id(1L))
                .thenReturn(List.of(applicationFor(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 6, 30))));
        when(roomAllocationRepository.existsLiveAllocationForStudentDuring(
                1L, StayPeriod.UNBOUNDED.from(), StayPeriod.UNBOUNDED.until())).thenReturn(false);

        AllocationEligibilityResponse response = allocationEligibilityService.checkStudentEligibility(1L);

        assertThat(response.isEligible()).isTrue();
    }

    @Test
    void getEligibleRoomsForStudent_countsBedsForTheStayAppliedFor() {
        StayPeriod stay = new StayPeriod(LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(applicationRepository.findByStudent_Id(1L)).thenReturn(List.of(applicationFor(stay.start(), stay.end())));
        when(allocationScoringService.getEligibleRoomsForStudent(student, null, stay)).thenReturn(List.of(room1));
        when(roomAllocationRepository.countHoldingBeds(1L, stay.from(), stay.until())).thenReturn(1L);

        List<EligibleRoomResponse> responses = allocationEligibilityService.getEligibleRoomsForStudent(1L, null);

        assertThat(responses.get(0).getAvailableSpots()).isEqualTo(1);
    }
}
