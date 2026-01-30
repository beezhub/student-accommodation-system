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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    private AllocationScoringService allocationScoringService;

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
        when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.empty());
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());

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
}
