package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.BuildingImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BuildingImageRepository extends JpaRepository<BuildingImage, Long> {

    List<BuildingImage> findByBuilding_IdOrderByDisplayOrderAsc(Long buildingId);

    Optional<BuildingImage> findByBuilding_IdAndPrimaryTrue(Long buildingId);

    boolean existsByObjectPath(String objectPath);
}
