package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.aspect.LogActivity;
import com.beezhub.student_accommodation.exceptions.DuplicateResourceException;
import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.model.dto.AllocationRuleRequest;
import com.beezhub.student_accommodation.model.dto.AllocationRuleResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.entity.AllocationRule;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.repository.AllocationRuleRepository;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.util.PageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
@Deprecated(since = "2.0", forRemoval = true)
public class AllocationRuleService {

    private final AllocationRuleRepository allocationRuleRepository;
    private final BuildingRepository buildingRepository;
    private final AppUserRepository appUserRepository;
    private final AuthService authService;

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /**
     * Gets all allocation rules for a building or global rules (buildingId = null).
     *
     * @param buildingId The building ID (null for global rules)
     * @param pageable   Pagination parameters
     * @return PageResponse of AllocationRuleResponse
     */
    public PageResponse<AllocationRuleResponse> getRulesByBuilding(Long buildingId, Pageable pageable) {
        log.info("Getting allocation rules for buildingId: {}", buildingId);

        Page<AllocationRule> rulesPage;
        if (buildingId == null) {
            rulesPage = allocationRuleRepository.findByBuildingIsNull(pageable);
        } else {
            // Verify building exists
            if (!buildingRepository.existsById(buildingId)) {
                throw new ResourceNotFoundException("Building not found with ID: " + buildingId);
            }
            rulesPage = allocationRuleRepository.findByBuilding_Id(buildingId, pageable);
        }

        Page<AllocationRuleResponse> responsePage = rulesPage.map(this::mapToResponse);
        log.info("Found {} rules for buildingId: {}", rulesPage.getTotalElements(), buildingId);

        return PageMapper.toPageResponse(responsePage);
    }

    /**
     * Gets a single allocation rule by ID.
     *
     * @param ruleId The rule ID
     * @return AllocationRuleResponse
     */
    public AllocationRuleResponse getRuleById(Long ruleId) {
        log.info("Getting allocation rule with ID: {}", ruleId);

        AllocationRule rule = allocationRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation rule not found with ID: " + ruleId));

        return mapToResponse(rule);
    }

    /**
     * Creates a new allocation rule.
     *
     * @param request  The rule request
     * @param username The admin username
     * @return AllocationRuleResponse
     */
    @Transactional
    @LogActivity(action = "CREATE", entityType = "ALLOCATION_RULE", description = "Created allocation rule {0}", entityIdParamIndex = -1)
    public AllocationRuleResponse createRule(AllocationRuleRequest request, String username) {
        log.info("Creating allocation rule '{}' for buildingId: {}", request.getRuleName(), request.getBuildingId());

        // Validate weights sum to 100%
        validateWeightsSum(request);

        // Check for duplicate rule name
        checkDuplicateRuleName(request.getBuildingId(), request.getRuleName(), null);

        // Get building if specified
        Building building = null;
        if (request.getBuildingId() != null) {
            building = buildingRepository.findById(request.getBuildingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Building not found with ID: " + request.getBuildingId()));
        }

        // Get current user
        AppUser admin = appUserRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Create rule
        AllocationRule rule = AllocationRule.builder()
                .building(building)
                .ruleName(request.getRuleName())
                .preferenceMatchWeight(request.getPreferenceMatchWeight() != null ?
                        request.getPreferenceMatchWeight() : BigDecimal.ONE)
                .desirabilityWeight(request.getDesirabilityWeight() != null ?
                        request.getDesirabilityWeight() : BigDecimal.ONE)
                .accessibilityWeight(request.getAccessibilityWeight() != null ?
                        request.getAccessibilityWeight() : BigDecimal.ONE)
                .active(request.getActive() != null ? request.getActive() : true)
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .createdBy(admin)
                .build();

        AllocationRule savedRule = allocationRuleRepository.save(rule);
        log.info("Created allocation rule with ID: {}", savedRule.getId());

        return mapToResponse(savedRule);
    }

    /**
     * Updates an existing allocation rule.
     *
     * @param ruleId   The rule ID
     * @param request  The rule request
     * @param username The admin username
     * @return AllocationRuleResponse
     */
    @Transactional
    @LogActivity(action = "UPDATE", entityType = "ALLOCATION_RULE", description = "Updated allocation rule {0}", entityIdParamIndex = 0)
    public AllocationRuleResponse updateRule(Long ruleId, AllocationRuleRequest request, String username) {
        log.info("Updating allocation rule with ID: {}", ruleId);

        AllocationRule rule = allocationRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation rule not found with ID: " + ruleId));

        // Validate weights sum to 100%
        validateWeightsSum(request);

        // Check for duplicate rule name (excluding current rule)
        if (!rule.getRuleName().equals(request.getRuleName())) {
            checkDuplicateRuleName(request.getBuildingId(), request.getRuleName(), ruleId);
        }

        // Update building if changed
        if (request.getBuildingId() != null) {
            Building building = buildingRepository.findById(request.getBuildingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Building not found with ID: " + request.getBuildingId()));
            rule.setBuilding(building);
        } else {
            rule.setBuilding(null);
        }

        // Update fields
        rule.setRuleName(request.getRuleName());

        if (request.getPreferenceMatchWeight() != null) {
            rule.setPreferenceMatchWeight(request.getPreferenceMatchWeight());
        }
        if (request.getDesirabilityWeight() != null) {
            rule.setDesirabilityWeight(request.getDesirabilityWeight());
        }
        if (request.getAccessibilityWeight() != null) {
            rule.setAccessibilityWeight(request.getAccessibilityWeight());
        }
        if (request.getActive() != null) {
            rule.setActive(request.getActive());
        }
        if (request.getPriority() != null) {
            rule.setPriority(request.getPriority());
        }

        AllocationRule updatedRule = allocationRuleRepository.save(rule);
        log.info("Updated allocation rule with ID: {}", updatedRule.getId());

        return mapToResponse(updatedRule);
    }

    /**
     * Soft deletes an allocation rule by setting active = false.
     *
     * @param ruleId   The rule ID
     * @param username The admin username
     */
    @Transactional
    @LogActivity(action = "DELETE", entityType = "ALLOCATION_RULE", description = "Deactivated allocation rule {0}", entityIdParamIndex = 0)
    public void deleteRule(Long ruleId, String username) {
        log.info("Soft deleting allocation rule with ID: {}", ruleId);

        AllocationRule rule = allocationRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation rule not found with ID: " + ruleId));

        rule.setActive(false);
        allocationRuleRepository.save(rule);

        log.info("Soft deleted (deactivated) allocation rule with ID: {}", ruleId);
    }

    /**
     * Activates an allocation rule by setting active = true.
     *
     * @param ruleId   The rule ID
     * @param username The admin username
     * @return AllocationRuleResponse
     */
    @Transactional
    @LogActivity(action = "UPDATE", entityType = "ALLOCATION_RULE", description = "Activated allocation rule {0}", entityIdParamIndex = 0)
    public AllocationRuleResponse activateRule(Long ruleId, String username) {
        log.info("Activating allocation rule with ID: {}", ruleId);

        AllocationRule rule = allocationRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation rule not found with ID: " + ruleId));

        rule.setActive(true);
        AllocationRule updatedRule = allocationRuleRepository.save(rule);

        log.info("Activated allocation rule with ID: {}", updatedRule.getId());

        return mapToResponse(updatedRule);
    }

    /**
     * Validates that weights sum to 100%.
     */
    private void validateWeightsSum(AllocationRuleRequest request) {
        BigDecimal preferenceWeight = request.getPreferenceMatchWeight() != null ?
                request.getPreferenceMatchWeight() : BigDecimal.ONE;
        BigDecimal desirabilityWeight = request.getDesirabilityWeight() != null ?
                request.getDesirabilityWeight() : BigDecimal.ONE;
        BigDecimal accessibilityWeight = request.getAccessibilityWeight() != null ?
                request.getAccessibilityWeight() : BigDecimal.ONE;

        BigDecimal sum = preferenceWeight.add(desirabilityWeight).add(accessibilityWeight);

        if (sum.compareTo(HUNDRED) != 0) {
            throw new IllegalArgumentException(
                    "Weights must sum to 100%. Current sum: " + sum +
                    " (preference: " + preferenceWeight +
                    ", desirability: " + desirabilityWeight +
                    ", accessibility: " + accessibilityWeight + ")");
        }
    }

    /**
     * Checks for duplicate rule name within the same building scope.
     */
    private void checkDuplicateRuleName(Long buildingId, String ruleName, Long excludeRuleId) {
        boolean exists;
        if (buildingId == null) {
            exists = allocationRuleRepository.existsByBuildingIsNullAndRuleName(ruleName);
        } else {
            exists = allocationRuleRepository.existsByBuilding_IdAndRuleName(buildingId, ruleName);
        }

        if (exists) {
            // If updating, check if it's the same rule
            if (excludeRuleId != null) {
                AllocationRule existingRule = allocationRuleRepository
                        .findByBuilding_IdAndRuleName(buildingId, ruleName)
                        .orElse(null);
                if (existingRule != null && existingRule.getId().equals(excludeRuleId)) {
                    return; // Same rule, no conflict
                }
            }
            throw new DuplicateResourceException(
                    "A rule with name '" + ruleName + "' already exists for this building");
        }
    }

    /**
     * Maps AllocationRule entity to AllocationRuleResponse DTO.
     */
    private AllocationRuleResponse mapToResponse(AllocationRule rule) {
        return AllocationRuleResponse.builder()
                .id(rule.getId())
                .buildingId(rule.getBuilding() != null ? rule.getBuilding().getId() : null)
                .buildingName(rule.getBuilding() != null ? rule.getBuilding().getName() : null)
                .ruleName(rule.getRuleName())
                .preferenceMatchWeight(rule.getPreferenceMatchWeight())
                .desirabilityWeight(rule.getDesirabilityWeight())
                .accessibilityWeight(rule.getAccessibilityWeight())
                .active(rule.getActive())
                .priority(rule.getPriority())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .createdByName(rule.getCreatedBy() != null ?
                        rule.getCreatedBy().getFirstName() + " " + rule.getCreatedBy().getLastName() : null)
                .build();
    }
}
