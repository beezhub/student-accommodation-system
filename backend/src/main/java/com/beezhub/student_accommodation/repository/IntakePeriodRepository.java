package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.IntakePeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IntakePeriodRepository extends JpaRepository<IntakePeriod, Long> {

    List<IntakePeriod> findByBuilding_IdAndIsActiveTrue(Long buildingId);

    /**
     * Returns the active intake period for a building that is currently open at {@code now}.
     * Handles both FIXED (must fall within open_date..close_date) and ROLLING (always open).
     */
    @Query("""
        SELECT ip FROM IntakePeriod ip
        WHERE ip.building.id = :buildingId
          AND ip.isActive = true
          AND (
            ip.mode = 'ROLLING'
            OR (ip.mode = 'FIXED' AND ip.openDate <= :now AND ip.closeDate >= :now)
          )
        ORDER BY ip.createdAt DESC
        """)
    Optional<IntakePeriod> findActiveIntakePeriodForBuilding(
            @Param("buildingId") Long buildingId,
            @Param("now") LocalDateTime now);
}
