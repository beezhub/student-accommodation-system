package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.ApplicationBuildingPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationBuildingPreferenceRepository extends JpaRepository<ApplicationBuildingPreference, Long> {

    List<ApplicationBuildingPreference> findByApplication_IdOrderByPreferenceOrderAsc(Long applicationId);
}
