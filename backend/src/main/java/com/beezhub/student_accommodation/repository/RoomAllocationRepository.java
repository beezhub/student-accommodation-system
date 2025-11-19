package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomAllocationRepository extends JpaRepository<RoomAllocation, Long>, JpaSpecificationExecutor<RoomAllocation> {

    // Check if student has any active allocation
    @Query("SELECT COUNT(ra) > 0 FROM room_allocation ra WHERE ra.student.id = :studentId AND ra.status = 'ACTIVE'")
    boolean existsActiveAllocationByStudentId(@Param("studentId") Long studentId);

    // Check if application has already been allocated
    @Query("SELECT COUNT(ra) > 0 FROM room_allocation ra WHERE ra.application.id = :applicationId")
    boolean existsByApplicationId(@Param("applicationId") Long applicationId);

    // Count active allocations for a room
    @Query("SELECT COUNT(ra) FROM room_allocation ra WHERE ra.room.id = :roomId AND ra.status = 'ACTIVE'")
    long countActiveAllocationsByRoomId(@Param("roomId") Long roomId);

    // Find active allocation by student ID
    @Query("SELECT ra FROM room_allocation ra WHERE ra.student.id = :studentId AND ra.status = 'ACTIVE'")
    Optional<RoomAllocation> findActiveAllocationByStudentId(@Param("studentId") Long studentId);

    // Find allocation by application ID
    @Query("SELECT ra FROM room_allocation ra WHERE ra.application.id = :applicationId")
    Optional<RoomAllocation> findByApplicationId(@Param("applicationId") Long applicationId);
}