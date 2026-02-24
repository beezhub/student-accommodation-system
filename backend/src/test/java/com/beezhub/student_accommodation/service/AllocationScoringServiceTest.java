package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.ScoredRoom;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.GenderRestriction;
import com.beezhub.student_accommodation.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllocationScoringServiceTest {

    @Mock
    private AllocationRuleRepository allocationRuleRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private RoomAttributesRepository roomAttributesRepository;
    @Mock
    private BuildingAttributesRepository buildingAttributesRepository;
    @Mock
    private RoomAllocationRepository roomAllocationRepository;

    @InjectMocks
    private LegacyScoringStrategy allocationScoringService;

    private Student student;
    private Application application;
    private Room room;
    private Building building;
    private AllocationRule allocationRule;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(1L);
        student.setGender("MALE");

        building = new Building();
        building.setId(1L);
        building.setName("Block A");

        room = new Room();
        room.setId(1L);
        room.setRoomNumber("101");
        room.setCapacity(2);
        room.setBuilding(building);

        application = new Application();
        application.setId(1L);
        application.setPreferredBuilding(building);

        allocationRule = new AllocationRule();
        allocationRule.setPreferenceMatchWeight(BigDecimal.ONE);
        allocationRule.setDesirabilityWeight(BigDecimal.ONE);
        allocationRule.setAccessibilityWeight(BigDecimal.ONE);
    }

    @Test
    void scoreAvailableRooms_shouldReturnScoredRooms() {
        when(roomRepository.findAll()).thenReturn(Collections.singletonList(room));
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L)).thenReturn(Collections.singletonList(allocationRule));
        when(roomAttributesRepository.findByRoom_IdIn(any())).thenReturn(Collections.emptyList());
        when(buildingAttributesRepository.findByBuilding_IdIn(any())).thenReturn(Collections.emptyList());
        when(roomAllocationRepository.hasStudentDeclinedRoom(anyLong(), anyLong())).thenReturn(false);

        List<ScoredRoom> scores = allocationScoringService.scoreAvailableRooms(student, application, null);

        assertThat(scores).isNotEmpty();
        assertThat(scores.get(0).getRoomId()).isEqualTo(1L);
        // Preference Match (100) * 1 + Desirability (50) * 1 + Accessibility (50) * 1 = 200
        assertThat(scores.get(0).getTotalScore()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void validateHardConstraints_shouldFail_whenGenderMismatch() {
        RoomAttributes roomAttrs = new RoomAttributes();
        roomAttrs.setGenderRestriction(GenderRestriction.FEMALE_ONLY);

        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.of(roomAttrs));

        boolean isValid = allocationScoringService.validateHardConstraints(student, room, false);

        assertThat(isValid).isFalse();
    }

    @Test
    void validateHardConstraints_shouldPass_whenGenderMatch() {
        RoomAttributes roomAttrs = new RoomAttributes();
        roomAttrs.setGenderRestriction(GenderRestriction.MALE_ONLY);

        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.of(roomAttrs));

        boolean isValid = allocationScoringService.validateHardConstraints(student, room, false);

        assertThat(isValid).isTrue();
    }

    @Test
    void validateHardConstraints_shouldFail_whenStudentHasDeclinedRoom() {
        // Given: Student has previously declined this room
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.empty());
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
        when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(true);

        // When: Validating hard constraints
        boolean isValid = allocationScoringService.validateHardConstraints(student, room, false);

        // Then: Should fail validation
        assertThat(isValid).isFalse();
    }

    @Test
    void validateHardConstraints_shouldPass_whenStudentHasNotDeclinedRoom() {
        // Given: Student has not declined this room
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.empty());
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
        when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

        // When: Validating hard constraints
        boolean isValid = allocationScoringService.validateHardConstraints(student, room, false);

        // Then: Should pass validation
        assertThat(isValid).isTrue();
    }

    @Test
    void scoreAvailableRooms_shouldExcludeDeclinedRooms() {
        // Given: Student has declined this room
        when(roomRepository.findAll()).thenReturn(Collections.singletonList(room));
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAttributesRepository.findByRoom_IdIn(any())).thenReturn(Collections.emptyList());
        when(buildingAttributesRepository.findByBuilding_IdIn(any())).thenReturn(Collections.emptyList());
        when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(true);

        // When: Scoring available rooms
        List<ScoredRoom> scores = allocationScoringService.scoreAvailableRooms(student, application, null);

        // Then: Declined room should not be in the list
        assertThat(scores).isEmpty();
    }

    // ==================== Preference Match Scoring Tests ====================

    @Test
    void scoreSingleRoom_preferredBuildingMatch_scores100Points() {
        // Given: Room is in the student's preferred building
        when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                .thenReturn(Collections.singletonList(allocationRule));
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.empty());
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

        // When: Scoring the room
        ScoredRoom scoredRoom = allocationScoringService.scoreSingleRoom(student, application, room);

        // Then: Preference match score should be 100 (PREFERRED_BUILDING_BONUS * weight)
        assertThat(scoredRoom.getPreferenceMatchScore()).isEqualByComparingTo(new BigDecimal("100.0000"));
    }

    @Test
    void scoreSingleRoom_noPreferredBuilding_scores50Points() {
        // Given: Application has no preferred building
        application.setPreferredBuilding(null);
        when(allocationRuleRepository.findByBuildingIsNullAndActiveTrue())
                .thenReturn(Collections.singletonList(allocationRule));
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.empty());
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

        // When: Scoring the room
        ScoredRoom scoredRoom = allocationScoringService.scoreSingleRoom(student, application, room);

        // Then: Preference match score should be 50 (ALTERNATIVE_BUILDING_BONUS * weight)
        assertThat(scoredRoom.getPreferenceMatchScore()).isEqualByComparingTo(new BigDecimal("50.0000"));
    }

    @Test
    void scoreSingleRoom_nonPreferredBuilding_scores50Points() {
        // Given: Room is NOT in the preferred building
        Building otherBuilding = new Building();
        otherBuilding.setId(2L);
        otherBuilding.setName("Block B");
        application.setPreferredBuilding(otherBuilding);

        when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                .thenReturn(Collections.singletonList(allocationRule));
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.empty());
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

        // When: Scoring the room
        ScoredRoom scoredRoom = allocationScoringService.scoreSingleRoom(student, application, room);

        // Then: Preference match score should be 50 (ALTERNATIVE_BUILDING_BONUS * weight)
        assertThat(scoredRoom.getPreferenceMatchScore()).isEqualByComparingTo(new BigDecimal("50.0000"));
    }

    // ==================== Desirability Scoring Tests ====================

    @Test
    void scoreSingleRoom_highDesirability_scores100Points() {
        // Given: Building has desirability rating of 10 (highest)
        BuildingAttributes buildingAttrs = new BuildingAttributes();
        buildingAttrs.setDesirabilityScore(10);

        when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                .thenReturn(Collections.singletonList(allocationRule));
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.empty());
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.of(buildingAttrs));
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

        // When: Scoring the room
        ScoredRoom scoredRoom = allocationScoringService.scoreSingleRoom(student, application, room);

        // Then: Desirability score should be 100 (10 * DESIRABILITY_MULTIPLIER * weight)
        assertThat(scoredRoom.getDesirabilityScore()).isEqualByComparingTo(new BigDecimal("100.0000"));
    }

    @Test
    void scoreSingleRoom_lowDesirability_scores10Points() {
        // Given: Building has desirability rating of 1 (lowest)
        BuildingAttributes buildingAttrs = new BuildingAttributes();
        buildingAttrs.setDesirabilityScore(1);

        when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                .thenReturn(Collections.singletonList(allocationRule));
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.empty());
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.of(buildingAttrs));
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

        // When: Scoring the room
        ScoredRoom scoredRoom = allocationScoringService.scoreSingleRoom(student, application, room);

        // Then: Desirability score should be 10 (1 * DESIRABILITY_MULTIPLIER * weight)
        assertThat(scoredRoom.getDesirabilityScore()).isEqualByComparingTo(new BigDecimal("10.0000"));
    }

    @Test
    void scoreSingleRoom_noDesirabilityRating_usesDefault50Points() {
        // Given: Building has no desirability rating
        when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                .thenReturn(Collections.singletonList(allocationRule));
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.empty());
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

        // When: Scoring the room
        ScoredRoom scoredRoom = allocationScoringService.scoreSingleRoom(student, application, room);

        // Then: Desirability score should be 50 (default 5 * DESIRABILITY_MULTIPLIER * weight)
        assertThat(scoredRoom.getDesirabilityScore()).isEqualByComparingTo(new BigDecimal("50.0000"));
    }

    // ==================== Accessibility Scoring Tests ====================

    @Test
    void scoreSingleRoom_accessibilityRequiredAndMatched_scores100Points() {
        // Given: Student requires accessibility and room is accessible
        application.setRequiresAccessibility(true);
        RoomAttributes roomAttrs = new RoomAttributes();
        roomAttrs.setAccessibilityCompliant(true);

        when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                .thenReturn(Collections.singletonList(allocationRule));
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.of(roomAttrs));
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

        // When: Scoring the room
        ScoredRoom scoredRoom = allocationScoringService.scoreSingleRoom(student, application, room);

        // Then: Accessibility score should be 100 (ACCESSIBILITY_MATCH_BONUS * weight)
        assertThat(scoredRoom.getAccessibilityScore()).isEqualByComparingTo(new BigDecimal("100.0000"));
    }

    @Test
    void scoreSingleRoom_accessibilityRequiredButNotMatched_scores0Points() {
        // Given: Student requires accessibility but room is not accessible
        application.setRequiresAccessibility(true);
        RoomAttributes roomAttrs = new RoomAttributes();
        roomAttrs.setAccessibilityCompliant(false);

        when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                .thenReturn(Collections.singletonList(allocationRule));
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.of(roomAttrs));
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

        // When: Scoring the room
        ScoredRoom scoredRoom = allocationScoringService.scoreSingleRoom(student, application, room);

        // Then: Accessibility score should be 0
        assertThat(scoredRoom.getAccessibilityScore()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void scoreSingleRoom_accessibilityNotRequiredButAvailable_scores25Points() {
        // Given: Student does not require accessibility but room is accessible
        application.setRequiresAccessibility(false);
        RoomAttributes roomAttrs = new RoomAttributes();
        roomAttrs.setAccessibilityCompliant(true);

        when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                .thenReturn(Collections.singletonList(allocationRule));
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.of(roomAttrs));
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

        // When: Scoring the room
        ScoredRoom scoredRoom = allocationScoringService.scoreSingleRoom(student, application, room);

        // Then: Accessibility score should be 25 (flexibility bonus * weight)
        assertThat(scoredRoom.getAccessibilityScore()).isEqualByComparingTo(new BigDecimal("25.0000"));
    }

    @Test
    void scoreSingleRoom_noAccessibilityRequirementOrCompliance_scores50Points() {
        // Given: No accessibility requirements or compliance
        application.setRequiresAccessibility(false);
        RoomAttributes roomAttrs = new RoomAttributes();
        roomAttrs.setAccessibilityCompliant(false);

        when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                .thenReturn(Collections.singletonList(allocationRule));
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.of(roomAttrs));
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

        // When: Scoring the room
        ScoredRoom scoredRoom = allocationScoringService.scoreSingleRoom(student, application, room);

        // Then: Accessibility score should be 50
        assertThat(scoredRoom.getAccessibilityScore()).isEqualByComparingTo(new BigDecimal("50.0000"));
    }

    // ==================== Combined Scoring & Perfect Match Tests ====================

    @Test
    void scoreSingleRoom_perfectMatch_returnsHighScore() {
        // Given: All preferences match perfectly
        application.setRequiresAccessibility(true);
        BuildingAttributes buildingAttrs = new BuildingAttributes();
        buildingAttrs.setDesirabilityScore(10);
        RoomAttributes roomAttrs = new RoomAttributes();
        roomAttrs.setAccessibilityCompliant(true);

        when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                .thenReturn(Collections.singletonList(allocationRule));
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.of(roomAttrs));
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.of(buildingAttrs));
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

        // When: Scoring the room
        ScoredRoom scoredRoom = allocationScoringService.scoreSingleRoom(student, application, room);

        // Then: Total score should be 300 (100 + 100 + 100 with default weights of 1)
        assertThat(scoredRoom.getTotalScore()).isEqualByComparingTo(new BigDecimal("300.0000"));
    }

    @Test
    void scoreSingleRoom_noMatch_returnsMinimumScore() {
        // Given: No preferences match
        Building otherBuilding = new Building();
        otherBuilding.setId(2L);
        application.setPreferredBuilding(otherBuilding);
        application.setRequiresAccessibility(true);

        BuildingAttributes buildingAttrs = new BuildingAttributes();
        buildingAttrs.setDesirabilityScore(1);
        RoomAttributes roomAttrs = new RoomAttributes();
        roomAttrs.setAccessibilityCompliant(false);

        when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                .thenReturn(Collections.singletonList(allocationRule));
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.of(roomAttrs));
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.of(buildingAttrs));
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

        // When: Scoring the room
        ScoredRoom scoredRoom = allocationScoringService.scoreSingleRoom(student, application, room);

        // Then: Total score should be low (50 + 10 + 0 = 60)
        assertThat(scoredRoom.getTotalScore()).isEqualByComparingTo(new BigDecimal("60.0000"));
    }

    // ==================== Custom Weights Tests ====================

    @Test
    void scoreSingleRoom_customWeightsFromRules_appliedCorrectly() {
        // Given: Custom allocation rule with different weights
        AllocationRule customRule = new AllocationRule();
        customRule.setPreferenceMatchWeight(new BigDecimal("0.4"));
        customRule.setDesirabilityWeight(new BigDecimal("0.3"));
        customRule.setAccessibilityWeight(new BigDecimal("0.3"));

        when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                .thenReturn(Collections.singletonList(customRule));
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.empty());
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

        // When: Scoring the room
        ScoredRoom scoredRoom = allocationScoringService.scoreSingleRoom(student, application, room);

        // Then: Scores should be weighted (100*0.4 + 50*0.3 + 50*0.3 = 40 + 15 + 15 = 70)
        assertThat(scoredRoom.getTotalScore()).isEqualByComparingTo(new BigDecimal("70.0000"));
    }

    @Test
    void scoreSingleRoom_zeroWeights_returnsZero() {
        // Given: All weights are zero
        AllocationRule zeroWeightRule = new AllocationRule();
        zeroWeightRule.setPreferenceMatchWeight(BigDecimal.ZERO);
        zeroWeightRule.setDesirabilityWeight(BigDecimal.ZERO);
        zeroWeightRule.setAccessibilityWeight(BigDecimal.ZERO);

        when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                .thenReturn(Collections.singletonList(zeroWeightRule));
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.empty());
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

        // When: Scoring the room
        ScoredRoom scoredRoom = allocationScoringService.scoreSingleRoom(student, application, room);

        // Then: Total score should be zero
        assertThat(scoredRoom.getTotalScore()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ==================== Tie-Breaking Logic Tests ====================

    @Test
    void scoreAvailableRooms_tiedScores_earlierApplicationWins() {
        // Given: Two rooms with identical scores, different application timestamps
        Room room2 = new Room();
        room2.setId(2L);
        room2.setRoomNumber("102");
        room2.setCapacity(2);
        room2.setBuilding(building);

        Application earlierApp = new Application();
        earlierApp.setId(1L);
        earlierApp.setPreferredBuilding(building);
        earlierApp.setCreatedAt(LocalDateTime.now().minusHours(2));

        when(roomRepository.findAll()).thenReturn(Arrays.asList(room, room2));
        when(roomAllocationRepository.countActiveAllocationsByRoomId(anyLong())).thenReturn(0L);
        when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                .thenReturn(Collections.singletonList(allocationRule));
        when(roomAttributesRepository.findByRoom_IdIn(any())).thenReturn(Collections.emptyList());
        when(buildingAttributesRepository.findByBuilding_IdIn(any())).thenReturn(Collections.emptyList());
        when(roomAllocationRepository.hasStudentDeclinedRoom(anyLong(), anyLong())).thenReturn(false);

        // When: Scoring available rooms
        List<ScoredRoom> scores = allocationScoringService.scoreAvailableRooms(student, earlierApp, null);

        // Then: Rooms should be sorted by score DESC, then by createdAt ASC
        assertThat(scores).hasSize(2);
        assertThat(scores.get(0).getTotalScore()).isEqualByComparingTo(scores.get(1).getTotalScore());
        assertThat(scores.get(0).getApplicationCreatedAt()).isEqualTo(earlierApp.getCreatedAt());
    }

    // ==================== Edge Cases & Null Handling Tests ====================

    @Test
    void scoreSingleRoom_nullApplication_handledGracefully() {
        // Given: Null application
        when(allocationRuleRepository.findByBuildingIsNullAndActiveTrue())
                .thenReturn(Collections.singletonList(allocationRule));
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.empty());
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

        // When: Scoring the room with null application
        ScoredRoom scoredRoom = allocationScoringService.scoreSingleRoom(student, null, room);

        // Then: Should return a score (default values used)
        assertThat(scoredRoom).isNotNull();
        assertThat(scoredRoom.getTotalScore()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void scoreSingleRoom_nullPreferences_handledGracefully() {
        // Given: Application with null preferences
        application.setPreferredBuilding(null);
        application.setRequiresAccessibility(null);

        when(allocationRuleRepository.findByBuildingIsNullAndActiveTrue())
                .thenReturn(Collections.singletonList(allocationRule));
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.empty());
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

        // When: Scoring the room
        ScoredRoom scoredRoom = allocationScoringService.scoreSingleRoom(student, application, room);

        // Then: Should return a score without throwing exceptions
        assertThat(scoredRoom).isNotNull();
        assertThat(scoredRoom.getTotalScore()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void scoreAvailableRooms_emptyRoomList_returnsEmptyList() {
        // Given: No available rooms
        when(roomRepository.findAll()).thenReturn(Collections.emptyList());

        // When: Scoring available rooms
        List<ScoredRoom> scores = allocationScoringService.scoreAvailableRooms(student, application, null);

        // Then: Should return empty list
        assertThat(scores).isEmpty();
    }

    @Test
    void scoreAvailableRooms_allRoomsFull_returnsEmptyList() {
        // Given: Room is at full capacity
        when(roomRepository.findAll()).thenReturn(Collections.singletonList(room));
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(2L); // Capacity is 2

        // When: Scoring available rooms
        List<ScoredRoom> scores = allocationScoringService.scoreAvailableRooms(student, application, null);

        // Then: Should return empty list (room filtered out)
        assertThat(scores).isEmpty();
    }

    // ==================== Hard Constraints Tests ====================

    @Test
    void validateHardConstraints_accessibilityRequired_failsWhenRoomNotAccessible() {
        // Given: Student requires accessibility, room is not accessible
        application.setRequiresAccessibility(true);
        student.setSpecialRequirements("Requires wheelchair accessible room");

        RoomAttributes roomAttrs = new RoomAttributes();
        roomAttrs.setAccessibilityCompliant(false);

        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.of(roomAttrs));
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());

        // When: Validating hard constraints
        boolean isValid = allocationScoringService.validateHardConstraints(student, room, false);

        // Then: Should fail validation
        assertThat(isValid).isFalse();
    }

    @Test
    void validateHardConstraints_accessibilityRequired_passesWhenRoomAccessible() {
        // Given: Student requires accessibility, room is accessible
        application.setRequiresAccessibility(true);
        RoomAttributes roomAttrs = new RoomAttributes();
        roomAttrs.setAccessibilityCompliant(true);

        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.of(roomAttrs));
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
        when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

        // When: Validating hard constraints
        boolean isValid = allocationScoringService.validateHardConstraints(student, room, false);

        // Then: Should pass validation
        assertThat(isValid).isTrue();
    }

    @Test
    void validateHardConstraints_allowOverride_alwaysReturnsTrue() {
        // Given: Override is allowed (no mocking needed as all checks are bypassed)

        // When: Validating hard constraints with override
        boolean isValid = allocationScoringService.validateHardConstraints(student, room, true);

        // Then: Should pass validation (override bypasses all checks)
        assertThat(isValid).isTrue();
    }

    @Test
    void validateHardConstraints_yearOfStudyMismatch_fails() {
        // Given: Room requires year 2-4, student is year 1
        YearOfStudy yearOne = new YearOfStudy();
        yearOne.setYearOfStudy("1");
        student.setYearOfStudy(yearOne);

        RoomAttributes roomAttrs = new RoomAttributes();
        roomAttrs.setMinYearOfStudy(2);
        roomAttrs.setMaxYearOfStudy(4);

        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.of(roomAttrs));
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());

        // When: Validating hard constraints
        boolean isValid = allocationScoringService.validateHardConstraints(student, room, false);

        // Then: Should fail validation
        assertThat(isValid).isFalse();
    }

    @Test
    void validateHardConstraints_yearOfStudyMatch_passes() {
        // Given: Room requires year 1-3, student is year 2
        YearOfStudy yearTwo = new YearOfStudy();
        yearTwo.setYearOfStudy("2");
        student.setYearOfStudy(yearTwo);

        RoomAttributes roomAttrs = new RoomAttributes();
        roomAttrs.setMinYearOfStudy(1);
        roomAttrs.setMaxYearOfStudy(3);

        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.of(roomAttrs));
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
        when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

        // When: Validating hard constraints
        boolean isValid = allocationScoringService.validateHardConstraints(student, room, false);

        // Then: Should pass validation
        assertThat(isValid).isTrue();
    }

    @Test
    void validateHardConstraints_institutionMismatch_fails() {
        // Given: Student's institution not served by building
        Institution studentInst = new Institution();
        studentInst.setId(1L);
        student.setInstitution(studentInst);

        Institution buildingInst = new Institution();
        buildingInst.setId(2L);
        building.setInstitutions(new HashSet<>(Arrays.asList(buildingInst)));

        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.empty());
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());

        // When: Validating hard constraints
        boolean isValid = allocationScoringService.validateHardConstraints(student, room, false);

        // Then: Should fail validation
        assertThat(isValid).isFalse();
    }

    @Test
    void validateHardConstraints_institutionMatch_passes() {
        // Given: Student's institution served by building
        Institution institution = new Institution();
        institution.setId(1L);
        student.setInstitution(institution);
        building.setInstitutions(new HashSet<>(Arrays.asList(institution)));

        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.empty());
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
        when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

        // When: Validating hard constraints
        boolean isValid = allocationScoringService.validateHardConstraints(student, room, false);

        // Then: Should pass validation
        assertThat(isValid).isTrue();
    }

    // ==================== Allocation Rule Tests ====================

    @Test
    void getLatestRuleForBuilding_buildingSpecificRuleExists_returnsBuildingRule() {
        // Given: Building-specific rule exists
        AllocationRule buildingRule = new AllocationRule();
        buildingRule.setRuleName("Building Specific");
        buildingRule.setPriority(10);

        when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                .thenReturn(Collections.singletonList(buildingRule));

        // When: Getting rule for building
        AllocationRule rule = allocationScoringService.getLatestRuleForBuilding(1L);

        // Then: Should return building-specific rule
        assertThat(rule.getRuleName()).isEqualTo("Building Specific");
    }

    @Test
    void getLatestRuleForBuilding_noBuildingRule_returnsGlobalRule() {
        // Given: No building-specific rule, global rule exists
        AllocationRule globalRule = new AllocationRule();
        globalRule.setRuleName("Global Rule");
        globalRule.setPriority(5);

        when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                .thenReturn(Collections.emptyList());
        when(allocationRuleRepository.findByBuildingIsNullAndActiveTrue())
                .thenReturn(Collections.singletonList(globalRule));

        // When: Getting rule for building
        AllocationRule rule = allocationScoringService.getLatestRuleForBuilding(1L);

        // Then: Should return global rule
        assertThat(rule.getRuleName()).isEqualTo("Global Rule");
    }

    @Test
    void getLatestRuleForBuilding_noRules_returnsDefaultRule() {
        // Given: No rules exist
        when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                .thenReturn(Collections.emptyList());
        when(allocationRuleRepository.findByBuildingIsNullAndActiveTrue())
                .thenReturn(Collections.emptyList());

        // When: Getting rule for building
        AllocationRule rule = allocationScoringService.getLatestRuleForBuilding(1L);

        // Then: Should return default rule
        assertThat(rule.getRuleName()).isEqualTo("Default");
        assertThat(rule.getPreferenceMatchWeight()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(rule.getDesirabilityWeight()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(rule.getAccessibilityWeight()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void getLatestRuleForBuilding_multipleRules_returnsHighestPriority() {
        // Given: Multiple rules with different priorities
        AllocationRule lowPriorityRule = new AllocationRule();
        lowPriorityRule.setRuleName("Low Priority");
        lowPriorityRule.setPriority(5);

        AllocationRule highPriorityRule = new AllocationRule();
        highPriorityRule.setRuleName("High Priority");
        highPriorityRule.setPriority(10);

        when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                .thenReturn(Arrays.asList(lowPriorityRule, highPriorityRule));

        // When: Getting rule for building
        AllocationRule rule = allocationScoringService.getLatestRuleForBuilding(1L);

        // Then: Should return highest priority rule
        assertThat(rule.getRuleName()).isEqualTo("High Priority");
        assertThat(rule.getPriority()).isEqualTo(10);
    }

    // ==================== Eligible Rooms Tests ====================

    @Test
    void getEligibleRoomsForStudent_filtersBasedOnHardConstraints() {
        // Given: Multiple rooms, one fails hard constraints
        Room eligibleRoom = new Room();
        eligibleRoom.setId(2L);
        eligibleRoom.setRoomNumber("102");
        eligibleRoom.setCapacity(2);
        eligibleRoom.setBuilding(building);

        RoomAttributes room1Attrs = new RoomAttributes();
        room1Attrs.setGenderRestriction(GenderRestriction.FEMALE_ONLY);

        when(roomRepository.findAll()).thenReturn(Arrays.asList(room, eligibleRoom));
        when(roomAllocationRepository.countActiveAllocationsByRoomId(anyLong())).thenReturn(0L);
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.of(room1Attrs));
        when(roomAttributesRepository.findByRoom_Id(2L)).thenReturn(Optional.empty());
        when(buildingAttributesRepository.findByBuilding_Id(anyLong())).thenReturn(Optional.empty());
        when(roomAllocationRepository.hasStudentDeclinedRoom(anyLong(), anyLong())).thenReturn(false);

        // When: Getting eligible rooms
        List<Room> eligibleRooms = allocationScoringService.getEligibleRoomsForStudent(student, null);

        // Then: Should return only room that passes hard constraints
        assertThat(eligibleRooms).hasSize(1);
        assertThat(eligibleRooms.get(0).getId()).isEqualTo(2L);
    }

    @Test
    void getEligibleRoomsForStudent_filtersByBuilding_whenSpecified() {
        // Given: Specific building filter
        Room room2 = new Room();
        room2.setId(2L);
        room2.setRoomNumber("102");
        room2.setCapacity(2);
        room2.setBuilding(building);

        when(roomRepository.findByBuildingId(1L)).thenReturn(Arrays.asList(room, room2));
        when(roomAllocationRepository.countActiveAllocationsByRoomId(anyLong())).thenReturn(0L);
        when(roomAttributesRepository.findByRoom_Id(anyLong())).thenReturn(Optional.empty());
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
        when(roomAllocationRepository.hasStudentDeclinedRoom(anyLong(), anyLong())).thenReturn(false);

        // When: Getting eligible rooms for specific building
        List<Room> eligibleRooms = allocationScoringService.getEligibleRoomsForStudent(student, building);

        // Then: Should return only rooms from that building
        assertThat(eligibleRooms).hasSize(2);
        assertThat(eligibleRooms).allMatch(r -> r.getBuilding().getId().equals(1L));
    }

    @Test
    void getEligibleRoomsForStudentByInstitution_filtersBasedOnInstitution() {
        // Given: Student with institution, buildings with institution restrictions
        Institution studentInst = new Institution();
        studentInst.setId(1L);
        student.setInstitution(studentInst);
        building.setInstitutions(new HashSet<>(Arrays.asList(studentInst)));

        when(roomRepository.findAll()).thenReturn(Collections.singletonList(room));
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.empty());
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
        when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

        // When: Getting eligible rooms filtered by institution
        List<Room> eligibleRooms = allocationScoringService.getEligibleRoomsForStudentByInstitution(student);

        // Then: Should return rooms from buildings serving student's institution
        assertThat(eligibleRooms).hasSize(1);
    }

    // ==================== Warning Generation Tests ====================

    @Test
    void generateWarnings_buildingMismatch_addsBuildingMismatchWarning() {
        // Given: Room not in preferred building
        Building otherBuilding = new Building();
        otherBuilding.setId(2L);
        application.setPreferredBuilding(otherBuilding);

        // Mock for scoreSingleRoomWithoutWarnings call inside generateWarnings
        when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                .thenReturn(Collections.singletonList(allocationRule));
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.empty());
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());

        // When: Generating warnings
        List<String> warnings = allocationScoringService.generateWarnings(student, application, room, allocationRule);

        // Then: Should include BUILDING_MISMATCH warning
        assertThat(warnings).contains("BUILDING_MISMATCH");
    }

    @Test
    void generateWarnings_suboptimalDesirability_addsSuboptimalWarning() {
        // Given: Low desirability score
        BuildingAttributes buildingAttrs = new BuildingAttributes();
        buildingAttrs.setDesirabilityScore(1); // Very low

        AllocationRule lowWeightRule = new AllocationRule();
        lowWeightRule.setPreferenceMatchWeight(new BigDecimal("0.1"));
        lowWeightRule.setDesirabilityWeight(new BigDecimal("0.1"));
        lowWeightRule.setAccessibilityWeight(new BigDecimal("0.1"));

        // Mock for scoreSingleRoomWithoutWarnings call inside generateWarnings
        when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                .thenReturn(Collections.singletonList(lowWeightRule));
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.empty());
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.of(buildingAttrs));

        // When: Generating warnings
        List<String> warnings = allocationScoringService.generateWarnings(student, application, room, lowWeightRule);

        // Then: Should include SUBOPTIMAL_DESIRABILITY warning (score < 50)
        assertThat(warnings).contains("SUBOPTIMAL_DESIRABILITY");
    }

    @Test
    void generateWarnings_accessibilityMismatch_addsAccessibilityWarning() {
        // Given: Student requires accessibility, room is not accessible
        application.setRequiresAccessibility(true);
        RoomAttributes roomAttrs = new RoomAttributes();
        roomAttrs.setAccessibilityCompliant(false);

        // Mock for scoreSingleRoomWithoutWarnings call inside generateWarnings
        when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                .thenReturn(Collections.singletonList(allocationRule));
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.of(roomAttrs));
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());

        // When: Generating warnings
        List<String> warnings = allocationScoringService.generateWarnings(student, application, room, allocationRule);

        // Then: Should include ACCESSIBILITY_MISMATCH warning
        assertThat(warnings).contains("ACCESSIBILITY_MISMATCH");
    }
}
