package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
   List<Room> findByAvailable(boolean available);

   List<Room> findByCapacityGreaterThanEqual(int capacity);
}
