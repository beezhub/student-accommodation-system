package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.Intake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface IntakeRepository extends JpaRepository<Intake, Long> {

    List<Intake> findByAcademicYear_IdOrderByStayStartDateAscNameAsc(Long academicYearId);

    /** The intakes of a page of years in one query, for the admin list. */
    List<Intake> findByAcademicYear_IdInOrderByStayStartDateAscNameAsc(Collection<Long> academicYearIds);

    boolean existsByAcademicYear_IdAndName(Long academicYearId, String name);

    boolean existsByAcademicYear_IdAndNameAndIdNot(Long academicYearId, String name, Long id);
}
