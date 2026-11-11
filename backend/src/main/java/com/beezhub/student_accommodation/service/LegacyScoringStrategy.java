package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.ScoredRoom;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.GenderRestriction;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.allocation.ScoringStrategy;
import com.beezhub.student_accommodation.service.allocation.StayPeriod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Legacy v1.1 scoring strategy with hardcoded weights.
 * Retained as a fallback/reference implementation.
 * {@code OwnerConfigurableScoringStrategy} is now {@code @Primary}.
 *
 * <p>No behaviour changes from the original {@code AllocationScoringService} —
 * all existing tests continue to pass.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LegacyScoringStrategy implements ScoringStrategy {

    private final AllocationRuleRepository allocationRuleRepository;
    private final RoomRepository roomRepository;
    private final RoomAttributesRepository roomAttributesRepository;
    private final BuildingAttributesRepository buildingAttributesRepository;
    private final RoomAllocationRepository roomAllocationRepository;

    // Scoring constants
    private static final BigDecimal PREFERRED_BUILDING_BONUS = new BigDecimal("100");
    private static final BigDecimal ALTERNATIVE_BUILDING_BONUS = new BigDecimal("50");
    private static final BigDecimal NO_PREFERENCE_BONUS = BigDecimal.ZERO;
    private static final BigDecimal ACCESSIBILITY_MATCH_BONUS = new BigDecimal("100");
    private static final BigDecimal DESIRABILITY_MULTIPLIER = new BigDecimal("10");
    private static final BigDecimal SUBOPTIMAL_THRESHOLD = new BigDecimal("50");

    // ==================== ScoringStrategy interface ====================

    /**
     * Scores all available rooms for a student and application.
     * Returns list sorted by score DESC, then by application createdAt ASC (for tie-breaking).
     *
     * @param student     The student to score rooms for
     * @param application The application containing preferences
     * @param preferred   Optional preferred building override
     * @return List of scored rooms sorted by score descending
     */
    @Override
    public List<ScoredRoom> scoreAvailableRooms(Student student, Application application, Building preferred) {
        log.info("Scoring available rooms for student: {}, application: {}",
                student.getId(), application != null ? application.getId() : "N/A");

        // Rooms with a free bed for the stay applied for (GH-496)
        List<Room> availableRooms = getAvailableRooms(StayPeriod.of(application));
        log.debug("Found {} rooms with available capacity", availableRooms.size());

        // Batch-load all attributes in 2 queries (N+1 fix — TASK-2.10)
        Set<Long> roomIds = availableRooms.stream().map(Room::getId).collect(Collectors.toSet());
        Set<Long> buildingIds = availableRooms.stream()
                .map(Room::getBuilding)
                .filter(b -> b != null)
                .map(Building::getId)
                .collect(Collectors.toSet());

        Map<Long, RoomAttributes> roomAttrMap = roomAttributesRepository
                .findByRoom_IdIn(roomIds)
                .stream()
                .collect(Collectors.toMap(ra -> ra.getRoom().getId(), ra -> ra));
        Map<Long, BuildingAttributes> buildingAttrMap = buildingAttributesRepository
                .findByBuilding_IdIn(buildingIds)
                .stream()
                .collect(Collectors.toMap(ba -> ba.getBuilding().getId(), ba -> ba));

        List<ScoredRoom> scoredRooms = new ArrayList<>();

        for (Room room : availableRooms) {
            // Check hard constraints first using pre-loaded maps
            boolean meetsConstraints = validateHardConstraintsFromMaps(student, room, roomAttrMap, buildingAttrMap);

            if (meetsConstraints) {
                ScoredRoom scoredRoom = scoreSingleRoomFromMaps(student, application, room, preferred,
                        roomAttrMap, buildingAttrMap);
                scoredRooms.add(scoredRoom);
            } else {
                log.debug("Room {} failed hard constraints for student {}", room.getRoomNumber(), student.getId());
            }
        }

        // Score descending, then the better room, then the id. The previous tie-break read the
        // application createdAt, which is the same for every room in this list and so decided
        // nothing, leaving equally-scored rooms in unspecified repository order (GH-140).
        scoredRooms.sort(Comparator
                .comparing(ScoredRoom::getTotalScore, Comparator.reverseOrder())
                .thenComparing(ScoredRoom::getDesirabilityRating,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ScoredRoom::getRoomId,
                        Comparator.nullsLast(Comparator.naturalOrder())));

        log.info("Scored {} rooms for student {}", scoredRooms.size(), student.getId());
        return scoredRooms;
    }

    /**
     * Calculates the score for a single student-room pair using the provided rule.
     *
     * @param student     The student
     * @param application The application with preferences
     * @param room        The room to score
     * @param rule        The allocation rule containing scoring weights
     * @return ScoredRoom with calculated score and warnings
     */
    @Override
    public ScoredRoom scoreSingleRoom(Student student, Application application, Room room, AllocationRule rule) {
        log.debug("Scoring room {} for student {} with explicit rule", room.getRoomNumber(), student.getId());
        return scoreSingleRoomWithRule(student, application, room, null, rule);
    }

    /**
     * Validates hard constraints for a student-room pair (interface contract).
     */
    @Override
    public boolean satisfiesHardConstraints(Student student, Room room) {
        return validateHardConstraints(student, room, false);
    }

    // ==================== Public API (backward-compatible) ====================

    /**
     * Calculates the score for a single student-room pair.
     *
     * @param student     The student
     * @param application The application with preferences
     * @param room        The room to score
     * @return ScoredRoom with calculated score and warnings
     */
    public ScoredRoom scoreSingleRoom(Student student, Application application, Room room) {
        return scoreSingleRoomInternal(student, application, room, null);
    }

    /**
     * Calculates the score for a single student-room pair with optional preferred building override.
     *
     * @param student           The student
     * @param application       The application with preferences
     * @param room              The room to score
     * @param preferredBuilding Optional preferred building override
     * @return ScoredRoom with calculated score and warnings
     */
    public ScoredRoom scoreSingleRoom(Student student, Application application, Room room, Building preferredBuilding) {
        return scoreSingleRoomInternal(student, application, room, preferredBuilding);
    }

    /**
     * Gets the latest active allocation rule for a building.
     * Falls back to global rule (building_id = null) if no building-specific rule exists.
     *
     * @param buildingId The building ID (can be null)
     * @return The active allocation rule
     */
    public AllocationRule getLatestRuleForBuilding(Long buildingId) {
        log.debug("Getting latest rule for building: {}", buildingId);

        // Try to get building-specific rule first
        if (buildingId != null) {
            List<AllocationRule> buildingRules = allocationRuleRepository
                    .findByBuilding_IdAndActiveTrue(buildingId);

            if (!buildingRules.isEmpty()) {
                // Return highest priority rule
                AllocationRule rule = buildingRules.stream()
                        .max(Comparator.comparing(AllocationRule::getPriority))
                        .orElse(buildingRules.get(0));
                log.debug("Found building-specific rule: {} with priority {}", rule.getRuleName(), rule.getPriority());
                return rule;
            }
        }

        // Fall back to global rule (building_id = null)
        List<AllocationRule> globalRules = allocationRuleRepository.findByBuildingIsNullAndActiveTrue();

        if (!globalRules.isEmpty()) {
            AllocationRule rule = globalRules.stream()
                    .max(Comparator.comparing(AllocationRule::getPriority))
                    .orElse(globalRules.get(0));
            log.debug("Using global rule: {} with priority {}", rule.getRuleName(), rule.getPriority());
            return rule;
        }

        // Return default rule if none found
        log.debug("No allocation rules found, using default weights");
        return createDefaultRule();
    }

    /**
     * Validates hard constraints for a student-room pair.
     *
     * @param student       The student
     * @param room          The room
     * @param allowOverride Whether to allow constraint bypass
     * @return true if all constraints pass, false otherwise
     */
    public boolean validateHardConstraints(Student student, Room room, boolean allowOverride) {
        if (allowOverride) {
            log.debug("Hard constraint validation bypassed for student {} and room {}",
                    student.getId(), room.getRoomNumber());
            return true;
        }

        // Get room and building attributes
        Optional<RoomAttributes> roomAttrs = roomAttributesRepository.findByRoom_Id(room.getId());
        Optional<BuildingAttributes> buildingAttrs = room.getBuilding() != null
                ? buildingAttributesRepository.findByBuilding_Id(room.getBuilding().getId())
                : Optional.empty();

        // Check gender compatibility
        if (!isGenderCompatible(student, roomAttrs.orElse(null), buildingAttrs.orElse(null))) {
            log.debug("Gender constraint failed for student {} and room {}", student.getId(), room.getRoomNumber());
            return false;
        }

        // Check accessibility compliance
        if (!isAccessibilityCompliant(student, roomAttrs.orElse(null))) {
            log.debug("Accessibility constraint failed for student {} and room {}", student.getId(), room.getRoomNumber());
            return false;
        }

        // Check year of study constraint
        if (!isYearOfStudyCompatible(student, roomAttrs.orElse(null))) {
            log.debug("Year of study constraint failed for student {} and room {}", student.getId(), room.getRoomNumber());
            return false;
        }

        // Check building-institution constraint
        if (!isBuildingInstitutionCompatible(student, room.getBuilding())) {
            log.debug("Building-institution constraint failed for student {} and room {}", student.getId(), room.getRoomNumber());
            return false;
        }

        // Check if student has previously declined this room
        if (roomAllocationRepository.hasStudentDeclinedRoom(student.getId(), room.getId())) {
            log.debug("Student {} has previously declined room {}, excluding from allocation", student.getId(), room.getRoomNumber());
            return false;
        }

        return true;
    }

    /**
     * Gets eligible rooms for a student without scoring.
     * Filters rooms based on hard constraints (gender, accessibility, year of study, institution).
     *
     * @param student  The student
     * @param building Optional building to filter by (if null, searches all buildings)
     * @return List of eligible rooms (no scoring, unordered)
     */
    public List<Room> getEligibleRoomsForStudent(Student student, Building building) {
        return getEligibleRoomsForStudent(student, building, StayPeriod.UNBOUNDED);
    }

    /**
     * Rooms that pass the hard constraints and have a free bed for {@code stay} (GH-496).
     */
    public List<Room> getEligibleRoomsForStudent(Student student, Building building, StayPeriod stay) {
        log.info("Getting eligible rooms for student: {}, building: {}",
                student.getId(), building != null ? building.getId() : "all");

        List<Room> availableRooms;
        if (building != null) {
            availableRooms = roomRepository.findByBuildingId(building.getId()).stream()
                    .filter(room -> hasFreeBed(room, stay))
                    .collect(Collectors.toList());
        } else {
            availableRooms = getAvailableRooms(stay);
        }

        List<Room> eligibleRooms = availableRooms.stream()
                .filter(room -> validateHardConstraints(student, room, false))
                .collect(Collectors.toList());

        log.info("Found {} eligible rooms for student {}", eligibleRooms.size(), student.getId());
        return eligibleRooms;
    }

    /**
     * Gets eligible rooms for a student filtered by institution.
     * Only shows rooms in buildings that serve the student's institution.
     *
     * @param student The student
     * @return List of eligible rooms filtered by institution
     */
    public List<Room> getEligibleRoomsForStudentByInstitution(Student student) {
        log.info("Getting eligible rooms for student {} by institution", student.getId());

        Institution studentInstitution = student.getInstitution();
        if (studentInstitution == null) {
            log.warn("Student {} has no institution, returning all eligible rooms", student.getId());
            return getEligibleRoomsForStudent(student, null);
        }

        // Get buildings that serve the student's institution
        List<Room> availableRooms = getAvailableRooms(StayPeriod.UNBOUNDED).stream()
                .filter(room -> {
                    Building building = room.getBuilding();
                    if (building == null || building.getInstitutions() == null || building.getInstitutions().isEmpty()) {
                        // Building with no institution restriction accepts all students
                        return true;
                    }
                    return building.getInstitutions().stream()
                            .anyMatch(inst -> inst.getId().equals(studentInstitution.getId()));
                })
                .collect(Collectors.toList());

        List<Room> eligibleRooms = availableRooms.stream()
                .filter(room -> validateHardConstraints(student, room, false))
                .collect(Collectors.toList());

        log.info("Found {} eligible rooms for student {} filtered by institution {}",
                eligibleRooms.size(), student.getId(), studentInstitution.getId());
        return eligibleRooms;
    }

    /**
     * Generates warning messages for a student-room-application combination.
     *
     * @param student     The student
     * @param application The application
     * @param room        The room
     * @param rule        The allocation rule
     * @return List of warning strings
     */
    public List<String> generateWarnings(Student student, Application application, Room room, AllocationRule rule) {
        List<String> warnings = new ArrayList<>();

        // Check building preference mismatch
        if (application != null && application.getPreferredBuilding() != null) {
            if (room.getBuilding() == null ||
                    !room.getBuilding().getId().equals(application.getPreferredBuilding().getId())) {
                warnings.add("BUILDING_MISMATCH");
                log.debug("Warning: Building mismatch for room {}", room.getRoomNumber());
            }
        }

        // Check suboptimal desirability
        ScoredRoom tempScore = scoreSingleRoomWithoutWarnings(student, application, room);
        if (tempScore.getTotalScore().compareTo(SUBOPTIMAL_THRESHOLD) < 0) {
            warnings.add("SUBOPTIMAL_DESIRABILITY");
            log.debug("Warning: Suboptimal desirability score {} for room {}",
                    tempScore.getTotalScore(), room.getRoomNumber());
        }

        // Check accessibility mismatch
        if (application != null && Boolean.TRUE.equals(application.getRequiresAccessibility())) {
            Optional<RoomAttributes> roomAttrs = roomAttributesRepository.findByRoom_Id(room.getId());
            boolean roomIsAccessible = roomAttrs.map(RoomAttributes::getAccessibilityCompliant).orElse(false);
            if (!roomIsAccessible) {
                warnings.add("ACCESSIBILITY_MISMATCH");
                log.debug("Warning: Accessibility mismatch for room {}", room.getRoomNumber());
            }
        }

        return warnings;
    }

    // ==================== Private helpers ====================

    /**
     * Validates hard constraints using pre-loaded attribute maps (avoids per-room queries).
     * Used by the batch scoring loop in {@link #scoreAvailableRooms}.
     */
    private boolean validateHardConstraintsFromMaps(Student student, Room room,
                                                    Map<Long, RoomAttributes> roomAttrMap,
                                                    Map<Long, BuildingAttributes> buildingAttrMap) {
        RoomAttributes roomAttrs = roomAttrMap.get(room.getId());
        BuildingAttributes buildingAttrs = room.getBuilding() != null
                ? buildingAttrMap.get(room.getBuilding().getId())
                : null;

        if (!isGenderCompatible(student, roomAttrs, buildingAttrs)) {
            return false;
        }
        if (!isAccessibilityCompliant(student, roomAttrs)) {
            return false;
        }
        if (!isYearOfStudyCompatible(student, roomAttrs)) {
            return false;
        }
        if (!isBuildingInstitutionCompatible(student, room.getBuilding())) {
            return false;
        }
        if (roomAllocationRepository.hasStudentDeclinedRoom(student.getId(), room.getId())) {
            return false;
        }
        return true;
    }

    /**
     * Scores a single room using pre-loaded attribute maps (avoids per-room queries).
     * Used by the batch scoring loop in {@link #scoreAvailableRooms}.
     */
    private ScoredRoom scoreSingleRoomFromMaps(Student student, Application application, Room room,
                                               Building preferredBuilding,
                                               Map<Long, RoomAttributes> roomAttrMap,
                                               Map<Long, BuildingAttributes> buildingAttrMap) {
        AllocationRule rule = getLatestRuleForBuilding(room.getBuilding() != null ? room.getBuilding().getId() : null);

        RoomAttributes roomAttrs = roomAttrMap.get(room.getId());
        BuildingAttributes buildingAttrs = room.getBuilding() != null
                ? buildingAttrMap.get(room.getBuilding().getId())
                : null;

        Building effectivePreferredBuilding = preferredBuilding != null
                ? preferredBuilding
                : (application != null ? application.getPreferredBuilding() : null);

        BigDecimal preferenceMatchScore = calculatePreferenceMatchScore(room, effectivePreferredBuilding);
        BigDecimal desirabilityScore = calculateDesirabilityScore(room, roomAttrs, buildingAttrs);
        BigDecimal accessibilityScore = calculateAccessibilityScore(student, application, roomAttrs);

        BigDecimal weightedPreference = preferenceMatchScore.multiply(rule.getPreferenceMatchWeight());
        BigDecimal weightedDesirability = desirabilityScore.multiply(rule.getDesirabilityWeight());
        BigDecimal weightedAccessibility = accessibilityScore.multiply(rule.getAccessibilityWeight());

        BigDecimal totalScore = weightedPreference
                .add(weightedDesirability)
                .add(weightedAccessibility)
                .setScale(4, RoundingMode.HALF_UP);

        List<String> warnings = generateWarningsFromMaps(student, application, room, rule, roomAttrs, buildingAttrs);

        long activeAllocations = bedsTaken(room, StayPeriod.of(application));
        int availableSpots = room.getCapacity() - (int) activeAllocations;

        return ScoredRoom.builder()
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .capacity(room.getCapacity())
                .currentOccupancy((int) activeAllocations)
                .availableSpots(availableSpots)
                .buildingId(room.getBuilding() != null ? room.getBuilding().getId() : null)
                .buildingName(room.getBuilding() != null ? room.getBuilding().getName() : null)
                .totalScore(totalScore)
                .preferenceMatchScore(weightedPreference.setScale(4, RoundingMode.HALF_UP))
                .desirabilityScore(weightedDesirability.setScale(4, RoundingMode.HALF_UP))
                .accessibilityScore(weightedAccessibility.setScale(4, RoundingMode.HALF_UP))
                .warnings(warnings)
                .meetsHardConstraints(validateHardConstraintsFromMaps(student, room, roomAttrMap, buildingAttrMap))
                .isPreferredBuilding(isPreferredBuilding(room, effectivePreferredBuilding))
                .isAccessibilityCompliant(roomAttrs != null && Boolean.TRUE.equals(roomAttrs.getAccessibilityCompliant()))
                .applicationCreatedAt(application != null ? application.getCreatedAt() : null)
                .genderRestriction(getEffectiveGenderRestriction(roomAttrs, buildingAttrs))
                .desirabilityRating(getEffectiveDesirability(roomAttrs, buildingAttrs))
                .build();
    }

    /**
     * Generates warnings using pre-loaded attribute maps.
     */
    private List<String> generateWarningsFromMaps(Student student, Application application, Room room,
                                                   AllocationRule rule, RoomAttributes roomAttrs,
                                                   BuildingAttributes buildingAttrs) {
        List<String> warnings = new ArrayList<>();

        if (application != null && application.getPreferredBuilding() != null) {
            if (room.getBuilding() == null ||
                    !room.getBuilding().getId().equals(application.getPreferredBuilding().getId())) {
                warnings.add("BUILDING_MISMATCH");
            }
        }

        // Quick score without warnings to check suboptimal threshold
        BigDecimal preferredBuilding1 = application != null && application.getPreferredBuilding() != null
                ? calculatePreferenceMatchScore(room, application.getPreferredBuilding())
                : calculatePreferenceMatchScore(room, null);
        BigDecimal desirability = calculateDesirabilityScore(room, roomAttrs, buildingAttrs);
        BigDecimal accessibility = calculateAccessibilityScore(student, application, roomAttrs);
        BigDecimal quickScore = preferredBuilding1.multiply(rule.getPreferenceMatchWeight())
                .add(desirability.multiply(rule.getDesirabilityWeight()))
                .add(accessibility.multiply(rule.getAccessibilityWeight()))
                .setScale(4, RoundingMode.HALF_UP);
        if (quickScore.compareTo(SUBOPTIMAL_THRESHOLD) < 0) {
            warnings.add("SUBOPTIMAL_DESIRABILITY");
        }

        if (application != null && Boolean.TRUE.equals(application.getRequiresAccessibility())) {
            if (roomAttrs == null || !Boolean.TRUE.equals(roomAttrs.getAccessibilityCompliant())) {
                warnings.add("ACCESSIBILITY_MISMATCH");
            }
        }

        return warnings;
    }

    private ScoredRoom scoreSingleRoomInternal(Student student, Application application, Room room, Building preferredBuilding) {
        log.debug("Scoring room {} for student {}", room.getRoomNumber(), student.getId());

        // Get the allocation rule for this building
        AllocationRule rule = getLatestRuleForBuilding(room.getBuilding() != null ? room.getBuilding().getId() : null);

        return scoreSingleRoomWithRule(student, application, room, preferredBuilding, rule);
    }

    private ScoredRoom scoreSingleRoomWithRule(Student student, Application application, Room room,
                                               Building preferredBuilding, AllocationRule rule) {
        // Get room and building attributes
        Optional<RoomAttributes> roomAttrs = roomAttributesRepository.findByRoom_Id(room.getId());
        Optional<BuildingAttributes> buildingAttrs = room.getBuilding() != null
                ? buildingAttributesRepository.findByBuilding_Id(room.getBuilding().getId())
                : Optional.empty();

        // Determine the effective preferred building
        Building effectivePreferredBuilding = preferredBuilding != null
                ? preferredBuilding
                : (application != null ? application.getPreferredBuilding() : null);

        // Calculate individual scores
        BigDecimal preferenceMatchScore = calculatePreferenceMatchScore(room, effectivePreferredBuilding);
        BigDecimal desirabilityScore = calculateDesirabilityScore(room, roomAttrs.orElse(null), buildingAttrs.orElse(null));
        BigDecimal accessibilityScore = calculateAccessibilityScore(student, application, roomAttrs.orElse(null));

        // Apply weights from rule
        BigDecimal weightedPreference = preferenceMatchScore.multiply(rule.getPreferenceMatchWeight());
        BigDecimal weightedDesirability = desirabilityScore.multiply(rule.getDesirabilityWeight());
        BigDecimal weightedAccessibility = accessibilityScore.multiply(rule.getAccessibilityWeight());

        // Calculate total score
        BigDecimal totalScore = weightedPreference
                .add(weightedDesirability)
                .add(weightedAccessibility)
                .setScale(4, RoundingMode.HALF_UP);

        // Generate warnings
        List<String> warnings = generateWarnings(student, application, room, rule);

        // Get room availability info
        long activeAllocations = bedsTaken(room, StayPeriod.of(application));
        int availableSpots = room.getCapacity() - (int) activeAllocations;

        // Build scored room response
        ScoredRoom scoredRoom = ScoredRoom.builder()
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .capacity(room.getCapacity())
                .currentOccupancy((int) activeAllocations)
                .availableSpots(availableSpots)
                .buildingId(room.getBuilding() != null ? room.getBuilding().getId() : null)
                .buildingName(room.getBuilding() != null ? room.getBuilding().getName() : null)
                .totalScore(totalScore)
                .preferenceMatchScore(weightedPreference.setScale(4, RoundingMode.HALF_UP))
                .desirabilityScore(weightedDesirability.setScale(4, RoundingMode.HALF_UP))
                .accessibilityScore(weightedAccessibility.setScale(4, RoundingMode.HALF_UP))
                .warnings(warnings)
                .meetsHardConstraints(validateHardConstraints(student, room, false))
                .isPreferredBuilding(isPreferredBuilding(room, effectivePreferredBuilding))
                .isAccessibilityCompliant(roomAttrs.map(RoomAttributes::getAccessibilityCompliant).orElse(false))
                .applicationCreatedAt(application != null ? application.getCreatedAt() : null)
                .genderRestriction(getEffectiveGenderRestriction(roomAttrs.orElse(null), buildingAttrs.orElse(null)))
                .desirabilityRating(getEffectiveDesirability(roomAttrs.orElse(null), buildingAttrs.orElse(null)))
                .build();

        log.debug("Room {} scored {} for student {}", room.getRoomNumber(), totalScore, student.getId());
        return scoredRoom;
    }

    /**
     * Rooms open for offers with a free bed for {@code stay}. Any status but MAINTENANCE: FULL and
     * OCCUPIED describe today (GH-496), and a room full today can still be free for next year.
     */
    private List<Room> getAvailableRooms(StayPeriod stay) {
        return roomRepository.findAll().stream()
                .filter(room -> room.getStatus() != null && !"MAINTENANCE".equals(room.getStatus()))
                .filter(room -> hasFreeBed(room, stay))
                .collect(Collectors.toList());
    }

    private boolean hasFreeBed(Room room, StayPeriod stay) {
        return bedsTaken(room, stay) < room.getCapacity();
    }

    /** Beds taken in the room during the stay, by planned dates. */
    private long bedsTaken(Room room, StayPeriod stay) {
        return roomAllocationRepository.countHoldingBeds(room.getId(), stay.from(), stay.until());
    }

    private BigDecimal calculatePreferenceMatchScore(Room room, Building preferredBuilding) {
        if (preferredBuilding == null) {
            // No preference specified - give alternative bonus
            return ALTERNATIVE_BUILDING_BONUS;
        }

        if (room.getBuilding() != null && room.getBuilding().getId().equals(preferredBuilding.getId())) {
            return PREFERRED_BUILDING_BONUS;
        }

        // Check if it's in the same general area or has similar attributes (alternative)
        if (room.getBuilding() != null) {
            return ALTERNATIVE_BUILDING_BONUS;
        }

        return NO_PREFERENCE_BONUS;
    }

    private BigDecimal calculateDesirabilityScore(Room room, RoomAttributes roomAttrs, BuildingAttributes buildingAttrs) {
        Integer desirability = getEffectiveDesirability(roomAttrs, buildingAttrs);
        if (desirability == null) {
            desirability = 5; // Default middle score
        }
        // Convert 1-10 scale to score (multiply by 10 to get 10-100 range)
        return new BigDecimal(desirability).multiply(DESIRABILITY_MULTIPLIER);
    }

    private BigDecimal calculateAccessibilityScore(Student student, Application application, RoomAttributes roomAttrs) {
        boolean requiresAccessibility = hasAccessibilityRequirements(student, application);
        boolean roomIsAccessible = roomAttrs != null && Boolean.TRUE.equals(roomAttrs.getAccessibilityCompliant());

        if (requiresAccessibility && roomIsAccessible) {
            return ACCESSIBILITY_MATCH_BONUS;
        } else if (requiresAccessibility && !roomIsAccessible) {
            // Required but not accessible - this should ideally be caught by hard constraints
            return BigDecimal.ZERO;
        } else if (!requiresAccessibility && roomIsAccessible) {
            // Room is accessible but not required - small bonus for flexibility
            return new BigDecimal("25");
        }

        // No accessibility requirement and room is not accessible
        return new BigDecimal("50");
    }

    private boolean isGenderCompatible(Student student, RoomAttributes roomAttrs, BuildingAttributes buildingAttrs) {
        String studentGender = student.getGender();
        if (studentGender == null || studentGender.isEmpty()) {
            return true; // No gender specified, allow any room
        }

        // Check room-level restriction first (takes precedence)
        GenderRestriction roomRestriction = roomAttrs != null ? roomAttrs.getGenderRestriction() : null;
        if (roomRestriction != null && roomRestriction != GenderRestriction.ANY) {
            return isGenderMatch(studentGender, roomRestriction);
        }

        // Check building-level restriction
        GenderRestriction buildingRestriction = buildingAttrs != null ? buildingAttrs.getGenderRestriction() : null;
        if (buildingRestriction != null && buildingRestriction != GenderRestriction.ANY) {
            // Check if building allows mixed gender
            if (buildingAttrs.getAllowMixedGender() != null && buildingAttrs.getAllowMixedGender()) {
                return true;
            }
            return isGenderMatch(studentGender, buildingRestriction);
        }

        return true; // No restrictions
    }

    private boolean isGenderMatch(String studentGender, GenderRestriction restriction) {
        if (restriction == GenderRestriction.ANY) {
            return true;
        }

        String normalizedGender = studentGender.toUpperCase().trim();
        return switch (restriction) {
            case MALE_ONLY -> normalizedGender.equals("MALE") || normalizedGender.equals("M");
            case FEMALE_ONLY -> normalizedGender.equals("FEMALE") || normalizedGender.equals("F");
            default -> true;
        };
    }

    private boolean isAccessibilityCompliant(Student student, RoomAttributes roomAttrs) {
        return isAccessibilityCompliant(student, null, roomAttrs);
    }

    private boolean isAccessibilityCompliant(Student student, Application application, RoomAttributes roomAttrs) {
        boolean requiresAccessibility = hasAccessibilityRequirements(student, application);

        if (!requiresAccessibility) {
            return true; // No accessibility requirements
        }

        // Student requires accessibility - room must be compliant
        return roomAttrs != null && Boolean.TRUE.equals(roomAttrs.getAccessibilityCompliant());
    }

    private boolean isYearOfStudyCompatible(Student student, RoomAttributes roomAttrs) {
        if (roomAttrs == null) {
            return true; // No room attributes, no restriction
        }

        Integer minYear = roomAttrs.getMinYearOfStudy();
        Integer maxYear = roomAttrs.getMaxYearOfStudy();

        // No year restrictions
        if (minYear == null && maxYear == null) {
            return true;
        }

        // Get student's year of study
        Integer studentYear = null;
        if (student.getYearOfStudy() != null && student.getYearOfStudy().getYearOfStudy() != null) {
            try {
                studentYear = Integer.parseInt(student.getYearOfStudy().getYearOfStudy());
            } catch (NumberFormatException e) {
                log.warn("Could not parse year of study: {}", student.getYearOfStudy().getYearOfStudy());
            }
        }

        if (studentYear == null) {
            // Student has no year of study set - allow by default
            return true;
        }

        // Check year range
        if (minYear != null && studentYear < minYear) {
            return false;
        }
        if (maxYear != null && studentYear > maxYear) {
            return false;
        }

        return true;
    }

    private boolean isBuildingInstitutionCompatible(Student student, Building building) {
        if (building == null) {
            return true;
        }

        // If building has no institution restrictions, it accepts all students
        if (building.getInstitutions() == null || building.getInstitutions().isEmpty()) {
            return true;
        }

        // Get student's institution
        Institution studentInstitution = student.getInstitution();
        if (studentInstitution == null) {
            // Student has no institution - allow by default (or could deny, depending on policy)
            return true;
        }

        // Check if building serves the student's institution
        return building.getInstitutions().stream()
                .anyMatch(inst -> inst.getId().equals(studentInstitution.getId()));
    }

    private boolean hasAccessibilityRequirements(Student student) {
        return hasAccessibilityRequirements(student, null);
    }

    private boolean hasAccessibilityRequirements(Student student, Application application) {
        // Primary: check application's explicit accessibility flag
        if (application != null && Boolean.TRUE.equals(application.getRequiresAccessibility())) {
            return true;
        }

        // Fallback: parse student's specialRequirements text (legacy)
        String specialRequirements = student.getSpecialRequirements();
        if (specialRequirements == null || specialRequirements.isEmpty()) {
            return false;
        }

        // Check for accessibility-related keywords
        String lowerReqs = specialRequirements.toLowerCase();
        return lowerReqs.contains("wheelchair") ||
                lowerReqs.contains("accessible") ||
                lowerReqs.contains("mobility") ||
                lowerReqs.contains("disability") ||
                lowerReqs.contains("handicap");
    }

    private boolean isPreferredBuilding(Room room, Building preferredBuilding) {
        if (preferredBuilding == null || room.getBuilding() == null) {
            return false;
        }
        return room.getBuilding().getId().equals(preferredBuilding.getId());
    }

    private String getEffectiveGenderRestriction(RoomAttributes roomAttrs, BuildingAttributes buildingAttrs) {
        if (roomAttrs != null && roomAttrs.getGenderRestriction() != null) {
            return roomAttrs.getGenderRestriction().name();
        }
        if (buildingAttrs != null && buildingAttrs.getGenderRestriction() != null) {
            return buildingAttrs.getGenderRestriction().name();
        }
        return GenderRestriction.ANY.name();
    }

    private Integer getEffectiveDesirability(RoomAttributes roomAttrs, BuildingAttributes buildingAttrs) {
        // Fall back to building desirability (desirabilityOverride removed from room)
        if (buildingAttrs != null && buildingAttrs.getDesirabilityScore() != null) {
            return buildingAttrs.getDesirabilityScore();
        }
        return null;
    }

    private ScoredRoom scoreSingleRoomWithoutWarnings(Student student, Application application, Room room) {
        // Simplified scoring without generating warnings to avoid recursion
        AllocationRule rule = getLatestRuleForBuilding(room.getBuilding() != null ? room.getBuilding().getId() : null);

        Optional<RoomAttributes> roomAttrs = roomAttributesRepository.findByRoom_Id(room.getId());
        Optional<BuildingAttributes> buildingAttrs = room.getBuilding() != null
                ? buildingAttributesRepository.findByBuilding_Id(room.getBuilding().getId())
                : Optional.empty();

        Building preferredBuilding = application != null ? application.getPreferredBuilding() : null;

        BigDecimal preferenceMatchScore = calculatePreferenceMatchScore(room, preferredBuilding);
        BigDecimal desirabilityScore = calculateDesirabilityScore(room, roomAttrs.orElse(null), buildingAttrs.orElse(null));
        BigDecimal accessibilityScore = calculateAccessibilityScore(student, application, roomAttrs.orElse(null));

        BigDecimal totalScore = preferenceMatchScore.multiply(rule.getPreferenceMatchWeight())
                .add(desirabilityScore.multiply(rule.getDesirabilityWeight()))
                .add(accessibilityScore.multiply(rule.getAccessibilityWeight()))
                .setScale(4, RoundingMode.HALF_UP);

        return ScoredRoom.builder()
                .totalScore(totalScore)
                .build();
    }

    private AllocationRule createDefaultRule() {
        return AllocationRule.builder()
                .ruleName("Default")
                .preferenceMatchWeight(BigDecimal.ONE)
                .desirabilityWeight(BigDecimal.ONE)
                .accessibilityWeight(BigDecimal.ONE)
                .active(true)
                .priority(0)
                .build();
    }
}
