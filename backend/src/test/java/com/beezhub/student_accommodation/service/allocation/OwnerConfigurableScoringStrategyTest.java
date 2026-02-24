package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.dto.ScoredRoom;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.GenderRestriction;
import com.beezhub.student_accommodation.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerConfigurableScoringStrategyTest {

    @Mock private AllocationRuleRepository      allocationRuleRepository;
    @Mock private RoomRepository                roomRepository;
    @Mock private RoomAttributesRepository      roomAttributesRepository;
    @Mock private BuildingAttributesRepository  buildingAttributesRepository;
    @Mock private RoomAllocationRepository      roomAllocationRepository;

    @InjectMocks
    private OwnerConfigurableScoringStrategy strategy;

    // ---- shared fixtures ----
    private Student     student;
    private Application application;
    private Room        room;
    private Building    building;
    private AllocationRule defaultRule;

    @BeforeEach
    void setUp() {
        building = new Building();
        building.setId(1L);
        building.setName("Block A");

        room = new Room();
        room.setId(1L);
        room.setRoomNumber("101");
        room.setCapacity(2);
        room.setBuilding(building);

        student = new Student();
        student.setId(1L);
        student.setGender("MALE");

        application = new Application();
        application.setId(1L);
        application.setPreferredBuilding(building);
        application.setCreatedAt(LocalDateTime.of(2026, 1, 10, 9, 0));

        defaultRule = AllocationRule.builder()
                .ruleName("Default")
                .preferenceMatchWeight(BigDecimal.ONE)
                .desirabilityWeight(BigDecimal.ONE)
                .accessibilityWeight(BigDecimal.ONE)
                .fcfsWeight(BigDecimal.ONE)
                .nsfasPriorityWeight(BigDecimal.ONE)
                .universityPrefWeight(BigDecimal.ONE)
                .active(true)
                .priority(0)
                .build();
    }

    // ====================================================================
    // satisfiesHardConstraints
    // ====================================================================

    @Nested
    class SatisfiesHardConstraints {

        @Test
        void satisfiesHardConstraints_genderMismatch_returnsFalse() {
            // student is MALE, room is FEMALE_ONLY
            RoomAttributes attrs = new RoomAttributes();
            attrs.setGenderRestriction(GenderRestriction.FEMALE_ONLY);

            when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.of(attrs));
            when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());

            assertThat(strategy.satisfiesHardConstraints(student, room)).isFalse();
        }

        @Test
        void satisfiesHardConstraints_genderMatch_returnsTrue() {
            RoomAttributes attrs = new RoomAttributes();
            attrs.setGenderRestriction(GenderRestriction.MALE_ONLY);

            when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.of(attrs));
            when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
            when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

            assertThat(strategy.satisfiesHardConstraints(student, room)).isTrue();
        }

        @Test
        void satisfiesHardConstraints_noGenderRestriction_returnsTrue() {
            when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.empty());
            when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
            when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

            assertThat(strategy.satisfiesHardConstraints(student, room)).isTrue();
        }

        @Test
        void satisfiesHardConstraints_accessibilityRequired_roomNotCompliant_returnsFalse() {
            student.setSpecialRequirements("requires wheelchair access");

            RoomAttributes attrs = new RoomAttributes();
            attrs.setAccessibilityCompliant(false);

            when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.of(attrs));
            when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());

            assertThat(strategy.satisfiesHardConstraints(student, room)).isFalse();
        }

        @Test
        void satisfiesHardConstraints_accessibilityRequired_roomCompliant_returnsTrue() {
            student.setSpecialRequirements("wheelchair user");

            RoomAttributes attrs = new RoomAttributes();
            attrs.setGenderRestriction(GenderRestriction.ANY);
            attrs.setAccessibilityCompliant(true);

            when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.of(attrs));
            when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
            when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

            assertThat(strategy.satisfiesHardConstraints(student, room)).isTrue();
        }

        @Test
        void satisfiesHardConstraints_priorDecline_returnsFalse() {
            when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.empty());
            when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
            when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(true);

            assertThat(strategy.satisfiesHardConstraints(student, room)).isFalse();
        }

        @Test
        void satisfiesHardConstraints_studentHasNoGender_returnsTrue() {
            student.setGender(null);

            when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.empty());
            when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
            when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(false);

            assertThat(strategy.satisfiesHardConstraints(student, room)).isTrue();
        }
    }

    // ====================================================================
    // calculateNsfasScore
    // ====================================================================

    @Nested
    class NsfasScore {

        @Test
        void calculateNsfasScore_applicationMentionsNsfas_returnsBonus() {
            application.setSpecialRequirements("NSFAS funded student");
            assertThat(strategy.calculateNsfasScore(student, application))
                    .isEqualByComparingTo(OwnerConfigurableScoringStrategy.NSFAS_PRIORITY_BONUS);
        }

        @Test
        void calculateNsfasScore_applicationMentionsFinancialAid_returnsBonus() {
            application.setSpecialRequirements("financial aid recipient");
            assertThat(strategy.calculateNsfasScore(student, application))
                    .isEqualByComparingTo(OwnerConfigurableScoringStrategy.NSFAS_PRIORITY_BONUS);
        }

        @Test
        void calculateNsfasScore_studentProfileMentionsNsfas_returnsBonus() {
            student.setSpecialRequirements("nsfas bursary holder");
            assertThat(strategy.calculateNsfasScore(student, application))
                    .isEqualByComparingTo(OwnerConfigurableScoringStrategy.NSFAS_PRIORITY_BONUS);
        }

        @Test
        void calculateNsfasScore_noNsfas_returnsZero() {
            assertThat(strategy.calculateNsfasScore(student, application))
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void calculateNsfasScore_nullApplication_checksFallback() {
            student.setSpecialRequirements(null);
            assertThat(strategy.calculateNsfasScore(student, null))
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ====================================================================
    // calculateUniversityPrefScore
    // ====================================================================

    @Nested
    class UniversityPrefScore {

        @Test
        void calculateUniversityPrefScore_institutionInPriorityList_returnsBonus() {
            Institution inst = new Institution();
            inst.setId(42L);
            student.setInstitution(inst);

            AllocationRule rule = AllocationRule.builder()
                    .priorityInstitutionIds("10, 42, 99")
                    .universityPrefWeight(BigDecimal.ONE)
                    .build();

            assertThat(strategy.calculateUniversityPrefScore(student, rule))
                    .isEqualByComparingTo(OwnerConfigurableScoringStrategy.UNIVERSITY_PREF_BONUS);
        }

        @Test
        void calculateUniversityPrefScore_institutionNotInList_returnsZero() {
            Institution inst = new Institution();
            inst.setId(7L);
            student.setInstitution(inst);

            AllocationRule rule = AllocationRule.builder()
                    .priorityInstitutionIds("10, 42, 99")
                    .universityPrefWeight(BigDecimal.ONE)
                    .build();

            assertThat(strategy.calculateUniversityPrefScore(student, rule))
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void calculateUniversityPrefScore_noPriorityIds_returnsZero() {
            AllocationRule rule = AllocationRule.builder()
                    .priorityInstitutionIds(null)
                    .universityPrefWeight(BigDecimal.ONE)
                    .build();

            assertThat(strategy.calculateUniversityPrefScore(student, rule))
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void calculateUniversityPrefScore_studentHasNoInstitution_returnsZero() {
            student.setInstitution(null);

            AllocationRule rule = AllocationRule.builder()
                    .priorityInstitutionIds("10, 42")
                    .universityPrefWeight(BigDecimal.ONE)
                    .build();

            assertThat(strategy.calculateUniversityPrefScore(student, rule))
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ====================================================================
    // scoreAvailableRooms — integration of all components
    // ====================================================================

    @Nested
    class ScoreAvailableRooms {

        /** Shared setup: room has capacity, no declines, no attributes */
        private void stubAvailableRoom() {
            when(roomRepository.findAll()).thenReturn(List.of(room));
            when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
            when(roomAttributesRepository.findByRoom_IdIn(any())).thenReturn(Collections.emptyList());
            when(buildingAttributesRepository.findByBuilding_IdIn(any())).thenReturn(Collections.emptyList());
            when(roomAllocationRepository.hasStudentDeclinedRoom(anyLong(), anyLong())).thenReturn(false);
            when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                    .thenReturn(List.of(defaultRule));
        }

        @Test
        void scoreAvailableRooms_returnsNonEmptyList() {
            stubAvailableRoom();
            List<ScoredRoom> result = strategy.scoreAvailableRooms(student, application, null);
            assertThat(result).isNotEmpty();
            assertThat(result.get(0).getRoomId()).isEqualTo(1L);
        }

        @Test
        void scoreAvailableRooms_excludesDeclinedRoom() {
            when(roomRepository.findAll()).thenReturn(List.of(room));
            when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
            when(roomAttributesRepository.findByRoom_IdIn(any())).thenReturn(Collections.emptyList());
            when(buildingAttributesRepository.findByBuilding_IdIn(any())).thenReturn(Collections.emptyList());
            when(roomAllocationRepository.hasStudentDeclinedRoom(1L, 1L)).thenReturn(true);

            List<ScoredRoom> result = strategy.scoreAvailableRooms(student, application, null);
            assertThat(result).isEmpty();
        }

        @Test
        void scoreAvailableRooms_excludesFullRoom() {
            when(roomRepository.findAll()).thenReturn(List.of(room)); // capacity = 2
            when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(2L); // full

            List<ScoredRoom> result = strategy.scoreAvailableRooms(student, application, null);
            assertThat(result).isEmpty();
        }

        /**
         * FCFS-only: when NSFAS and university-pref weights are zero,
         * earlier application wins (higher FCFS score).
         *
         * Two rooms in the same building — only FCFS distinguishes them when the
         * student's application has a fixed createdAt. In this test both rooms
         * get the same FCFS score (same application) so we verify total score > 0.
         */
        @Test
        void scoreAllocation_FCFSOnly_applicationScoresPositive() {
            AllocationRule fcfsOnlyRule = AllocationRule.builder()
                    .ruleName("FCFS-only")
                    .fcfsWeight(BigDecimal.ONE)
                    .nsfasPriorityWeight(BigDecimal.ZERO)
                    .universityPrefWeight(BigDecimal.ZERO)
                    .accessibilityWeight(BigDecimal.ZERO)
                    .preferenceMatchWeight(BigDecimal.ZERO)
                    .desirabilityWeight(BigDecimal.ZERO)
                    .active(true).priority(0).build();

            when(roomRepository.findAll()).thenReturn(List.of(room));
            when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
            when(roomAttributesRepository.findByRoom_IdIn(any())).thenReturn(Collections.emptyList());
            when(buildingAttributesRepository.findByBuilding_IdIn(any())).thenReturn(Collections.emptyList());
            when(roomAllocationRepository.hasStudentDeclinedRoom(anyLong(), anyLong())).thenReturn(false);
            when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                    .thenReturn(List.of(fcfsOnlyRule));

            List<ScoredRoom> result = strategy.scoreAvailableRooms(student, application, null);

            assertThat(result).hasSize(1);
            // FCFS score 100 * weight 1 = 100
            assertThat(result.get(0).getTotalScore())
                    .isEqualByComparingTo(new BigDecimal("100.0000"));
        }

        /**
         * NSFAS priority: NSFAS student scores higher than non-NSFAS with equal FCFS.
         * We use scoreSingleRoom to compare directly.
         */
        @Test
        void scoreAllocation_NSFASPriority_NSFASStudentScoresHigher() {
            AllocationRule nsfasRule = AllocationRule.builder()
                    .ruleName("NSFAS-priority")
                    .fcfsWeight(BigDecimal.ONE)
                    .nsfasPriorityWeight(new BigDecimal("2"))
                    .universityPrefWeight(BigDecimal.ZERO)
                    .accessibilityWeight(BigDecimal.ZERO)
                    .preferenceMatchWeight(BigDecimal.ZERO)
                    .desirabilityWeight(BigDecimal.ZERO)
                    .active(true).priority(0).build();

            // NSFAS student
            Student nsfasStudent = new Student();
            nsfasStudent.setId(10L);
            nsfasStudent.setGender("MALE");
            nsfasStudent.setSpecialRequirements("nsfas funded");

            Application nsfasApp = new Application();
            nsfasApp.setId(10L);
            nsfasApp.setCreatedAt(LocalDateTime.of(2026, 1, 10, 9, 0));

            // Non-NSFAS student
            Student regularStudent = new Student();
            regularStudent.setId(20L);
            regularStudent.setGender("MALE");

            Application regularApp = new Application();
            regularApp.setId(20L);
            regularApp.setCreatedAt(LocalDateTime.of(2026, 1, 10, 9, 0)); // same time

            when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.empty());
            when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
            when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);

            ScoredRoom nsfasScore   = strategy.scoreSingleRoom(nsfasStudent,   nsfasApp,   room, nsfasRule);
            ScoredRoom regularScore = strategy.scoreSingleRoom(regularStudent, regularApp, room, nsfasRule);

            assertThat(nsfasScore.getTotalScore())
                    .isGreaterThan(regularScore.getTotalScore());
        }

        @Test
        void scoreAvailableRooms_sortedDescByTotalScore() {
            // Two rooms — room2 gets higher NSFAS bonus
            Building building2 = new Building();
            building2.setId(2L);
            building2.setName("Block B");

            Room room2 = new Room();
            room2.setId(2L);
            room2.setRoomNumber("201");
            room2.setCapacity(2);
            room2.setBuilding(building2);

            student.setSpecialRequirements("nsfas funded"); // NSFAS bonus applies to all rooms

            when(roomRepository.findAll()).thenReturn(List.of(room, room2));
            when(roomAllocationRepository.countActiveAllocationsByRoomId(anyLong())).thenReturn(0L);
            when(roomAttributesRepository.findByRoom_IdIn(any())).thenReturn(Collections.emptyList());
            when(buildingAttributesRepository.findByBuilding_IdIn(any())).thenReturn(Collections.emptyList());
            when(roomAllocationRepository.hasStudentDeclinedRoom(anyLong(), anyLong())).thenReturn(false);
            when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(anyLong()))
                    .thenReturn(List.of(defaultRule));

            List<ScoredRoom> result = strategy.scoreAvailableRooms(student, application, null);

            assertThat(result).hasSize(2);
            // Scores should be descending
            assertThat(result.get(0).getTotalScore())
                    .isGreaterThanOrEqualTo(result.get(1).getTotalScore());
        }
    }

    // ====================================================================
    // scoreSingleRoom (interface method)
    // ====================================================================

    @Nested
    class ScoreSingleRoom {

        @Test
        void scoreSingleRoom_noSpecialConditions_returnsPositiveScore() {
            when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.empty());
            when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
            when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);

            ScoredRoom result = strategy.scoreSingleRoom(student, application, room, defaultRule);

            assertThat(result).isNotNull();
            assertThat(result.getTotalScore()).isGreaterThan(BigDecimal.ZERO);
            assertThat(result.getRoomId()).isEqualTo(1L);
        }

        @Test
        void scoreSingleRoom_accessibilityMatch_addsBonus() {
            student.setSpecialRequirements("wheelchair user");

            RoomAttributes attrs = new RoomAttributes();
            attrs.setAccessibilityCompliant(true);

            when(roomAttributesRepository.findByRoom_Id(1L)).thenReturn(Optional.of(attrs));
            when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
            when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);

            AllocationRule accessRule = AllocationRule.builder()
                    .ruleName("Access")
                    .fcfsWeight(BigDecimal.ZERO)
                    .nsfasPriorityWeight(BigDecimal.ZERO)
                    .universityPrefWeight(BigDecimal.ZERO)
                    .accessibilityWeight(BigDecimal.ONE)
                    .preferenceMatchWeight(BigDecimal.ZERO)
                    .desirabilityWeight(BigDecimal.ZERO)
                    .active(true).priority(0).build();

            ScoredRoom result = strategy.scoreSingleRoom(student, application, room, accessRule);

            // Accessibility match: 100 * weight 1 = 100
            assertThat(result.getAccessibilityScore())
                    .isEqualByComparingTo(new BigDecimal("100.0000"));
        }
    }

    // ====================================================================
    // calculateAccessibilityScore edge cases
    // ====================================================================

    @Nested
    class AccessibilityScore {

        @Test
        void accessibilityScore_requiredAndCompliant_returnsMax() {
            student.setSpecialRequirements("disabled - mobility");
            RoomAttributes attrs = new RoomAttributes();
            attrs.setAccessibilityCompliant(true);

            BigDecimal score = strategy.calculateAccessibilityScore(student, null, attrs);
            assertThat(score).isEqualByComparingTo(OwnerConfigurableScoringStrategy.ACCESSIBILITY_MATCH_BONUS);
        }

        @Test
        void accessibilityScore_requiredButNotCompliant_returnsZero() {
            student.setSpecialRequirements("wheelchair");
            RoomAttributes attrs = new RoomAttributes();
            attrs.setAccessibilityCompliant(false);

            BigDecimal score = strategy.calculateAccessibilityScore(student, null, attrs);
            assertThat(score).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void accessibilityScore_notRequired_compliantRoom_returnsFlexibilityBonus() {
            // student has no accessibility requirement
            RoomAttributes attrs = new RoomAttributes();
            attrs.setAccessibilityCompliant(true);

            BigDecimal score = strategy.calculateAccessibilityScore(student, null, attrs);
            assertThat(score).isEqualByComparingTo(new BigDecimal("25"));
        }

        @Test
        void accessibilityScore_notRequired_nonCompliantRoom_returnsMidScore() {
            BigDecimal score = strategy.calculateAccessibilityScore(student, null, null);
            assertThat(score).isEqualByComparingTo(new BigDecimal("50"));
        }

        @Test
        void accessibilityScore_applicationFlagOverridesStudentField() {
            application.setRequiresAccessibility(true);
            RoomAttributes attrs = new RoomAttributes();
            attrs.setAccessibilityCompliant(true);

            BigDecimal score = strategy.calculateAccessibilityScore(student, application, attrs);
            assertThat(score).isEqualByComparingTo(OwnerConfigurableScoringStrategy.ACCESSIBILITY_MATCH_BONUS);
        }
    }

    // ====================================================================
    // getLatestRuleForBuilding fallback behaviour
    // ====================================================================

    @Nested
    class RuleLookup {

        @Test
        void getLatestRuleForBuilding_buildingSpecificRuleFound_returnsBuildingRule() {
            AllocationRule buildingRule = AllocationRule.builder()
                    .ruleName("Building-specific")
                    .fcfsWeight(BigDecimal.ONE)
                    .nsfasPriorityWeight(BigDecimal.ONE)
                    .universityPrefWeight(BigDecimal.ONE)
                    .accessibilityWeight(BigDecimal.ONE)
                    .preferenceMatchWeight(BigDecimal.ONE)
                    .desirabilityWeight(BigDecimal.ONE)
                    .priority(10).active(true).build();

            when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                    .thenReturn(List.of(buildingRule));

            AllocationRule result = strategy.getLatestRuleForBuilding(1L);
            assertThat(result.getRuleName()).isEqualTo("Building-specific");
        }

        @Test
        void getLatestRuleForBuilding_noBuildingRule_fallsBackToGlobal() {
            AllocationRule globalRule = AllocationRule.builder()
                    .ruleName("Global")
                    .fcfsWeight(BigDecimal.ONE)
                    .nsfasPriorityWeight(BigDecimal.ONE)
                    .universityPrefWeight(BigDecimal.ONE)
                    .accessibilityWeight(BigDecimal.ONE)
                    .preferenceMatchWeight(BigDecimal.ONE)
                    .desirabilityWeight(BigDecimal.ONE)
                    .priority(5).active(true).build();

            when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(1L))
                    .thenReturn(Collections.emptyList());
            when(allocationRuleRepository.findByBuildingIsNullAndActiveTrue())
                    .thenReturn(List.of(globalRule));

            AllocationRule result = strategy.getLatestRuleForBuilding(1L);
            assertThat(result.getRuleName()).isEqualTo("Global");
        }

        @Test
        void getLatestRuleForBuilding_noRulesAtAll_returnsDefaultRule() {
            when(allocationRuleRepository.findByBuilding_IdAndActiveTrue(anyLong()))
                    .thenReturn(Collections.emptyList());
            when(allocationRuleRepository.findByBuildingIsNullAndActiveTrue())
                    .thenReturn(Collections.emptyList());

            AllocationRule result = strategy.getLatestRuleForBuilding(1L);
            assertThat(result.getRuleName()).isEqualTo("Default");
            assertThat(result.getFcfsWeight()).isEqualByComparingTo(BigDecimal.ONE);
        }

        @Test
        void getLatestRuleForBuilding_nullBuildingId_usesGlobalRule() {
            AllocationRule globalRule = AllocationRule.builder()
                    .ruleName("Global")
                    .fcfsWeight(BigDecimal.ONE)
                    .nsfasPriorityWeight(BigDecimal.ONE)
                    .universityPrefWeight(BigDecimal.ONE)
                    .accessibilityWeight(BigDecimal.ONE)
                    .preferenceMatchWeight(BigDecimal.ONE)
                    .desirabilityWeight(BigDecimal.ONE)
                    .priority(0).active(true).build();

            when(allocationRuleRepository.findByBuildingIsNullAndActiveTrue())
                    .thenReturn(List.of(globalRule));

            AllocationRule result = strategy.getLatestRuleForBuilding(null);
            assertThat(result.getRuleName()).isEqualTo("Global");
        }
    }
}
