package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.StagedAllocation;
import com.beezhub.student_accommodation.model.enums.StagedAllocationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StagedAllocationRepository extends JpaRepository<StagedAllocation, Long> {

    List<StagedAllocation> findByBatchId(String batchId);

    List<StagedAllocation> findByStatus(StagedAllocationStatus status);

    List<StagedAllocation> findByStudent_Id(Long studentId);

    List<StagedAllocation> findByRoom_Id(Long roomId);

    List<StagedAllocation> findByApplication_Id(Long applicationId);

    List<StagedAllocation> findByBatchIdAndStatus(String batchId, StagedAllocationStatus status);

    Optional<StagedAllocation> findByStudent_IdAndStatus(Long studentId, StagedAllocationStatus status);

    List<StagedAllocation> findByBatchIdOrderByScoreDesc(String batchId);

    long countByBatchIdAndStatus(String batchId, StagedAllocationStatus status);

    List<StagedAllocation> findByStatusOrderByCreatedAtDesc(StagedAllocationStatus status);

    // Pagination support
    Page<StagedAllocation> findByBatchIdAndStatus(String batchId, StagedAllocationStatus status, Pageable pageable);

    Page<StagedAllocation> findByBatchId(String batchId, Pageable pageable);

    Page<StagedAllocation> findByStatus(StagedAllocationStatus status, Pageable pageable);
}
