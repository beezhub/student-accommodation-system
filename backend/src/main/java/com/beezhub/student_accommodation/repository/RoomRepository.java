package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {
    Optional<Room> findFirstByStatusOrderByBuilding_IdAscRoomNumberAsc(String status);
}