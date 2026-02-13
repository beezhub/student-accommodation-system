package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuildingRepository extends JpaRepository<Building, Long>, JpaSpecificationExecutor<Building> {
    
    @Query("SELECT b FROM Building b JOIN b.institutions i WHERE i.id = :institutionId")
    List<Building> findByInstitutionId(@Param("institutionId") Long institutionId);
}