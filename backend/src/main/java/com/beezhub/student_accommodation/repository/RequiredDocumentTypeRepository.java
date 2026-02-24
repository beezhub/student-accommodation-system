package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.RequiredDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequiredDocumentTypeRepository extends JpaRepository<RequiredDocumentType, Long> {

    List<RequiredDocumentType> findByBuilding_IdAndIsActiveTrue(Long buildingId);

    List<RequiredDocumentType> findByBuilding_IdAndIsActiveTrueAndIsRequiredTrue(Long buildingId);
}
