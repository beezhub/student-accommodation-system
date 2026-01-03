package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long>,
        JpaSpecificationExecutor<ActivityLog> {

    /**
     * Find logs by user ID with pagination
     */
    Page<ActivityLog> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    /**
     * Find logs by action type with pagination
     */
    Page<ActivityLog> findByActionOrderByTimestampDesc(String action, Pageable pageable);

    /**
     * Find logs by entity type with pagination
     */
    Page<ActivityLog> findByEntityTypeOrderByTimestampDesc(String entityType, Pageable pageable);

    /**
     * Find logs within a date range
     */
    @Query("SELECT a FROM ActivityLog a WHERE a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    Page<ActivityLog> findByTimestampBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * Find logs for a specific entity
     */
    Page<ActivityLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(
            String entityType,
            Long entityId,
            Pageable pageable
    );

    /**
     * Count logs by action type
     */
    long countByAction(String action);

    /**
     * Count logs by entity type
     */
    long countByEntityType(String entityType);

    /**
     * Delete old logs (for cleanup/archival)
     */
    @Query("DELETE FROM ActivityLog a WHERE a.timestamp < :cutoffDate")
    void deleteOldLogs(@Param("cutoffDate") LocalDateTime cutoffDate);
}
