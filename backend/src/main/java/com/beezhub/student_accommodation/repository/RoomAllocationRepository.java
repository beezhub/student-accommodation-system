package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomAllocationRepository extends JpaRepository<RoomAllocation, Long>, JpaSpecificationExecutor<RoomAllocation> {

    @EntityGraph(attributePaths = {"student", "room", "room.building", "application"})
    @Override
    Page<RoomAllocation> findAll(Pageable pageable);

    // Find all allocations with eager loading of related entities
    @Query("SELECT DISTINCT ra FROM RoomAllocation ra " +
           "LEFT JOIN FETCH ra.student s " +
           "LEFT JOIN FETCH ra.room r " +
           "LEFT JOIN FETCH r.building b " +
           "LEFT JOIN FETCH ra.application a")
    List<RoomAllocation> findAllWithDetails();

    // Find allocation by ID with eager loading
    @Query("SELECT ra FROM RoomAllocation ra " +
           "LEFT JOIN FETCH ra.student s " +
           "LEFT JOIN FETCH ra.room r " +
           "LEFT JOIN FETCH r.building b " +
           "LEFT JOIN FETCH ra.application a " +
           "WHERE ra.id = :id")
    Optional<RoomAllocation> findByIdWithDetails(@Param("id") Long id);

    // Check if student has any active allocation
    @Query("SELECT COUNT(ra) > 0 FROM RoomAllocation ra WHERE ra.student.id = :studentId AND ra.status = 'ACTIVE'")
    boolean existsActiveAllocationByStudentId(@Param("studentId") Long studentId);

    // Check if application has already been allocated
    @Query("SELECT COUNT(ra) > 0 FROM RoomAllocation ra WHERE ra.application.id = :applicationId")
    boolean existsByApplicationId(@Param("applicationId") Long applicationId);

    // Count active allocations for a room
    @Query("SELECT COUNT(ra) FROM RoomAllocation ra WHERE ra.room.id = :roomId AND ra.status = 'ACTIVE'")
    long countActiveAllocationsByRoomId(@Param("roomId") Long roomId);

    // Find active allocation by student ID with eager loading
    @Query("SELECT ra FROM RoomAllocation ra " +
           "LEFT JOIN FETCH ra.student s " +
           "LEFT JOIN FETCH ra.room r " +
           "LEFT JOIN FETCH r.building b " +
           "LEFT JOIN FETCH ra.application a " +
           "WHERE ra.student.id = :studentId AND ra.status = 'ACTIVE'")
    Optional<RoomAllocation> findActiveAllocationByStudentId(@Param("studentId") Long studentId);

    // Find allocation by application ID
    @Query("SELECT ra FROM RoomAllocation ra WHERE ra.application.id = :applicationId")
    Optional<RoomAllocation> findByApplicationId(@Param("applicationId") Long applicationId);

    // Delete all allocations for a student
    void deleteByStudent(com.beezhub.student_accommodation.model.entity.Student student);
}