package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    // Check for duplicate room number within a building, excluding a specific room (for updates)
    boolean existsByRoomNumberAndBuilding_IdAndIdNot(String roomNumber, Long buildingId, Long excludeId);
}