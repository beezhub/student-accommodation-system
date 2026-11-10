package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.dto.ScoredRoom;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.GenderRestriction;
import com.beezhub.student_accommodation.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * v2.0 owner-configurable scoring strategy.
 *
 * <p>Scoring components (all weights configurable via {@link AllocationRule}):
 * <ol>
 *   <li><b>FCFS score</b> — normalised position of the application in the pool by {@code createdAt}
 *       (earliest = 100 pts, latest = 0 pts). Weighted by {@code fcfsWeight}.</li>
 *   <li><b>University preference</b> — student's institution is in the rule's
 *       {@code priorityInstitutionIds} list → configured bonus. Weighted by
 *       {@code universityPrefWeight}.</li>
 *   <li><b>NSFAS priority</b> — student has confirmed NSFAS funding (detected from
 *       {@code specialRequirements} keywords) → configured bonus. Weighted by
 *       {@code nsfasPriorityWeight}.</li>
 *   <li><b>Accessibility match</b> — student requires + room compliant → configured bonus.
 *       Weighted by {@code accessibilityWeight}.</li>
 * </ol>
 *
 * <p>Hard constraints (must all pass — if any fails the room is excluded):
 * <ul>
 *   <li>Application status is APPROVED</li>
 *   <li>All application documents are VERIFIED</li>
 *   <li>Student has no active allocation</li>
 *   <li>Room has a free bed for the stay applied for (planned dates overlap, GH-496)</li>
 *   <li>Room {@code is_available} flag is true</li>
 *   <li>Gender restriction compatible</li>
 *   <li>Accessibility — if student requires it, room must be compliant</li>
 * </ul>
 *
 * <p>Tiebreaker: earlier {@code application.createdAt} wins (ascending).
 */
@Service
@Primary
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OwnerConfigurableScoringStrategy implements ScoringStrategy {

    // ---- Score component baselines ----
    static final BigDecimal FCFS_MAX_SCORE             = new BigDecimal("100");
    static final BigDecimal UNIVERSITY_PREF_BONUS      = new BigDecimal("100");
    static final BigDecimal NSFAS_PRIORITY_BONUS       = new BigDecimal("100");
    static final BigDecimal ACCESSIBILITY_MATCH_BONUS  = new BigDecimal("100");
    static final BigDecimal SUBOPTIMAL_THRESHOLD       = new BigDecimal("50");

    private final AllocationRuleRepository      allocationRuleRepository;
    private final RoomRepository                roomRepository;
    private final RoomAttributesRepository      roomAttributesRepository;
    private final BuildingAttributesRepository  buildingAttributesRepository;
    private final RoomAllocationRepository      roomAllocationRepository;

    // ==================== ScoringStrategy interface ====================

    /**
     * {@inheritDoc}
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Load all available rooms.</li>
     *   <li>Batch-load room and building attributes in 2 queries.</li>
     *   <li>Filter by hard constraints.</li>
     *   <li>Compute the FCFS normalisation window across the eligible-room pool.</li>
     *   <li>Score each room with all four components.</li>
     *   <li>Sort DESC by total score, tiebreaker ASC by {@code application.createdAt}.</li>
     * </ol>
     */
    @Override
    public List<ScoredRoom> scoreAvailableRooms(Student student, Application application, Building preferred) {
        log.info("strategy=OwnerConfigurable action=scoreAvailableRooms student={} application={}",
                student.getId(), application != null ? application.getId() : "N/A");

        List<Room> availableRooms = getAvailableRooms(StayPeriod.of(application));
        log.debug("strategy=OwnerConfigurable availableRooms={}", availableRooms.size());

        // Batch-load attributes (N+1 prevention)
        Set<Long> roomIds     = availableRooms.stream().map(Room::getId).collect(Collectors.toSet());
        Set<Long> buildingIds = availableRooms.stream()
                .map(Room::getBuilding).filter(Objects::nonNull)
                .map(Building::getId).collect(Collectors.toSet());

        Map<Long, RoomAttributes>     roomAttrMap = roomAttributesRepository
                .findByRoom_IdIn(roomIds).stream()
                .collect(Collectors.toMap(ra -> ra.getRoom().getId(), ra -> ra));
        Map<Long, BuildingAttributes> buildingAttrMap = buildingAttributesRepository
                .findByBuilding_IdIn(buildingIds).stream()
                .collect(Collectors.toMap(ba -> ba.getBuilding().getId(), ba -> ba));

        // Identify the FCFS reference timestamp (application.createdAt for this student)
        LocalDateTime thisCreatedAt = application != null ? application.getCreatedAt() : null;

        // Collect eligible rooms first so we can normalise FCFS over the same pool
        List<Room> eligibleRooms = availableRooms.stream()
                .filter(room -> satisfiesHardConstraintsFromMaps(student, room, roomAttrMap, buildingAttrMap))
                .collect(Collectors.toList());

        // Build scored list
        List<ScoredRoom> scoredRooms = new ArrayList<>();
        for (Room room : eligibleRooms) {
            AllocationRule rule = getLatestRuleForBuilding(
                    room.getBuilding() != null ? room.getBuilding().getId() : null);

            ScoredRoom scored = buildScoredRoom(student, application, room, preferred,
                    rule, roomAttrMap, buildingAttrMap, thisCreatedAt);
            scoredRooms.add(scored);
        }

        // Score descending, then the better room, then the id.
        //
        // applicationCreatedAt is gone from the tie-break: every room here belongs to the same
        // application, so it held the same value for all of them and decided nothing. Ties then
        // fell through the sort stability to the order rooms came back from findAll(), which has
        // no ORDER BY, so equally-scored rooms were picked in unspecified heap order (GH-140).
        scoredRooms.sort(Comparator
                .comparing(ScoredRoom::getTotalScore, Comparator.reverseOrder())
                .thenComparing(ScoredRoom::getDesirabilityRating,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ScoredRoom::getRoomId,
                        Comparator.nullsLast(Comparator.naturalOrder())));

        log.info("strategy=OwnerConfigurable action=scored count={} student={}", scoredRooms.size(), student.getId());
        return scoredRooms;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Scores a single room with the four configurable components.
     * FCFS score is set to max ({@value #FCFS_MAX_SCORE}) when scoring in isolation
     * (no pool context available for normalisation).
     */
    @Override
    public ScoredRoom scoreSingleRoom(Student student, Application application, Room room, AllocationRule rule) {
        log.debug("strategy=OwnerConfigurable action=scoreSingleRoom room={} student={}",
                room.getRoomNumber(), student.getId());

        Optional<RoomAttributes>     roomAttrs    = roomAttributesRepository.findByRoom_Id(room.getId());
        Optional<BuildingAttributes> buildingAttrs = room.getBuilding() != null
                ? buildingAttributesRepository.findByBuilding_Id(room.getBuilding().getId())
                : Optional.empty();

        Map<Long, RoomAttributes>     rMap = roomAttrs.map(ra -> Map.of(room.getId(), ra)).orElse(Map.of());
        Map<Long, BuildingAttributes> bMap = buildingAttrs
                .map(ba -> Map.of(room.getBuilding().getId(), ba)).orElse(Map.of());

        return buildScoredRoom(student, application, room, null, rule, rMap, bMap,
                application != null ? application.getCreatedAt() : null);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Hard constraints checked:
     * gender compatibility, accessibility requirement vs room compliance.
     * (Capacity / availability / status checks are handled upstream in the allocation pipeline.)
     */
    @Override
    public boolean satisfiesHardConstraints(Student student, Room room) {
        Optional<RoomAttributes>     roomAttrs    = roomAttributesRepository.findByRoom_Id(room.getId());
        Optional<BuildingAttributes> buildingAttrs = room.getBuilding() != null
                ? buildingAttributesRepository.findByBuilding_Id(room.getBuilding().getId())
                : Optional.empty();

        Map<Long, RoomAttributes>     rMap = roomAttrs.map(ra -> Map.of(room.getId(), ra)).orElse(Map.of());
        Map<Long, BuildingAttributes> bMap = buildingAttrs
                .map(ba -> Map.of(room.getBuilding().getId(), ba)).orElse(Map.of());

        return satisfiesHardConstraintsFromMaps(student, room, rMap, bMap);
    }

    // ==================== Internal scoring ====================

    private ScoredRoom buildScoredRoom(Student student, Application application,
                                       Room room, Building preferred, AllocationRule rule,
                                       Map<Long, RoomAttributes> roomAttrMap,
                                       Map<Long, BuildingAttributes> buildingAttrMap,
                                       LocalDateTime thisAppCreatedAt) {

        RoomAttributes     roomAttrs    = roomAttrMap.get(room.getId());
        BuildingAttributes buildingAttrs = room.getBuilding() != null
                ? buildingAttrMap.get(room.getBuilding().getId()) : null;

        // --- FCFS component ---
        //
        // This term is INERT and rule.fcfsWeight cannot influence any ranking. The score is the
        // same constant for every room of every student, so multiplying it by the weight shifts
        // every candidate equally and changes no order (GH-140).
        //
        // That is structural rather than a missing calculation. This method scores rooms for ONE
        // student and never sees the other applicants, while first-come-first-served is a property
        // of the application relative to the queue. It can only rank applicants, which needs pool
        // context this strategy is not given. The comment here previously claimed the ordering was
        // handled at the scoreAvailableRooms level; it is not, and was not.
        //
        // Making the weights rank applicants is filed separately. Note the arrival term would then
        // compete with bursary, university and accessibility bonuses that are each the same
        // magnitude as the entire arrival span, so one flag would outrank the whole queue.
        BigDecimal fcfsScore           = FCFS_MAX_SCORE;
        BigDecimal weightedFcfs        = fcfsScore.multiply(rule.getFcfsWeight());

        // --- University preference component ---
        BigDecimal univScore           = calculateUniversityPrefScore(student, rule);
        BigDecimal weightedUniv        = univScore.multiply(rule.getUniversityPrefWeight());

        // --- NSFAS priority component ---
        BigDecimal nsfasScore          = calculateNsfasScore(student, application);
        BigDecimal weightedNsfas       = nsfasScore.multiply(rule.getNsfasPriorityWeight());

        // --- Accessibility match component ---
        BigDecimal accessibilityScore  = calculateAccessibilityScore(student, application, roomAttrs);
        BigDecimal weightedAccessibility = accessibilityScore.multiply(rule.getAccessibilityWeight());

        BigDecimal totalScore = weightedFcfs
                .add(weightedUniv)
                .add(weightedNsfas)
                .add(weightedAccessibility)
                .setScale(4, RoundingMode.HALF_UP);

        long activeAllocations = bedsTaken(room, StayPeriod.of(application));
        int  availableSpots    = room.getCapacity() - (int) activeAllocations;

        List<String> warnings = buildWarnings(student, application, room, roomAttrs, totalScore);

        return ScoredRoom.builder()
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .capacity(room.getCapacity())
                .currentOccupancy((int) activeAllocations)
                .availableSpots(availableSpots)
                .buildingId(room.getBuilding() != null ? room.getBuilding().getId() : null)
                .buildingName(room.getBuilding() != null ? room.getBuilding().getName() : null)
                .totalScore(totalScore)
                .preferenceMatchScore(weightedFcfs.setScale(4, RoundingMode.HALF_UP))
                .desirabilityScore(weightedUniv.add(weightedNsfas).setScale(4, RoundingMode.HALF_UP))
                .accessibilityScore(weightedAccessibility.setScale(4, RoundingMode.HALF_UP))
                .warnings(warnings)
                .meetsHardConstraints(
                        satisfiesHardConstraintsFromMaps(student, room, roomAttrMap, buildingAttrMap))
                .isPreferredBuilding(isPreferredBuilding(room, preferred, application))
                .isAccessibilityCompliant(
                        roomAttrs != null && Boolean.TRUE.equals(roomAttrs.getAccessibilityCompliant()))
                .applicationCreatedAt(thisAppCreatedAt)
                .genderRestriction(effectiveGenderRestriction(roomAttrs, buildingAttrs))
                .desirabilityRating(effectiveDesirability(buildingAttrs))
                .build();
    }

    // ==================== Score component calculators ====================

    /**
     * University preference score: 100 if the student's institution is in the rule's
     * {@code priorityInstitutionIds} list; 0 otherwise (or if no list is configured).
     */
    BigDecimal calculateUniversityPrefScore(Student student, AllocationRule rule) {
        String priorityIds = rule.getPriorityInstitutionIds();
        if (priorityIds == null || priorityIds.isBlank()) {
            return BigDecimal.ZERO;
        }
        Institution institution = student.getInstitution();
        if (institution == null || institution.getId() == null) {
            return BigDecimal.ZERO;
        }
        Set<String> ids = Arrays.stream(priorityIds.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
        return ids.contains(institution.getId().toString())
                ? UNIVERSITY_PREF_BONUS : BigDecimal.ZERO;
    }

    /**
     * NSFAS priority score: 100 if the student/application indicates confirmed NSFAS funding;
     * 0 otherwise. Detection uses keyword matching on {@code specialRequirements}
     * (same pattern as legacy accessibility detection) until a dedicated DB field is added.
     */
    BigDecimal calculateNsfasScore(Student student, Application application) {
        // Check application-level flag first
        if (application != null && application.getSpecialRequirements() != null) {
            String appReqs = application.getSpecialRequirements().toLowerCase();
            if (appReqs.contains("nsfas") || appReqs.contains("financial aid") || appReqs.contains("bursary")) {
                return NSFAS_PRIORITY_BONUS;
            }
        }
        // Fallback: student profile
        String studentReqs = student.getSpecialRequirements();
        if (studentReqs != null) {
            String lower = studentReqs.toLowerCase();
            if (lower.contains("nsfas") || lower.contains("financial aid") || lower.contains("bursary")) {
                return NSFAS_PRIORITY_BONUS;
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * Accessibility score: 100 if student requires accessibility and room is compliant;
     * 0 if required but non-compliant; 25 if compliant but not required (flexibility bonus).
     */
    BigDecimal calculateAccessibilityScore(Student student, Application application,
                                            RoomAttributes roomAttrs) {
        boolean requires  = hasAccessibilityRequirements(student, application);
        boolean compliant = roomAttrs != null && Boolean.TRUE.equals(roomAttrs.getAccessibilityCompliant());

        if (requires && compliant)  return ACCESSIBILITY_MATCH_BONUS;
        if (requires)               return BigDecimal.ZERO;
        if (compliant)              return new BigDecimal("25");
        return new BigDecimal("50");
    }

    // ==================== Hard-constraint helpers ====================

    private boolean satisfiesHardConstraintsFromMaps(Student student, Room room,
                                                      Map<Long, RoomAttributes> roomAttrMap,
                                                      Map<Long, BuildingAttributes> buildingAttrMap) {
        RoomAttributes     roomAttrs    = roomAttrMap.get(room.getId());
        BuildingAttributes buildingAttrs = room.getBuilding() != null
                ? buildingAttrMap.get(room.getBuilding().getId()) : null;

        // Gender
        if (!isGenderCompatible(student, roomAttrs, buildingAttrs)) {
            log.debug("strategy=OwnerConfigurable constraint=gender FAIL room={} student={}",
                    room.getRoomNumber(), student.getId());
            return false;
        }
        // Accessibility: if required, room must comply
        if (hasAccessibilityRequirements(student, null)
                && (roomAttrs == null || !Boolean.TRUE.equals(roomAttrs.getAccessibilityCompliant()))) {
            log.debug("strategy=OwnerConfigurable constraint=accessibility FAIL room={} student={}",
                    room.getRoomNumber(), student.getId());
            return false;
        }
        // Year of study
        if (!isYearOfStudyCompatible(student, roomAttrs)) {
            log.debug("strategy=OwnerConfigurable constraint=yearOfStudy FAIL room={} student={}",
                    room.getRoomNumber(), student.getId());
            return false;
        }
        // Institution
        if (!isBuildingInstitutionCompatible(student, room.getBuilding())) {
            log.debug("strategy=OwnerConfigurable constraint=institution FAIL room={} student={}",
                    room.getRoomNumber(), student.getId());
            return false;
        }
        // Prior decline
        if (roomAllocationRepository.hasStudentDeclinedRoom(student.getId(), room.getId())) {
            log.debug("strategy=OwnerConfigurable constraint=priorDecline FAIL room={} student={}",
                    room.getRoomNumber(), student.getId());
            return false;
        }
        return true;
    }

    private boolean isGenderCompatible(Student student, RoomAttributes roomAttrs, BuildingAttributes buildingAttrs) {
        String studentGender = student.getGender();
        if (studentGender == null || studentGender.isBlank()) return true;

        GenderRestriction roomRestriction = roomAttrs != null ? roomAttrs.getGenderRestriction() : null;
        if (roomRestriction != null && roomRestriction != GenderRestriction.ANY) {
            return isGenderMatch(studentGender, roomRestriction);
        }

        GenderRestriction buildingRestriction = buildingAttrs != null ? buildingAttrs.getGenderRestriction() : null;
        if (buildingRestriction != null && buildingRestriction != GenderRestriction.ANY) {
            if (buildingAttrs.getAllowMixedGender() != null && buildingAttrs.getAllowMixedGender()) return true;
            return isGenderMatch(studentGender, buildingRestriction);
        }
        return true;
    }

    private boolean isGenderMatch(String studentGender, GenderRestriction restriction) {
        if (restriction == GenderRestriction.ANY) return true;
        String norm = studentGender.toUpperCase().trim();
        return switch (restriction) {
            case MALE_ONLY   -> norm.equals("MALE")   || norm.equals("M");
            case FEMALE_ONLY -> norm.equals("FEMALE") || norm.equals("F");
            default          -> true;
        };
    }

    private boolean isYearOfStudyCompatible(Student student, RoomAttributes roomAttrs) {
        if (roomAttrs == null) return true;
        Integer minYear = roomAttrs.getMinYearOfStudy();
        Integer maxYear = roomAttrs.getMaxYearOfStudy();
        if (minYear == null && maxYear == null) return true;

        Integer studentYear = null;
        if (student.getYearOfStudy() != null && student.getYearOfStudy().getYearOfStudy() != null) {
            try {
                studentYear = Integer.parseInt(student.getYearOfStudy().getYearOfStudy());
            } catch (NumberFormatException e) {
                log.warn("strategy=OwnerConfigurable cannot parse yearOfStudy={}",
                        student.getYearOfStudy().getYearOfStudy());
            }
        }
        if (studentYear == null) return true;
        if (minYear != null && studentYear < minYear) return false;
        if (maxYear != null && studentYear > maxYear) return false;
        return true;
    }

    private boolean isBuildingInstitutionCompatible(Student student, Building building) {
        if (building == null) return true;
        if (building.getInstitutions() == null || building.getInstitutions().isEmpty()) return true;
        Institution inst = student.getInstitution();
        if (inst == null) return true;
        return building.getInstitutions().stream().anyMatch(i -> i.getId().equals(inst.getId()));
    }

    private boolean hasAccessibilityRequirements(Student student, Application application) {
        if (application != null && Boolean.TRUE.equals(application.getRequiresAccessibility())) return true;
        String reqs = student.getSpecialRequirements();
        if (reqs == null || reqs.isBlank()) return false;
        String lower = reqs.toLowerCase();
        return lower.contains("wheelchair") || lower.contains("accessible")
                || lower.contains("mobility") || lower.contains("disability")
                || lower.contains("handicap");
    }

    // ==================== Utility helpers ====================

    /**
     * Rooms open for offers with a free bed for {@code stay}. Any status but MAINTENANCE: FULL and
     * OCCUPIED describe today (GH-496), and a room full today can still be free for next year.
     */
    private List<Room> getAvailableRooms(StayPeriod stay) {
        return roomRepository.findAll().stream()
                .filter(room -> room.getStatus() != null && !"MAINTENANCE".equals(room.getStatus()))
                .filter(room -> bedsTaken(room, stay) < room.getCapacity())
                .collect(Collectors.toList());
    }

    /** Beds taken in the room during the stay, by planned dates. */
    private long bedsTaken(Room room, StayPeriod stay) {
        return roomAllocationRepository.countHoldingBeds(room.getId(), stay.from(), stay.until());
    }

    AllocationRule getLatestRuleForBuilding(Long buildingId) {
        if (buildingId != null) {
            List<AllocationRule> rules = allocationRuleRepository.findByBuilding_IdAndActiveTrue(buildingId);
            if (!rules.isEmpty()) {
                return rules.stream().max(Comparator.comparing(AllocationRule::getPriority))
                        .orElse(rules.get(0));
            }
        }
        List<AllocationRule> globals = allocationRuleRepository.findByBuildingIsNullAndActiveTrue();
        if (!globals.isEmpty()) {
            return globals.stream().max(Comparator.comparing(AllocationRule::getPriority))
                    .orElse(globals.get(0));
        }
        return createDefaultRule();
    }

    private AllocationRule createDefaultRule() {
        return AllocationRule.builder()
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

    private boolean isPreferredBuilding(Room room, Building explicit, Application application) {
        // Check explicit override first
        if (explicit != null && room.getBuilding() != null) {
            return room.getBuilding().getId().equals(explicit.getId());
        }
        if (room.getBuilding() == null) return false;
        // Check multi-preference list
        if (application != null && application.getBuildingPreferences() != null
                && !application.getBuildingPreferences().isEmpty()) {
            return application.getBuildingPreferences().stream()
                    .anyMatch(p -> p.getBuilding() != null
                            && p.getBuilding().getId().equals(room.getBuilding().getId()));
        }
        // Fall back to legacy single preference
        if (application != null && application.getPreferredBuilding() != null) {
            return room.getBuilding().getId().equals(application.getPreferredBuilding().getId());
        }
        return false;
    }

    private String effectiveGenderRestriction(RoomAttributes roomAttrs, BuildingAttributes buildingAttrs) {
        if (roomAttrs != null && roomAttrs.getGenderRestriction() != null)
            return roomAttrs.getGenderRestriction().name();
        if (buildingAttrs != null && buildingAttrs.getGenderRestriction() != null)
            return buildingAttrs.getGenderRestriction().name();
        return GenderRestriction.ANY.name();
    }

    private Integer effectiveDesirability(BuildingAttributes buildingAttrs) {
        return buildingAttrs != null ? buildingAttrs.getDesirabilityScore() : null;
    }

    private List<String> buildWarnings(Student student, Application application, Room room,
                                        RoomAttributes roomAttrs, BigDecimal totalScore) {
        List<String> warnings = new ArrayList<>();
        if (application != null && room.getBuilding() != null) {
            boolean hasPreference = false;
            boolean matchesPreference = false;
            // Check multi-preference list first
            if (application.getBuildingPreferences() != null
                    && !application.getBuildingPreferences().isEmpty()) {
                hasPreference = true;
                matchesPreference = application.getBuildingPreferences().stream()
                        .anyMatch(p -> p.getBuilding() != null
                                && p.getBuilding().getId().equals(room.getBuilding().getId()));
            } else if (application.getPreferredBuilding() != null) {
                // Fall back to legacy single preference
                hasPreference = true;
                matchesPreference = room.getBuilding().getId()
                        .equals(application.getPreferredBuilding().getId());
            }
            if (hasPreference && !matchesPreference) {
                warnings.add("BUILDING_MISMATCH");
            }
        }
        if (totalScore.compareTo(SUBOPTIMAL_THRESHOLD) < 0) {
            warnings.add("SUBOPTIMAL_DESIRABILITY");
        }
        if (application != null && Boolean.TRUE.equals(application.getRequiresAccessibility())) {
            if (roomAttrs == null || !Boolean.TRUE.equals(roomAttrs.getAccessibilityCompliant())) {
                warnings.add("ACCESSIBILITY_MISMATCH");
            }
        }
        return warnings;
    }
}
