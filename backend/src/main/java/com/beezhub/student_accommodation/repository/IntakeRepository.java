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

    /**
     * Intakes of one type in a year that start on or after {@code from}, earliest first (GH-498):
     * the candidates for extending a stay that ends on {@code from}.
     */
    List<Intake> findByAcademicYear_IdAndIntakeTypeAndStayStartDateGreaterThanEqualOrderByStayStartDateAsc(
            Long academicYearId, com.beezhub.student_accommodation.model.enums.IntakeType intakeType,
            java.time.LocalDate from);
}
