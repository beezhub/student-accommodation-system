package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomAllocationRepository
              extends JpaRepository<RoomAllocation, Long>, JpaSpecificationExecutor<RoomAllocation> {

       @EntityGraph(attributePaths = { "student", "room", "room.building", "application" })
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

       // Check if student has any active (non-terminal) allocation
       @Query("SELECT COUNT(ra) > 0 FROM RoomAllocation ra WHERE ra.student.id = :studentId AND ra.status NOT IN (com.beezhub.student_accommodation.model.enums.AllocationStatus.COMPLETED, com.beezhub.student_accommodation.model.enums.AllocationStatus.TERMINATED, com.beezhub.student_accommodation.model.enums.AllocationStatus.CANCELLED, com.beezhub.student_accommodation.model.enums.AllocationStatus.DECLINED, com.beezhub.student_accommodation.model.enums.AllocationStatus.EXPIRED, com.beezhub.student_accommodation.model.enums.AllocationStatus.CHECK_IN_EXPIRED)")
       boolean existsActiveAllocationByStudentId(@Param("studentId") Long studentId);

       // Check if application has already been allocated
       @Query("SELECT COUNT(ra) > 0 FROM RoomAllocation ra WHERE ra.application.id = :applicationId AND ra.status NOT IN (com.beezhub.student_accommodation.model.enums.AllocationStatus.DECLINED, com.beezhub.student_accommodation.model.enums.AllocationStatus.CANCELLED, com.beezhub.student_accommodation.model.enums.AllocationStatus.EXPIRED, com.beezhub.student_accommodation.model.enums.AllocationStatus.CHECK_IN_EXPIRED)")
       boolean existsByApplicationId(@Param("applicationId") Long applicationId);

       // Count active allocations for a room (occupancy)
       // Only count Check-In (ACTIVE) for strictly physical occupancy, but for
       // capacity PLANNING we should count ACCEPTED too?
       // Business decision: Capacity includes ACTIVE and ACCEPTED. OFFERED reserves a
       // spot?
       // Let's assume Capacity = ACTIVE + ACCEPTED + OFFERED to prevents overbooking.
       @Query("SELECT COUNT(ra) FROM RoomAllocation ra WHERE ra.room.id = :roomId AND ra.status IN (com.beezhub.student_accommodation.model.enums.AllocationStatus.ACTIVE, com.beezhub.student_accommodation.model.enums.AllocationStatus.ACCEPTED, com.beezhub.student_accommodation.model.enums.AllocationStatus.OFFERED)")
       long countActiveAllocationsByRoomId(@Param("roomId") Long roomId);

       // Find active allocation by student ID with eager loading
       @Query("SELECT ra FROM RoomAllocation ra " +
                     "LEFT JOIN FETCH ra.student s " +
                     "LEFT JOIN FETCH ra.room r " +
                     "LEFT JOIN FETCH r.building b " +
                     "LEFT JOIN FETCH ra.application a " +
                     "WHERE ra.student.id = :studentId AND ra.status IN (com.beezhub.student_accommodation.model.enums.AllocationStatus.ACTIVE, com.beezhub.student_accommodation.model.enums.AllocationStatus.ACCEPTED, com.beezhub.student_accommodation.model.enums.AllocationStatus.OFFERED)")
       Optional<RoomAllocation> findActiveAllocationByStudentId(@Param("studentId") Long studentId);

       // Find allocation by application ID
       @Query("SELECT ra FROM RoomAllocation ra WHERE ra.application.id = :applicationId")
       Optional<RoomAllocation> findByApplicationId(@Param("applicationId") Long applicationId);

       /**
        * Allocations for one application that still hold a room, with the room and student loaded.
        *
        * <p>Used when an application reaches a terminal status: those allocations have to be
        * released, or the room stays counted as occupied until the expiry sweep runs and the
        * student keeps seeing an offer they can no longer act on (GH-223).
        */
       @Query("SELECT ra FROM RoomAllocation ra " +
                     "LEFT JOIN FETCH ra.room r " +
                     "LEFT JOIN FETCH r.building " +
                     "LEFT JOIN FETCH ra.student s " +
                     "LEFT JOIN FETCH s.appUser " +
                     "WHERE ra.application.id = :applicationId AND ra.status IN :statuses")
       List<RoomAllocation> findByApplicationIdAndStatusIn(
                     @Param("applicationId") Long applicationId,
                     @Param("statuses") Collection<AllocationStatus> statuses);

       // Delete all allocations for a student
       /** Every allocation for a student, used to find the rooms to recompute after a delete (GH-136). */
       List<RoomAllocation> findByStudent_Id(Long studentId);

       void deleteByStudent(com.beezhub.student_accommodation.model.entity.Student student);

       // Count declined allocations by application ID (for decline limit tracking)
       @Query("SELECT COUNT(ra) FROM RoomAllocation ra WHERE ra.application.id = :applicationId AND ra.status = com.beezhub.student_accommodation.model.enums.AllocationStatus.DECLINED")
       long countDeclinedAllocationsByApplicationId(@Param("applicationId") Long applicationId);

       // Count allocations created within a recent window for a given academic year —
       // used as an "allocation velocity" signal to estimate waitlist wait time.
       long countByAcademicYear_IdAndCreatedAtAfter(Long academicYearId, LocalDateTime createdAt);

       // Check if a student has previously declined a specific room
       @Query("SELECT COUNT(ra) > 0 FROM RoomAllocation ra WHERE ra.student.id = :studentId AND ra.room.id = :roomId AND ra.status = com.beezhub.student_accommodation.model.enums.AllocationStatus.DECLINED")
       boolean hasStudentDeclinedRoom(@Param("studentId") Long studentId, @Param("roomId") Long roomId);

       // Find expired offers
       @Query("SELECT ra FROM RoomAllocation ra " +
              "LEFT JOIN FETCH ra.student s " +
              "LEFT JOIN FETCH s.appUser " +
              "LEFT JOIN FETCH ra.room r " +
              "LEFT JOIN FETCH r.building " +
              "LEFT JOIN FETCH ra.application a " +
              "WHERE ra.status = :status AND ra.offerExpiryDate < :cutoffDate")
       List<RoomAllocation> findByStatusAndOfferExpiryDateBefore(
           @Param("status") AllocationStatus status,
           @Param("cutoffDate") LocalDateTime cutoffDate);

        // Find expired check-ins
        @Query("SELECT ra FROM RoomAllocation ra " +
               "LEFT JOIN FETCH ra.student s " +
               "LEFT JOIN FETCH s.appUser " +
               "LEFT JOIN FETCH ra.room r " +
               "LEFT JOIN FETCH r.building " +
               "LEFT JOIN FETCH ra.application a " +
               "WHERE ra.status = :status AND ra.checkInExpiryDate < :cutoffDate")
        List<RoomAllocation> findByStatusAndCheckInExpiryDateBefore(
            @Param("status") AllocationStatus status,
            @Param("cutoffDate") LocalDateTime cutoffDate);

        // Find OFFERED allocations expiring within a window that have not yet had a reminder sent
        @Query("SELECT ra FROM RoomAllocation ra " +
               "LEFT JOIN FETCH ra.student s " +
               "LEFT JOIN FETCH s.appUser " +
               "LEFT JOIN FETCH ra.room r " +
               "LEFT JOIN FETCH r.building " +
               "LEFT JOIN FETCH ra.application a " +
               "WHERE ra.status = com.beezhub.student_accommodation.model.enums.AllocationStatus.OFFERED " +
               "AND ra.offerExpiryDate >= :windowStart " +
               "AND ra.offerExpiryDate < :windowEnd " +
               "AND ra.reminder24hSent = false")
        List<RoomAllocation> findOffersExpiringSoonWithoutReminder(
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd);

        // Find ACCEPTED allocations whose check-in deadline falls within the window and have not had a reminder sent
        @Query("SELECT ra FROM RoomAllocation ra " +
               "LEFT JOIN FETCH ra.student s " +
               "LEFT JOIN FETCH s.appUser " +
               "LEFT JOIN FETCH ra.room r " +
               "LEFT JOIN FETCH r.building " +
               "LEFT JOIN FETCH ra.application a " +
               "WHERE ra.status = com.beezhub.student_accommodation.model.enums.AllocationStatus.ACCEPTED " +
               "AND ra.checkInExpiryDate >= :windowStart " +
               "AND ra.checkInExpiryDate < :windowEnd " +
               "AND ra.checkInReminder24hSent = false")
        List<RoomAllocation> findCheckInsExpiringSoonWithoutReminder(
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd);
}