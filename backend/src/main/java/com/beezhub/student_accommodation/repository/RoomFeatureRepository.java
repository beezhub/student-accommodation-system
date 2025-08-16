package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.RoomFeature;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomFeatureRepository extends JpaRepository<RoomFeature, Long> {
    RoomFeature findByName(String name);
}
