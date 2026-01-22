package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.AllocationAuditLog;
import com.beezhub.student_accommodation.model.enums.AllocationAuditActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AllocationAuditLogRepository extends JpaRepository<AllocationAuditLog, Long> {

    List<AllocationAuditLog> findByAllocation_Id(Long allocationId);

    List<AllocationAuditLog> findByAdminUser_Id(Long adminUserId);

    List<AllocationAuditLog> findByActionType(AllocationAuditActionType actionType);

    List<AllocationAuditLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    Page<AllocationAuditLog> findByAllocation_IdOrderByCreatedAtDesc(Long allocationId, Pageable pageable);

    Page<AllocationAuditLog> findByAdminUser_IdOrderByCreatedAtDesc(Long adminUserId, Pageable pageable);

    List<AllocationAuditLog> findByConstraintBypassedIsNotNull();

    Page<AllocationAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByActionType(AllocationAuditActionType actionType);

    long countByCreatedAtAfter(LocalDateTime dateTime);
}
