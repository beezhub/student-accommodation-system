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

    /**
     * Buildings a student of one institution can apply to (GH-495): the company has a year that has
     * not ended for that institution, or an "All institutions" year, and the building lists the
     * institution or lists none. Pass {@code -1} for a student with no institution, which matches
     * only "All institutions" years and buildings that list none.
     *
     * <p>Native, so it reads every company's buildings and years whatever the caller's tenant filter.
     */
    @Query(value = "SELECT b.* FROM building b "
            + "WHERE EXISTS (SELECT 1 FROM academic_year y WHERE y.tenant_id = b.tenant_id "
            + "      AND y.end_date >= :today "
            + "      AND (y.institution_id IS NULL OR y.institution_id = :institutionId)) "
            + "  AND (NOT EXISTS (SELECT 1 FROM building_institution bi WHERE bi.building_id = b.id) "
            + "       OR EXISTS (SELECT 1 FROM building_institution bi "
            + "                  WHERE bi.building_id = b.id AND bi.institution_id = :institutionId)) "
            + "ORDER BY b.name", nativeQuery = true)
    List<Building> findTakingInstitution(@Param("institutionId") long institutionId,
                                         @Param("today") java.time.LocalDate today);

}