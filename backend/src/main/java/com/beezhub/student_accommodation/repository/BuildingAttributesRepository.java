package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.BuildingAttributes;
import com.beezhub.student_accommodation.model.enums.GenderRestriction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BuildingAttributesRepository extends JpaRepository<BuildingAttributes, Long> {

    Optional<BuildingAttributes> findByBuilding_Id(Long buildingId);

    List<BuildingAttributes> findByBuilding_IdIn(Collection<Long> buildingIds);

    List<BuildingAttributes> findByGenderRestriction(GenderRestriction genderRestriction);

    List<BuildingAttributes> findByAllowMixedGenderTrue();

    List<BuildingAttributes> findByDesirabilityScoreGreaterThanEqual(Integer minScore);
}
