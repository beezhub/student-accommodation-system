package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {

    Optional<AcademicYear> findByIsCurrentTrue();

    Optional<AcademicYear> findByName(String name);

    boolean existsByName(String name);

    @Modifying
    @Query("UPDATE AcademicYear a SET a.isCurrent = false WHERE a.isCurrent = true")
    void clearCurrentAcademicYear();
}
