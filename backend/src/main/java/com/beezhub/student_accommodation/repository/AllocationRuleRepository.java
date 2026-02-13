package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.AllocationRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AllocationRuleRepository extends JpaRepository<AllocationRule, Long> {

    List<AllocationRule> findByBuilding_Id(Long buildingId);

    List<AllocationRule> findByActiveTrue();

    List<AllocationRule> findByBuilding_IdAndActiveTrue(Long buildingId);

    List<AllocationRule> findByActiveTrueOrderByPriorityDesc();

    Optional<AllocationRule> findByBuilding_IdAndRuleName(Long buildingId, String ruleName);

    List<AllocationRule> findByBuildingIsNullAndActiveTrue();

    // Pagination support
    Page<AllocationRule> findByBuilding_Id(Long buildingId, Pageable pageable);

    Page<AllocationRule> findByBuildingIsNull(Pageable pageable);

    // Check for duplicate rule name (for validation)
    boolean existsByBuilding_IdAndRuleName(Long buildingId, String ruleName);

    boolean existsByBuildingIsNullAndRuleName(String ruleName);
}
