package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.CustomApplicationField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomApplicationFieldRepository extends JpaRepository<CustomApplicationField, Long> {

    List<CustomApplicationField> findByBuilding_IdAndIsActiveTrueOrderByDisplayOrderAsc(Long buildingId);
}
