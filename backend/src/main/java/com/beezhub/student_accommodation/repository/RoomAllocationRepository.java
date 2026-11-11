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

import java.time.LocalDate;
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

       // Check if application has already been allocated
       @Query("SELECT COUNT(ra) > 0 FROM RoomAllocation ra WHERE ra.application.id = :applicationId AND ra.status NOT IN (com.beezhub.student_accommodation.model.enums.AllocationStatus.DECLINED, com.beezhub.student_accommodation.model.enums.AllocationStatus.CANCELLED, com.beezhub.student_accommodation.model.enums.AllocationStatus.EXPIRED, com.beezhub.student_accommodation.model.enums.AllocationStatus.CHECK_IN_EXPIRED)")
       boolean existsByApplicationId(@Param("applicationId") Long applicationId);

       /**
        * Every allocation holding a bed in a room, whatever its dates: OFFERED, ACCEPTED or ACTIVE.
        * Only for deciding whether the room can be deleted; capacity is counted by
        * {@link #countHoldingBeds}, which looks at the planned stay dates.
        */
       @Query("SELECT COUNT(ra) FROM RoomAllocation ra WHERE ra.room.id = :roomId AND ra.status IN (com.beezhub.student_accommodation.model.enums.AllocationStatus.ACTIVE, com.beezhub.student_accommodation.model.enums.AllocationStatus.ACCEPTED, com.beezhub.student_accommodation.model.enums.AllocationStatus.OFFERED)")
       long countLiveAllocationsByRoomId(@Param("roomId") Long roomId);

       /**
        * Beds taken in a room during a stay (GH-496): OFFERED, ACCEPTED and ACTIVE allocations whose
        * planned stay overlaps {@code [from, until)}. An offer reserves the bed until it lapses, or two
        * students could be offered the last one. An allocation with no planned dates counts for any
        * stay. Pass {@code StayPeriod.from()} and {@code until()}, which are never null.
        */
       @Query("SELECT COUNT(ra) FROM RoomAllocation ra WHERE ra.room.id = :roomId AND ra.status IN (com.beezhub.student_accommodation.model.enums.AllocationStatus.ACTIVE, com.beezhub.student_accommodation.model.enums.AllocationStatus.ACCEPTED, com.beezhub.student_accommodation.model.enums.AllocationStatus.OFFERED) " +
                     "AND (ra.stayStartDate IS NULL OR ra.stayStartDate < :until) " +
                     "AND (ra.stayEndDate IS NULL OR ra.stayEndDate > :from)")
       long countHoldingBeds(@Param("roomId") Long roomId, @Param("from") LocalDate from,
                     @Param("until") LocalDate until);

       /**
        * Who is in a room on {@code day}, for {@code room.current_occupancy}: everyone checked in,
        * including a resident whose stay has ended but who has not checked out, plus OFFERED and
        * ACCEPTED allocations whose planned stay includes the day. A 2027 offer does not fill a 2026
        * room.
        */
       @Query("SELECT COUNT(ra) FROM RoomAllocation ra WHERE ra.room.id = :roomId AND (" +
                     "ra.status = com.beezhub.student_accommodation.model.enums.AllocationStatus.ACTIVE OR (" +
                     "ra.status IN (com.beezhub.student_accommodation.model.enums.AllocationStatus.ACCEPTED, com.beezhub.student_accommodation.model.enums.AllocationStatus.OFFERED) " +
                     "AND (ra.stayStartDate IS NULL OR ra.stayStartDate <= :day) " +
                     "AND (ra.stayEndDate IS NULL OR ra.stayEndDate > :day)))")
       long countInRoomOn(@Param("roomId") Long roomId, @Param("day") LocalDate day);

       /** Allocations in a room with one status; with ACTIVE, the students physically living there. */
       long countByRoom_IdAndStatus(Long roomId, AllocationStatus status);

       /**
        * Whether a student holds a bed (OFFERED, ACCEPTED or ACTIVE) for a stay overlapping
        * {@code [from, until)} (GH-496). A 2026 resident can be offered a 2027 room, but not a second
        * bed for dates they already have one.
        */
       @Query("SELECT COUNT(ra) > 0 FROM RoomAllocation ra WHERE ra.student.id = :studentId AND ra.status IN (com.beezhub.student_accommodation.model.enums.AllocationStatus.ACTIVE, com.beezhub.student_accommodation.model.enums.AllocationStatus.ACCEPTED, com.beezhub.student_accommodation.model.enums.AllocationStatus.OFFERED) " +
                     "AND (ra.stayStartDate IS NULL OR ra.stayStartDate < :until) " +
                     "AND (ra.stayEndDate IS NULL OR ra.stayEndDate > :from)")
       boolean existsLiveAllocationForStudentDuring(@Param("studentId") Long studentId,
                     @Param("from") LocalDate from, @Param("until") LocalDate until);

       // The same live set as countLiveAllocationsByRoomId, across every room in a building.
       @Query("SELECT COUNT(ra) FROM RoomAllocation ra WHERE ra.room.building.id = :buildingId AND ra.status IN (com.beezhub.student_accommodation.model.enums.AllocationStatus.ACTIVE, com.beezhub.student_accommodation.model.enums.AllocationStatus.ACCEPTED, com.beezhub.student_accommodation.model.enums.AllocationStatus.OFFERED)")
       long countActiveAllocationsByBuildingId(@Param("buildingId") Long buildingId);

       /**
        * Every allocation a student holds a bed with, with details loaded. A student can hold two at
        * once since GH-496, for example a 2026 room they live in and an accepted 2027 offer.
        */
       @Query("SELECT ra FROM RoomAllocation ra " +
                     "LEFT JOIN FETCH ra.student s " +
                     "LEFT JOIN FETCH ra.room r " +
                     "LEFT JOIN FETCH r.building b " +
                     "LEFT JOIN FETCH ra.application a " +
                     "WHERE ra.student.id = :studentId AND ra.status IN (com.beezhub.student_accommodation.model.enums.AllocationStatus.ACTIVE, com.beezhub.student_accommodation.model.enums.AllocationStatus.ACCEPTED, com.beezhub.student_accommodation.model.enums.AllocationStatus.OFFERED)")
       List<RoomAllocation> findLiveAllocationsByStudentId(@Param("studentId") Long studentId);

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

       long countByAcademicYear_Id(Long academicYearId);

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

        /**
         * Companies with accepted places that may still have to reconfirm before move-in (GH-496).
         * The daily job runs once per company with that company in the tenant context.
         */
        @Query("SELECT DISTINCT ra.tenantId FROM RoomAllocation ra " +
               "WHERE ra.status = com.beezhub.student_accommodation.model.enums.AllocationStatus.ACCEPTED " +
               "AND ra.reconfirmedAt IS NULL AND ra.acceptedAt IS NOT NULL AND ra.stayStartDate > :today")
        List<Long> findTenantIdsAwaitingReconfirmation(@Param("today") LocalDate today);

        /** One company's accepted places that may still have to reconfirm, by move-in date. */
        @Query("SELECT ra.id FROM RoomAllocation ra " +
               "WHERE ra.tenantId = :tenantId " +
               "AND ra.status = com.beezhub.student_accommodation.model.enums.AllocationStatus.ACCEPTED " +
               "AND ra.reconfirmedAt IS NULL AND ra.acceptedAt IS NOT NULL AND ra.stayStartDate > :today " +
               "ORDER BY ra.stayStartDate, ra.id")
        List<Long> findIdsAwaitingReconfirmation(@Param("tenantId") Long tenantId, @Param("today") LocalDate today);

        /** Companies with an allocation in one of the statuses (GH-498), across every company. */
        @Query("SELECT DISTINCT ra.tenantId FROM RoomAllocation ra WHERE ra.status IN :statuses")
        List<Long> findTenantIdsWithStatusIn(@Param("statuses") Collection<AllocationStatus> statuses);

        /** One company's allocations in the statuses, with the student and application loaded. */
        @Query("SELECT ra FROM RoomAllocation ra JOIN FETCH ra.student s LEFT JOIN FETCH s.institution " +
               "LEFT JOIN FETCH ra.application a LEFT JOIN FETCH a.institution " +
               "WHERE ra.tenantId = :tenantId AND ra.status IN :statuses")
        List<RoomAllocation> findByTenantIdAndStatusInWithStudent(@Param("tenantId") Long tenantId,
                        @Param("statuses") Collection<AllocationStatus> statuses);

        /** Whether the student has held an allocation in one of the statuses at a company (GH-499). */
        boolean existsByStudent_IdAndTenantIdAndStatusIn(Long studentId, Long tenantId,
                        Collection<AllocationStatus> statuses);
}
