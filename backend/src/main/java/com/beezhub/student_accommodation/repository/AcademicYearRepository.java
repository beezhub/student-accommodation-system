package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.AcademicYear;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {

    /** One company's years, newest first. */
    Page<AcademicYear> findByTenantId(Long tenantId, Pageable pageable);

    /**
     * Name check scoped like {@code uq_academic_year_tenant_institution_name}. A null
     * {@code institutionId} is matched with {@code IS NULL}, so two "All institutions" years of
     * the same company cannot share a name.
     */
    boolean existsByTenantIdAndInstitutionIdAndName(Long tenantId, Long institutionId, String name);

    boolean existsByTenantIdAndInstitutionIdAndNameAndIdNot(Long tenantId, Long institutionId, String name, Long id);

    /**
     * A company's years whose dates overlap {@code [startDate, endDate]}, for the overlap rule.
     * The tenant is explicit, so the result is the same with or without the tenant filter.
     */
    @Query("SELECT a FROM AcademicYear a WHERE a.tenantId = :tenantId "
            + "AND a.startDate <= :endDate AND a.endDate >= :startDate ORDER BY a.startDate")
    List<AcademicYear> findOverlapping(@Param("tenantId") Long tenantId,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    /**
     * A company's years that have not ended by {@code today}: the candidates for the current year.
     * Native so the tenant filter does not hide platform years from a company admin when the
     * lookup falls back to them.
     */
    @Query(value = "SELECT * FROM academic_year WHERE tenant_id = :tenantId AND end_date >= :today "
            + "ORDER BY start_date", nativeQuery = true)
    List<AcademicYear> findNotEndedByTenant(@Param("tenantId") Long tenantId, @Param("today") LocalDate today);
}
