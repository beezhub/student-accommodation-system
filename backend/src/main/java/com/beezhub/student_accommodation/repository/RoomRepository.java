package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {
    Optional<Room> findFirstByStatusOrderByBuilding_IdAscRoomNumberAsc(String status);
    
    // Find rooms by building with attributes loaded
    @Query("SELECT r FROM Room r LEFT JOIN FETCH r.attributes LEFT JOIN FETCH r.building WHERE r.building.id = :buildingId")
    List<Room> findByBuildingIdWithAttributes(@Param("buildingId") Long buildingId);
    
    // Find rooms by building
    List<Room> findByBuildingId(Long buildingId);
    
    // Find room by ID with attributes loaded
    @Query("SELECT r FROM Room r LEFT JOIN FETCH r.attributes LEFT JOIN FETCH r.building WHERE r.id = :roomId")
    Optional<Room> findByIdWithAttributes(@Param("roomId") Long roomId);
    
    // Room status counts
    long countByStatus(String status);
    
    // Capacity aggregation
    @Query("SELECT COALESCE(SUM(r.capacity), 0) FROM Room r")
    Long sumTotalCapacity();
    
    @Query("SELECT COALESCE(SUM(r.currentOccupancy), 0) FROM Room r")
    Long sumTotalOccupancy();
    
    // Building-level aggregation
    @Query("SELECT r.building.id, r.building.name, COUNT(r), COALESCE(SUM(r.capacity), 0), COALESCE(SUM(r.currentOccupancy), 0) " +
           "FROM Room r WHERE r.building IS NOT NULL GROUP BY r.building.id, r.building.name ORDER BY r.building.name")
    List<Object[]> getBuildingOccupancyStats();
    
    // Room counts by building
    @Query("SELECT COUNT(r) FROM Room r WHERE r.building.id = :buildingId")
    long countByBuildingId(@Param("buildingId") Long buildingId);

    // Check for duplicate room number within a building (per-building uniqueness)
    boolean existsByRoomNumberAndBuilding_Id(String roomNumber, Long buildingId);

    // Batch check: return the subset of roomNumbers that already exist in a building (for bulk operations)
    @Query("SELECT r.roomNumber FROM Room r WHERE r.building.id = :buildingId AND r.roomNumber IN :roomNumbers")
    Set<String> findRoomNumbersByBuildingIdAndRoomNumberIn(@Param("buildingId") Long buildingId, @Param("roomNumbers") Collection<String> roomNumbers);

    // Check for duplicate room number within a building, excluding a specific room (for updates)
    boolean existsByRoomNumberAndBuilding_IdAndIdNot(String roomNumber, Long buildingId, Long excludeId);

    /**
     * Rooms that can take an offer for some dates: every room not in MAINTENANCE. Whether a bed is
     * free is a question about a stay's dates (GH-496), so the batch job checks each student's stay
     * against {@code RoomAllocationRepository.countHoldingBeds} as it matches them.
     *
     * <p>MAINTENANCE is excluded, so auto-allocation never places a student in a room an admin has
     * deliberately parked (GH-221).
     */
    @Query("SELECT r FROM Room r WHERE r.status <> 'MAINTENANCE'")
    List<Room> findRoomsOpenForOffers();

    // Pricing summary per building: [buildingId, minMonthlyRate, maxMonthlyRate]
    @Query("SELECT r.building.id, MIN(r.monthlyRate), MAX(r.monthlyRate) " +
           "FROM Room r WHERE r.building IS NOT NULL AND r.monthlyRate IS NOT NULL " +
           "GROUP BY r.building.id")
    List<Object[]> findPricingSummaryByBuilding();

    /**
     * Available room count per building: [buildingId, availableCount].
     *
     * <p>Counted from the live allocation count, not {@code r.status = 'AVAILABLE'}. That string is
     * derived by {@code RoomOccupancyManager.deriveStatus}, which only says AVAILABLE when the room
     * is completely empty — so a two-bed room with one occupant read as having no space at all, and
     * every shared room was undercounted here (GH-221).
     *
     * <p>A bed counts as taken when any live allocation holds it from {@code today} on (GH-496): a
     * stay that has ended no longer takes it, while one planned for next year still does, so a
     * room is never shown as free when it is already promised.
     */
    @Query("SELECT r.building.id, COUNT(r) FROM Room r " +
           "WHERE r.building IS NOT NULL AND r.status <> 'MAINTENANCE' AND r.capacity > " +
           "(SELECT COUNT(ra) FROM RoomAllocation ra WHERE ra.room.id = r.id AND ra.status IN ('ACTIVE', 'ACCEPTED', 'OFFERED') " +
           "AND (ra.stayEndDate IS NULL OR ra.stayEndDate > :today)) " +
           "GROUP BY r.building.id")
    List<Object[]> findAvailableRoomCountByBuilding(@Param("today") java.time.LocalDate today);

    /**
     * How many rooms in one building still have space. Same definition as
     * {@link #findAvailableRoomCountByBuilding()}, for the single-building detail view.
     */
    @Query("SELECT COUNT(r) FROM Room r " +
           "WHERE r.building.id = :buildingId AND r.status <> 'MAINTENANCE' AND r.capacity > " +
           "(SELECT COUNT(ra) FROM RoomAllocation ra WHERE ra.room.id = r.id AND ra.status IN ('ACTIVE', 'ACCEPTED', 'OFFERED') " +
           "AND (ra.stayEndDate IS NULL OR ra.stayEndDate > :today))")
    long countAvailableRoomsByBuildingId(@Param("buildingId") Long buildingId,
                                         @Param("today") java.time.LocalDate today);

    /**
     * Loads a room and locks its row until the transaction ends (GH-498). Two students asking for
     * the last bed of the same stay at once are served one after the other, so the second sees the
     * first one's extension in its count.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);
}
