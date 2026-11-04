package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {

    /**
     * The one current year. {@code idx_academic_year_current} allows a single current row across
     * all tenants, and every application takes it whichever company it goes to, so this must find
     * it for any caller. Uses a native query to bypass the Hibernate tenantFilter.
     */
    @Query(value = "SELECT * FROM academic_year WHERE is_current = TRUE", nativeQuery = true)
    Optional<AcademicYear> findByIsCurrentTrue();

    /**
     * What a company admin sees: platform years, the company's own, and the current year whoever
     * owns it. Uses a native query to bypass the Hibernate tenantFilter, which would otherwise
     * hide everything but the company's own rows.
     */
    @Query(value = "SELECT * FROM academic_year WHERE tenant_id IN (0, :tenantId) OR is_current = TRUE "
            + "ORDER BY start_date DESC", nativeQuery = true)
    List<AcademicYear> findVisibleToTenant(@Param("tenantId") Long tenantId);

    Optional<AcademicYear> findByName(String name);

    boolean existsByName(String name);

    @Modifying
    @Query("UPDATE AcademicYear a SET a.isCurrent = false WHERE a.isCurrent = true")
    void clearCurrentAcademicYear();
}
