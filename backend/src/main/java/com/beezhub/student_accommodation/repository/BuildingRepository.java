package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildingRepository extends JpaRepository<Building, Long> {
    Building findByName(String name);
}
