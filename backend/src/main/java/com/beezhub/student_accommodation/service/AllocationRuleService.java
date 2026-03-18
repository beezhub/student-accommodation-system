package com.beezhub.student_accommodation.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AllocationRuleService {

    private final AllocationRuleRepository allocationRuleRepository;
    private final BuildingRepository buildingRepository;
    private final AppUserRepository appUserRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public PageResponse<AllocationRuleResponse> getRulesByBuilding(Long buildingId, Pageable pageable) {
        Page<AllocationRule> page;
        if (buildingId == null) {
            page = allocationRuleRepository.findByBuildingIsNull(pageable);
        } else {
            if (!buildingRepository.existsById(buildingId)) {
                throw new ResourceNotFoundException("Building not found with id: " + buildingId);
            }
            page = allocationRuleRepository.findByBuilding_Id(buildingId, pageable);
        }
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public AllocationRuleResponse getRuleById(Long id) {
        AllocationRule rule = allocationRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation rule not found with id: " + id));
        return toResponse(rule);
    }

    @Transactional
    public AllocationRuleResponse createRule(AllocationRuleRequest request, String adminEmail) {
        validateWeights(request);

        if (request.getBuildingId() != null) {
            if (allocationRuleRepository.existsByBuilding_IdAndRuleName(request.getBuildingId(), request.getRuleName())) {
                throw new DuplicateResourceException("Rule with name '" + request.getRuleName()
                        + "' already exists for building id: " + request.getBuildingId());
            }
        } else {
            if (allocationRuleRepository.existsByBuildingIsNullAndRuleName(request.getRuleName())) {
                throw new DuplicateResourceException("Global rule with name '" + request.getRuleName() + "' already exists");
            }
        }

        Building building = null;
        if (request.getBuildingId() != null) {
            building = buildingRepository.findById(request.getBuildingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Building not found with id: " + request.getBuildingId()));
        }

        AppUser createdBy = appUserRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + adminEmail));

        AllocationRule rule = AllocationRule.builder()
                .building(building)
                .ruleName(request.getRuleName())
                .preferenceMatchWeight(request.getPreferenceMatchWeight())
                .desirabilityWeight(request.getDesirabilityWeight())
                .accessibilityWeight(request.getAccessibilityWeight())
                .active(request.getActive() != null ? request.getActive() : true)
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .createdBy(createdBy)
                .build();

        return toResponse(allocationRuleRepository.save(rule));
    }

    @Transactional
    public AllocationRuleResponse updateRule(Long id, AllocationRuleRequest request, String adminEmail) {
        AllocationRule rule = allocationRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation rule not found with id: " + id));

        // Only check duplicate name if name changed
        if (!rule.getRuleName().equals(request.getRuleName())) {
            if (request.getBuildingId() != null) {
                if (allocationRuleRepository.existsByBuilding_IdAndRuleName(request.getBuildingId(), request.getRuleName())) {
                    // allow if it's the same rule
                    allocationRuleRepository.findByBuilding_IdAndRuleName(request.getBuildingId(), request.getRuleName())
                            .filter(existing -> !existing.getId().equals(id))
                            .ifPresent(existing -> {
                                throw new DuplicateResourceException("Rule with name '" + request.getRuleName()
                                        + "' already exists for building id: " + request.getBuildingId());
                            });
                    // If findByBuilding_IdAndRuleName returns empty, there's a stale check — still treat as duplicate
                    if (allocationRuleRepository.findByBuilding_IdAndRuleName(request.getBuildingId(), request.getRuleName()).isEmpty()) {
                        throw new DuplicateResourceException("Rule with name '" + request.getRuleName()
                                + "' already exists for building id: " + request.getBuildingId());
                    }
                }
            }
        }

        if (request.getBuildingId() != null) {
            Building building = buildingRepository.findById(request.getBuildingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Building not found with id: " + request.getBuildingId()));
            rule.setBuilding(building);
        } else {
            rule.setBuilding(null);
        }

        rule.setRuleName(request.getRuleName());
        rule.setPreferenceMatchWeight(request.getPreferenceMatchWeight());
        rule.setDesirabilityWeight(request.getDesirabilityWeight());
        rule.setAccessibilityWeight(request.getAccessibilityWeight());
        if (request.getActive() != null) {
            rule.setActive(request.getActive());
        }
        if (request.getPriority() != null) {
            rule.setPriority(request.getPriority());
        }

        return toResponse(allocationRuleRepository.save(rule));
    }

    @Transactional
    public void deleteRule(Long id, String adminEmail) {
        AllocationRule rule = allocationRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation rule not found with id: " + id));
        rule.setActive(false);
        allocationRuleRepository.save(rule);
    }

    @Transactional
    public AllocationRuleResponse activateRule(Long id, String adminEmail) {
        AllocationRule rule = allocationRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation rule not found with id: " + id));
        rule.setActive(true);
        return toResponse(allocationRuleRepository.save(rule));
    }

    // --- Mapping helpers ---

    private AllocationRuleResponse toResponse(AllocationRule rule) {
        return AllocationRuleResponse.builder()
                .id(rule.getId())
                .buildingId(rule.getBuilding() != null ? rule.getBuilding().getId() : null)
                .buildingName(rule.getBuilding() != null ? rule.getBuilding().getName() : null)
                .ruleName(rule.getRuleName())
                .preferenceMatchWeight(rule.getPreferenceMatchWeight())
                .desirabilityWeight(rule.getDesirabilityWeight())
                .accessibilityWeight(rule.getAccessibilityWeight())
                .fcfsWeight(rule.getFcfsWeight())
                .nsfasPriorityWeight(rule.getNsfasPriorityWeight())
                .universityPrefWeight(rule.getUniversityPrefWeight())
                .priorityInstitutionIds(rule.getPriorityInstitutionIds())
                .active(rule.getActive())
                .priority(rule.getPriority())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }

    private PageResponse<AllocationRuleResponse> toPageResponse(Page<AllocationRule> page) {
        return PageResponse.<AllocationRuleResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }

    private void validateWeights(AllocationRuleRequest request) {
        BigDecimal total = BigDecimal.ZERO;
        if (request.getPreferenceMatchWeight() != null) {
            total = total.add(request.getPreferenceMatchWeight());
        }
        if (request.getDesirabilityWeight() != null) {
            total = total.add(request.getDesirabilityWeight());
        }
        if (request.getAccessibilityWeight() != null) {
            total = total.add(request.getAccessibilityWeight());
        }
        if (total.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(
                    "Sum of preferenceMatchWeight, desirabilityWeight, and accessibilityWeight must not exceed 100. Got: " + total);
        }
    }
}
