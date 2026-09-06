package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * Rooms that still have space: capacity above the live count of allocations holding a bed.
     *
     * <p>ACTIVE, ACCEPTED and OFFERED all hold one — an offer reserves the bed until it lapses, or
     * two students could be offered the last one. This is the same set
     * {@code countActiveAllocationsByRoomId} counts, and the one {@code RoomOccupancyManager}
     * derives occupancy from, so the read and the write agree.
     *
     * <p>MAINTENANCE is excluded. It was not before, and nothing downstream filtered it either, so
     * auto-allocation could place a student in a room an admin had deliberately parked (GH-221).
     */
    @Query("SELECT r FROM Room r WHERE r.status <> 'MAINTENANCE' AND r.capacity > " +
           "(SELECT COUNT(ra) FROM RoomAllocation ra WHERE ra.room.id = r.id AND ra.status IN ('ACTIVE', 'ACCEPTED', 'OFFERED'))")
    List<Room> findRoomsWithAvailableCapacity();

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
     */
    @Query("SELECT r.building.id, COUNT(r) FROM Room r " +
           "WHERE r.building IS NOT NULL AND r.status <> 'MAINTENANCE' AND r.capacity > " +
           "(SELECT COUNT(ra) FROM RoomAllocation ra WHERE ra.room.id = r.id AND ra.status IN ('ACTIVE', 'ACCEPTED', 'OFFERED')) " +
           "GROUP BY r.building.id")
    List<Object[]> findAvailableRoomCountByBuilding();

    /**
     * How many rooms in one building still have space. Same definition as
     * {@link #findAvailableRoomCountByBuilding()}, for the single-building detail view.
     */
    @Query("SELECT COUNT(r) FROM Room r " +
           "WHERE r.building.id = :buildingId AND r.status <> 'MAINTENANCE' AND r.capacity > " +
           "(SELECT COUNT(ra) FROM RoomAllocation ra WHERE ra.room.id = r.id AND ra.status IN ('ACTIVE', 'ACCEPTED', 'OFFERED'))")
    long countAvailableRoomsByBuildingId(@Param("buildingId") Long buildingId);
}